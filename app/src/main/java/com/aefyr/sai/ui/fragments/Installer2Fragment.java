package com.aefyr.sai.ui.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.SaiPiSessionsAdapter;
import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.DarkLightThemeSelectionDialogFragment;
import com.aefyr.sai.ui.dialogs.DonationSuggestionDialogFragment;
import com.aefyr.sai.ui.dialogs.ErrorLogDialogFragment2;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.InstallationConfirmationDialogFragment;
import com.aefyr.sai.ui.dialogs.InstallerXDialogFragment;
import com.aefyr.sai.ui.dialogs.ThemeSelectionDialogFragment;
import com.aefyr.sai.ui.recycler.RecyclerPaddingDecoration;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Installer2Fragment extends InstallerFragment implements FilePickerDialogFragment.OnFilesSelectedListener, InstallationConfirmationDialogFragment.ConfirmationListener, SaiPiSessionsAdapter.ActionDelegate {

    private static final int REQUEST_CODE_GET_FILES = 337;

    private InstallerViewModel mViewModel;

    private RecyclerView mSessionsRecycler;
    private ViewGroup mPlaceholderContainer;

    private PreferencesHelper mHelper;

    private Uri mPendingActionViewUri;

    private ToolTipsManager mToolTipsManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHelper = PreferencesHelper.getInstance(getContext());

        mPlaceholderContainer = findViewById(R.id.container_installer_placeholder);

        mSessionsRecycler = findViewById(R.id.rv_installer_sessions);
        mSessionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        SaiPiSessionsAdapter sessionsAdapter = new SaiPiSessionsAdapter(requireContext());
        sessionsAdapter.setActionsDelegate(this);

        mSessionsRecycler.setAdapter(sessionsAdapter);
        mSessionsRecycler.addItemDecoration(new RecyclerPaddingDecoration(0, requireContext().getResources().getDimensionPixelSize(R.dimen.installer_sessions_recycler_top_padding), 0, requireContext().getResources().getDimensionPixelSize(R.dimen.installer_sessions_recycler_bottom_padding)));

        mViewModel = new ViewModelProvider(this).get(InstallerViewModel.class);
        mViewModel.getEvents().observe(getViewLifecycleOwner(), (event) -> {
            if (event.isConsumed())
                return;

            //For some reason this observer gets called after state save on some devices
            if (isStateSaved())
                return;

            if (event.type().equals(InstallerViewModel.EVENT_PACKAGE_INSTALLED))
                DonationSuggestionDialogFragment.showIfNeeded(requireContext(), getChildFragmentManager());

            if (!mHelper.showInstallerDialogs()) {
                event.consume();
                return;
            }

            switch (event.type()) {
                case InstallerViewModel.EVENT_PACKAGE_INSTALLED:
                    showPackageInstalledAlert(event.consume());
                    break;
                case InstallerViewModel.EVENT_INSTALLATION_FAILED:
                    String[] errors = event.consume();
                    ErrorLogDialogFragment2.newInstance(getString(R.string.installer_installation_failed), errors[0], errors[1], false).show(getChildFragmentManager(), "installation_error_dialog");
                    break;
            }
        });
        mViewModel.getSessions().observe(getViewLifecycleOwner(), (sessions) -> {
            setPlaceholderShown(sessions.size() == 0);
            sessionsAdapter.setData(sessions);
        });

        findViewById(R.id.ib_toggle_theme).setOnClickListener((v -> {
            if (Theme.getInstance(requireContext()).getThemeMode() == Theme.Mode.AUTO_LIGHT_DARK) {
                DarkLightThemeSelectionDialogFragment.newInstance(DarkLightThemeSelectionDialogFragment.MODE_APPLY).show(getChildFragmentManager(), null);
            } else {
                ThemeSelectionDialogFragment.newInstance(requireContext()).show(getChildFragmentManager(), "theme_selection_dialog");
            }
        }));
        findViewById(R.id.ib_help).setOnClickListener((v) -> AlertsUtils.showAlert(this, R.string.help, R.string.installer_help));

        Button installButtton = findViewById(R.id.button_install);
        installButtton.setOnClickListener((v) -> {
            if (mHelper.isInstallerXEnabled())
                openInstallerXDialog();
            else
                checkPermissionsAndPickFiles();
        });
        installButtton.setOnLongClickListener((v) -> pickFilesWithSaf());

        if (mHelper.shouldShowSafTip()) {
            mToolTipsManager = new ToolTipsManager((view1, anchorViewId, byUser) -> {
                if (byUser)
                    mHelper.setSafTipShown();
            });

            ToolTip tooltip = new ToolTip.Builder(requireContext(), installButtton, ((ViewGroup) view), getText(R.string.installer_saf_tip), ToolTip.POSITION_ABOVE)
                    .setBackgroundColor(Utils.getThemeColor(requireContext(), R.attr.colorAccent))
                    .setTextAppearance(R.style.SAITooltipTextAppearance)
                    .setGravity(ToolTip.GRAVITY_CENTER)
                    .build();

            installButtton.post(() -> mToolTipsManager.show(tooltip));
        }

        if (mPendingActionViewUri != null) {
            handleActionView(mPendingActionViewUri);
            mPendingActionViewUri = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mToolTipsManager != null)
            mToolTipsManager.dismissAll();

    }

    @Override
    public void handleActionView(Uri uri) {
        if (!isAdded()) {
            mPendingActionViewUri = uri;
            return;
        }

        DialogFragment existingDialog = (DialogFragment) getChildFragmentManager().findFragmentByTag("installation_confirmation_dialog");
        if (existingDialog != null)
            existingDialog.dismiss();

        InstallationConfirmationDialogFragment.newInstance(uri).show(getChildFragmentManager(), "installation_confirmation_dialog");
    }

    private void setPlaceholderShown(boolean shown) {
        if (shown) {
            mPlaceholderContainer.setVisibility(View.VISIBLE);
            mSessionsRecycler.setVisibility(View.GONE);
        } else {
            mPlaceholderContainer.setVisibility(View.GONE);
            mSessionsRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void openInstallerXDialog() {
        new InstallerXDialogFragment().show(getChildFragmentManager(), null);
    }

    private void checkPermissionsAndPickFiles() {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.offset = new File(mHelper.getHomeDirectory());
        properties.extensions = new String[]{"apk", "zip", "apks", "xapk"};
        properties.sortBy = mHelper.getFilePickerSortBy();
        properties.sortOrder = mHelper.getFilePickerSortOrder();

        FilePickerDialogFragment.newInstance(null, getString(R.string.installer_pick_apks), properties).show(getChildFragmentManager(), "dialog_files_picker");
    }

    private boolean pickFilesWithSaf() {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("*/*");
        getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(getContentIntent, getString(R.string.installer_pick_apks)), REQUEST_CODE_GET_FILES);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
            else
                checkPermissionsAndPickFiles();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GET_FILES) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            //TODO support multiple .apks files here
            if (data.getData() != null) {
                mViewModel.installPackagesFromContentProviderZip(data.getData());
                return;
            }

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                List<Uri> apkUris = new ArrayList<>(clipData.getItemCount());

                for (int i = 0; i < clipData.getItemCount(); i++)
                    apkUris.add(clipData.getItemAt(i).getUri());

                mViewModel.installPackagesFromContentProviderUris(apkUris);
            }
        }
    }

    private void showPackageInstalledAlert(String packageName) {
        AppInstalledDialogFragment.newInstance(packageName).show(getChildFragmentManager(), "dialog_app_installed");
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        if (files.size() == 0 || !ensureExtensionsConsistency(files)) {
            AlertsUtils.showAlert(this, R.string.error, R.string.installer_error_installer2_mixed_extensions_internal);
            return;
        }

        String extension = Utils.getExtension(files.get(0).getName());

        if ("apks".equals(extension) || "zip".equals(extension) || "xapk".equals(extension)) {
            mViewModel.installPackagesFromZip(files);
        } else if ("apk".equals(extension)) {
            mViewModel.installPackages(files);
        } else {
            AlertsUtils.showAlert(this, R.string.error, R.string.installer_error_installer2_mixed_extensions_internal);
        }
    }

    private boolean ensureExtensionsConsistency(List<File> files) {
        String firstFileExtension = Utils.getExtension(files.get(0).getName());
        if (firstFileExtension == null)
            return false;

        for (int i = 1; i < files.size(); i++) {
            if (!files.get(i).getName().endsWith(firstFileExtension))
                return false;
        }

        return true;
    }

    @Override
    public void onConfirmed(Uri apksFileUri) {
        mViewModel.installPackagesFromContentProviderZip(apksFileUri);
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_installer2;
    }

    @Override
    public void launchApp(String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            Intent appLaunchIntent = pm.getLaunchIntentForPackage(packageName);
            Objects.requireNonNull(appLaunchIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(appLaunchIntent);
        } catch (Exception e) {
            Log.w("SAI", e);
            Toast.makeText(requireContext(), R.string.installer_unable_to_launch_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showError(String shortError, String fullError) {
        ErrorLogDialogFragment2.newInstance(getString(R.string.installer_installation_failed), shortError, fullError, false).show(getChildFragmentManager(), "installation_error_dialog");
    }
}
