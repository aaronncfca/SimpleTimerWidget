package com.example.simpletimerwidget;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    private BroadcastReceiver receiver;

    private long secondsSet = 60; // Modified when the user sets the timer.
    private boolean timerIsRunning = false;

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

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(TimerService.ACTION_STARTED.equals(action)) {
                    timerIsRunning = true;
                    // TODO: show pause icon, hide others
                } else if(TimerService.ACTION_PAUSED.equals(action)) {
                    timerIsRunning = false;
                    // TODO: hide pause icon, show resume and reset icons.
                } else if(TimerService.ACTION_RESET.equals(action)) {
                    long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, -1);
                    if(secondsLeft < 0) throw new IllegalArgumentException();
                    timerIsRunning = false;
                    onTimerReset(secondsLeft);
                } else if(TimerService.ACTION_EXPIRED.equals(action)) {
                    timerIsRunning = false;
                    // TODO: flash red, show "stop alarm" button, or something?
                } else if(TimerService.ACTION_TICK.equals(action)) {
                    long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, -1);
                    if(secondsLeft < 0) throw new IllegalArgumentException();
                    setTimeView(secondsLeft);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_STARTED);
        filter.addAction(TimerService.ACTION_PAUSED);
        filter.addAction(TimerService.ACTION_RESET);
        filter.addAction(TimerService.ACTION_EXPIRED);
        filter.addAction(TimerService.ACTION_TICK);
        // Calling ContextCompat to avoid warnings related to specifying whether the receiver
        // is exported.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
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

    private void startTimerService(String action, long extraSecondsLeft) {
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.setAction(action);
        if(extraSecondsLeft >= 0) {
            serviceIntent.putExtra(TimerService.EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    public void startTimer(View view) {
        textView.setBackgroundColor(Color.TRANSPARENT);
        startTimerService(TimerService.ACTION_START, secondsSet);
        timerIsRunning = true;
    }

    public void resetTimer (View view) {
        startTimerService(TimerService.ACTION_CANCEL, -1);
    }

    public void pauseTimer (View view) {
        startTimerService(TimerService.ACTION_PAUSE, -1);
    }

    public void timeClicked (View view) {
        // Don't allow setting the timer if currently ticking.
        if(timerIsRunning) return;

        MyTimePicker timePicker = new MyTimePicker();
        timePicker.setTitle("Set timer");
        //timePicker.includeHours = false
        timePicker.setOnTimeSetOption("Set", (hour, minute, second) ->  {
            secondsSet = (hour*60*60 + minute*60 + second);
            return null;
        });

        // To show the dialog you have to supply the "fragment manager"
        // and a tag (whatever you want)
        timePicker.show(getSupportFragmentManager(), "time_picker");

    }
}