package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.model.common.AppFeature;
import com.google.android.material.chip.Chip;

import java.util.List;

public class BackupAppFeatureAdapter extends RecyclerView.Adapter<BackupAppFeatureAdapter.ViewHolder> {

    private List<AppFeature> mFeatures;

    private LayoutInflater mInflater;

    public BackupAppFeatureAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }


    public void setFeatures(List<AppFeature> features) {
        mFeatures = features;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_backup_app_feature, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mFeatures.get(position));
    }

    @Override
    public int getItemCount() {
        return mFeatures == null ? 0 : mFeatures.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private Chip mChip;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setFocusable(false);

            mChip = (Chip) itemView;
        }

        void bind(AppFeature feature) {
            mChip.setText(feature.toText());
        }
    }

}
