package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class SingleChoiceListDialogFragment extends DialogFragment {

    private static final String ARG_TAG = "tag";
    private static final String ARG_ITEMS_ARRAY_RES = "items_array_res";
    private static final String ARG_CHECKED_ITEM = "checked_item";

    public interface OnItemSelectedListener {
        void onItemSelected(String dialogTag, int selectedItemIndex);
    }

    private OnItemSelectedListener mListener;

    private String mTag;
    private int mItemsArrayRes;
    private int mCheckedItem;

    public static SingleChoiceListDialogFragment newInstance(String tag, @ArrayRes int items, int checkedItem) {
        SingleChoiceListDialogFragment fragment = new SingleChoiceListDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_ITEMS_ARRAY_RES, items);
        args.putInt(ARG_CHECKED_ITEM, checkedItem);

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
        mItemsArrayRes = args.getInt(ARG_ITEMS_ARRAY_RES);
        mCheckedItem = args.getInt(ARG_CHECKED_ITEM);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setSingleChoiceItems(mItemsArrayRes, mCheckedItem, (dialog, which) -> {
                    if (mListener != null)
                        mListener.onItemSelected(mTag, which);
                    dismiss();
                }).create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null)
                mListener = (OnItemSelectedListener) getParentFragment();
            else
                mListener = (OnItemSelectedListener) getActivity();
        } catch (Exception e) {
            throw new IllegalStateException("Activity/Fragment that uses SingleChoiceListDialogFragment must implement SingleChoiceListDialogFragment.OnItemSelectedListener");
        }
    }
}
