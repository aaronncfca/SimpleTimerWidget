package com.example.simpletimerwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.content.ComponentName;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class TimerWidget extends AppWidgetProvider {
    private final String WIDGET_BTN_CLICK = "WIDGETBTN_CLICK";

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
            views.setOnClickPendingIntent(R.id.start_button, getPendingSelfIntent(context, WIDGET_BTN_CLICK, appWidgetId));

            appWidgetManager.updateAppWidget(appWidgetId, views);
            //updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(WIDGET_BTN_CLICK.equals(intent.getAction())) {
            // TODO: ensure service isn't already running.
            Intent serviceIntent = new Intent(context, TimerService.class);
            serviceIntent.setAction(TimerService.ACTION_START);
            serviceIntent.putExtra(TimerService.EXTRA_SECONDS_LEFT, 60L); // TODO: get from activity...?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Toast.makeText(context, "serviceStarted", Toast.LENGTH_SHORT).show();
        }
        if (TimerService.ACTION_TICK.equals(intent.getAction())) {
            long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 0);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(secondsLeft));

            ComponentName widget = new ComponentName(context, TimerWidget.class);
            appWidgetManager.updateAppWidget(widget, views);
        }
        if(TimerService.ACTION_RESET.equals(intent.getAction())) {
            long secondsLeft = intent.getLongExtra(TimerService.EXTRA_SECONDS_LEFT, 0);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.timer_widget);
            views.setTextViewText(R.id.timer_text, TimerService.formatTimeLeft(secondsLeft));

            ComponentName widget = new ComponentName(context, TimerWidget.class);
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
        return PendingIntent.getBroadcast(context, widgetId, intent, PendingIntent.FLAG_IMMUTABLE);
    }

}