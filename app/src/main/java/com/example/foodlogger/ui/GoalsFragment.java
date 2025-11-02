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
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodlogger.R;
import com.example.foodlogger.util.GoalPrefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class GoalsFragment extends Fragment {

    private TextInputEditText etAge, etHeight, etWeight;
    private RadioButton rbMale, rbFemale;
    private AutoCompleteTextView spActivity;

    private TextInputEditText etKcal, etProt, etCarb, etFat;

    private static class ActivityLevel {
        final String label; final double factor;
        ActivityLevel(String l, double f){ label=l; factor=f; }
        @Override public String toString(){ return label; }
    }

    private final ActivityLevel[] ACTIVITY = new ActivityLevel[]{
            new ActivityLevel("Sedentary: little or no exercise", 1.2),
            new ActivityLevel("Light: exercise 1–3 times/week", 1.375),
            new ActivityLevel("Moderate: exercise 3–5 times/week", 1.55),
            new ActivityLevel("Active: 6–7 times/week", 1.725),
            new ActivityLevel("Very Active: hard daily exercise or physical job", 1.9)
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_goals, container, false);

        // Auto-calc inputs
        etAge    = v.findViewById(R.id.etAge);
        etHeight = v.findViewById(R.id.etHeight);
        etWeight = v.findViewById(R.id.etWeight);
        rbMale   = v.findViewById(R.id.rbMale);
        rbFemale = v.findViewById(R.id.rbFemale);
        spActivity = v.findViewById(R.id.spActivity);

        spActivity.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, ACTIVITY));
        spActivity.setText(ACTIVITY[2].label, false); // default Moderate

        MaterialButton btnCalc = v.findViewById(R.id.btnCalc);
        btnCalc.setOnClickListener(view -> doCalculate());

        // Manual override
        etKcal = v.findViewById(R.id.etKcal);
        etProt = v.findViewById(R.id.etProt);
        etCarb = v.findViewById(R.id.etCarb);
        etFat  = v.findViewById(R.id.etFat);

        // Pre-fill from saved goals
        etKcal.setText(String.format(Locale.US, "%.0f", GoalPrefs.kcal(requireContext())));
        etProt.setText(String.format(Locale.US, "%.0f", GoalPrefs.prot(requireContext())));
        etCarb.setText(String.format(Locale.US, "%.0f", GoalPrefs.carb(requireContext())));
        etFat .setText(String.format(Locale.US, "%.0f", GoalPrefs.fat (requireContext())));
        // Optional: clear textboxes visually every time fragment opens
        etKcal.getText().clear();
        etProt.getText().clear();
        etCarb.getText().clear();
        etFat.getText().clear();


        MaterialButton btnSave = v.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(view -> {
            float kcal = parseFloat(etKcal, GoalPrefs.DEF_KCAL);
            float prot = parseFloat(etProt, GoalPrefs.DEF_PROT);
            float carb = parseFloat(etCarb, GoalPrefs.DEF_CARB);
            float fat  = parseFloat(etFat,  GoalPrefs.DEF_FAT);

            GoalPrefs.save(requireContext(), kcal, prot, carb, fat);
            Toast.makeText(getContext(), "Goals saved successfully!", Toast.LENGTH_SHORT).show();

            // clear the textboxes after saving
            etKcal.setText("");
            etProt.setText("");
            etCarb.setText("");
            etFat.setText("");
        });


        // keep text tidy (no leading zeros/spaces)
        addTrimWatcher(etAge); addTrimWatcher(etHeight); addTrimWatcher(etWeight);
        addTrimWatcher(etKcal); addTrimWatcher(etProt); addTrimWatcher(etCarb); addTrimWatcher(etFat);

        return v;
    }

    private void doCalculate() {
        // Validate
        if (TextUtils.isEmpty(etAge.getText()) || TextUtils.isEmpty(etHeight.getText()) || TextUtils.isEmpty(etWeight.getText())) {
            Toast.makeText(getContext(), "Enter age, height, weight", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(etAge.getText().toString().trim());
        double h = Double.parseDouble(etHeight.getText().toString().trim()); // cm
        double w = Double.parseDouble(etWeight.getText().toString().trim()); // kg

        boolean male = rbMale.isChecked();
        double bmr = (10 * w) + (6.25 * h) - (5 * age) + (male ? 5 : -161);

        double factor = ACTIVITY[2].factor; // default Moderate
        String sel = spActivity.getText() == null ? "" : spActivity.getText().toString();
        for (ActivityLevel a : ACTIVITY) if (a.label.equals(sel)) { factor = a.factor; break; }

        double tdee = bmr * factor;

        // Simple macro targets: Protein 2.0 g/kg, Fat 0.8 g/kg, Carbs from remainder
        double protein = 2.0 * w;
        double fat = 0.8 * w;
        double kcalFromProtFat = (protein * 4) + (fat * 9);
        double carbs = Math.max(0, (tdee - kcalFromProtFat) / 4.0);

        // write into manual fields so user can tweak then Save
        etKcal.setText(String.format(Locale.US, "%.0f", tdee));
        etProt.setText(String.format(Locale.US, "%.0f", protein));
        etCarb.setText(String.format(Locale.US, "%.0f", carbs));
        etFat .setText(String.format(Locale.US, "%.0f", fat));

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
                int len = s.length();
                if (len > 0 && s.charAt(0) == ' ') { s.delete(0, 1); }
            }
        });
    }
}
