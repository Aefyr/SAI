package com.aefyr.sai.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.DefaultFilterConfigViewHolderFactory;
import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.ui.FilterDialog;
import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupPackagesAdapter;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.ui.dialogs.BackupAllSplitApksDialogFragment;
import com.aefyr.sai.ui.dialogs.BackupDialogFragment;
import com.aefyr.sai.ui.dialogs.OneTimeWarningDialogFragment;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
import com.aefyr.sai.ui.recycler.RecyclerPaddingDecoration;
import com.aefyr.sai.utils.MathUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.BackupViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BackupFragment extends SaiBaseFragment implements BackupPackagesAdapter.OnItemInteractionListener, FilterDialog.OnApplyConfigListener, SharedPreferences.OnSharedPreferenceChangeListener {


    private BackupViewModel mViewModel;

    private BackupPackagesAdapter mAdapter;

    private int mSearchBarOffset;

    private int mFocusedItemIndex = -1;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OneTimeWarningDialogFragment.showIfNeeded(requireContext(), getChildFragmentManager(), R.string.help, R.string.backup_warning, "backup_faq");

        mViewModel = new ViewModelProvider(this).get(BackupViewModel.class);


        RecyclerView recyclerView = findViewById(R.id.rv_packages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        int padding = requireContext().getResources().getDimensionPixelSize(R.dimen.backup_recycler_top_bottom_padding);
        recyclerView.addItemDecoration(new RecyclerPaddingDecoration(0, padding, 0, padding));

        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 24);

        mAdapter = new BackupPackagesAdapter(mViewModel.getSelection(), getViewLifecycleOwner(), getContext());
        mAdapter.setInteractionListener(this);
        recyclerView.setAdapter(mAdapter);

        setupToolbar();

        findViewById(R.id.button_backup_filter).setOnClickListener(v -> {
            FilterDialog.newInstance(getString(R.string.backup_filter), mViewModel.getRawFilterConfig(), DefaultFilterConfigViewHolderFactory.class).show(getChildFragmentManager(), null);
        });

        invalidateAppFeaturesVisibility();
        mViewModel.getPackages().observe(getViewLifecycleOwner(), mAdapter::setData);

        PreferencesHelper.getInstance(requireContext()).getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferencesHelper.getInstance(requireContext()).getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setupToolbar() {
        //Search
        EditText editTextSearch = findViewById(R.id.et_search);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mViewModel.search(s.toString());
            }
        });

        findViewById(R.id.ib_backup_search_more).setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), v);
            popupMenu.getMenuInflater().inflate(R.menu.backup_fragment, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener((menuItem) -> {
                switch (menuItem.getItemId()) {
                    case R.id.menu_export_all_split_apks:
                        exportAllSplitApks();
                        break;
                    case R.id.menu_backup_help:
                        SimpleAlertDialogFragment.newInstance(requireContext(), R.string.help, R.string.backup_warning).show(getChildFragmentManager(), null);
                        break;
                }
                return true;
            });

            popupMenu.show();
        });

        //Selection
        findViewById(R.id.ib_backup_clear_selection).setOnClickListener(v -> mViewModel.getSelection().clear());
        findViewById(R.id.ib_backup_select_all).setOnClickListener(v -> {
            List<PackageMeta> packages = mViewModel.getPackages().getValue();
            if (packages == null)
                return;

            Collection<String> keys = new ArrayList<>(packages.size());
            for (PackageMeta pkg : packages) {
                keys.add(pkg.packageName);
            }

            mViewModel.getSelection().batchSetSelected(keys, true);
        });

        //Selection/Search switching
        View searchBarContainer = findViewById(R.id.container_backup_search_bar);
        View selectionBarContainer = findViewById(R.id.container_backup_selection_bar);
        TextView selectionStatus = findViewById(R.id.tv_backup_selection_status);
        mViewModel.getSelection().asLiveData().observe(getViewLifecycleOwner(), selection -> {
            if (selection.hasSelection()) {
                searchBarContainer.setVisibility(View.GONE);
                selectionBarContainer.setVisibility(View.VISIBLE);
                selectionStatus.setText(getString(R.string.backup_selection_status, selection.size()));
            } else {
                searchBarContainer.setVisibility(View.VISIBLE);
                selectionBarContainer.setVisibility(View.GONE);
            }
        });

        //Hide on scroll
        if (!Utils.isTv(requireContext())) {
            CardView searchBar = findViewById(R.id.card_search);
            RecyclerView recyclerView = findViewById(R.id.rv_packages);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (dy == 0)
                        mSearchBarOffset = 0;
                    else
                        mSearchBarOffset = MathUtils.clamp(mSearchBarOffset - dy, -searchBar.getHeight(), 0);

                    searchBar.setTranslationY(mSearchBarOffset);
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mSearchBarOffset != 0 && mSearchBarOffset != -searchBar.getHeight())
                            recyclerView.smoothScrollBy(0, mSearchBarOffset - MathUtils.closest(mSearchBarOffset, 0, -searchBar.getHeight()));
                    }

                }
            });
        } else {
            RecyclerView recyclerView = findViewById(R.id.rv_packages);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        int yOffset = recyclerView.computeVerticalScrollOffset();
                        if (mFocusedItemIndex == 0 && yOffset != 0) {
                            recyclerView.smoothScrollBy(0, -yOffset);
                        } else if (mFocusedItemIndex == mAdapter.getItemCount() - 1) {
                            recyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                        }
                    }

                }
            });
        }
    }

    private void exportAllSplitApks() {
        BackupAllSplitApksDialogFragment.newInstance().show(getChildFragmentManager(), null);
    }

    private void invalidateAppFeaturesVisibility() {
        if (PreferencesHelper.getInstance(requireContext()).shouldShowAppFeatures()) {
            mViewModel.getBackupFilterConfig().observe(getViewLifecycleOwner(), config -> mAdapter.setFilterConfig(config, false));
            mAdapter.setFilterConfig(mViewModel.getBackupFilterConfig().getValue(), true);
        } else {
            mViewModel.getBackupFilterConfig().removeObservers(getViewLifecycleOwner());
            mAdapter.setFilterConfig(null, true);
        }
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_backup;
    }

    @Override
    public void onBackupButtonClicked(PackageMeta packageMeta) {
        BackupDialogFragment.newInstance(packageMeta).show(getChildFragmentManager(), null);
    }

    @Override
    public void onItemFocusChanged(boolean hasFocus, int index, PackageMeta packageMeta) {
        if (hasFocus)
            mFocusedItemIndex = index;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden)
            Utils.hideKeyboard(this);
    }

    @Override
    public void onApplyConfig(ComplexFilterConfig config) {
        mViewModel.applyFilterConfig(config);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesKeys.SHOW_APP_FEATURES.equals(key)) {
            invalidateAppFeaturesVisibility();
        }
    }
}
