package com.msra.avliveness.activities;
import android.content.res.AssetManager;
import android.os.Bundle;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.view.View;


import com.msra.avliveness.utils.DeviceInfo;
import com.msra.avliveness.utils.DeviceQuery;
import com.msra.avliveness.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String ROLE_STANDALONE = "standalone";
    private static final String ROLE_CONTROLLER = "controller";
    private static final String ROLE_DEPENDENT = "dependent";
    private static final String SETTINGS_NAME = "Settings";
    private static final String KEY_ROLE = "Role";
    private static final String KEY_AUDIO_TX_ENABLED = "AudioTxEnabled";
    private static final String KEY_AUDIO_RX_ENABLED = "AudioRxEnabled";
    private static final String KEY_MQTT_HOST = "MqttHost";
    private static final String KEY_MQTT_PORT = "MqttPort";
    private static final String KEY_SESSION_DURATION_SECONDS = "SessionDurationSeconds";
    private static final String KEY_SIGNAL_LOOP = "SignalLoop";
    private static final String KEY_SIGNAL_IDLE_BETWEEN_SECONDS = "SignalIdleBetweenSeconds";
    private static final String KEY_PLAY_FILE_NAME = "PlayFileName";
    private static final String DEFAULT_MQTT_HOST = "localhost";
    private static final String DEFAULT_MQTT_PORT = "1883";
    private static final String DEFAULT_SESSION_DURATION_SECONDS = "10";
    private static final String DEFAULT_SIGNAL_IDLE_SECONDS = "1.0";
    private static final String DEFAULT_PLAY_FILE = "logsweep.wav";
    private static final String KEY_CAMERA_CHOICE = "CameraChoice";
    private static final String KEY_SPEAKER_CHOICE = "SpeakerChoice";
    private static final String KEY_MICROPHONE_CHOICE = "MicrophoneChoice";

    private CheckBox checkBoxAudioTx, checkBoxAudioRx, checkBoxVideo, checkBoxPlayFromFile, checkBoxSignalLoop;
    private Spinner spinnerCamera, spinnerSpeaker, spinnerMicrophone, spinnerPlayFile, spinnerRole;
    private EditText editTextExperimentName, editTextMqttHost, editTextMqttPort, editTextSessionDuration, editTextSignalIdleSeconds;

    private Button btnSave;

    private DeviceQuery deviceQuery;

    private List<String> fileNames = new ArrayList<>();
    private boolean isProgrammaticUpdate = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize UI components
        checkBoxAudioTx = findViewById(R.id.checkBoxAudioTx);
        checkBoxAudioRx = findViewById(R.id.checkBoxAudioRx);
        checkBoxVideo = findViewById(R.id.checkBoxVideo);
        checkBoxPlayFromFile = findViewById(R.id.checkBoxPlayFromFile);
        checkBoxSignalLoop = findViewById(R.id.checkBoxSignalLoop);
        spinnerCamera = findViewById(R.id.spinnerCamera);
        spinnerSpeaker = findViewById(R.id.spinnerSpeaker);
        spinnerMicrophone = findViewById(R.id.spinnerMicrophone);
        spinnerPlayFile = findViewById(R.id.spinnerPlay);
        spinnerRole = findViewById(R.id.spinnerRole);
        editTextExperimentName = findViewById(R.id.editTextExperimentName);
        editTextMqttHost = findViewById(R.id.editTextMqttHost);
        editTextMqttPort = findViewById(R.id.editTextMqttPort);
        editTextSessionDuration = findViewById(R.id.editTextSessionDuration);
        editTextSignalIdleSeconds = findViewById(R.id.editTextSignalIdleSeconds);
        btnSave = findViewById(R.id.btnSave);



        deviceQuery = new DeviceQuery(this);



        // Set up spinners with dummy data
        populateSpinners();
        setupRoleAndAudioBehavior();

        // Load saved preferences
        loadPreferences();

        // Save button listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                setResult(RESULT_OK);
                finish(); // Close the settings activity and return to main activity
            }
        });
    }

    private void populateSpinners() {
        // Populate Camera Spinner
        List<DeviceInfo> cameras = deviceQuery.getAvailableCameras();
        ArrayAdapter<DeviceInfo> cameraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cameras);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCamera.setAdapter(cameraAdapter);

        // Populate Speaker Spinner
        List<DeviceInfo> speakers = deviceQuery.getAvailableSpeakers();
        ArrayAdapter<DeviceInfo> speakerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, speakers);
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeaker.setAdapter(speakerAdapter);

        // Populate Microphone Spinner
        List<DeviceInfo> microphones = deviceQuery.getAvailableMicrophones();
        ArrayAdapter<DeviceInfo> microphoneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, microphones);
        microphoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMicrophone.setAdapter(microphoneAdapter);

        List<String> roles = new ArrayList<>();
        roles.add(ROLE_STANDALONE);
        roles.add(ROLE_CONTROLLER);
        roles.add(ROLE_DEPENDENT);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        AssetManager assetManager = getAssets();
        try {
            String[] files = assetManager.list("");
            if (files != null) {

                for (String file : files) {
                    if (file.endsWith(".wav")){
                        fileNames.add(file);
                    }

                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fileNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPlayFile.setAdapter(adapter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // populate play list spinner
    }

    private void setupRoleAndAudioBehavior() {
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);
                applyRoleSelection(selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        checkBoxAudioTx.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticUpdate) {
                return;
            }
            if (ROLE_CONTROLLER.equals(getSelectedRole()) && isChecked) {
                withProgrammaticUpdate(() -> checkBoxAudioRx.setChecked(false));
            }
        });

        checkBoxAudioRx.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticUpdate) {
                return;
            }
            if (ROLE_CONTROLLER.equals(getSelectedRole()) && isChecked) {
                withProgrammaticUpdate(() -> checkBoxAudioTx.setChecked(false));
            }
        });
    }

    private void withProgrammaticUpdate(Runnable action) {
        isProgrammaticUpdate = true;
        try {
            action.run();
        } finally {
            isProgrammaticUpdate = false;
        }
    }

    private String getSelectedRole() {
        Object selected = spinnerRole.getSelectedItem();
        return selected == null ? ROLE_CONTROLLER : selected.toString();
    }

    private void applyRoleSelection(String role) {
        final String selectedRole = role == null ? ROLE_CONTROLLER : role;
        withProgrammaticUpdate(() -> {
            if (ROLE_STANDALONE.equals(selectedRole)) {
                checkBoxAudioTx.setEnabled(false);
                checkBoxAudioRx.setEnabled(false);
                checkBoxAudioTx.setChecked(true);
                checkBoxAudioRx.setChecked(true);
                return;
            }
            if (ROLE_DEPENDENT.equals(selectedRole)) {
                checkBoxAudioTx.setEnabled(false);
                checkBoxAudioRx.setEnabled(false);
                checkBoxAudioTx.setChecked(false);
                checkBoxAudioRx.setChecked(false);
                return;
            }

            checkBoxAudioTx.setEnabled(true);
            checkBoxAudioRx.setEnabled(true);
            if (checkBoxAudioTx.isChecked() && checkBoxAudioRx.isChecked()) {
                checkBoxAudioRx.setChecked(false);
            }
            if (!checkBoxAudioTx.isChecked() && !checkBoxAudioRx.isChecked()) {
                checkBoxAudioTx.setChecked(true);
            }
        });
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        boolean legacyAudioEnabled = preferences.getBoolean("Audio", false);
        checkBoxAudioTx.setChecked(preferences.getBoolean(KEY_AUDIO_TX_ENABLED, legacyAudioEnabled));
        checkBoxAudioRx.setChecked(preferences.getBoolean(KEY_AUDIO_RX_ENABLED, legacyAudioEnabled));
        checkBoxVideo.setChecked(preferences.getBoolean("Video", false));
        checkBoxPlayFromFile.setChecked(preferences.getBoolean("PlayFromFile", false));
        checkBoxSignalLoop.setChecked(preferences.getBoolean(KEY_SIGNAL_LOOP, true));
        editTextExperimentName.setText(preferences.getString("ExperimentName", ""));
        editTextMqttHost.setText(preferences.getString(KEY_MQTT_HOST, DEFAULT_MQTT_HOST));
        editTextMqttPort.setText(preferences.getString(KEY_MQTT_PORT, DEFAULT_MQTT_PORT));
        editTextSessionDuration.setText(preferences.getString(KEY_SESSION_DURATION_SECONDS, DEFAULT_SESSION_DURATION_SECONDS));
        editTextSignalIdleSeconds.setText(preferences.getString(KEY_SIGNAL_IDLE_BETWEEN_SECONDS, DEFAULT_SIGNAL_IDLE_SECONDS));

        String savedPlayFileName = preferences.getString(KEY_PLAY_FILE_NAME, null);
        int playFileIndex = resolvePlayFileIndex(savedPlayFileName);
        if (playFileIndex < 0) {
            playFileIndex = fileNames.indexOf(DEFAULT_PLAY_FILE);
        }
        if (playFileIndex < 0) {
            playFileIndex = 0;
        }
        if (spinnerPlayFile.getCount() > 0) {
            spinnerPlayFile.setSelection(Math.max(0, Math.min(playFileIndex, spinnerPlayFile.getCount() - 1)));
        }

        spinnerCamera.setSelection(getSavedIndex(preferences, KEY_CAMERA_CHOICE, spinnerCamera.getCount()));
        spinnerSpeaker.setSelection(getSavedIndex(preferences, KEY_SPEAKER_CHOICE, spinnerSpeaker.getCount()));
        spinnerMicrophone.setSelection(getSavedIndex(preferences, KEY_MICROPHONE_CHOICE, spinnerMicrophone.getCount()));
        String selectedRole = preferences.getString(KEY_ROLE, ROLE_CONTROLLER);
        ArrayAdapter<String> roleAdapter = (ArrayAdapter<String>) spinnerRole.getAdapter();
        int selectedRoleIndex = roleAdapter.getPosition(selectedRole);
        if (selectedRoleIndex < 0) {
            selectedRoleIndex = 0;
        }
        spinnerRole.setSelection(selectedRoleIndex);
        applyRoleSelection(selectedRole);
        // Add logic to load spinners values if needed
    }

    private void savePreferences() {
        String selectedRole = getSelectedRole();
        applyRoleSelection(selectedRole);

        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_AUDIO_TX_ENABLED, checkBoxAudioTx.isChecked());
        editor.putBoolean(KEY_AUDIO_RX_ENABLED, checkBoxAudioRx.isChecked());
        editor.putBoolean("Audio", checkBoxAudioTx.isChecked() || checkBoxAudioRx.isChecked());
        editor.putBoolean("Video", checkBoxVideo.isChecked());
        editor.putBoolean("PlayFromFile", checkBoxPlayFromFile.isChecked());
        editor.putBoolean(KEY_SIGNAL_LOOP, checkBoxSignalLoop.isChecked());
        editor.putString("ExperimentName", editTextExperimentName.getText().toString());
        String mqttHost = editTextMqttHost.getText().toString().trim();
        String mqttPort = editTextMqttPort.getText().toString().trim();
        String sessionDurationText = normalizeSessionDurationInput(
                editTextSessionDuration.getText().toString().trim()
        );
        String idleSecondsText = normalizeIdleSecondsInput(
                editTextSignalIdleSeconds.getText().toString().trim()
        );
        editor.putString(KEY_MQTT_HOST, mqttHost.isEmpty() ? DEFAULT_MQTT_HOST : mqttHost);
        editor.putString(KEY_MQTT_PORT, mqttPort.isEmpty() ? DEFAULT_MQTT_PORT : mqttPort);
        editor.putString(KEY_SESSION_DURATION_SECONDS, sessionDurationText);
        editor.putString(KEY_SIGNAL_IDLE_BETWEEN_SECONDS, idleSecondsText);
        String selectedPlayFile = spinnerPlayFile.getSelectedItem() == null
                ? null
                : spinnerPlayFile.getSelectedItem().toString();
        if (selectedPlayFile == null || selectedPlayFile.trim().isEmpty()) {
            editor.remove(KEY_PLAY_FILE_NAME);
        } else {
            editor.putString(KEY_PLAY_FILE_NAME, selectedPlayFile.trim());
        }
        editor.putInt(KEY_CAMERA_CHOICE, spinnerCamera.getSelectedItemPosition());
        editor.putInt(KEY_SPEAKER_CHOICE, spinnerSpeaker.getSelectedItemPosition());
        editor.putInt(KEY_MICROPHONE_CHOICE, spinnerMicrophone.getSelectedItemPosition());
        editor.putString(KEY_ROLE, selectedRole);
        // Add logic to save spinners values if needed
        editor.apply();
    }

    private int resolvePlayFileIndex(String savedPlayFileName) {
        if (savedPlayFileName != null && !savedPlayFileName.trim().isEmpty()) {
            int indexByName = fileNames.indexOf(savedPlayFileName.trim());
            if (indexByName >= 0) {
                return indexByName;
            }
        }
        return -1;
    }

    private int getSavedIndex(SharedPreferences preferences, String key, int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        int saved = preferences.getInt(key, 0);
        return Math.max(0, Math.min(saved, itemCount - 1));
    }

    private String normalizeSessionDurationInput(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return DEFAULT_SESSION_DURATION_SECONDS;
        }
        try {
            Double.parseDouble(rawValue);
            return rawValue;
        } catch (NumberFormatException e) {
            return DEFAULT_SESSION_DURATION_SECONDS;
        }
    }

    private String normalizeIdleSecondsInput(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return DEFAULT_SIGNAL_IDLE_SECONDS;
        }
        try {
            double value = Double.parseDouble(rawValue);
            return value < 0 ? DEFAULT_SIGNAL_IDLE_SECONDS : rawValue;
        } catch (NumberFormatException e) {
            return DEFAULT_SIGNAL_IDLE_SECONDS;
        }
    }




}
