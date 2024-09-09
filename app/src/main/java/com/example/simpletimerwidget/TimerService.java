package com.example.simpletimerwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TimerService extends Service {
    public static final String CHANNEL_ID = "TimerServiceChannel";

    // Intent actions received by this service (passed to startService)
    public static final String ACTION_START = "SIMPLETIMER_ACTION_START"; // Must carry EXTRA_SECONDS_LEFT
    public static final String ACTION_PAUSE = "SIMPLETIMER_ACTION_PAUSE";
    public static final String ACTION_RESUME = "SIMPLETIMER_ACTION_RESUME";
    public static final String ACTION_CANCEL = "SIMPLETIMER_ACTION_CANCEL";

    // Intent actions broadcast by this service. Must match action names in AndroidManifest.
    public static final String ACTION_STARTED = "SIMPLETIMER_ACTION_STARTED";
    public static final String ACTION_PAUSED = "SIMPLETIMER_ACTION_PAUSED";
    public static final String ACTION_RESET = "SIMPLETIMER_ACTION_RESET"; // Must carry EXTRA_SECONDS_LEFT
    public static final String ACTION_EXPIRED = "SIMPLETIMER_ACTION_EXPIRED";
    public static final String ACTION_SILENCED = "SIMPLETIMER_ACTION_SILENCED"; // TODO: not yet used... issue RESET instead???
    public static final String ACTION_TICK = "SIMPLETIMER_ACTION_TICK"; // Must carry EXTRA_SECONDS_LEFT


    public static final String EXTRA_SECONDS_LEFT = "secondsLeft";

    private MyTimer timer;
    private NotificationCompat.Builder notificationBuilder;
    private boolean expired = false;

    public static String formatTimeLeft(long secondsLeft) {
        NumberFormat f = new DecimalFormat("00");
        long hour = (secondsLeft / 60 / 60) % 24;
        long min = (secondsLeft / 60) % 60;
        long sec = secondsLeft % 60;

        String text = "";
        // TODO: use a method that takes locale into account?
        if (hour > 0) {
            text += hour + ":" + f.format(min) + ":";
        } else {
            text += min + ":";
        }
        text += f.format(sec);
        return text;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if(ACTION_START.equals(action)) {
            if(timer != null && timer.IsStarted()) {
                // Timer is already started. Do nothing.
                return START_NOT_STICKY;
            }
            long secondsLeft = intent.getLongExtra(EXTRA_SECONDS_LEFT, -1);
            if(secondsLeft < 0) throw new IllegalArgumentException();

            timer = new MyTimer(secondsLeft * 1000L);
            timer.Start();

            initNotificationBuilder();

            startForeground(1, getNotification("Timer started"));

            sendTimerBroadcast(ACTION_STARTED);

        } else if(ACTION_PAUSE.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            timer.Pause();
            initNotificationBuilder();
            // Update the notification here, since there won't be any more ticks until resumed.
            updateNotification("Timer paused: " + formatTimeLeft(timer.GetCurrMs()/1000));
            sendTimerBroadcast(ACTION_PAUSED);
        } else if(ACTION_RESUME.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            timer.Start();
            initNotificationBuilder();
            sendTimerBroadcast(ACTION_STARTED);
        } else if(ACTION_CANCEL.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            timer.Reset();
            // Will sendTimerBroadcast in MyTimer.onReset.
            stopForeground(true);
            stopSelf();
        }

        // Could change to START_STICKY if able to remember a resume a timer after killing and
        // restarting the service.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (timer != null) {
            timer.Reset();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendTimerBroadcast(String action) { sendTimerBroadcast(action, -1); }

    private void sendTimerBroadcast(String action, long extraSecondsLeft) {
        Intent intent = new Intent(action)
                .setPackage(getApplicationContext().getPackageName());
        if(extraSecondsLeft >= 0) {
            intent.putExtra(EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Service Channel 2",
                    NotificationManager.IMPORTANCE_HIGH // For the sake of alarms
            );

            // Use an alarm sound, since the only time the notification will sound is
            // on timer expired.
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            serviceChannel.setSound(alarmSound, audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    // Initializes notificationBuilder with all our notification settings.
    // Must be called on START, PAUSE, and RESUME before posting notifications.
    private void initNotificationBuilder() {
        boolean paused = (timer != null) && !timer.IsStarted();

        // Uncomment and use this to open the activity when the notification is pressed.
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, TimerService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent piCancel = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer);

        // Depending on the current state, we want to either pause, resume, or cancel when
        // the notification is pressed. Unfortunately, I have not found a way to use addAction
        // while the timer is running, since the updating of the notification makes it
        // difficult to press the action buttons.
        if(expired) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            notificationBuilder.setContentText("Tap to dismiss")
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // For older models. TODO: create new channel for the alarm--or play the alarm in a different activity.
                    .setSound(alarmSound)
                    .setContentIntent(piCancel);
        } else {
            // Other than on expired, we don't want the notification bothering the user directly.
            notificationBuilder.setOnlyAlertOnce(true)
                    .setSilent(true);

            if(paused) {
                Intent resumeIntent = new Intent(this, TimerService.class);
                resumeIntent.setAction(ACTION_RESUME);
                PendingIntent piResume = PendingIntent.getService(this, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                // TODO: if the user happens to tap to pause too soon after a tick, this
                // notification may be gobbled up by the system. Consider creating some
                // kind of delay before sending this notification.
                notificationBuilder.setContentText("Tap to resume")
                        .setContentIntent(piResume)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", piCancel);
            } else {
                Intent pauseIntent = new Intent(this, TimerService.class);
                pauseIntent.setAction(ACTION_PAUSE);
                PendingIntent piPause = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                notificationBuilder.setContentText("Tap to pause")
                        .setContentIntent(piPause);
            }
        }
    }

    private Notification getNotification(String text) {
        notificationBuilder.setContentTitle(text);
        return notificationBuilder.build();
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
            updateNotification("Time remaining: " + formatTimeLeft(secondsLeft));
            sendTimerBroadcast(ACTION_TICK, secondsLeft);
        }

        @Override
        public void onFinish() {
            expired = true;
            initNotificationBuilder();
            updateNotification("Timer finished");
            sendTimerBroadcast(ACTION_EXPIRED);
        }

        @Override
        public void onReset(long secondsUntilFinished) {
            if(timer == null) return; // This is called during Timer initialization, at which point timer is still null.
            sendTimerBroadcast(ACTION_RESET, timer.GetCurrMs()/1000);
        }
    }
}