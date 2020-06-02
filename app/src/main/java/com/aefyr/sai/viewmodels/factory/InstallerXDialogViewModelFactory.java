package com.aefyr.sai.viewmodels.factory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.installerx.resolver.urimess.UriHostFactory;

public class InstallerXDialogViewModelFactory implements ViewModelProvider.Factory {

    private Context mAppContext;
    private UriHostFactory mUriHostFactory;

    public InstallerXDialogViewModelFactory(Context context, @Nullable UriHostFactory uriHostFactory) {
        mAppContext = context.getApplicationContext();
        mUriHostFactory = uriHostFactory;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        UriHost uriHost = null;
        if (mUriHostFactory != null)
            uriHost = mUriHostFactory.createUriHost(mAppContext);

        try {
            return modelClass.getConstructor(Context.class, UriHost.class).newInstance(mAppContext, uriHost);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
