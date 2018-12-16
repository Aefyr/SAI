package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FilePickerDialogFragment extends DialogFragment {

    public interface OnFilesSelectedListener {
        void onFilesSelected(List<File> files);
    }

    private OnFilesSelectedListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = Environment.getExternalStorageDirectory();
        properties.extensions = new String[]{"apk"};

        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties, Theme.getInstance(getContext()).isDark() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        dialog.setDialogSelectionListener((files) -> {
            if (mListener == null)
                return;

            ArrayList<File> selectedFiles = new ArrayList<>(files.length);

            for (String file : files)
                selectedFiles.add(new File(file));

            mListener.onFilesSelected(selectedFiles);
        });
        dialog.setTitle(R.string.installer_pick_apks);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFilesSelectedListener) getActivity();
        } catch (Exception e) {
            throw new IllegalStateException("Activity that uses FilePickerDialogFragment must implement FilePickerDialogFragment.OnFilesSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
