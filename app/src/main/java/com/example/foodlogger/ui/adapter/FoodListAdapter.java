package com.example.foodlogger.ui.adapter;

import android.database.Cursor;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodlogger.R;

public class FoodListAdapter extends RecyclerView.Adapter<FoodListAdapter.VH> {
    private Cursor cursor;
    public void submitCursor(Cursor c){ this.cursor = c; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_food, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos){
        if(cursor==null || cursor.isClosed()) return;
        cursor.moveToPosition(pos);
        String name = cursor.getString(1);
        double cal=cursor.getDouble(2), carbs=cursor.getDouble(3), fat=cursor.getDouble(4), prot=cursor.getDouble(5);
        h.name.setText(name);
        h.macros.setText(String.format("Per 100g: %.0f kcal | C %.1f | F %.1f | P %.1f", cal, carbs, fat, prot));
    }

    @Override public int getItemCount(){ return cursor==null?0:cursor.getCount(); }

    static class VH extends RecyclerView.ViewHolder{
        TextView name, macros; VH(View v){ super(v); name=v.findViewById(R.id.tvName); macros=v.findViewById(R.id.tvMacros); }
    }
}
