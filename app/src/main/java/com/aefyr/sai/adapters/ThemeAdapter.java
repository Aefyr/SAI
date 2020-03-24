package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.billing.DonationStatus;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.view.ThemeView;

import java.util.List;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private List<Theme.ThemeDescriptor> mThemes;
    private DonationStatus mDonationStatus;

    private Context mContext;
    private LayoutInflater mInflater;

    private OnThemeInteractionListener mListener;

    public ThemeAdapter(Context c) {
        mContext = c;
        mInflater = LayoutInflater.from(c);
        setHasStableIds(true);
    }

    public void setThemes(List<Theme.ThemeDescriptor> themes) {
        mThemes = themes;
        notifyDataSetChanged();
    }

    public void setDonationStatus(DonationStatus donationStatus) {
        mDonationStatus = donationStatus;
        notifyDataSetChanged();
    }

    public void setOnThemeInteractionListener(OnThemeInteractionListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_theme, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindTo(mThemes.get(position));
    }

    @Override
    public int getItemCount() {
        return mThemes != null ? mThemes.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return mThemes.get(position).getTheme();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ThemeView mThemeView;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.requestFocus();

            mThemeView = itemView.findViewById(R.id.themeview_theme_item);

            mThemeView.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (mListener != null)
                    mListener.onThemeClicked(mThemes.get(adapterPosition));
            });
        }

        private void bindTo(Theme.ThemeDescriptor theme) {
            mThemeView.setTheme(theme);

            if (theme.isDonationRequired() && !mDonationStatus.unlocksThemes()) {
                mThemeView.setMessage(R.string.donate_donate_only_theme);
            } else {
                mThemeView.setMessage(null);
            }

        }
    }

    public interface OnThemeInteractionListener {

        void onThemeClicked(Theme.ThemeDescriptor theme);

    }
}
