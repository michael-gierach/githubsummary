package net.gierach.structured_provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseOpenHelper";

    protected final Context mContext;
    protected final int mDatabaseVersion;

    private ProviderHandlerDependencyGraph mDependencyGraph = null;

    public DatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, name, factory, databaseVersion);

        this.mContext = context;
        this.mDatabaseVersion = databaseVersion;
    }

    protected ProviderHandlerDependencyGraph getDependencyGraph() {
        ProviderHandlerDependencyGraph result = this.mDependencyGraph;

        if (result == null) {

            result = instantiateDependencyGraph();

            //*********************************

            this.mDependencyGraph = result;
        }

        return result;
    }

    protected abstract ProviderHandlerDependencyGraph instantiateDependencyGraph();

    @Override
    public void onCreate(SQLiteDatabase db) {
        ProviderHandlerDependencyGraph graph = getDependencyGraph();
        graph.onCreate(db, mDatabaseVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to version " + newVersion);

        ProviderHandlerDependencyGraph graph = getDependencyGraph();

        try {
            graph.onUpgrade(db, oldVersion, newVersion);

        } catch (SQLiteException e) {
            Log.i(TAG, "Upgrade of database failed! Resetting database...", e);
            graph.resetDatabase(db);
            graph.onCreate(db, newVersion);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Downgrading database from version " + oldVersion + " to version " + newVersion);

        ProviderHandlerDependencyGraph graph = getDependencyGraph();

        graph.resetDatabase(db);
        graph.onCreate(db, newVersion);
    }
}
