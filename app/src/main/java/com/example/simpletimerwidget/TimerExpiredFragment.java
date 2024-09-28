package com.example.simpletimerwidget;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class TimerExpiredFragment extends Fragment {

    public TimerExpiredFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timer_expired, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnDismiss).setOnClickListener(this::onBtnDismissPress);
        ImageView iv = view.findViewById(R.id.expiredAnim);
        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) iv.getDrawable();
        avd.start();
    }

    private void onBtnDismissPress(View view) {
        Context activityContext = getActivity();
        if(activityContext == null) throw new IllegalStateException();

        TimerService.startTimerService(activityContext, TimerService.ACTION_CANCEL, -1);
    }
}