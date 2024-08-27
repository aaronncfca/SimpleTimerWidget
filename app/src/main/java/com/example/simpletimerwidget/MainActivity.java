package com.example.simpletimerwidget;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity implements TimePicker.OnTimeChangedListener {

    private TextView textView;

    // TODO: make global to application, not to activity.
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.textView);
        TimePicker tp = findViewById(R.id.timePicker);
        tp.setIs24HourView(true);
        tp.setOnTimeChangedListener(this);

        timer = new Timer(60000) {
            @Override
            public void onTick(long secondsUntilFinished) {
                setTimeView(secondsUntilFinished);
            }

            @Override
            public void onFinish() {
                textView.setText("00:00:00");
            }
        };
    }

    private void setTimeView(long seconds) {
        NumberFormat f = new DecimalFormat("00");
        long hour = (seconds / 60 / 60) % 24;
        long min = (seconds / 60) % 60;
        long sec = seconds % 60;

        String text = "";
        // TODO: use a method that takes locale into account?
        if (hour > 0) {
            text += hour + ":" + f.format(min) + ":";
        } else if (min > 0) {
            text += min + ":";
        }
        text += f.format(sec);

        textView.setText(text);
    }

    public void startTimer(View view) {
        timer.Start();
    }

    public void resetTimer (View view) {
        timer.Reset();
        setTimeView(timer.GetCurrMs()/1000);
    }

    public void pauseTimer (View view) {
        timer.Pause();
    }

    @Override
    public void onTimeChanged (TimePicker tp, int hour, int min) {
        int seconds = hour * 60 * 60 + min * 60;
        timer.Set(seconds * 1000L);
        setTimeView(seconds);
    }
}