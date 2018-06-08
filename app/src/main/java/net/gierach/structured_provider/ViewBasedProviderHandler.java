
package net.gierach.structured_provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.Set;

public abstract class ViewBasedProviderHandler implements ProviderHandler {

    protected final String mViewName;
    protected final String mContentType;
    protected final String mEntryContentType;
    protected final Uri mContentUri;

    protected ViewBasedProviderHandler(String viewName, String contentType, String entryContentType, Uri contentUri) {
        this.mViewName = viewName;
        this.mContentType = contentType;
        this.mEntryContentType = entryContentType;
        this.mContentUri = contentUri;
    }

    protected FullTextSearchJoiner getFullTextSearchJoiner() {
        return null;
    }

    @Override
    public Cursor handleQuery(
            ProviderContext providerContext,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String orderBy,
            String groupBy,
            String having,
            String limit) {
        FullTextSearchJoiner fullTextSearchJoiner = getFullTextSearchJoiner();

        if (!TextUtils.isEmpty(having) && TextUtils.isEmpty(groupBy)) {
            groupBy = "\'\'";
        }

        if (fullTextSearchJoiner != null && fullTextSearchJoiner.hasSearchTerm(providerContext)) {
            return fullTextSearchJoiner.openSearchCursor(providerContext, projection, selection, selectionArgs, orderBy, groupBy, having, limit);
        }

        return providerContext.db.query(this.mViewName, projection, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    public long handleInsert(ProviderContext providerContext, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int handleUpdate(ProviderContext providerContext, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int handleDelete(ProviderContext providerContext, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return this.mViewName;
    }

    @Override
    public Uri getContentUri() {
        return this.mContentUri;
    }

    @Override
    public String getContentType() {
        return this.mContentType;
    }

    @Override
    public String getEntryContentType() {
        return this.mEntryContentType;
    }

    public String getPrimaryTableName(ProviderContext providerContext) {
        return null;
    }

    @Override
    public void commitTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData) {

    }

    @Override
    public void failedTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData) {

    }

    @Override
    public void buildNotificationSetOnInsert(ProviderContext providerContext, ContentValues insertedValues, Uri newEntryUri, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
    }

    @Override
    public void buildNotificationSetOnUpdateEntryUri(ProviderContext providerContext, ContentValues contentValues, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
    }

    @Override
    public void buildNotificationSetOnUpdateContentUri(
            ProviderContext providerContext,
            ContentValues values,
            String selection,
            String[] selectionArgs,
            Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
    }

    @Override
    public void buildNotificationSetOnDeleteEntryUri(ProviderContext context, Uri entryUri, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
    }

    @Override
    public void buildNotificationSetOnDeleteContentUri(
            ProviderContext context,
            Uri contentUri,
            String selection,
            String[] selectionArgs,
            Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
    }

    @Override
    public String getBaseIdField() {
        return BaseColumns._ID;
    }

    public abstract String[] getDependentTables();

    public abstract String[] getDependentViews();

    public abstract boolean hasChangesOnUpgrade(int oldVersion, int newVersion);

    public abstract String createViewSQL(int version);
}
