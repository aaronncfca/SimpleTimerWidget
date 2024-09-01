package com.example.simpletimerwidget;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
        EdgeToEdge.enable(this); // TODO: disable?
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.textView);
//        TimePicker tp = findViewById(R.id.timePicker);
//        tp.setIs24HourView(true);
//        tp.setOnTimeChangedListener(this);

        timer = new Timer(60000) {
            @Override
            public void onTick(long secondsUntilFinished) {
                setTimeView(secondsUntilFinished);
            }

            @Override
            public void onFinish() {
                textView.setText("00:00:00");
            }

            @Override
            public void onReset(long seconds) { onTimerReset(seconds); }
        };
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
    }

    private void onTimerReset(long seconds) {
        textView.setBackgroundColor(0x20808080); // 50% gray at 13% opacity
        setTimeView(seconds);
    }

    private void setTimeView(long seconds) {
        textView.setText(TimerService.formatTimeLeft(seconds));
    }

    public void startTimer(View view) {
        textView.setBackgroundColor(Color.TRANSPARENT);
        timer.Start();
    }

    public void resetTimer (View view) {
        timer.Reset();
        setTimeView(timer.GetCurrMs()/1000);
    }

    public void pauseTimer (View view) {
        timer.Pause();
    }

    public void timeClicked (View view) {
        // Don't allow setting the timer if currently ticking.
        if(timer.IsStarted()) return;

        MyTimePicker timePicker = new MyTimePicker();
        timePicker.setTitle("Set timer");
        //timePicker.includeHours = false
        timePicker.setOnTimeSetOption("Set", (hour, minute, second) ->  {
            timer.Set((hour*60*60 + minute*60 + second)*1000L);
            return null;
        });

            /* To show the dialog you have to supply the "fragment manager"
                and a tag (whatever you want)
            */
        timePicker.show(getSupportFragmentManager(), "time_picker");

    }

    @Override
    public void onTimeChanged (TimePicker tp, int hour, int min) {
        int seconds = hour * 60 * 60 + min * 60;
        timer.Set(seconds * 1000L);
    }
}