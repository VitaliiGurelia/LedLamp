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
import android.widget.Toast;
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
    private final String[] daysKeys = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
    private final int[] daysNamesRes = {
            R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
            R.string.day_fri, R.string.day_sat, R.string.day_sun
    };

    private final int[] timeValues = {5, 10, 15, 20, 30, 40, 50, 60};
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dawn_alarm);

        containerDays = findViewById(R.id.containerDays);
        spinnerDuration = findViewById(R.id.spinnerDawnDuration);
        btnBack = findViewById(R.id.btnBack);

        setupDurationSpinner();
        generateDaysList();

        btnBack.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isListening = true;
        loadAlarmsFromLamp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isListening = false;
    }

    private void setupDurationSpinner() {
        ArrayList<String> list = new ArrayList<>();
        String minStr = getString(R.string.unit_min);
        for (int t : timeValues) list.add(t + " " + minStr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, list);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDuration.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int pos = prefs.getInt("dawn_duration_pos", 4);
        if (pos < timeValues.length) spinnerDuration.setSelection(pos);

        spinnerDuration.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                int minutes = timeValues[position];
                sendUdpCommand("DAWN " + minutes);
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("dawn_duration_pos", position).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void generateDaysList() {
        containerDays.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);

        for (int i = 0; i < 7; i++) {
            final int lampDayId = i + 1;
            View row = inflater.inflate(R.layout.item_alarm_day, containerDays, false);

            TextView tvName = row.findViewById(R.id.textDayName);
            SwitchCompat swEnable = row.findViewById(R.id.switchDayEnable);
            TextView tvTime = row.findViewById(R.id.textDayTime);

            tvName.setText(getString(daysNamesRes[i]));

            String keyPrefix = "alm_" + daysKeys[i];
            boolean isEnabled = prefs.getBoolean(keyPrefix + "_en", false);
            int hour = prefs.getInt(keyPrefix + "_h", 7);
            int min = prefs.getInt(keyPrefix + "_m", 0);

            swEnable.setChecked(isEnabled);
            tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, min));

            swEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean(keyPrefix + "_en", isChecked).apply();
                String[] parts = tvTime.getText().toString().split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                sendAlarmCommand(lampDayId, isChecked, h, m);
            });

            tvTime.setOnClickListener(v -> {
                vibrate();
                String[] parts = tvTime.getText().toString().split(":");
                int curH = Integer.parseInt(parts[0]);
                int curM = Integer.parseInt(parts[1]);

                new TimePickerDialog(this, (view, h, m) -> {
                    tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
                    prefs.edit().putInt(keyPrefix + "_h", h).putInt(keyPrefix + "_m", m).apply();
                    if (swEnable.isChecked()) {
                        sendAlarmCommand(lampDayId, true, h, m);
                    }
                }, curH, curM, true).show();
            });

            containerDays.addView(row);
        }
    }

    private void loadAlarmsFromLamp() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(2000);
                SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = settings.getString("LAMP_IP", "");
                if (ip.isEmpty()) return;

                InetAddress address = InetAddress.getByName(ip);
                byte[] req = "ALM_REQ".getBytes();
                socket.send(new DatagramPacket(req, req.length, address, LAMP_PORT));

                byte[] buf = new byte[1024];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                socket.receive(recv);

                String msg = new String(recv.getData(), 0, recv.getLength());
                if (msg.startsWith("ALM_DAT")) {
                    String[] items = msg.substring(8).trim().split(" ");
                    SharedPreferences.Editor editor = getSharedPreferences("LampSettings", MODE_PRIVATE).edit();

                    for (String item : items) {
                        String[] parts = item.split(":");
                        if (parts.length == 4) {
                            int d = Integer.parseInt(parts[0]) - 1;
                            if (d >= 0 && d < 7) {
                                boolean en = parts[1].equals("1");
                                int h = Integer.parseInt(parts[2]);
                                int m = Integer.parseInt(parts[3]);

                                String keyPrefix = "alm_" + daysKeys[d];
                                editor.putBoolean(keyPrefix + "_en", en);
                                editor.putInt(keyPrefix + "_h", h);
                                editor.putInt(keyPrefix + "_m", m);
                            }
                        }
                    }
                    editor.apply();
                    runOnUiThread(this::generateDaysList);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load alarms", e);
                // ВИПРАВЛЕННЯ: Додано повідомлення про помилку синхронізації
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_sync_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendAlarmCommand(int day, boolean state, int h, int m) {
        String cmd = String.format(Locale.US, "ALM_SET %d %d %d %d", day, state ? 1 : 0, h, m);
        sendUdpCommand(cmd);
    }

    private void sendUdpCommand(String command) {
        SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String ip = settings.getString("LAMP_IP", "");
        if (ip.isEmpty()) return;

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "UDP Send failed", e);
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
