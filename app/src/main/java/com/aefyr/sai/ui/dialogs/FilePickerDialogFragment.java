package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FilePickerDialogFragment extends DialogFragment {

    private static final String ARG_TAG = "tag";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SELECTION_MODE = "selection_mode";
    private static final String ARG_SELECTION_TYPE = "selection_type";
    private static final String ARG_ROOT_FOLDER = "root";
    private static final String ARG_STARTING_FOLDER = "start";
    private static final String ARG_ERROR_FOLDER = "error";
    private static final String ARG_EXTENSIONS = "extensions";
    private static final String ARG_SORT_BY = "sort_by";
    private static final String ARG_SORT_ORDER = "sort_order";

    public interface OnFilesSelectedListener {
        void onFilesSelected(String tag, List<File> files);
    }

    private OnFilesSelectedListener mListener;

    private String mTag;
    private String mTitle = "Select files";
    private DialogProperties mDialogProperties = new DialogProperties();

    public static FilePickerDialogFragment newInstance(String tag, String title, DialogProperties properties) {
        FilePickerDialogFragment fragment = new FilePickerDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_SELECTION_MODE, properties.selection_mode);
        args.putInt(ARG_SELECTION_TYPE, properties.selection_type);
        args.putString(ARG_ROOT_FOLDER, properties.root.getAbsolutePath());
        args.putString(ARG_STARTING_FOLDER, properties.offset.getAbsolutePath());
        args.putString(ARG_ERROR_FOLDER, properties.error_dir.getAbsolutePath());
        args.putStringArray(ARG_EXTENSIONS, properties.extensions);
        args.putInt(ARG_SORT_BY, properties.sortBy);
        args.putInt(ARG_SORT_ORDER, properties.sortOrder);
        //properties.
        //args.putInt(ARG_SORT_BY, properties.);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        mTag = args.getString(ARG_TAG);
        mTitle = args.getString(ARG_TITLE);

        mDialogProperties.selection_mode = args.getInt(ARG_SELECTION_MODE, mDialogProperties.selection_mode);
        mDialogProperties.selection_type = args.getInt(ARG_SELECTION_TYPE, mDialogProperties.selection_type);
        mDialogProperties.root = new File(args.getString(ARG_ROOT_FOLDER, mDialogProperties.root.getAbsolutePath()));
        mDialogProperties.offset = new File(args.getString(ARG_STARTING_FOLDER, mDialogProperties.offset.getAbsolutePath()));
        mDialogProperties.error_dir = new File(args.getString(ARG_ERROR_FOLDER, mDialogProperties.error_dir.getAbsolutePath()));
        mDialogProperties.extensions = args.getStringArray(ARG_EXTENSIONS);
        mDialogProperties.sortBy = args.getInt(ARG_SORT_BY, mDialogProperties.sortBy);
        mDialogProperties.sortOrder = args.getInt(ARG_SORT_ORDER, mDialogProperties.sortOrder);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        FilePickerDialog dialog = new FilePickerDialog(getContext(), mDialogProperties, Theme.getInstance(getContext()).isDark() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        dialog.setDialogSelectionListener((files) -> {
            if (mListener == null)
                return;

            ArrayList<File> selectedFiles = new ArrayList<>(files.length);

            for (String file : files)
                selectedFiles.add(new File(file));

            mListener.onFilesSelected(mTag, selectedFiles);
        });
        dialog.setTitle(mTitle);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null)
                mListener = (OnFilesSelectedListener) getParentFragment();
            else
                mListener = (OnFilesSelectedListener) getActivity();
        } catch (Exception e) {
            throw new IllegalStateException("Activity/Fragment that uses FilePickerDialogFragment must implement FilePickerDialogFragment.OnFilesSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
