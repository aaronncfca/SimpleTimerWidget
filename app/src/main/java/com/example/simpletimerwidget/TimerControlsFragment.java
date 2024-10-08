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
import android.widget.ImageButton;
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
    private ImageButton btnReset;

    private BroadcastReceiver receiver;

    private long secondsSet = 60; // Modified when the user sets the timer.
    private boolean timerIsRunning = true;


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
                    if(btnReset != null) btnReset.setVisibility(View.GONE);
                } else if(TimerService.ACTION_PAUSED.equals(action)) {
                    timerIsRunning = false;
                    if(btnReset != null) btnReset.setVisibility(View.VISIBLE);
                } else if(TimerService.ACTION_RESET.equals(action)) {
                    // TODO: this clause should be unnecessary, because this fragment
                    // is replaced on ACTION_RESET.
                    long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, -1);
                    if(secondsLeft < 0) throw new IllegalArgumentException();
                    timerIsRunning = false;
                    setTimeView(secondsLeft);
                } else if(TimerService.ACTION_TICK.equals(action)) {
                    timerIsRunning = true;
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
        filter.addAction(TimerService.ACTION_TICK);
        // Calling ContextCompat to avoid warnings related to specifying whether the receiver
        // is exported.
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        btnReset = view.findViewById(R.id.btnReset);
        view.findViewById(R.id.btnPauseResume).setOnClickListener(this::pauseResumeTimer);
        btnReset.setOnClickListener(this::resetTimer);
        btnReset.setVisibility(View.GONE);
    }


    private void setTimeView(long seconds) {
        textView.setText(TimerService.formatTimeLeft(seconds));
    }

    public void pauseResumeTimer(View view) {
        if(timerIsRunning) {
            TimerService.startTimerService(activity, TimerService.ACTION_PAUSE, -1);
            timerIsRunning = false;
        } else {
            TimerService.startTimerService(activity, TimerService.ACTION_RESUME, -1);
            timerIsRunning = true;
        }

    }

    public void resetTimer (View view) {
        TimerService.startTimerService(activity, TimerService.ACTION_CANCEL, -1);
    }



}