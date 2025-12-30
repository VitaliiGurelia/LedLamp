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
import android.widget.EditText;
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {

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
    String currentIp = "";

    // Адаптер для ефектів
    ArrayAdapter<EffectEntity> effectsAdapter;
    // Список видимих ефектів (відфільтрований)
    ArrayList<EffectEntity> visibleEffects = new ArrayList<>();

    // Для логіки
    private int lastReportedHiddenId = -1;
    boolean isProgrammaticChange = false;

    // Для слухача UDP
    private boolean isListening = false;
    private Thread listenerThread;
    private long lastUpdate = 0;
    private static final int LAMP_PORT = 8888;
    boolean isUserAction = true;

    // --- БАЗА ДАНИХ ЕФЕКТІВ ---
    public static final ArrayList<EffectEntity> EFFECTS_DB = new ArrayList<>();
    public static ArrayList<EffectEntity> userEffectsList = new ArrayList<>();

    static {
        // --- БАЗОВІ ---
        EFFECTS_DB.add(new EffectEntity(0, "Біле світло", "White Light", "Hvidt lys", 9, 207, 26, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(1, "Колір", "Color", "Farve", 9, 180, 99, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(2, "Зміна кольору", "Color Change", "Farveskift", 10, 252, 32, 1, 255, 1, 255, 0));

        // --- ГРУПА ВОГНІ ---
        EFFECTS_DB.add(new EffectEntity(18, "Вогонь 1", "Fire 1", "Ild 1", 22, 53, 3, 1, 255, 0, 255, 1));
        EFFECTS_DB.add(new EffectEntity(19, "Вогонь 2", "Fire 2", "Ild 2", 9, 51, 11, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(39, "Вогонь 3", "Fire 3", "Ild 3", 9, 225, 59, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(40, "Вогонь 4", "Fire 4", "Ild 4", 57, 225, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(41, "Вогонь 5", "Fire 5", "Ild 5", 9, 220, 20, 120, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(42, "Вогонь 6", "Fire 6", "Ild 6", 22, 225, 1, 99, 252, 1, 100, 1));

        // --- ІНШІ ЕФЕКТИ ---
        EFFECTS_DB.add(new EffectEntity(43, "Вихори полум'я", "Fire Whirls", "Ildhvirvler", 9, 240, 1, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(45, "Магма", "Magma", "Magma", 9, 198, 20, 150, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(46, "Кипіння", "Boiling", "Kogning", 7, 240, 18, 170, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(3, "Безумство", "Madness", "Galskab", 11, 33, 58, 1, 150, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(4, "Хмари", "Clouds", "Skyer", 8, 4, 34, 1, 15, 1, 50, 0));
        EFFECTS_DB.add(new EffectEntity(5, "Лава", "Lava", "Lava", 8, 9, 24, 5, 60, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(6, "Плазма", "Plasma", "Plasma", 11, 19, 59, 1, 30, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(7, "Веселка 3D", "Rainbow 3D", "Regnbue 3D", 11, 13, 60, 1, 70, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(8, "Павич", "Peacock", "Påfugl", 11, 5, 12, 1, 15, 1, 30, 0));
        EFFECTS_DB.add(new EffectEntity(9, "Зебра", "Zebra", "Zebra", 7, 8, 21, 1, 30, 7, 40, 0));
        EFFECTS_DB.add(new EffectEntity(10, "Ліс", "Forest", "Skov", 7, 8, 95, 2, 30, 70, 100, 0));
        EFFECTS_DB.add(new EffectEntity(11, "Океан", "Ocean", "Ocean", 7, 6, 12, 2, 15, 4, 30, 0));
        EFFECTS_DB.add(new EffectEntity(12, "М'ячики", "Balls", "Bolde", 24, 255, 26, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(13, "М'ячики без кордонів", "Bounce", "Bolde uden grænser", 18, 11, 70, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(14, "Попкорн", "Popcorn", "Popcorn", 19, 32, 16, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(15, "Спіралі", "Spirals", "Spiraler", 9, 46, 3, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(16, "Призмата", "Prismata", "Prismata", 17, 100, 2, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(17, "Димові шашки", "Smokeballs", "Røgkugler", 12, 44, 17, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(20, "Тихий океан", "Pacific", "Stillehavet", 55, 127, 100, 1, 255, 100, 100, 2));
        EFFECTS_DB.add(new EffectEntity(21, "Тіні", "Shadows", "Skygger", 39, 77, 1, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(22, "ДНК", "DNA", "DNA", 15, 77, 95, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(23, "Зграя", "Flock", "Flok", 15, 136, 4, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(24, "Зграя і хижак", "Flock & Predator", "Flok og rovdyr", 15, 128, 80, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(25, "Метелики", "Butterflies", "Sommerfugle", 11, 53, 87, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(26, "Лампа з метеликами", "Lamp w/ Butterflies", "Lampe med sommerfugle", 7, 61, 100, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(27, "Змійки", "Snakes", "Slanger", 9, 96, 31, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(28, "Nexus", "Nexus", "Nexus", 19, 60, 20, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(29, "Кулі", "Spheres", "Kugler", 9, 85, 85, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(30, "Синусоїд", "Sinusoid", "Sinusoide", 7, 89, 83, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(31, "Метаболз", "Metaballs", "Metabolde", 7, 85, 3, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(32, "Північне сяйво", "Aurora", "Nordlys", 12, 73, 38, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(33, "Плазмова лампа", "Plasma Lamp", "Plasmaler", 8, 59, 18, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(34, "Лавова лампа", "Lava Lamp", "Lavalampe", 23, 203, 1, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(35, "Рідка лампа", "Liquid Lamp", "Flydende lampe", 11, 63, 1, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(36, "Рідка лампа (авто)", "Liquid Auto", "Flydende auto", 11, 124, 39, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(37, "Краплі на склі", "Drops", "Dråber", 23, 71, 59, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(38, "Матриця", "Matrix", "Matrix", 27, 186, 23, 99, 240, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(44, "Різнокольорові вихори", "Multi Whirls", "Farvede hvirvler", 9, 240, 86, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(47, "Водоспад", "Waterfall", "Vandfald", 5, 212, 54, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(48, "Водоспад 4в1", "Waterfall 4in1", "Vandfald 4i1", 7, 197, 22, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(49, "Басейн", "Pool", "Pool", 8, 222, 63, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(50, "Пульс", "Pulse", "Puls", 12, 185, 6, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(51, "Райдужний пульс", "Rainbow Pulse", "Regnbuepuls", 11, 185, 31, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(52, "Білий пульс", "White Pulse", "Hvid puls", 9, 179, 11, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(53, "Осцилятор", "Oscillator", "Oscillator", 8, 208, 100, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(54, "Джерело", "Fountain", "Kilde", 15, 233, 77, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(55, "Фея", "Fairy", "Fe", 19, 212, 44, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(56, "Комета", "Comet", "Komet", 16, 220, 28, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(57, "Одноколірна комета", "Color Comet", "Farvet komet", 14, 212, 69, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(58, "Дві комети", "Two Comets", "To kometer", 27, 186, 19, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(59, "Три комети", "Three Comets", "Tre kometer", 24, 186, 9, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(60, "Тяжіння", "Attract", "Tiltrækning", 21, 203, 65, 160, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(61, "Ширяючий вогонь", "Firefly", "Svævende ild", 26, 206, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(62, "Верховий вогонь", "Firefly Top", "Top ild", 26, 190, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(63, "Райдужний змій", "Snake", "Regnbueslange", 12, 178, 1, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(64, "Конфетті", "Sparkles", "Konfetti", 16, 142, 63, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(65, "Мерехтіння", "Twinkles", "Flimren", 25, 236, 4, 60, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(66, "Дим", "Smoke", "Røg", 9, 157, 100, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(67, "Різнокольоровий дим", "Color Smoke", "Farvet røg", 9, 157, 30, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(68, "Пікассо", "Picasso", "Picasso", 9, 189, 43, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(69, "Хвилі", "Waves", "Bølger", 9, 236, 80, 220, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(70, "Кольорові драже", "Sand", "Farvede piller", 9, 195, 80, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(71, "Кодовий замок", "Rings", "Kodelås", 10, 222, 92, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(72, "Кубик Рубіка", "Cube 2D", "Rubiks terning", 10, 231, 89, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(73, "Хмарка в банці", "Simple Rain", "Sky i krukke", 30, 233, 2, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(74, "Гроза в банці", "Stormy Rain", "Tordenvejr", 20, 236, 25, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(75, "Опади", "Color Rain", "Nedbør", 15, 198, 99, 99, 252, 0, 255, 1));
        EFFECTS_DB.add(new EffectEntity(76, "Різнокольоровий дощ", "Rain", "Farvet regn", 15, 225, 1, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(77, "Снігопад", "Snow", "Snefald", 9, 180, 90, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(78, "Зорепад / Заметіль", "Starfall", "Stjerneskud", 20, 199, 54, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(79, "Стрибуни", "Leapers", "Springere", 24, 203, 5, 150, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(80, "Світлячки", "Lighters", "Ildfluer", 15, 157, 23, 50, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(81, "Світлячки зі шлейфом", "Lighter Traces", "Ildfluespor", 21, 198, 93, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(82, "Люмен'єр", "Lumenjer", "Lumenjer", 14, 223, 40, 1, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(83, "Пейнтбол", "Paintball", "Paintball", 11, 236, 7, 215, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(84, "Веселка", "Rainbow Ver", "Regnbue", 8, 196, 56, 50, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(85, "Годинник", "Clock", "Ur", 4, 5, 100, 1, 245, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(86, "Прапор України", "Flag UA", "Ukraines flag", 120, 150, 50, 1, 255, 1, 100, 2));
        EFFECTS_DB.add(new EffectEntity(87, "Прапор Данії", "Flag DK", "Danmarks flag", 120, 150, 20, 1, 255, 1, 100, 2));
        EFFECTS_DB.add(new EffectEntity(88, "Малюнок", "Drawing", "Tegning", 10, 5, 1, 1, 255, 0, 255, 2)); // Type 2 (без масштабу)
        EFFECTS_DB.add(new EffectEntity(89, "Бігучий рядок", "Running Text", "Løbende tekst", 10, 99, 38, 1, 252, 1, 100, 1));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        btnMenu.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        lampAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, lampList);
        lampAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerLamps.setAdapter(lampAdapter);

        spinnerLamps.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Lamp selectedLamp = lampList.get(position);
                currentIp = selectedLamp.ip;
                getSharedPreferences("LampSettings", MODE_PRIVATE)
                        .edit().putString("LAST_LAMP_IP", currentIp)
                        .putString("LAMP_IP", currentIp).apply();
                checkLampStatusAndRevertIfNeeded();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        effectsAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, visibleEffects);
        effectsAdapter.setDropDownViewResource(R.layout.spinner_item);
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
                sendUdpCommand("EFF " + effect.id);
                updateInterfaceForEffect(effect);
                getSharedPreferences("LampSettings", MODE_PRIVATE).edit().putInt("LAST_EFFECT_ID", effect.id).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnPrev.setOnClickListener(v -> {
            vibrate();
            int current = spinnerEffects.getSelectedItemPosition();
            if (current > 0) spinnerEffects.setSelection(current - 1);
            else spinnerEffects.setSelection(visibleEffects.size() - 1);
        });

        btnNext.setOnClickListener(v -> {
            vibrate();
            int current = spinnerEffects.getSelectedItemPosition();
            if (current < visibleEffects.size() - 1) spinnerEffects.setSelection(current + 1);
            else spinnerEffects.setSelection(0);
        });

        btnReset.setOnClickListener(v -> {
            vibrate();
            if (spinnerEffects.getSelectedItem() != null) {
                EffectEntity currentEffect = (EffectEntity) spinnerEffects.getSelectedItem();
                sendUdpCommand("BRI " + currentEffect.defBright);
                sendUdpCommand("SPD " + currentEffect.defSpeed);
                sendUdpCommand("SCA " + currentEffect.defScale);
                seekBarBrightness.setProgress(currentEffect.defBright);
                seekBarSpeed.setProgress(currentEffect.defSpeed);
                seekBarScale.setProgress(currentEffect.defScale);
                Toast.makeText(this, R.string.msg_reset_done, Toast.LENGTH_SHORT).show();
            }
        });

        switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrate();
            if (isProgrammaticChange) {
                isProgrammaticChange = false;
                return;
            }
            if (isChecked) {
                sendUdpCommand("P_ON");
                checkLampStatusAndRevertIfNeeded();
            } else {
                sendUdpCommand("P_OFF");
            }
        });

        switchCycle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrate();
            sendUdpCommand(isChecked ? "CYC 1" : "CYC 0");
            updateEffectsSpinnerList();
        });

        setupSeekBar(seekBarBrightness, textBriVal, "BRI");
        setupSeekBar(seekBarSpeed, textSpeedVal, "SPD");
        setupSeekBar(seekBarScale, textScaleVal, "SCA");

        setupPlusMinusButtons(btnBriMinus, btnBriPlus, seekBarBrightness, "BRI");
        setupPlusMinusButtons(btnSpdMinus, btnSpdPlus, seekBarSpeed, "SPD");
        setupPlusMinusButtons(btnScaMinus, btnScaPlus, seekBarScale, "SCA");

        // --- СПЕЦІАЛЬНИЙ ОБРОБНИК ДЛЯ ШКАЛИ МАСШТАБУ/КОЛЬОРУ ---
        // Ми перезаписуємо стандартний, щоб додати зміну кольору тексту
        seekBarScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 1. Перевіряємо, чи це режим "Веселка" (за текстом заголовка або поточному ефекту)
                // Найпростіше: перевірити заголовок, бо ми його міняємо в updateInterfaceForEffect
                String label = labelScale.getText().toString();
                // Або краще: взяти поточний ефект зі спінера
                boolean isRainbow = false;
                if (spinnerEffects.getSelectedItem() != null) {
                    EffectEntity currentEffect = (EffectEntity) spinnerEffects.getSelectedItem();
                    isRainbow = (currentEffect.scaleType == 1);
                }

                // 2. Оновлюємо колір тексту
                updateScaleTextColor(progress, isRainbow);

                // 3. Стандартна логіка відправки
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

    private void setupPlusMinusButtons(Button btnMinus, Button btnPlus, SeekBar seekBar, String cmdPrefix) {
        btnMinus.setOnClickListener(v -> {
            vibrate();
            changeSliderValue(seekBar, -1, cmdPrefix);
        });
        btnPlus.setOnClickListener(v -> {
            vibrate();
            changeSliderValue(seekBar, 1, cmdPrefix);
        });
    }

    private void changeSliderValue(SeekBar seekBar, int delta, String cmdPrefix) {
        if (seekBar.getVisibility() != View.VISIBLE || !seekBar.isEnabled()) return;
        int val = seekBar.getProgress() + delta;
        if (val < 0) val = 0;
        if (val > seekBar.getMax()) val = seekBar.getMax();
        seekBar.setProgress(val);
        sendUdpCommand(cmdPrefix + " " + val);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLamps();
        applySliderStyle();

        reloadUserList(this);
        updateEffectsSpinnerList();

        isListening = true;
        startUdpListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isListening = false;
    }

    private void updateEffectsSpinnerList() {
        boolean isCycleMode = switchCycle.isChecked();
        visibleEffects.clear();
        for (EffectEntity eff : userEffectsList) {
            if (eff.isVisible) {
                if (!isCycleMode || eff.useInCycle) {
                    visibleEffects.add(eff);
                }
            }
        }
        if (effectsAdapter != null) effectsAdapter.notifyDataSetChanged();

        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        int lastId = prefs.getInt("LAST_EFFECT_ID", 0);
        for (int i = 0; i < visibleEffects.size(); i++) {
            if (visibleEffects.get(i).id == lastId) {
                spinnerEffects.setSelection(i);
                break;
            }
        }
    }

    private void setupSeekBar(SeekBar seekBar, TextView textView, String commandPrefix) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(String.valueOf(progress));
                if (fromUser) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdate > 50) {
                        sendUdpCommand(commandPrefix + " " + progress);
                        lastUpdate = currentTime;
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                sendUdpCommand(commandPrefix + " " + seekBar.getProgress());
            }
        });
    }

    private void sendUdpCommand(String command) {
        if (currentIp.isEmpty()) return;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                InetAddress address = InetAddress.getByName(currentIp);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadLamps() {
        SharedPreferences prefs = getSharedPreferences("LampSettings", MODE_PRIVATE);
        String json = prefs.getString("LAMPS_JSON", "[]");
        String lastIp = prefs.getString("LAST_LAMP_IP", "");

        lampList.clear();
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                lampList.add(new Lamp(obj.getString("n"), obj.getString("i")));
            }
        } catch (Exception e) {}

        if (lampList.isEmpty()) {
            lampList.add(new Lamp("Demo Lamp", "192.168.0.105"));
        }

        if (lampAdapter != null) lampAdapter.notifyDataSetChanged();

        if (!lastIp.isEmpty()) {
            for (int i = 0; i < lampList.size(); i++) {
                if (lampList.get(i).ip.equals(lastIp)) {
                    spinnerLamps.setSelection(i);
                    currentIp = lastIp;
                    break;
                }
            }
        } else if (!lampList.isEmpty()) {
            currentIp = lampList.get(0).ip;
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

    private void checkLampStatusAndRevertIfNeeded() {
        if (currentIp.isEmpty()) return;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(2000);
                byte[] data = "GET".getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(currentIp), LAMP_PORT);
                socket.send(packet);
                byte[] buf = new byte[1024];
                DatagramPacket r = new DatagramPacket(buf, buf.length);
                socket.receive(r);
                socket.close();
            } catch (SocketTimeoutException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, R.string.msg_lamp_not_found, Toast.LENGTH_SHORT).show();
                    isProgrammaticChange = true;
                    switchPower.setChecked(false);
                    vibrate();
                });
            } catch (Exception e) {}
        }).start();
    }

    private void applySliderStyle() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        int style = prefs.getInt("slider_style", 0);
        SeekBar[] sliders = {seekBarBrightness, seekBarSpeed, seekBarScale};
        int colorNeon = getResources().getColor(R.color.neon_green);

        for (SeekBar sb : sliders) {
            if (sb == null) continue;
            sb.setPadding(0, 0, 0, 0);
            sb.setThumbOffset(0);

            if (style == 1) { // Plasma
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_plasma));
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_transparent));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
            } else if (style == 2) { // Cyber
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_neon));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTint(colorNeon);
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_cyber));
                if (sb.getThumb() != null) sb.getThumb().setTint(colorNeon);
            } else if (style == 3) { // Gradient
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_gradient));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_cyber));
                if (sb.getThumb() != null) sb.getThumb().setTint(getResources().getColor(R.color.white));
            } else { // Neon
                sb.setProgressDrawable(getResources().getDrawable(R.drawable.track_neon));
                sb.setThumb(getResources().getDrawable(R.drawable.thumb_round));
                if (sb.getProgressDrawable() != null) sb.getProgressDrawable().setTintList(null);
                if (sb.getThumb() != null) sb.getThumb().setTintList(null);
            }
        }
    }

    private void updateInterfaceForEffect(EffectEntity effect) {
        seekBarSpeed.setMax(effect.speedMax);

        if (effect.scaleType == 2) {
            // ТИП 2: Приховати
            seekBarScale.setVisibility(View.INVISIBLE);
            textScaleVal.setVisibility(View.INVISIBLE);
            labelScale.setVisibility(View.INVISIBLE);
            btnScaMinus.setVisibility(View.INVISIBLE); // Ховаємо кнопки теж
            btnScaPlus.setVisibility(View.INVISIBLE);

        } else {
            // Показуємо
            seekBarScale.setVisibility(View.VISIBLE);
            textScaleVal.setVisibility(View.VISIBLE);
            labelScale.setVisibility(View.VISIBLE);
            btnScaMinus.setVisibility(View.VISIBLE);
            btnScaPlus.setVisibility(View.VISIBLE);

            seekBarScale.setMax(effect.scaleMax);

            // Встановлюємо правильний колір тексту відразу
            boolean isRainbow = (effect.scaleType == 1);
            updateScaleTextColor(seekBarScale.getProgress(), isRainbow);

            if (isRainbow) {
                // ТИП 1: КОЛІР (ВЕСЕЛКА)
                labelScale.setText(R.string.label_color);

                // ... (тут ваш код GradientDrawable) ...
                int[] colors = new int[] { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 };
                GradientDrawable rainbow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                rainbow.setCornerRadius(20f);

                ClipDrawable transparentClip = new ClipDrawable(new ColorDrawable(Color.TRANSPARENT), Gravity.LEFT, ClipDrawable.HORIZONTAL);
                LayerDrawable layers = new LayerDrawable(new Drawable[]{rainbow, transparentClip});
                layers.setId(0, android.R.id.background);
                layers.setId(1, android.R.id.progress);
                seekBarScale.setProgressDrawable(layers);

                if (seekBarScale.getThumb() != null) seekBarScale.getThumb().setTint(0xFFFFFFFF);

            } else {
                // ТИП 0: МАСШТАБ
                labelScale.setText(R.string.label_scale);
                applySliderStyle();
            }
        }
    }

    private void startUdpListener() {
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(100);

                long lastPingTime = 0;
                InetAddress address = null;

                while (isListening) {
                    if (!currentIp.isEmpty() && address == null) {
                        try { address = InetAddress.getByName(currentIp); } catch (Exception e) {}
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastPingTime > 2000) {
                        if (address != null) {
                            try {
                                byte[] data = "GET".getBytes();
                                DatagramPacket packet = new DatagramPacket(data, data.length, address, LAMP_PORT);
                                socket.send(packet);
                            } catch (Exception e) {}
                        }
                        lastPingTime = now;
                    }
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        if (msg.startsWith("CUR")) {
                            parseCurPacket(msg);
                        }
                    } catch (SocketTimeoutException e) {
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
            } finally {
                if (socket != null) socket.close();
            }
        });
        listenerThread.start();
    }

    private void parseCurPacket(String msg) {
        try {
            String[] parts = msg.trim().split(" ");

            if (parts.length >= 2) {
                int modeId = Integer.parseInt(parts[1]);

                if (parts.length >= 6) {
                    int brightness = Integer.parseInt(parts[2]);
                    int speed = Integer.parseInt(parts[3]);
                    int scale = Integer.parseInt(parts[4]);
                    int state = Integer.parseInt(parts[5]);

                    boolean isCycleOn = false;
                    if (parts.length >= 11) {
                        try {
                            int cycleVal = Integer.parseInt(parts[10]);
                            isCycleOn = (cycleVal == 1);
                        } catch (Exception e) {}
                    }
                    boolean finalIsCycleOn = isCycleOn;

                    runOnUiThread(() -> {
                        if (switchPower.isChecked() != (state == 1)) {
                            isProgrammaticChange = true;
                            switchPower.setChecked(state == 1);
                        }

                        if (switchCycle.isChecked() != finalIsCycleOn) {
                            switchCycle.setOnCheckedChangeListener(null);
                            switchCycle.setChecked(finalIsCycleOn);
                            updateEffectsSpinnerList();
                            switchCycle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                vibrate();
                                sendUdpCommand(isChecked ? "CYC 1" : "CYC 0");
                                updateEffectsSpinnerList();
                            });
                        }

                        seekBarBrightness.setProgress(brightness);
                        seekBarSpeed.setProgress(speed);
                        seekBarScale.setProgress(scale);

                        textBriVal.setText(String.valueOf(brightness));
                        textSpeedVal.setText(String.valueOf(speed));
                        textScaleVal.setText(String.valueOf(scale));

                        int foundPosition = -1;
                        for (int i = 0; i < visibleEffects.size(); i++) {
                            if (visibleEffects.get(i).id == modeId) {
                                foundPosition = i;
                                break;
                            }
                        }

                        if (foundPosition != -1) {
                            if (spinnerEffects.getSelectedItemPosition() != foundPosition) {
                                isUserAction = false;
                                spinnerEffects.setSelection(foundPosition);
                            }
                            updateInterfaceForEffect(visibleEffects.get(foundPosition));
                            lastReportedHiddenId = -1;
                        } else {
                            for (EffectEntity eff : EFFECTS_DB) {
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
        } catch (Exception e) {}
    }
    // Метод відновлює список з пам'яті або створює з бази (викликається з інших Activity)
    public static void reloadUserList(Context context) {
        userEffectsList.clear();
        SharedPreferences prefs = context.getSharedPreferences("LampAppPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("USER_EFFECTS_ORDER", "");

        if (json.isEmpty()) {
            // Якщо налаштувань немає - просто копіюємо все з бази
            userEffectsList.addAll(EFFECTS_DB);
        } else {
            // Відновлюємо порядок
            String[] ids = json.split(",");
            for (String idStr : ids) {
                if (idStr.startsWith("HIDDEN_")) {
                    // Це прихований ефект
                    try {
                        int id = Integer.parseInt(idStr.replace("HIDDEN_", ""));
                        for (EffectEntity eff : EFFECTS_DB) {
                            if (eff.id == id) {
                                eff.isVisible = false;
                                userEffectsList.add(eff);
                                break;
                            }
                        }
                    } catch (Exception e) {}
                } else {
                    // Це видимий ефект
                    try {
                        int id = Integer.parseInt(idStr);
                        for (EffectEntity eff : EFFECTS_DB) {
                            if (eff.id == id) {
                                eff.isVisible = true;
                                userEffectsList.add(eff);
                                break;
                            }
                        }
                    } catch (Exception e) {}
                }
            }

            // Додаємо нові ефекти, яких не було в збереженому списку (наприклад, після оновлення)
            for (EffectEntity dbEff : EFFECTS_DB) {
                boolean found = false;
                for (EffectEntity userEff : userEffectsList) {
                    if (userEff.id == dbEff.id) {
                        found = true;
                        break;
                    }
                }
                if (!found) userEffectsList.add(dbEff);
            }
        }
    }
    // Функція, яка фарбує текст залежно від значення
    private void updateScaleTextColor(int progress, boolean isRainbowMode) {
        textScaleVal.setText(String.valueOf(progress));

        if (isRainbowMode) {
            // 1. Дізнаємося реальний максимум слайдера (він може бути 100 або 255)
            float max = (float) seekBarScale.getMax();
            if (max == 0) max = 255f; // Захист від ділення на нуль

            // 2. Рахуємо відсоток заповнення (від 0.0 до 1.0)
            float ratio = progress / max;

            // 3. Перетворюємо відсоток у повне коло кольорів (0..360 градусів)
            float hue = ratio * 360.0f;

            // 4. Отримуємо колір
            int color = Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});
            textScaleVal.setTextColor(color);
        } else {
            // Звичайний режим - зелений
            textScaleVal.setTextColor(getResources().getColor(R.color.neon_green));
        }
    }
}