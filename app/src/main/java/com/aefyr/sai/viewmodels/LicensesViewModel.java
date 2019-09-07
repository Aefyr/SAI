package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.model.licenses.License;
import com.aefyr.sai.utils.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LicensesViewModel extends AndroidViewModel {

    private MutableLiveData<List<License>> mLicenses = new MutableLiveData<>();
    private MutableLiveData<Boolean> mAreLicensesLoading = new MutableLiveData<>();

    public LicensesViewModel(@NonNull Application application) {
        super(application);

        mLicenses.setValue(Collections.emptyList());
        mAreLicensesLoading.setValue(false);

        loadLicences();
    }

    public LiveData<List<License>> getLicenses() {
        return mLicenses;
    }

    public LiveData<Boolean> getAreLicensesLoading() {
        return mAreLicensesLoading;
    }

    private void loadLicences() {
        if (mAreLicensesLoading.getValue())
            return;

        mAreLicensesLoading.setValue(true);

        new Thread(() -> {
            try {
                AssetManager assetManager = getApplication().getAssets();
                String licensesDir = "licenses";

                String[] rawLicenses = assetManager.list(licensesDir);
                ArrayList<License> licenses = new ArrayList<>(rawLicenses.length);

                for (String rawLicense : rawLicenses)
                    licenses.add(new License(rawLicense, IOUtils.readStream(assetManager.open(licensesDir + "/" + rawLicense), StandardCharsets.UTF_8)));

                Collections.sort(licenses, (license1, license2) -> license1.subject.compareToIgnoreCase(license2.subject));

                mLicenses.postValue(licenses);
                mAreLicensesLoading.postValue(false);
            } catch (Exception e) {
                Log.wtf("Licenses", e);
                mAreLicensesLoading.postValue(false);
            }
        }).start();
    }

}
