package com.example.simpletimerwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.content.ComponentName;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class TimerWidget extends AppWidgetProvider {
    private final String START_BTN_CLICK = "START_BTN_CLICK";
    private final String RESET_BTN_CLICK = "RESET_BTN_CLICK";
    private final String TIMER_TEXT_CLICK = "TIMER_TEXT_CLICK";

    private boolean isPaused = false;
    private boolean isRunning = false;
    private long startingSeconds = 60L;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
        views.setTextViewText(R.id.timer_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            views.setOnClickPendingIntent(R.id.start_button, getPendingSelfIntent(context, START_BTN_CLICK, appWidgetId));
            views.setOnClickPendingIntent(R.id.reset_button, getPendingSelfIntent(context, RESET_BTN_CLICK, appWidgetId));

            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent openAppPendingIntent = PendingIntent.getActivity(context,
                    appWidgetId,
                    openAppIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.timer_text, openAppPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            //updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        loadTimerState(context);
        if(START_BTN_CLICK.equals(intent.getAction())) {
            if(isPaused) {
                startTimerService(context, TimerService.ACTION_RESUME, startingSeconds);
            } else if(isRunning) {
                startTimerService(context, TimerService.ACTION_PAUSE, startingSeconds);
            } else {
                startTimerService(context, TimerService.ACTION_START, startingSeconds);
            }
        }
        if(RESET_BTN_CLICK.equals(intent.getAction())) {
            startTimerService(context, TimerService.ACTION_CANCEL, -1L);
        }
        if(TIMER_TEXT_CLICK.equals(intent.getAction())) {
            // TODO: open app.
            Toast.makeText(context, "Timer widget clicked.", Toast.LENGTH_SHORT).show();
        }
        if (TimerService.ACTION_TICK.equals(intent.getAction())) {
            long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(secondsLeft));

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widget, views);
        }
        if(TimerService.ACTION_RESET.equals(intent.getAction())) {
            startingSeconds = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 60L);
            saveTimerState(context, false, false);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(startingSeconds));
            hideResetBtn(views);

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widget, views);
        }
        if(TimerService.ACTION_PAUSED.equals(intent.getAction())) {
            saveTimerState(context, true, false);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            showResetBtn(views);

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widget, views);
        }
        if(TimerService.ACTION_STARTED.equals(intent.getAction())) {
            saveTimerState(context, false, true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            hideResetBtn(views);

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widget, views);
        }
        if(TimerService.ACTION_EXPIRED.equals(intent.getAction())) {
            saveTimerState(context, false, false);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            showResetBtnOnly(views);

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(widget, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action, int widgetId) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, widgetId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void showResetBtnOnly(RemoteViews views) {
        views.setViewVisibility(R.id.start_button, View.GONE);
        views.setViewVisibility(R.id.reset_button, View.VISIBLE);
        views.setViewVisibility(R.id.divider, View.GONE);
    }

    private void showResetBtn(RemoteViews views) {
        views.setViewVisibility(R.id.start_button, View.VISIBLE);
        views.setViewVisibility(R.id.reset_button, View.VISIBLE);
        views.setViewVisibility(R.id.divider, View.VISIBLE);
    }

    private void hideResetBtn(RemoteViews views) {
        views.setViewVisibility(R.id.start_button, View.VISIBLE);
        views.setViewVisibility(R.id.reset_button, View.GONE);
        views.setViewVisibility(R.id.divider, View.GONE);
    }

    private void startTimerService(Context context, String action, long extraSecondsLeft) {
        Intent serviceIntent = new Intent(context, TimerService.class);
        serviceIntent.setAction(action);
        if(extraSecondsLeft >= 0) {
            serviceIntent.putExtra(TimerService.EXTRA_SECONDS_LEFT, extraSecondsLeft);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private void saveTimerState(Context context, boolean isPaused, boolean isRunning) {
        SharedPreferences prefs = context.getSharedPreferences(TimerService.TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isPaused", isPaused);
        editor.putBoolean("isRunning", isRunning);
        editor.apply();
    }

    private void loadTimerState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TimerService.TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        isPaused = prefs.getBoolean("isPaused", false);
        isRunning = prefs.getBoolean("isRunning", false);
        startingSeconds = prefs.getLong(TimerService.PREF_STARTING_SECONDS, 60L);
    }

}