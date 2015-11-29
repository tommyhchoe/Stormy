package com.example.tommychoe.stormy.ui;

import android.app.ListActivity;
import android.support.v7.app.AppCompatActivity;
import com.example.tommychoe.stormy.R;
import com.example.tommychoe.stormy.adapters.DayAdapter;
import com.example.tommychoe.stormy.models.Day;

import android.os.Bundle;
import android.widget.ArrayAdapter;

public class DailyForecastActivity extends ListActivity {

    private Day[] mDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        DayAdapter adapter = new DayAdapter(this, mDays);
    }
}
