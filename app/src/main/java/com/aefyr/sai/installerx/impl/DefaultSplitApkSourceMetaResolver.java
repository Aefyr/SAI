package com.aefyr.sai.installerx.impl;

import android.util.Log;

import com.aefyr.sai.installerx.SplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.splitmeta.BaseSplitMeta;
import com.aefyr.sai.installerx.splitmeta.SplitMeta;
import com.aefyr.sai.installerx.util.AndroidBinXmlParser;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.model.installerx.SplitApkSourceMeta;
import com.aefyr.sai.model.installerx.SplitCategory;
import com.aefyr.sai.model.installerx.SplitPart;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Stopwatch;
import com.aefyr.sai.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DefaultSplitApkSourceMetaResolver implements SplitApkSourceMetaResolver {
    private static final String TAG = "DSASMetaResolver";

    private static final String MANIFEST_FILE = "AndroidManifest.xml";

    private static final String CATEGORY_ID_BASE_APK = "base";

    @Override
    public SplitApkSourceMeta resolveFor(File apkSourceFile) throws Exception {
        Stopwatch sw = new Stopwatch();

        try {
            SplitApkSourceMeta meta = parseViaParsingManifests(apkSourceFile);
            Log.d(TAG, String.format("Resolved meta for %s via parsing manifests in %d ms.", apkSourceFile.getAbsoluteFile(), sw.millisSinceStart()));
            return meta;
        } catch (Exception e) {
            //TODO alt parse
            throw e;
        }
    }

    private SplitApkSourceMeta parseViaParsingManifests(File apkSourceFile) throws Exception {
        ZipFile zipFile = new ZipFile(apkSourceFile);

        String packageName = null;
        boolean seenApk = false;
        boolean seenBaseApk = false;

        ArrayList<SplitCategory> categories = new ArrayList<>();

        ArrayList<SplitPart> parts = new ArrayList<>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!Utils.getFileNameFromZipEntry(entry).toLowerCase().endsWith(".apk"))
                continue;

            seenApk = true;
            boolean seenManifestElement = false;

            HashMap<String, String> manifestAttrs = new HashMap<>();

            AndroidBinXmlParser parser = new AndroidBinXmlParser(stealManifestFromApk(zipFile.getInputStream(entry)));
            int eventType = parser.getEventType();
            while (eventType != AndroidBinXmlParser.EVENT_END_DOCUMENT) {

                if (eventType == AndroidBinXmlParser.EVENT_START_ELEMENT) {
                    if (parser.getName().equals("manifest") && parser.getNamespace().isEmpty()) {
                        if (seenManifestElement)
                            throw new RuntimeException("Duplicate manifest element found");

                        seenManifestElement = true;

                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).isEmpty())
                                continue;

                            String namespace = "" + (parser.getAttributeNamespace(i).isEmpty() ? "" : (parser.getAttributeNamespace(i) + ":"));

                            manifestAttrs.put(namespace + parser.getAttributeName(i), parser.getAttributeStringValue(i));
                        }
                    }
                }


                eventType = parser.next();
            }

            if (!seenManifestElement)
                throw new RuntimeException("No manifest element found in xml");

            SplitMeta splitMeta = SplitMeta.from(manifestAttrs);
            if (packageName == null) {
                packageName = splitMeta.packageName();
            } else {
                if (!packageName.equals(splitMeta.packageName()))
                    throw new RuntimeException("Parts have mismatching packages");
            }

            if (splitMeta instanceof BaseSplitMeta) {
                if (seenBaseApk)
                    throw new RuntimeException("Multiple base APKs found");

                seenBaseApk = true;

                BaseSplitMeta baseSplitMeta = (BaseSplitMeta) splitMeta;
                categories.add(new SplitCategory(CATEGORY_ID_BASE_APK, "Base", "Base apk for the app")
                        .addPart(new SplitPart(entry.getName(), baseSplitMeta.packageName(), entry.getName(), null, true)));

                continue;
            }

            parts.add(new SplitPart(entry.getName(), splitMeta.splitName(), entry.getName(), null, false));

        }

        if (!seenApk)
            throw new RuntimeException("Archive doesn't contain apk files");

        SplitCategory category = new SplitCategory("all", "All", "aaaa").addParts(parts);
        categories.add(category);

        return new SplitApkSourceMeta(new PackageMeta.Builder(packageName)
                .setLabel(packageName)
                .build(), categories, Collections.emptyList());
    }

    private ByteBuffer stealManifestFromApk(InputStream apkInputSteam) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(apkInputSteam)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(MANIFEST_FILE)) {
                    zipInputStream.closeEntry();
                    continue;
                }


                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copyStream(zipInputStream, buffer);
                return ByteBuffer.wrap(buffer.toByteArray());
            }
        }

        throw new IOException("Manifest not found");
    }
}
