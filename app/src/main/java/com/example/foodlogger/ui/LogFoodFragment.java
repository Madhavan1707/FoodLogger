package com.example.foodlogger.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodlogger.R;
import com.example.foodlogger.db.DBHelper;
import com.example.foodlogger.ui.adapter.FoodListAdapter;
import android.util.Log;

import java.util.Locale;

public class LogFoodFragment extends Fragment {
    private static final String TAG = "LogFoodFrag";
    private DBHelper db; private FoodListAdapter adapter; private RecyclerView rv;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        Log.d(TAG, "onCreateView");
        View v = inf.inflate(R.layout.fragment_log_food, c, false);
        db = new DBHelper(requireContext());
        rv = v.findViewById(R.id.foodList); rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FoodListAdapter(); rv.setAdapter(adapter);

        Button add = v.findViewById(R.id.btnAdd);
        EditText name=v.findViewById(R.id.etName), cal=v.findViewById(R.id.etCal), carbs=v.findViewById(R.id.etCarbs),
                fat=v.findViewById(R.id.etFat), prot=v.findViewById(R.id.etProt);

        add.setOnClickListener(view -> {
            Log.d(TAG, "Add clicked");

            // Collect missing numeric fields
            StringBuilder missing = new StringBuilder();
            if(TextUtils.isEmpty(cal.getText()))   missing.append("calories, ");
            if(TextUtils.isEmpty(carbs.getText())) missing.append("carbs, ");
            if(TextUtils.isEmpty(fat.getText()))   missing.append("fat, ");
            if(TextUtils.isEmpty(prot.getText()))  missing.append("protein, ");

            if(TextUtils.isEmpty(name.getText())) { name.setError("Required"); }

            if(missing.length() > 0) {
                String msg = missing.substring(0, missing.length()-2);
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Missing values")
                        .setMessage("Please enter: " + msg)
                        .setPositiveButton("OK", null)
                        .show();
                Log.w(TAG, "Missing fields: " + msg);
                return;
            }
            if(TextUtils.isEmpty(name.getText())) { return; }

            try {
                double vCal = Double.parseDouble(cal.getText().toString());
                double vCarbs = Double.parseDouble(carbs.getText().toString());
                double vFat = Double.parseDouble(fat.getText().toString());
                double vProt = Double.parseDouble(prot.getText().toString());
                String rawName = name.getText().toString();
                String titleName = toTitleCase(rawName);
// keep validation as-is, then:
                long id = db.insertFood(titleName, vCal, vCarbs, vFat, vProt);
                Log.d(TAG, "Food inserted id=" + id);
                name.setText(""); cal.setText(""); carbs.setText(""); fat.setText(""); prot.setText("");
                load();
            } catch (NumberFormatException e){
                Log.e(TAG, "Parsing error", e);
                Toast.makeText(getContext(), "Enter valid numbers for calories/carbs/fat/protein", Toast.LENGTH_SHORT).show();
            } catch (Exception e){
                Log.e(TAG, "Insert error", e);
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        load();
        return v;
    }
    private static String toTitleCase(String s){
        if (s == null) return "";
        s = s.trim().toLowerCase(Locale.US);
        StringBuilder out = new StringBuilder(s.length());
        boolean cap = true;
        for (int i=0;i<s.length();i++){
            char ch = s.charAt(i);
            if (Character.isLetter(ch)) {
                out.append(cap ? Character.toTitleCase(ch) : ch);
                cap = false;
            } else {
                out.append(ch);
                // start a new word after space or punctuation
                cap = Character.isWhitespace(ch) || ch=='-' || ch=='/' || ch=='(' || ch==')';
            }
        }
        return out.toString();
    }





    private void load(){
        Cursor c = db.getAllFoods();
        Log.d(TAG, "load foods count=" + (c==null?0:c.getCount()));
        adapter.submitCursor(c);
    }
}
