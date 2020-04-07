package com.aefyr.sai.installerx.postprocessing;

import com.aefyr.sai.installerx.ParserContext;
import com.aefyr.sai.installerx.SplitCategory;

import java.util.Collections;

public class SortPostprocessor implements Postprocessor {

    @Override
    public void process(ParserContext parserContext) {

        Collections.sort(parserContext.getCategoriesList(), (o1, o2) -> Integer.compare(o1.category().ordinal(), o2.category().ordinal()));

        for (SplitCategory category : parserContext.getCategoriesList()) {
            Collections.sort(category.parts(), (o1, o2) -> o1.name().compareTo(o2.name()));
        }
    }

}
