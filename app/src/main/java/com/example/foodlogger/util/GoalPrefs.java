package com.example.foodlogger.util;

import android.content.Context;
import android.content.SharedPreferences;

public class GoalPrefs {
    private static final String PREFS = "user_goals";
    private static final String K_SETUP_DONE = "setup_done";
    private static final String K_KCAL = "goal_kcal";
    private static final String K_PROT = "goal_prot";
    private static final String K_CARB = "goal_carb";
    private static final String K_FAT  = "goal_fat";

    public static void save(Context ctx, float kcal, float prot, float carb, float fat) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putFloat(K_KCAL, kcal)
                .putFloat(K_PROT, prot)
                .putFloat(K_CARB, carb)
                .putFloat(K_FAT,  fat)
                .putBoolean(K_SETUP_DONE, true)
                .apply();
    }
    public static boolean isSetupDone(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(K_SETUP_DONE, false);
    }


    public static float kcal(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(K_KCAL, 0f);
    }
    public static float prot(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(K_PROT, 0f);
    }
    public static float carb(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(K_CARB, 0f);
    }
    public static float fat(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(K_FAT, 0f);
    }

    // Returns true if all four goals are set to positive values
    public static boolean isSetupComplete(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(K_SETUP_DONE, false)
                && kcal(ctx) > 0 && prot(ctx) > 0 && carb(ctx) > 0 && fat(ctx) > 0;
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

}
