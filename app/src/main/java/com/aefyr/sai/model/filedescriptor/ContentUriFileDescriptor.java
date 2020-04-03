package com.aefyr.sai.model.filedescriptor;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.utils.saf.SafUtils;

import java.io.InputStream;

public class ContentUriFileDescriptor implements FileDescriptor {

    private ContentResolver mContentResolver;
    private Uri mContentUri;
    private DocumentFile mDocumentFile;

    public ContentUriFileDescriptor(Context c, Uri contentUri) {
        mContentResolver = c.getContentResolver();
        mContentUri = contentUri;
        mDocumentFile = SafUtils.docFileFromSingleUriOrFileUri(c, contentUri);
    }


    @Override
    public String name() throws Exception {
        String name = mDocumentFile.getName();
        if (name == null)
            throw new BadContentProviderException("DISPLAY_NAME column is null");

        return name;
    }

    @Override
    public long length() throws Exception {
        long length = mDocumentFile.length();

        if (length == 0)
            throw new BadContentProviderException("SIZE column is 0");

        return length;
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
