package com.aefyr.sai.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupNameFormatBuilderPartsAdapter;
import com.aefyr.sai.model.backup.BackupNameFormatBuilder;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.BackupNameFormat;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.NameFormatBuilderViewModel;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.Arrays;

public class NameFormatBuilderDialogFragment extends BaseBottomSheetDialogFragment {

    private static final String EXTRA_FORMAT = "format";

    private NameFormatBuilderViewModel mViewModel;

    public static NameFormatBuilderDialogFragment newInstance(String format) {
        NameFormatBuilderDialogFragment fragment = new NameFormatBuilderDialogFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_FORMAT, format);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String format = requireArguments().getString(EXTRA_FORMAT);

        mViewModel = new ViewModelProvider(this, new NameFormatBuilderViewModel.Factory(requireContext(), format)).get(NameFormatBuilderViewModel.class);
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_name_format_builder, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        revealBottomSheet();
        setTitle(R.string.name_format_builder_title);

        getNegativeButton().setOnClickListener((v) -> dismiss());
        getPositiveButton().setOnClickListener((v) -> {
            deliverFormatAndDismiss(mViewModel.getFormat().getValue().build());
        });

        RecyclerView recycler = view.findViewById(R.id.rv_name_builder);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);
        recycler.setLayoutManager(layoutManager);

        BackupNameFormatBuilderPartsAdapter adapter = new BackupNameFormatBuilderPartsAdapter(mViewModel.getSelection(), this, requireContext());
        adapter.setData(Arrays.asList(BackupNameFormatBuilder.Part.values()));
        recycler.setAdapter(adapter);

        TextView preview = view.findViewById(R.id.tv_name_builder_sample);

        mViewModel.getSelection().asLiveData().observe(this, (selection) -> getPositiveButton().setEnabled(selection.hasSelection()));
        mViewModel.getFormat().observe(this, (format) -> {
            preview.setText(format.getParts().isEmpty() ? getString(R.string.name_format_builder_preview_empty) : getString(R.string.name_format_builder_preview, BackupNameFormat.format(format.build(), mViewModel.getOwnMeta())));
        });
    }

    private void deliverFormatAndDismiss(String format) {
        OnFormatBuiltListener listener = Utils.getParentAs(this, OnFormatBuiltListener.class);
        if (listener != null)
            listener.onFormatBuilt(getTag(), format);

        dismiss();
    }

    public interface OnFormatBuiltListener {

        void onFormatBuilt(@Nullable String tag, @NonNull String format);

    }
}
