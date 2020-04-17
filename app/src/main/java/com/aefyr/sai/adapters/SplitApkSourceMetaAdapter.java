package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.SelectableAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.SplitCategory;
import com.aefyr.sai.installerx.SplitPart;
import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.meta.Notice;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SplitApkSourceMetaAdapter extends SelectableAdapter<String, SplitApkSourceMetaAdapter.BaseViewHolder> {

    public static final int VH_TYPE_HEADER = 0;
    public static final int VH_TYPE_NOTICE = 1;
    public static final int VH_TYPE_CATEGORY = 2;
    public static final int VH_TYPE_SPLIT_PART = 3;

    private Context mContext;
    private LayoutInflater mInflater;

    private SplitApkSourceMeta mMeta;
    private List<Object> mFlattenedData;

    public SplitApkSourceMetaAdapter(Selection<String> partsSelection, LifecycleOwner lifecycleOwner, Context context) {
        super(partsSelection, lifecycleOwner);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public void setMeta(SplitApkSourceMeta meta) {
        mMeta = meta;
        mFlattenedData = new ArrayList<>();

        mFlattenedData.addAll(meta.notices());

        for (SplitCategory category : meta.splits()) {
            mFlattenedData.add(category);
            mFlattenedData.addAll(category.parts());
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VH_TYPE_HEADER;

        Object object = getItemForAdapterPosition(position);

        if (object instanceof Notice)
            return VH_TYPE_NOTICE;

        if (object instanceof SplitCategory)
            return VH_TYPE_CATEGORY;
        else if (object instanceof SplitPart)
            return VH_TYPE_SPLIT_PART;

        throw new IllegalStateException("Unexpected object class in data - " + object.getClass().getCanonicalName());
    }

    @NonNull
    @Override
    public SplitApkSourceMetaAdapter.BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VH_TYPE_HEADER:
                return new HeaderViewHolder(mInflater.inflate(R.layout.item_installerx_header, parent, false));
            case VH_TYPE_NOTICE:
                return new NoticeViewHolder(mInflater.inflate(R.layout.item_installerx_notice, parent, false));
            case VH_TYPE_CATEGORY:
                return new SplitCategoryViewHolder(mInflater.inflate(R.layout.item_installerx_split_category, parent, false));
            case VH_TYPE_SPLIT_PART:
                return new SplitPartViewHolder(mInflater.inflate(R.layout.item_installerx_split_part, parent, false));
        }

        throw new IllegalArgumentException("Unknown viewType " + viewType);
    }

    @Override
    protected String getKeyForPosition(int position) {
        if (position == 0)
            return "SplitApkSourceMetaAdapter.header";

        Object object = getItemForAdapterPosition(position);

        if (object instanceof Notice)
            return "SplitApkSourceMetaAdapter.notice." + object.hashCode();

        if (object instanceof SplitCategory)
            return ((SplitCategory) object).id();

        if (object instanceof SplitPart)
            return ((SplitPart) object).localPath();

        throw new IllegalStateException("Unexpected object class in data - " + object.getClass().getCanonicalName());
    }

    @Override
    public void onBindViewHolder(@NonNull SplitApkSourceMetaAdapter.BaseViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder instanceof HeaderViewHolder) {
            holder.bindTo(mMeta);
            return;
        }

        if (holder instanceof NoticeViewHolder) {
            holder.bindTo(getItemForAdapterPosition(position));
            return;
        }

        if (holder instanceof SplitCategoryViewHolder) {
            holder.bindTo(getItemForAdapterPosition(position));
            return;
        }

        if (holder instanceof SplitPartViewHolder) {
            holder.bindTo(getItemForAdapterPosition(position));
            return;
        }

        throw new IllegalArgumentException("Unknown ViewHolder class - " + holder.getClass().getCanonicalName());
    }

    @Override
    public int getItemCount() {
        return mFlattenedData == null ? 0 : 1 + mFlattenedData.size();
    }

    @Override
    public void onViewRecycled(@NonNull BaseViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    private <T> T getItemForAdapterPosition(int adapterPosition) {
        return (T) mFlattenedData.get(adapterPosition - 1);
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

            itemView.requestFocus(); //TV fix
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

    protected class NoticeViewHolder extends BaseViewHolder<Notice> {

        private TextView mNoticeText;

        private NoticeViewHolder(@NonNull View itemView) {
            super(itemView);

            mNoticeText = itemView.findViewById(R.id.tv_installerx_notice);
        }

        @Override
        void bindTo(Notice notice) {
            mNoticeText.setText(notice.text());
        }

        @Override
        void recycle() {

        }
    }

    protected class SplitCategoryViewHolder extends BaseViewHolder<SplitCategory> {

        private TextView mTitle;
        private TextView mDesc;

        private SplitCategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitle = itemView.findViewById(R.id.tv_installerx_split_category_title);
            mDesc = itemView.findViewById(R.id.tv_installerx_split_category_desc);
        }

        @Override
        void bindTo(SplitCategory category) {
            mTitle.setText(category.name());

            mDesc.setVisibility(category.description() != null ? View.VISIBLE : View.GONE);
            mDesc.setText(category.description());
        }

        @Override
        void recycle() {

        }
    }

    protected class SplitPartViewHolder extends BaseViewHolder<SplitPart> {

        private TextView mName;
        private TextView mDescription;

        private CheckBox mCheck;

        private SplitPartViewHolder(@NonNull View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.tv_split_part_name);
            mDescription = itemView.findViewById(R.id.tv_split_part_description);

            mCheck = itemView.findViewById(R.id.check_split_apk_part);

            itemView.setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                SplitPart item = getItemForAdapterPosition(adapterPosition);
                if (item.isRequired())
                    return;

                boolean selected = switchSelection(item.localPath());
                mCheck.setChecked(selected);
            });
        }

        @Override
        void bindTo(SplitPart part) {
            mName.setText(part.name());

            if (part.description() != null) {
                mDescription.setText(part.description());
            } else {
                mDescription.setText(null);
                mDescription.setVisibility(View.GONE);
            }


            mCheck.setChecked(part.isRequired() || isSelected(part.localPath()));

            mCheck.setEnabled(!part.isRequired());
            itemView.setEnabled(!part.isRequired());
        }

        @Override
        void recycle() {

        }
    }
}
