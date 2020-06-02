package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.BackupComponent;
import com.aefyr.sai.backup2.impl.components.StandardComponentTypes;
import com.aefyr.sai.utils.Utils;
import com.google.android.material.chip.Chip;

import java.util.List;

public class BackupComponentsAdapter extends RecyclerView.Adapter<BackupComponentsAdapter.ViewHolder> {

    private List<BackupComponent> mComponents;

    private LayoutInflater mInflater;
    private ComponentRenderer mComponentRenderer;

    public BackupComponentsAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mComponentRenderer = new DefaultComponentRenderer(context);
    }


    public void setComponents(List<BackupComponent> components) {
        mComponents = components;
        notifyDataSetChanged();
    }

    public void setComponentRenderer(ComponentRenderer renderer) {
        mComponentRenderer = renderer;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_backup_component, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mComponents.get(position));
    }

    @Override
    public int getItemCount() {
        return mComponents == null ? 0 : mComponents.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private Chip mChip;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setFocusable(false);

            mChip = (Chip) itemView;
        }

        void bind(BackupComponent component) {
            mChip.setText(mComponentRenderer.render(component));
        }
    }

    interface ComponentRenderer {

        String render(BackupComponent component);

    }

    public static class DefaultComponentRenderer implements ComponentRenderer {

        private Context mContext;

        public DefaultComponentRenderer(Context context) {
            mContext = context;
        }


        @Override
        public String render(BackupComponent component) {
            switch (component.type()) {
                case StandardComponentTypes.TYPE_APK_FILES:
                    return formatWithSize(R.string.backup_component_apk_files, component.size());
            }

            return formatWithSize(mContext.getString(R.string.backup_component_unknown, component.type()), component.size());
        }

        private String formatWithSize(@StringRes int stringId, long size) {
            return formatWithSize(mContext.getString(stringId), size);
        }

        private String formatWithSize(String s, long size) {
            return s + " (" + Utils.formatSize(mContext, size) + ")";
        }


    }
}
