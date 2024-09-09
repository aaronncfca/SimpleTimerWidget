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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

        FragmentManager fragmentManager = getSupportFragmentManager();


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(TimerService.ACTION_STARTED.equals(action)) {
                    timerIsRunning = true;
                    // Switch to TimerControlsFragment. TODO: check if already on that fragment.
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.replace(R.id.fragmentContainerView, TimerControlsFragment.class, null);
                    ft.commit();
                } else if(TimerService.ACTION_PAUSED.equals(action)) {
                    timerIsRunning = false;
                    // TODO: hide pause icon, show resume and reset icons.
                } else if(TimerService.ACTION_RESET.equals(action)) {
                    long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, -1);
                    if(secondsLeft < 0) throw new IllegalArgumentException();
                    // Switch to SetTimerFragment
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.replace(R.id.fragmentContainerView, SetTimerFragment.class, SetTimerFragment.Args(secondsLeft));
                    ft.commit();
                    timerIsRunning = false;
                } else if(TimerService.ACTION_EXPIRED.equals(action)) {
                    // TODO: switch to TimerExpiredFragment
                    timerIsRunning = false;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_STARTED);
        filter.addAction(TimerService.ACTION_PAUSED);
        filter.addAction(TimerService.ACTION_RESET);
        filter.addAction(TimerService.ACTION_EXPIRED);
        // Calling ContextCompat to avoid warnings related to specifying whether the receiver
        // is exported.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        if(receiver != null) unregisterReceiver(receiver);
        super.onDestroy();
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
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

}