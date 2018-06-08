package net.gierach.githubsummary.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import net.gierach.githubsummary.provider.ReposContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class UserAccountDao {

    private static UserAccountDao sInstance;

    public static synchronized UserAccountDao getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UserAccountDao(context);
        }

        return sInstance;
    }

    private class SaveAccountRunnable implements Runnable {

        private final UserAccount userAccount;

        public SaveAccountRunnable(UserAccount userAccount) {
            this.userAccount = userAccount;
        }

        @Override
        public void run() {
            ContentValues contentValues = userAccount.getContentValues(mContext);

            Uri inserted = mContext.getContentResolver().insert(ReposContract.makeInsertOrUpdateUri(ReposContract.UserAccounts.CONTENT_URI), contentValues);
            if (inserted != null) {
                userAccount.setRecordId(ContentUris.parseId(inserted));
            }
        }
    }

    private static final int MSG_CURRENT_ACCOUNT_CHANGED = 0;
    private static final int MSG_ACCOUNT_INVALIDATED = 1;
    private static final int MSG_ACCOUNT_VALIDATED = 2;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            UserAccount userAccount = (UserAccount)msg.obj;
            if (mListeners.isEmpty()) {
                return;
            }
            int size = mListeners.size();
            switch (msg.what) {
                case MSG_CURRENT_ACCOUNT_CHANGED:
                    for (int i = 0; i < size; ++i) {
                        mListeners.get(i).onCurrentAccountChanged(userAccount);
                    }
                    break;
                case MSG_ACCOUNT_INVALIDATED:
                    for (int i = 0; i < size; ++i) {
                        mListeners.get(i).onAccountInvalidated(userAccount);
                    }
                    break;
                case MSG_ACCOUNT_VALIDATED:
                    for (int i = 0; i < size; ++i) {
                        mListeners.get(i).onAccountValidated(userAccount);
                    }
                    break;
            }
        }
    };

    private final Context mContext;

    private final ReentrantLock mLoadLock = new ReentrantLock();
    private final Condition mLoadedCondition = mLoadLock.newCondition();
    private boolean mLoaded = false;
    private final HashMap<String, UserAccount> mUserAccounts = new HashMap<>();
    private UserAccount mCurrentAccount;

    public interface Listener {
        void onCurrentAccountChanged(UserAccount userAccount);
        void onAccountInvalidated(UserAccount userAccount);
        void onAccountValidated(UserAccount userAccount);
    }
    private final List<Listener> mListeners = new ArrayList<>();

    private UserAccountDao(Context context) {
        this.mContext = context.getApplicationContext();

        Runnable loadRunnable = new Runnable() {
            @Override
            public void run() {
                Cursor cursor = mContext.getContentResolver().query(ReposContract.UserAccounts.CONTENT_URI, null,
                        null, null, ReposContract.UserAccountColumns.LAST_USED);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        UserAccount account = null;
                        do {
                            account = new UserAccount(mContext, cursor);

                            mUserAccounts.put(account.getUsername(), account);
                        } while (cursor.moveToNext());

                        mCurrentAccount = account;
                    }
                    cursor.close();
                }

                mLoadLock.lock();
                try {
                    mLoaded = true;
                    mLoadedCondition.signalAll();
                } finally  {
                    mLoadLock.unlock();
                }
            }
        };

        AsyncTask.SERIAL_EXECUTOR.execute(loadRunnable);
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

    private void waitForLoad() throws InterruptedException {
        while (!mLoaded) {
            mLoadedCondition.await();
        }
    }

    public boolean updateUserAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            if (mUserAccounts.containsKey(userAccount.getUsername())) {
                mUserAccounts.put(userAccount.getUsername(), userAccount);
                saveUserAccount(userAccount);
                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }



    private void saveUserAccount(UserAccount userAccount) {
        AsyncTask.SERIAL_EXECUTOR.execute(new SaveAccountRunnable(userAccount));
    }

    public boolean addUserAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            UserAccount found = mUserAccounts.get(userAccount.getUsername());
            if (found == null) {
                mUserAccounts.put(userAccount.getUsername(), userAccount);
                setCurrentAccountInner(userAccount);

                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }

    public boolean setCurrentAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            UserAccount found = mUserAccounts.get(userAccount.getUsername());
            if (found != null) {
                setCurrentAccountInner(found);
                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }

    //Must hold the lock before calling this method.
    private void setCurrentAccountInner(UserAccount userAccount) {
        if (mCurrentAccount != userAccount) {
            mCurrentAccount = userAccount;
            if (userAccount != null) {
                userAccount.resetLastUsed();
                saveUserAccount(userAccount);
            }
            mMainThreadHandler.obtainMessage(MSG_CURRENT_ACCOUNT_CHANGED, userAccount).sendToTarget();
        }
    }

    public UserAccount getCurrentAccount() {
        mLoadLock.lock();
        try {
            waitForLoad();

            return mCurrentAccount;
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return null;
    }

    public boolean invalidateUserAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            UserAccount found = mUserAccounts.get(userAccount.getUsername());
            if (found != null && (found.isValidated() == null || found.isValidated())) {
                found.setPassword(null);
                found.setValidated(false);
                saveUserAccount(found);
                mMainThreadHandler.obtainMessage(MSG_ACCOUNT_INVALIDATED, found).sendToTarget();
                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }

    public boolean validateUserAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            UserAccount found = mUserAccounts.get(userAccount.getUsername());
            if (found != null && (found.isValidated() == null || !found.isValidated())) {
                found.setValidated(true);
                saveUserAccount(found);
                mMainThreadHandler.obtainMessage(MSG_ACCOUNT_VALIDATED, found).sendToTarget();
                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }

    public boolean removeAccount(UserAccount userAccount) {
        boolean result = false;
        mLoadLock.lock();
        try {
            waitForLoad();

            final UserAccount found = mUserAccounts.remove(userAccount.getUsername());
            if (found != null) {
                Runnable deleteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mContext.getContentResolver().delete(ReposContract.UserAccounts.CONTENT_URI, ReposContract.UserAccountColumns.USERNAME + "=?", new String[]{found.getUsername()});
                    }
                };
                SyncingStateManager.getInstance().syncingFinishedForUserAccount(found);
                if (found == mCurrentAccount) {
                    UserAccount newCurrent = null;
                    long maxTime = Long.MIN_VALUE;
                    for (UserAccount temp : mUserAccounts.values()) {
                        if (temp.getLastUsed() > maxTime) {
                            maxTime = temp.getLastUsed();
                            newCurrent = temp;
                        }
                    }

                    setCurrentAccountInner(newCurrent);
                }
                AsyncTask.SERIAL_EXECUTOR.execute(deleteRunnable);
                result = true;
            }
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }

    public UserAccount getAccountByUsername(String username) {
        UserAccount result = null;

        mLoadLock.lock();
        try {
            waitForLoad();

            result = mUserAccounts.get(username);
        } catch (InterruptedException ie) {

        } finally {
            mLoadLock.unlock();
        }

        return result;
    }
}
