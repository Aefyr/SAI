package com.aefyr.sai.utils;

//TODO better events, casting to T is no good
public class Event2 {

    private boolean mConsumed;
    private String mType;
    private Object mData;

    public Event2(String type, Object data) {
        mType = type;
        mData = data;
    }

    public Event2() {
    }

    public String type() {
        return mType;
    }

    public <T> T consume() {
        if (mConsumed)
            return null;

        mConsumed = true;
        return (T) mData;
    }

    public boolean isConsumed() {
        return mConsumed;
    }
}
