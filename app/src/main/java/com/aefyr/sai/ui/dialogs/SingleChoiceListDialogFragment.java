package com.aefyr.sai.ui.dialogs;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;

import java.util.Objects;

public class SingleChoiceListDialogFragment extends BaseBottomSheetDialogFragment {

    protected static final String ARG_PARAMS = "params";
    public interface OnItemSelectedListener {
        void onItemSelected(String dialogTag, int selectedItemIndex);
    }

    private DialogParams mParams;

    /**
     * Create a SingleChoiceListDialogFragment with item checking
     *
     * @param title
     * @param items
     * @param checkedItem
     * @return
     */
    public static SingleChoiceListDialogFragment newInstance(CharSequence title, @ArrayRes int items, int checkedItem) {
        SingleChoiceListDialogFragment fragment = new SingleChoiceListDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAMS, new DialogParams(title, items, checkedItem));
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a SingleChoiceListDialogFragment without item checking
     *
     * @param title
     * @param items
     * @return
     */
    public static SingleChoiceListDialogFragment newInstance(CharSequence title, @ArrayRes int items) {
        SingleChoiceListDialogFragment fragment = new SingleChoiceListDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAMS, new DialogParams(title, items));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        mParams = Objects.requireNonNull(args.getParcelable(ARG_PARAMS), "params must not be null");
        mParams.setTag(getTag());
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

        setTitle(mParams.title);
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
            mItems = getResources().getStringArray(mParams.itemsArrayRes);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.item_single_choice_dialog, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bindTo(mItems[position], mParams.checkedItem != -1, position == mParams.checkedItem);
        }

        @Override
        public int getItemCount() {
            return mItems != null ? mItems.length : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private RadioButton mRadioButton;
            private TextView mText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                mRadioButton = itemView.findViewById(R.id.rb_single_choice_item);
                mText = itemView.findViewById(R.id.tv_single_choice_item);

                itemView.setOnClickListener((v) -> {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION)
                        return;

                    deliverSelectionResult(mParams.tag, adapterPosition);
                    dismiss();
                });
            }

            private void bindTo(String item, boolean checkingEnabled, boolean checked) {
                mText.setText(item);

                if (checkingEnabled) {
                    mRadioButton.setVisibility(View.VISIBLE);
                    mRadioButton.setChecked(checked);
                } else {
                    mRadioButton.setVisibility(View.GONE);
                }
            }
        }

    }

    protected static class DialogParams implements Parcelable {
        private String tag;
        private CharSequence title;
        private int itemsArrayRes;
        private int checkedItem;

        protected DialogParams(CharSequence title, @ArrayRes int itemsArrayRes, int checkedItem) {
            this.title = title;
            this.itemsArrayRes = itemsArrayRes;
            this.checkedItem = checkedItem;
        }

        protected DialogParams(CharSequence title, @ArrayRes int itemsArrayRes) {
            this.title = title;
            this.itemsArrayRes = itemsArrayRes;
            this.checkedItem = -1;
        }

        protected void setTag(String tag) {
            this.tag = tag;
        }

        protected DialogParams(Parcel in) {
            tag = in.readString();
            title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            itemsArrayRes = in.readInt();
            checkedItem = in.readInt();
        }

        public static final Creator<DialogParams> CREATOR = new Creator<DialogParams>() {
            @Override
            public DialogParams createFromParcel(Parcel in) {
                return new DialogParams(in);
            }

            @Override
            public DialogParams[] newArray(int size) {
                return new DialogParams[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(tag);
            TextUtils.writeToParcel(title, dest, 0);
            dest.writeInt(itemsArrayRes);
            dest.writeInt(checkedItem);
        }
    }
}
