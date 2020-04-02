package com.aefyr.sai.installerx.postprocessing;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.Category;
import com.aefyr.sai.installerx.SplitCategory;
import com.aefyr.sai.installerx.SplitCategoryIndex;
import com.aefyr.sai.installerx.SplitPart;
import com.aefyr.sai.installerx.splitmeta.config.AbiConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.ConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.LocaleConfigSplitMeta;
import com.aefyr.sai.installerx.splitmeta.config.ScreenDestinyConfigSplitMeta;
import com.aefyr.sai.utils.BiConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeviceInfoAwarePostprocessor implements SplitCategoryIndexPostprocessor {
    private static final String NO_MODULE = "DeviceInfoAwarePostprocessor.NO_MODULE";

    private Context mContext;

    public DeviceInfoAwarePostprocessor(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void process(SplitCategoryIndex categoryIndex) {
        processAbiCategory(categoryIndex.get(Category.CONFIG_ABI));
        processLocaleCategory(categoryIndex.get(Category.CONFIG_LOCALE));
        processScreenDensityCategory(categoryIndex.get(Category.CONFIG_DENSITY));
    }

    private void scopeToModuleAndProcess(List<SplitPart> parts, BiConsumer<String, List<SplitPart>> processor) {
        Map<String, List<SplitPart>> moduleToParts = new HashMap<>();

        for (SplitPart part : parts) {
            String module = ((ConfigSplitMeta) part.meta()).module();
            if (module == null)
                module = NO_MODULE;

            List<SplitPart> moduleParts = moduleToParts.get(module);
            if (moduleParts == null) {
                moduleParts = new ArrayList<>();
                moduleToParts.put(module, moduleParts);
            }

            moduleParts.add(part);
        }

        for (Map.Entry<String, List<SplitPart>> entry : moduleToParts.entrySet()) {
            processor.accept(entry.getKey(), entry.getValue());
        }
    }

    private void processAbiCategory(@Nullable SplitCategory abiCategory) {
        if (abiCategory == null)
            return;

        abiCategory.setDescription(mContext.getString(R.string.installerx_category_config_abi_desc, TextUtils.join(", ", Build.SUPPORTED_ABIS)));

        scopeToModuleAndProcess(abiCategory.parts(), this::processAbiParts);
    }

    private void processAbiParts(String module, List<SplitPart> parts) {

        Map<String, Integer> supportedAbisRanking = getSupportedAbisRanking();
        SplitPart bestMatchingPart = null;
        int bestMatchingPartIndex = Integer.MAX_VALUE;

        for (SplitPart part : parts) {
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

    private void processLocaleCategory(@Nullable SplitCategory localeCategory) {
        if (localeCategory == null)
            return;

        scopeToModuleAndProcess(localeCategory.parts(), this::processLocaleParts);
    }

    private void processLocaleParts(String module, List<SplitPart> parts) {
        SplitPart bestMatchingPart = null;
        for (SplitPart part : parts) {
            LocaleConfigSplitMeta localeConfigSplitMeta = (LocaleConfigSplitMeta) part.meta();

            if (localeConfigSplitMeta.locale().getLanguage().equals(Locale.getDefault().getLanguage())) {
                bestMatchingPart = part;
            }

            if (localeConfigSplitMeta.locale().equals(Locale.getDefault())) {
                bestMatchingPart = part;
                break; //Exact match
            }
        }

        if (bestMatchingPart != null)
            bestMatchingPart.setRecommended(true);
    }

    private void processScreenDensityCategory(@Nullable SplitCategory dpiCategory) {
        if (dpiCategory == null)
            return;

        dpiCategory.setDescription(mContext.getString(R.string.installerx_category_config_dpi_desc, mContext.getResources().getDisplayMetrics().densityDpi));

        scopeToModuleAndProcess(dpiCategory.parts(), this::processScreenDensityParts);
    }

    private void processScreenDensityParts(String module, List<SplitPart> parts) {

        int deviceDpi = mContext.getResources().getDisplayMetrics().densityDpi;
        SplitPart bestPart = null;
        int bestPartDelta = Integer.MAX_VALUE;

        for (SplitPart part : parts) {
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

}
