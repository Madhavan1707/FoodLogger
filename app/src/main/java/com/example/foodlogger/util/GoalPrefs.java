package com.example.foodlogger.util;

import android.content.Context;
import android.content.SharedPreferences;

public class GoalPrefs {
    private static final String PREFS = "user_goals";
    private static final String K_KCAL = "goal_kcal";
    private static final String K_PROT = "goal_prot";
    private static final String K_CARB = "goal_carb";
    private static final String K_FAT  = "goal_fat";

    // sensible defaults (your previous static values)
    public static final float DEF_KCAL = 1800f;
    public static final float DEF_PROT = 130f;
    public static final float DEF_CARB = 160f;
    public static final float DEF_FAT  = 50f;

    public static void save(Context ctx, float kcal, float prot, float carb, float fat) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putFloat(K_KCAL, kcal)
                .putFloat(K_PROT, prot)
                .putFloat(K_CARB, carb)
                .putFloat(K_FAT,  fat)
                .apply();
    }

    public static float kcal(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(K_KCAL, DEF_KCAL); }
    public static float prot(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(K_PROT, DEF_PROT); }
    public static float carb(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(K_CARB, DEF_CARB); }
    public static float fat (Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(K_FAT,  DEF_FAT); }
}
