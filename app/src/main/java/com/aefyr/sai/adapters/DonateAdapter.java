package com.aefyr.sai.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.billing.BillingProduct;
import com.aefyr.sai.billing.DonationStatus;
import com.aefyr.sai.billing.DonationStatusRenderer;
import com.bumptech.glide.Glide;

import java.util.List;

public class DonateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VH_TYPE_HEADER = 0;
    private static final int VH_TYPE_PRODUCT = 1;

    private LayoutInflater mInflater;

    private DonationStatus mDonationStatus;
    private DonationStatusRenderer mDonationStatusRenderer;
    private List<BillingProduct> mProducts;

    private OnProductInteractionListener mProductInteractionListener;

    public DonateAdapter(Context context, DonationStatusRenderer donationStatusRenderer) {
        mInflater = LayoutInflater.from(context);
        mDonationStatusRenderer = donationStatusRenderer;
        setHasStableIds(true);
    }

    public void setDonationStatus(DonationStatus donationStatus) {
        mDonationStatus = donationStatus;
        notifyDataSetChanged();
    }

    public void setProducts(List<BillingProduct> products) {
        mProducts = products;
        notifyDataSetChanged();
    }

    public void setOnProductInteractionListener(OnProductInteractionListener listener) {
        mProductInteractionListener = listener;
    }

    private BillingProduct getBillingProductAt(int adapterPosition) {
        return mProducts.get(mDonationStatus == null ? adapterPosition : (adapterPosition - 1));
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VH_TYPE_HEADER : VH_TYPE_PRODUCT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VH_TYPE_HEADER)
            return new HeaderViewHolder(mInflater.inflate(R.layout.item_donate_header, parent, false));

        if (viewType == VH_TYPE_PRODUCT)
            return new ProductViewHolder(mInflater.inflate(R.layout.item_donate_product, parent, false));

        throw new IllegalArgumentException("Unknown view type");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bindTo(mDonationStatus);
            return;
        }

        if (holder instanceof ProductViewHolder) {
            ((ProductViewHolder) holder).bindTo(getBillingProductAt(position));
            return;
        }

        throw new IllegalArgumentException("Unknown ViewHolder class - " + holder.getClass().getCanonicalName());
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ProductViewHolder) {
            ((ProductViewHolder) holder).recycle();
            return;
        }
    }

    @Override
    public int getItemCount() {
        return (mDonationStatus != null ? 1 : 0) + (mProducts != null ? mProducts.size() : 0);
    }

    @Override
    public long getItemId(int position) {
        if (position == 0 && mDonationStatus != null)
            return 0;

        return getBillingProductAt(position).getId().hashCode();
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView mMessage;
        private AppCompatImageView mIcon;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            mMessage = itemView.findViewById(R.id.tv_donate_header);
            mIcon = itemView.findViewById(R.id.iv_donate_header);
        }

        private void bindTo(DonationStatus status) {
            mMessage.setText(mDonationStatusRenderer.getText(itemView.getContext(), status));
            mIcon.setImageDrawable(mDonationStatusRenderer.getIcon(itemView.getContext(), status));
        }
    }

    public interface OnProductInteractionListener {
        void onProductClicked(BillingProduct product);
    }

    private class ProductViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mDesc;
        private TextView mPrice;

        private ImageView mIcon;

        private ViewGroup mContainer;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            mTitle = itemView.findViewById(R.id.tv_donate_product_title);
            mDesc = itemView.findViewById(R.id.tv_donate_product_desc);
            mPrice = itemView.findViewById(R.id.tv_donate_product_price);

            mIcon = itemView.findViewById(R.id.iv_donate_product_icon);

            mContainer = itemView.findViewById(R.id.container_donate_product);

            mContainer.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION)
                    return;

                if (mProductInteractionListener != null)
                    mProductInteractionListener.onProductClicked(getBillingProductAt(adapterPosition));
            });
        }

        private void bindTo(BillingProduct product) {
            mTitle.setText(product.getTitle());
            mDesc.setText(product.getDescription());
            mPrice.setText(product.getPrice());

            Drawable placeholder = itemView.getContext().getDrawable(R.drawable.placeholder_donate_product);
            Glide.with(mIcon)
                    .load(!TextUtils.isEmpty(product.getIconUrl()) ? product.getIconUrl() : placeholder)
                    .placeholder(placeholder)
                    .into(mIcon);
        }

        private void recycle() {
            Glide.with(mIcon).clear(mIcon);
        }
    }

}
