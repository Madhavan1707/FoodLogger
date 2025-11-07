package com.example.foodlogger.reminder;

import android.content.Context;
import android.util.Log;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class ReminderScheduler {
    private static final String UNIQUE_NAME = "daily_meal_reminder";

    public static void scheduleDailyReminder(Context ctx, int hour24, int minute) {
        long delayMinutes = computeInitialDelayMinutes(hour24, minute);
        Log.d("RS", "schedulign 24 hour" + delayMinutes) ;
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(ReminderWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
    }

    private static long computeInitialDelayMinutes(int hour24, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.with(LocalTime.of(hour24, minute, 0));
        if (!target.isAfter(now)) target = target.plusDays(1);
        return Math.max(1, Duration.between(now, target).toMinutes()); // at least 1 minute
    }
}
