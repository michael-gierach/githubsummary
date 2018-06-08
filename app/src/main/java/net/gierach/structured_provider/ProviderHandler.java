
package net.gierach.structured_provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.Set;

public interface ProviderHandler {

    String QUERY_STR_INSERT_OR_UPDATE = "insertOrUpdate";

    String getName();

    String getBaseIdField();

    Cursor handleQuery(ProviderContext providerContext,
                       String[] projection,
                       String selection,
                       String[] selectionArgs,
                       String orderBy,
                       String groupBy,
                       String having,
                       String limit);

    long handleInsert(ProviderContext providerContext, ContentValues values);

    int handleUpdate(ProviderContext providerContext, ContentValues values, String selection, String[] selectionArgs);

    int handleDelete(ProviderContext providerContext, String selection, String[] selectionArgs);

    String getContentType();

    String getEntryContentType();

    Uri getContentUri();

    void commitTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData);

    void failedTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData);

    void buildNotificationSetOnInsert(ProviderContext providerContext, ContentValues insertedValues, Uri newEntryUri, Set<Uri> notificationUris);

    void buildNotificationSetOnUpdateEntryUri(ProviderContext providerContext, ContentValues contentValues, Set<Uri> notificationUris);

    void buildNotificationSetOnUpdateContentUri(
            ProviderContext providerContext,
            ContentValues values,
            String selection,
            String[] selectionArgs,
            Set<Uri> notificationUris);

    void buildNotificationSetOnDeleteEntryUri(ProviderContext context, Uri entryUri, Set<Uri> notificationUris);

    void buildNotificationSetOnDeleteContentUri(ProviderContext context, Uri contentUri, String selection, String[] selectionArgs, Set<Uri> notificationUris);

}
