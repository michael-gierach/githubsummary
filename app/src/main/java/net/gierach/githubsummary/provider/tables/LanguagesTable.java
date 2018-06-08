package net.gierach.githubsummary.provider.tables;

import net.gierach.githubsummary.provider.ReposContract.LanguageColumns;
import net.gierach.githubsummary.provider.ReposContract.Languages;
import net.gierach.structured_provider.ProviderContext;
import net.gierach.structured_provider.TableBasedProviderHandler;

import java.util.ArrayList;

public class LanguagesTable extends TableBasedProviderHandler {

    private static class InstanceHolder {
        public static final LanguagesTable sInstance = new LanguagesTable();
    }

    public static LanguagesTable getInstance() {
        return InstanceHolder.sInstance;
    }

    private LanguagesTable() {
        super(Languages.TABLE_NAME, Languages.CONTENT_TYPE, Languages.ENTRY_CONTENT_TYPE, Languages.CONTENT_URI);
    }

    @Override
    protected TableField[] getTableDefinition() {
        return new TableField[] {
                new AutonumberPrimaryKeyField(1),
                new TextField(LanguageColumns.LANGUAGE, 1)
        };
    }

    @Override
    public String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds, createIndexCommand(mTableName, "language_idx", true, LanguageColumns.LANGUAGE));
        }

        return arrayListToArrayOrNull(cmds);
    }

    @Override
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return new String[] {
                LanguageColumns.LANGUAGE
        };
    }
}
