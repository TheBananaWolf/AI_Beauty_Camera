package com.example.myapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.Utills.ToolType;

import java.util.ArrayList;
import java.util.List;

public class MainToolAdapter extends RecyclerView.Adapter<MainToolAdapter.ViewHolder> {
    List<ToolModel> toolList = new ArrayList<>();
    OnToolItemSelected toolItemSelected;

    public MainToolAdapter(OnToolItemSelected selected) {
        toolItemSelected = selected;
        toolList.add(new ToolModel("Lips Beauty", ToolType.LIPS_BEAUTY, R.drawable.ic_lips_outer));
        toolList.add(new ToolModel("Face Glow", ToolType.FACE_GLOW, R.drawable.ic_face));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_main_tool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mTextView.setText(toolList.get(position).mTitle);
        holder.mImageView.setImageResource(toolList.get(position).mIcon);
    }

    @Override
    public int getItemCount() {
        return toolList.size();
    }

    public interface OnToolItemSelected {
        void OnToolItemSelected(ToolType toolType);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final ImageView mImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.recyclerview_icon);
            mTextView = itemView.findViewById(R.id.recyclerview_text);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toolItemSelected.OnToolItemSelected(toolList.get(getAdapterPosition()).mToolType);
                }
            });
        }
    }

    public class ToolModel {
        private final String mTitle;
        private final ToolType mToolType;
        private final int mIcon;

        public ToolModel(String mTitle, ToolType mToolType, int mIcon) {
            this.mTitle = mTitle;
            this.mToolType = mToolType;
            this.mIcon = mIcon;
        }
    }
}
