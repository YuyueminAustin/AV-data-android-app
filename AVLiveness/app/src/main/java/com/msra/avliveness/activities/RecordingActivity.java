//package com.msra.avliveness.activities;
//
//import android.os.Bundle;
//import android.webkit.WebView;
//import android.widget.Button;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;
//
//import com.msra.avliveness.R;
//import com.msra.avliveness.fragments.VideoPreviewFragment;
//import com.msra.avliveness.services.AudioRecorderHelper;
//import com.msra.avliveness.services.CameraHelper;
//
//public class RecordingActivity extends AppCompatActivity {
//
//    private boolean isRecording = false;
//    private Button buttonStartEnd;
//    private Button buttonReturn;
//    private CameraHelper cameraHelper;
//    private AudioRecorderHelper audioRecorderHelper;
//    private WebView instructionsWebView;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_recording);
//
//
//        buttonStartEnd = findViewById(R.id.buttonStartEnd);
//        buttonReturn = findViewById(R.id.buttonReturn);
//        instructionsWebView = findViewById(R.id.instructions);
//
//        // Add VideoPreviewFragment
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, new VideotoPreviewFragment())
//                .commit();
//    }
//
//    private void initializeFragments() {
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//
//        CameraFragment cameraFragment = new CameraFragment();
//        AudioWaveformFragment audioWaveformFragment = new AudioWaveformFragment();
//
//        fragmentTransaction.replace(R.id.cameraContainer, cameraFragment);
//        fragmentTransaction.replace(R.id.audioWaveform, audioWaveformFragment);
//
//        fragmentTransaction.commit();
//    }
//}