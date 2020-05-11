package com.aefyr.sai.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.SelectableAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.backup2.BackupAppDetails;
import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.model.common.PackageMeta;
import com.bumptech.glide.Glide;

public class BackupAppDetailsAdapter extends SelectableAdapter<String, BackupAppDetailsAdapter.BaseViewHolder> {
    private static final int VH_TYPE_HEADER = 0;
    private static final int VH_TYPE_BACKUP = 1;

    private LayoutInflater mInflater;

    private BackupAppDetails mDetails;

    private ActionDelegate mActionDelegate;

    public BackupAppDetailsAdapter(Context context, Selection<String> selection, LifecycleOwner lifecycleOwner, ActionDelegate actionDelegate) {
        super(selection, lifecycleOwner);
        mInflater = LayoutInflater.from(context);
        mActionDelegate = actionDelegate;
    }

    public void setDetails(@Nullable BackupAppDetails details) {
        mDetails = details;
        notifyDataSetChanged();
    }

    private BackupFileMeta getBackupForPosition(int position) {
        return mDetails.backups().get(position - 1);
    }

    @Override
    protected String getKeyForPosition(int position) {
        if (position == 0)
            return "BackupAppDetailsAdapter.Header";

        BackupFileMeta backup = getBackupForPosition(position);

        return backup.uri + "@" + backup.storageId;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VH_TYPE_HEADER;

        return VH_TYPE_BACKUP;
    }

    @NonNull
    @Override
    public BackupAppDetailsAdapter.BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VH_TYPE_HEADER:
                return new HeaderViewHolder(mInflater.inflate(R.layout.item_backup_app_details_header, parent, false));
            case VH_TYPE_BACKUP:
                return new BackupViewHolder(mInflater.inflate(R.layout.item_backup_app_details_backup, parent, false));
        }

        throw new IllegalArgumentException("Unknown viewType - " + viewType);
    }

    @Override
    public int getItemCount() {
        if (mDetails == null)
            return 0;

        return 1 + mDetails.backups().size();
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (position == 0)
            holder.bindTo(mDetails.app());
        else
            holder.bindTo(getBackupForPosition(position));
    }

    @Override
    public void onViewRecycled(@NonNull BaseViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    protected static abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        protected abstract void bindTo(T t);

        protected void recycle() {

        }
    }

    protected class HeaderViewHolder extends BaseViewHolder<BackupApp> {

        private ImageView mAppIcon;
        private TextView mAppTitle;
        private TextView mAppPackage;
        private TextView mAppVersion;

        private Button mBackupButton;
        private Button mDeleteButton;
        private Button mInstallButton;

        private BackupApp mBoundApp;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            mAppIcon = itemView.findViewById(R.id.iv_backup_app_details_header_app_icon);
            mAppTitle = itemView.findViewById(R.id.tv_backup_app_details_header_app_title);
            mAppPackage = itemView.findViewById(R.id.tv_backup_app_details_header_app_package);
            mAppVersion = itemView.findViewById(R.id.tv_backup_app_details_header_app_version);

            mBackupButton = itemView.findViewById(R.id.button_backup_app_details_backup);
            mDeleteButton = itemView.findViewById(R.id.button_backup_app_details_delete);
            mInstallButton = itemView.findViewById(R.id.button_backup_app_details_install);

            mBackupButton.setOnClickListener(v -> mActionDelegate.backupApp(mBoundApp));
            mDeleteButton.setOnClickListener(v -> mActionDelegate.deleteApp(mBoundApp));
            mInstallButton.setOnClickListener(v -> mActionDelegate.installApp(mBoundApp));
        }

        @Override
        protected void bindTo(BackupApp app) {
            mBoundApp = app;

            PackageMeta packageMeta = app.packageMeta();
            Glide.with(mAppIcon)
                    .load(packageMeta.iconUri != null ? packageMeta.iconUri : R.drawable.placeholder_app_icon)
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(mAppIcon);

            mAppTitle.setText(packageMeta.label != null ? packageMeta.label : packageMeta.packageName);
            if (!app.isInstalled()) {
                mAppTitle.setPaintFlags(mAppTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                mAppTitle.setPaintFlags(mAppTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            mAppVersion.setVisibility(packageMeta.versionName != null ? View.VISIBLE : View.GONE);
            mAppVersion.setText(packageMeta.versionName);
            mAppPackage.setText(packageMeta.packageName);

            boolean appInstalled = app.isInstalled();
            mBackupButton.setVisibility(appInstalled ? View.VISIBLE : View.GONE);
            mDeleteButton.setVisibility(appInstalled ? View.VISIBLE : View.GONE);
            mInstallButton.setVisibility(appInstalled ? View.GONE : View.VISIBLE);
        }

        @Override
        protected void recycle() {
            mBoundApp = null;

            Glide.with(mAppIcon)
                    .clear(mAppIcon);
        }
    }

    protected class BackupViewHolder extends BaseViewHolder<BackupFileMeta> {

        public BackupViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        protected void bindTo(BackupFileMeta meta) {

        }
    }

    public interface ActionDelegate {

        void backupApp(BackupApp backupApp);

        void deleteApp(BackupApp backupApp);

        void installApp(BackupApp backupApp);

    }

}
