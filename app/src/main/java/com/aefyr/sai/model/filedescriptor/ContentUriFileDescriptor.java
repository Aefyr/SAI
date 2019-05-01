package com.aefyr.sai.model.filedescriptor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.InputStream;

public class ContentUriFileDescriptor implements FileDescriptor {

    private ContentResolver mContentResolver;
    private Uri mContentUri;

    public ContentUriFileDescriptor(Context c, Uri contentUri) {
        mContentResolver = c.getContentResolver();
        mContentUri = contentUri;
    }


    @Override
    public String name() throws Exception {
        try(Cursor cursor = mContentResolver.query(mContentUri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor == null)
                throw new BadContentProviderException("Cursor is null");

            cursor.moveToFirst();
            String name = cursor.getString(0);

            if (name == null)
                throw new BadContentProviderException("DISPLAY_NAME column is null");

            return name;
        }
    }

    @Override
    public long length() throws Exception {
        try(Cursor cursor = mContentResolver.query(mContentUri, new String[]{MediaStore.MediaColumns.SIZE}, null, null, null)){
            if (cursor == null)
                throw new BadContentProviderException("Cursor is null");

            cursor.moveToFirst();
            long length = cursor.getLong(0);

            if (length == 0)
                throw new BadContentProviderException("SIZE column is 0");

            return length;
        }
    }

    @Override
    public InputStream open() throws Exception {
        return mContentResolver.openInputStream(mContentUri);
    }

    private static class BadContentProviderException extends Exception {

        private BadContentProviderException(String message) {
            super(message);
        }
    }
}
