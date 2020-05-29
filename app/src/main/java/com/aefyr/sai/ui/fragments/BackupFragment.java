package com.aefyr.sai.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.DefaultFilterConfigViewHolderFactory;
import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.ui.FilterDialog;
import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupPackagesAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.ui.activities.BackupManageAppActivity;
import com.aefyr.sai.ui.dialogs.BatchBackupDialogFragment;
import com.aefyr.sai.ui.dialogs.DonationSuggestionDialogFragment;
import com.aefyr.sai.ui.dialogs.OneTimeWarningDialogFragment;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
import com.aefyr.sai.ui.recycler.RecyclerPaddingDecoration;
import com.aefyr.sai.utils.MathUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.view.NumberTextView;
import com.aefyr.sai.viewmodels.BackupViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class BackupFragment extends SaiBaseFragment implements BackupPackagesAdapter.OnItemInteractionListener, FilterDialog.OnApplyConfigListener, SharedPreferences.OnSharedPreferenceChangeListener, BatchBackupDialogFragment.OnBatchBackupEnqueuedListener {


    private BackupViewModel mViewModel;

    private BackupPackagesAdapter mAdapter;

    private CardView mSearchBar;
    private int mSearchBarOffset;
    private boolean mSearchBarScrollResponsePaused = false;

    private int mFocusedItemIndex = -1;

    private Fragment mStorageConfigFragment;

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

        findViewById(R.id.button_backup_action).setOnClickListener(v -> {
            Selection<String> selection = mViewModel.getSelection();
            if (!selection.hasSelection()) {
                FilterDialog.newInstance(getString(R.string.backup_filter), mViewModel.getRawFilterConfig(), DefaultFilterConfigViewHolderFactory.class).show(getChildFragmentManager(), null);
            } else {
                BatchBackupDialogFragment.newInstance(new ArrayList<>(mViewModel.getSelection().getSelectedKeys())).show(getChildFragmentManager(), null);
            }
        });

        invalidateAppFeaturesVisibility();
        mViewModel.getPackages().observe(getViewLifecycleOwner(), mAdapter::setData);

        ViewGroup indexingOverlay = findViewById(R.id.overlay_backup_indexing);
        ProgressBar indexingProgressBar = findViewById(R.id.progress_backup_indexing);
        TextView indexingStatusText = findViewById(R.id.tv_backup_indexing_status);
        mViewModel.getIndexingStatus().observe(getViewLifecycleOwner(), status -> {
            if (status.isInProgress()) {
                indexingOverlay.setVisibility(View.VISIBLE);
                indexingStatusText.setText(getString(R.string.backup_indexing, status.progress(), status.goal()));

                indexingProgressBar.setMax(status.goal());
                indexingProgressBar.setProgress(status.progress());

            } else {
                indexingOverlay.setVisibility(View.GONE);
            }
        });

        FragmentContainerView storageConfigOverlay = findViewById(R.id.fragment_container_backup_storage_setup);
        mViewModel.getDefaultStorageProvider().getIsConfiguredLiveData().observe(getViewLifecycleOwner(), isConfigured -> {
            if (isConfigured) {
                storageConfigOverlay.setVisibility(View.GONE);

                if (mStorageConfigFragment != null) {
                    getChildFragmentManager().beginTransaction()
                            .remove(mStorageConfigFragment)
                            .commitNow();

                    mStorageConfigFragment = null;
                }

            } else {
                storageConfigOverlay.setVisibility(View.VISIBLE);

                if (mStorageConfigFragment == null) {
                    mStorageConfigFragment = mViewModel.getDefaultStorageProvider().createConfigFragment();

                    getChildFragmentManager().beginTransaction()
                            .add(R.id.fragment_container_backup_storage_setup, mStorageConfigFragment)
                            .commitNow();
                }
            }
        });


        PreferencesHelper.getInstance(requireContext()).getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferencesHelper.getInstance(requireContext()).getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    private boolean mViewStateRestored;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mViewStateRestored = true;
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
                if (mViewStateRestored)
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
                    case R.id.menu_backup_reindex:
                        mViewModel.reindexBackups();
                        break;
                }
                return true;
            });

            popupMenu.show();
        });

        //Selection
        findViewById(R.id.ib_backup_toolbar_action).setOnClickListener(v -> {
            if (mViewModel.getSelection().hasSelection())
                mViewModel.getSelection().clear();
        });
        findViewById(R.id.ib_backup_select_all).setOnClickListener(v -> mViewModel.selectAllApps());

        //Selection/Search switching
        View searchBarContainer = findViewById(R.id.container_backup_search_bar);
        View selectionBarContainer = findViewById(R.id.container_backup_selection_bar);
        NumberTextView selectionStatus = findViewById(R.id.tv_backup_selection_status);

        ImageButton toolbarActionButton = findViewById(R.id.ib_backup_toolbar_action);

        MaterialButton actionButton = findViewById(R.id.button_backup_action);
        mViewModel.getSelection().asLiveData().observe(getViewLifecycleOwner(), selection -> {
            if (selection.hasSelection()) {
                searchBarContainer.setVisibility(View.GONE);
                selectionBarContainer.setVisibility(View.VISIBLE);

                selectionStatus.setNumber(selection.size(), true);

                actionButton.setText(R.string.backup_enqueue);
                actionButton.setIconResource(R.drawable.ic_backup_enqueue);

                toolbarActionButton.setClickable(true);
                toolbarActionButton.setImageResource(R.drawable.ic_clear_selection);
                toolbarActionButton.setColorFilter(Utils.getThemeColor(requireContext(), R.attr.colorAccent));

                mSearchBarScrollResponsePaused = true;
                mSearchBarOffset = 0;
                mSearchBar.animate()
                        .setDuration(200)
                        .translationY(mSearchBarOffset)
                        .start();
            } else {
                searchBarContainer.setVisibility(View.VISIBLE);
                selectionBarContainer.setVisibility(View.GONE);

                selectionStatus.setNumber(0, false);

                actionButton.setText(R.string.backup_filter);
                actionButton.setIconResource(R.drawable.ic_filter);

                toolbarActionButton.setClickable(false);
                toolbarActionButton.setImageResource(R.drawable.ic_search);
                toolbarActionButton.setColorFilter(Utils.getThemeColor(requireContext(), android.R.attr.textColorSecondary));

                mSearchBarScrollResponsePaused = false;
                mSearchBar.clearAnimation();
            }
        });

        //Hide on scroll
        if (!Utils.isTv(requireContext())) {
            mSearchBar = findViewById(R.id.card_search);
            RecyclerView recyclerView = findViewById(R.id.rv_packages);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (mSearchBarScrollResponsePaused)
                        return;

                    if (dy == 0)
                        mSearchBarOffset = 0;
                    else
                        mSearchBarOffset = MathUtils.clamp(mSearchBarOffset - dy, -mSearchBar.getHeight(), 0);

                    mSearchBar.setTranslationY(mSearchBarOffset);
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mSearchBarScrollResponsePaused)
                            return;

                        if (mSearchBarOffset != 0 && mSearchBarOffset != -mSearchBar.getHeight())
                            recyclerView.smoothScrollBy(0, mSearchBarOffset - MathUtils.closest(mSearchBarOffset, 0, -mSearchBar.getHeight()));
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
        BatchBackupDialogFragment.newInstance().show(getChildFragmentManager(), null);
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
    public void onBackupButtonClicked(BackupApp backupApp) {
        BackupManageAppActivity.start(requireContext(), backupApp.packageMeta().packageName);
    }

    @Override
    public void onItemFocusChanged(boolean hasFocus, int index, BackupApp backupApp) {
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

    @Override
    public void onBatchBackupEnqueued(@Nullable String dialogTag) {
        mViewModel.getSelection().clear();
        DonationSuggestionDialogFragment.showIfNeeded(requireContext(), getChildFragmentManager());
    }
}
