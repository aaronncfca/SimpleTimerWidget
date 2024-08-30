package com.example.simpletimerwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {
    public static final String CHANNEL_ID = "TimerServiceChannel";

    // Must match action names in AndroidManifest.
    public static final String ACTION_STARTED = "SIMPLETIMER_ACTION_STARTED";
    public static final String ACTION_STOPPED = "SIMPLETIMER_ACTION_STARTED";
    public static final String ACTION_TICK = "SIMPLETIMER_ACTION_TICK";
    public static final String EXTRA_SECONDS_LEFT = "secondsLeft";

    private MyTimer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long secondsLeft = intent.getLongExtra(EXTRA_SECONDS_LEFT, 50); // Default to 1 minute
        startForeground(1, getNotification("Timer started"));

        timer = new MyTimer(secondsLeft * 1000L);
        timer.Start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.Pause();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Service")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String text) {
        Notification notification = getNotification(text);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }
    }

    private class MyTimer extends Timer {
        public MyTimer(long defaultMs) {
            super(defaultMs);
        }

        @Override
        public void onTick(long secondsLeft) {
            updateNotification("Time left: " + secondsLeft + " seconds");
            sendBroadcast(new Intent(ACTION_TICK).setPackage(getApplicationContext().getPackageName()).putExtra(EXTRA_SECONDS_LEFT, secondsLeft));
        }

        @Override
        public void onFinish() {
            updateNotification("Timer finished");
            stopForeground(true); // TODO: don't stop until after alarm has been silenced.
            stopSelf();
        }

        @Override
        public void onReset(long secondsUntilFinished) {
            updateNotification("Timer reset to: " + secondsUntilFinished + " seconds");
        }
    }
}