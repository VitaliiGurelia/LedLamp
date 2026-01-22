package com.example.ledlamp;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;

public class DawnAlarmActivity extends BaseActivity {
    private static final String TAG = "DawnAlarmActivity";

    LinearLayout containerDays;
    Spinner spinnerDuration;
    Button btnBack;

    private static final int LAMP_PORT = 8888;
    // Дні тижня (для збереження)
    private final String[] daysKeys = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
    // Ресурси назв днів
    private final int[] daysNamesRes = {
            R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
            R.string.day_fri, R.string.day_sat, R.string.day_sun
    };

    // Час за замовчуванням (7:00)
    private final int[] timeValues = {5, 10, 15, 20, 30, 40, 50, 60}; // Хвилини світанку

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dawn_alarm);

        containerDays = findViewById(R.id.containerDays);
        spinnerDuration = findViewById(R.id.spinnerDawnDuration);
        btnBack = findViewById(R.id.btnBack);

        // 1. Налаштування спінера тривалості
        setupDurationSpinner();

        // 2. Генерація 7 днів
        generateDaysList();

        btnBack.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    private void setupDurationSpinner() {
        ArrayList<String> list = new ArrayList<>();
        String minStr = getString(R.string.unit_min);
        for (int t : timeValues) list.add(t + " " + minStr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDuration.setAdapter(adapter);

        // Відновлення
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int pos = prefs.getInt("dawn_duration_pos", 4); // 30 min default
        if (pos < timeValues.length) spinnerDuration.setSelection(pos);

        // Слухач
        spinnerDuration.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                int minutes = timeValues[position];
                // Команда DAWN <хвилини>
                sendUdpCommand("DAWN " + minutes);
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("dawn_duration_pos", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void generateDaysList() {
        LayoutInflater inflater = LayoutInflater.from(this);
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);

        for (int i = 0; i < 7; i++) {
            // У прошивці дні зазвичай 1..7 (1=Пн)
            final int lampDayId = i + 1;

            View row = inflater.inflate(R.layout.item_alarm_day, containerDays, false);

            TextView tvName = row.findViewById(R.id.textDayName);
            SwitchCompat swEnable = row.findViewById(R.id.switchDayEnable);
            TextView tvTime = row.findViewById(R.id.textDayTime);

            // Назва дня
            tvName.setText(getString(daysNamesRes[i]));

            // Відновлення збереженого стану
            String keyPrefix = "alm_" + daysKeys[i];
            boolean isEnabled = prefs.getBoolean(keyPrefix + "_en", false);
            int hour = prefs.getInt(keyPrefix + "_h", 7);
            int min = prefs.getInt(keyPrefix + "_m", 0);

            swEnable.setChecked(isEnabled);
            tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, min));

            // Логіка ПЕРЕМИКАЧА
            swEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean(keyPrefix + "_en", isChecked).apply();
                // Отримуємо актуальний час з TextView (бо його могли змінити)
                String[] parts = tvTime.getText().toString().split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);

                sendAlarmCommand(lampDayId, isChecked, h, m);
            });

            // Логіка ЧАСУ
            tvTime.setOnClickListener(v -> {
                vibrate();
                // Поточне значення з екрану
                String[] parts = tvTime.getText().toString().split(":");
                int curH = Integer.parseInt(parts[0]);
                int curM = Integer.parseInt(parts[1]);

                new TimePickerDialog(this, (view, h, m) -> {
                    // Оновлюємо текст
                    tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));

                    // Зберігаємо
                    prefs.edit().putInt(keyPrefix + "_h", h).putInt(keyPrefix + "_m", m).apply();

                    // Відправляємо на лампу (тільки якщо будильник увімкнено)
                    if (swEnable.isChecked()) {
                        sendAlarmCommand(lampDayId, true, h, m);
                    }
                }, curH, curM, true).show();
            });

            containerDays.addView(row);
        }
    }

    // Відправка команди ALM_SET день вкл година хвилина
    private void sendAlarmCommand(int day, boolean state, int h, int m) {
        // Протокол Gunner/SottNick: ALM_SET 1 1 07 30 (Пн Вкл 07:30)
        String cmd = String.format(Locale.US, "ALM_SET %d %d %d %d", day, state ? 1 : 0, h, m);
        sendUdpCommand(cmd);
    }

    private void sendUdpCommand(String command) {
        SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String ip = settings.getString("LAMP_IP", "");
        if (ip.isEmpty()) return;

        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to send UDP command: " + command, e);
            }
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(50);
            }
        }
    }
}
