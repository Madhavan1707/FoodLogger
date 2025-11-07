package com.example.foodlogger.reminder;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        Log.d("RW","do work");
        // Option A (simple): always remind at 11 PM
        NotificationHelper.showDailyReminder(getApplicationContext());
        return Result.success();
    }
}
