package com.aefyr.sai.installerx.postprocessing;

import android.content.Context;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.common.MutableSplitCategory;
import com.aefyr.sai.installerx.common.MutableSplitPart;
import com.aefyr.sai.installerx.common.ParserContext;
import com.aefyr.sai.installerx.resolver.meta.Notice;

public class HugeAppWarningPostprocessor implements Postprocessor {

    private static final String NOTICE_TYPE_HUGE_APP = "Notice.HugeAppWarningPostprocessor.HugeApp";
    private static final long WARNING_THRESHOLD_SIZE_BYTES = 150 * 1000 * 1000; //150MB

    private Context mContext;

    public HugeAppWarningPostprocessor(Context context) {
        mContext = context;
    }

    @Override
    public void process(ParserContext parserContext) {
        long allApksSize = 0;

        for (MutableSplitCategory category : parserContext.getCategoriesList()) {
            for (MutableSplitPart splitPart : category.getPartsList()) {
                allApksSize += splitPart.size();
            }
        }

        if (allApksSize > WARNING_THRESHOLD_SIZE_BYTES) {
            parserContext.addNotice(new Notice(NOTICE_TYPE_HUGE_APP, null, mContext.getString(R.string.installerx_notice_huge_app)));
        }
    }
}
