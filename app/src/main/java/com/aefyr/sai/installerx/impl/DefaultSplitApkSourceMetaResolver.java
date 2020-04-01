package com.aefyr.sai.installerx.impl;

import android.text.TextUtils;
import android.util.Log;

import com.aefyr.sai.installerx.SplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.splitmeta.BaseSplitMeta;
import com.aefyr.sai.installerx.splitmeta.SplitMeta;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.model.installerx.SplitApkSourceMeta;
import com.aefyr.sai.model.installerx.SplitCategory;
import com.aefyr.sai.model.installerx.SplitPart;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Stopwatch;
import com.aefyr.sai.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import fr.xgouchet.axml.Attribute;
import fr.xgouchet.axml.CompressedXmlParser;
import fr.xgouchet.axml.CompressedXmlParserListener;

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

            AtomicBoolean seenManifestElement = new AtomicBoolean(false);
            AtomicInteger currentDepth = new AtomicInteger(0);
            HashMap<String, String> manifestAttrs = new HashMap<>();
            new CompressedXmlParser().parse(stealManifestFromApk(zipFile.getInputStream(entry)), new CompressedXmlParserListener() {
                @Override
                public void startDocument() {

                }

                @Override
                public void endDocument() {

                }

                @Override
                public void startPrefixMapping(String prefix, String uri) {

                }

                @Override
                public void endPrefixMapping(String prefix, String uri) {

                }

                @Override
                public void startElement(String uri, String localName, String qName, Attribute[] atts) {
                    currentDepth.incrementAndGet();

                    if (currentDepth.get() == 1 && localName.equals("manifest")) {
                        if (seenManifestElement.get())
                            throw new RuntimeException("Duplicate manifest element found");

                        seenManifestElement.set(true);

                        for (Attribute attr : atts) {
                            if (!TextUtils.isEmpty(attr.getName())) {
                                String value = attr.getValue() == null ? "" : attr.getValue();
                                manifestAttrs.put((!TextUtils.isEmpty(attr.getNamespace()) ? attr.getNamespace() + ":" : "") + attr.getName(), value);
                            }
                        }
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    currentDepth.decrementAndGet();
                }

                @Override
                public void text(String data) {

                }

                @Override
                public void characterData(String data) {

                }

                @Override
                public void processingInstruction(String target, String data) {

                }
            });

            if (!seenManifestElement.get())
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

    private ByteArrayInputStream stealManifestFromApk(InputStream apkInputSteam) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(apkInputSteam)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(MANIFEST_FILE)) {
                    zipInputStream.closeEntry();
                    continue;
                }


                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copyStream(zipInputStream, buffer);
                return new ByteArrayInputStream(buffer.toByteArray());
            }
        }

        throw new IOException("Manifest not found");
    }
}
