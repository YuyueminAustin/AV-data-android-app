package com.msra.avliveness.services;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MqttSyncClient {
    private static final String TAG = "MqttSyncClient";

    public static final String COMMAND_TOPIC = "Command";
    public static final String TIME_SYNC_TOPIC = "time_sync";
    public static final String ACK_TOPIC = "Ack";
    public static final String TIMING_TOPIC = "timing";

    public interface Listener {
        void onCommandReceived(String command);
        void onTimeSyncReceived(double targetUnixTime);
        void onPeerTimingReceived(JSONObject payload);
        void onConnectionChanged(boolean connected, String message);
    }

    private final String role;
    private final Listener listener;
    private final Object ackLock = new Object();
    private MqttClient client;
    private CountDownLatch ackLatch = new CountDownLatch(0);
    private String latestAck;

    public MqttSyncClient(String role, Listener listener) {
        this.role = role == null ? "unknown" : role;
        this.listener = listener;
    }

    public synchronized void connect(String host, int port) throws MqttException {
        if (isConnected()) {
            return;
        }
        String brokerUri = "tcp://" + host + ":" + port;
        String clientId = "avliveness-" + role + "-" + System.currentTimeMillis();
        client = new MqttClient(brokerUri, clientId, new MemoryPersistence());
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                try {
                    subscribeAll();
                } catch (MqttException e) {
                    Log.e(TAG, "subscribeAll failed", e);
                }
                if (listener != null) {
                    listener.onConnectionChanged(true, "Connected: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                if (listener != null) {
                    String message = cause == null ? "connectionLost" : cause.getMessage();
                    listener.onConnectionChanged(false, message);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // No-op.
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(5);
        IMqttToken token = client.connectWithResult(options);
        token.waitForCompletion(5000);
        subscribeAll();
    }

    public synchronized boolean isConnected() {
        return client != null && client.isConnected();
    }

    public synchronized void disconnect() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            }
        } catch (MqttException e) {
            Log.e(TAG, "disconnect failed", e);
        } finally {
            client = null;
            if (listener != null) {
                listener.onConnectionChanged(false, "Disconnected");
            }
        }
    }

    public boolean publishCommandAndWaitAck(String command, long timeoutMs) {
        synchronized (ackLock) {
            latestAck = null;
            ackLatch = new CountDownLatch(1);
        }
        if (!publish(COMMAND_TOPIC, command, 1)) {
            return false;
        }
        boolean signalled;
        try {
            signalled = ackLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!signalled) {
            return false;
        }
        String expectedAck = command + "Ack";
        synchronized (ackLock) {
            return expectedAck.equals(latestAck);
        }
    }

    public boolean publishAck(String command, long timeoutMs) {
        return publish(ACK_TOPIC, command + "Ack", 1, timeoutMs);
    }

    public boolean publishTargetTime(double targetUnixTime, long timeoutMs) {
        String payload = String.format(Locale.US, "%.6f", targetUnixTime);
        return publish(TIME_SYNC_TOPIC, payload, 1, timeoutMs);
    }

    public boolean publishTiming(
            String operation,
            Double targetTime,
            Double actualStartTime,
            Double actualEndTime,
            boolean success,
            String savePath,
            long timeoutMs
    ) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("source", role);
            payload.put("operation", operation);
            payload.put("target_time", targetTime);
            payload.put("actual_start_time", actualStartTime);
            payload.put("actual_end_time", actualEndTime);
            payload.put("success", success);
            payload.put("save_path", savePath);
            payload.put("published_at", nowUnixSeconds());
        } catch (JSONException e) {
            Log.e(TAG, "publishTiming payload build failed", e);
            return false;
        }
        return publish(TIMING_TOPIC, payload.toString(), 1, timeoutMs);
    }

    private double nowUnixSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    private synchronized void subscribeAll() throws MqttException {
        if (!isConnected()) {
            return;
        }
        client.subscribe(COMMAND_TOPIC, 1);
        client.subscribe(TIME_SYNC_TOPIC, 1);
        client.subscribe(ACK_TOPIC, 1);
        client.subscribe(TIMING_TOPIC, 1);
    }

    private boolean publish(String topic, String payload, int qos) {
        return publish(topic, payload, qos, 3000);
    }

    private boolean publish(String topic, String payload, int qos, long timeoutMs) {
        if (!isConnected()) {
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            client.publish(topic, message);
            return true;
        } catch (MqttException e) {
            Log.e(TAG, "publish failed, topic=" + topic + ", payload=" + payload, e);
            return false;
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        if (ACK_TOPIC.equals(topic)) {
            synchronized (ackLock) {
                latestAck = payload;
                ackLatch.countDown();
            }
            return;
        }
        if (COMMAND_TOPIC.equals(topic)) {
            if (listener != null) {
                listener.onCommandReceived(payload);
            }
            return;
        }
        if (TIME_SYNC_TOPIC.equals(topic)) {
            try {
                double targetTime = Double.parseDouble(payload);
                if (listener != null) {
                    listener.onTimeSyncReceived(targetTime);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid time_sync payload: " + payload, e);
            }
            return;
        }
        if (TIMING_TOPIC.equals(topic)) {
            try {
                JSONObject timingPayload = new JSONObject(payload);
                String source = timingPayload.optString("source", "");
                if (!role.equals(source) && listener != null) {
                    listener.onPeerTimingReceived(timingPayload);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Invalid timing payload: " + payload, e);
            }
        }
    }
}
