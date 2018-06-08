package net.gierach.structured_provider;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;

public class ProviderTransaction implements SQLiteTransactionListener {

    private static final String TAG = "ProviderTransaction";

    private final ArrayList<Pair<ProviderHandler, Object>> mediaInfoTransactions = new ArrayList<>();
    public final HashSet<Uri> mPendingNotificationUris = new HashSet<>();
    public final SQLiteDatabase mDb;
    private final StructuredContentProvider mContentProvider;

    public ProviderTransaction(StructuredContentProvider contentProvider, SQLiteDatabase db) {
        this.mContentProvider = contentProvider;
        this.mDb = db;
    }

    public void beginTransaction() {
        this.mDb.beginTransactionWithListener(this);
    }

    public void setTransactionSuccessful() {
        this.mDb.setTransactionSuccessful();
    }

    public void endTransaction() {
        this.mDb.endTransaction();
    }

    @Override
    public void onBegin() {
        this.mediaInfoTransactions.clear();
        this.mPendingNotificationUris.clear();
    }

    @Override
    public void onCommit() {
        for (Pair<ProviderHandler, Object> pair : this.mediaInfoTransactions) {
            pair.first.commitTransaction(this.mContentProvider, this.mDb, pair.second);
        }
        ContentResolver cr = this.mContentProvider.getContext().getContentResolver();
        if (StructuredContentProvider.DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (Uri uri : this.mPendingNotificationUris) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(uri.getPath());
            }
            Log.v(TAG, "Sending " + mPendingNotificationUris.size() + " change notification(s): " + sb.toString());
        }
        for (Uri uri : this.mPendingNotificationUris) {
            cr.notifyChange(uri, null);
        }
    }

    @Override
    public void onRollback() {
        for (Pair<ProviderHandler, Object> pair : this.mediaInfoTransactions) {
            pair.first.failedTransaction(this.mContentProvider, this.mDb, pair.second);
        }
    }

    public Object getTransactionData(ProviderHandler providerHandler) {
        for (Pair<ProviderHandler, Object> pair : this.mediaInfoTransactions) {
            if (pair.first.equals(providerHandler)) {
                return pair.second;
            }
        }

        return null;
    }

    public void setTransactionData(ProviderHandler providerHandler, Object transactionData) {
        Pair<ProviderHandler, Object> newPair = new Pair<>(providerHandler, transactionData);
        for (int i = 0; i < this.mediaInfoTransactions.size(); ++i) {
            Pair<ProviderHandler, Object> pair = this.mediaInfoTransactions.get(i);
            if (pair.first.equals(providerHandler)) {
                this.mediaInfoTransactions.set(i, newPair);
                return;
            }
        }

        this.mediaInfoTransactions.add(newPair);
    }

    public void putChangeUri(Uri uri) {
        this.mPendingNotificationUris.add(uri);
    }
}
