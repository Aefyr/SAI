package com.aefyr.sai.backup2.impl.components;

import com.aefyr.sai.backup2.BackupComponent;

public class SimpleBackupComponent implements BackupComponent {

    private String mType;
    private long mSize;

    public SimpleBackupComponent(String type, long size) {
        mType = type;
        mSize = size;
    }

    @Override
    public String type() {
        return mType;
    }

    @Override
    public long size() {
        return mSize;
    }
}
