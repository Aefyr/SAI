package com.aefyr.sai.installerx.resolver.urimess;

import android.content.Context;

/**
 * Factory that creates a uri host, implementations must have an empty constructor
 */
public interface UriHostFactory {

    UriHost createUriHost(Context context);

}
