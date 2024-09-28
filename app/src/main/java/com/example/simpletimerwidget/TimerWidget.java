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

import java.util.Arrays;

/**
 * Implementation of App Widget functionality.
 */
public class TimerWidget extends AppWidgetProvider {
    private final String START_BTN_CLICK = "START_BTN_CLICK";
    private final String RESET_BTN_CLICK = "RESET_BTN_CLICK";

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

        String action = intent.getAction();
        if(action == null) return;

        // BUTTON CLICK HANDLERS

        if(START_BTN_CLICK.equals(action)) {
            if(isPaused) {
                TimerService.startTimerService(context, TimerService.ACTION_RESUME, startingSeconds);
            } else if(isRunning) {
                TimerService.startTimerService(context, TimerService.ACTION_PAUSE, startingSeconds);
            } else {
                TimerService.startTimerService(context, TimerService.ACTION_START, startingSeconds);
            }
        }
        if(RESET_BTN_CLICK.equals(action)) {
            TimerService.startTimerService(context, TimerService.ACTION_CANCEL, -1L);
        }

        // TIMER STATUS UPDATE HANDLERS

        final String[] timerActions = new String[] {
                TimerService.ACTION_TICK,
                TimerService.ACTION_RESET,
                TimerService.ACTION_PAUSED,
                TimerService.ACTION_STARTED,
                TimerService.ACTION_EXPIRED
        };

        if(!Arrays.stream(timerActions).anyMatch(action::equals)) {
            return;
        }

        loadTimerState(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);

        if (TimerService.ACTION_TICK.equals(action)) {
            long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 0);

            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(secondsLeft));
        }
        if(TimerService.ACTION_RESET.equals(action)) {
            startingSeconds = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 60L);
            saveTimerState(context, false, false);

            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(startingSeconds));
            hideResetBtn(views);
        }
        if(TimerService.ACTION_PAUSED.equals(action)) {
            saveTimerState(context, true, false);
            showResetBtn(views);
        }
        if(TimerService.ACTION_STARTED.equals(action)) {
            saveTimerState(context, false, true);
            hideResetBtn(views);
        }
        if(TimerService.ACTION_EXPIRED.equals(action)) {
            saveTimerState(context, false, false);
            showResetBtnOnly(views);
        }

        ComponentName widget = new ComponentName(context, TimerWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widget, views);
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