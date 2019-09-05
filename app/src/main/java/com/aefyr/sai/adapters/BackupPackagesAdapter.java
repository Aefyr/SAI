package com.aefyr.sai.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.model.backup.PackageMeta;
import com.bumptech.glide.Glide;

import java.util.List;

public class BackupPackagesAdapter extends RecyclerView.Adapter<BackupPackagesAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private List<PackageMeta> mPackages;

    private OnItemInteractionListener mListener;


    public BackupPackagesAdapter(Context c) {
        mInflater = LayoutInflater.from(c);
        setHasStableIds(true);
    }

    public void setData(List<PackageMeta> packages) {
        mPackages = packages;
        notifyDataSetChanged();
    }

    public void setInteractionListener(OnItemInteractionListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_backup_package, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PackageMeta packageMeta = mPackages.get(position);
        holder.bindTo(packageMeta);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.recycle();
    }

    @Override
    public int getItemCount() {
        return mPackages == null ? 0 : mPackages.size();
    }

    @Override
    public long getItemId(int position) {
        return mPackages.get(position).packageName.hashCode();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mAppName;
        private TextView mAppVersion;
        private TextView mAppPackage;
        private AppCompatImageView mAppIcon;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            mAppName = itemView.findViewById(R.id.tv_app_name);
            mAppVersion = itemView.findViewById(R.id.tv_app_version);
            mAppPackage = itemView.findViewById(R.id.tv_app_package);
            mAppIcon = itemView.findViewById(R.id.iv_app_icon);

            itemView.findViewById(R.id.ib_backup).setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (mListener != null)
                    mListener.onBackupButtonClicked(mPackages.get(adapterPosition));
            });
        }

        @SuppressLint("DefaultLocale")
        void bindTo(PackageMeta packageMeta) {
            mAppName.setText(packageMeta.label);
            mAppVersion.setText(String.format("%s (%d)", packageMeta.versionName, packageMeta.versionCode));
            mAppPackage.setText(packageMeta.packageName);
            
            Glide.with(mAppIcon)
                    .load(packageMeta.iconUri != null ? packageMeta.iconUri : R.drawable.placeholder_app_icon)
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(mAppIcon);
        }

        void recycle() {
            Glide.with(mAppIcon)
                    .clear(mAppIcon);
        }
    }

    public interface OnItemInteractionListener {
        void onBackupButtonClicked(PackageMeta packageMeta);
    }

}
