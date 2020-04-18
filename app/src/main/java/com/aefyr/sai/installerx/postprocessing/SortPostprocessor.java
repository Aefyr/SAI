package com.aefyr.sai.installerx.postprocessing;

import com.aefyr.sai.installerx.common.MutableSplitCategory;
import com.aefyr.sai.installerx.common.ParserContext;

import java.util.Collections;

public class SortPostprocessor implements Postprocessor {

    @Override
    public void process(ParserContext parserContext) {

        Collections.sort(parserContext.getCategoriesList(), (o1, o2) -> Integer.compare(o1.category().ordinal(), o2.category().ordinal()));

        for (MutableSplitCategory category : parserContext.getCategoriesList()) {
            Collections.sort(category.getPartsList(), (o1, o2) -> o1.name().compareTo(o2.name()));
        }
    }

}
