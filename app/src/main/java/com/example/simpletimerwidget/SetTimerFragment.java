package com.example.simpletimerwidget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetTimerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetTimerFragment extends Fragment {
    private static final String ARG_INITIAL_SECONDS = "initial_time_seconds";
    private NumberPicker hourPicker;
    private NumberPicker minPicker;
    private NumberPicker secPicker;
    private long initialSeconds = 60; // Passed in as argument

    public SetTimerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetTimerFragment.
     */
    public static SetTimerFragment newInstance(long initialTimeSeconds) {
        SetTimerFragment fragment = new SetTimerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL_SECONDS, initialTimeSeconds);
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle Args(long initialTimeSeconds) {
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL_SECONDS, initialTimeSeconds);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            initialSeconds = getArguments().getLong(ARG_INITIAL_SECONDS);
        } else if(getActivity() != null){
            SharedPreferences prefs = getActivity().getSharedPreferences(TimerService.TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            initialSeconds = prefs.getLong(TimerService.PREF_STARTING_SECONDS, 60L);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hourPicker = view.findViewById(R.id.hours);
        minPicker = view.findViewById(R.id.minutes);
        secPicker = view.findViewById(R.id.seconds);
        hourPicker.setMaxValue(23);
        hourPicker.setMinValue(0);
        minPicker.setMaxValue(59);
        minPicker.setMinValue(0);
        secPicker.setMaxValue(59);
        minPicker.setMinValue(0);
        hourPicker.setValue((int)initialSeconds / 60 / 60);
        minPicker.setValue(((int)initialSeconds / 60) % 60);
        secPicker.setValue((int)initialSeconds % 60);
        view.findViewById(R.id.startTimerButton).setOnClickListener(this::startTimerClicked);
    }

    private void startTimerService(String action, long extraSecondsLeft) {
        Context activityContext = getActivity();
        if(activityContext == null) throw new IllegalStateException();

        Intent serviceIntent = new Intent(activityContext, TimerService.class);
        serviceIntent.setAction(action);
        if(extraSecondsLeft >= 0) {
            serviceIntent.putExtra(TimerService.EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activityContext.startForegroundService(serviceIntent);
        } else {
            activityContext.startService(serviceIntent);
        }
    }

    public void startTimerClicked (View view) {
        int hour = hourPicker.getValue();
        int minute = minPicker.getValue();
        int second = secPicker.getValue();
        long secondsLeft = (hour*60L*60 + minute*60L + second);

        if(getActivity() != null) {
            // Save the time in shared preferences
            SharedPreferences prefs = getActivity().getSharedPreferences(TimerService.TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(TimerService.PREF_STARTING_SECONDS, secondsLeft);
            editor.apply();
        }

        startTimerService(TimerService.ACTION_START, secondsLeft);
    }
}