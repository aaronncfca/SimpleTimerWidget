package com.example.simpletimerwidget;

import android.os.CountDownTimer;

public abstract class Timer {
    private CountDownTimer cdt;
    private long currMs;
    private long startMs;
    private boolean counting;

    public Timer(long defaultMs) {
        Set(defaultMs);
    }

    public void Set(long ms) {
        startMs = ms;
        Reset();
    }

    public void Reset() {
        Pause();
        currMs = startMs;
        onReset(currMs/1000);
    }

    public void Start() {
        counting = true;

        // Count down interval set to 200ms so that it doesn't tick too often, but often
        // enough that when paused and unpaused it will be more precise than a full second.
        // (This is not intended for use as a precision chronograph.)
        cdt = new CountDownTimer(currMs, 200) {
            @Override
            public void onTick(long msUntilFinished) {
                long lastMs = currMs;
                currMs = msUntilFinished;

                // Only tick once per second. Otherwise, set currMs and leave.
                if(lastMs % 1000 == currMs % 1000) return;

                Timer.this.onTick(msUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                Timer.this.onFinish();
            }
        };

        cdt.start();
    }

    public void Pause() {
        counting = false;
        if(cdt != null) {
            cdt.cancel();
            cdt = null;
        }
    }

    public long GetCurrMs() {
        return currMs;
    }

    public boolean IsStarted() { return counting; }

    public abstract void onTick(long secondsUntilFinished);

    /**
     * Called when timer is finished. NOTE: this will be called up to 1 second after
     * the timer reaches 0 (i.e. onTick(0) is called) due to rounding and such.
     */
    public abstract void onFinish();

    public abstract void onReset(long secondsUntilFinished);
}
