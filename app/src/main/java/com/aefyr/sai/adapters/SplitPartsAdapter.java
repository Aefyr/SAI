package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.SelectableAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.model.installerx.SplitPart;

import java.util.List;

public class SplitPartsAdapter extends SelectableAdapter<String, SplitPartsAdapter.SplitPartViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;

    private List<SplitPart> mParts;

    public SplitPartsAdapter(Selection<String> selection, LifecycleOwner lifecycleOwner, Context context) {
        super(selection, lifecycleOwner);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        setHasStableIds(true);
    }

    public void setParts(List<SplitPart> parts) {
        mParts = parts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SplitPartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SplitPartViewHolder(mInflater.inflate(R.layout.item_installerx_split_part, parent, false));
    }

    @Override
    protected String getKeyForPosition(int position) {
        return mParts.get(position).id();
    }

    @Override
    public void onBindViewHolder(@NonNull SplitPartViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bindTo(this, mParts.get(position));
    }

    @Override
    public int getItemCount() {
        return mParts == null ? 0 : mParts.size();
    }

    @Override
    public long getItemId(int position) {
        return mParts.get(position).id().hashCode();
    }

    static class SplitPartViewHolder extends RecyclerView.ViewHolder {

        private SplitPartsAdapter mHost;

        private TextView mName;
        private TextView mDescription;
        private TextView mPath;

        private CheckBox mCheck;

        private SplitPartViewHolder(@NonNull View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.tv_split_part_name);
            mDescription = itemView.findViewById(R.id.tv_split_part_description);
            mPath = itemView.findViewById(R.id.tv_split_part_path);

            mCheck = itemView.findViewById(R.id.check_split_apk_part);

            itemView.setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                SplitPart item = mHost.mParts.get(adapterPosition);
                if (item.isRequired())
                    return;

                boolean selected = mHost.switchSelection(item.id());
                mCheck.setChecked(selected);
            });
        }

        private void bindTo(SplitPartsAdapter host, SplitPart part) {
            mHost = host;

            mName.setText(part.name());

            if (part.description() != null) {
                mDescription.setText(part.description());
            } else {
                mDescription.setText(null);
                mDescription.setVisibility(View.GONE);
            }


            mCheck.setChecked(part.isRequired() || mHost.isSelected(part.id()));

            mCheck.setEnabled(!part.isRequired());
            itemView.setEnabled(!part.isRequired());
        }
    }
}
