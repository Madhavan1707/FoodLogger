package com.example.foodlogger;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodlogger.ui.LogFoodFragment;
import com.example.foodlogger.ui.LogMealFragment;
import com.example.foodlogger.ui.GoalsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.pager);
        TabLayout tabs = findViewById(R.id.tabs);

        // We now have 3 tabs: Log Meal, Log Food, Goals
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int pos) {
                Log.d(TAG, "createFragment pos=" + pos);
                switch (pos) {
                    case 0:
                        return new LogMealFragment();
                    case 1:
                        return new LogFoodFragment();
                    case 2:
                        return new GoalsFragment();
                    default:
                        return new LogMealFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3; // total tabs
            }
        });

        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            if (pos == 0) tab.setText("Log Meal");
            else if (pos == 1) tab.setText("Log Food");
            else tab.setText("Goals");
        }).attach();
    }
}
