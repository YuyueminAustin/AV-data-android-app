package com.msra.avliveness.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.content.Loader;

import com.msra.avliveness.fragments.LogFragment;
import com.msra.avliveness.fragments.SensorDataFragment;
import com.msra.avliveness.services.AudioPlayerHelper;
import com.msra.avliveness.services.AudioRecorderHelper;
import com.msra.avliveness.services.CameraController;
import com.msra.avliveness.services.CameraNoViewHelper;
import com.msra.avliveness.services.MqttSyncClient;
import com.msra.avliveness.FigActivity;
import com.msra.avliveness.utils.DeviceQuery;
import com.msra.avliveness.utils.FileNameManager;
import com.msra.avliveness.utils.NtpTimeClient;
import com.msra.avliveness.models.PreProcessAudio;
//import com.msra.avliveness.models.PreProcessVideo;
import com.msra.avliveness.R;
import com.otaliastudios.cameraview.CameraView;

//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.imgproc.Imgproc;
////import org.pytorch.Module;
//import org.pytorch.LiteModuleLoader;
//import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedWriter;
import java.math.BigDecimal;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";
    private static final String ROLE_STANDALONE = "standalone";
    private static final String ROLE_CONTROLLER = "controller";
    private static final String ROLE_DEPENDENT = "dependent";
    private static final String COMMAND_PLAYBACK = "Playback";
    private static final String COMMAND_RECORD = "Record";
    private static final String COMMAND_STOP = "Stop";
    private static final String SETTINGS_NAME = "Settings";
    private static final String KEY_ROLE = "Role";
    private static final String KEY_AUDIO_TX_ENABLED = "AudioTxEnabled";
    private static final String KEY_AUDIO_RX_ENABLED = "AudioRxEnabled";
    private static final String KEY_VIDEO_ENABLED = "Video";
    private static final String KEY_PLAY_FROM_FILE_ENABLED = "PlayFromFile";
    private static final String KEY_PLAY_FILE_NAME = "PlayFileName";
    private static final String KEY_SIGNAL_LOOP = "SignalLoop";
    private static final String KEY_SIGNAL_IDLE_BETWEEN_SECONDS = "SignalIdleBetweenSeconds";
    private static final String KEY_EXPERIMENT_NAME = "ExperimentName";
    private static final String KEY_MQTT_HOST = "MqttHost";
    private static final String KEY_MQTT_PORT = "MqttPort";
    private static final String KEY_SESSION_DURATION_SECONDS = "SessionDurationSeconds";
    private static final String DEFAULT_PLAY_FILE = "logsweep.wav";
    private static final String DEFAULT_MQTT_HOST = "localhost";
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final double DEFAULT_SESSION_DURATION_SECONDS = 10.0;
    private static final boolean DEFAULT_SIGNAL_LOOP = true;
    private static final double DEFAULT_SIGNAL_IDLE_BETWEEN_SECONDS = 1.0;
    private static final long MQTT_ACK_TIMEOUT_MS = 5000;
    private static final long MQTT_PUBLISH_TIMEOUT_MS = 3000;
    private static final long MQTT_SYNC_LEAD_TIME_MS = 3000;
    private static final long MQTT_TIMING_TIMEOUT_MS = 5000;

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 201;

    private static final int REQUEST_PERMISSIONS = 1;
    private TextView remoteStatusTextView;
    private Button startEndButton, buttonLog, connectButton;

    private TextureView textureView;
    private View audioAmplitudeView;

//    private CameraHelper cameraHelper;
//    private CameraNoViewHelper cameraHelper;
    private AudioPlayerHelper audioPlayerHelper;
    private AudioRecorderHelper audioRecorderHelper;

    private FileNameManager fileNameManager;

    private String selectedCameraId = "0";
    private String selectedSpeakerId;
    private String selectedMicrophoneId;
    private boolean audioTxEnabled;
    private boolean audioRxEnabled;
    private boolean videoEnabled;
    private boolean playFromFileEnabled;
    private String role = ROLE_CONTROLLER;
    private boolean signalLoopEnabled = DEFAULT_SIGNAL_LOOP;
    private double signalIdleBetweenLoopsSeconds = DEFAULT_SIGNAL_IDLE_BETWEEN_SECONDS;

    private String playFile;
    private String experimentName;

    private String audioFilePath;

    private boolean isStarted = false;

    private static final int WIN_LEN = 3; // window length in seconds
    private static final int WIN_STEP = 1; // window step in seconds
    private static final int VIDEO_FPS = 30;
    private static final int AUDIO_FS = 48000;

    private CameraController cameraController;
    public CameraView cameraView;

    public String audioPath;
    public String videoPath;

    public File sessionFolder;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mqttExecutor = Executors.newSingleThreadExecutor();
    private MqttSyncClient mqttSyncClient;
    private final Object peerTimingLock = new Object();
    private String mqttClientRole;
    private String pendingRemoteCommand;
    private Runnable pendingStartRunnable;
    private Runnable pendingAutoStopRunnable;
    private Double sessionTargetTimeUnix;
    private Double sessionActualStartTimeUnix;
    private String sessionOperation;
    private boolean sessionControlledByMqtt;
    private JSONObject latestPeerTimingPayload;
    private boolean mqttConnected;
    private String mqttConnectionState = "Awaiting connect";
    private boolean pendingConnectAfterPermission;
    private double sessionDurationSeconds = DEFAULT_SESSION_DURATION_SECONDS;
    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Toast.makeText(this, "Settings updated.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

//    private View overlay;
//    private TextView countdownText;

//    private PreProcessAudio preProcessAudio;
//    private PreProcessVideo preProcessVideo;

//    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            Log.d(TAG, "surface texture available");
//            cameraHelper.openCamera();
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            cameraHelper.stopCamera();
//            return true;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Liveness Detection");

        startEndButton = findViewById(R.id.buttonStartEnd);
        startEndButton.setText("Start");
        connectButton = findViewById(R.id.buttonConnect);
        remoteStatusTextView = findViewById(R.id.textRemoteStatus);
        connectButton.setOnClickListener(v -> onConnectButtonClicked());

        Button buttonFig = findViewById(R.id.buttonFig);
        buttonFig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FigActivity.class);
                startActivity(intent);
            }
        });

        WebView instructionsWebView = findViewById(R.id.instructions);
        String instructionsHtml = "<html><body>" +
                "<h2>Instruction</h2>" +
                "<ol>" +
                "<li>Press Settings and check whether using front camera</li>" +
                "<li>Press Start and look at your cameras. Do not stay far way.</li>" +
                "<li><b>Do the following:</b><br>" +
                "<br>" +
                "<b><i>'Move towards the camera and back'</i></b></li>" +
                "<br>" +
                "<li>Press End when you are finished, and wait for the results.</li>" +
                "</ol>" +
                "</body></html>";

        instructionsWebView.loadData(instructionsHtml, "text/html", "UTF-8");

        buttonLog = findViewById(R.id.buttonLog);
        buttonLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogFragment();
            }
        });

        cameraView = findViewById(R.id.camera);
        cameraController = new CameraController(this, cameraView);


        DeviceQuery.listMicrophones(this);

        fileNameManager = new FileNameManager();

//        // Check and request permissions
//        overlay = findViewById(R.id.overlay);
//        countdownText = findViewById(R.id.countdownText);

//        boolean openCVIsInitialized = OpenCVLoader.initLocal();


        startEndButton.setOnClickListener(v -> {
            if (isStarted) {
                Log.i("Button", "End Button Pressed");
                if (ROLE_CONTROLLER.equals(role) && sessionControlledByMqtt) {
                    sendRemoteStopCommandAsync();
                }
                setStartButtonIdle();
                endAllThreads();
                return;
            }

            Log.i("Button", "Start Button Pressed");
            if (!arePermissionsGranted()) {
                Log.i("Permission", "Requesting Permissions");
                requestPermissions();
                return;
            }

            init();
            if (ROLE_DEPENDENT.equals(role)) {
                Toast.makeText(this, "Dependent mode is remote-controlled. Connect and wait for command.", Toast.LENGTH_LONG).show();
                return;
            }

            if (ROLE_CONTROLLER.equals(role)) {
                if (!mqttConnected) {
                    Toast.makeText(this, "Connect to MQTT first.", Toast.LENGTH_LONG).show();
                    return;
                }
                startControllerSynchronizedSession();
                return;
            }

            scheduleSessionStartAt(
                    System.currentTimeMillis() + 5000L,
                    null,
                    deriveOperationForCurrentFlags(),
                    false
            );
        });

        loadRoleFromPreferences();
        applyRoleConnectionUiState();

//        preProcessAudio = new PreProcessAudio();
//        preProcessVideo = new PreProcessVideo();
    }

    private void init() {


//        textureView = findViewById(R.id.textureView);
//        audioAmplitudeView = findViewById(R.id.audioAmplitudeView);

        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        Intent intent = getIntent();
        selectedCameraId = intent.getStringExtra("selectedCameraId");
        if (selectedCameraId == null) {
            selectedCameraId = "1"; // set as the front camera
        }
        selectedSpeakerId = intent.getStringExtra("selectedSpeakerId");
        if (selectedSpeakerId == null) {
            selectedSpeakerId = "0";
        }
        selectedMicrophoneId = intent.getStringExtra("selectedMicrophoneId");
        if (selectedMicrophoneId == null) {
            selectedMicrophoneId = "0";
        }
        audioTxEnabled = intent.hasExtra("audioTxEnabled")
                ? intent.getBooleanExtra("audioTxEnabled", false)
                : preferences.getBoolean(KEY_AUDIO_TX_ENABLED, false);
        audioRxEnabled = intent.hasExtra("audioRxEnabled")
                ? intent.getBooleanExtra("audioRxEnabled", false)
                : preferences.getBoolean(KEY_AUDIO_RX_ENABLED, false);
        videoEnabled = intent.hasExtra("videoEnabled")
                ? intent.getBooleanExtra("videoEnabled", true)
                : preferences.getBoolean(KEY_VIDEO_ENABLED, false);
        playFromFileEnabled = intent.hasExtra("playFromFileEnabled")
                ? intent.getBooleanExtra("playFromFileEnabled", false)
                : preferences.getBoolean(KEY_PLAY_FROM_FILE_ENABLED, false);
        signalLoopEnabled = intent.hasExtra("signalLoopEnabled")
                ? intent.getBooleanExtra("signalLoopEnabled", DEFAULT_SIGNAL_LOOP)
                : preferences.getBoolean(KEY_SIGNAL_LOOP, DEFAULT_SIGNAL_LOOP);
        String idleSecondsRaw = intent.hasExtra("signalIdleSeconds")
                ? String.valueOf(intent.getDoubleExtra("signalIdleSeconds", DEFAULT_SIGNAL_IDLE_BETWEEN_SECONDS))
                : preferences.getString(KEY_SIGNAL_IDLE_BETWEEN_SECONDS, String.valueOf(DEFAULT_SIGNAL_IDLE_BETWEEN_SECONDS));
        signalIdleBetweenLoopsSeconds = parseDoubleOrDefault(idleSecondsRaw, DEFAULT_SIGNAL_IDLE_BETWEEN_SECONDS);
        experimentName = intent.getStringExtra("experimentName");
        if (experimentName == null) {
            experimentName = preferences.getString(KEY_EXPERIMENT_NAME, "");
        }
        playFile = intent.getStringExtra("playFile");
        if (playFile == null || playFile.isEmpty()) {
            playFile = readPlayFileFromPreferences(preferences);
        }
        if (playFile == null || playFile.isEmpty()) {
            playFile = DEFAULT_PLAY_FILE;
        }
        role = intent.getStringExtra("role");
        if (role == null) {
            role = preferences.getString(KEY_ROLE, ROLE_CONTROLLER);
        }
        sessionDurationSeconds = readSessionDurationSeconds(intent, preferences);
        applyRoleAudioPolicy();
        refreshRemoteStatusUi();
            Log.i(TAG, "Selected role: " + role + ", audioTxEnabled=" + audioTxEnabled + ", audioRxEnabled=" + audioRxEnabled + ", sessionDurationSeconds=" + sessionDurationSeconds
                    + ", signalLoopEnabled=" + signalLoopEnabled + ", signalIdleBetweenLoopsSeconds=" + signalIdleBetweenLoopsSeconds);

        //        cameraHelper = new CameraHelper(this, textureView, selectedCameraId);
//        cameraHelper = new CameraNoViewHelper(this, selectedCameraId, textureView);
//        cameraHelper.setVideoSize(new Size(640, 360));


        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(0.75*maxVolume), 0);

        audioPlayerHelper = new AudioPlayerHelper(this);
        int microphoneId;
        try {
            microphoneId = Integer.parseInt(selectedMicrophoneId);
        } catch (NumberFormatException e) {
            microphoneId = 0;
        }
        audioRecorderHelper = new AudioRecorderHelper(MediaRecorder.AudioSource.UNPROCESSED, this, microphoneId);


//        if (videoEnabled){
//            cameraHelper.startBackgroundThread();
//            cameraHelper.openCamera();
////            cameraHelper.startCamera();
//        }
    }

    private void applyRoleAudioPolicy() {
        if (role == null) {
            role = ROLE_CONTROLLER;
        }
        if ("control".equals(role)) {
            role = ROLE_CONTROLLER;
        }

        if (ROLE_STANDALONE.equals(role)) {
            audioTxEnabled = true;
            audioRxEnabled = true;
            return;
        }
        if (ROLE_DEPENDENT.equals(role)) {
            // Network-triggered control for dependent will be added later.
            audioTxEnabled = false;
            audioRxEnabled = false;
            return;
        }

        role = ROLE_CONTROLLER;
        if (audioTxEnabled && audioRxEnabled) {
            audioRxEnabled = false;
        }
        if (!audioTxEnabled && !audioRxEnabled) {
            audioTxEnabled = true;
        }
    }

    private void loadRoleFromPreferences() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        role = preferences.getString(KEY_ROLE, ROLE_CONTROLLER);
    }

    private void onConnectButtonClicked() {
        if (ROLE_STANDALONE.equals(role)) {
            return;
        }
        if (mqttConnected) {
            disconnectMqttClient();
            pendingRemoteCommand = null;
            setMqttConnectionState(false, "Disconnected");
            return;
        }
        if (!arePermissionsGranted()) {
            pendingConnectAfterPermission = true;
            requestPermissions();
            return;
        }
        pendingConnectAfterPermission = false;
        configureMqttForRole();
    }

    private void applyRoleConnectionUiState() {
        if (ROLE_STANDALONE.equals(role)) {
            disconnectMqttClient();
            setMqttConnectionState(false, "Disabled");
            return;
        }
        if (mqttClientRole != null && !role.equals(mqttClientRole)) {
            disconnectMqttClient();
        }
        boolean connected = mqttSyncClient != null && mqttSyncClient.isConnected();
        setMqttConnectionState(connected, connected ? "Connected" : "Awaiting connect");
    }

    private void refreshControlButtons() {
        if (startEndButton == null || connectButton == null) {
            return;
        }

        if (ROLE_STANDALONE.equals(role)) {
            connectButton.setText("Connect");
            connectButton.setEnabled(false);
            ViewCompat.setBackgroundTintList(connectButton, ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            if (!isStarted && pendingStartRunnable == null) {
                setStartButtonIdle();
                startEndButton.setEnabled(true);
            }
            return;
        }

        connectButton.setEnabled(true);
        if (mqttConnected) {
            connectButton.setText("Disconnect");
            ViewCompat.setBackgroundTintList(connectButton, ColorStateList.valueOf(Color.parseColor("#B00020")));
        } else {
            connectButton.setText("Connect");
            ViewCompat.setBackgroundTintList(connectButton, ColorStateList.valueOf(Color.parseColor("#1565C0")));
        }

        if (isStarted || pendingStartRunnable != null) {
            return;
        }

        if (ROLE_DEPENDENT.equals(role)) {
            setStartButtonIdle();
            startEndButton.setEnabled(false);
            ViewCompat.setBackgroundTintList(startEndButton, ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            return;
        }

        setStartButtonIdle();
        if (ROLE_CONTROLLER.equals(role) && !mqttConnected) {
            startEndButton.setEnabled(false);
            ViewCompat.setBackgroundTintList(startEndButton, ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        } else {
            startEndButton.setEnabled(true);
        }
    }

    private void setMqttConnectionState(boolean connected, String state) {
        mqttConnected = connected;
        mqttConnectionState = (state == null || state.trim().isEmpty())
                ? (connected ? "Connected" : "Disconnected")
                : state.trim();
        runOnUiThread(() -> {
            refreshRemoteStatusUi();
            refreshControlButtons();
        });
    }

    private void refreshRemoteStatusUi() {
        if (remoteStatusTextView == null) {
            return;
        }
        if (ROLE_STANDALONE.equals(role)) {
            remoteStatusTextView.setVisibility(View.GONE);
            return;
        }

        remoteStatusTextView.setVisibility(View.VISIBLE);
        String broker = getMqttHost() + ":" + getMqttPort();
        String state = mqttConnectionState;
        if (ROLE_DEPENDENT.equals(role) && mqttConnected && !isStarted && pendingStartRunnable == null) {
            state = "Connected (awaiting remote start)";
        }
        if (!mqttConnected && ("Disconnected".equalsIgnoreCase(state) || state == null || state.trim().isEmpty())) {
            state = "Awaiting connect";
        }
        String status = "Remote (" + role + "): " + state + " @ " + broker;
        remoteStatusTextView.setText(status);

        if (mqttConnected) {
            remoteStatusTextView.setTextColor(Color.parseColor("#006400"));
        } else if ("Connecting...".equalsIgnoreCase(mqttConnectionState)) {
            remoteStatusTextView.setTextColor(Color.parseColor("#CC8400"));
        } else {
            remoteStatusTextView.setTextColor(Color.parseColor("#B00020"));
        }
    }

    private void configureMqttForRole() {
        if (ROLE_STANDALONE.equals(role)) {
            disconnectMqttClient();
            setMqttConnectionState(false, "Disabled");
            return;
        }

        String mqttHost = getMqttHost();
        int mqttPort = getMqttPort();
        boolean roleChanged = mqttSyncClient == null || !role.equals(mqttClientRole);
        if (roleChanged) {
            disconnectMqttClient();
            mqttClientRole = role;
            mqttSyncClient = new MqttSyncClient(role, createMqttListener());
        }
        mqttExecutor.execute(() -> {
            try {
                if (mqttSyncClient != null && !mqttSyncClient.isConnected()) {
                    setMqttConnectionState(false, "Connecting...");
                    mqttSyncClient.connect(mqttHost, mqttPort);
                    Log.i(TAG, "MQTT connected, role=" + role + ", broker=" + mqttHost + ":" + mqttPort);
                } else if (mqttSyncClient != null && mqttSyncClient.isConnected()) {
                    setMqttConnectionState(true, "Connected");
                }
            } catch (Exception e) {
                Log.e(TAG, "MQTT connect failed", e);
                setMqttConnectionState(false, "Connect failed");
            }
        });
    }

    private void disconnectMqttClient() {
        if (mqttSyncClient == null) {
            return;
        }
        MqttSyncClient clientToDisconnect = mqttSyncClient;
        mqttSyncClient = null;
        mqttClientRole = null;
        mqttExecutor.execute(clientToDisconnect::disconnect);
    }

    private String getMqttHost() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        String host = preferences.getString(KEY_MQTT_HOST, DEFAULT_MQTT_HOST);
        if (host == null || host.trim().isEmpty()) {
            return DEFAULT_MQTT_HOST;
        }
        return host.trim();
    }

    private int getMqttPort() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        String portText = preferences.getString(KEY_MQTT_PORT, String.valueOf(DEFAULT_MQTT_PORT));
        try {
            return Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return DEFAULT_MQTT_PORT;
        }
    }

    private MqttSyncClient.Listener createMqttListener() {
        return new MqttSyncClient.Listener() {
            @Override
            public void onCommandReceived(String command) {
                runOnUiThread(() -> handleIncomingCommand(command));
            }

            @Override
            public void onTimeSyncReceived(double targetUnixTime) {
                runOnUiThread(() -> handleIncomingTimeSync(targetUnixTime));
            }

            @Override
            public void onPeerTimingReceived(JSONObject payload) {
                Log.i(TAG, "Peer timing received: " + payload);
                synchronized (peerTimingLock) {
                    latestPeerTimingPayload = cloneJson(payload);
                }
            }

            @Override
            public void onConnectionChanged(boolean connected, String message) {
                Log.i(TAG, "MQTT connection changed: " + connected + ", message=" + message);
                setMqttConnectionState(connected, connected ? "Connected" : "Disconnected");
            }
        };
    }

    private void handleIncomingCommand(String command) {
        if (!ROLE_DEPENDENT.equals(role)) {
            return;
        }
        if (command == null) {
            return;
        }
        if (!COMMAND_PLAYBACK.equals(command) && !COMMAND_RECORD.equals(command) && !COMMAND_STOP.equals(command)) {
            Log.w(TAG, "Ignore unsupported command: " + command);
            return;
        }

        publishAckAsync(command);
        if (COMMAND_STOP.equals(command)) {
            pendingRemoteCommand = null;
            cancelPendingSessionStart();
            if (isStarted) {
                setStartButtonIdle();
                endAllThreads();
            }
            return;
        }

        pendingRemoteCommand = command;
        Log.i(TAG, "Pending remote command: " + pendingRemoteCommand);
    }

    private void handleIncomingTimeSync(double targetUnixTime) {
        if (!ROLE_DEPENDENT.equals(role)) {
            return;
        }
        if (pendingRemoteCommand == null) {
            Log.w(TAG, "time_sync received without pending command.");
            return;
        }

        init();
        if (COMMAND_PLAYBACK.equals(pendingRemoteCommand)) {
            audioTxEnabled = true;
            audioRxEnabled = false;
            sessionOperation = "playback";
        } else if (COMMAND_RECORD.equals(pendingRemoteCommand)) {
            audioTxEnabled = false;
            audioRxEnabled = true;
            sessionOperation = "record";
        } else {
            return;
        }
        sessionControlledByMqtt = true;
        scheduleSessionStartAt(
                (long) (targetUnixTime * 1000),
                targetUnixTime,
                sessionOperation,
                true
        );
        pendingRemoteCommand = null;
    }

    private void publishAckAsync(String command) {
        mqttExecutor.execute(() -> {
            if (mqttSyncClient == null) {
                return;
            }
            mqttSyncClient.publishAck(command, MQTT_PUBLISH_TIMEOUT_MS);
        });
    }

    private void startControllerSynchronizedSession() {
        if (mqttSyncClient == null || !mqttSyncClient.isConnected()) {
            Toast.makeText(this, "MQTT is not connected.", Toast.LENGTH_LONG).show();
            return;
        }
        if (audioTxEnabled == audioRxEnabled) {
            Toast.makeText(this, "Controller mode needs exactly one of Tx/Rx enabled.", Toast.LENGTH_LONG).show();
            return;
        }

        final String remoteCommand = audioTxEnabled ? COMMAND_RECORD : COMMAND_PLAYBACK;
        final String localOperation = audioTxEnabled ? "playback" : "record";
        startEndButton.setEnabled(false);
        mqttExecutor.execute(() -> {
            boolean ackOk = mqttSyncClient.publishCommandAndWaitAck(remoteCommand, MQTT_ACK_TIMEOUT_MS);
            if (!ackOk) {
                runOnUiThread(() -> {
                    startEndButton.setEnabled(true);
                    setStartButtonIdle();
                    Toast.makeText(this, "Ack timeout for command: " + remoteCommand, Toast.LENGTH_LONG).show();
                });
                return;
            }

            double ntpNow = queryNtpTimeWithFallback();
            double targetUnixTime = ntpNow + (MQTT_SYNC_LEAD_TIME_MS / 1000.0);
            boolean publishOk = mqttSyncClient.publishTargetTime(targetUnixTime, MQTT_PUBLISH_TIMEOUT_MS);
            if (!publishOk) {
                runOnUiThread(() -> {
                    startEndButton.setEnabled(true);
                    setStartButtonIdle();
                    Toast.makeText(this, "Failed to publish time_sync.", Toast.LENGTH_LONG).show();
                });
                return;
            }

            runOnUiThread(() -> scheduleSessionStartAt(
                    (long) (targetUnixTime * 1000),
                    targetUnixTime,
                    localOperation,
                    true
            ));
        });
    }

    private void sendRemoteStopCommandAsync() {
        mqttExecutor.execute(() -> {
            if (mqttSyncClient == null || !mqttSyncClient.isConnected()) {
                return;
            }
            mqttSyncClient.publishCommandAndWaitAck(COMMAND_STOP, MQTT_ACK_TIMEOUT_MS);
        });
    }

    private double queryNtpTimeWithFallback() {
        try {
            return NtpTimeClient.queryUnixTimeSeconds("pool.ntp.org", 2000);
        } catch (IOException e) {
            Log.w(TAG, "NTP query failed, fallback to local clock.", e);
            return nowUnixSeconds();
        }
    }

    private void scheduleSessionStartAt(long targetTimeMs, Double targetTimeUnix, String operation, boolean mqttControlled) {
        long delayMs = Math.max(0L, targetTimeMs - System.currentTimeMillis());
        cancelPendingSessionStart();
        cancelPendingAutoStop();
        synchronized (peerTimingLock) {
            latestPeerTimingPayload = null;
        }
        sessionTargetTimeUnix = targetTimeUnix;
        sessionOperation = operation;
        sessionControlledByMqtt = mqttControlled;
        startEndButton.setEnabled(false);
        setStartButtonIdle();
        pendingStartRunnable = () -> {
            pendingStartRunnable = null;
            startEndButton.setEnabled(true);
            setStartButtonRunning();
            sessionActualStartTimeUnix = nowUnixSeconds();
            startAllThreads();
        };
        mainHandler.postDelayed(pendingStartRunnable, delayMs);
    }

    private String deriveOperationForCurrentFlags() {
        if (audioTxEnabled && audioRxEnabled) {
            return "playback+record";
        }
        if (audioTxEnabled) {
            return "playback";
        }
        if (audioRxEnabled) {
            return "record";
        }
        return "idle";
    }

    private double readSessionDurationSeconds(Intent intent, SharedPreferences preferences) {
        if (intent != null && intent.hasExtra("sessionDurationSeconds")) {
            return sanitizeSessionDuration(intent.getDoubleExtra("sessionDurationSeconds", DEFAULT_SESSION_DURATION_SECONDS));
        }
        String saved = preferences.getString(KEY_SESSION_DURATION_SECONDS, String.valueOf(DEFAULT_SESSION_DURATION_SECONDS));
        try {
            return sanitizeSessionDuration(Double.parseDouble(saved));
        } catch (NumberFormatException e) {
            return DEFAULT_SESSION_DURATION_SECONDS;
        }
    }

    private double sanitizeSessionDuration(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_SESSION_DURATION_SECONDS;
        }
        return value;
    }

    private double parseDoubleOrDefault(String raw, double defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String readPlayFileFromPreferences(SharedPreferences preferences) {
        String savedPlayFileName = preferences.getString(KEY_PLAY_FILE_NAME, null);
        if (savedPlayFileName != null && !savedPlayFileName.isEmpty()) {
            return savedPlayFileName;
        }
        return DEFAULT_PLAY_FILE;
    }

    private void setStartButtonRunning() {
        startEndButton.setText("End");
        ViewCompat.setBackgroundTintList(startEndButton, ColorStateList.valueOf(Color.RED));
    }

    private void setStartButtonIdle() {
        startEndButton.setText("Start");
        ViewCompat.setBackgroundTintList(startEndButton, ColorStateList.valueOf(Color.parseColor("#006400")));
    }

    private double nowUnixSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    private void publishTimingForCurrentSession(double endTimeUnix) {
        if (!sessionControlledByMqtt || mqttSyncClient == null || sessionOperation == null) {
            return;
        }
        if (!"playback".equals(sessionOperation)) {
            return;
        }
        JSONObject localTiming = buildTimingPayload(
                sessionOperation,
                sessionTargetTimeUnix,
                sessionActualStartTimeUnix,
                endTimeUnix,
                true,
                "record".equals(sessionOperation) ? audioPath : null
        );
        if (localTiming == null) {
            return;
        }
        String operation = localTiming.optString("operation", sessionOperation);
        Double target = localTiming.optDouble("target_time");
        if (Double.isNaN(target)) {
            target = null;
        }
        Double actualStart = localTiming.optDouble("actual_start_time");
        if (Double.isNaN(actualStart)) {
            actualStart = null;
        }
        String savePath = localTiming.optString("save_path", null);
        if (savePath != null && savePath.isEmpty()) {
            savePath = null;
        }
        final String finalOperation = operation;
        final Double finalTarget = target;
        final Double finalActualStart = actualStart;
        final String finalSavePath = savePath;
        mqttExecutor.execute(() ->
                mqttSyncClient.publishTiming(
                        finalOperation,
                        finalTarget,
                        finalActualStart,
                        endTimeUnix,
                        true,
                        finalSavePath,
                        MQTT_PUBLISH_TIMEOUT_MS
                )
        );
    }

    private void saveRecorderArtifacts(double endTimeUnix) {
        if (!audioRxEnabled || audioPath == null || audioPath.isEmpty()) {
            return;
        }
        String recordingSavePath = resolveRecordingSavePath(audioPath);
        saveConfigSnapshot(recordingSavePath);

        JSONObject localRecordTiming = buildTimingPayload(
                "record",
                sessionTargetTimeUnix,
                sessionActualStartTimeUnix,
                endTimeUnix,
                true,
                recordingSavePath
        );
        JSONObject localPlaybackTiming = null;
        if (audioTxEnabled) {
            localPlaybackTiming = buildTimingPayload(
                    "playback",
                    sessionTargetTimeUnix,
                    sessionActualStartTimeUnix,
                    endTimeUnix,
                    true,
                    null
            );
        }

        if (sessionControlledByMqtt) {
            JSONObject finalLocalPlaybackTiming = localPlaybackTiming;
            mqttExecutor.execute(() -> {
                JSONObject peerTiming = waitForPeerTimingPayload(MQTT_TIMING_TIMEOUT_MS);
                saveTimingMetadata(localRecordTiming, finalLocalPlaybackTiming, peerTiming);
            });
            return;
        }
        saveTimingMetadata(localRecordTiming, localPlaybackTiming, getLatestPeerTimingCopy());
    }

    private JSONObject buildTimingPayload(
            String operation,
            Double targetTime,
            Double actualStartTime,
            Double actualEndTime,
            boolean success,
            String savePath
    ) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("source", role);
            payload.put("operation", operation);
            payload.put("target_time", targetTime == null ? JSONObject.NULL : toJsonPlainNumber(targetTime));
            payload.put("actual_start_time", actualStartTime == null ? JSONObject.NULL : toJsonPlainNumber(actualStartTime));
            payload.put("actual_end_time", actualEndTime == null ? JSONObject.NULL : toJsonPlainNumber(actualEndTime));
            payload.put("success", success);
            payload.put("save_path", savePath == null ? JSONObject.NULL : savePath);
            payload.put("published_at", toJsonPlainNumber(nowUnixSeconds()));
            return payload;
        } catch (Exception e) {
            Log.e(TAG, "buildTimingPayload failed", e);
            return null;
        }
    }

    private void saveTimingMetadata(JSONObject localRecordTiming, JSONObject localPlaybackTiming, JSONObject peerTiming) {
        JSONObject recordingMetadata = localRecordTiming != null ? cloneJson(localRecordTiming) : null;
        JSONObject playbackMetadata = localPlaybackTiming != null ? cloneJson(localPlaybackTiming) : null;
        if (peerTiming != null) {
            String op = peerTiming.optString("operation", "");
            if ("record".equals(op) && recordingMetadata == null) {
                recordingMetadata = cloneJson(peerTiming);
            } else if ("playback".equals(op) && playbackMetadata == null) {
                playbackMetadata = cloneJson(peerTiming);
            }
        }
        if (recordingMetadata == null) {
            Log.w(TAG, "Timing metadata was not saved: recording metadata is missing.");
            return;
        }
        normalizeTimingPayloadNumbers(recordingMetadata);
        normalizeTimingPayloadNumbers(playbackMetadata);

        String savePath = recordingMetadata.optString("save_path", "");
        if (savePath.isEmpty()) {
            Log.w(TAG, "Timing metadata was not saved: no save_path in recording metadata.");
            return;
        }
        File parentDir = new File(savePath).getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            Log.w(TAG, "Timing metadata was not saved: invalid parent dir for " + savePath);
            return;
        }

        File outputFile = new File(parentDir, "timing.json");
        try {
            JSONObject payload = new JSONObject();
            payload.put("recording_metadata", recordingMetadata);
            payload.put("playback_metadata", playbackMetadata == null ? JSONObject.NULL : playbackMetadata);
            payload.put("saved_at_unix", toJsonPlainNumber(nowUnixSeconds()));

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
                writer.write(toReadableJson(payload));
            }
            Log.i(TAG, "Timing metadata saved to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed saving timing metadata", e);
        }
    }

    private JSONObject waitForPeerTimingPayload(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() <= deadline) {
            JSONObject peer = getLatestPeerTimingCopy();
            if (peer != null) {
                return peer;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    private JSONObject getLatestPeerTimingCopy() {
        synchronized (peerTimingLock) {
            return cloneJson(latestPeerTimingPayload);
        }
    }

    private JSONObject cloneJson(JSONObject payload) {
        if (payload == null) {
            return null;
        }
        try {
            return new JSONObject(payload.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveRecordingSavePath(String rawRecordingPath) {
        if (rawRecordingPath == null || rawRecordingPath.isEmpty()) {
            return rawRecordingPath;
        }
        if (rawRecordingPath.endsWith(".pcm")) {
            return rawRecordingPath.substring(0, rawRecordingPath.length() - 4) + ".wav";
        }
        return rawRecordingPath;
    }

    private BigDecimal toJsonPlainNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private String toPlainUnixString(Double value) {
        if (value == null) {
            return "null";
        }
        BigDecimal normalized = toJsonPlainNumber(value);
        return normalized == null ? String.valueOf(value) : normalized.toPlainString();
    }

    private void normalizeTimingPayloadNumbers(JSONObject payload) {
        if (payload == null) {
            return;
        }
        normalizeTimingField(payload, "target_time");
        normalizeTimingField(payload, "actual_start_time");
        normalizeTimingField(payload, "actual_end_time");
        normalizeTimingField(payload, "published_at");
    }

    private void normalizeTimingField(JSONObject payload, String key) {
        if (!payload.has(key) || payload.isNull(key)) {
            return;
        }
        BigDecimal normalized = toPlainNumber(payload.opt(key));
        if (normalized == null) {
            return;
        }
        try {
            payload.put(key, normalized);
        } catch (Exception e) {
            Log.w(TAG, "Failed to normalize timing field: " + key, e);
        }
    }

    private BigDecimal toPlainNumber(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                if (value instanceof Double || value instanceof Float) {
                    double asDouble = ((Number) value).doubleValue();
                    if (Double.isNaN(asDouble) || Double.isInfinite(asDouble)) {
                        return null;
                    }
                    return BigDecimal.valueOf(asDouble);
                }
                return new BigDecimal(value.toString());
            }
            if (value instanceof String) {
                String raw = ((String) value).trim();
                if (raw.isEmpty()) {
                    return null;
                }
                return new BigDecimal(raw);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Cannot parse numeric timing value: " + value, e);
        }
        return null;
    }

    private String toReadableJson(JSONObject payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return payload.toString(2).replace("\\/", "/");
        } catch (Exception e) {
            Log.w(TAG, "Failed to pretty-print JSON payload.", e);
            return payload.toString().replace("\\/", "/");
        }
    }

    private void saveConfigSnapshot(String recordingSavePath) {
        File parentDir = new File(recordingSavePath).getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            return;
        }
        File configFile = new File(parentDir, "config.yaml");
        String yaml = buildConfigYamlSnapshot();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile, false))) {
            writer.write(yaml);
            Log.i(TAG, "Config snapshot saved to: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed saving config snapshot", e);
        }
    }

    private String buildConfigYamlSnapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append("broker:\n");
        sb.append("  host: ").append(yamlQuoted(getMqttHost())).append('\n');
        sb.append("  port: ").append(getMqttPort()).append('\n');
        sb.append("devices:\n");
        sb.append("  input_id: ").append(yamlQuoted(selectedMicrophoneId)).append('\n');
        sb.append("  output_id: ").append(yamlQuoted(selectedSpeakerId)).append('\n');
        sb.append("recording:\n");
        sb.append("  sample_rate: 48000\n");
        sb.append("  channels: 1\n");
        sb.append("  bits_per_sample: 16\n");
        sb.append("  total_duration_seconds: ").append(sessionDurationSeconds).append('\n');
        sb.append("experiment:\n");
        sb.append("  name: ").append(yamlQuoted(experimentName)).append('\n');
        sb.append("  role: ").append(yamlQuoted(role)).append('\n');
        sb.append("  play_file: ").append(yamlQuoted(playFile)).append('\n');
        sb.append("session:\n");
        sb.append("  audio_tx_enabled: ").append(audioTxEnabled).append('\n');
        sb.append("  audio_rx_enabled: ").append(audioRxEnabled).append('\n');
        sb.append("  video_enabled: ").append(videoEnabled).append('\n');
        sb.append("  play_from_file_enabled: ").append(playFromFileEnabled).append('\n');
        sb.append("timing:\n");
        if (sessionTargetTimeUnix == null) {
            sb.append("  target_time: null\n");
        } else {
            sb.append("  target_time: ").append(toPlainUnixString(sessionTargetTimeUnix)).append('\n');
        }
        if (sessionActualStartTimeUnix == null) {
            sb.append("  actual_start_time: null\n");
        } else {
            sb.append("  actual_start_time: ").append(toPlainUnixString(sessionActualStartTimeUnix)).append('\n');
        }
        sb.append("  operation: ").append(yamlQuoted(sessionOperation)).append('\n');
        sb.append("  controlled_by_mqtt: ").append(sessionControlledByMqtt).append('\n');
        return sb.toString();
    }

    private String yamlQuoted(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void clearSessionTracking() {
        sessionTargetTimeUnix = null;
        sessionActualStartTimeUnix = null;
        sessionOperation = null;
        sessionControlledByMqtt = false;
    }

    private void scheduleAutoStopIfNeeded() {
        cancelPendingAutoStop();
        if (sessionDurationSeconds <= 0 || !isStarted) {
            return;
        }
        long durationMs = Math.max(1L, (long) (sessionDurationSeconds * 1000L));
        pendingAutoStopRunnable = () -> {
            pendingAutoStopRunnable = null;
            if (!isStarted) {
                return;
            }
            Log.i(TAG, "Auto stop session after " + sessionDurationSeconds + " seconds.");
            if (ROLE_CONTROLLER.equals(role) && sessionControlledByMqtt) {
                sendRemoteStopCommandAsync();
            }
            setStartButtonIdle();
            endAllThreads();
        };
        mainHandler.postDelayed(pendingAutoStopRunnable, durationMs);
    }

    private void cancelPendingAutoStop() {
        if (pendingAutoStopRunnable != null) {
            mainHandler.removeCallbacks(pendingAutoStopRunnable);
            pendingAutoStopRunnable = null;
        }
    }

    private void cancelPendingSessionStart() {
        if (pendingStartRunnable != null) {
            mainHandler.removeCallbacks(pendingStartRunnable);
            pendingStartRunnable = null;
        }
    }

    private void startAllThreads() {
        if (!arePermissionsGranted()) {
            requestPermissions();
            setStartButtonIdle();
            clearSessionTracking();
            refreshControlButtons();
            return;
        }

        if (audioTxEnabled && playFile != null) {
            audioFilePath = getAudioFilePathFromAssets(playFile);
            Log.d(TAG, "playFile: " + playFile);
        } else {
            audioFilePath = null;
        }
        experimentName = wrapUpExpName(experimentName, playFile); // add play file info in exp name

        sessionFolder = fileNameManager.createNewSessionFolder(MainActivity.this, experimentName);
        this.audioPath = fileNameManager.generateAudioFileName(sessionFolder);
        this.videoPath = fileNameManager.generateVideoFileName(sessionFolder);
        Log.d(TAG, "videoPath: " + videoPath);


        // Show the SensorDataFragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            // Only add the fragment if it is not already added
            SensorDataFragment sensorDataFragment = SensorDataFragment.newInstance(sessionFolder.getAbsolutePath());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, sensorDataFragment);
            transaction.commit();
            getSupportFragmentManager().executePendingTransactions();  // Ensure transaction is committed
            // Make the fragment container visible
            findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
//            findViewById(R.id.instructions).setVisibility(View.GONE);
        }

        int taskCount = 0;
        if (videoEnabled) {
            taskCount++;
        }
        if (audioTxEnabled && audioFilePath != null) {
            taskCount++;
        }
        if (audioRxEnabled) {
            taskCount++;
        }
        if (taskCount == 0) {
            Log.w(TAG, "No enabled stream for role=" + role + ". Nothing to start.");
            setStartButtonIdle();
            clearSessionTracking();
            return;
        }
        isStarted = true;

        final CountDownLatch latch = new CountDownLatch(taskCount);

        if (videoEnabled) {
            // Start video recording thread
            new Thread(() -> {
                try {
                    latch.await(); // Wait until the count reaches zero
                    cameraController.startRecording(videoPath);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        if (audioTxEnabled && audioFilePath != null) {
            // Start audio playback thread
            new Thread(() -> {
                try {
                    latch.await(); // Wait until the count reaches zero
                    audioPlayerHelper.startAudioPlayback(audioFilePath, signalLoopEnabled, signalIdleBetweenLoopsSeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        if (audioRxEnabled) {
            // Start audio recording thread
            new Thread(() -> {
                try {
                    latch.await(); // Wait until the count reaches zero
                    audioRecorderHelper.startAudioRecording(this, audioPath);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }



        // Countdown latch for all threads
        for (int idx = 0; idx < taskCount; idx++) {
            latch.countDown();
        }
        scheduleAutoStopIfNeeded();

////        cameraHelper.startVideoRecording(getExternalFilesDir(null).getPath() + "/videoRecording.mp4");
//        // Start audio playback thread
//        new Thread(() -> audioPlayerHelper.startAudioPlayback(audioFilePath)).start();
//
//        // Start audio recording thread
//        new Thread(() -> audioRecorderHelper.startAudioRecording(this, audioPath)).start();
//
//        // Start video recording thread
//        cameraHelper.startVideoRecording(videoPath);
    }

    private void endAllThreads(){
        isStarted = false;
        cancelPendingAutoStop();
        double endTimeUnix = nowUnixSeconds();


        if (videoEnabled) {
            cameraController.stopRecording(String.valueOf(fileNameManager.getIdx()));
        }
        if (audioTxEnabled) {
            audioPlayerHelper.stopAudioPlayback();
        }
        if (audioRxEnabled) {
            audioRecorderHelper.stopAudioRecording();
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(fragment);
            transaction.commit();

            // Hide the fragment container
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
//            findViewById(R.id.instructions).setVisibility(View.VISIBLE);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sessionFolder != null) {
                    saveRecordMetaData();
                }
            }
        }, 1000);

        publishTimingForCurrentSession(endTimeUnix);
        saveRecorderArtifacts(endTimeUnix);
        clearSessionTracking();
        refreshControlButtons();


    }

    private void saveRecordMetaData(){
        File metadata = new File(sessionFolder, "meta_data.csv");
        String videoStartTime = cameraController.getVideoRecordStartTime();
        String videoEndTime = cameraController.getVideoRecordEndTime();
        String audioStartTime = audioRecorderHelper.getAudioRecordStartTime();
        String audioEndTime = audioRecorderHelper.getAudioRecordEndTime();
        String audioPlayPath = playFile;

        FileWriter writer = null;
        try {
            // Initialize FileWriter with the metadata file
            writer = new FileWriter(metadata, true); // 'true' to append to the file

            // Write header if the file is empty
            if (metadata.length() == 0) {
                writer.write("VideoStartTime,VideoEndTime,AudioStartTime,AudioEndTime,AudioPlayPath\n");
            }

            // Write the metadata values to the file
            writer.write(videoStartTime + "," + videoEndTime + "," + audioStartTime + "," + audioEndTime + "," + audioPlayPath + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the FileWriter
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showLogFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new LogFragment());
        fragmentTransaction.addToBackStack(null); // This will allow you to return to the main page
        fragmentTransaction.commit();

        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        findViewById(R.id.instructions).sendAccessibilityEvent(View.INVISIBLE);
        buttonLog.setVisibility(View.GONE); // Hide the button when the log fragment is shown
    }
    // Show Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuSettings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            settingsLauncher.launch(intent);
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean arePermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0) {
                boolean cameraPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean audioPermissionGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean writeStoragePermissionGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean readStoragePermissionGranted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean allGranted = cameraPermissionGranted
                        && audioPermissionGranted
                        && writeStoragePermissionGranted
                        && readStoragePermissionGranted;

//                if (cameraPermissionGranted && audioPermissionGranted && writeStoragePermissionGranted && readStoragePermissionGranted){
                if (cameraPermissionGranted && audioPermissionGranted){
//                    cameraHelper.startCamera();
//                    cameraHelper.openCamera();
//                    cameraHelper.startBackgroundThread();
//                    cameraHelper.openCamera();

//                    textureView.setSurfaceTextureListener(textureListener);
//                    cameraHelper.startCamera();
                    Log.d("PermissionGranted", "Read and Audio Permissions are granted.");
                } else {
                    Log.d("PermissionDenied", "cameraPermissionGranted: " + cameraPermissionGranted +
                            ", audioPermissionGranted: " + audioPermissionGranted
//                            ", writeStoragePermissionGranted: " +
//                            writeStoragePermissionGranted + ", readStoragePermissionGranted: " + readStoragePermissionGranted
                    );
                    Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show();
                }

                if (writeStoragePermissionGranted && readStoragePermissionGranted){
                    Toast.makeText(this, "Read and write permissions are given", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("PermissionDenied", "writeStoragePermissionGranted: " +
                            writeStoragePermissionGranted + ", readStoragePermissionGranted: " + readStoragePermissionGranted );
                }

                if (pendingConnectAfterPermission) {
                    pendingConnectAfterPermission = false;
                    if (allGranted) {
                        configureMqttForRole();
                    } else {
                        Toast.makeText(this, "Permissions are required before connecting MQTT.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }


//    private void preprocessData(String audioPath, String videoPath) throws IOException {
//        Toast.makeText(this, "Processing your data...", Toast.LENGTH_LONG).show();
//        Module module = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "model.ptl"));
//
//        Log.d(TAG, "Entering processing audio");
//        // Example input processing
//        PreProcessAudio.ExtractSignalOffset preprocessedAudio = preProcessAudio.processAudio(audioPath, 1.5, 1);
//        Log.d(TAG, "preprocessed audio: " + preprocessedAudio.extractedSignal.length);
//
//
//        int[][][][] videoFrames = preProcessVideo.extractFrames(videoPath, 0, new org.opencv.core.Size(72, 72));
//
//        Log.d(TAG, "videoFrames size: " + videoFrames.length);
//
//        double startOffset = (double) preprocessedAudio.startIndex / AUDIO_FS;
//        int startVideoOffset = (int) (startOffset * VIDEO_FPS);
//        videoFrames = sliceVideoFrames(videoFrames, startVideoOffset);
//        Log.d(TAG, "videoFrames size: (after slice) " + videoFrames.length);
//
//        int tMax = Math.min(preprocessedAudio.extractedSignal.length / AUDIO_FS, videoFrames.length / VIDEO_FPS);
//        Log.d(TAG, "tMax = " + tMax);
//
//        for (int tdx = 0; tdx < tMax - WIN_LEN; tdx += WIN_STEP) {
//            double[] audioWindow = sliceAudioWindow(preprocessedAudio.extractedSignal, tdx, AUDIO_FS);
//            int[][][][] videoWindow = sliceVideoWindow(videoFrames, tdx);
//
//            Log.d(TAG, "audio window length: " + audioWindow.length);
//            Log.d(TAG, "video window length: " + videoWindow.length);
//
//            // Assume preprocessDoppler is a method that returns a 2D array representing the doppler spectrum
//            PreProcessAudio.DopplerArrayResult dopplerArrayResult = preProcessAudio.processDoppler(audioWindow, AUDIO_FS, 8160, 512, 8192, 50);
//
//            double[][][] doppler_spectrums = dopplerArrayResult.filteredMagnitudesArray;
//            double[] predict_results = new double[doppler_spectrums.length];
//            for (int sdx = 0; sdx < doppler_spectrums.length; sdx++) {
//                double[][] resizedSpectrum = resizeSpectrum(doppler_spectrums[sdx], new org.opencv.core.Size(72, 72));
//                // input: videoWindow, resizedSpectrum
//                Log.d(TAG, "resizedSpectrum shape: "+ resizedSpectrum.length + "," + resizedSpectrum[0].length);
//
//            }
//        }
//    }
//
//    private int[][][][] sliceVideoFrames(int[][][][] videoFrames, int startOffset) {
//        int length = videoFrames.length - startOffset;
//        int[][][][] slicedFrames = new int[length][][][];
//        System.arraycopy(videoFrames, startOffset, slicedFrames, 0, length);
//        return slicedFrames;
//    }
//
//    private double[] sliceAudioWindow(double[] syncRecord, int t, int sampleRate) {
//        int start = t * sampleRate;
//        int end = (t + WIN_LEN) * sampleRate;
//        return Arrays.copyOfRange(syncRecord, start, end);
//    }
//
//    private int[][][][] sliceVideoWindow(int[][][][] videoFrames, int t) {
//        int start = t * VIDEO_FPS;
//        int end = (t + WIN_LEN) * VIDEO_FPS;
//        return Arrays.copyOfRange(videoFrames, start, end);
//    }
//
//    private double[][] resizeSpectrum(double[][] spectrum, org.opencv.core.Size size) {
//        int originalHeight = spectrum.length;
//        int originalWidth = spectrum[0].length;
//        Mat spectrumMat = new Mat(originalHeight, originalWidth, CvType.CV_64F);
//        for (int i = 0; i < originalHeight; i++) {
//            spectrumMat.put(i, 0, spectrum[i]);
//        }
//
//        Mat resizedMat = new Mat();
//        Imgproc.resize(spectrumMat, resizedMat, size, 0, 0, Imgproc.INTER_LINEAR);
//
//        int newHeight = (int) size.height;
//        int newWidth = (int) size.width;
//        double[][] resizedArray = new double[newHeight][newWidth];
//        for (int y = 0; y < newHeight; y++) {
//            for (int x = 0; x < newWidth; x++) {
//                resizedArray[y][x] = resizedMat.get(y, x)[0];
//            }
//        }
//        return resizedArray;
//    }



    private String getAudioFilePathFromAssets(String assetFileName) {
        File file = new File(getExternalFilesDir(null), assetFileName);
        try (InputStream inputStream = getAssets().open(assetFileName);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("FILE_PATH", file.getPath());
        return file.getPath();
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private String wrapUpExpName(String experimentName, String playFile) {
        if (playFile == null) {
            return experimentName;
        }
        String newExperimentName = experimentName;
        Log.d(TAG, "playFile: " + playFile + " ,experimentName: " + experimentName);
        String playFileName = playFile.split("\\.")[0];
        if (playFileName.startsWith("pure_tone")) {
            String frequency = playFileName.split("_")[2];
            frequency = frequency.substring(0, frequency.length()-2);
            newExperimentName += String.format("PT-F-%s", frequency);
        }
        return newExperimentName;
    }

//    private void startCountdown(int seconds) {
//        overlay.setAlpha(0f);
//        overlay.setVisibility(View.VISIBLE);
//        overlay.animate().alpha(1f).setDuration(300).start();
//
//        countdownText.setAlpha(0f);
//        countdownText.setVisibility(View.VISIBLE);
//        countdownText.animate().alpha(1f).setDuration(300).start();
//
//        new CountDownTimer(seconds * 1000, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                countdownText.setText(String.valueOf(millisUntilFinished / 1000));
//            }
//
//            @Override
//            public void onFinish() {
//                overlay.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        overlay.setVisibility(View.GONE);
//                    }
//                }).start();
//
//                countdownText.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        countdownText.setVisibility(View.GONE);
//                    }
//                }).start();
//            }
//        }.start();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoleFromPreferences();
        applyRoleConnectionUiState();
        cameraController.resumeCamera();
    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        cameraHelper.closeCamera();
//        audioRecorderHelper.stopAudioRecording();
//        audioPlayerHelper.stopAudioPlayback();
//    }
//
    @Override
    public void onStop(){
        super.onStop();
        cameraController.pauseCamera();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        cameraController.destroyCamera();
        disconnectMqttClient();
        mqttExecutor.shutdownNow();
    }





}
