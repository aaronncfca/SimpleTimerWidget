package com.example.simpletimerwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerControlsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class TimerControlsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Context activity;
    private TextView textView;

    private BroadcastReceiver receiver;

    private long secondsSet = 60; // Modified when the user sets the timer.
    private boolean timerIsRunning = false;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TimerControlsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TimerControlsFragment newInstance(String param1, String param2) {
        TimerControlsFragment fragment = new TimerControlsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TimerControlsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timer_controls, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = getActivity();
        if(activity == null) throw new IllegalStateException(); // Suppress warnings.
        textView = view.findViewById(R.id.textView);
        setTimeView(secondsSet);

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
                    setTimeView(secondsLeft);
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
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        view.findViewById(R.id.btnPause).setOnClickListener(this::pauseTimer);
        view.findViewById(R.id.btnPlay).setOnClickListener(this::startTimer);
        view.findViewById(R.id.btnReset).setOnClickListener(this::resetTimer);
    }


    private void setTimeView(long seconds) {
        textView.setText(TimerService.formatTimeLeft(seconds));
    }

    private void startTimerService(String action, long extraSecondsLeft) {
        Intent serviceIntent = new Intent(getActivity(), TimerService.class);
        serviceIntent.setAction(action);
        if(extraSecondsLeft >= 0) {
            serviceIntent.putExtra(TimerService.EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(serviceIntent);
        } else {
            activity.startService(serviceIntent);
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



}