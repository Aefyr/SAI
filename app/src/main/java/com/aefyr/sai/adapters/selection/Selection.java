package com.aefyr.sai.adapters.selection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Selection<Key> {

    private KeyStorage<Key> mKeyStorage;

    private Set<Observer<Key>> mObservers = new HashSet<>();
    private MutableLiveData<Selection<Key>> mLiveSelection = new MutableLiveData<>(this);

    public Selection(KeyStorage keyStorage) {
        mKeyStorage = keyStorage;
    }

    public void setSelected(Key key, boolean selected) {
        if (selected)
            mKeyStorage.store(key);
        else
            mKeyStorage.remove(key);

        for (Observer<Key> observer : mObservers) {
            observer.onKeySelectionChanged(this, key);
        }
        mLiveSelection.setValue(this);
    }

    public boolean switchSelection(Key key) {
        boolean isSelected = isSelected(key);
        setSelected(key, !isSelected);
        return !isSelected;
    }

    public boolean isSelected(Key key) {
        return mKeyStorage.isStored(key);
    }

    public Collection<Key> getSelectedKeys() {
        return mKeyStorage.getAllStoredKeys();
    }

    public boolean hasSelection() {
        return mKeyStorage.getStoredKeysCount() > 0;
    }

    public void clear() {
        mKeyStorage.clear();

        for (Observer<Key> observer : mObservers) {
            observer.onCleared(this);
        }
    }

    public void addObserver(Observer<Key> observer) {
        mObservers.add(observer);
    }

    public void removeObserver(Observer<Key> observer) {
        mObservers.remove(observer);
    }

    public LiveData<Selection<Key>> asLiveData() {
        return mLiveSelection;
    }


    public static abstract class Observer<Key> {

        protected void onKeySelectionChanged(Selection<Key> selection, Key key) {

        }

        protected void onCleared(Selection<Key> selection) {

        }

    }


    public interface KeyStorage<K> {
        void store(K key);

        void remove(K key);

        boolean isStored(K key);

        Collection<K> getAllStoredKeys();

        int getStoredKeysCount();

        void clear();
    }

}
