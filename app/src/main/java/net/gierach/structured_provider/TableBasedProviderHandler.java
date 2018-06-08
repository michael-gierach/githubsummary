package net.gierach.structured_provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

public abstract class TableBasedProviderHandler implements ProviderHandler {

    private static final String TAG = "TableBasedProviderHndlr";

    public static String fieldTypeToSqlType(int fieldType) {
        switch (fieldType) {
            case Cursor.FIELD_TYPE_STRING:
                return "TEXT";
            case Cursor.FIELD_TYPE_BLOB:
                return "BLOB";
            case Cursor.FIELD_TYPE_FLOAT:
                return "REAL";
            case Cursor.FIELD_TYPE_INTEGER:
            default:
                return "INTEGER";

        }
    }

    public static String createIndexCommand(String tableName, String indexName, boolean unique, String... fields) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (unique) {
            sb.append("UNIQUE ");
        }
        sb.append("INDEX ");

        sb.append("IF NOT EXISTS ");
        sb.append(indexName).append(" ON ").append(tableName).append(" (");
        boolean first = true;
        for (String field : fields) {
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            sb.append(field);
        }
        sb.append(");");

        return sb.toString();
    }

    public static String createInsertTrigger(String tableName, String triggerName, String... subCommands) {
        StringBuilder sb = new StringBuilder("CREATE TRIGGER IF NOT EXISTS ");
        sb.append(triggerName);
        sb.append(" AFTER INSERT ON ").append(tableName);
        sb.append(" FOR EACH ROW BEGIN ");
        for (String command : subCommands) {
            sb.append(command);
            if (!command.endsWith(";")) {
                sb.append(';');
            }
        }
        sb.append(" END;");

        return sb.toString();
    }

    public static String createDeleteTrigger(String tableName, String triggerName, String... subCommands) {
        StringBuilder sb = new StringBuilder("CREATE TRIGGER IF NOT EXISTS ");
        sb.append(triggerName);
        sb.append(" AFTER DELETE ON ").append(tableName);
        sb.append(" FOR EACH ROW BEGIN ");
        for (String command : subCommands) {
            sb.append(command);
            if (!command.endsWith(";")) {
                sb.append(';');
            }
        }
        sb.append(" END;");

        return sb.toString();
    }

    public static String createUpdateTrigger(String tableName, String[] fieldNames, String triggerName, String... subCommands) {
        StringBuilder sb = new StringBuilder("CREATE TRIGGER IF NOT EXISTS ");
        sb.append(triggerName);
        sb.append(" AFTER UPDATE");
        if (fieldNames != null) {
            sb.append(" OF ");
            for (int i = 0; i < fieldNames.length; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(fieldNames[i]);
            }
        }
        sb.append(" ON ").append(tableName);
        sb.append(" FOR EACH ROW BEGIN ");
        for (String command : subCommands) {
            sb.append(command);
            if (!command.endsWith(";")) {
                sb.append(';');
            }
        }
        sb.append(" END;");

        return sb.toString();
    }

    public static String dropIndexCommand(String indexName) {
        return "DROP INDEX IF EXISTS " + indexName + ';';
    }

    public static String dropTriggerCommand(String triggerName) {
        return "DROP TRIGGER IF EXISTS " + triggerName + ';';
    }

    public abstract static class TableField {

        public final int fromVersion;
        public final String name;
        private final int type;
        private final Object defaultValue;

        public TableField(String name, int type, int fromVersion, Object defaultValue) {
            this.fromVersion = fromVersion;
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public TableField(String name, int type, int fromVersion) {
            this(name, type, fromVersion, null);
        }

        public String fieldDefinition() {
            String defaultStr = "";
            if (this.defaultValue != null) {
                if (this.defaultValue instanceof String) {
                    defaultStr = " DEFAULT \'" + this.defaultValue + '\'';
                } else if (this.defaultValue instanceof byte[]) {
                    defaultStr = "DEFAULT X'" + bytesToHex((byte[])this.defaultValue) + '\'';
                } else {
                    defaultStr = " DEFAULT " + this.defaultValue;
                }
            }
            return this.name + " " + TableBasedProviderHandler.fieldTypeToSqlType(this.type) + defaultStr;
        }

        private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
        private static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }
    }

    public static class AutonumberPrimaryKeyField extends TableField {

        public AutonumberPrimaryKeyField(int fromVersion) {
            super(BaseColumns._ID, Cursor.FIELD_TYPE_INTEGER, fromVersion);
        }

        public AutonumberPrimaryKeyField(String name, int fromVersion) {
            super(name, Cursor.FIELD_TYPE_INTEGER, fromVersion);
        }

        @Override
        public String fieldDefinition() {
            return this.name + " INTEGER PRIMARY KEY AUTOINCREMENT";
        }
    }

    public static class IntegerField extends TableField {

        public IntegerField(String name, int fromVersion, Long defaultValue) {
            super(name, Cursor.FIELD_TYPE_INTEGER, fromVersion, defaultValue);
        }

        public IntegerField(String name, int fromVersion) {
            super(name, Cursor.FIELD_TYPE_INTEGER, fromVersion);
        }
    }

    public static class FloatField extends TableField {

        public FloatField(String name, int fromVersion, Double defaultValue) {
            super(name, Cursor.FIELD_TYPE_FLOAT, fromVersion, defaultValue);
        }

        public FloatField(String name, int fromVersion) {
            super(name, Cursor.FIELD_TYPE_FLOAT, fromVersion);
        }
    }

    public static class TextField extends TableField {

        public TextField(String name, int fromVersion, String defaultValue) {
            super(name, Cursor.FIELD_TYPE_STRING, fromVersion, defaultValue);
        }

        public TextField(String name, int fromVersion) {
            super(name, Cursor.FIELD_TYPE_STRING, fromVersion);
        }
    }

    public static class BinaryField extends TableField {

        public BinaryField(String name, int fromVersion, byte[] defaultValue) {
            super(name, Cursor.FIELD_TYPE_BLOB, fromVersion, defaultValue);
        }

        public BinaryField(String name, int fromVersion) {
            super(name, Cursor.FIELD_TYPE_BLOB, fromVersion);
        }
    }

    protected final String mTableName;
    protected final String mContentType;
    protected final String mEntryContentType;
    protected final Uri mContentUri;
    protected ArrayList<String> mDependentViews = null;

    public TableBasedProviderHandler(String tableName, String contentType, String entryContentType, Uri contentUri) {
        this.mTableName = tableName;
        this.mContentType = contentType;
        this.mEntryContentType = entryContentType;
        this.mContentUri = contentUri;
    }

    @Override
    public String getName() {
        return this.mTableName;
    }

    @Override
    public Uri getContentUri() {
        return this.mContentUri;
    }

    @Override
    public void buildNotificationSetOnInsert(ProviderContext providerContext, ContentValues insertedValues, Uri newEntryUri, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);

        ProviderHandlerDependencyGraph dependencyGraph = providerContext.contentProvider.getDependencyGraph();
        ArrayList<String> dependentViews = getDependentViews(providerContext);
        for (String viewName : dependentViews) {
            ViewBasedProviderHandler view = dependencyGraph.getViewHandler(viewName);
            if (view != null) {
                view.buildNotificationSetOnInsert(providerContext, insertedValues, newEntryUri, notificationUris);
            }
        }
    }

    @Override
    public void buildNotificationSetOnUpdateEntryUri(ProviderContext providerContext, ContentValues contentValues, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
        //notificationUris.add(providerContext.uri);

        ProviderHandlerDependencyGraph dependencyGraph = providerContext.contentProvider.getDependencyGraph();
        ArrayList<String> dependentViews = getDependentViews(providerContext);
        //long updatedId = ContentUris.parseId(providerContext.uri);
        for (String viewName : dependentViews) {
            ViewBasedProviderHandler view = dependencyGraph.getViewHandler(viewName);
            if (view != null) {
                //                if (this.mTableName.equals(view.getPrimaryTableName(providerContext))) {
                //                    notificationUris.add(ContentUris.withAppendedId(view.getContentUri(), updatedId));
                //                }
                view.buildNotificationSetOnUpdateEntryUri(providerContext, contentValues, notificationUris);
            }
        }
    }

    @Override
    public void buildNotificationSetOnUpdateContentUri(ProviderContext providerContext,
                                                       ContentValues values,
                                                       String selection,
                                                       String[] selectionArgs,
                                                       Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);

        ProviderHandlerDependencyGraph dependencyGraph = providerContext.contentProvider.getDependencyGraph();
        ArrayList<String> dependentViews = getDependentViews(providerContext);
        for (String viewName : dependentViews) {
            ViewBasedProviderHandler view = dependencyGraph.getViewHandler(viewName);
            if (view != null) {
                view.buildNotificationSetOnUpdateContentUri(providerContext, values, selection, selectionArgs, notificationUris);
            }
        }

    }

    @Override
    public void buildNotificationSetOnDeleteEntryUri(ProviderContext context, Uri entryUri, Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);
        //notificationUris.add(entryUri);

        ProviderHandlerDependencyGraph dependencyGraph = context.contentProvider.getDependencyGraph();
        ArrayList<String> dependentViews = getDependentViews(context);
        //long deletedId = ContentUris.parseId(entryUri);
        for (String viewName : dependentViews) {
            ViewBasedProviderHandler view = dependencyGraph.getViewHandler(viewName);
            if (view != null) {
                //                if (this.mTableName.equals(view.getPrimaryTableName(context))) {
                //                    notificationUris.add(ContentUris.withAppendedId(view.getContentUri(), deletedId));
                //                }
                view.buildNotificationSetOnDeleteEntryUri(context, entryUri, notificationUris);
            }
        }
    }

    @Override
    public void buildNotificationSetOnDeleteContentUri(ProviderContext context,
                                                       Uri contentUri,
                                                       String selection,
                                                       String[] selectionArgs,
                                                       Set<Uri> notificationUris) {
        notificationUris.add(this.mContentUri);

        ProviderHandlerDependencyGraph dependencyGraph = context.contentProvider.getDependencyGraph();
        ArrayList<String> dependentViews = getDependentViews(context);
        for (String viewName : dependentViews) {
            ViewBasedProviderHandler view = dependencyGraph.getViewHandler(viewName);
            if (view != null) {
                view.buildNotificationSetOnDeleteContentUri(context, contentUri, selection, selectionArgs, notificationUris);
            }
        }
    }

    protected ArrayList<String> getDependentViews(ProviderContext context) {
        if (this.mDependentViews == null) {
            this.mDependentViews = new ArrayList<>();
            context.contentProvider.getDependencyGraph().markTableChanged(getName(), this.mDependentViews);
        }

        return this.mDependentViews;
    }

    protected abstract TableField[] getTableDefinition();

    /**
     * getUpgradeSQLCommands should return a list of SQL commands needed to upgrade this table from the oldVersion
     * to the newVersion. The default implementation makes use of the abstract method getTableDefinition to either
     * create or alter a table. If needed, you may override this method. If there are no upgrade commands you MUST
     * return null. Any table that has an upgrade command will result in dependent views be re-created.
     *
     * @param oldVersion
     * @param newVersion
     * @return If there no upgrade commands, you MUST return null.
     */
    public String[] getUpgradeSQLCommands(int oldVersion, int newVersion) {
        TableField[] fields = getTableDefinition();
        ArrayList<String> commands = new ArrayList<>();

        int minVersion = Integer.MAX_VALUE;
        for (TableField field1 : fields) {
            minVersion = Math.min(minVersion, field1.fromVersion);
        }

        if (oldVersion < minVersion && minVersion <= newVersion) {
            commands.add("DROP TABLE IF EXISTS " + this.mTableName + ";");
            StringBuilder sb = new StringBuilder("CREATE TABLE ");
            sb.append(this.mTableName).append('(');
            for (int i = 0; i < fields.length; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(fields[i].fieldDefinition());
            }
            sb.append(");");
            commands.add(sb.toString());
        } else {
            StringBuilder sb = new StringBuilder();
            for (TableField field : fields) {
                if (field.fromVersion > oldVersion && field.fromVersion <= newVersion) {
                    sb.delete(0, sb.length());
                    sb.append("ALTER TABLE ").append(this.mTableName).append(" ADD COLUMN ");
                    sb.append(field.fieldDefinition()).append(';');
                    commands.add(sb.toString());
                }
            }
        }

        if (commands.size() == 0) {
            return null;
        } else {
            return commands.toArray(new String[commands.size()]);
        }
    }

    /**
     * getUpgradeDataCommands will be called during the upgrade procedure for the entire database.
     * It will be called after all tables have had their table
     * structure update commands executed and after all index and trigger update commands are executed.
     *
     * @param oldVersion
     * @param newVersion
     * @param db
     * @return
     */
    public String[] getUpgradeDataCommands(int oldVersion, int newVersion, SQLiteDatabase db) {
        return null;
    }

    /**
     * getUpgradeIndexCommands will be called during the upgrade procedure for the entire database. It will be called
     * after all tables have had their table structure update commands executed. The implementation of this method should
     * return null if there are no upgrade commands to be executed.
     *
     * @param oldVersion
     * @param newVersion
     * @return
     */
    public String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        return null;
    }

    /**
     * getUpgradTriggerCommands will be called during the upgrade procedure for the entire database. It will be called
     * after all tables have had their table structure update commands executed and after each table has had their
     * getUpgradIndexCommands executed. The implementation of this method should return null if there are no upgrade commands
     * to be executed.
     *
     * @param oldVersion
     * @param newVersion
     * @return
     */
    public String[] getUpgradeTriggerCommands(int oldVersion, int newVersion) {
        return null;
    }

    protected FullTextSearchJoiner getFullTextSearchJoiner() {
        return null;
    }

    @Override
    public Cursor handleQuery(ProviderContext providerContext,
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

        return providerContext.db.query(this.mTableName, projection, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    /**
     * getInsertOrUpdateKeyFields should return a list of fields that act as keys for doing an optional update on a row instead of
     * an insert.
     *
     * @param providerContext
     * @return
     */
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return null;
    }

    @Override
    public long handleInsert(ProviderContext providerContext, ContentValues values) {
        boolean tryUpdate = Boolean.TRUE.toString().equalsIgnoreCase(providerContext.uri.getQueryParameter(QUERY_STR_INSERT_OR_UPDATE));
        if (tryUpdate) {
            String[] insertOrUpdateKeyFields = getInsertOrUpdateKeyFields(providerContext);
            if (insertOrUpdateKeyFields != null && insertOrUpdateKeyFields.length > 0) {
                StringBuilder selection = new StringBuilder();
                ArrayList<String> selectionArgs = new ArrayList<>(insertOrUpdateKeyFields.length);

                boolean canLookup = true;
                for (int i = 0; i < insertOrUpdateKeyFields.length; ++i) {
                    if (values.containsKey(insertOrUpdateKeyFields[i])) {
                        Object value = values.get(insertOrUpdateKeyFields[i]);
                        if (i > 0) {
                            selection.append(" AND ");
                        }
                        selection.append(insertOrUpdateKeyFields[i]);
                        if (value == null) {
                            selection.append(" IS NULL");
                        } else {
                            selection.append("=?");
                            selectionArgs.add(value.toString());
                        }
                    } else {
                        canLookup = false;
                        break;
                    }
                }
                if (canLookup) {
                    Long rowId = null;
                    String idFieldName = getBaseIdField();
                    String[] argArray = null;
                    if (selectionArgs.size() > 0) {
                        argArray = selectionArgs.toArray(new String[selectionArgs.size()]);
                    }
                    Cursor cursor = providerContext.db.query(this.mTableName, new String[]{idFieldName}, selection.toString(), argArray, null, null, null);
                    if (cursor != null) {
                        if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                            rowId = cursor.getLong(0);
                        }

                        cursor.close();
                    }

                    if (rowId != null) {
                        providerContext.db.update(this.mTableName, values, idFieldName + "=?", new String[]{rowId.toString()});
                        ProviderContext updateContext = new ProviderContext(providerContext.contentProvider,
                                                                            providerContext.db,
                                                                            ContentUris.withAppendedId(getContentUri(), rowId),
                                                                            providerContext.transaction,
                                                                            providerContext.notiUris);
                        buildNotificationSetOnUpdateEntryUri(updateContext, values, providerContext.notiUris);

                        return rowId;
                    }
                }
            } else {
                Log.w(TAG, "Insert or Update query parameter is not supported for Uri: " + providerContext.uri);
            }
        }

        return providerContext.db.insert(this.mTableName, null, values);
    }

    @Override
    public int handleUpdate(ProviderContext providerContext, ContentValues values, String selection, String[] selectionArgs) {
        if (values == null || values.size() == 0) {
            Log.w(TAG, "No values when updating " + mTableName + " (values:" + values + ", selection:" + selection + ")");
            return 0;
        }

        return providerContext.db.update(this.mTableName, values, selection, selectionArgs);
    }

    @Override
    public int handleDelete(ProviderContext providerContext, String selection, String[] selectionArgs) {
        return providerContext.db.delete(this.mTableName, selection, selectionArgs);
    }

    @Override
    public String getContentType() {
        return this.mContentType;
    }

    @Override
    public String getEntryContentType() {
        return this.mEntryContentType;
    }

    @Override
    public void commitTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData) {

    }

    @Override
    public void failedTransaction(ContentProvider contentProvider, SQLiteDatabase db, Object transactionData) {

    }

    public static String[] arrayListToArrayOrNull(ArrayList<String> list) {
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.toArray(new String[list.size()]);
        }
    }

    public static ArrayList<String> createArrayListAndAdd(ArrayList<String> list, String str) {
        if (list == null) {
            list = new ArrayList<>();
        }

        list.add(str);

        return list;
    }

    @Override
    public String getBaseIdField() {
        return BaseColumns._ID;
    }
}
