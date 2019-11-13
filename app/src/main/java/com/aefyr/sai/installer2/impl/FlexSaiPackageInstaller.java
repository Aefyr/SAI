package com.aefyr.sai.installer2.impl;

import android.content.Context;

import com.aefyr.sai.installer2.base.SaiPackageInstaller;
import com.aefyr.sai.installer2.base.SaiPiSessionObserver;
import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.installer2.impl.rootless.RootlessSaiPackageInstaller;
import com.aefyr.sai.installer2.impl.shell.RootedSaiPackageInstaller;
import com.aefyr.sai.installer2.impl.shell.ShizukuSaiPackageInstaller;
import com.aefyr.sai.utils.PreferencesValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FlexSaiPackageInstaller implements SaiPackageInstaller, SaiPiSessionObserver {

    private static FlexSaiPackageInstaller sInstance;

    private Context mContext;

    private SaiPackageInstaller mDefaultInstaller;
    private HashMap<Integer, SaiPackageInstaller> mInstallers = new HashMap<>();
    private ConcurrentHashMap<String, SaiPackageInstaller> mSessionIdToInstaller = new ConcurrentHashMap<>();

    private Set<SaiPiSessionObserver> mObservers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static FlexSaiPackageInstaller getInstance(Context c) {
        synchronized (FlexSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new FlexSaiPackageInstaller(c);
        }
    }

    private FlexSaiPackageInstaller(Context c) {
        mContext = c.getApplicationContext();
        addInstaller(PreferencesValues.INSTALLER_ROOTLESS, RootlessSaiPackageInstaller.getInstance(mContext));
        addInstaller(PreferencesValues.INSTALLER_ROOTED, RootedSaiPackageInstaller.getInstance(mContext));
        addInstaller(PreferencesValues.INSTALLER_SHIZUKU, ShizukuSaiPackageInstaller.getInstance(mContext));
        sInstance = this;
    }

    public void addInstaller(int id, SaiPackageInstaller installer) {
        if (mInstallers.containsKey(id))
            throw new IllegalStateException("Installer with this id already added");

        if (mDefaultInstaller == null)
            mDefaultInstaller = installer;

        mInstallers.put(id, installer);
        installer.registerSessionObserver(this);
    }

    public String createSessionOnInstaller(int installerId, SaiPiSessionParams params) {
        return createSessionOnInstaller(Objects.requireNonNull(mInstallers.get(installerId)), params);
    }

    private String createSessionOnInstaller(SaiPackageInstaller installer, SaiPiSessionParams params) {
        String sessionId = installer.createSession(params);
        mSessionIdToInstaller.put(sessionId, installer);
        return sessionId;
    }

    @Override
    public String createSession(SaiPiSessionParams params) {
        return createSessionOnInstaller(mDefaultInstaller, params);
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPackageInstaller installer = mSessionIdToInstaller.remove(sessionId);
        if (installer == null)
            throw new IllegalArgumentException("Unknown sessionId");

        installer.enqueueSession(sessionId);
    }

    @Override
    public void registerSessionObserver(SaiPiSessionObserver observer) {
        mObservers.add(observer);
    }

    @Override
    public void unregisterSessionObserver(SaiPiSessionObserver observer) {
        mObservers.remove(observer);
    }

    @Override
    public List<SaiPiSessionState> getSessions() {
        ArrayList<SaiPiSessionState> sessions = new ArrayList<>();

        for (SaiPackageInstaller installer : mInstallers.values())
            sessions.addAll(installer.getSessions());

        Collections.sort(sessions);

        return sessions;
    }

    @Override
    public void onSessionStateChanged(SaiPiSessionState state) {
        for (SaiPiSessionObserver observer : mObservers)
            observer.onSessionStateChanged(state);
    }
}
