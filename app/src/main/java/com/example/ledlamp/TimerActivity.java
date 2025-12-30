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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerActivity extends BaseActivity { // Використовуємо BaseActivity для мови

    Spinner spinnerTimer;
    TextView textCountdown, textCurrentTime;
    Button btnBack;

    private static final int LAMP_PORT = 8888;
    private final int[] timerValues = {0, 5, 10, 15, 30, 45, 60};
    private CountDownTimer countDownTimer;

    // Змінні для слухача UDP
    private boolean isListening = false;
    private Thread listenerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        spinnerTimer = findViewById(R.id.spinnerTimer);
        textCountdown = findViewById(R.id.textCountdown);
        textCurrentTime = findViewById(R.id.textCurrentTime);
        btnBack = findViewById(R.id.btnBack);

        setupSpinner();
        checkSavedTimer();

        spinnerTimer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int minutes = timerValues[position];
                // Відправляємо команду тільки якщо це вибір користувача
                // (Для спрощення шлемо завжди, лампа обробить)
                if (minutes > 0) startTimer(minutes);
                else stopTimer();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnBack.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    // --- СИНХРОНІЗАЦІЯ ЧАСУ З ЛАМПОЮ ---

    @Override
    protected void onResume() {
        super.onResume();
        isListening = true;
        startUdpListener(); // Тепер цей метод оголошений нижче!
    }

    @Override
    protected void onPause() {
        super.onPause();
        isListening = false; // Зупиняємо потік при виході
    }

    private void startUdpListener() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(2000);
                SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = settings.getString("LAMP_IP", "");

                // 1. Питаємо статус лампи один раз при вході
                if (!ip.isEmpty()) {
                    byte[] data = "GET".getBytes();
                    DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), LAMP_PORT);
                    socket.send(p);
                }

                // 2. Починаємо слухати відповіді
                while (isListening) {
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        // Пакет CURR містить час у форматі HH:mm:ss в самому кінці
                        if (msg.startsWith("CURR")) {
                            String[] parts = msg.split(" ");
                            if (parts.length >= 10) {
                                String lampTime = parts[parts.length - 1]; // Останній елемент
                                runOnUiThread(() -> textCurrentTime.setText(lampTime));
                            }
                        }
                    } catch (Exception e) {
                        // Timeout або помилка прийому - просто крутимо цикл далі
                    }
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- ЛОГІКА ТАЙМЕРА ---

    private void setupSpinner() {
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
        sendUdpCommand("TOFF " + minutes);
        if (countDownTimer != null) countDownTimer.cancel();
        long millis = minutes * 60 * 1000L;
        long endTime = System.currentTimeMillis() + millis;
        getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putLong("timer_end_time", endTime).apply();
        startCountdownUI(millis);
    }

    private void stopTimer() {
        sendUdpCommand("TOFF 0");
        if (countDownTimer != null) countDownTimer.cancel();
        textCountdown.setText(R.string.timer_not_active);
        getSharedPreferences("LampSettings", MODE_PRIVATE).edit().remove("timer_end_time").apply();
    }

    private void startCountdownUI(long millisLeft) {
        countDownTimer = new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                textCountdown.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
            }
            @Override
            public void onFinish() {
                textCountdown.setText("00:00");
                spinnerTimer.setSelection(0);
            }
        }.start();
    }

    private void checkSavedTimer() {
        long endTime = getSharedPreferences("LampSettings", MODE_PRIVATE).getLong("timer_end_time", 0);
        long now = System.currentTimeMillis();
        if (endTime > now) startCountdownUI(endTime - now);
        else textCountdown.setText(R.string.timer_not_active);
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
            } catch (Exception e) {}
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else { v.vibrate(50); }
            }
        }
    }
}