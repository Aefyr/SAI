package com.aefyr.sai.installerx.postprocessing;

import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.common.Category;
import com.aefyr.sai.installerx.common.MutableSplitCategory;
import com.aefyr.sai.installerx.common.MutableSplitPart;
import com.aefyr.sai.installerx.common.ParserContext;
import com.aefyr.sai.installerx.resolver.meta.Notice;
import com.aefyr.sai.installerx.splitmeta.config.AbiConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.ConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.LocaleConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.ScreenDestinyConfigSplitMeta;
import com.aefyr.sai.utils.TriConsumer;
import com.aefyr.sai.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceInfoAwarePostprocessor implements Postprocessor {
    private static final String NO_MODULE = "DeviceInfoAwarePostprocessor.NO_MODULE";

    public static final String NOTICE_TYPE_NO_MATCHING_LIBS = "Notice.DeviceInfoAwarePostprocessor.NoMatchingLibs";
    public static final String NOTICE_TYPE_NO_MATCHING_LOCALES = "Notice.DeviceInfoAwarePostprocessor.NoMatchingLocales";

    private Context mContext;

    public DeviceInfoAwarePostprocessor(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void process(ParserContext parserContext) {
        processAbiCategory(parserContext, parserContext.getCategory(Category.CONFIG_ABI));
        processLocaleCategory(parserContext, parserContext.getCategory(Category.CONFIG_LOCALE));
        processScreenDensityCategory(parserContext, parserContext.getCategory(Category.CONFIG_DENSITY));
        processUnknownCategory(parserContext, parserContext.getCategory(Category.UNKNOWN));
    }

    private void scopeToModuleAndProcess(ParserContext parserContext, List<MutableSplitPart> parts, TriConsumer<ParserContext, String, List<MutableSplitPart>> processor) {
        Map<String, List<MutableSplitPart>> moduleToParts = new HashMap<>();

        for (MutableSplitPart part : parts) {
            String module = ((ConfigSplitMeta) part.meta()).module();
            if (module == null)
                module = NO_MODULE;

            List<MutableSplitPart> moduleParts = moduleToParts.get(module);
            if (moduleParts == null) {
                moduleParts = new ArrayList<>();
                moduleToParts.put(module, moduleParts);
            }

            moduleParts.add(part);
        }

        for (Map.Entry<String, List<MutableSplitPart>> entry : moduleToParts.entrySet()) {
            processor.accept(parserContext, entry.getKey(), entry.getValue());
        }
    }

    private void processAbiCategory(ParserContext parserContext, @Nullable MutableSplitCategory abiCategory) {
        if (abiCategory == null)
            return;

        abiCategory.setDescription(mContext.getString(R.string.installerx_category_config_abi_desc, TextUtils.join(", ", Build.SUPPORTED_ABIS)));

        scopeToModuleAndProcess(parserContext, abiCategory.getPartsList(), this::processAbiParts);
    }

    private void processAbiParts(ParserContext parserContext, String module, List<MutableSplitPart> parts) {

        Map<String, Integer> supportedAbisRanking = getSupportedAbisRanking();
        MutableSplitPart bestMatchingPart = null;
        int bestMatchingPartIndex = Integer.MAX_VALUE;

        for (MutableSplitPart part : parts) {
            AbiConfigSplitMeta abiConfigSplitMeta = (AbiConfigSplitMeta) part.meta();
            Integer rank = supportedAbisRanking.get(abiConfigSplitMeta.abi());
            if (rank == null)
                continue;

            if (rank < bestMatchingPartIndex) {
                bestMatchingPart = part;
                bestMatchingPartIndex = rank;
            }
        }

        if (bestMatchingPart != null) {
            if (module.equals(NO_MODULE))
                bestMatchingPart.setRequired(true);
            else
                bestMatchingPart.setRecommended(true);
        } else {
            if (module.equals(NO_MODULE))
                parserContext.addNotice(new Notice(NOTICE_TYPE_NO_MATCHING_LIBS, null, mContext.getString(R.string.installerx_notice_no_code_for_base)));
            else
                parserContext.addNotice(new Notice(NOTICE_TYPE_NO_MATCHING_LIBS, module, mContext.getString(R.string.installerx_notice_no_code_for_feature, module)));

        }
    }

    private Map<String, Integer> getSupportedAbisRanking() {
        HashMap<String, Integer> abisRanking = new HashMap<>();
        for (int i = 0; i < Build.SUPPORTED_ABIS.length; i++) {
            //For some reason _'s are used instead of -'s in split apk abi config parts naming
            abisRanking.put(Build.SUPPORTED_ABIS[i].replace("-", "_"), i);
        }

        return abisRanking;
    }

    private void processLocaleCategory(ParserContext parserContext, @Nullable MutableSplitCategory localeCategory) {
        if (localeCategory == null)
            return;

        localeCategory.setDescription(mContext.getString(R.string.installerx_category_config_locale_desc, mContext.getResources().getConfiguration().locale.getDisplayLanguage()));

        scopeToModuleAndProcess(parserContext, localeCategory.getPartsList(), this::processLocaleParts);
    }

    private void processLocaleParts(ParserContext parserContext, String module, List<MutableSplitPart> parts) {
        Map<String, Integer> langRanking = getPreferredLanguagesRanking();

        MutableSplitPart bestMatchingPart = null;
        int bestMatchingPartRank = Integer.MAX_VALUE;
        for (MutableSplitPart part : parts) {
            LocaleConfigSplitMeta localeConfigSplitMeta = (LocaleConfigSplitMeta) part.meta();

            Locale partLocale = localeConfigSplitMeta.locale();
            Integer rank = langRanking.get(partLocale.getLanguage());
            if (rank != null && rank < bestMatchingPartRank) {
                bestMatchingPart = part;
                bestMatchingPartRank = rank;
            }
        }

        if (bestMatchingPart != null) {
            if (module.equals(NO_MODULE))
                bestMatchingPart.setRequired(true);
            else
                bestMatchingPart.setRecommended(true);
        } else {
            if (module.equals(NO_MODULE))
                parserContext.addNotice(new Notice(NOTICE_TYPE_NO_MATCHING_LOCALES, null, mContext.getString(R.string.installerx_notice_no_locale_for_base)));
            else
                parserContext.addNotice(new Notice(NOTICE_TYPE_NO_MATCHING_LOCALES, null, mContext.getString(R.string.installerx_notice_no_locale_for_feature, module)));
        }
    }

    private Map<String, Integer> getPreferredLanguagesRanking() {
        HashMap<String, Integer> localeRanking = new HashMap<>();
        if (!Utils.apiIsAtLeast(Build.VERSION_CODES.N)) {
            localeRanking.put(mContext.getResources().getConfiguration().locale.getLanguage(), 0);
        } else {
            LocaleList localeList = mContext.getResources().getConfiguration().getLocales();
            for (int i = 0; i < localeList.size(); i++) {
                localeRanking.put(localeList.get(i).getLanguage(), i);
            }

        }
        return localeRanking;
    }

    private void processScreenDensityCategory(ParserContext parserContext, @Nullable MutableSplitCategory dpiCategory) {
        if (dpiCategory == null)
            return;

        dpiCategory.setDescription(mContext.getString(R.string.installerx_category_config_dpi_desc, mContext.getResources().getDisplayMetrics().densityDpi));

        scopeToModuleAndProcess(parserContext, dpiCategory.getPartsList(), this::processScreenDensityParts);
    }

    private void processScreenDensityParts(ParserContext parserContext, String module, List<MutableSplitPart> parts) {

        int deviceDpi = mContext.getResources().getDisplayMetrics().densityDpi;
        MutableSplitPart bestPart = null;
        int bestPartDelta = Integer.MAX_VALUE;

        for (MutableSplitPart part : parts) {
            ScreenDestinyConfigSplitMeta dpiConfigSplitMeta = (ScreenDestinyConfigSplitMeta) part.meta();

            int delta = Math.abs(deviceDpi - dpiConfigSplitMeta.density());
            if (delta < bestPartDelta) {
                bestPart = part;
                bestPartDelta = delta;
            }
        }

        if (bestPart != null) {
            if (module.equals(NO_MODULE))
                bestPart.setRequired(true);
            else
                bestPart.setRecommended(true);
        }
    }

    private void processUnknownCategory(ParserContext parserContext, @Nullable MutableSplitCategory unknownCategory) {
        if (unknownCategory == null)
            return;

        unknownCategory.setDescription(mContext.getString(R.string.installerx_category_unknown_desc));
    }

}
