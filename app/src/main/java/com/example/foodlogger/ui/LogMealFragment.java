package com.example.foodlogger.ui;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodlogger.R;import android.text.TextUtils;
import com.example.foodlogger.util.GoalPrefs;
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
    // Daily goals (adjust later or move to settings)
    private static final double DAILY_KCAL_GOAL = 1800;
    private static final double DAILY_PROT_GOAL = 130;
    private static final double DAILY_CARB_GOAL = 160;
    private static final double DAILY_FAT_GOAL  = 50;

    // 2x2 meal buttons
    private RadioButton rbBreakfast, rbLunch, rbSnack, rbDinner;

    private MealEntryAdapter mealAdapter;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private View root;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        root = inf.inflate(R.layout.fragment_log_meal, container, false);

        db = new DBHelper(requireContext());

        etDate = root.findViewById(R.id.etDate);
        etDate.setText(fmt.format(new Date()));
        etDate.setOnClickListener(vw -> pickDate());

        // find 2x2 radio buttons
        rbBreakfast = root.findViewById(R.id.rbBreakfast);
        rbLunch     = root.findViewById(R.id.rbLunch);
        rbSnack     = root.findViewById(R.id.rbSnack);
        rbDinner    = root.findViewById(R.id.rbDinner);

        setupMealButtons(); // handles exclusivity + reload

        // Search bar opens picker dialog
        etSearch = root.findViewById(R.id.etSearch);
        etSearch.setOnClickListener(v -> openFoodPickerDialog());

        // Logged items list
        rvMeals = root.findViewById(R.id.rvMeals);
        rvMeals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMeals.setHasFixedSize(false);
        rvMeals.setNestedScrollingEnabled(false);

        mealAdapter = new MealEntryAdapter(
                id -> {
                    Log.d(TAG, "delete meal id=" + id);
                    db.deleteMeal(id);
                    loadMeals();
                    loadTotals(root);
                },
                (id, grams, cal, carbs, fat, prot, name) -> {
                    showEditMealDialog(id, grams, cal, carbs, fat, prot, name);
                }
        );

        rvMeals.setAdapter(mealAdapter);

        // initial loads
        loadMeals();
        loadTotals(root);

        return root;
    }
    private void showEditMealDialog(long mealId, double oldGrams, double oldCal, double oldCarbs, double oldFat, double oldProt, String name){
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(getContext());
        hint.setText("Edit amount (grams). Macros will adjust automatically.");
        box.addView(hint);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("grams");
        input.setText(String.format(Locale.US, "%.0f", oldGrams));
        box.addView(input);

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Edit " + name)
                .setView(box)
                .setPositiveButton("Save", (d,w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter grams", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double newGrams = Double.parseDouble(s);
                        if (newGrams <= 0) {
                            Toast.makeText(getContext(), "Amount must be > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Scale macros linearly by grams change
                        double factor = newGrams / oldGrams;
                        double newCal   = Math.round(oldCal   * factor);
                        double newCarbs = Math.round(oldCarbs * factor * 10.0) / 10.0;
                        double newFat   = Math.round(oldFat   * factor * 10.0) / 10.0;
                        double newProt  = Math.round(oldProt  * factor * 10.0) / 10.0;

                        int rows = db.updateMealValues(mealId, newGrams, newCal, newCarbs, newFat, newProt);
                        Log.d(TAG, "updateMealValues rows=" + rows);

                        loadMeals();
                        loadTotals(root);
                        Toast.makeText(getContext(), "Updated", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException ex){
                        Log.e(TAG, "edit grams parse error", ex);
                        Toast.makeText(getContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private void setupMealButtons() {
        // default selection
        rbBreakfast.setChecked(true);

        CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
            if (!isChecked) return;
            // manual exclusivity
            rbBreakfast.setChecked(button == rbBreakfast);
            rbLunch.setChecked(button == rbLunch);
            rbSnack.setChecked(button == rbSnack);
            rbDinner.setChecked(button == rbDinner);
            // refresh list for selected meal
            loadMeals();
        };

        rbBreakfast.setOnCheckedChangeListener(listener);
        rbLunch.setOnCheckedChangeListener(listener);
        rbSnack.setOnCheckedChangeListener(listener);
        rbDinner.setOnCheckedChangeListener(listener);
    }
    private CharSequence boldLine(String label, String content) {
        SpannableStringBuilder sb = new SpannableStringBuilder(label + " " + content);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }


    private void openFoodPickerDialog() {
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
            showAddGramsDialog(foodId, name, cal, carbs, fat, prot);
        });
        rv.setAdapter(adapter);
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
    @Override
    public void onResume() {
        super.onResume();
        if (root != null) {
            loadTotals(root);   // re-pulls values from GoalPrefs each time
        }
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
        if (rbBreakfast.isChecked()) return "Breakfast";
        if (rbLunch.isChecked())     return "Lunch";
        if (rbSnack.isChecked())     return "Snack";
        return "Dinner";
    }

    private void loadMeals(){
        Cursor c = db.getMealsFor(etDate.getText().toString(), currentMealType());
        Log.d(TAG, "meals count=" + (c==null?0:c.getCount()));
        mealAdapter.submitCursor(c);

        boolean hasMeals = (c != null && c.getCount() > 0);

        TextView empty = root.findViewById(R.id.tvEmptyMeals);
        View divider   = root.findViewById(R.id.viewDivider);     // if you added it earlier
        View cardMeals = root.findViewById(R.id.cardMeals);       // NEW

        if (empty != null)   empty.setVisibility(hasMeals ? View.GONE : View.VISIBLE);
        if (divider != null) divider.setVisibility(hasMeals ? View.VISIBLE : View.GONE);
        if (cardMeals != null) cardMeals.setVisibility(hasMeals ? View.VISIBLE : View.GONE);
    }
    private void loadTotals(View anchorRoot){
        Cursor c = db.getTotalsForDay(etDate.getText().toString());
        if (c.moveToFirst()) {
            double cal   = c.isNull(0) ? 0 : c.getDouble(0);
            double carbs = c.isNull(1) ? 0 : c.getDouble(1);
            double fat   = c.isNull(2) ? 0 : c.getDouble(2);
            double prot  = c.isNull(3) ? 0 : c.getDouble(3);

            // Read current goals from SharedPreferences (falls back to defaults)
            double GOAL_KCAL = GoalPrefs.kcal(requireContext());
            double GOAL_PROT = GoalPrefs.prot(requireContext());
            double GOAL_CARB = GoalPrefs.carb(requireContext());
            double GOAL_FAT  = GoalPrefs.fat (requireContext());

            // 1) Running totals
            TextView tvRun = anchorRoot.findViewById(R.id.tvRunningDetails);
            if (tvRun != null) {
                int NEUTRAL = 0xFF000000;
                int GREEN   = 0xFF388E3C;  // under goal (good)
                int ORANGE  = 0xFFFFB74D;  // mild over
                int RED     = 0xFFE57373;  // strong over

// Calories: under -> green; +10–200 -> orange; >200 -> red; 0–9 over -> neutral
                double calDiff = cal - GOAL_KCAL;
                int colorCal = (calDiff < 0) ? GREEN
                        : (calDiff < 10) ? NEUTRAL
                        : (calDiff <= 200) ? ORANGE
                        : RED;

// Protein: keep previous rule (met/over = green, else neutral)
                // Protein: under -> red; equal or over -> green
                int colorProt = (prot < GOAL_PROT) ? RED : GREEN;


// Carbs: under -> green; +31–50g -> orange; >50g -> red; 0–30 over -> neutral
                double carbDiff = carbs - GOAL_CARB;
                int colorCarb = (carbDiff < 0) ? GREEN
                        : (carbDiff <= 30) ? NEUTRAL
                        : (carbDiff <= 50) ? ORANGE
                        : RED;

// Fat: under -> green; +31–50g -> orange; >50g -> red; 0–30 over -> neutral
                double fatDiff = fat - GOAL_FAT;
                int colorFat = (fatDiff < 0) ? GREEN
                        : (fatDiff <= 30) ? NEUTRAL
                        : (fatDiff <= 50) ? ORANGE
                        : RED;


                SpannableStringBuilder sb = new SpannableStringBuilder();

                java.util.function.BiConsumer<String, Runnable> appendBoldLabel = (label, after) -> {
                    int start = sb.length();
                    sb.append(label).append(" ");
                    sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    after.run();
                    sb.append("\n");
                };

// Calories
                appendBoldLabel.accept("Calories:", () -> {
                    String left  = String.format(Locale.US, "%.0f", cal);
                    String right = String.format(Locale.US, " / %.0f kcal", GOAL_KCAL);
                    int s = sb.length();
                    sb.append(left);
                    sb.setSpan(new android.text.style.ForegroundColorSpan(colorCal), s, s + left.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(right);
                });

// Protein
                appendBoldLabel.accept("Protein:", () -> {
                    String left  = String.format(Locale.US, "%.1f", prot);
                    String right = String.format(Locale.US, " / %.0f g", GOAL_PROT);
                    int s = sb.length();
                    sb.append(left);
                    sb.setSpan(new android.text.style.ForegroundColorSpan(colorProt), s, s + left.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(right);
                });

// Carbs
                appendBoldLabel.accept("Carbs:", () -> {
                    String left  = String.format(Locale.US, "%.1f", carbs);
                    String right = String.format(Locale.US, " / %.0f g", GOAL_CARB);
                    int s = sb.length();
                    sb.append(left);
                    sb.setSpan(new android.text.style.ForegroundColorSpan(colorCarb), s, s + left.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(right);
                });

// Fat
                appendBoldLabel.accept("Fat:", () -> {
                    String left  = String.format(Locale.US, "%.1f", fat);
                    String right = String.format(Locale.US, " / %.0f g", GOAL_FAT);
                    int s = sb.length();
                    sb.append(left);
                    sb.setSpan(new android.text.style.ForegroundColorSpan(colorFat), s, s + left.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(right);
                });

                sb.delete(sb.length() - 1, sb.length()); // remove last \n
                tvRun.setText(sb);


            }

            // 2) Remaining
            TextView tvRem = anchorRoot.findViewById(R.id.tvRemainingDetails);
            if (tvRem != null) {
                double kcalLeft = GOAL_KCAL - cal;
                double protLeft = GOAL_PROT - prot;
                double carbLeft = GOAL_CARB - carbs;
                double fatLeft  = GOAL_FAT  - fat;

                CharSequence line1 = boldLine("Calories left:", String.format(Locale.US, "%.0f kcal", kcalLeft));
                CharSequence line2 = boldLine("Protein left:",  String.format(Locale.US, "%.1f g",   protLeft));
                CharSequence line3 = boldLine("Carbs left:",    String.format(Locale.US, "%.1f g",   carbLeft));
                CharSequence line4 = boldLine("Fat left:",      String.format(Locale.US, "%.1f g",   fatLeft));
                tvRem.setText(android.text.TextUtils.concat(line1, "\n", line2, "\n", line3, "\n", line4));
            }
        }
    }






    // ---------- Adapters (unchanged) ----------

    interface OnFoodPick { void onPick(long id, String name, double cal, double carbs, double fat, double prot); }

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
        interface OnDelete { void onDel(long id); }
        interface OnEdit   { void onEdit(long id, double grams, double cal, double carbs, double fat, double prot, String name); }

        private Cursor cursor;
        private final OnDelete onDelete;
        private final OnEdit onEdit;
        private static final String TAG = "MealEntryAdp";

        MealEntryAdapter(OnDelete onDelete, OnEdit onEdit){
            this.onDelete = onDelete;
            this.onEdit = onEdit;
        }
        void submitCursor(Cursor c){ this.cursor=c; notifyDataSetChanged(); Log.d(TAG, "submitCursor count=" + (c==null?0:c.getCount())); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_meal_entry, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos){
            cursor.moveToPosition(pos);
            long id = cursor.getLong(0);
            double grams = cursor.getDouble(1), cal = cursor.getDouble(2),
                    carbs = cursor.getDouble(3), fat = cursor.getDouble(4), prot = cursor.getDouble(5);
            String name = cursor.getString(6);

            h.top.setText(String.format(Locale.US, "%s — %.0fg", name, grams));
            h.bottom.setText(String.format(Locale.US, "%.0f kcal | C %.1f | F %.1f | P %.1f", cal, carbs, fat, prot));

            h.itemView.setOnClickListener(v -> {
                Log.d(TAG, "edit click id=" + id);
                onEdit.onEdit(id, grams, cal, carbs, fat, prot, name);
            });

            h.del.setOnClickListener(v -> {
                Log.d(TAG, "delete click id=" + id);
                onDelete.onDel(id);
            });
        }
        @Override public int getItemCount(){ return cursor==null?0:cursor.getCount(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView top,bottom; ImageButton del;
            VH(View v){ super(v); top=v.findViewById(R.id.tvTop); bottom=v.findViewById(R.id.tvBottom); del=v.findViewById(R.id.btnDelete); }
        }
    }
}
