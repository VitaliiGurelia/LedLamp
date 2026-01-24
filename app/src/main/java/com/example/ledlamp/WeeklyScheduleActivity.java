package com.example.ledlamp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;

public class WeeklyScheduleActivity extends BaseActivity {
    private static final String TAG = "WeeklyScheduleActivity";

    // [День][Слот][0=Год, 1=Хв, 2=Дія, 3=Ефект]
    public static int[][][] scheduleData = new int[7][5][4];

    LinearLayout containerDays;
    Button btnSend, btnBack;
    Spinner spinnerLamps;
    View layoutLampSelection;
    
    ArrayList<Lamp> lampList = new ArrayList<>();
    ArrayAdapter<Lamp> lampAdapter;
    String selectedLampIp = "";

    private static final int LAMP_PORT = 8888;

    private final int[] daysShortRes = {
            R.string.short_day_mon, R.string.short_day_tue, R.string.short_day_wed, R.string.short_day_thu,
            R.string.short_day_fri, R.string.short_day_sat, R.string.short_day_sun
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_schedule);

        containerDays = findViewById(R.id.containerDays);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        spinnerLamps = findViewById(R.id.spinnerLamps);
        layoutLampSelection = findViewById(R.id.layoutLampSelection);

        setupLampsSpinner();

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                vibrate();
                checkConnectionAndSend();
            });
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    private void setupLampsSpinner() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String json = prefs.getString("LAMPS_JSON", "[]");
        String currentIp = prefs.getString("LAMP_IP", "");

        lampList.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                lampList.add(new Lamp(o.getString("n"), o.getString("i")));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading lamps", e);
        }

        if (lampList.isEmpty() && !currentIp.isEmpty()) {
            lampList.add(new Lamp("Default", currentIp));
        }

        // --- ЛОГІКА ПРИХОВУВАННЯ ---
        if (layoutLampSelection != null) {
            if (lampList.size() < 2) {
                layoutLampSelection.setVisibility(View.GONE);
            } else {
                layoutLampSelection.setVisibility(View.VISIBLE);
            }
        }

        lampAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, lampList);
        lampAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerLamps.setAdapter(lampAdapter);

        for (int i = 0; i < lampList.size(); i++) {
            if (lampList.get(i).ip.equals(currentIp)) {
                spinnerLamps.setSelection(i);
                selectedLampIp = currentIp;
                break;
            }
        }

        spinnerLamps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Lamp selectedLamp = lampList.get(position);
                if (!selectedLamp.ip.equals(selectedLampIp)) {
                    selectedLampIp = selectedLamp.ip;
                    loadFromLamp();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!selectedLampIp.isEmpty()) loadFromLamp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateDayRows();
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void generateDayRows() {
        if (containerDays == null) return;
        containerDays.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int accentColor = getThemeColor(R.attr.accentColor);
        int dangerColor = getThemeColor(R.attr.dangerColor);
        int subTextColor = getThemeColor(R.attr.subTextColor);

        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            View row = inflater.inflate(R.layout.item_weekly_day, containerDays, false);
            TextView tvDay = row.findViewById(R.id.textDayShort);
            LinearLayout timesContainer = row.findViewById(R.id.containerTimes);

            tvDay.setText(getString(daysShortRes[i]));

            ArrayList<int[]> dayEvents = new ArrayList<>();
            for (int s = 0; s < 5; s++) {
                if (scheduleData[i][s][2] != 0) {
                    dayEvents.add(new int[]{scheduleData[i][s][0], scheduleData[i][s][1], scheduleData[i][s][2]});
                }
            }

            java.util.Collections.sort(dayEvents, (a, b) -> Integer.compare(a[0] * 60 + a[1], b[0] * 60 + b[1]));

            if (!dayEvents.isEmpty()) {
                for (int[] event : dayEvents) {
                    TextView timeView = new TextView(this);
                    timeView.setText(String.format(Locale.US, "%02d:%02d", event[0], event[1]));
                    timeView.setTextSize(16);
                    timeView.setPadding(0, 0, 20, 0);
                    timeView.setTypeface(null, android.graphics.Typeface.BOLD);
                    timeView.setTextColor(event[2] == 1 ? accentColor : dangerColor);
                    timesContainer.addView(timeView);
                }
            } else {
                TextView emptyView = new TextView(this);
                emptyView.setText("-");
                emptyView.setTextColor(subTextColor);
                timesContainer.addView(emptyView);
            }

            row.setOnClickListener(v -> {
                vibrate();
                Intent intent = new Intent(this, DayScheduleActivity.class);
                intent.putExtra("day_index", dayIndex);
                startActivity(intent);
            });
            containerDays.addView(row);
        }
    }

    private void checkConnectionAndSend() {
        if (selectedLampIp.isEmpty()) return;
        Toast.makeText(this, R.string.msg_syncing, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(2000);
                InetAddress address = InetAddress.getByName(selectedLampIp);

                byte[] ping = "GET".getBytes();
                socket.send(new DatagramPacket(ping, ping.length, address, LAMP_PORT));
                byte[] buf = new byte[1024];
                socket.receive(new DatagramPacket(buf, buf.length));

                for (int d = 0; d < 7; d++) {
                    for (int s = 0; s < 5; s++) {
                        String cmd = "SCH_SET " + d + " " + s + " " +
                                scheduleData[d][s][0] + " " +
                                scheduleData[d][s][1] + " " +
                                scheduleData[d][s][2] + " " +
                                scheduleData[d][s][3];
                        byte[] data = cmd.getBytes();
                        socket.send(new DatagramPacket(data, data.length, address, LAMP_PORT));
                        Thread.sleep(40);
                    }
                }
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Failed to send schedule", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show());
            } finally {
                if (socket != null) socket.close();
            }
        }).start();
    }

    private void loadFromLamp() {
        if (selectedLampIp.isEmpty() || selectedLampIp.equals("192.168.0.105")) return;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(3000);
                InetAddress address = InetAddress.getByName(selectedLampIp);
                byte[] req = "SCH_REQ".getBytes();
                socket.send(new DatagramPacket(req, req.length, address, LAMP_PORT));

                byte[] buf = new byte[4096];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                socket.receive(recv);

                String data = new String(recv.getData(), 0, recv.getLength());
                if (data.startsWith("SCH_DAT")) {
                    String[] items = data.substring(7).trim().split(" ");
                    // Очистка перед завантаженням
                    for (int d_i = 0; d_i < 7; d_i++) {
                        for (int s_i = 0; s_i < 5; s_i++) {
                            scheduleData[d_i][s_i][2] = 0;
                            scheduleData[d_i][s_i][3] = 0;
                        }
                    }

                    for (String item : items) {
                        String[] parts = item.split(":");
                        if (parts.length >= 5) {
                            int d = Integer.parseInt(parts[0]);
                            int s = Integer.parseInt(parts[1]);
                            int h = Integer.parseInt(parts[2]);
                            int m = Integer.parseInt(parts[3]);
                            int a = Integer.parseInt(parts[4]);
                            int e = 0;
                            if (parts.length >= 6) {
                                e = Integer.parseInt(parts[5]);
                            }
                            
                            if (d < 7 && s < 5) {
                                scheduleData[d][s][0] = h;
                                scheduleData[d][s][1] = m;
                                scheduleData[d][s][2] = a;
                                scheduleData[d][s][3] = e;
                            }
                        }
                    }
                    runOnUiThread(this::generateDayRows);
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Load failed", e);
            }
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else { v.vibrate(50); }
            }
        }
    }
}
