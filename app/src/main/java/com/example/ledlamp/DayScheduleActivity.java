package com.example.ledlamp;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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

        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            View row = inflater.inflate(R.layout.item_schedule_slot, slotsContainer, false);

            LinearLayout layoutRoot = row.findViewById(R.id.layoutSlotRoot);
            CheckBox checkEnable = row.findViewById(R.id.checkSlotEnable);
            TextView tvTime = row.findViewById(R.id.textSlotTime);
            RadioGroup radioGroup = row.findViewById(R.id.radioGroupAction);
            RadioButton radioOn = row.findViewById(R.id.radioOn);
            RadioButton radioOff = row.findViewById(R.id.radioOff);

            int h = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][0];
            int m = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][1];
            int act = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2];

            tvTime.setText(String.format(Locale.US, "%02d:%02d", h, m));

            checkEnable.setChecked(act != 0);
            if (act == 2) radioOff.setChecked(true);
            else radioOn.setChecked(true);

            updateSlotUI(layoutRoot, checkEnable.isChecked(), radioOn.isChecked(), radioOn, radioOff);

            checkEnable.setOnCheckedChangeListener((bv, isChecked) -> {
                vibrate();
                int newAct = isChecked ? (radioOn.isChecked() ? 1 : 2) : 0;
                WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2] = newAct;
                updateSlotUI(layoutRoot, isChecked, radioOn.isChecked(), radioOn, radioOff);
            });

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkEnable.isChecked()) {
                    vibrate();
                    int newAct = (checkedId == R.id.radioOn) ? 1 : 2;
                    WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2] = newAct;
                    updateSlotUI(layoutRoot, true, checkedId == R.id.radioOn, radioOn, radioOff);
                }
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

    private void updateSlotUI(View root, boolean enabled, boolean isOn, View rOn, View rOff) {
        if (!enabled) {
            root.setBackgroundResource(R.drawable.bg_item_slot_none);
            root.setAlpha(0.5f);
            rOn.setEnabled(false);
            rOff.setEnabled(false);
        } else {
            root.setAlpha(1.0f);
            rOn.setEnabled(true);
            rOff.setEnabled(true);
            root.setBackgroundResource(isOn ? R.drawable.bg_item_slot_on : R.drawable.bg_item_slot_off);
        }
    }

    private void vibrate() {
        SharedPreferences prefs = getSharedPreferences("LampAppPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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
