package com.example.myapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Utills.ListForRecyclerViewForImage;
import com.example.myapplication.R;

import java.util.List;

public class AdapterForRecycle extends RecyclerView.Adapter<AdapterForRecycle.ViewHolder> {
    private List<ListForRecyclerViewForImage> mFruitlis;
    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView fruitImage;
        TextView fruitName;
        public ViewHolder (View view){
            super(view);
            fruitImage=view.findViewById(R.id.fruit_image);
            fruitName=view.findViewById(R.id.fruit_name);
        }
    }
    public AdapterForRecycle(List<ListForRecyclerViewForImage> listForRecyclerViewForImageList){
         mFruitlis= listForRecyclerViewForImageList;
    }

    @Override
    public AdapterForRecycle.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.fruit,parent,false);
        ViewHolder holder=new ViewHolder(view);
        holder.fruitImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position=holder.getAdapterPosition();
                ListForRecyclerViewForImage listForRecyclerViewForImage =mFruitlis.get(position);
                Toast.makeText(v.getContext(),String.valueOf(listForRecyclerViewForImage.getName()),Toast.LENGTH_SHORT).show();
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterForRecycle.ViewHolder holder, int position) {
        ListForRecyclerViewForImage listForRecyclerViewForImage =mFruitlis.get(position);
        holder.fruitImage.setImageResource(listForRecyclerViewForImage.getImageID());
        holder.fruitName.setText(listForRecyclerViewForImage.getName());

    }




    @Override
    public int getItemCount() {
        return mFruitlis.size();
    }
}
