package com.aefyr.pseudoapksigner;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ManifestGenerator {

    private File mApkFile;
    private String mHashingAlgorithm;
    private ArrayList<ManifestEntry> mEntries;

    private long mVersion = 0;
    private String mCachedManifest;
    private long mCachedVersion = -1;

    ManifestGenerator(File apkFile, String hashingAlgorithm) {
        mApkFile = apkFile;
        mHashingAlgorithm = hashingAlgorithm;
        mEntries = new ArrayList<>();
    }

    String getHashingAlgorithm() {
        return mHashingAlgorithm;
    }

    String generate() throws Exception {
        if (mVersion == mCachedVersion)
            return mCachedManifest;

        parseApkAndGenerateEntries();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(generateHeader().toString());
        for (ManifestEntry entry : mEntries) {
            stringBuilder.append(entry.toString());
        }

        mCachedVersion = mVersion;
        mCachedManifest = stringBuilder.toString();

        return mCachedManifest;
    }

    private ManifestEntry generateHeader() {
        ManifestEntry header = new ManifestEntry();
        header.setAttribute("Manifest-Version", "1.0");
        header.setAttribute("Created-By", Constants.GENERATOR_NAME);
        return header;
    }

    static class ManifestEntry {
        private LinkedHashMap<String, String> mAttributes;

        ManifestEntry() {
            mAttributes = new LinkedHashMap<>();
        }

        void setAttribute(String attribute, String value) {
            mAttributes.put(attribute, value);
        }

        String getAttribute(String attribute) {
            return mAttributes.get(attribute);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();

            for (String key : mAttributes.keySet())
                stringBuilder.append(String.format("%s: %s" + Constants.LINE_ENDING, key, mAttributes.get(key)));

            stringBuilder.append(Constants.LINE_ENDING);

            return stringBuilder.toString();
        }
    }

    private void addEntry(ManifestEntry entry) {
        mEntries.add(entry);
        mVersion++;
    }

    List<ManifestEntry> getEntries() {
        return mEntries;
    }

    private void parseApkAndGenerateEntries() throws Exception {
        mEntries.clear();

        ZipFile apkZipFile = new ZipFile(mApkFile);

        Enumeration<? extends ZipEntry> zipEntries = apkZipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();

            if (zipEntry.isDirectory())
                continue;

            if (zipEntry.getName().toLowerCase().startsWith("meta-inf"))
                continue;

            ManifestEntry manifestEntry = new ManifestEntry();
            manifestEntry.setAttribute("Name", zipEntry.getName());
            manifestEntry.setAttribute(mHashingAlgorithm + "-Digest", Utils.base64Encode(Utils.getFileHash(apkZipFile.getInputStream(zipEntry), mHashingAlgorithm)));
            addEntry(manifestEntry);
        }

        apkZipFile.close();
    }
}
