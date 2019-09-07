package com.aefyr.sai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.model.licenses.License;

import java.util.List;

public class LicensesAdapter extends RecyclerView.Adapter<LicensesAdapter.ViewHolder> {

    private LayoutInflater mInflater;

    private List<License> mLicenses;

    public LicensesAdapter(Context c) {
        mInflater = LayoutInflater.from(c);
    }

    public void setLicenses(List<License> licenses) {
        mLicenses = licenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_license, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mLicenses.get(position));
    }

    @Override
    public int getItemCount() {
        return mLicenses == null ? 0 : mLicenses.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mSubject;
        private TextView mText;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            mSubject = itemView.findViewById(R.id.tv_subject);
            mText = itemView.findViewById(R.id.tv_text);
        }

        private void bind(License license) {
            mSubject.setText(license.subject);
            mText.setText(license.text);
        }
    }

}
