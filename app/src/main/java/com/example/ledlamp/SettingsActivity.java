package com.example.ledlamp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import java.util.Locale;

// Важливо: наслідуємося від BaseActivity, щоб підхоплювати поточну мову при вході
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // --- КНОПКА НАЗАД ---
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }

        // --- 1. З'ЄДНАННЯ ---
        setupMenuLink(R.id.menuConnection, ConnectionActivity.class);

        // --- 2. НАЛАШТУВАННЯ ДОДАТКУ ---
        setupMenuLink(R.id.menuAppSettings, AppSettingsActivity.class);

        // --- 3. ЕФЕКТИ ---
        setupMenuLink(R.id.menuEffects, EffectsSettingsActivity.class);

        // --- 4. ЦИКЛ ---
        setupMenuLink(R.id.menuCycle, CycleActivity.class);

        // --- 5. БУДИЛЬНИК ---
        setupMenuLink(R.id.menuDawn, DawnAlarmActivity.class);

        // --- 6. ТАЙМЕР ---
        setupMenuLink(R.id.menuTimer, TimerActivity.class);

        // --- 7. РОЗКЛАД ---
        setupMenuLink(R.id.menuSchedule, WeeklyScheduleActivity.class);

        // --- 8. МАЛЮВАННЯ ---
        setupMenuLink(R.id.menuPainting, PaintingActivity.class);

        // --- 9. ТЕКСТ ---
        setupMenuLink(R.id.menuText, TextActivity.class);

        // --- 10. МОВА (ПРАВИЛЬНА ЛОГІКА) ---
        View btnLang = findViewById(R.id.menuLanguage);

        // --- 11. ІНСТРУКЦІЯ ---
        setupMenuLink(R.id.menuManual, ManualActivity.class);

        if (btnLang != null) {
            btnLang.setOnClickListener(v -> {
                vibrate();
                showLanguageDialog(); // <--- Викликаємо наш діалог!
            });
        }
    }

    // Універсальний метод для кнопок
    private void setupMenuLink(int id, Class<?> activityClass) {
        View btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                vibrate();
                startActivity(new Intent(SettingsActivity.this, activityClass));
            });
        }
    }

    // --- ЛОГІКА ВИБОРУ МОВИ ---
    private void showLanguageDialog() {
        final String[] languages = {"English", "Українська", "Dansk"};
        final String[] codes = {"en", "uk", "da"};

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_language)
                .setItems(languages, (dialog, which) -> {
                    // Зберігаємо вибір
                    setLocale(codes[which]);

                    // Перезапускаємо додаток повністю, щоб застосувати мову
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Зберігаємо в пам'ять
        SharedPreferences.Editor editor = getSharedPreferences("LampAppPrefs", MODE_PRIVATE).edit();
        editor.putString("My_Lang", langCode);
        editor.apply();
    }

    // Функція вібрації
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