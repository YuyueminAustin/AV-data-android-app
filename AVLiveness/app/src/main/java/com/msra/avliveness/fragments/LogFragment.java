package com.msra.avliveness.fragments;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.msra.avliveness.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LogFragment extends Fragment {

    private TextView logTextView;
    private Handler handler;
    private Runnable logUpdater;
    private Button returnButton;

    public LogFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logTextView = view.findViewById(R.id.log_text_view);
        returnButton = view.findViewById(R.id.return_button);

        // Write some logs for demonstration
        Log.d("LogFragment", "This is a debug log message");
        Log.i("LogFragment", "This is an info log message");
        Log.e("LogFragment", "This is an error log message");

        handler = new Handler(Looper.getMainLooper());
        logUpdater = new Runnable() {
            @Override
            public void run() {
                updateLogcat();
                handler.postDelayed(this, 2000); // Update every 2 seconds
            }
        };
        handler.post(logUpdater);

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack(); // Navigate back to the main page
            }
        });
    }

    private void updateLogcat() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Filter log lines for your app
                if (line.contains("LogFragment")) {
                    log.append(line).append("\n");
                }
            }
            logTextView.setText(log.toString());
        } catch (Exception e) {
            logTextView.setText("Failed to get logcat output.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(logUpdater); // Stop updating when the view is destroyed
        FragmentActivity activity = getActivity();
        if (activity != null) {
            View logButton = activity.findViewById(R.id.buttonLog);
            if (logButton != null) {
                logButton.setVisibility(View.VISIBLE);
            }
        }
    }
}
