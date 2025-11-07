package com.example.foodlogger;

import android.app.Application;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // Schedule 11:00 PM reminder (local time) once; WorkManager persists it
        com.example.foodlogger.reminder.ReminderScheduler.scheduleDailyReminder(this, 23, 0);
    }
}
