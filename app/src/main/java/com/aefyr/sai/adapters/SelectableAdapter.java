package com.aefyr.sai.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

//TODO handle data set changes
//TODO maybe just rewrite this
public abstract class SelectableAdapter<Item extends SelectableAdapter.SelectableItem, ViewHolder extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<ViewHolder> {

    private SelectedKeysStorage mStorage;
    private HashMap<String, Integer> mKeyToPosition = new HashMap<>();
    private HashMap<Integer, String> mPositionToKey = new HashMap<>();

    private final SelectionObserver mObserver = new SelectionObserver() {
        @Override
        protected void onSelectionChanged(SelectedKeysStorage storage, String key, boolean selected) {
            Integer position = mKeyToPosition.get(key);
            if (position != null)
                notifyItemChanged(position);
        }
    };

    public void setSelectedItemsStorage(SelectedKeysStorage storage) {
        if (mStorage != null)
            mStorage.removeObserver(mObserver);
        mStorage = storage;
        mStorage.addObserver(mObserver);
        notifyDataSetChanged();
    }

    protected boolean isItemSelected(Item item) {
        ensureStorageAttached();
        return mStorage.isKeySelected(item.toKey());
    }

    protected void setItemSelected(Item item, boolean selected) {
        ensureStorageAttached();
        mStorage.setKeySelected(item.toKey(), selected);
    }

    protected boolean switchSelection(Item item) {
        boolean isSelected = isItemSelected(item);
        setItemSelected(item, !isSelected);
        return !isSelected;
    }

    protected boolean isSelectionStorageAttached() {
        return mStorage != null;
    }

    private void ensureStorageAttached() {
        if (!isSelectionStorageAttached())
            throw new IllegalStateException("No SelectedItemsStorage is attached to this adapter");
    }

    protected abstract Item getItemAt(int position);

    //TODO im pretty sure this is gonna be inconsistent af
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String key = getItemAt(position).toKey();
        mKeyToPosition.put(key, position);
        mPositionToKey.put(position, key);
    }

    public static abstract class SelectionObserver {

        protected void onSelectionChanged(SelectedKeysStorage storage, String key, boolean selected) {

        }

        protected void onSelectionChanged(SelectedKeysStorage storage) {

        }
    }

    public interface SelectedKeysStorage {
        void setKeySelected(String key, boolean selected);

        boolean isKeySelected(String key);

        List<String> getSelectedKeys();

        int getSelectedKeysCount();

        void clear();

        void addObserver(SelectionObserver observer);

        void removeObserver(SelectionObserver observer);
    }

    public interface SelectableItem {
        String toKey();
    }

    public static class SimpleSelectedKeysStorage implements SelectedKeysStorage {

        private LinkedHashSet<SelectionObserver> mObservers = new LinkedHashSet<>();
        private LinkedHashSet<String> mSelectedKeys = new LinkedHashSet<>();

        @Override
        public void setKeySelected(String key, boolean selected) {
            if (selected)
                mSelectedKeys.add(key);
            else
                mSelectedKeys.remove(key);

            for (SelectionObserver observer : mObservers) {
                observer.onSelectionChanged(this, key, selected);
                observer.onSelectionChanged(this);
            }
        }

        @Override
        public boolean isKeySelected(String key) {
            return mSelectedKeys.contains(key);
        }

        @Override
        public List<String> getSelectedKeys() {
            return new ArrayList<>(mSelectedKeys);
        }

        @Override
        public int getSelectedKeysCount() {
            return mSelectedKeys.size();
        }

        @Override
        public void clear() {
            for (String key : mSelectedKeys) {
                mSelectedKeys.remove(key);
                for (SelectionObserver observer : mObservers)
                    observer.onSelectionChanged(this, key, false);
            }
            for (SelectionObserver observer : mObservers)
                observer.onSelectionChanged(this);
        }

        @Override
        public void addObserver(SelectionObserver observer) {
            mObservers.add(observer);
        }

        @Override
        public void removeObserver(SelectionObserver observer) {
            mObservers.remove(observer);
        }
    }

}
