package com.aefyr.sai.installerx.resolver.meta;

import java.io.InputStream;
import java.util.List;

public interface ApkSourceFile extends AutoCloseable {

    /**
     * List entries in this ApkSourceFile
     *
     * @return
     * @throws Exception
     */
    List<Entry> listEntries() throws Exception;

    /**
     * Open an input stream for the given entry
     *
     * @return
     * @throws Exception
     */
    InputStream openEntryInputStream(Entry entry) throws Exception;

    /**
     * Get name of this ApkSourceFile
     *
     * @return
     */
    String getName();

    @Override
    default void close() {

    }

    class Entry {
        private String mName;
        private String mLocalPath;
        private long mSize;

        public Entry(String name, String localPath, long size) {
            mName = name;
            mLocalPath = localPath;
            mSize = size;
        }

        public String getName() {
            return mName;
        }

        /**
         * @return path of this entry in the ApkSourceFile
         */
        public String getLocalPath() {
            return mLocalPath;
        }

        /**
         * @return size in bytes of this entry or -1 if unknown
         */
        public long getSize() {
            return mSize;
        }

    }
}
