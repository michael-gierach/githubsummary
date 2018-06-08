package net.gierach.githubsummary.model;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SyncingStateManager {

    private static class InstanceHolder {
        public static final SyncingStateManager sInstance = new SyncingStateManager();
    }

    public static SyncingStateManager getInstance() {
        return InstanceHolder.sInstance;
    }

    public interface Listener {
        void onSyncingStartedForUsername(String username);
        void onSyncingFinishedForUsername(String username);
    }

    private static final int MSG_SYNCING_STARTED = 1;
    private static final int MSG_SYNCING_FINISHED = 2;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String username = (String)msg.obj;
            int count = mListeners.size();
            switch (msg.what) {
                case MSG_SYNCING_STARTED:
                    for (int i = 0; i < count; ++i) {
                        mListeners.get(i).onSyncingStartedForUsername(username);
                    }
                    break;
                case MSG_SYNCING_FINISHED:
                    for (int i = 0; i < count; ++i) {
                        mListeners.get(i).onSyncingFinishedForUsername(username);
                    }
                    break;

            }
        }
    };

    private final HashSet<String> mSyncingUsernames;
    private final List<Listener> mListeners;


    private SyncingStateManager() {
        mSyncingUsernames = new HashSet<>();
        mListeners = new ArrayList<>();
    }

    public synchronized boolean isUserAccountSyncing(UserAccount userAccount) {
        if (userAccount == null) {
            return false;
        }
        return mSyncingUsernames.contains(userAccount.getUsername());
    }

    public synchronized void syncingStartedForUserAccount(UserAccount userAccount) {
        if (userAccount != null) {
            if (!mSyncingUsernames.contains(userAccount.getUsername())) {
                mSyncingUsernames.add(userAccount.getUsername());

                mHandler.obtainMessage(MSG_SYNCING_STARTED, userAccount.getUsername()).sendToTarget();
            }
        }
    }

    public synchronized void syncingFinishedForUserAccount(UserAccount userAccount) {
        if (userAccount != null) {
            if (mSyncingUsernames.remove(userAccount.getUsername())) {
                mHandler.obtainMessage(MSG_SYNCING_FINISHED, userAccount.getUsername()).sendToTarget();
            }
        }
    }

    private void throwIfNotMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("Must call method on main thread.");
        }
    }

    public void registerListener(Listener listener) {
        throwIfNotMainThread();

        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void unregisterListener(Listener listener) {
        throwIfNotMainThread();

        mListeners.remove(listener);
    }
}
