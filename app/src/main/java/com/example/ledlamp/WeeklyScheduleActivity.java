package com.example.ledlamp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;

public class WeeklyScheduleActivity extends BaseActivity {

    // [День][Слот][0=Год, 1=Хв, 2=Дія]
    public static int[][][] scheduleData = new int[7][5][3];

    LinearLayout containerDays;
    Button btnSend, btnBack;
    private static final int LAMP_PORT = 8888;

    // Ресурси скорочених назв
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

        // Завантаження при старті
        loadFromLamp();

        btnSend.setOnClickListener(v -> { vibrate(); sendToLamp(); });
        btnBack.setOnClickListener(v -> { vibrate(); finish(); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перемальовуємо список кожного разу, коли екран стає видимим
        // (наприклад, коли повернулися з редагування дня)
        generateDayRows();
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void generateDayRows() {
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

            // 1. Збираємо всі активні події цього дня в тимчасовий список
            java.util.ArrayList<int[]> dayEvents = new java.util.ArrayList<>();

            for (int s = 0; s < 5; s++) {
                int h = scheduleData[i][s][0];
                int m = scheduleData[i][s][1];
                int action = scheduleData[i][s][2];

                if (action != 0) {
                    dayEvents.add(new int[]{h, m, action});
                }
            }

            // 2. Сортуємо список за часом (від ранку до ночі)
            java.util.Collections.sort(dayEvents, (a, b) -> {
                int timeA = a[0] * 60 + a[1]; // Час у хвилинах від початку доби
                int timeB = b[0] * 60 + b[1];
                return Integer.compare(timeA, timeB);
            });

            // 3. Виводимо відсортовані події на екран
            if (!dayEvents.isEmpty()) {
                for (int[] event : dayEvents) {
                    int h = event[0];
                    int m = event[1];
                    int action = event[2];

                    TextView timeView = new TextView(this);
                    String timeText = String.format(Locale.US, "%02d:%02d", h, m);
                    timeView.setText(timeText);
                    timeView.setTextSize(16);
                    timeView.setPadding(0, 0, 20, 0);
                    timeView.setTypeface(null, android.graphics.Typeface.BOLD);

                    if (action == 1) {
                        timeView.setTextColor(accentColor); // ON
                    } else {
                        timeView.setTextColor(dangerColor);   // OFF
                    }

                    timesContainer.addView(timeView);
                }
            } else {
                // Якщо подій немає
                TextView emptyView = new TextView(this);
                emptyView.setText("-");
                emptyView.setTextColor(subTextColor);
                timesContainer.addView(emptyView);
            }

            // Натискання відкриває редактор
            row.setOnClickListener(v -> {
                vibrate();
                Intent intent = new Intent(WeeklyScheduleActivity.this, DayScheduleActivity.class);
                intent.putExtra("day_index", dayIndex);
                startActivity(intent);
            });

            containerDays.addView(row);
        }
    }

    private void sendToLamp() {
        Toast.makeText(this, R.string.msg_text_sent, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = prefs.getString("LAMP_IP", "");
                InetAddress address = InetAddress.getByName(ip);

                for (int d = 0; d < 7; d++) {
                    for (int s = 0; s < 5; s++) {
                        String cmd = "SCH_SET " + d + " " + s + " " +
                                scheduleData[d][s][0] + " " +
                                scheduleData[d][s][1] + " " +
                                scheduleData[d][s][2];
                        byte[] data = cmd.getBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                        socket.send(packet);
                        Thread.sleep(30);
                    }
                }
                socket.close();
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {}
        }).start();
    }

    private void loadFromLamp() {
        // ... (Ця функція залишається без змін, але зверни увагу на generateDayRows в кінці)
        Toast.makeText(this, R.string.msg_syncing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(3000);
                SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
                String ip = prefs.getString("LAMP_IP", "");
                if (ip.isEmpty()) return;

                InetAddress address = InetAddress.getByName(ip);
                byte[] req = "SCH_REQ".getBytes();
                socket.send(new DatagramPacket(req, req.length, address, LAMP_PORT));

                byte[] buf = new byte[4096];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                socket.receive(recv);

                String data = new String(recv.getData(), 0, recv.getLength());
                if (data.startsWith("SCH_DAT")) {
                    String[] items = data.substring(7).trim().split(" ");
                    for (String item : items) {
                        String[] parts = item.split(":");
                        if (parts.length == 5) {
                            int d = Integer.parseInt(parts[0]);
                            int s = Integer.parseInt(parts[1]);
                            int h = Integer.parseInt(parts[2]);
                            int m = Integer.parseInt(parts[3]);
                            int a = Integer.parseInt(parts[4]);
                            if (d < 7 && s < 5) {
                                scheduleData[d][s][0] = h;
                                scheduleData[d][s][1] = m;
                                scheduleData[d][s][2] = a;
                            }
                        }
                    }
                    // ОНОВЛЮЄМО СПИСОК ПІСЛЯ ЗАВАНТАЖЕННЯ
                    runOnUiThread(() -> {
                        generateDayRows();
                        Toast.makeText(this, R.string.msg_synced, Toast.LENGTH_SHORT).show();
                    });
                }
                socket.close();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else { v.vibrate(50); }
            }
        }
    }
}