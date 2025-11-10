package com.example.foodlogger;
import android.content.Intent;
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
import com.google.android.material.tabs.TabLayoutMediator;  import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
        implements com.example.foodlogger.ui.GoalsFragment.OnGoalsSavedListener {
    private static final String TAG = "MainActivity";
    private static final String EXTRA_OPEN_TAB = "open_tab";
    private static final int REQ_POST_NOTIF = 42;
    private static final int LOG_MEAL_INDEX = 0; // <-- adjust if your Log Meal tab index is different


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                        REQ_POST_NOTIF
                );
            }
        }


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

        // Choose initial tab: if goals not set, open Goals (index 2); else Log Meal (index 0)
        int initialIndex = com.example.foodlogger.util.GoalPrefs.isSetupComplete(this) ? 0 : 2;
        pager.setCurrentItem(initialIndex, false);

// Finally, allow an incoming intent (e.g., from notification) to override the tab
        handleOpenTabIntent(getIntent());

    }

    @Override
    public void onGoalsSaved() {
        ViewPager2 pager = findViewById(R.id.pager);
        if (pager != null) {
            // Jump to Log Meal after a successful first-time setup (or any save)
            pager.setCurrentItem(0, true); // adjust index if your order differs
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIF && Build.VERSION.SDK_INT >= 33) {
                   boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                 if (!granted) {
                                  android.widget.Toast.makeText(this, "Enable notifications to get daily reminders", android.widget.Toast.LENGTH_SHORT).show();
                              }

              }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenTabIntent(intent);
    }
    private void handleOpenTabIntent(Intent intent) {
        Log.d("ma","handle");
        if (intent == null) return;
        String open = intent.getStringExtra(EXTRA_OPEN_TAB);
        if ("log_meal".equals(open)) {
            // Example for ViewPager2:
            androidx.viewpager2.widget.ViewPager2 vp = findViewById(R.id.pager);
            if (vp != null) vp.setCurrentItem(LOG_MEAL_INDEX, true);
            // If you use a different nav system, navigate to Log Meal accordingly.
        }
    }

}
