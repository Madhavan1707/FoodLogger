package com.example.foodlogger;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodlogger.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.foodlogger.ui.LogFoodFragment;
import com.example.foodlogger.ui.LogMealFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.pager);
        TabLayout tabs = findViewById(R.id.tabs);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                Log.d(TAG, "createFragment pos=" + pos);
                return pos == 0 ? new LogMealFragment() : new LogFoodFragment();
            }
            @Override public int getItemCount(){ return 2; }
        });
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(pos==0 ? "Log Meal" : "Log Food")).attach();
    }
}
