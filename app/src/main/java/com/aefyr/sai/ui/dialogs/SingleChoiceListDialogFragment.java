package com.aefyr.sai.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;

public class SingleChoiceListDialogFragment extends BaseBottomSheetDialogFragment {

    protected static final String ARG_TAG = "tag";
    protected static final String ARG_TITLE = "title";
    protected static final String ARG_ITEMS_ARRAY_RES = "items_array_res";
    protected static final String ARG_CHECKED_ITEM = "checked_item";

    public interface OnItemSelectedListener {
        void onItemSelected(String dialogTag, int selectedItemIndex);
    }

    private String mTag;
    private CharSequence mTitle;
    private int mItemsArrayRes;
    private int mCheckedItem;

    public static SingleChoiceListDialogFragment newInstance(String tag, CharSequence title, @ArrayRes int items, int checkedItem) {
        SingleChoiceListDialogFragment fragment = new SingleChoiceListDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putCharSequence(ARG_TITLE, title);
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
        mTitle = args.getCharSequence(ARG_TITLE);
        mItemsArrayRes = args.getInt(ARG_ITEMS_ARRAY_RES);
        mCheckedItem = args.getInt(ARG_CHECKED_ITEM);
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

        setTitle(mTitle);
        getPositiveButton().setVisibility(View.GONE);
        getNegativeButton().setOnClickListener(v -> dismiss());

        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new ItemsAdapter());

        revealBottomSheet();
    }

    protected void deliverSelectionResult(String tag, int selectedItemIndex) {
        try {
            OnItemSelectedListener listener;
            if (getParentFragment() != null)
                listener = (OnItemSelectedListener) getParentFragment();
            else
                listener = (OnItemSelectedListener) getActivity();

            if (listener != null)
                listener.onItemSelected(tag, selectedItemIndex);
        } catch (Exception e) {
            throw new IllegalStateException("Activity/Fragment that uses SingleChoiceListDialogFragment must implement SingleChoiceListDialogFragment.OnItemSelectedListener");
        }
    }

    private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

        private LayoutInflater mInflater;

        private String[] mItems;

        private ItemsAdapter() {
            mInflater = LayoutInflater.from(requireContext());
            mItems = getResources().getStringArray(mItemsArrayRes);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.item_single_choice_dialog, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindTo(mItems[position], position == mCheckedItem);
        }

        @Override
        public int getItemCount() {
            return mItems != null ? mItems.length : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private RadioButton mRadioButton;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                mRadioButton = itemView.findViewById(R.id.rb_single_choice_item);

                itemView.setOnClickListener((v) -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION)
                        return;

                    deliverSelectionResult(mTag, adapterPosition);
                    dismiss();
                });
            }

            private void bindTo(String item, boolean checked) {
                mRadioButton.setText(item);
                mRadioButton.setChecked(checked);
            }
        }

    }
}
