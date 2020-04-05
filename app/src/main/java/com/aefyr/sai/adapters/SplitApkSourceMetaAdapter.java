package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.SplitCategory;
import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.bumptech.glide.Glide;

public class SplitApkSourceMetaAdapter extends RecyclerView.Adapter<SplitApkSourceMetaAdapter.BaseViewHolder> {

    private static final int VH_TYPE_HEADER = 0;
    private static final int VH_TYPE_CATEGORY = 1;

    private Context mContext;
    private LayoutInflater mInflater;

    private SplitApkSourceMeta mMeta;

    private Selection<String> mPartsSelection;
    private LifecycleOwner mLifecycleOwner;

    private RecyclerView.RecycledViewPool mSplitPartsViewPool;

    public SplitApkSourceMetaAdapter(Selection<String> partsSelection, LifecycleOwner lifecycleOwner, Context context) {
        mPartsSelection = partsSelection;
        mLifecycleOwner = lifecycleOwner;
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        mSplitPartsViewPool = new RecyclerView.RecycledViewPool();
        mSplitPartsViewPool.setMaxRecycledViews(0, 16);

        setHasStableIds(true);
    }

    public void setMeta(SplitApkSourceMeta meta) {
        mMeta = meta;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VH_TYPE_HEADER;

        return VH_TYPE_CATEGORY;
    }

    @NonNull
    @Override
    public SplitApkSourceMetaAdapter.BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VH_TYPE_HEADER:
                return new HeaderViewHolder(mInflater.inflate(R.layout.item_installerx_header, parent, false));
            case VH_TYPE_CATEGORY:
                return new SplitCategoryViewHolder(mInflater.inflate(R.layout.item_installerx_split_category, parent, false));
        }

        throw new IllegalArgumentException("Unknown viewType " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull SplitApkSourceMetaAdapter.BaseViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            holder.bindTo(mMeta);
            return;
        }

        if (holder instanceof SplitCategoryViewHolder) {
            holder.bindTo(getCategoryForAdapterPosition(position));
            return;
        }

        throw new IllegalArgumentException("Unknown ViewHolder class - " + holder.getClass().getCanonicalName());
    }

    @Override
    public int getItemCount() {
        return mMeta == null ? 0 : 1 + mMeta.splits().size();
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? Integer.MIN_VALUE : getCategoryForAdapterPosition(position).id().hashCode();
    }

    @Override
    public void onViewRecycled(@NonNull BaseViewHolder holder) {
        holder.recycle();
    }

    private SplitCategory getCategoryForAdapterPosition(int adapterPosition) {
        return mMeta.splits().get(adapterPosition - 1);
    }

    protected abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bindTo(T t);

        abstract void recycle();
    }

    protected class HeaderViewHolder extends BaseViewHolder<SplitApkSourceMeta> {

        private ImageView mAppIcon;
        private TextView mAppTitle;
        private TextView mAppVersion;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            mAppIcon = itemView.findViewById(R.id.iv_installerx_header_app_icon);
            mAppTitle = itemView.findViewById(R.id.tv_installerx_header_app_title);
            mAppVersion = itemView.findViewById(R.id.tv_installerx_header_app_version);
        }

        @Override
        void bindTo(SplitApkSourceMeta meta) {
            AppMeta appMeta = meta.appMeta();
            if (appMeta == null) {
                //TODO fill with some unknown data
                return;
            }

            Glide.with(mAppIcon)
                    .load(appMeta.iconUri != null ? appMeta.iconUri : R.drawable.placeholder_app_icon)
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(mAppIcon);

            mAppTitle.setText(appMeta.appName != null ? appMeta.appName : appMeta.packageName);

            mAppVersion.setVisibility(appMeta.versionName != null ? View.VISIBLE : View.GONE);
            mAppVersion.setText(appMeta.versionName);
        }

        @Override
        void recycle() {
            Glide.with(mAppIcon)
                    .clear(mAppIcon);
        }
    }

    protected class SplitCategoryViewHolder extends BaseViewHolder<SplitCategory> {

        private TextView mTitle;
        private TextView mDesc;
        private SplitPartsAdapter mPartsAdapter;

        public SplitCategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitle = itemView.findViewById(R.id.tv_installerx_split_category_title);
            mDesc = itemView.findViewById(R.id.tv_installerx_split_category_desc);

            RecyclerView partsRecycler = itemView.findViewById(R.id.rv_installerx_split_category_parts);
            partsRecycler.setLayoutManager(new LinearLayoutManager(mContext));
            partsRecycler.setRecycledViewPool(mSplitPartsViewPool);

            mPartsAdapter = new SplitPartsAdapter(mPartsSelection, mLifecycleOwner, mContext);
            partsRecycler.setAdapter(mPartsAdapter);
        }

        @Override
        void bindTo(SplitCategory category) {
            mTitle.setText(category.name());

            mDesc.setVisibility(category.description() != null ? View.VISIBLE : View.GONE);
            mDesc.setText(category.description());

            mPartsAdapter.setParts(category.parts());
        }

        @Override
        void recycle() {
            mPartsAdapter.setParts(null);
        }
    }
}
