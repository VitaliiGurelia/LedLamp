package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class CycleActivity extends BaseActivity {

    Switch switchKeep;
    Spinner spinnerInterval;
    Button btnSelectAll, btnBack;
    ListView listView;

    CycleAdapter adapter;
    private static final int LAMP_PORT = 8888;

    // Максимальна кількість ефектів у прошивці (перевірте Constants.h, там 89 або 90)
    private static final int MAX_MODES = 90;

    private final int[] timeValues = {5, 10, 15, 30, 45, 60, 120, 180, 300, 600, 900, 1800, 3600};
    private boolean allSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);

        switchKeep = findViewById(R.id.switchKeepCycle);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.listCycleEffects);

        // (SeekBar яскравості прибрали, бо він не є частиною команди FAV_SET)

        setupTimeSpinner();

        adapter = new CycleAdapter(this, EffectsRepository.EFFECTS_DB);
        listView.setAdapter(adapter);

        loadSettings();

        // --- ОБРОБНИКИ ---

        // Зміна часу -> відправляємо конфіг
        spinnerInterval.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("cycle_interval_pos", position).apply();
                // Не відправляємо при першому запуску (щоб не спамити), тільки якщо це дія користувача
                // Але для простоти можна відправляти завжди, лампа витримає.
                sendCycleConfig();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Перемикач "Не вимикати цикл" -> відправляємо конфіг
        switchKeep.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrate();
            // В оригіналі цей перемикач часто є просто налаштуванням "Увімкнути цикл зараз"
            // Або "Use Saved" (зберігати після перезавантаження).
            // Будемо використовувати його як "State" (Вкл/Викл) у команді FAV_SET
            sendCycleConfig();
        });

        // Кнопка "Вибрати все"
        btnSelectAll.setOnClickListener(v -> {
            vibrate();
            allSelected = !allSelected;
            btnSelectAll.setText(allSelected ? R.string.btn_deselect_all : R.string.btn_select_all);

            for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                eff.useInCycle = allSelected;
            }
            adapter.notifyDataSetChanged();
            sendCycleConfig(); // Відправляємо зміни
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> {
            vibrate();
            finish();
        });
    }

    // --- ГОЛОВНА ФУНКЦІЯ: ЗБИРАЄ І ВІДПРАВЛЯЄ FAV_SET ---
    private void sendCycleConfig() {
        // Формат: FAV_SET <State> <Interval> <Dispersion> <UseSaved> <e0> <e1> ... <eN>

        // 1. State (Вкл/Викл)
        int state = switchKeep.isChecked() ? 1 : 0;

        // 2. Interval (сек)
        int intervalPos = spinnerInterval.getSelectedItemPosition();
        if (intervalPos < 0) intervalPos = 0;
        int interval = timeValues[intervalPos];

        // 3. Dispersion (Розкид) - не використовуємо, ставимо 0
        int dispersion = 0;

        // 4. UseSaved (Зберігати стан) - ставимо 1
        int useSaved = 1;

        StringBuilder sb = new StringBuilder();
        sb.append("FAV_SET ").append(state).append(" ")
                .append(interval).append(" ")
                .append(dispersion).append(" ")
                .append(useSaved);

        // 5. Прапорці ефектів (0 або 1 для кожного ID від 0 до MAX_MODES)
        // Нам треба пройтися по всіх можливих ID і знайти їх у нашому списку

        for (int i = 0; i < MAX_MODES; i++) {
            int flag = 0; // За замовчуванням викл

            // Шукаємо ефект з таким ID у нашій базі
            for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                if (eff.id == i) {
                    if (eff.useInCycle) flag = 1;
                    break;
                }
            }
            sb.append(" ").append(flag);
        }

        // Відправляємо
        sendUdpCommand(sb.toString());
    }

    private void setupTimeSpinner() {
        List<String> timeLabels = new ArrayList<>();
        String sec = getString(R.string.unit_sec);
        String min = getString(R.string.unit_min);
        for (int val : timeValues) {
            if (val < 60) timeLabels.add(val + " " + sec);
            else timeLabels.add((val / 60) + " " + min);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, timeLabels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); // ВИПРАВЛЕНО!
        spinnerInterval.setAdapter(adapter);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int pos = prefs.getInt("cycle_interval_pos", 0);
        if (pos < timeValues.length) spinnerInterval.setSelection(pos);
        // Тут ми не зберігаємо стан switchKeep, бо він приходить з лампи (CUR пакет),
        // але поки що читаємо локально для ініціалізації
    }

    class CycleAdapter extends ArrayAdapter<EffectEntity> {
        public CycleAdapter(Context context, ArrayList<EffectEntity> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_cycle_effect, parent, false);
            }
            EffectEntity item = getItem(position);
            TextView tvName = convertView.findViewById(R.id.textEffectName);
            CheckBox checkBox = convertView.findViewById(R.id.checkInclude);

            tvName.setText(item.getLocalizedName());
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.useInCycle);

            checkBox.setOnClickListener(v -> {
                vibrate();
                item.useInCycle = checkBox.isChecked();
                sendCycleConfig(); // Відправляємо оновлений список відразу
            });
            return convertView;
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
                InetAddress address = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(20);
            }
        }
    }
}