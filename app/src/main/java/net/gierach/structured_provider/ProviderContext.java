
package net.gierach.structured_provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.Set;

public class ProviderContext {

    public final SQLiteDatabase db;
    public final StructuredContentProvider contentProvider;
    public final Uri uri;
    public final ProviderTransaction transaction;
    public final Set<Uri> notiUris;

    public ProviderContext(StructuredContentProvider contentProvider, SQLiteDatabase db, Uri uri, ProviderTransaction transaction, Set<Uri> notiUris) {
        this.contentProvider = contentProvider;
        this.uri = uri;
        this.db = db;
        this.transaction = transaction;
        this.notiUris = notiUris;
    }
}
