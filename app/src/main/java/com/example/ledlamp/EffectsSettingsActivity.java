package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;

public class EffectsSettingsActivity extends BaseActivity {

    ListView listView;
    Button btnBack;
    EffectsManagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effects_settings);

        listView = findViewById(R.id.listEffectsPreview);
        btnBack = findViewById(R.id.btnBack);

        // Завантажуємо актуальний список
        MainActivity.reloadUserList(this);

        // Підключаємо адаптер
        adapter = new EffectsManagerAdapter(this, MainActivity.userEffectsList);
        listView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> {
            saveOrder(); // Зберігаємо при виході
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveOrder(); // Зберігаємо при згортанні
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MainActivity.userEffectsList.size(); i++) {
            EffectEntity eff = MainActivity.userEffectsList.get(i);

            // Якщо прихований, додаємо префікс
            if (!eff.isVisible) sb.append("HIDDEN_");

            sb.append(eff.id);
            if (i < MainActivity.userEffectsList.size() - 1) sb.append(",");
        }

        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("USER_EFFECTS_ORDER", sb.toString()).apply();
    }

    // --- АДАПТЕР СПИСКУ ---
    class EffectsManagerAdapter extends ArrayAdapter<EffectEntity> {
        public EffectsManagerAdapter(Context context, ArrayList<EffectEntity> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_effect_manage, parent, false);
            }

            EffectEntity item = getItem(position);

            TextView tvName = convertView.findViewById(R.id.textEffectName);
            CheckBox checkBox = convertView.findViewById(R.id.checkVisible);
            ImageButton btnUp = convertView.findViewById(R.id.btnMoveUp);
            ImageButton btnDown = convertView.findViewById(R.id.btnMoveDown);

            // --- ВИПРАВЛЕННЯ ТУТ ---
            // Було: tvName.setText(item.nameUA);
            // Стало: Використовуємо метод, який сам обирає мову (UA/EN/DK)
            tvName.setText(item.getLocalizedName());
            // -----------------------

            checkBox.setChecked(item.isVisible);

            // Логіка галочки
            checkBox.setOnClickListener(v -> {
                item.isVisible = checkBox.isChecked();
                tvName.setAlpha(item.isVisible ? 1.0f : 0.5f);
            });
            tvName.setAlpha(item.isVisible ? 1.0f : 0.5f);

            // Логіка ВГОРУ
            btnUp.setOnClickListener(v -> {
                if (position > 0) {
                    Collections.swap(MainActivity.userEffectsList, position, position - 1);
                    notifyDataSetChanged();
                }
            });

            // Логіка ВНИЗ
            btnDown.setOnClickListener(v -> {
                if (position < MainActivity.userEffectsList.size() - 1) {
                    Collections.swap(MainActivity.userEffectsList, position, position + 1);
                    notifyDataSetChanged();
                }
            });

            // Ховаємо стрілки на краях
            btnUp.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
            btnDown.setVisibility(position == getCount() - 1 ? View.INVISIBLE : View.VISIBLE);

            return convertView;
        }
    }
}