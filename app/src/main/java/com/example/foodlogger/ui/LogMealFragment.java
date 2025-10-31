package com.example.foodlogger.ui;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodlogger.R;
import com.example.foodlogger.db.DBHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class LogMealFragment extends Fragment {
    private static final String TAG = "LogMealFrag";

    private DBHelper db;
    private RecyclerView rvMeals;
    private EditText etDate, etSearch;
    private RadioGroup rgMeal;
    private MealEntryAdapter mealAdapter;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private View root;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        root = inf.inflate(R.layout.fragment_log_meal, container, false);

        db = new DBHelper(requireContext());
        etDate = root.findViewById(R.id.etDate);
        etDate.setText(fmt.format(new Date()));
        etDate.setOnClickListener(vw -> pickDate());

        rgMeal = root.findViewById(R.id.rgMeal);

        // Search bar opens picker dialog only when tapped
        etSearch = root.findViewById(R.id.etSearch);
        etSearch.setOnClickListener(v -> openFoodPickerDialog());

        rvMeals = root.findViewById(R.id.rvMeals);
        rvMeals.setLayoutManager(new LinearLayoutManager(getContext()));
        mealAdapter = new MealEntryAdapter(id -> {
            Log.d(TAG, "delete meal id=" + id);
            db.deleteMeal(id);
            loadMeals();
            loadTotals(root);
        });
        rvMeals.setAdapter(mealAdapter);

        rgMeal.setOnCheckedChangeListener((g, id) -> {
            Log.d(TAG, "meal changed " + currentMealType());
            loadMeals();
        });

        // initial loads
        loadMeals();
        loadTotals(root);

        return root;
    }

    private void openFoodPickerDialog() {
        // Build a simple dialog with (search + list) entirely in code to avoid extra XML files
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etQuery = new EditText(getContext());
        etQuery.setHint("Search food…");
        etQuery.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        container.addView(etQuery);

        RecyclerView rv = new RecyclerView(getContext());
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        container.addView(rv);

        FoodsAdapterForDialog adapter = new FoodsAdapterForDialog((foodId, name, cal, carbs, fat, prot) -> {
            // After pick, ask grams then insert
            showAddGramsDialog(foodId, name, cal, carbs, fat, prot);
        });
        rv.setAdapter(adapter);

        // initial load (no filter)
        adapter.submitCursor(db.getAllFoods());

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString();
                Cursor c = q.trim().isEmpty() ? db.getAllFoods() : db.getFoodsFiltered(q);
                adapter.submitCursor(c);
            }
        });

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Add food to " + currentMealType())
                .setView(container)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddGramsDialog(long foodId, String name, double cal, double carbs, double fat, double prot){
        LinearLayout container1 = new LinearLayout(getContext());
        container1.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container1.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(getContext());
        hint.setText("Enter amount (grams). For liquids, ml ≈ grams.");
        container1.addView(hint);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("e.g., 150");
        container1.addView(input);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button b50 = new Button(getContext()); b50.setText("50g");
        Button b100 = new Button(getContext()); b100.setText("100g");
        Button b250 = new Button(getContext()); b250.setText("250g");
        View.OnClickListener setVal = v -> {
            String t = ((Button)v).getText().toString().replace("g","");
            input.setText(t);
            input.setSelection(input.getText().length());
        };
        b50.setOnClickListener(setVal); b100.setOnClickListener(setVal); b250.setOnClickListener(setVal);
        row.addView(b50); row.addView(b100); row.addView(b250);
        container1.addView(row);

        if (name.toLowerCase(Locale.US).contains("egg")) {
            LinearLayout eggRow = new LinearLayout(getContext());
            eggRow.setOrientation(LinearLayout.HORIZONTAL);
            Button wholeEgg = new Button(getContext()); wholeEgg.setText("1 egg (~50g)");
            Button whiteEgg = new Button(getContext()); whiteEgg.setText("1 white (~30g)");
            wholeEgg.setOnClickListener(v -> { input.setText("50"); input.setSelection(input.getText().length()); });
            whiteEgg.setOnClickListener(v -> { input.setText("30"); input.setSelection(input.getText().length()); });
            eggRow.addView(wholeEgg); eggRow.addView(whiteEgg);
            container1.addView(eggRow);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Add " + name)
                .setView(container1)
                .setPositiveButton("Add", (d,w)->{
                    String s = input.getText().toString().trim();
                    if(s.isEmpty()){
                        Log.w(TAG, "grams empty");
                        Toast.makeText(getContext(), "Please enter grams/ml", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double grams = Double.parseDouble(s);
                        if(grams <= 0){
                            Toast.makeText(getContext(), "Amount must be > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String date = etDate.getText().toString(); String mealType = currentMealType();
                        Log.d(TAG, "insertMeal start: " + date + " " + mealType + " grams=" + grams);
                        long id = db.insertMeal(date, mealType, foodId, grams, cal, carbs, fat, prot);
                        Log.d(TAG, "insertMeal done rowId=" + id);
                        loadMeals();
                        loadTotals(root);
                    } catch (NumberFormatException ex){
                        Log.e(TAG, "grams parse error", ex);
                        Toast.makeText(getContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void pickDate(){
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(), (dp,y,m,d)->{
            Calendar cc = Calendar.getInstance(); cc.set(y,m,d);
            etDate.setText(fmt.format(cc.getTime()));
            Log.d(TAG, "date chosen=" + etDate.getText());
            loadMeals();
            loadTotals(root);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String currentMealType(){
        int id = rgMeal.getCheckedRadioButtonId();
        if(id==R.id.rbBreakfast) return "Breakfast";
        if(id==R.id.rbLunch) return "Lunch";
        if(id==R.id.rbSnack) return "Snack";
        return "Dinner";
    }

    private void loadMeals(){
        Cursor c = db.getMealsFor(etDate.getText().toString(), currentMealType());
        Log.d(TAG, "meals count=" + (c==null?0:c.getCount()));
        mealAdapter.submitCursor(c);

        TextView empty = root.findViewById(R.id.tvEmptyMeals);
        if (empty != null) {
            empty.setVisibility((c == null || c.getCount() == 0) ? View.VISIBLE : View.GONE);
        }
    }

    private void loadTotals(View anchorRoot){
        Cursor c = db.getTotalsForDay(etDate.getText().toString());
        if(c.moveToFirst()){
            double cal=c.isNull(0)?0:c.getDouble(0),
                    carbs=c.isNull(1)?0:c.getDouble(1),
                    fat=c.isNull(2)?0:c.getDouble(2),
                    prot=c.isNull(3)?0:c.getDouble(3);
            Log.d(TAG, "totals cal=" + cal + " C=" + carbs + " F=" + fat + " P=" + prot);
            TextView tv = anchorRoot.findViewById(R.id.tvTotals);
            if (tv != null) {
                tv.setText(String.format(Locale.US,
                        "Total today: %.0f kcal | C %.1f | F %.1f | P %.1f",
                        cal, carbs, fat, prot));
            }
        }
    }

    // ---------- Adapters ----------

    interface OnFoodPick { void onPick(long id, String name, double cal, double carbs, double fat, double prot); }

    // Simple adapter used inside the picker dialog
    static class FoodsAdapterForDialog extends RecyclerView.Adapter<FoodsAdapterForDialog.VH>{
        private Cursor cursor; private final OnFoodPick cb; private static final String TAG = "FoodsPicker";
        FoodsAdapterForDialog(OnFoodPick cb){ this.cb=cb; }
        void submitCursor(Cursor c){ this.cursor=c; notifyDataSetChanged(); Log.d(TAG, "submitCursor count=" + (c==null?0:c.getCount())); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_food, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos){
            cursor.moveToPosition(pos);
            long id = cursor.getLong(0); String name = cursor.getString(1);
            double cal=cursor.getDouble(2), carbs=cursor.getDouble(3), fat=cursor.getDouble(4), prot=cursor.getDouble(5);
            h.name.setText(name);
            h.macros.setText(String.format(Locale.US, "Per 100g: %.0f kcal | C %.1f | F %.1f | P %.1f", cal, carbs, fat, prot));
            h.itemView.setOnClickListener(v->{ Log.d(TAG, "pick pos=" + pos + " id=" + id); cb.onPick(id,name,cal,carbs,fat,prot); });
        }
        @Override public int getItemCount(){ return cursor==null?0:cursor.getCount(); }
        static class VH extends RecyclerView.ViewHolder{
            TextView name, macros; VH(View v){ super(v); name=v.findViewById(R.id.tvName); macros=v.findViewById(R.id.tvMacros); }
        }
    }

    static class MealEntryAdapter extends RecyclerView.Adapter<MealEntryAdapter.VH> {
        private Cursor cursor; interface OnDelete{ void onDel(long id);} private final OnDelete onDelete; private static final String TAG = "MealEntryAdp";
        MealEntryAdapter(OnDelete onDelete){ this.onDelete = onDelete; }
        void submitCursor(Cursor c){ this.cursor=c; notifyDataSetChanged(); Log.d(TAG, "submitCursor count=" + (c==null?0:c.getCount())); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_meal_entry, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos){
            cursor.moveToPosition(pos);
            long id = cursor.getLong(0); double grams=cursor.getDouble(1), cal=cursor.getDouble(2),
                    carbs=cursor.getDouble(3), fat=cursor.getDouble(4), prot=cursor.getDouble(5);
            String name = cursor.getString(6);
            h.top.setText(String.format(Locale.US, "%s — %.0fg", name, grams));
            h.bottom.setText(String.format(Locale.US, "%.0f kcal | C %.1f | F %.1f | P %.1f", cal, carbs, fat, prot));
            h.del.setOnClickListener(v->{ Log.d(TAG, "delete click id=" + id); onDelete.onDel(id); });
        }
        @Override public int getItemCount(){ return cursor==null?0:cursor.getCount(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView top,bottom; ImageButton del;
            VH(View v){ super(v); top=v.findViewById(R.id.tvTop); bottom=v.findViewById(R.id.tvBottom); del=v.findViewById(R.id.btnDelete); }
        }
    }
}
