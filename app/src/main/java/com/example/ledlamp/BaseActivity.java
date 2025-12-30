package com.example.ledlamp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Цей код виконається ПЕРЕД запуском будь-якого екрану
        applyLanguage();
        applyTheme();
        super.onCreate(savedInstanceState);
    }

    // Метод оновлення мови
    public void applyLanguage() {
        SharedPreferences languagePrefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        String lang = languagePrefs.getString("My_Lang", "");
        if (!lang.equals("")) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            android.content.res.Configuration config = new android.content.res.Configuration();
            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }

    public void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        int theme = prefs.getInt("app_theme", 0);

        if (theme == 1) {
            setTheme(R.style.Theme_LedLamp_Light);
        } else if (theme == 2) {
            setTheme(R.style.Theme_LedLamp_Cyberpunk);
        } else {
            setTheme(R.style.Theme_LedLamp); // Темна (Default)
        }
    }

    // Оновлюємо також при відновленні (на випадок, якщо змінили в налаштуваннях і повернулись)
    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
    }
}