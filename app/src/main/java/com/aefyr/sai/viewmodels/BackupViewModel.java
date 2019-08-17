package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.model.backup.PackageMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BackupViewModel extends AndroidViewModel {

    private Context mContext;

    private BackupRepository mBackupRepo;
    private Observer<List<PackageMeta>> mBackupRepoPackagesObserver;

    private FilterQuery mCurrentFilterQuery = new FilterQuery("", true, true);
    private Filter mFilter = new PackagesFilter();
    private List<PackageMeta> mRawPackages = new ArrayList<>();

    private MutableLiveData<List<PackageMeta>> mPackagesLiveData = new MutableLiveData<>();

    public BackupViewModel(@NonNull Application application) {
        super(application);
        mContext = application;

        mPackagesLiveData.setValue(new ArrayList<>());

        mBackupRepo = BackupRepository.getInstance(mContext);
        mBackupRepoPackagesObserver = (packages) -> filter(mCurrentFilterQuery);
        mBackupRepo.getPackages().observeForever(mBackupRepoPackagesObserver);
    }

    public LiveData<List<PackageMeta>> getPackages() {
        return mPackagesLiveData;
    }

    public void filter(String query, boolean splitsOnly, boolean includeSystemApps) {
        filter(new FilterQuery(query, splitsOnly, includeSystemApps));
    }

    private void filter(FilterQuery filterQuery) {
        mCurrentFilterQuery = filterQuery;
        //TODO probably do something about this cuz concurrency. Tho it should still be fine
        mRawPackages = mBackupRepo.getPackages().getValue();
        mFilter.filter(filterQuery.serializeToString());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        BackupRepository.getInstance(mContext).getPackages().removeObserver(mBackupRepoPackagesObserver);
    }

    private class PackagesFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<PackageMeta> packages = new ArrayList<>(mRawPackages);
            FilterQuery filterQuery = FilterQuery.fromString(constraint.toString());
            String query = filterQuery.query.toLowerCase();

            if (constraint.length() == 0) {
                FilterResults results = new FilterResults();
                results.values = new ArrayList<>(packages);
                results.count = packages.size();
                return results;
            }

            Iterator<PackageMeta> iterator = packages.iterator();
            while (iterator.hasNext()) {
                PackageMeta packageMeta = iterator.next();

                //Apply splitsOnly
                if (filterQuery.splitsOnly && !packageMeta.hasSplits) {
                    iterator.remove();
                    continue;
                }

                //Apply includeSystemApps
                if (!filterQuery.includeSystemApps && packageMeta.isSystemApp) {
                    iterator.remove();
                    continue;
                }

                //Apply query
                //Check if app label matches
                String[] wordsInLabel = packageMeta.label.toLowerCase().split(" ");
                boolean labelMatches = false;
                for (String word : wordsInLabel) {
                    if (word.startsWith(query)) {
                        labelMatches = true;
                        break;
                    }
                }

                //Check if app packages matches
                boolean packagesMatches = packageMeta.packageName.toLowerCase().startsWith(query);

                if (!labelMatches && !packagesMatches)
                    iterator.remove();
            }

            FilterResults results = new FilterResults();
            results.values = new ArrayList<>(packages);
            results.count = packages.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mPackagesLiveData.setValue((List<PackageMeta>) results.values);
        }
    }

    private static class FilterQuery {
        String query;
        boolean splitsOnly;
        boolean includeSystemApps;

        FilterQuery(String query, boolean splitsOnly, boolean includeSystemApps) {
            this.query = query;
            this.splitsOnly = splitsOnly;
            this.includeSystemApps = includeSystemApps;
        }

        static FilterQuery fromString(String serializedFilterQuery) {
            boolean splitsOnly = serializedFilterQuery.charAt(0) == '1';
            boolean includeSystemApps = serializedFilterQuery.charAt(1) == '1';
            String query = serializedFilterQuery.substring(2);
            return new FilterQuery(query, splitsOnly, includeSystemApps);
        }

        String serializeToString() {
            return new StringBuilder().append(splitsOnly ? '1' : '0')
                    .append(includeSystemApps ? '1' : '0')
                    .append(query).toString();
        }
    }
}
