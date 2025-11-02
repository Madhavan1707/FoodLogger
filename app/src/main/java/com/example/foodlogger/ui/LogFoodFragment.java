package com.example.foodlogger.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodlogger.R;
import com.example.foodlogger.db.DBHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.function.BiFunction;

public class LogFoodFragment extends Fragment {
    private static final String TAG = "LogFoodFragment";

    private DBHelper db;
    private RecyclerView rv;
    private FoodsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inf.inflate(R.layout.fragment_log_food, container, false);
        db = new DBHelper(requireContext());

        rv = root.findViewById(R.id.rvFoods);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setHasFixedSize(true);
        adapter = new FoodsAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = root.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddFoodDialog());

        loadFoods();
        return root;
    }

    private void loadFoods() {
        Cursor c = db.getAllFoods();
        adapter.submitCursor(c);
        Log.d(TAG, "Foods loaded: " + c.getCount());
    }

    private void showAddFoodDialog() {
        LinearLayout container = makeFoodForm(null);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Food")
                .setView(container)
                .setPositiveButton("Add", (d, w) -> {
                    EditText etName = container.findViewById(R.id.etName);
                    EditText etCal = container.findViewById(R.id.etCal);
                    EditText etCarb = container.findViewById(R.id.etCarb);
                    EditText etFat = container.findViewById(R.id.etFat);
                    EditText etProt = container.findViewById(R.id.etProt);

                    String name = etName.getText().toString().trim();
                    String calS = etCal.getText().toString().trim();
                    String carbS = etCarb.getText().toString().trim();
                    String fatS = etFat.getText().toString().trim();
                    String protS = etProt.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(calS) ||
                            TextUtils.isEmpty(carbS) || TextUtils.isEmpty(fatS) || TextUtils.isEmpty(protS)) {
                        Toast.makeText(getContext(), "Please enter all values", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    name = capitalizeWords(name);
                    long id = db.insertFood(
                            name,
                            Double.parseDouble(calS),
                            Double.parseDouble(carbS),
                            Double.parseDouble(fatS),
                            Double.parseDouble(protS)
                    );
                    if (id == -1) {
                        Toast.makeText(getContext(), "Error: Could not add food", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Food added", Toast.LENGTH_SHORT).show();
                        loadFoods();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Creates the reusable input layout, optionally prefilled with existing values. */
    private LinearLayout makeFoodForm(@Nullable Cursor existing) {
        Context ctx = getContext();
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        // helper for creating field pairs
        BiFunction<String, Integer, TextInputLayout> makeField = (hint, id) -> {
            TextInputLayout layout = new TextInputLayout(ctx);
            layout.setHint(hint);
            layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
            layout.setBoxCornerRadii(12,12,12,12);
            TextInputEditText et = new TextInputEditText(ctx);
            et.setId(id);
            layout.addView(et);
            container.addView(layout);
            return layout;
        };

        makeField.apply("Food Name", R.id.etName);
        makeField.apply("Calories / 100g", R.id.etCal);
        makeField.apply("Carbs / 100g", R.id.etCarb);
        makeField.apply("Fat / 100g", R.id.etFat);
        makeField.apply("Protein / 100g", R.id.etProt);

        if (existing != null) {
            ((EditText) container.findViewById(R.id.etName)).setText(existing.getString(1));
            ((EditText) container.findViewById(R.id.etCal)).setText(String.format(Locale.US, "%.0f", existing.getDouble(2)));
            ((EditText) container.findViewById(R.id.etCarb)).setText(String.format(Locale.US, "%.1f", existing.getDouble(3)));
            ((EditText) container.findViewById(R.id.etFat)).setText(String.format(Locale.US, "%.1f", existing.getDouble(4)));
            ((EditText) container.findViewById(R.id.etProt)).setText(String.format(Locale.US, "%.1f", existing.getDouble(5)));
        }

        return container;
    }


    /** Capitalize each word (like “guava shake” → “Guava Shake”). */
    private static String capitalizeWords(String input) {
        String[] words = input.toLowerCase(Locale.US).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    // ---------------- Adapter ----------------

    private class FoodsAdapter extends RecyclerView.Adapter<FoodsAdapter.VH> {
        private Cursor cursor;

        void submitCursor(Cursor c) {
            cursor = c;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_food, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            cursor.moveToPosition(pos);
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            double cal = cursor.getDouble(2), carb = cursor.getDouble(3),
                    fat = cursor.getDouble(4), prot = cursor.getDouble(5);
            h.tvName.setText(name);
            h.tvMacros.setText(String.format(Locale.US, "Per 100g: %.0f kcal | C %.1f | F %.1f | P %.1f", cal, carb, fat, prot));

            h.itemView.setOnClickListener(v -> showOptionsDialog(id));
        }

        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvMacros;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvMacros = v.findViewById(R.id.tvMacros);
            }
        }
    }

    // ---------------- Edit/Delete Flow ----------------

    private void showOptionsDialog(long foodId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Choose Action")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) showEditDialog(foodId);
                    else confirmDelete(foodId);
                })
                .show();
    }

    private void confirmDelete(long foodId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Food")
                .setMessage("Are you sure you want to delete this food?")
                .setPositiveButton("Yes", (d, w) -> {
                    db.deleteFood(foodId);
                    Toast.makeText(getContext(), "Food deleted", Toast.LENGTH_SHORT).show();
                    loadFoods();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(long foodId) {
        Cursor c = db.getFoodById(foodId);
        if (c == null || !c.moveToFirst()) {
            Toast.makeText(getContext(), "Error loading food", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout container = makeFoodForm(c);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Food")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    EditText etName = container.findViewById(R.id.etName);
                    EditText etCal = container.findViewById(R.id.etCal);
                    EditText etCarb = container.findViewById(R.id.etCarb);
                    EditText etFat = container.findViewById(R.id.etFat);
                    EditText etProt = container.findViewById(R.id.etProt);

                    String name = etName.getText().toString().trim();
                    String calS = etCal.getText().toString().trim();
                    String carbS = etCarb.getText().toString().trim();
                    String fatS = etFat.getText().toString().trim();
                    String protS = etProt.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(calS)
                            || TextUtils.isEmpty(carbS) || TextUtils.isEmpty(fatS)
                            || TextUtils.isEmpty(protS)) {
                        Toast.makeText(getContext(), "All fields required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    name = capitalizeWords(name);
                    db.updateFood(foodId,
                            name,
                            Double.parseDouble(calS),
                            Double.parseDouble(carbS),
                            Double.parseDouble(fatS),
                            Double.parseDouble(protS));
                    Toast.makeText(getContext(), "Food updated", Toast.LENGTH_SHORT).show();
                    loadFoods();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
