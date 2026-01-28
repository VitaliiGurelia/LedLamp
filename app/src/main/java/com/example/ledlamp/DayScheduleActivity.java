package com.example.ledlamp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DayScheduleActivity extends BaseActivity {

    LinearLayout slotsContainer;
    TextView dayTitle;
    Button btnBack;
    int currentDayIndex;

    private final int[] daysRes = {
            R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
            R.string.day_fri, R.string.day_sat, R.string.day_sun
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_schedule);

        slotsContainer = findViewById(R.id.slotsContainer);
        dayTitle = findViewById(R.id.textDayTitle);
        btnBack = findViewById(R.id.btnBack);

        currentDayIndex = getIntent().getIntExtra("day_index", 0);
        String dayName = getString(daysRes[currentDayIndex]);
        dayTitle.setText(getString(R.string.title_day_edit, dayName));

        LayoutInflater inflater = LayoutInflater.from(this);

        // Підготовка списку ефектів з елементом "Без змін"
        List<String> effectNames = new ArrayList<>();
        effectNames.add(getString(R.string.effect_no_change)); // Додаємо "Без змін" першим
        for (EffectEntity eff : EffectsRepository.EFFECTS_DB) {
            effectNames.add(eff.getLocalizedName());
        }

        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            View row = inflater.inflate(R.layout.item_schedule_slot, slotsContainer, false);

            LinearLayout layoutRoot = row.findViewById(R.id.layoutSlotRoot);
            CheckBox checkEnable = row.findViewById(R.id.checkSlotEnable);
            TextView tvTime = row.findViewById(R.id.textSlotTime);
            RadioGroup radioGroup = row.findViewById(R.id.radioGroupAction);
            RadioButton radioOn = row.findViewById(R.id.radioOn);
            RadioButton radioOff = row.findViewById(R.id.radioOff);
            Spinner spinnerEffect = row.findViewById(R.id.spinnerSlotEffect);

            // Налаштування списку ефектів
            ArrayAdapter<String> effectAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, effectNames);
            effectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerEffect.setAdapter(effectAdapter);

            int h = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][0];
            int m = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][1];
            int act = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2];
            int effId = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][3];

            tvTime.setText(String.format(Locale.US, "%02d:%02d", h, m));
            checkEnable.setChecked(act != 0);
            if (act == 2) radioOff.setChecked(true);
            else radioOn.setChecked(true);

            // Встановлення ефекту в спінері (враховуючи зміщення через "Без змін")
            if (effId == 255) {
                spinnerEffect.setSelection(0);
            } else {
                for (int j = 0; j < EffectsRepository.EFFECTS_DB.size(); j++) {
                    if (EffectsRepository.EFFECTS_DB.get(j).id == effId) {
                        spinnerEffect.setSelection(j + 1);
                        break;
                    }
                }
            }

            updateSlotUI(layoutRoot, checkEnable.isChecked(), radioOn.isChecked(), radioOn, radioOff, spinnerEffect);

            checkEnable.setOnCheckedChangeListener((bv, isChecked) -> {
                vibrate();
                int newAct = isChecked ? (radioOn.isChecked() ? 1 : 2) : 0;
                WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2] = newAct;
                updateSlotUI(layoutRoot, isChecked, radioOn.isChecked(), radioOn, radioOff, spinnerEffect);
            });

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                vibrate();
                int newAct = (checkedId == R.id.radioOn) ? 1 : 2;
                WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2] = newAct;
                updateSlotUI(layoutRoot, checkEnable.isChecked(), checkedId == R.id.radioOn, radioOn, radioOff, spinnerEffect);
            });

            spinnerEffect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][3] = 255; // "Без змін"
                    } else {
                        WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][3] = EffectsRepository.EFFECTS_DB.get(position - 1).id;
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            tvTime.setOnClickListener(v -> {
                vibrate();
                int curH = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][0];
                int curM = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][1];
                new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                    WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][0] = hourOfDay;
                    WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][1] = minute;
                    tvTime.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
                }, curH, curM, true).show();
            });

            slotsContainer.addView(row);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                vibrate();
                finish();
            });
        }
    }

    private void updateSlotUI(View root, boolean enabled, boolean isOn, View rOn, View rOff, Spinner spinnerEffect) {
        if (!enabled) {
            root.setBackgroundResource(R.drawable.bg_item_slot_none);
            root.setAlpha(0.5f);
            rOn.setEnabled(false);
            rOff.setEnabled(false);
            spinnerEffect.setVisibility(View.GONE);
        } else {
            root.setAlpha(1.0f);
            rOn.setEnabled(true);
            rOff.setEnabled(true);
            if (isOn) {
                root.setBackgroundResource(R.drawable.bg_item_slot_on);
                spinnerEffect.setVisibility(View.VISIBLE);
                spinnerEffect.setEnabled(true);
            } else {
                root.setBackgroundResource(R.drawable.bg_item_slot_off);
                spinnerEffect.setVisibility(View.GONE);
            }
        }
    }
}
