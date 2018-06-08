
package net.gierach.structured_provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class StructuredContentProvider extends ContentProvider {

    public static boolean DEBUG = true;
    private static final String TAG = "StructuredCntPrvider";

    private UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private int patternCounter = 0;

    private SparseArray<ProviderHandler> PATTERN_TO_HANDLER_MAP = new SparseArray<>();
    private Set<Integer> CONTENT_PATTERNS = new HashSet<>();
    private Set<Integer> ENTRY_PATTERNS = new HashSet<>();

    protected final void registerPatterns(String path, ProviderHandler handler) {
        int contentPattern = patternCounter++;
        int entryPattern = patternCounter++;

        PATTERN_TO_HANDLER_MAP.put(contentPattern, handler);
        PATTERN_TO_HANDLER_MAP.put(entryPattern, handler);

        URI_MATCHER.addURI(mAuthority, path, contentPattern);
        URI_MATCHER.addURI(mAuthority, path + "/#", entryPattern);

        CONTENT_PATTERNS.add(contentPattern);
        ENTRY_PATTERNS.add(entryPattern);
    }

    protected final String mAuthority;

    private DatabaseOpenHelper mOpenHelper;

    private ProviderTransaction mTransaction = null;

    private ReadWriteLock mLock;

    protected StructuredContentProvider(String authority) {
        this.mAuthority = authority;
    }

    @Override
    public boolean onCreate() {

        this.mOpenHelper = instantiateDatabaseOpenHelper();
        this.mLock = new ReentrantReadWriteLock();

        registerProviderHandlerPaths();

        return true;
    }

    protected abstract DatabaseOpenHelper instantiateDatabaseOpenHelper();

    protected abstract void registerProviderHandlerPaths();

    public ProviderHandlerDependencyGraph getDependencyGraph() {
        return mOpenHelper.getDependencyGraph();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int pattern = getPatternOrThrow(uri);
        ProviderHandler handler = getHandler(pattern);

        warnIfOnMainThread();

        if (isEntryPattern(pattern)) {
            selection = DatabaseUtils.concatenateWhere(selection, handler.getBaseIdField() + "=?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
        }
        String limit = uri.getQueryParameter(StructuredProviderContract.QUERY_STR_LIMIT);
        String having = uri.getQueryParameter(StructuredProviderContract.QUERY_STR_HAVING);

        ProviderContext context = new ProviderContext(this, this.mOpenHelper.getReadableDatabase(), uri, null, null);
        Cursor cursor = null;

        this.mLock.readLock().lock();
        try {
            cursor = handler.handleQuery(context, projection, selection, selectionArgs, sortOrder, null, having, limit);
        } finally {
            this.mLock.readLock().unlock();
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), handler.getContentUri());
        }
        return cursor;
    }

    private void warnIfOnMainThread() {
        if (DEBUG) {
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                Log.w(TAG, "Accessing database on main thread: {}", new Exception());
            }
        }
    }

    @Override
    public String getType(Uri uri) {
        int pattern = getPatternOrThrow(uri);
        ProviderHandler handler = getHandler(pattern);
        if (isContentPattern(pattern)) {
            return handler.getContentType();
        } else if (isEntryPattern(pattern)) {
            return handler.getEntryContentType();
        } else {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int pattern = getPatternOrThrow(uri);

        warnIfOnMainThread();

        if (!isContentPattern(pattern)) {
            throw new IllegalArgumentException("Invalid uri " + uri);
        }

        ProviderHandler handler = getHandler(pattern);

        long rowId;
        Uri newEntryUri;
        Set<Uri> notiUris;
        this.mLock.writeLock().lock();
        try {

            SQLiteDatabase db;
            if (this.mTransaction != null) {
                notiUris = this.mTransaction.mPendingNotificationUris;
                db = this.mTransaction.mDb;
            } else {
                notiUris = new HashSet<>();
                db = this.mOpenHelper.getWritableDatabase();
            }

            ProviderContext context = new ProviderContext(this, db, uri, this.mTransaction, notiUris);

            rowId = handler.handleInsert(context, values);

            if (rowId <= 0) {
                throw new RuntimeException("Failed to insert new record into handler: " + handler.getName());
            }

            newEntryUri = ContentUris.withAppendedId(uri, rowId);
            handler.buildNotificationSetOnInsert(context, values, newEntryUri, notiUris);
            if (this.mTransaction != null) {
                notiUris = null;
            }
        } finally {
            this.mLock.writeLock().unlock();
        }

        if (notiUris != null) {
            publishNotis(notiUris);
        }

        return newEntryUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int pattern = getPatternOrThrow(uri);
        boolean isEntry = isEntryPattern(pattern);
        ProviderHandler handler = getHandler(pattern);
        if (isEntry) {
            selection = DatabaseUtils.concatenateWhere(selection, handler.getBaseIdField() + "=?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
        }

        warnIfOnMainThread();

        if (TextUtils.isEmpty(selection)) {
            //pass selection=1 so that the delete function returns a count of the rows deleted
            selection = "1";
        }

        Set<Uri> notiUris = new HashSet<>();
        int numDeleted = 0;
        this.mLock.writeLock().lock();
        try {

            SQLiteDatabase db;
            if (this.mTransaction != null) {
                db = this.mTransaction.mDb;
            } else {
                db = this.mOpenHelper.getWritableDatabase();
            }
            ProviderContext context = new ProviderContext(this, db, uri, this.mTransaction, notiUris);
            if (isEntry) {
                handler.buildNotificationSetOnDeleteEntryUri(context, uri, notiUris);
            } else {
                handler.buildNotificationSetOnDeleteContentUri(context, uri, selection, selectionArgs, notiUris);
            }
            numDeleted = handler.handleDelete(context, selection, selectionArgs);
            if (numDeleted > 0) {
                if (this.mTransaction != null) {
                    this.mTransaction.mPendingNotificationUris.addAll(notiUris);
                    notiUris = null;
                }
            } else {
                notiUris = null;
            }
        } finally {
            this.mLock.writeLock().unlock();
        }

        if (notiUris != null) {
            publishNotis(notiUris);
        }

        return numDeleted;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        boolean hasWrite = false;
        for (int i = 0; i < operations.size(); ++i) {
            if (operations.get(i).isWriteOperation()) {
                hasWrite = true;
                break;
            }
        }

        warnIfOnMainThread();

        ContentProviderResult[] result = new ContentProviderResult[operations.size()];

        final SQLiteDatabase db;
        final Lock lock;
        if (hasWrite) {
            db = this.mOpenHelper.getWritableDatabase();
            lock = this.mLock.writeLock();

        } else {
            db = this.mOpenHelper.getReadableDatabase();
            lock = this.mLock.readLock();
        }
        lock.lock();
        try {
            if (hasWrite) {
                this.mTransaction = new ProviderTransaction(this, db);
                this.mTransaction.beginTransaction();
            }
            int i = 0;
            try {
                for (; i < operations.size(); ++i) {
                    ContentProviderOperation operation = operations.get(i);
                    result[i] = operation.apply(this, result, i);
                }
                if (hasWrite) {
                    this.mTransaction.setTransactionSuccessful();
                }
            } catch (OperationApplicationException oae) {
                ContentProviderOperation failed = operations.get(i);
                Log.e(TAG, "Error applying batch. Failed : " + failed, oae);
                throw oae;
            } finally {
                if (hasWrite) {
                    this.mTransaction.endTransaction();
                    this.mTransaction = null;
                }
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int pattern = getPatternOrThrow(uri);

        if (values == null || values.length == 0) {
            return 0;
        }

        if (!isContentPattern(pattern)) {
            throw new IllegalArgumentException("Invalid uri " + uri);
        }

        warnIfOnMainThread();

        ProviderHandler handler = getHandler(pattern);

        int numInserted = 0;

        this.mLock.writeLock().lock();
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            ProviderTransaction transaction = new ProviderTransaction(this, db);
            ProviderContext context = new ProviderContext(this, transaction.mDb, uri, transaction, transaction.mPendingNotificationUris);
            transaction.beginTransaction();
            try {
                for (ContentValues value : values) {
                    long rowId = handler.handleInsert(context, value);
                    if (rowId <= 0) {
                        throw new RuntimeException("Failed to insert new record into handler: " + handler.getName());
                    }

                    Uri newEntryUri = ContentUris.withAppendedId(uri, rowId);
                    handler.buildNotificationSetOnInsert(context, value, newEntryUri, transaction.mPendingNotificationUris);
                }
                transaction.setTransactionSuccessful();
            } finally {
                transaction.endTransaction();
            }
        } finally {
            this.mLock.writeLock().unlock();
        }

        return numInserted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int pattern = getPatternOrThrow(uri);

        ProviderHandler handler = getHandler(pattern);

        warnIfOnMainThread();

        boolean isEntry = isEntryPattern(pattern);
        if (isEntry) {
            selection = DatabaseUtils.concatenateWhere(selection, handler.getBaseIdField() + "=?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
        }

        int numUpdated = 0;
        Set<Uri> notiUris = new HashSet<>();
        this.mLock.writeLock().lock();
        try {

            SQLiteDatabase db;
            if (this.mTransaction != null) {
                db = this.mTransaction.mDb;
            } else {
                db = this.mOpenHelper.getWritableDatabase();
            }

            ProviderContext context = new ProviderContext(this, db, uri, this.mTransaction, notiUris);

            if (isEntry) {
                handler.buildNotificationSetOnUpdateEntryUri(context, values, notiUris);
            } else {
                handler.buildNotificationSetOnUpdateContentUri(context, values, selection, selectionArgs, notiUris);
            }
            numUpdated = handler.handleUpdate(context, values, selection, selectionArgs);

            if (numUpdated > 0) {
                if (this.mTransaction != null) {
                    this.mTransaction.mPendingNotificationUris.addAll(notiUris);
                    notiUris = null;
                }
            } else {
                notiUris = null;
            }
        } finally {
            this.mLock.writeLock().unlock();
        }

        if (notiUris != null) {
            publishNotis(notiUris);
        }

        return numUpdated;

    }

    private void publishNotis(Set<Uri> notiUris) {
        ContentResolver contentResolver = getContext().getContentResolver();
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (Uri notiUri : notiUris) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(notiUri.getPath());
            }
            Log.v(TAG, "Notifying " + notiUris.size() + " change notification(s): " + sb.toString());
        }
        for (Uri notiUri : notiUris) {
            contentResolver.notifyChange(notiUri, null);
        }
    }

    private int getPatternOrThrow(Uri uri) {
        int pattern = URI_MATCHER.match(uri);
        if (pattern == -1) {
            throw new IllegalArgumentException("Invalid uri " + uri);
        }
        return pattern;
    }

    private ProviderHandler getHandler(int pattern) {
        ProviderHandler handler = PATTERN_TO_HANDLER_MAP.get(pattern);
        if (handler == null) {
            throw new IllegalArgumentException("No table mapping found for pattern " + pattern);
        }
        return handler;
    }

    private boolean isContentPattern(int pattern) {
        return CONTENT_PATTERNS.contains(pattern);
    }

    private boolean isEntryPattern(int pattern) {
        return ENTRY_PATTERNS.contains(pattern);
    }
}
