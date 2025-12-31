package com.example.ledlamp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class ManualActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual); // Переконайтесь, що цей XML має 5 кнопок (btnMan1...btnMan5)

        // 1. Підключення
        setupButton(R.id.btnMan1, R.string.title_manual_1, R.string.text_manual_1);

        // 2. Додаток (Ваш новий текст)
        setupButton(R.id.btnMan2, R.string.title_manual_2, R.string.text_manual_2);

        // 3. Кнопка (Ваш текст про керування кнопкою)
        setupButton(R.id.btnMan3, R.string.title_manual_3, R.string.text_manual_3);

        // 4. Багато пристроїв (Ваш текст)
        setupButton(R.id.btnMan4, R.string.title_manual_4, R.string.text_manual_4);

        // 5. КОМАНДИ (Використовуємо вже існуючий текст "text_help_content")
        setupButton(R.id.btnMan5, R.string.title_manual_5, R.string.text_help_content);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // Метод, який запускає HelpActivity з потрібним текстом
    private void setupButton(int btnId, int titleRes, int textRes) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                Intent intent = new Intent(this, HelpActivity.class);
                intent.putExtra("TITLE_ID", titleRes); // Передаємо заголовок
                intent.putExtra("TEXT_ID", textRes);   // Передаємо сам текст
                startActivity(intent);
            });
        }
    }
}