package com.example.ledlamp;

import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        TextView tvTitle = findViewById(R.id.textHelpTitle);
        TextView tvContent = findViewById(R.id.textHelpContent);

        // --- ЛОГІКА УНІВЕРСАЛЬНОСТІ ---
        // 1. Отримуємо дані з Інтента (посилки)
        // Якщо даних немає (0), використовуємо стандартні (Секретні команди)
        int titleResId = getIntent().getIntExtra("TITLE_ID", R.string.title_help);
        int textResId = getIntent().getIntExtra("TEXT_ID", R.string.text_help_content);

        // 2. Встановлюємо текст
        if (tvTitle != null) {
            tvTitle.setText(titleResId);
        }

        if (tvContent != null) {
            // Використовуємо Html.fromHtml для підтримки жирного шрифту і переносів
            tvContent.setText(Html.fromHtml(getString(textResId), Html.FROM_HTML_MODE_COMPACT));
        }

        // Кнопка Назад
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}