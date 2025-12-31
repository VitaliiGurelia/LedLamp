package com.example.ledlamp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AppSettingsActivity extends BaseActivity {
    private static final String TAG = "AppSettingsActivity";

    Switch switchVibration, switchExitConfirm, switchAutoDst, switchSwapColors;
    RadioGroup radioGroupSliders;
    Button btnBack, btnSyncTime;
    Spinner spinnerTimeZone;
    TextView textDeviceTime;
    RadioGroup radioGroupTheme;

    private static final int LAMP_PORT = 8888;
    private boolean isListening = false;

    private final int[] gmtValues = {
            -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1,
            0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        // Ініціалізація
        switchVibration = findViewById(R.id.switchVibration);
        switchExitConfirm = findViewById(R.id.switchExitConfirm);
        switchAutoDst = findViewById(R.id.switchAutoDst);
        switchSwapColors = findViewById(R.id.switchSwapColors);
        radioGroupSliders = findViewById(R.id.radioGroupSliders);
        btnBack = findViewById(R.id.btnBack);
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone);
        textDeviceTime = findViewById(R.id.textDeviceTime);
        btnSyncTime = findViewById(R.id.btnSyncTime);

        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);

        // Відновлення стану
        if (switchVibration != null) switchVibration.setChecked(prefs.getBoolean("vibration", true));
        if (switchExitConfirm != null) switchExitConfirm.setChecked(prefs.getBoolean("exit_confirm", false));
        if (switchAutoDst != null) switchAutoDst.setChecked(prefs.getBoolean("auto_dst", false));
        if (switchSwapColors != null) switchSwapColors.setChecked(prefs.getBoolean("swap_colors", false));

        if (radioGroupSliders != null) {
            int currentStyle = prefs.getInt("slider_style", 0);
            if (currentStyle == 1) radioGroupSliders.check(R.id.radioStylePlasma);
            else if (currentStyle == 3) radioGroupSliders.check(R.id.radioStyleGradient);
            else radioGroupSliders.check(R.id.radioStyleNeon);
        }

        setupTimeZoneSpinner(prefs);

        // --- ОБРОБНИКИ ---

        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        if (radioGroupTheme != null) {
            int savedTheme = prefs.getInt("app_theme", 0); // 0=Dark, 1=Light, 2=Cyber
            if (savedTheme == 1) radioGroupTheme.check(R.id.radioThemeLight);
            else if (savedTheme == 2) radioGroupTheme.check(R.id.radioThemeCyber);
            else radioGroupTheme.check(R.id.radioThemeDark);

            radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
                vibrate();
                int newTheme = 0;
                if (checkedId == R.id.radioThemeLight) newTheme = 1;
                else if (checkedId == R.id.radioThemeCyber) newTheme = 2;

                prefs.edit().putInt("app_theme", newTheme).apply();

                // Перезапуск для застосування теми
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        if (btnSyncTime != null) {
            btnSyncTime.setOnClickListener(v -> {
                vibrate();
                syncTimeWithLamp();
            });
        }

        if (switchVibration != null) {
            switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean("vibration", isChecked).apply();
            });
        }

        if (switchExitConfirm != null) {
            switchExitConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean("exit_confirm", isChecked).apply();
            });
        }

        if (radioGroupSliders != null) {
            radioGroupSliders.setOnCheckedChangeListener((group, checkedId) -> {
                vibrate();
                int newStyle = 0;
                if (checkedId == R.id.radioStylePlasma) newStyle = 1;
                else if (checkedId == R.id.radioStyleGradient) newStyle = 3;
                prefs.edit().putInt("slider_style", newStyle).apply();
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            });
        }

        if (switchAutoDst != null) {
            switchAutoDst.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean("auto_dst", isChecked).apply();
                sendUdpCommand("DST " + (isChecked ? "1" : "0"));
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            });
        }

        if (switchSwapColors != null) {
            switchSwapColors.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                prefs.edit().putBoolean("swap_colors", isChecked).apply();
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
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
        startTimeListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isListening = false;
    }

    // --- МОНІТОРИНГ ЧАСУ ---
    private void startTimeListener() {
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(1500);

                SharedPreferences settings = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = settings.getString("LAMP_IP", "");
                if (ip.isEmpty()) return;

                InetAddress address = InetAddress.getByName(ip);

                while (isListening) {
                    try {
                        byte[] req = "GET".getBytes();
                        DatagramPacket packet = new DatagramPacket(req, req.length, address, LAMP_PORT);
                        socket.send(packet);

                        byte[] buf = new byte[1024];
                        DatagramPacket recv = new DatagramPacket(buf, buf.length);
                        socket.receive(recv);

                        String msg = new String(recv.getData(), 0, recv.getLength());

                        if (msg.startsWith("CUR")) {
                            String[] parts = msg.trim().split(" ");
                            if (parts.length >= 2) {
                                // Час і день в кінці пакету
                                String dayStr = parts[parts.length - 1];
                                String timeStr = parts[parts.length - 2];

                                if (timeStr.contains(":")) {
                                    String dayName = "";
                                    try {
                                        int dayNum = Integer.parseInt(dayStr);
                                        dayName = getDayNameByNumber(dayNum);
                                    } catch (Exception e) {
                                        // Ignore parse exception
                                    }

                                    String finalStr = timeStr + "  " + dayName;
                                    runOnUiThread(() -> {
                                        if (textDeviceTime != null) textDeviceTime.setText(finalStr);
                                    });
                                }
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        runOnUiThread(() -> {
                            if (textDeviceTime != null) textDeviceTime.setText(R.string.error_connection);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error receiving time", e);
                    }

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in time listener", e);
            } finally {
                if (socket != null) socket.close();
            }
        }).start();
    }

    private String getDayNameByNumber(int dayNum) {
        // 1=Нд ... 7=Сб
        switch (dayNum) {
            case 1: return getString(R.string.short_day_sun);
            case 2: return getString(R.string.short_day_mon);
            case 3: return getString(R.string.short_day_tue);
            case 4: return getString(R.string.short_day_wed);
            case 5: return getString(R.string.short_day_thu);
            case 6: return getString(R.string.short_day_fri);
            case 7: return getString(R.string.short_day_sat);
            default: return "";
        }
    }

    private void syncTimeWithLamp() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        String cmd = String.format(Locale.US, "TME %d %d %d", h, m, s);
        sendUdpCommand(cmd);
        Toast.makeText(this, R.string.msg_text_sent, Toast.LENGTH_SHORT).show();
    }

    private void setupTimeZoneSpinner(SharedPreferences prefs) {
        if (spinnerTimeZone == null) return;
        ArrayList<String> gmtLabels = new ArrayList<>();
        int savedGmt = prefs.getInt("gmt_value", 1);
        int selectedIndex = 12;

        for (int i = 0; i < gmtValues.length; i++) {
            int val = gmtValues[i];
            if (val > 0) gmtLabels.add("GMT +" + val);
            else if (val < 0) gmtLabels.add("GMT " + val);
            else gmtLabels.add("GMT 0");

            if (val == savedGmt) selectedIndex = i;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, gmtLabels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerTimeZone.setAdapter(adapter);
        spinnerTimeZone.setSelection(selectedIndex);

        spinnerTimeZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedGmt = gmtValues[position];
                if (selectedGmt != prefs.getInt("gmt_value", -100)) {
                    prefs.edit().putInt("gmt_value", selectedGmt).apply();
                    sendUdpCommand("TMZ " + selectedGmt);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
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
                DatagramPacket packet = new DatagramPacket(data, data.length, address, 8888);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error sending UDP command", e);
            }
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }
        }
    }
}