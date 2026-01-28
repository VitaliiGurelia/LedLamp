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
        Button btnBack = findViewById(R.id.btnBack);

        // Отримуємо дані з Intent
        int titleRes = getIntent().getIntExtra("TITLE_ID", R.string.title_help);
        int textRes = getIntent().getIntExtra("TEXT_ID", R.string.text_help_content);

        if (tvTitle != null) tvTitle.setText(getString(titleRes));
        if (tvContent != null) {
            tvContent.setText(Html.fromHtml(getString(textRes), Html.FROM_HTML_MODE_COMPACT));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }
}
