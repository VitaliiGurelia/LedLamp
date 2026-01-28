package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {
    private int mCurrentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLanguage();
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        mCurrentTheme = prefs.getInt("app_theme", 0);
        applyTheme(mCurrentTheme);
        super.onCreate(savedInstanceState);
    }

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

    public void applyTheme(int theme) {
        if (theme == 1) setTheme(R.style.Theme_LedLamp_Light);
        else if (theme == 2) setTheme(R.style.Theme_LedLamp_Cyberpunk);
        else setTheme(R.style.Theme_LedLamp);
    }

    public void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(30);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getInt("app_theme", 0) != mCurrentTheme) {
            recreate();
        }
    }
}
