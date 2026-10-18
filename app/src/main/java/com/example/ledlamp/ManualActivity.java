package com.example.ledlamp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class ManualActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        // 1. Підключення
        setupButton(R.id.btnMan1, R.string.title_manual_1, R.string.text_manual_1);

        // 2. Робота з додатком
        setupButton(R.id.btnMan2, R.string.title_manual_2, R.string.text_manual_2);

        // 3. Керування кнопкою
        setupButton(R.id.btnMan3, R.string.title_manual_3, R.string.text_manual_3);

        // 4. Декілька телефонів
        setupButton(R.id.btnMan4, R.string.title_manual_4, R.string.text_manual_4);

        // 5. Команди
        setupButton(R.id.btnMan5, R.string.title_manual_5, R.string.text_help_content);

        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    private void setupButton(int btnId, int titleRes, int textRes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                vibrate();
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra("TITLE_ID", titleRes);
                intent.putExtra("TEXT_ID", textRes);
                startActivity(intent);
            });
        }
    }
}
