package com.aefyr.sai.installerx.resolver.impl;

import android.content.Context;
import android.util.Log;

import androidx.annotation.StringRes;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.Category;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.SplitCategory;
import com.aefyr.sai.installerx.SplitCategoryIndex;
import com.aefyr.sai.installerx.SplitPart;
import com.aefyr.sai.installerx.postprocessing.DeviceInfoAwarePostprocessor;
import com.aefyr.sai.installerx.resolver.SplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.splitmeta.BaseSplitMeta;
import com.aefyr.sai.installerx.splitmeta.FeatureSplitMeta;
import com.aefyr.sai.installerx.splitmeta.SplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.AbiConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.LocaleConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.ScreenDestinyConfigSplitMeta;
import com.aefyr.sai.installerx.util.AndroidBinXmlParser;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Stopwatch;
import com.aefyr.sai.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DefaultSplitApkSourceMetaResolver implements SplitApkSourceMetaResolver {
    private static final String TAG = "DSASMetaResolver";

    private static final String MANIFEST_FILE = "AndroidManifest.xml";

    private Context mContext;

    public DefaultSplitApkSourceMetaResolver(Context context) {
        mContext = context.getApplicationContext();
    }

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

        SplitCategoryIndex categoryIndex = new SplitCategoryIndex();

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
                categoryIndex.getOrCreate(Category.BASE_APK, getString(R.string.installerx_category_base_apk), null)
                        .addPart(new SplitPart(splitMeta, entry.getName(), baseSplitMeta.packageName(), null, true, true));

                continue;
            }

            if (splitMeta instanceof FeatureSplitMeta) {
                FeatureSplitMeta featureSplitMeta = (FeatureSplitMeta) splitMeta;

                categoryIndex.getOrCreate(Category.FEATURE, getString(R.string.installerx_category_dynamic_features), null)
                        .addPart(new SplitPart(splitMeta, entry.getName(), getString(R.string.installerx_dynamic_feature, featureSplitMeta.module()), null, false, true));
                continue;
            }

            if (splitMeta instanceof AbiConfigSplitMeta) {
                AbiConfigSplitMeta abiConfigSplitMeta = (AbiConfigSplitMeta) splitMeta;

                String name;
                if (abiConfigSplitMeta.isForModule()) {
                    name = getString(R.string.installerx_split_config_abi_for_module, abiConfigSplitMeta.abi(), abiConfigSplitMeta.module());
                } else {
                    name = getString(R.string.installerx_split_config_abi_for_base, abiConfigSplitMeta.abi());
                }

                categoryIndex.getOrCreate(Category.CONFIG_ABI, getString(R.string.installerx_category_config_abi), null)
                        .addPart(new SplitPart(splitMeta, entry.getName(), name, null, false, false));
                continue;
            }

            if (splitMeta instanceof LocaleConfigSplitMeta) {
                LocaleConfigSplitMeta localeConfigSplitMeta = (LocaleConfigSplitMeta) splitMeta;

                String name;
                if (localeConfigSplitMeta.isForModule()) {
                    name = getString(R.string.installerx_split_config_locale_for_module, localeConfigSplitMeta.locale().getDisplayName(), localeConfigSplitMeta.module());
                } else {
                    name = getString(R.string.installerx_split_config_locale_for_base, localeConfigSplitMeta.locale().getDisplayName());
                }

                categoryIndex.getOrCreate(Category.CONFIG_LOCALE, getString(R.string.installerx_category_config_locale), null)
                        .addPart(new SplitPart(splitMeta, entry.getName(), name, null, false, false));
                continue;
            }

            if (splitMeta instanceof ScreenDestinyConfigSplitMeta) {
                ScreenDestinyConfigSplitMeta screenDestinyConfigSplitMeta = (ScreenDestinyConfigSplitMeta) splitMeta;

                String name;
                if (screenDestinyConfigSplitMeta.isForModule()) {
                    name = getString(R.string.installerx_split_config_dpi_for_module, screenDestinyConfigSplitMeta.densityName(), screenDestinyConfigSplitMeta.density(), screenDestinyConfigSplitMeta.module());
                } else {
                    name = getString(R.string.installerx_split_config_dpi_for_base, screenDestinyConfigSplitMeta.densityName(), screenDestinyConfigSplitMeta.density());
                }

                categoryIndex.getOrCreate(Category.CONFIG_DENSITY, getString(R.string.installerx_category_config_dpi), null)
                        .addPart(new SplitPart(splitMeta, entry.getName(), name, null, false, false));
                continue;
            }

            categoryIndex.getOrCreate(Category.UNKNOWN, getString(R.string.installerx_category_unknown), getString(R.string.installerx_category_unknown_desc))
                    .addPart(new SplitPart(splitMeta, entry.getName(), splitMeta.splitName(), null, false, true));

        }

        if (!seenApk)
            throw new RuntimeException("Archive doesn't contain apk files");

        new DeviceInfoAwarePostprocessor(mContext).process(categoryIndex);

        List<SplitCategory> splitCategoryList = categoryIndex.toList();
        Collections.sort(splitCategoryList, (o1, o2) -> Integer.compare(o1.category().ordinal(), o2.category().ordinal()));

        return new SplitApkSourceMeta(new PackageMeta.Builder(packageName)
                .setLabel(packageName)
                .build(), splitCategoryList, Collections.emptyList());
    }

    private String getString(@StringRes int id) {
        return mContext.getString(id);
    }

    private String getString(@StringRes int id, Object... formatArgs) {
        return mContext.getString(id, formatArgs);
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
