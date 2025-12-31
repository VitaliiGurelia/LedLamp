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

    // --- ЗМІННІ ІНТЕРФЕЙСУ ---
    Switch switchPower, switchCycle;
    SeekBar seekBarBrightness, seekBarSpeed, seekBarScale;
    TextView textBriVal, textSpeedVal, textScaleVal, labelScale;
    Spinner spinnerEffects, spinnerLamps;
    Button btnPrev, btnNext, btnReset;
    ImageButton btnMenu;
    Button btnBriMinus, btnBriPlus, btnSpdMinus, btnSpdPlus, btnScaMinus, btnScaPlus;

    // --- ДАНІ ---
    ArrayList<Lamp> lampList = new ArrayList<>();
    ArrayAdapter<Lamp> lampAdapter;

    // Адаптер для ефектів
    ArrayAdapter<EffectEntity> effectsAdapter;
    // Список видимих ефектів
    ArrayList<EffectEntity> visibleEffects = new ArrayList<>();

    // Список користувача (статичний, щоб бачили інші Activity)
    public static ArrayList<EffectEntity> userEffectsList = new ArrayList<>();

    // --- ЛОГІКА ---
    private int lastReportedHiddenId = -1;
    boolean isProgrammaticChange = false;
    boolean isUserAction = true;
    private long lastUpdate = 0;

    // --- ПОМІЧНИК МЕРЕЖІ ---
    private UdpHelper udpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ініціалізація UDP Helper
        udpHelper = new UdpHelper();
        udpHelper.setListener(msg -> {
            if (msg.startsWith("CUR")) {
                parseCurPacket(msg);
            }
        });

        // Ініціалізація UI
        initViews();
        setupListeners();
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
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                vibrate();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            });
        }

        // Лампи
        lampAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, lampList);
        lampAdapter.setDropDownViewResource(R.layout.spinner_item);
        if (spinnerLamps != null) {
            spinnerLamps.setAdapter(lampAdapter);

            spinnerLamps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Lamp selectedLamp = lampList.get(position);
                    udpHelper.setIp(selectedLamp.ip);

                    getSharedPreferences("LampSettings", MODE_PRIVATE)
                            .edit().putString("LAST_LAMP_IP", selectedLamp.ip)
                            .putString("LAMP_IP", selectedLamp.ip).apply();

                    udpHelper.sendCommand("GET");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Ефекти
        effectsAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, visibleEffects);
        effectsAdapter.setDropDownViewResource(R.layout.spinner_item);
        if (spinnerEffects != null) {
            spinnerEffects.setAdapter(effectsAdapter);

            spinnerEffects.setOnTouchListener((v, event) -> {
                isUserAction = true;
                v.performClick();
                return false;
            });

            spinnerEffects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!isUserAction) {
                        isUserAction = true;
                        return;
                    }
                    EffectEntity effect = (EffectEntity) parent.getItemAtPosition(position);
                    udpHelper.sendCommand("EFF " + effect.id);
                    updateInterfaceForEffect(effect);

                    if (effect.id != 88) { // Не зберігаємо малювання
                        getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("LAST_EFFECT_ID", effect.id).apply();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Кнопки
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
                    EffectEntity currentEffect = (EffectEntity) spinnerEffects.getSelectedItem();
                    udpHelper.sendCommand("BRI " + currentEffect.defBright);
                    udpHelper.sendCommand("SPD " + currentEffect.defSpeed);
                    udpHelper.sendCommand("SCA " + currentEffect.defScale);

                    seekBarBrightness.setProgress(currentEffect.defBright);
                    seekBarSpeed.setProgress(currentEffect.defSpeed);
                    seekBarScale.setProgress(currentEffect.defScale);
                    Toast.makeText(this, R.string.msg_reset_done, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Перемикачі
        if (switchPower != null) {
            switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                if (isProgrammaticChange) {
                    isProgrammaticChange = false;
                    return;
                }
                if (isChecked) {
                    udpHelper.sendCommand("P_ON");
                    // Перевірку статусу зробить слухач автоматично через 2 сек
                } else {
                    udpHelper.sendCommand("P_OFF");
                }
            });
        }

        if (switchCycle != null) {
            switchCycle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                vibrate();
                udpHelper.sendCommand(isChecked ? "CYC 1" : "CYC 0");
                updateEffectsSpinnerList();
            });
        }

        // Слайдери
        setupSeekBar(seekBarBrightness, textBriVal, "BRI");
        setupSeekBar(seekBarSpeed, textSpeedVal, "SPD");
        setupSeekBar(seekBarScale, textScaleVal, "SCA");

        setupPlusMinusButtons(btnBriMinus, btnBriPlus, seekBarBrightness, "BRI");
        setupPlusMinusButtons(btnSpdMinus, btnSpdPlus, seekBarSpeed, "SPD");
        setupPlusMinusButtons(btnScaMinus, btnScaPlus, seekBarScale, "SCA");

        // Вихід
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (getSharedPreferences("LampAppPrefs", MODE_PRIVATE).getBoolean("exit_confirm", false)) {
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.dialog_exit_title)
                            .setMessage(R.string.dialog_exit_msg)
                            .setPositiveButton(R.string.btn_yes, (d, w) -> finish())
                            .setNegativeButton(R.string.btn_no, null).show();
                } else { finish(); }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLamps();
        applySliderStyle();

        // Оновлюємо IP в хелпері
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String ip = prefs.getString("LAMP_IP", "");
        udpHelper.setIp(ip);

        // Завантаження списку
        reloadUserList(this);
        updateEffectsSpinnerList();

        // Старт слухача
        udpHelper.startListening();
        udpHelper.sendCommand("GET"); // Примусовий пінг
    }

    @Override
    protected void onPause() {
        super.onPause();
        udpHelper.stopListening();
    }

    // --- ДОПОМІЖНІ МЕТОДИ ---

    private void setupSeekBar(SeekBar seekBar, TextView textView, String cmd) {
        if (seekBar == null) return;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (textView != null) textView.setText(String.valueOf(p));
                if (fromUser) {
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
        } catch (Exception e) {
            Log.e(TAG, "Error loading lamps", e);
        }

        if (lampList.isEmpty()) lampList.add(new Lamp("Demo Lamp", "192.168.0.105"));
        if (lampAdapter != null) lampAdapter.notifyDataSetChanged();

        if (!lastIp.isEmpty()) {
            for (int i = 0; i < lampList.size(); i++) {
                if (lampList.get(i).ip.equals(lastIp)) {
                    if (spinnerLamps != null) spinnerLamps.setSelection(i);
                    udpHelper.setIp(lastIp);
                    break;
                }
            }
        } else if (!lampList.isEmpty()) {
            udpHelper.setIp(lampList.get(0).ip);
        }
    }

    private void parseCurPacket(String msg) {
        try {
            String[] parts = msg.trim().split(" ");
            if (parts.length >= 2) {
                int modeId = Integer.parseInt(parts[1]);

                if (parts.length >= 6) {
                    int bri = Integer.parseInt(parts[2]);
                    int spd = Integer.parseInt(parts[3]);
                    int sca = Integer.parseInt(parts[4]);
                    int state = Integer.parseInt(parts[5]);
                    boolean isCycleOn = false;
                    if (parts.length >= 11) isCycleOn = Integer.parseInt(parts[10]) == 1;

                    boolean finalCycle = isCycleOn;

                    runOnUiThread(() -> {
                        if (switchPower != null && switchPower.isChecked() != (state == 1)) {
                            isProgrammaticChange = true;
                            switchPower.setChecked(state == 1);
                        }

                        if (switchCycle != null && switchCycle.isChecked() != finalCycle) {
                            switchCycle.setOnCheckedChangeListener(null);
                            switchCycle.setChecked(finalCycle);
                            updateEffectsSpinnerList();
                            switchCycle.setOnCheckedChangeListener((b, c) -> {
                                vibrate();
                                udpHelper.sendCommand(c ? "CYC 1" : "CYC 0");
                                updateEffectsSpinnerList();
                            });
                        }

                        if (seekBarBrightness != null) seekBarBrightness.setProgress(bri);
                        if (seekBarSpeed != null) seekBarSpeed.setProgress(spd);
                        if (seekBarScale != null) seekBarScale.setProgress(sca);
                        if (textBriVal != null) textBriVal.setText(String.valueOf(bri));
                        if (textSpeedVal != null) textSpeedVal.setText(String.valueOf(spd));
                        if (textScaleVal != null) textScaleVal.setText(String.valueOf(sca));

                        // Синхронізація спінера
                        int foundPos = -1;
                        for (int i = 0; i < visibleEffects.size(); i++) {
                            if (visibleEffects.get(i).id == modeId) {
                                foundPos = i;
                                break;
                            }
                        }

                        if (foundPos != -1) {
                            if (spinnerEffects != null && spinnerEffects.getSelectedItemPosition() != foundPos) {
                                isUserAction = false;
                                spinnerEffects.setSelection(foundPos);
                            }
                            updateInterfaceForEffect(visibleEffects.get(foundPos));
                            lastReportedHiddenId = -1;
                        } else {
                            // Прихований ефект
                            for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                                if (eff.id == modeId) {
                                    updateInterfaceForEffect(eff);
                                    if (lastReportedHiddenId != modeId) {
                                        Toast.makeText(this, getString(R.string.msg_hidden_effect, eff.getLocalizedName()), Toast.LENGTH_SHORT).show();
                                        lastReportedHiddenId = modeId;
                                    }
                                    break;
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing CUR packet", e);
        }
    }

    // --- ФУНКЦІЇ, ЯКІ МИ ВИКЛИКАЄМО ЗІ СТОРОННІХ ФАЙЛІВ ---

    public static void reloadUserList(Context context) {
        userEffectsList.clear();
        SharedPreferences prefs = context.getSharedPreferences("LampAppPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("USER_EFFECTS_ORDER", "");

        if (json.isEmpty()) {
            userEffectsList.addAll(EffectsRepository.EFFECTS_DB);
        } else {
            String[] ids = json.split(",");
            for (String idStr : ids) {
                if (idStr.startsWith("HIDDEN_")) {
                    try {
                        int id = Integer.parseInt(idStr.replace("HIDDEN_", ""));
                        for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                            if (eff.id == id) {
                                eff.isVisible = false;
                                userEffectsList.add(eff);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing hidden effect ID: " + idStr, e);
                    }
                } else {
                    try {
                        int id = Integer.parseInt(idStr);
                        for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
                            if (eff.id == id) {
                                eff.isVisible = true;
                                userEffectsList.add(eff);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing effect ID: " + idStr, e);
                    }
                }
            }
            // Додати нові
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
            if (eff.isVisible) {
                if (!isCycleMode || eff.useInCycle) visibleEffects.add(eff);
            }
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
        if (seekBarSpeed != null) seekBarSpeed.setMax(effect.speedMax);

        if (seekBarScale == null || textScaleVal == null || labelScale == null || btnScaMinus == null || btnScaPlus == null) return;

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

            if (effect.scaleType == 1) { // Веселка
                labelScale.setText(R.string.label_color);
                updateScaleTextColor(seekBarScale.getProgress(), true);

                int[] colors = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
                GradientDrawable rainbow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                rainbow.setCornerRadius(20f);

                ClipDrawable progress = new ClipDrawable(new ColorDrawable(Color.TRANSPARENT), Gravity.LEFT, ClipDrawable.HORIZONTAL);
                LayerDrawable layers = new LayerDrawable(new Drawable[]{rainbow, progress});
                layers.setId(0, android.R.id.background);
                layers.setId(1, android.R.id.progress);

                seekBarScale.setProgressDrawable(layers);
                if (seekBarScale.getThumb() != null) seekBarScale.getThumb().setTint(0xFFFFFFFF);
            } else {
                labelScale.setText(R.string.label_scale);
                updateScaleTextColor(seekBarScale.getProgress(), false);
                applySliderStyle();
            }
        }
    }

    private void updateScaleTextColor(int progress, boolean isRainbowMode) {
        if (textScaleVal == null) return;
        textScaleVal.setText(String.valueOf(progress));
        if (isRainbowMode) {
            float max = seekBarScale.getMax() > 0 ? seekBarScale.getMax() : 255f;
            float hue = (progress / max) * 360.0f;
            textScaleVal.setTextColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
        } else {
            textScaleVal.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_green));
        }
    }

    private void applySliderStyle() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        int style = prefs.getInt("slider_style", 0);
        SeekBar[] sliders = {seekBarBrightness, seekBarSpeed, seekBarScale};
        int colorNeon = androidx.core.content.ContextCompat.getColor(this, R.color.neon_green);

        for (SeekBar sb : sliders) {
            if (sb == null) continue;
            sb.setPadding(0, 0, 0, 0);
            sb.setThumbOffset(0);

            if (style == 1) { // Plasma
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_plasma));
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_transparent));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
            } else if (style == 2) { // Cyber
                sb.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.track_neon));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTint(colorNeon);
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_cyber));
                if (sb.getThumb() != null) sb.getThumb().setTint(colorNeon);
            } else if (style == 3) { // Gradient
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_gradient));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_cyber));
                if (sb.getThumb() != null) sb.getThumb().setTint(getResources().getColor(R.color.white));
            } else { // Neon
                sb.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.track_neon));
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_round));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
                if (sb.getThumb() != null) sb.getThumb().setTintList(null);
            }
        }
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else { v.vibrate(50); }
            }
        }
    }
}