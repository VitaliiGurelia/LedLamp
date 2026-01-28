package com.example.ledlamp;

import android.content.Context;
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
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.util.Log;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    Switch switchPower, switchCycle;
    SeekBar seekBarBrightness, seekBarSpeed, seekBarScale;
    TextView textBriVal, textSpeedVal, textScaleVal, labelScale;
    Spinner spinnerEffects, spinnerLamps;
    Button btnPrev, btnNext, btnReset;
    ImageButton btnMenu;
    Button btnBriMinus, btnBriPlus, btnSpdMinus, btnSpdPlus, btnScaMinus, btnScaPlus;

    ArrayList<Lamp> lampList = new ArrayList<>();
    ArrayAdapter<Lamp> lampAdapter;
    ArrayAdapter<EffectEntity> effectsAdapter;
    ArrayList<EffectEntity> visibleEffects = new ArrayList<>();
    public static ArrayList<EffectEntity> userEffectsList = new ArrayList<>();

    private int lastReportedHiddenId = -1;
    boolean isProgrammaticChange = false;
    boolean isUserAction = true;
    private long lastUpdate = 0;
    private UdpHelper udpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        udpHelper = new UdpHelper();
        udpHelper.setListener(new UdpHelper.UdpListener() {
            @Override
            public void onMessageReceived(String msg) {
                if (msg.startsWith("CUR")) parseCurPacket(msg);
            }

            @Override
            public void onConnectionLost() {
                runOnUiThread(() -> {
                    if (switchPower != null && switchPower.isChecked()) {
                        isProgrammaticChange = true;
                        switchPower.setChecked(false);
                        Toast.makeText(MainActivity.this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        initViews();
        setupListeners();
        
        seekBarScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boolean isRainbow = false;
                if (spinnerEffects.getSelectedItem() != null) {
                    EffectEntity currentEffect = (EffectEntity) spinnerEffects.getSelectedItem();
                    isRainbow = (currentEffect.scaleType == 1);
                    if (fromUser) currentEffect.scale = progress; 
                }
                updateScaleTextColor(progress, isRainbow);
                if (fromUser) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdate > 50) {
                        sendUdpCommand("SCA " + progress);
                        lastUpdate = currentTime;
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                sendUdpCommand("SCA " + seekBar.getProgress());
            }
        });
    }

    private void initViews() {
        btnMenu = findViewById(R.id.btnMenu);
        spinnerLamps = findViewById(R.id.spinnerLamps);
        switchPower = findViewById(R.id.switchPower);
        switchCycle = findViewById(R.id.switchCycle);
        seekBarBrightness = findViewById(R.id.seekBarBrightness);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        seekBarScale = findViewById(R.id.seekBarScale);
        textBriVal = findViewById(R.id.textBrightnessVal);
        textSpeedVal = findViewById(R.id.textSpeedVal);
        textScaleVal = findViewById(R.id.textScaleVal);
        spinnerEffects = findViewById(R.id.spinnerEffects);
        btnPrev = findViewById(R.id.btnPrevEffect);
        btnNext = findViewById(R.id.btnNextEffect);
        btnReset = findViewById(R.id.btnReset);
        labelScale = findViewById(R.id.labelScale);
        btnBriMinus = findViewById(R.id.btnBriMinus);
        btnBriPlus = findViewById(R.id.btnBriPlus);
        btnSpdMinus = findViewById(R.id.btnSpdMinus);
        btnSpdPlus = findViewById(R.id.btnSpdPlus);
        btnScaMinus = findViewById(R.id.btnScaMinus);
        btnScaPlus = findViewById(R.id.btnScaPlus);
    }

    private void setupListeners() {
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        lampAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, lampList);
        lampAdapter.setDropDownViewResource(R.layout.spinner_item);
        if (spinnerLamps != null) {
            spinnerLamps.setAdapter(lampAdapter);
            spinnerLamps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Lamp selectedLamp = lampList.get(position);
                    udpHelper.setIp(selectedLamp.ip);
                    getSharedPreferences("LampSettings", MODE_PRIVATE).edit()
                            .putString("LAST_LAMP_IP", selectedLamp.ip)
                            .putString("LAMP_IP", selectedLamp.ip).apply();
                    udpHelper.sendCommand("GET");
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        effectsAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, visibleEffects);
        effectsAdapter.setDropDownViewResource(R.layout.spinner_item);
        if (spinnerEffects != null) {
            spinnerEffects.setAdapter(effectsAdapter);
            spinnerEffects.setOnTouchListener((v, event) -> { isUserAction = true; return false; });
            spinnerEffects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isUserAction) return;
                    EffectEntity effect = (EffectEntity) parent.getItemAtPosition(position);
                    udpHelper.sendCommand("EFF " + effect.id);
                    udpHelper.sendCommand("BRI " + effect.bright);
                    udpHelper.sendCommand("SPD " + effect.speed);
                    udpHelper.sendCommand("SCA " + effect.scale);
                    updateInterfaceForEffect(effect);
                    if (effect.id != 88) {
                        getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("LAST_EFFECT_ID", effect.id).apply();
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                vibrate();
                int current = spinnerEffects.getSelectedItemPosition();
                if (current > 0) spinnerEffects.setSelection(current - 1);
                else spinnerEffects.setSelection(visibleEffects.size() - 1);
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                vibrate();
                int current = spinnerEffects.getSelectedItemPosition();
                if (current < visibleEffects.size() - 1) spinnerEffects.setSelection(current + 1);
                else spinnerEffects.setSelection(0);
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                vibrate();
                if (spinnerEffects.getSelectedItem() != null) {
                    EffectEntity eff = (EffectEntity) spinnerEffects.getSelectedItem();
                    eff.bright = eff.defBright;
                    eff.speed = eff.defSpeed;
                    eff.scale = eff.defScale;
                    udpHelper.sendCommand("BRI " + eff.bright);
                    udpHelper.sendCommand("SPD " + eff.speed);
                    udpHelper.sendCommand("SCA " + eff.scale);
                    updateInterfaceForEffect(eff);
                    Toast.makeText(this, R.string.msg_reset_done, Toast.LENGTH_SHORT).show();
                }
            });
            btnReset.setOnLongClickListener(v -> {
                vibrate();
                new android.app.AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_effects_reset_title)
                        .setMessage(R.string.dialog_effects_reset_msg)
                        .setPositiveButton(R.string.btn_yes, (d, w) -> {
                            udpHelper.sendCommand("RND_Z");
                            EffectsRepository.resetAllToDefaults();
                            if (spinnerEffects.getSelectedItem() != null) {
                                updateInterfaceForEffect((EffectEntity) spinnerEffects.getSelectedItem());
                            }
                            Toast.makeText(this, R.string.msg_reset_done, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.btn_no, null).show();
                return true;
            });
        }

        if (switchPower != null) {
            switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isProgrammaticChange) {
                    isProgrammaticChange = false;
                    return;
                }
                vibrate();
                udpHelper.sendCommand(isChecked ? "P_ON" : "P_OFF");
            });
        }

        if (switchCycle != null) {
            switchCycle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                udpHelper.sendCommand(isChecked ? "CYC 1" : "CYC 0");
                updateEffectsSpinnerList();
            });
        }

        setupSeekBar(seekBarBrightness, textBriVal, "BRI");
        setupSeekBar(seekBarSpeed, textSpeedVal, "SPD");
        setupPlusMinusButtons(btnBriMinus, btnBriPlus, seekBarBrightness, "BRI");
        setupPlusMinusButtons(btnSpdMinus, btnSpdPlus, seekBarSpeed, "SPD");
        setupPlusMinusButtons(btnScaMinus, btnScaPlus, seekBarScale, "SCA");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLamps();
        applySliderStyle();
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        udpHelper.setIp(prefs.getString("LAMP_IP", ""));
        reloadUserList(this);
        updateEffectsSpinnerList();
        udpHelper.startListening();
        udpHelper.sendCommand("GET"); 
    }

    @Override
    protected void onPause() {
        super.onPause();
        udpHelper.stopListening();
    }

    private void setupSeekBar(SeekBar seekBar, TextView textView, String cmd) {
        if (seekBar == null) return;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (textView != null) textView.setText(String.valueOf(p));
                if (fromUser) {
                    EffectEntity eff = (EffectEntity) spinnerEffects.getSelectedItem();
                    if (eff != null) {
                        if (cmd.equals("BRI")) eff.bright = p;
                        else if (cmd.equals("SPD")) eff.speed = p;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 50) {
                        udpHelper.sendCommand(cmd + " " + p);
                        lastUpdate = now;
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                udpHelper.sendCommand(cmd + " " + s.getProgress());
            }
        });
    }

    private void setupPlusMinusButtons(Button btnMinus, Button btnPlus, SeekBar seekBar, String cmd) {
        if (btnMinus != null) btnMinus.setOnClickListener(v -> changeSlider(seekBar, -1, cmd));
        if (btnPlus != null) btnPlus.setOnClickListener(v -> changeSlider(seekBar, 1, cmd));
    }

    private void changeSlider(SeekBar seekBar, int delta, String cmd) {
        if (seekBar == null || !seekBar.isEnabled() || seekBar.getVisibility() != View.VISIBLE) return;
        vibrate();
        int val = Math.max(0, Math.min(seekBar.getMax(), seekBar.getProgress() + delta));
        seekBar.setProgress(val);
        EffectEntity eff = (EffectEntity) spinnerEffects.getSelectedItem();
        if (eff != null) {
            if (cmd.equals("BRI")) eff.bright = val;
            else if (cmd.equals("SPD")) eff.speed = val;
            else if (cmd.equals("SCA")) eff.scale = val;
        }
        udpHelper.sendCommand(cmd + " " + val);
    }

    private void loadLamps() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String json = prefs.getString("LAMPS_JSON", "[]");
        String lastIp = prefs.getString("LAST_LAMP_IP", "");
        lampList.clear();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                lampList.add(new Lamp(o.getString("n"), o.getString("i")));
            }
        } catch (Exception e) {}
        if (lampList.isEmpty()) {
            String currentIp = prefs.getString("LAMP_IP", "");
            if (!currentIp.isEmpty()) lampList.add(new Lamp("Default Lamp", currentIp));
        }
        if (spinnerLamps != null) spinnerLamps.setVisibility(lampList.size() < 2 ? View.GONE : View.VISIBLE);
        if (lampAdapter != null) lampAdapter.notifyDataSetChanged();
        if (!lastIp.isEmpty()) {
            for (int i = 0; i < lampList.size(); i++) {
                if (lampList.get(i).ip.equals(lastIp)) {
                    if (spinnerLamps != null) spinnerLamps.setSelection(i);
                    udpHelper.setIp(lastIp); break;
                }
            }
        }
    }

    private void parseCurPacket(String msg) {
        try {
            String[] parts = msg.trim().split(" ");
            if (parts.length >= 6) {
                int modeId = Integer.parseInt(parts[1]);
                int bri = Integer.parseInt(parts[2]);
                int spd = Integer.parseInt(parts[3]);
                int sca = Integer.parseInt(parts[4]);
                int state = Integer.parseInt(parts[5]);

                for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                    if (eff.id == modeId) {
                        eff.bright = bri;
                        eff.speed = spd;
                        eff.scale = sca;
                        break;
                    }
                }

                runOnUiThread(() -> {
                    if (switchPower != null && switchPower.isChecked() != (state == 1)) {
                        isProgrammaticChange = true;
                        switchPower.setChecked(state == 1);
                    }
                    
                    if (parts.length >= 11) {
                        boolean isCycleOn = parts[10].equals("1");
                        if (switchCycle != null && switchCycle.isChecked() != isCycleOn) {
                            switchCycle.setOnCheckedChangeListener(null);
                            switchCycle.setChecked(isCycleOn);
                            switchCycle.setOnCheckedChangeListener((b, c) -> {
                                vibrate();
                                udpHelper.sendCommand(c ? "CYC 1" : "CYC 0");
                                updateEffectsSpinnerList();
                            });
                        }
                    }

                    int foundPos = -1;
                    for (int i = 0; i < visibleEffects.size(); i++) {
                        if (visibleEffects.get(i).id == modeId) { foundPos = i; break; }
                    }

                    if (foundPos != -1) {
                        if (spinnerEffects != null && spinnerEffects.getSelectedItemPosition() != foundPos) {
                            isUserAction = false;
                            spinnerEffects.setSelection(foundPos);
                        }
                        updateInterfaceForEffect(visibleEffects.get(foundPos));
                    }
                });
            }
        } catch (Exception e) {}
    }

    public static void reloadUserList(Context context) {
        userEffectsList.clear();
        SharedPreferences prefs = context.getSharedPreferences("LampAppPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("USER_EFFECTS_ORDER", "");
        if (json.isEmpty()) {
            userEffectsList.addAll(EffectsRepository.EFFECTS_DB);
        } else {
            String[] ids = json.split(",");
            for (String idStr : ids) {
                int id;
                boolean visible = true;
                if (idStr.startsWith("HIDDEN_")) {
                    id = Integer.parseInt(idStr.replace("HIDDEN_", ""));
                    visible = false;
                } else { id = Integer.parseInt(idStr); }
                for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                    if (eff.id == id) { eff.isVisible = visible; userEffectsList.add(eff); break; }
                }
            }
            for (EffectEntity dbEff : EffectsRepository.EFFECTS_DB) {
                boolean found = false;
                for (EffectEntity userEff : userEffectsList) {
                    if (userEff.id == dbEff.id) { found = true; break; }
                }
                if (!found) userEffectsList.add(dbEff);
            }
        }
    }

    private void updateEffectsSpinnerList() {
        boolean isCycleMode = switchCycle != null && switchCycle.isChecked();
        visibleEffects.clear();
        for (EffectEntity eff : userEffectsList) {
            if (eff.isVisible && (!isCycleMode || eff.useInCycle)) visibleEffects.add(eff);
        }
        if (effectsAdapter != null) effectsAdapter.notifyDataSetChanged();
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int lastId = prefs.getInt("LAST_EFFECT_ID", 0);
        for (int i = 0; i < visibleEffects.size(); i++) {
            if (visibleEffects.get(i).id == lastId) {
                if (spinnerEffects != null) spinnerEffects.setSelection(i);
                break;
            }
        }
    }

    private void updateInterfaceForEffect(EffectEntity effect) {
        if (seekBarBrightness != null) {
            seekBarBrightness.setProgress(effect.bright);
            textBriVal.setText(String.valueOf(effect.bright));
        }
        if (seekBarSpeed != null) {
            seekBarSpeed.setMax(effect.speedMax);
            seekBarSpeed.setProgress(effect.speed);
            textSpeedVal.setText(String.valueOf(effect.speed));
        }
        if (seekBarScale != null) {
            if (effect.scaleType == 2) {
                seekBarScale.setVisibility(View.INVISIBLE);
                textScaleVal.setVisibility(View.INVISIBLE);
                labelScale.setVisibility(View.INVISIBLE);
                btnScaMinus.setVisibility(View.INVISIBLE);
                btnScaPlus.setVisibility(View.INVISIBLE);
            } else {
                seekBarScale.setVisibility(View.VISIBLE);
                textScaleVal.setVisibility(View.VISIBLE);
                labelScale.setVisibility(View.VISIBLE);
                btnScaMinus.setVisibility(View.VISIBLE);
                btnScaPlus.setVisibility(View.VISIBLE);
                seekBarScale.setMax(effect.scaleMax);
                seekBarScale.setProgress(effect.scale);
                labelScale.setText(effect.scaleType == 1 ? R.string.label_color : R.string.label_scale);
                updateScaleTextColor(effect.scale, effect.scaleType == 1);
            }
        }
    }

    private void updateScaleTextColor(int progress, boolean isRainbowMode) {
        textScaleVal.setText(String.valueOf(progress));
        if (isRainbowMode) {
            float max = (float) seekBarScale.getMax();
            if (max == 0) max = 255f;
            float hue = (progress / max) * 360.0f;
            textScaleVal.setTextColor(Color.HSVToColor(new float[]{hue, 1.0f, 1.0f}));
        } else {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(R.attr.accentColor, typedValue, true);
            textScaleVal.setTextColor(typedValue.data);
        }
    }

    private void applySliderStyle() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        int style = prefs.getInt("slider_style", 0);
        SeekBar[] sliders = {seekBarBrightness, seekBarSpeed, seekBarScale};
        for (SeekBar sb : sliders) {
            if (sb == null) continue;
            sb.setPadding(0, 0, 0, 0);
            if (style == 1) {
                sb.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.track_plasma));
                sb.setThumb(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.thumb_transparent));
                sb.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF006EFF));
            } else if (style == 3) {
                sb.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.track_gradient));
                sb.setThumb(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.thumb_cyber));
            } else {
                sb.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.track_standard));
                sb.setThumb(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.thumb_round));
            }
        }
    }

    private void sendUdpCommand(String command) { if (udpHelper != null) udpHelper.sendCommand(command); }
}
