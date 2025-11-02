package com.example.foodlogger.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "foodlogger.db";
    private static final int DB_VER = 1;
    private static final String TAG = "DBHelper";

    public DBHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: creating tables");
        db.execSQL("CREATE TABLE foods (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, cal REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, protein REAL NOT NULL);");
        db.execSQL("CREATE TABLE meal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_date TEXT NOT NULL, meal_type TEXT NOT NULL, food_id INTEGER NOT NULL, grams REAL NOT NULL, cal REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL, protein REAL NOT NULL, FOREIGN KEY(food_id) REFERENCES foods(id) ON DELETE CASCADE);");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        Log.w(TAG, "onUpgrade: " + oldV + " -> " + newV + ", dropping tables");
        db.execSQL("DROP TABLE IF EXISTS meal_entries");
        db.execSQL("DROP TABLE IF EXISTS foods");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);");
        onCreate(db);
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
        String[] args = new String[]{date, mealType};
        return getReadableDatabase().rawQuery(
                "SELECT me.id, me.grams, me.cal, me.carbs, me.fat, me.protein, f.name " +
                        "FROM meal_entries me JOIN foods f ON me.food_id=f.id " +
                        "WHERE me.entry_date=? AND me.meal_type=? ORDER BY me.id DESC", args);
    }

    public Cursor getTotalsForDay(String date) {
        Log.d(TAG, "getTotalsForDay: date=" + date);
        return getReadableDatabase().rawQuery(
                "SELECT SUM(cal), SUM(carbs), SUM(fat), SUM(protein) FROM meal_entries WHERE entry_date=?",
                new String[]{date}
        );
    }

    public int deleteMeal(long id) {
        Log.d(TAG, "deleteMeal: id=" + id);
        return getWritableDatabase().delete("meal_entries", "id=?", new String[]{String.valueOf(id)});
    }

    private static double round2(double v){ return Math.round(v * 100.0)/100.0; }
}
