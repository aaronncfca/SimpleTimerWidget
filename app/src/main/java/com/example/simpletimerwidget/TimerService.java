package com.example.simpletimerwidget;

import android.app.AlarmManager;
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
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.Settings;

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

    // Used for AlarmManager PendingIntents, not called by or broadcasted to external classes.
    private static final String ACTION_ALARMCLOCK = "SIMPLETIMER_ACTION_ALARMCLOCK";
    public static final String ACTION_SILENCED = "SIMPLETIMER_ACTION_SILENCED"; // TODO: not yet used... issue RESET instead???
    public static final String ACTION_TICK = "SIMPLETIMER_ACTION_TICK"; // Must carry EXTRA_SECONDS_LEFT

    public static final String TIMER_SHARED_PREFERENCES = "TimerWidgetPrefs";
    public static final String PREF_STARTING_SECONDS = "startingSeconds";

    public static final String EXTRA_SECONDS_LEFT = "secondsLeft";

    private MyTimer timer;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private NotificationCompat.Builder notificationBuilder;
    private boolean expired = false;
    private boolean usingAlarmManager = false; // Set to true if canScheduleExactAlarms.
    private PowerManager.WakeLock wakeLock = null;

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

    public static void startTimerService(Context context, String action, long extraSecondsLeft) {
        Intent serviceIntent = new Intent(context, TimerService.class);
        serviceIntent.setAction(action);
        if(extraSecondsLeft >= 0) {
            serviceIntent.putExtra(EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private void scheduleAlarm(long msLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager == null || !alarmManager.canScheduleExactAlarms()) {
                usingAlarmManager = false;
                return; // Do nothing; we aren't allowed to schedule an alarm.
            }
        }
        usingAlarmManager = true;

        if(alarmManager != null) {
            long triggerAtMillis = System.currentTimeMillis() + msLeft;

            Intent intent = new Intent(this, TimerService.class);
            intent.setAction(ACTION_ALARMCLOCK);
            alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, alarmIntent);
            alarmManager.setAlarmClock(info, alarmIntent);
        }
    }

    // Method to cancel an alarm
    private void cancelAlarm() {
        if (alarmManager != null && alarmIntent != null) {
            alarmManager.cancel(alarmIntent);
            alarmIntent = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if(ACTION_START.equals(action)) {
            if(timer == null || !timer.IsStarted()) { // If timer is already created and running, do nothing.
                long secondsLeft = intent.getLongExtra(EXTRA_SECONDS_LEFT, -1);
                if (secondsLeft < 0) throw new IllegalArgumentException();

                timer = new MyTimer(secondsLeft * 1000L);
                timer.Start();
                scheduleAlarm(secondsLeft * 1000L);

                initNotificationBuilder();

                startForeground(1, getNotification("Timer started"));

                sendTimerBroadcast(ACTION_STARTED);
            }
        } else if(ACTION_PAUSE.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            if(timer.IsStarted()) { // If timer is already paused, do nothing.
                timer.Pause();
                cancelAlarm();
                initNotificationBuilder();
                // Update the notification here, since there won't be any more ticks until resumed.
                updateNotification("Timer paused: " + formatTimeLeft(timer.GetCurrMs()/1000));
                sendTimerBroadcast(ACTION_PAUSED);
            }
        } else if(ACTION_RESUME.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            if(!timer.IsStarted()) { // If timer is already running, do nothing.
                timer.Start();
                scheduleAlarm(timer.GetCurrMs());
                initNotificationBuilder();
                sendTimerBroadcast(ACTION_STARTED);
            }
        } else if(ACTION_CANCEL.equals(action)) {
            if(timer == null) throw new IllegalStateException();
            timer.Reset();
            cancelAlarm();
            if(wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
            // Will sendTimerBroadcast in MyTimer.onReset.
            stopForeground(true);
            stopSelf();
        } else if(ACTION_ALARMCLOCK.equals(action)) {
            // Keep the device awake so the user can see and dismiss the notification.
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation // PowerManager.FULL_WAKE_LOCK is deprecated, but there is no acceptable alternative.
            wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "SimpleTimer:WakeLock");
            wakeLock.acquire(20 * 1000L); // Acquire wake lock for 20 seconds or until notification is dismissed.

            expired = true; // Important to set before calling initNotificationBuilder().
            initNotificationBuilder();
            updateNotification("Timer finished");
            sendTimerBroadcast(ACTION_EXPIRED);
        }

        // Could change to START_STICKY if able to remember a resume a timer after killing and
        // restarting the service.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            // Will result in MyTimer::onReset (below), which will broadcast ACTION_RESET.
            timer.Reset();
            cancelAlarm();
        }

        super.onDestroy();
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
            sendTimerBroadcast(ACTION_TICK, secondsLeft);
            if(secondsLeft >= 2) {
                // Avoid updating the notification at 1 second, since the notification manager may
                // eat up the timer expired notification if it sees it in too close proximity to
                // an earlier update.
                updateNotification("Time remaining: " + formatTimeLeft(secondsLeft));
            }
        }

        @Override
        public void onFinish() {
            if(!usingAlarmManager) {
                // If we aren't allowed to use the alarm manager, explicitly trigger ACTION_ALARMCLOCK
                // here instead. Otherwise the alarm manager will do so, ensuring the device awakens
                // if sleeping, etc.
                startTimerService(TimerService.this, ACTION_ALARMCLOCK, -1);
            }
        }

        @Override
        public void onReset(long secondsUntilFinished) {
            if(timer == null) return; // This is called during Timer initialization, at which point timer is still null.
            sendTimerBroadcast(ACTION_RESET, timer.GetCurrMs()/1000);
        }
    }
}