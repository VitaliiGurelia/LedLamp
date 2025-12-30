package com.example.ledlamp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
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

        String[] actions = {
                getString(R.string.action_none),
                getString(R.string.action_on),
                getString(R.string.action_off)
        };

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            View row = inflater.inflate(R.layout.item_schedule_slot, slotsContainer, false);

            TextView tvNum = row.findViewById(R.id.textSlotNum);
            TextView tvTime = row.findViewById(R.id.textSlotTime);
            Spinner spinner = row.findViewById(R.id.spinnerSlotAction);

            tvNum.setText(getString(R.string.slot_label, i + 1));

            int h = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][0];
            int m = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][1];
            int act = WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2];

            tvTime.setText(String.format(Locale.US, "%02d:%02d", h, m));

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, actions);
            adapter.setDropDownViewResource(R.layout.spinner_item);
            spinner.setAdapter(adapter);
            if (act < 3) spinner.setSelection(act);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    WeeklyScheduleActivity.scheduleData[currentDayIndex][slotIndex][2] = position;
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            tvTime.setOnClickListener(v -> {
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

        btnBack.setOnClickListener(v -> finish());
    }
}