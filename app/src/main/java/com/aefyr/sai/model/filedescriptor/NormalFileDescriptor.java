package com.aefyr.sai.model.filedescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class NormalFileDescriptor implements FileDescriptor {

    private File mFile;

    public NormalFileDescriptor(File file) {
        mFile = file;
    }

    @Override
    public String name() {
        return mFile.getName();
    }

    @Override
    public long length() {
        return mFile.length();
    }

    @Override
    public InputStream open() throws Exception {
        return new FileInputStream(mFile);
    }
}
