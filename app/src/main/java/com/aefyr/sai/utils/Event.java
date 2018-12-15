package com.aefyr.sai.utils;

public class Event<T> {
    private boolean mConsumed;
    private T mData;

    public Event(T t) {
        mData = t;
    }

    public T consume() {
        if (mConsumed)
            return null;

        mConsumed = true;
        return mData;
    }

    public boolean isConsumed() {
        return mConsumed;
    }
}
