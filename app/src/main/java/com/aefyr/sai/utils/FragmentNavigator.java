package com.aefyr.sai.utils;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Provides an easy way to add switching fragments
 */
public class FragmentNavigator {

    private FragmentManager mFragmentManager;
    private int mContainerId;
    private FragmentFactory mFragmentFactory;

    private Fragment mCurrentFragment;

    private boolean mWasRestoreStateCalled = false;

    public FragmentNavigator(FragmentManager fragmentManager, @IdRes int containerId, FragmentFactory fragmentFactory) {
        mFragmentManager = fragmentManager;
        mContainerId = containerId;
        mFragmentFactory = fragmentFactory;
    }

    public FragmentNavigator(@Nullable Bundle savedInstanceState, FragmentManager fragmentManager, @IdRes int containerId, FragmentFactory fragmentFactory) {
        this(fragmentManager, containerId, fragmentFactory);
        restoreState(savedInstanceState);
    }

    public void switchTo(String tag) {
        ensureStateWasRestored();

        FragmentTransaction transaction = mFragmentManager.beginTransaction();

        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }

        Fragment newFragment = mFragmentManager.findFragmentByTag(tag);
        if (newFragment != null) {
            transaction.show(newFragment);
        } else {
            newFragment = mFragmentFactory.createFragment(tag);
            transaction.add(mContainerId, newFragment, tag);
        }

        mCurrentFragment = newFragment;
        transaction.commit();
    }

    public <T extends Fragment> T findFragmentByTag(String tag) {
        ensureStateWasRestored();
        return (T) mFragmentManager.findFragmentByTag(tag);
    }

    /**
     * Write state of this FragmentNavigator to a Bundle, do this in activity/fragment onSaveInstanceState
     *
     * @param bundle
     */
    public void writeStateToBundle(@NonNull Bundle bundle) {
        bundle.putString("fragment_navigator_current_fragment", mCurrentFragment != null ? mCurrentFragment.getTag() : null);
    }

    /**
     * Restore state of a FragmentNavigator from a Bundle, do this in activity/fragment onCreate
     *
     * @param bundle
     */
    public void restoreState(@Nullable Bundle bundle) {
        mWasRestoreStateCalled = true;

        if (bundle == null)
            return;

        String currentFragmentTag = bundle.getString("fragment_navigator_current_fragment", null);
        if (currentFragmentTag != null) {
            mCurrentFragment = mFragmentManager.findFragmentByTag(currentFragmentTag);
            Log.d("beb", "restored current fragment from bundle : " + mCurrentFragment.getTag());
        }

    }

    private void ensureStateWasRestored() {
        if (!mWasRestoreStateCalled)
            throw new IllegalStateException("Please call restoreState before using this FragmentNavigator");
    }

    public interface FragmentFactory {
        Fragment createFragment(String tag);
    }
}
