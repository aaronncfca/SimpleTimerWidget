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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    private BroadcastReceiver receiver;
    private Class<? extends Fragment> activeFragmentClass;

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
                Class<? extends Fragment> switchToFrag = activeFragmentClass;
                Bundle fragArgs = null;

                String action = intent.getAction();
                if(TimerService.ACTION_STARTED.equals(action)) {
                    timerIsRunning = true;
                    switchToFrag = TimerControlsFragment.class;
                } else if(TimerService.ACTION_PAUSED.equals(action)) {
                    timerIsRunning = false;
                    // TODO: hide pause icon, show resume and reset icons.
                } else if(TimerService.ACTION_RESET.equals(action)) {
                    long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, -1);
                    if(secondsLeft < 0) throw new IllegalArgumentException();
                    // Switch to SetTimerFragment
                    switchToFrag = SetTimerFragment.class;
                    fragArgs = SetTimerFragment.Args(secondsLeft);
                    timerIsRunning = false;
                } else if(TimerService.ACTION_EXPIRED.equals(action)) {
                    switchToFrag = TimerExpiredFragment.class;
                    timerIsRunning = false;
                } else if(TimerService.ACTION_TICK.equals(action)) {
                    // We'll check to make sure we don't refresh the fragment every tick,
                    // but this will ensure that the right fragment is shown quickly if the
                    // Activity is opened while the timer is already running.
                    switchToFrag = TimerControlsFragment.class;
                }

                if(activeFragmentClass != switchToFrag) {
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.replace(R.id.fragmentContainerView, switchToFrag, fragArgs);
                    ft.commit();
                    activeFragmentClass = switchToFrag;
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