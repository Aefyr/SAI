package com.aefyr.sai.ui.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.ThemeAdapter;
import com.aefyr.sai.billing.BillingManager;
import com.aefyr.sai.billing.DefaultBillingManager;
import com.aefyr.sai.billing.DonationStatus;
import com.aefyr.sai.ui.activities.DonateActivity;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.Theme;

public class ThemeSelectionDialogFragment extends BaseBottomSheetDialogFragment implements ThemeAdapter.OnThemeInteractionListener {

    private BillingManager mBillingManager;

    public static ThemeSelectionDialogFragment newInstance(Context context) {
        ThemeSelectionDialogFragment fragment = new ThemeSelectionDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBillingManager = DefaultBillingManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        return recyclerView;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);

        setTitle(R.string.installer_select_theme);
        getPositiveButton().setVisibility(View.GONE);
        getNegativeButton().setOnClickListener(v -> dismiss());

        RecyclerView recycler = (RecyclerView) view;
        recycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        ThemeAdapter adapter = new ThemeAdapter(requireContext());
        adapter.setThemes(Theme.getInstance(requireContext()).getThemes());
        adapter.setOnThemeInteractionListener(this);
        mBillingManager.getDonationStatus().observe(this, adapter::setDonationStatus);
        recycler.setAdapter(adapter);

        revealBottomSheet();
    }

    @Override
    public void onThemeClicked(Theme.ThemeDescriptor theme) {
        DonationStatus donationStatus = mBillingManager.getDonationStatus().getValue();
        if (theme.isDonationRequired() && !(donationStatus == DonationStatus.DONATED || donationStatus == DonationStatus.FLOSS_MODE)) {
            DonateActivity.start(requireContext());
        } else {
            Theme.getInstance(getContext()).setCurrentTheme(theme);
            requireActivity().recreate();
            dismiss();
        }
    }
}

