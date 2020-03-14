package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.SelectableAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.model.backup.BackupNameFormatBuilder;
import com.google.android.material.chip.Chip;

import java.util.List;

public class BackupNameFormatBuilderPartsAdapter extends SelectableAdapter<BackupNameFormatBuilder.Part, BackupNameFormatBuilderPartsAdapter.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;

    private List<BackupNameFormatBuilder.Part> mParts;

    public BackupNameFormatBuilderPartsAdapter(Selection<BackupNameFormatBuilder.Part> selection, LifecycleOwner lifecycleOwner, Context c) {
        super(selection, lifecycleOwner);
        mContext = c;
        mInflater = LayoutInflater.from(c);
        setHasStableIds(true);
    }

    public void setData(List<BackupNameFormatBuilder.Part> data) {
        mParts = data;
        notifyDataSetChanged();
    }

    @Override
    protected BackupNameFormatBuilder.Part getKeyForPosition(int position) {
        return mParts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mParts.get(position).hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_name_format_builder_chip, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bindTo(mParts.get(position));
    }

    @Override
    public int getItemCount() {
        return mParts == null ? 0 : mParts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private boolean mPauseCheckedListener = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.requestFocus();

            ((Chip) itemView).setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mPauseCheckedListener)
                    return;

                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                setSelected(getKeyForPosition(adapterPosition), isChecked);
            });
        }

        void bindTo(BackupNameFormatBuilder.Part part) {
            ((Chip) itemView).setText(part.getDisplayName(mContext));

            mPauseCheckedListener = true;
            ((Chip) itemView).setChecked(isSelected(part));
            mPauseCheckedListener = false;
        }
    }

}
