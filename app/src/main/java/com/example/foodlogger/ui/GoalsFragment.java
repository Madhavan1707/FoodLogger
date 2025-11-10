package com.example.foodlogger.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodlogger.R;
import com.example.foodlogger.util.GoalPrefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GoalsFragment extends Fragment {

    private TextInputEditText etAge, etHeight, etWeight;
    private RadioButton rbMale, rbFemale;
    private AutoCompleteTextView spActivity;

    private TextInputEditText etKcal, etProt, etCarb, etFat;
    public interface OnGoalsSavedListener {
        void onGoalsSaved();
    }

    private OnGoalsSavedListener goalsSavedListener;


    private final Map<String, Double> activityMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_goals, container, false);

        etAge = v.findViewById(R.id.etAge);
        etHeight = v.findViewById(R.id.etHeight);
        etWeight = v.findViewById(R.id.etWeight);
        rbMale = v.findViewById(R.id.rbMale);
        rbFemale = v.findViewById(R.id.rbFemale);
        spActivity = v.findViewById(R.id.spActivity);

        // Fill dropdown items
        String[] levels = {
                "Sedentary (little or no exercise)",
                "Light (exercise 1–3 days/week)",
                "Moderate (exercise 3–5 days/week)",
                "Active (6–7 days/week)",
                "Very Active (hard daily exercise or physical job)"
        };
        Double[] factors = {1.2, 1.375, 1.55, 1.725, 1.9};
        for (int i = 0; i < levels.length; i++) activityMap.put(levels[i], factors[i]);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, levels);
        spActivity.setAdapter(adapter);
        spActivity.setText(levels[2], false); // default moderate

        MaterialButton btnCalc = v.findViewById(R.id.btnCalc);
        btnCalc.setOnClickListener(view -> {doCalculate();
            etAge.setText("");
            etHeight.setText("");
            etWeight.setText("");
            spActivity.setText("");});

        etKcal = v.findViewById(R.id.etKcal);
        etProt = v.findViewById(R.id.etProt);
        etCarb = v.findViewById(R.id.etCarb);
        etFat = v.findViewById(R.id.etFat);

        // Pre-fill and clear text boxes
        etKcal.setText(String.format(Locale.US, "%.0f", GoalPrefs.kcal(requireContext())));
        etProt.setText(String.format(Locale.US, "%.0f", GoalPrefs.prot(requireContext())));
        etCarb.setText(String.format(Locale.US, "%.0f", GoalPrefs.carb(requireContext())));
        etFat.setText(String.format(Locale.US, "%.0f", GoalPrefs.fat(requireContext())));
        etKcal.getText().clear();
        etProt.getText().clear();
        etCarb.getText().clear();
        etFat.getText().clear();

        MaterialButton btnSave = v.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(view -> {
//            float kcal = parseFloat(etKcal, 0f);
//            float prot = parseFloat(etProt, 0f);
//            float carb = parseFloat(etCarb, 0f);
//            float fat  = parseFloat(etFat,  0f);
            Float kcalF = readFloat(etKcal);
            Float protF = readFloat(etProt);
            Float carbF = readFloat(etCarb);
            Float fatF  = readFloat(etFat);
            // Clear old errors
            etKcal.setError(null);
            etProt.setError(null);
            etCarb.setError(null);
            etFat.setError(null);

            boolean ok = true;
            // Validate: required and > 0
            if (kcalF == null || kcalF <= 0f) { etKcal.setError("Enter kcal > 0"); ok = false; }
            if (protF == null || protF <= 0f) { etProt.setError("Enter protein > 0"); ok = false; }
            if (carbF == null || carbF <= 0f) { etCarb.setError("Enter carbs > 0"); ok = false; }
            if (fatF  == null || fatF  <= 0f) { etFat.setError("Enter fat > 0"); ok = false; }

            if (!ok) {
                android.widget.Toast.makeText(requireContext(), "Please fix the highlighted fields", android.widget.Toast.LENGTH_SHORT).show();
                return; // IMPORTANT: do not proceed or switch tabs
            }

            GoalPrefs.save(requireContext(), kcalF, protF, carbF, fatF);
            Toast.makeText(getContext(), "Goals saved successfully!", Toast.LENGTH_SHORT).show();
            if (goalsSavedListener != null) {
                goalsSavedListener.onGoalsSaved();
            }


            etKcal.setText("");
            etProt.setText("");
            etCarb.setText("");
            etFat.setText("");
        });

        addTrimWatcher(etAge);
        addTrimWatcher(etHeight);
        addTrimWatcher(etWeight);
        addTrimWatcher(etKcal);
        addTrimWatcher(etProt);
        addTrimWatcher(etCarb);
        addTrimWatcher(etFat);

        return v;
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnGoalsSavedListener) {
            goalsSavedListener = (OnGoalsSavedListener) context;
        }
    }

    // NEW: returns null if empty or invalid
    @Nullable
    private Float readFloat(EditText et) {
        String s = et.getText() == null ? "" : et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return null; }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        goalsSavedListener = null;
    }


    private void doCalculate() {
        if (TextUtils.isEmpty(etAge.getText()) || TextUtils.isEmpty(etHeight.getText()) || TextUtils.isEmpty(etWeight.getText())) {
            Toast.makeText(getContext(), "Enter age, height, and weight", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(etAge.getText().toString().trim());
        double h = Double.parseDouble(etHeight.getText().toString().trim());
        double w = Double.parseDouble(etWeight.getText().toString().trim());

        boolean male = rbMale.isChecked();
        double bmr = (10 * w) + (6.25 * h) - (5 * age) + (male ? 5 : -161);

        String selected = spActivity.getText().toString();
        double factor = activityMap.getOrDefault(selected, 1.55);

        double tdee = bmr * factor;

        double protein = 2.0 * w;
        double fat = 0.8 * w;
        double kcalFromProtFat = (protein * 4) + (fat * 9);
        double carbs = Math.max(0, (tdee - kcalFromProtFat) / 4.0);

        etKcal.setText(String.format(Locale.US, "%.0f", tdee));
        etProt.setText(String.format(Locale.US, "%.0f", protein));
        etCarb.setText(String.format(Locale.US, "%.0f", carbs));
        etFat.setText(String.format(Locale.US, "%.0f", fat));

        Toast.makeText(getContext(), "Calculated! Review & Save.", Toast.LENGTH_SHORT).show();
    }

    private static float parseFloat(TextInputEditText et, float defVal) {
        try { return Float.parseFloat(et.getText().toString().trim()); }
        catch (Exception ignore) { return defVal; }
    }

    private static void addTrimWatcher(TextInputEditText et) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s == null) return;
                if (s.length() > 0 && s.charAt(0) == ' ') s.delete(0, 1);
            }
        });
    }
}
