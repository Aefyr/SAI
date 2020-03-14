package com.aefyr.sai.adapters;

import android.content.Context;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.billing.DonationStatus;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.google.android.material.card.MaterialCardView;

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

        private MaterialCardView mCard;
        private TextView mTitle;
        private TextView mDonationRequiredTv;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.requestFocus();

            mCard = itemView.findViewById(R.id.container_theme_wrapper);
            mTitle = itemView.findViewById(R.id.tv_theme_title);
            mDonationRequiredTv = itemView.findViewById(R.id.tv_theme_requires_donation);

            mCard.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (mListener != null)
                    mListener.onThemeClicked(mThemes.get(adapterPosition));
            });
        }

        private void bindTo(Theme.ThemeDescriptor theme) {
            mTitle.setText(theme.getName(mContext));

            Context themedContext = new ContextThemeWrapper(mContext, theme.getTheme());
            mCard.setCardBackgroundColor(Utils.getThemeColor(themedContext, R.attr.colorPrimary));

            int accentColor = Utils.getThemeColor(themedContext, R.attr.colorAccent);
            mCard.setStrokeColor(accentColor);
            mTitle.setTextColor(accentColor);

            if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
                mCard.setRippleColor(themedContext.getColorStateList(R.color.selector_theme_card_ripple));
            }

            if (theme.isDonationRequired() && !(mDonationStatus == DonationStatus.DONATED || mDonationStatus == DonationStatus.FLOSS_MODE)) {
                mDonationRequiredTv.setTextColor(Utils.getThemeColor(themedContext, android.R.attr.textColorPrimary));
                mDonationRequiredTv.setVisibility(View.VISIBLE);
            } else {
                mDonationRequiredTv.setVisibility(View.GONE);
            }

        }
    }

    public interface OnThemeInteractionListener {

        void onThemeClicked(Theme.ThemeDescriptor theme);

    }
}
