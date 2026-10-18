package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class CycleActivity extends BaseActivity {
    private static final String TAG = "CycleActivity";

    Switch switchKeep, switchShuffle;
    Spinner spinnerInterval;
    Button btnSelectAll, btnBack;
    ListView listView;

    CycleAdapter adapter;
    private static final int LAMP_PORT = 8888;
    private static final int MAX_MODES = 90;

    private final int[] timeValues = {5, 10, 15, 30, 45, 60, 120, 180, 300, 600, 900, 1800, 3600};
    private boolean allSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);

        switchKeep = findViewById(R.id.switchKeepCycle);
        switchShuffle = findViewById(R.id.switchShuffle);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.listCycleEffects);

        setupTimeSpinner();

        adapter = new CycleAdapter(this, EffectsRepository.EFFECTS_DB);
        listView.setAdapter(adapter);

        loadSettings();

        // --- ОБРОБНИКИ ---

        if (spinnerInterval != null) {
            spinnerInterval.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("cycle_interval_pos", position).apply();
                    sendCycleConfig();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (switchKeep != null) {
            switchKeep.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                sendCycleConfig();
            });
        }

        if (switchShuffle != null) {
            switchShuffle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putBoolean("cycle_shuffle", isChecked).apply();
                sendCycleConfig();
            });
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                vibrate();
                allSelected = !allSelected;
                btnSelectAll.setText(allSelected ? R.string.btn_deselect_all : R.string.btn_select_all);

                for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                    eff.useInCycle = allSelected;
                }
                adapter.notifyDataSetChanged();
                sendCycleConfig();
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                sendCycleConfig(); // На всякий випадок зберігаємо при виході
                finish();
            });
        }
    }

    private void sendCycleConfig() {
        if (switchKeep == null || spinnerInterval == null || switchShuffle == null) return;

        // Новий Формат: FAV_SET <State> <Interval> <Dispersion> <UseSaved> <SHUFFLE> <e0> <e1> ...
        int state = switchKeep.isChecked() ? 1 : 0;
        int intervalPos = spinnerInterval.getSelectedItemPosition();
        int interval = timeValues[intervalPos >= 0 ? intervalPos : 0];
        int dispersion = 0;
        int useSaved = 1;
        int shuffle = switchShuffle.isChecked() ? 1 : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("FAV_SET ").append(state).append(" ")
                .append(interval).append(" ")
                .append(dispersion).append(" ")
                .append(useSaved).append(" ")
                .append(shuffle);

        for (int i = 0; i < MAX_MODES; i++) {
            int flag = 0;
            for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                if (eff.id == i) {
                    if (eff.useInCycle) flag = 1;
                    break;
                }
            }
            sb.append(" ").append(flag);
        }

        sendUdpCommand(sb.toString());
    }

    private void setupTimeSpinner() {
        if (spinnerInterval == null) return;
        List<String> timeLabels = new ArrayList<>();
        String sec = getString(R.string.unit_sec);
        String min = getString(R.string.unit_min);
        for (int val : timeValues) {
            if (val < 60) timeLabels.add(val + " " + sec);
            else timeLabels.add((val / 60) + " " + min);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, timeLabels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerInterval.setAdapter(adapter);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        if (spinnerInterval != null) {
            int pos = prefs.getInt("cycle_interval_pos", 0);
            if (pos < timeValues.length) spinnerInterval.setSelection(pos);
        }
        if (switchShuffle != null) {
            switchShuffle.setChecked(prefs.getBoolean("cycle_shuffle", false));
        }
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

            if (item != null) {
                tvName.setText(item.getLocalizedName());
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(item.useInCycle);
                checkBox.setOnClickListener(v -> {
                    vibrate();
                    item.useInCycle = checkBox.isChecked();
                    sendCycleConfig();
                });
            }
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
            } catch (Exception e) {
                Log.e(TAG, "Error sending UDP command", e);
            }
        }).start();
    }
}
