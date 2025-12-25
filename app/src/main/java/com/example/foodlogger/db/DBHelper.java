package com.example.foodlogger.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "foodlogger.db";
    // Add near the top, with your other constants (optional)
    private static final String QUICK_ADD_NAME = "Quick Add (kcal)";

    private static final int DB_VER = 2;
    private static final String TAG = "DBHelper";

    public DBHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: creating tables");
        db.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, cal REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, protein REAL NOT NULL);");
        db.execSQL("CREATE TABLE meal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_date TEXT NOT NULL, meal_type TEXT NOT NULL, food_id INTEGER NOT NULL, grams REAL NOT NULL, cal REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, protein REAL NOT NULL, FOREIGN KEY(food_id) REFERENCES foods(id) ON DELETE CASCADE);");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);");
        // NEW: weights table (one row per date)
        db.execSQL("CREATE TABLE IF NOT EXISTS weights (" +
                "entry_date TEXT PRIMARY KEY," +
                "weight REAL NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_date ON weights(entry_date)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        Log.w(TAG, "onUpgrade: " + oldV + " -> " + newV);
        // Gentle migration: only add new artifacts for newer versions.
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS weights (" +
                    "entry_date TEXT PRIMARY KEY," +
                    "weight REAL NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_date ON weights(entry_date)");
        }
    }

    public int updateMealValues(long mealId, double grams, double cal, double carbs, double fat, double protein) {
        // We only need to store grams; macros are derived at read time now.
        ContentValues cv = new ContentValues();
        cv.put("grams", grams);
        return getWritableDatabase().update("meal_entries", cv, "id = ?", new String[]{ String.valueOf(mealId) });
    }



    // Foods CRUD
    public long insertFood(String name, double cal, double carbs, double fat, double protein) {
        Log.d(TAG, "insertFood: name=" + name + " cal=" + cal + " carbs=" + carbs + " fat=" + fat + " protein=" + protein);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("cal", cal); cv.put("carbs", carbs); cv.put("fat", fat); cv.put("protein", protein);
        long rowId = db.insertWithOnConflict("foods", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "insertFood: rowId=" + rowId);
        return rowId;
    }

    public Cursor getAllFoods() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT rowid AS _id, name, cal, carbs, fat, protein " +
                        "FROM foods " +
                        "ORDER BY name COLLATE NOCASE ASC",
                null
        );
    }



    public Cursor getFoodsFiltered(String q) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT rowid AS _id, name, cal, carbs, fat, protein " +
                        "FROM foods " +
                        "WHERE name LIKE ? COLLATE NOCASE " +   // case-insensitive filter
                        "ORDER BY name COLLATE NOCASE ASC",
                new String[]{"%" + q + "%"}
        );
    }



    // Log meal
    public long insertMeal(String date, String mealType, long foodId, double grams,
                           double calPer100, double carbsPer100, double fatPer100, double proteinPer100) {
        double factor = grams / 100.0;
        double cal = round2(calPer100 * factor);
        double carbs = round2(carbsPer100 * factor);
        double fat = round2(fatPer100 * factor);
        double protein = round2(proteinPer100 * factor);
        Log.d(TAG, "insertMeal: date=" + date + ", meal=" + mealType + ", foodId=" + foodId + ", grams=" + grams +
                ", cal=" + cal + ", C=" + carbs + ", F=" + fat + ", P=" + protein);
        ContentValues cv = new ContentValues();
        cv.put("entry_date", date); cv.put("meal_type", mealType); cv.put("food_id", foodId); cv.put("grams", grams);
        cv.put("cal", cal); cv.put("carbs", carbs); cv.put("fat", fat); cv.put("protein", protein);
        long rowId = getWritableDatabase().insert("meal_entries", null, cv);
        Log.d(TAG, "insertMeal: rowId=" + rowId);
        return rowId;
    }

    public Cursor getMealsFor(String date, String mealType) {
        Log.d(TAG, "getMealsFor: date=" + date + ", meal=" + mealType);
        return getReadableDatabase().rawQuery(
                "SELECT " +
                        "  me.id, " +
                        "  me.grams, " +
                        "  ROUND(f.cal     * me.grams/100.0, 0) AS cal, " +
                        "  ROUND(f.carbs   * me.grams/100.0, 1) AS carbs, " +
                        "  ROUND(f.fat     * me.grams/100.0, 1) AS fat, " +
                        "  ROUND(f.protein * me.grams/100.0, 1) AS protein, " +
                        "  f.name " +
                        "FROM meal_entries me " +
                        "JOIN foods f ON me.food_id = f.id " +
                        "WHERE me.entry_date=? AND me.meal_type=? " +
                        "ORDER BY me.id DESC",
                new String[]{date, mealType}
        );
    }

    public int updateFoodById(long id, String name, double cal, double carbs, double fat, double protein) {
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put("name", name.trim());
        cv.put("cal", cal);
        cv.put("carbs", carbs);
        cv.put("fat", fat);
        cv.put("protein", protein);
        // This preserves the same row id -> meal_entries keep working
        return getWritableDatabase().update("foods", cv, "id=?", new String[]{ String.valueOf(id) });
    }

    // Lookup by title-cased name
    public long findFoodIdByName(String name) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM foods " +
                        "WHERE TRIM(LOWER(name)) = TRIM(LOWER(?)) " +
                        "LIMIT 1",
                new String[]{ name }
        );
        try {
            return c.moveToFirst() ? c.getLong(0) : -1L;
        } finally { if (c != null) c.close(); }
    }


    // Update in place + recompute all linked meals (provided earlier)
    public void updateFoodAndRecalc(long foodId, String name,
                                    double calPer100, double carbsPer100,
                                    double fatPer100, double proteinPer100) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // 1) Update the food row IN PLACE (no REPLACE)
            ContentValues cv = new ContentValues();
            cv.put("name", name.trim());
            cv.put("cal", calPer100);
            cv.put("carbs", carbsPer100);
            cv.put("fat", fatPer100);
            cv.put("protein", proteinPer100);
            db.update("foods", cv, "id=?", new String[]{ String.valueOf(foodId) });

            // 2) Recompute existing meal_entries that reference this food
            //    Keep grams, recompute macros from new per-100g
            db.execSQL(
                    "UPDATE meal_entries " +
                            "SET cal     = ROUND((? * grams) / 100.0, 0), " +
                            "    carbs   = ROUND((? * grams) / 100.0, 2), " +
                            "    fat     = ROUND((? * grams) / 100.0, 2), " +
                            "    protein = ROUND((? * grams) / 100.0, 2) " +
                            "WHERE food_id = ?",
                    new Object[]{ calPer100, carbsPer100, fatPer100, proteinPer100, foodId }
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }




    public Cursor getTotalsForDay(String date) {
        Log.d(TAG, "getTotalsForDay: date=" + date);
        return getReadableDatabase().rawQuery(
                "SELECT " +
                        "  COALESCE(ROUND(SUM(f.cal     * me.grams/100.0), 0), 0) AS sum_cal, " +
                        "  COALESCE(ROUND(SUM(f.carbs   * me.grams/100.0), 1), 0) AS sum_carbs, " +
                        "  COALESCE(ROUND(SUM(f.fat     * me.grams/100.0), 1), 0) AS sum_fat, " +
                        "  COALESCE(ROUND(SUM(f.protein * me.grams/100.0), 1), 0) AS sum_prot " +
                        "FROM meal_entries me " +
                        "JOIN foods f ON me.food_id = f.id " +
                        "WHERE me.entry_date=?",
                new String[]{date}
        );

    }

    public int deleteMeal(long id) {
        Log.d(TAG, "deleteMeal: id=" + id);
        return getWritableDatabase().delete("meal_entries", "id=?", new String[]{String.valueOf(id)});
    }

    private static double round2(double v){ return Math.round(v * 100.0)/100.0; }

    // ---------- Weights (NEW) ----------
    public long upsertWeight(String date, double weightKg) {
        ContentValues cv = new ContentValues();
        cv.put("entry_date", date);
        cv.put("weight", weightKg);
        long rowId = getWritableDatabase().insertWithOnConflict("weights", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "upsertWeight: date=" + date + " weight=" + weightKg + " rowId=" + rowId);
        return rowId;
    }

    public Cursor getWeightFor(String date) {
        return getReadableDatabase().rawQuery(
                "SELECT weight FROM weights WHERE entry_date=?",
                new String[]{date}
        );
    }

    // ADD: list all foods EXCEPT Quick Add
    public Cursor getAllFoodsNoQuickAdd() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT rowid AS _id, name, cal, carbs, fat, protein " +
                        "FROM foods " +
                        "WHERE name <> ? " +
                        "ORDER BY name COLLATE NOCASE ASC",
                new String[]{ QUICK_ADD_NAME }
        );
    }

    // ADD: filter foods EXCEPT Quick Add
    public Cursor getFoodsFilteredNoQuickAdd(String q) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT rowid AS _id, name, cal, carbs, fat, protein " +
                        "FROM foods " +
                        "WHERE name <> ? AND name LIKE ? COLLATE NOCASE " +
                        "ORDER BY name COLLATE NOCASE ASC",
                new String[]{ QUICK_ADD_NAME, "%" + q + "%" }
        );
    }



    // --- Quick Add (kcal) support ---
    public long ensureQuickAddFood() {
        // name: unique; per-100g: 100 kcal, 0/0/0 macros
        long id = getFoodIdByName("Quick Add (kcal)");
        if (id != -1) return id;
        return insertFood("Quick Add (kcal)", 100.0, 0.0, 0.0, 0.0);
    }

    public long getFoodIdByName(String name) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM foods WHERE name = ? LIMIT 1", new String[]{name});
        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }


    public Cursor getWeightsForDates(String[] dates) {
        if (dates == null || dates.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT entry_date, weight FROM weights WHERE entry_date IN (");
        for (int i = 0; i < dates.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        return getReadableDatabase().rawQuery(sb.toString(), dates);
    }
}
