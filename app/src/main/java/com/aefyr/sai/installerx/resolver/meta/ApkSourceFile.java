package com.aefyr.sai.installerx.resolver.meta;

import java.io.InputStream;
import java.util.List;

public interface ApkSourceFile extends AutoCloseable {

    /**
     * List entries in this ApkSourceFile
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
