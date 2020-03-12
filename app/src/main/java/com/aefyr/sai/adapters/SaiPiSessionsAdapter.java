package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.model.common.PackageMeta;
import com.bumptech.glide.Glide;

import java.util.List;

public class SaiPiSessionsAdapter extends RecyclerView.Adapter<SaiPiSessionsAdapter.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;

    private List<SaiPiSessionState> mSessions;

    private ActionDelegate mActionDelegate;

    public SaiPiSessionsAdapter(Context c) {
        mContext = c;
        mInflater = LayoutInflater.from(c);
        setHasStableIds(true);
    }

    public void setData(List<SaiPiSessionState> data) {
        mSessions = data;
        notifyDataSetChanged();
    }

    public void setActionsDelegate(ActionDelegate delegate) {
        mActionDelegate = delegate;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_installer_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindTo(mSessions.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mSessions.get(position).sessionId().hashCode();
    }

    @Override
    public int getItemCount() {
        return mSessions == null ? 0 : mSessions.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.recycle();
    }

    private void launchApp(String packageName) {
        if (mActionDelegate != null)
            mActionDelegate.launchApp(packageName);
    }

    private void showException(Exception e) {
        if (mActionDelegate != null)
            mActionDelegate.showError(e);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup mContainer;
        private TextView mName;
        private TextView mStatus;
        private ImageView mAppIcon;
        private ImageView mActionIcon;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            mContainer = itemView.findViewById(R.id.container_item_installer_session);
            mName = itemView.findViewById(R.id.tv_session_name);
            mStatus = itemView.findViewById(R.id.tv_session_status);
            mAppIcon = itemView.findViewById(R.id.iv_app_icon);
            mActionIcon = itemView.findViewById(R.id.iv_installed_app_action);

            mContainer.setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                SaiPiSessionState state = mSessions.get(adapterPosition);
                switch (state.status()) {
                    case INSTALLATION_SUCCEED:
                        launchApp(state.packageName());
                        break;
                    case INSTALLATION_FAILED:
                        showException(state.exception());
                        break;
                }
            });
        }

        private void bindTo(SaiPiSessionState state) {
            PackageMeta packageMeta = state.packageMeta();
            if (packageMeta != null) {
                mName.setText(packageMeta.label);
            } else if (state.appTempName() != null) {
                mName.setText(state.appTempName());
            } else {
                mName.setText(mContext.getString(R.string.installer_unknown_app));
            }

            if (packageMeta != null) {
                Glide.with(mAppIcon)
                        .load(packageMeta.iconUri != null ? packageMeta.iconUri : R.drawable.placeholder_app_icon)
                        .placeholder(R.drawable.placeholder_app_icon)
                        .into(mAppIcon);
            } else {
                Glide.with(mAppIcon)
                        .load(R.drawable.placeholder_app_icon)
                        .placeholder(R.drawable.placeholder_app_icon)
                        .into(mAppIcon);
            }

            mStatus.setText(state.status().getReadableName(mContext));

            switch (state.status()) {
                case INSTALLATION_SUCCEED:
                    mActionIcon.setImageResource(R.drawable.ic_launch);
                    mActionIcon.setVisibility(state.packageName() != null ? View.VISIBLE : View.GONE);
                    mContainer.setEnabled(state.packageName() != null);
                    break;
                case INSTALLATION_FAILED:
                    mActionIcon.setImageResource(R.drawable.ic_error);
                    mActionIcon.setVisibility(state.exception() != null ? View.VISIBLE : View.GONE);
                    mContainer.setEnabled(state.exception() != null);
                    break;
                default:
                    mActionIcon.setVisibility(View.GONE);
                    mContainer.setEnabled(false);
                    break;
            }
        }

        private void recycle() {
            Glide.with(mAppIcon).clear(mAppIcon);
        }
    }

    public interface ActionDelegate {

        void launchApp(String packageName);

        void showError(Exception exception);
    }
}
