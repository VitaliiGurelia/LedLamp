package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerActivity extends BaseActivity {
    private static final String TAG = "TimerActivity";

    Spinner spinnerTimer;
    TextView textCountdown, textCurrentTime;
    Button btnBack;

    private static final int LAMP_PORT = 8888;
    private final int[] timerValues = {0, 5, 10, 15, 30, 45, 60};
    private CountDownTimer countDownTimer;

    private boolean isListening = false;
    private boolean isUserAction = false; // Щоб уникнути циклічного спрацювання спінера

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        spinnerTimer = findViewById(R.id.spinnerTimer);
        textCountdown = findViewById(R.id.textCountdown);
        textCurrentTime = findViewById(R.id.textCurrentTime);
        btnBack = findViewById(R.id.btnBack);

        setupSpinner();

        // Важливо: спочатку перевіряємо збережений таймер
        checkSavedTimer();

        if (spinnerTimer != null) {
            spinnerTimer.setOnTouchListener((v, event) -> {
                isUserAction = true;
                return false;
            });

            spinnerTimer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isUserAction) return;
                    
                    int minutes = timerValues[position];
                    if (minutes > 0) startTimer(minutes);
                    else stopTimer();
                    
                    isUserAction = false;
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isListening = true;
        startUdpListener();
        checkSavedTimer(); // Перевіряємо ще раз при поверненні в Activity
    }

    @Override
    protected void onPause() {
        super.onPause();
        isListening = false;
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void startUdpListener() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(2000);
                SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = settings.getString("LAMP_IP", "");

                if (!ip.isEmpty()) {
                    byte[] data = "GET".getBytes();
                    DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), LAMP_PORT);
                    socket.send(p);
                }

                while (isListening) {
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        if (msg.startsWith("CURR")) {
                            String[] parts = msg.split(" ");
                            if (parts.length >= 10) {
                                String lampTime = parts[parts.length - 1];
                                runOnUiThread(() -> {
                                    if (textCurrentTime != null) textCurrentTime.setText(lampTime);
                                });
                            }
                        }
                    } catch (Exception e) {}
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error in UDP listener", e);
            }
        }).start();
    }

    private void setupSpinner() {
        if (spinnerTimer == null) return;
        List<String> labels = new ArrayList<>();
        String minStr = getString(R.string.timer_min);
        for (int val : timerValues) {
            if (val == 0) labels.add(getString(R.string.timer_off));
            else labels.add(val + " " + minStr);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, labels);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerTimer.setAdapter(adapter);
    }

    private void startTimer(int minutes) {
        vibrate();
        sendUdpCommand("TOFF " + minutes);
        
        if (countDownTimer != null) countDownTimer.cancel();
        
        long millis = minutes * 60 * 1000L;
        long endTime = System.currentTimeMillis() + millis;
        
        // Зберігаємо час закінчення
        getSharedPreferences("LampSettings", MODE_PRIVATE)
                .edit()
                .putLong("timer_end_time", endTime)
                .putInt("timer_initial_min", minutes)
                .apply();
                
        startCountdownUI(millis);
    }

    private void stopTimer() {
        vibrate();
        sendUdpCommand("TOFF 0");
        if (countDownTimer != null) countDownTimer.cancel();
        if (textCountdown != null) textCountdown.setText(R.string.timer_not_active);
        
        getSharedPreferences("LampSettings", MODE_PRIVATE)
                .edit()
                .remove("timer_end_time")
                .remove("timer_initial_min")
                .apply();
    }

    private void startCountdownUI(long millisLeft) {
        if (textCountdown == null) return;
        
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                textCountdown.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                textCountdown.setText(R.string.timer_not_active);
                spinnerTimer.setSelection(0);
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().remove("timer_end_time").apply();
            }
        }.start();
    }

    private void checkSavedTimer() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        long endTime = prefs.getLong("timer_end_time", 0);
        long now = System.currentTimeMillis();
        
        if (endTime > now) {
            long timeLeft = endTime - now;
            startCountdownUI(timeLeft);
            
            // Оновлюємо вибір у спінері (опціонально, показуємо початковий вибір)
            int initialMin = prefs.getInt("timer_initial_min", 0);
            for (int i = 0; i < timerValues.length; i++) {
                if (timerValues[i] == initialMin) {
                    spinnerTimer.setSelection(i);
                    break;
                }
            }
        } else {
            if (textCountdown != null) textCountdown.setText(R.string.timer_not_active);
            if (spinnerTimer != null) spinnerTimer.setSelection(0);
            prefs.edit().remove("timer_end_time").apply();
        }
    }

    private void sendUdpCommand(String command) {
        SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String ip = settings.getString("LAMP_IP", "");
        if (ip.isEmpty()) return;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error sending UDP command", e);
            }
        }).start();
    }
}
