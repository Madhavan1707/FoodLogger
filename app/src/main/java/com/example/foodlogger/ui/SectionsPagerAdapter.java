package com.example.foodlogger.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SectionsPagerAdapter extends FragmentStateAdapter {

    public SectionsPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new LogMealFragment();
            case 1: return new LogFoodFragment();
            case 2: return new GoalsFragment();
            default: return new LogMealFragment();
        }
    }

    @Override
    public int getItemCount() { return 3; }
}
