package com.aefyr.sai.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.SelectableAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.backup2.BackupStatus;
import com.aefyr.sai.model.backup.BackupPackagesFilterConfig;
import com.aefyr.sai.model.backup.SimpleAppFeature;
import com.aefyr.sai.model.common.AppFeature;
import com.aefyr.sai.model.common.PackageMeta;
import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BackupPackagesAdapter extends SelectableAdapter<String, BackupPackagesAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private List<BackupApp> mApps;

    private OnItemInteractionListener mListener;

    private BackupPackagesFilterConfig mFilterConfig;

    private RecyclerView.RecycledViewPool mFeatureViewPool;

    private SimpleDateFormat mInstallOrUpdateDateSdf = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());


    public BackupPackagesAdapter(Selection<String> selection, LifecycleOwner lifecycleOwner, Context c) {
        super(selection, lifecycleOwner);

        mInflater = LayoutInflater.from(c);
        setHasStableIds(true);

        mFeatureViewPool = new RecyclerView.RecycledViewPool();
        mFeatureViewPool.setMaxRecycledViews(0, 16);
    }

    public void setData(List<BackupApp> packages) {
        mApps = packages;
        notifyDataSetChanged();
    }

    public void setFilterConfig(@Nullable BackupPackagesFilterConfig config, boolean applyNow) {
        mFilterConfig = config;

        if (applyNow)
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
    protected String getKeyForPosition(int position) {
        return mApps.get(position).packageMeta().packageName;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        BackupApp backupApp = mApps.get(position);
        holder.bindTo(backupApp);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    @Override
    public int getItemCount() {
        return mApps == null ? 0 : mApps.size();
    }

    @Override
    public long getItemId(int position) {
        return mApps.get(position).packageMeta().packageName.hashCode();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mAppName;
        private TextView mAppVersion;
        private TextView mAppPackage;
        private AppCompatImageView mAppIcon;
        private View mSelectionOverlay;
        private ImageView mBackupStatus;

        private BackupAppFeatureAdapter mFeatureAdapter;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            mAppName = itemView.findViewById(R.id.tv_app_name);
            mAppVersion = itemView.findViewById(R.id.tv_app_version);
            mAppPackage = itemView.findViewById(R.id.tv_app_package);
            mAppIcon = itemView.findViewById(R.id.iv_app_icon);
            mSelectionOverlay = itemView.findViewById(R.id.overlay_backup_package_selection);
            mBackupStatus = itemView.findViewById(R.id.iv_backup_status);

            itemView.findViewById(R.id.container_backup_package).setOnFocusChangeListener((v, hasFocus) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (mListener != null)
                    mListener.onItemFocusChanged(hasFocus, adapterPosition, mApps.get(adapterPosition));
            });

            itemView.findViewById(R.id.container_backup_package).setOnLongClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return false;

                BackupApp item = mApps.get(adapterPosition);
                boolean selected = switchSelection(item.packageMeta().packageName);
                mSelectionOverlay.setVisibility(selected ? View.VISIBLE : View.GONE);

                return true;
            });

            itemView.findViewById(R.id.container_backup_package).setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (!getSelection().hasSelection()) {
                    if (mListener != null)
                        mListener.onBackupButtonClicked(mApps.get(adapterPosition));
                } else {
                    BackupApp item = mApps.get(adapterPosition);
                    boolean selected = switchSelection(item.packageMeta().packageName);
                    mSelectionOverlay.setVisibility(selected ? View.VISIBLE : View.GONE);
                }
            });

            RecyclerView featureRecycler = itemView.findViewById(R.id.rv_backup_app_features);
            FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(itemView.getContext(), FlexDirection.ROW, FlexWrap.WRAP);
            layoutManager.setJustifyContent(JustifyContent.FLEX_START);
            featureRecycler.setLayoutManager(layoutManager);

            featureRecycler.setRecycledViewPool(mFeatureViewPool);

            mFeatureAdapter = new BackupAppFeatureAdapter(itemView.getContext());
            featureRecycler.setAdapter(mFeatureAdapter);
            featureRecycler.setFocusable(false);
        }

        @SuppressLint("DefaultLocale")
        void bindTo(BackupApp app) {
            PackageMeta packageMeta = app.packageMeta();

            mAppName.setText(packageMeta.label);
            if (!app.isInstalled()) {
                mAppName.setPaintFlags(mAppName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                mAppName.setPaintFlags(mAppName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            if (app.backupStatus() == BackupStatus.NO_BACKUP) {
                mBackupStatus.setVisibility(View.GONE);
            } else {
                mBackupStatus.setImageResource(app.backupStatus().getIconRes());
                mBackupStatus.setVisibility(View.VISIBLE);
            }

            mAppVersion.setText(String.format("%s (%d)", packageMeta.versionName, packageMeta.versionCode));
            mAppPackage.setText(packageMeta.packageName);

            Glide.with(mAppIcon)
                    .load(packageMeta.iconUri != null ? packageMeta.iconUri : R.drawable.placeholder_app_icon)
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(mAppIcon);

            mFeatureAdapter.setFeatures(createContextualFeatures(packageMeta));

            mSelectionOverlay.setVisibility(isSelected(packageMeta.packageName) ? View.VISIBLE : View.GONE);
        }

        private List<AppFeature> createContextualFeatures(PackageMeta packageMeta) {
            if (mFilterConfig == null)
                return Collections.emptyList();

            ArrayList<AppFeature> features = new ArrayList<>();

            Resources res = itemView.getResources();

            switch (mFilterConfig.getSort()) {
                case NAME:
                    break;
                case INSTALL_TIME:
                    features.add(new SimpleAppFeature(res.getString(R.string.backup_app_feature_install_date, mInstallOrUpdateDateSdf.format(packageMeta.installTime))));
                    break;
                case UPDATE_TIME:
                    features.add(new SimpleAppFeature(res.getString(R.string.backup_app_feature_update_date, mInstallOrUpdateDateSdf.format(packageMeta.updateTime))));
                    break;
            }

            if (mFilterConfig.getSplitApkFilter() == BackupPackagesFilterConfig.SimpleFilterMode.WHATEVER && packageMeta.hasSplits)
                features.add(new SimpleAppFeature(res.getString(R.string.backup_app_feature_split)));

            if (mFilterConfig.getSystemAppFilter() == BackupPackagesFilterConfig.SimpleFilterMode.WHATEVER && packageMeta.isSystemApp)
                features.add(new SimpleAppFeature(res.getString(R.string.backup_app_feature_system_app)));

            return features;
        }

        void recycle() {
            Glide.with(mAppIcon)
                    .clear(mAppIcon);

            mFeatureAdapter.setFeatures(null);
        }
    }

    public interface OnItemInteractionListener {
        void onBackupButtonClicked(BackupApp backupApp);

        void onItemFocusChanged(boolean hasFocus, int index, BackupApp backupApp);
    }

}
