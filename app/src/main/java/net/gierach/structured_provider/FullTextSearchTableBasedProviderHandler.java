
package net.gierach.structured_provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;

public abstract class FullTextSearchTableBasedProviderHandler extends TableBasedProviderHandler {

    protected FullTextSearchTableBasedProviderHandler(String tableName, String contentType, String entryContentType, Uri contentUri) {
        super(tableName, contentType, entryContentType, contentUri);
    }

    @Override
    public String[] getUpgradeSQLCommands(int oldVersion, int newVersion) {
        TableField[] fields = getTableDefinition();
        ArrayList<String> commands = null;

        boolean mustChange = false;
        for (TableField field : fields) {
            if (field.fromVersion > oldVersion) {
                mustChange = true;
                break;
            }
        }

        if (mustChange) {
            commands = new ArrayList<>();
            commands.add("DROP TABLE IF EXISTS " + this.mTableName + ";");
            StringBuilder sb = new StringBuilder("CREATE VIRTUAL TABLE ");
            sb.append(this.mTableName).append(" USING fts4(");
            for (int i = 0; i < fields.length; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(fields[i].fieldDefinition());
            }
            sb.append(");");
            commands.add(sb.toString());
        }

        return TableBasedProviderHandler.arrayListToArrayOrNull(commands);
    }

    protected abstract String[] getCommandsToPopulateData(int oldVersion, int newVersion);

    @Override
    public String[] getUpgradeDataCommands(int oldVersion, int newVersion, SQLiteDatabase db) {
        TableField[] fields = getTableDefinition();
        String[] commands = null;

        boolean mustImport = false;
        for (TableField field : fields) {
            if (field.fromVersion > oldVersion) {
                mustImport = true;
                break;
            }
        }

        if (mustImport) {
            commands = getCommandsToPopulateData(oldVersion, newVersion);
        }

        return commands;
    }

    @Override
    public String getBaseIdField() {
        return StructuredProviderContract.BaseSearchColumns._ID;
    }

    @Override
    public final String[] getUpgradeTriggerCommands(int oldVersion, int newVersion) {
        //NOTE: You cannot have any triggers linked to virtual tables in SQLite.
        return null;
    }

    @Override
    public final String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        //NOTE: You cannot have any indexes on a virtual table in SQLite.
        return null;
    }

    @Override
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return new String[] {StructuredProviderContract.BaseSearchColumns._ID};
    }
}
