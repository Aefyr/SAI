package com.aefyr.sai.ui.dialogs;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.SplitApkSourceMetaAdapter;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.viewmodels.InstallerXDialogViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;

public class InstallerXDialogFragment extends BaseBottomSheetDialogFragment implements FilePickerDialogFragment.OnFilesSelectedListener {

    private InstallerXDialogViewModel mViewModel;

    private PreferencesHelper mHelper;

    public static InstallerXDialogFragment newInstance() {
        return new InstallerXDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = PreferencesHelper.getInstance(requireContext());
        mViewModel = new ViewModelProvider(this).get(InstallerXDialogViewModel.class);
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

        setTitle(R.string.installerx_dialog_title);

        RecyclerView recycler = (RecyclerView) view;
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        SplitApkSourceMetaAdapter adapter = new SplitApkSourceMetaAdapter(mViewModel.getPartsSelection(), this, requireContext());
        recycler.setAdapter(adapter);

        mViewModel.getMeta().observe(this, meta -> {
            adapter.setMeta(meta);
            revealBottomSheet();
        });

        checkPermissionsAndPickFiles();
    }

    private void checkPermissionsAndPickFiles() {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.offset = new File(mHelper.getHomeDirectory());
        properties.extensions = new String[]{"apk", "zip", "apks"};
        properties.sortBy = mHelper.getFilePickerSortBy();
        properties.sortOrder = mHelper.getFilePickerSortOrder();

        FilePickerDialogFragment.newInstance(null, getString(R.string.installer_pick_apks), properties).show(getChildFragmentManager(), "dialog_files_picker");
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
    public void onFilesSelected(String tag, List<File> files) {
        mViewModel.parseApkSourceFile(files.get(0));
    }
}
