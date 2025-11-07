package com.example.foodlogger.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.foodlogger.MainActivity;
import com.example.foodlogger.R;

public class NotificationHelper {
    private static final String CHANNEL_ID = "daily_reminders_v2";
    private static final int NOTIF_ID = 1001;

    public static void showDailyReminder(Context ctx) {
        ensureChannel(ctx);

        Intent intent = new Intent(ctx, MainActivity.class)
                .putExtra("open_tab", "log_meal")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // ensure you have a valid monochrome icon
                .setContentTitle("Log your meals")
                .setContentText("Missed anything today? Log before midnight.")
                .setPriority(NotificationCompat.PRIORITY_LOW) // silent, respectful
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIF_ID, b.build());
            android.util.Log.d("Reminder", "notify() posted on " + CHANNEL_ID);
        } catch (Exception e) {
            android.util.Log.e("Reminder", "notify() failed", e);
        }

    }

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Daily Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Daily 11 PM reminders to log missed meals");
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }
}
