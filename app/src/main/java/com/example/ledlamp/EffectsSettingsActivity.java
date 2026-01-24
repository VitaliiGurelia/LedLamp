package com.example.ledlamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;

public class EffectsSettingsActivity extends BaseActivity {

    RecyclerView recyclerView;
    Button btnBack;
    EffectsRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effects_settings);

        recyclerView = findViewById(R.id.recyclerViewEffects);
        btnBack = findViewById(R.id.btnBack);

        MainActivity.reloadUserList(this);

        adapter = new EffectsRecyclerAdapter(this, MainActivity.userEffectsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Додаємо підтримку Drag and Drop
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                Collections.swap(MainActivity.userEffectsList, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Свайпи не використовуємо
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                // Візуальний ефект при виборі (підсвітка)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.7f);
                    viewHolder.itemView.setScaleX(1.02f);
                    viewHolder.itemView.setScaleY(1.02f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Скидаємо візуальні ефекти після того, як відпустили
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                saveOrder(); // Зберігаємо новий порядок
            }
        });

        touchHelper.attachToRecyclerView(recyclerView);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                saveOrder();
                finish();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveOrder();
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MainActivity.userEffectsList.size(); i++) {
            EffectEntity eff = MainActivity.userEffectsList.get(i);
            if (!eff.isVisible) sb.append("HIDDEN_");
            sb.append(eff.id);
            if (i < MainActivity.userEffectsList.size() - 1) sb.append(",");
        }
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("USER_EFFECTS_ORDER", sb.toString()).apply();
    }

    // --- АДАПТЕР RECYCLERVIEW ---
    static class EffectsRecyclerAdapter extends RecyclerView.Adapter<EffectsRecyclerAdapter.EffectViewHolder> {
        private final Context context;
        private final ArrayList<EffectEntity> list;

        public EffectsRecyclerAdapter(Context context, ArrayList<EffectEntity> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public EffectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_effect_manage, parent, false);
            return new EffectViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EffectViewHolder holder, int position) {
            EffectEntity item = list.get(position);
            holder.tvName.setText(item.getLocalizedName());
            holder.tvName.setAlpha(item.isVisible ? 1.0f : 0.5f);
            
            holder.checkBox.setOnCheckedChangeListener(null); // Скидаємо, щоб уникнути багів при переробці
            holder.checkBox.setChecked(item.isVisible);
            
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isVisible = isChecked;
                holder.tvName.setAlpha(isChecked ? 1.0f : 0.5f);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class EffectViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            CheckBox checkBox;

            public EffectViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.textEffectName);
                checkBox = itemView.findViewById(R.id.checkVisible);
                
                // Приховуємо старі кнопки стрілок, якщо вони є в макеті
                View btnUp = itemView.findViewById(R.id.btnMoveUp);
                View btnDown = itemView.findViewById(R.id.btnMoveDown);
                if (btnUp != null) btnUp.setVisibility(View.GONE);
                if (btnDown != null) btnDown.setVisibility(View.GONE);
            }
        }
    }
}
