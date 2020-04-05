package com.aefyr.sai.installerx.resolver.meta;

import androidx.annotation.Nullable;

import java.io.InputStream;

public interface ApkSourceFile extends AutoCloseable {
    /**
     * Move to the next entry
     *
     * @return next entry or null if there are no more entries
     * @throws Exception
     */
    @Nullable
    Entry nextEntry() throws Exception;

    /**
     * Open an input stream for the current entry
     *
     * @return
     * @throws Exception
     */
    InputStream openEntryInputStream() throws Exception;

    /**
     * Get name of this ApkSourceFile
     *
     * @return
     */
    String getName();

    @Override
    default void close() throws Exception {

    }

    class Entry {
        private String mName;
        private String mLocalPath;

        public Entry(String name, String localPath) {
            mName = name;
            mLocalPath = localPath;
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

    }
}
