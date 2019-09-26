package com.aefyr.sai.adapters.selection;

import android.annotation.SuppressLint;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

public abstract class SelectableAdapter<Key, ViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<ViewHolder> {

    private final Selection<Key> mSelection;
    private final HashMap<Key, Integer> mKeyToPosition = new HashMap<>();
    @SuppressLint("UseSparseArrays")
    private final HashMap<Integer, Key> mPositionToKey = new HashMap<>();

    private final Selection.Observer<Key> mSelectionObserver = new Selection.Observer<Key>() {
        @Override
        protected void onKeySelectionChanged(Selection<Key> selection, Key key) {
            Integer position = mKeyToPosition.get(key);
            if (position != null)
                notifyItemChanged(position);
        }

        @Override
        protected void onCleared(Selection<Key> selection) {
            clearKeysMapping();
            notifyDataSetChanged();
        }
    };

    private final RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            clearKeysMapping();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            for (int i = positionStart; i < positionStart + itemCount; i++)
                mKeyToPosition.remove(mPositionToKey.remove(i));
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            //TODO optimize this
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            //TODO optimize this
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            //TODO optimize this
            notifyDataSetChanged();
        }
    };

    public SelectableAdapter(Selection<Key> selection) {
        mSelection = selection;
    }

    public final Selection<Key> getSelection() {
        return mSelection;
    }

    protected final boolean isSelected(Key key) {
        return mSelection.isSelected(key);
    }

    protected final void setSelected(Key key, boolean selected) {
        mSelection.setSelected(key, selected);
    }

    protected final boolean switchSelection(Key key) {
        return mSelection.switchSelection(key);
    }

    protected abstract Key getKeyForPosition(int position);

    @CallSuper
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Key key = getKeyForPosition(position);
        mKeyToPosition.put(key, position);
        mPositionToKey.put(position, key);
    }

    @CallSuper
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mSelection.addObserver(mSelectionObserver);
        registerAdapterDataObserver(mAdapterObserver);
    }

    @CallSuper
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mSelection.removeObserver(mSelectionObserver);
        unregisterAdapterDataObserver(mAdapterObserver);
    }

    private void clearKeysMapping() {
        mPositionToKey.clear();
        mKeyToPosition.clear();
    }
}
