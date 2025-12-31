package com.example.ledlamp;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class ConnectionActivity extends BaseActivity {
    private static final String TAG = "ConnectionActivity";

    EditText editName, editIp;
    Button btnAdd, btnFind, btnBack;
    ListView listView;

    ArrayList<Lamp> lampList = new ArrayList<>();
    LampsAdapter adapter;

    private static final int LAMP_PORT = 8888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        editName = findViewById(R.id.editLampName);
        editIp = findViewById(R.id.editIpAddress);
        btnAdd = findViewById(R.id.btnAddLamp);
        btnFind = findViewById(R.id.btnFindLamps);
        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.listViewLamps);

        adapter = new LampsAdapter(this, lampList);
        listView.setAdapter(adapter);

        loadLamps();

        // ДОДАВАННЯ
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                vibrate();
                String name = editName.getText().toString();
                String ip = editIp.getText().toString();
                if (!name.isEmpty() && !ip.isEmpty()) {
                    for (Lamp l : lampList) {
                        if (l.ip.equals(ip)) {
                            Toast.makeText(this, R.string.msg_ip_exists, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    lampList.add(new Lamp(name, ip));
                    saveLamps();
                    adapter.notifyDataSetChanged();
                    editName.setText("");
                    editIp.setText("");
                    Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.msg_empty_fields, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // ПОШУК
        if (btnFind != null) {
            btnFind.setOnClickListener(v -> {
                vibrate();
                findNewLamp();
            });
        }

        // НАЗАД
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    private void findNewLamp() {
        Toast.makeText(this, R.string.msg_scanning, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(3000);

                byte[] data = "GET".getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), LAMP_PORT);
                socket.send(packet);

                boolean foundNew = false;
                long endTime = System.currentTimeMillis() + 3000;

                while (System.currentTimeMillis() < endTime) {
                    byte[] buf = new byte[1024];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);

                    try {
                        socket.receive(recv);
                        String foundIp = recv.getAddress().getHostAddress();

                        boolean exists = false;
                        for (Lamp l : lampList) {
                            if (l.ip.equals(foundIp)) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            foundNew = true;
                            runOnUiThread(() -> {
                                if (editIp != null) editIp.setText(foundIp);
                                if (editName != null) editName.setText(R.string.default_new_lamp_name);
                                Toast.makeText(ConnectionActivity.this, getString(R.string.msg_found_lamp, foundIp), Toast.LENGTH_LONG).show();
                            });
                            break;
                        }

                    } catch (SocketTimeoutException e) {
                        // Timeout
                    }
                }

                if (!foundNew) {
                    runOnUiThread(() -> Toast.makeText(ConnectionActivity.this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error finding lamp", e);
            } finally {
                if (socket != null) socket.close();
            }
        }).start();
    }

    private void saveLamps() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for (Lamp l : lampList) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("n", l.name);
                obj.put("i", l.ip);
                jsonArray.put(obj);
            } catch (Exception e) {
                Log.e(TAG, "Error saving lamp to JSON", e);
            }
        }
        editor.putString("LAMPS_JSON", jsonArray.toString());
        editor.apply();
    }

    private void loadLamps() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String json = prefs.getString("LAMPS_JSON", "[]");
        lampList.clear();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                lampList.add(new Lamp(obj.getString("n"), obj.getString("i")));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading lamps from JSON", e);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // --- АДАПТЕР СПИСКУ (Optimized with ViewHolder) ---
    class LampsAdapter extends ArrayAdapter<Lamp> {
        
        private class ViewHolder {
            TextView tvName;
            TextView tvIp;
            ImageButton btnCheck;
            ImageButton btnWifiReset;
            ImageButton btnDelete;
        }

        public LampsAdapter(Context context, ArrayList<Lamp> lamps) {
            super(context, 0, lamps);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Lamp lamp = getItem(position);
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_lamp, parent, false);
                holder = new ViewHolder();
                holder.tvName = convertView.findViewById(R.id.itemLampName);
                holder.tvIp = convertView.findViewById(R.id.itemLampIp);
                holder.btnCheck = convertView.findViewById(R.id.btnItemCheck);
                holder.btnWifiReset = convertView.findViewById(R.id.btnItemWifiReset);
                holder.btnDelete = convertView.findViewById(R.id.btnItemDelete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (lamp != null) {
                holder.tvName.setText(lamp.name);
                holder.tvIp.setText(lamp.ip);

                // КНОПКА 1: ПЕРЕВІРКА
                holder.btnCheck.setOnClickListener(v -> {
                    vibrate();
                    Toast.makeText(getContext(), getString(R.string.msg_pinging, lamp.name), Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            DatagramSocket s = new DatagramSocket();
                            s.setSoTimeout(2000);
                            byte[] d = "GET".getBytes();
                            DatagramPacket p = new DatagramPacket(d, d.length, InetAddress.getByName(lamp.ip), LAMP_PORT);
                            s.send(p);
                            byte[] buf = new byte[1024];
                            DatagramPacket r = new DatagramPacket(buf, buf.length);
                            s.receive(r);
                            runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.msg_ping_ok, lamp.name), Toast.LENGTH_SHORT).show());
                            s.close();
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(getContext(), getString(R.string.msg_lamp_not_found), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                });

                // КНОПКА 2: СКИНУТИ WI-FI
                holder.btnWifiReset.setOnClickListener(v -> {
                    vibrate();
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.dialog_reset_title)
                            .setMessage(getString(R.string.dialog_reset_msg, lamp.name))
                            .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                                new Thread(() -> {
                                    try {
                                        DatagramSocket s = new DatagramSocket();
                                        byte[] d = "RST".getBytes();
                                        DatagramPacket p = new DatagramPacket(d, d.length, InetAddress.getByName(lamp.ip), LAMP_PORT);
                                        s.send(p);
                                        s.close();
                                        runOnUiThread(() -> Toast.makeText(getContext(), R.string.msg_reset_cmd_sent, Toast.LENGTH_SHORT).show());
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error sending reset command", e);
                                    }
                                }).start();
                            })
                            .setNegativeButton(R.string.btn_no, null)
                            .show();
                });

                // КНОПКА 3: ВИДАЛИТИ
                holder.btnDelete.setOnClickListener(v -> {
                    vibrate();
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.dialog_delete_title)
                            .setMessage(getString(R.string.dialog_delete_msg, lamp.name))
                            .setPositiveButton(R.string.btn_yes, (dialog, which) -> {
                                lampList.remove(position);
                                saveLamps();
                                notifyDataSetChanged();
                                Toast.makeText(getContext(), R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show();
                });
            }

            return convertView;
        }
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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