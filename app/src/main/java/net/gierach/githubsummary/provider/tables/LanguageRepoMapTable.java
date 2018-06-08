package net.gierach.githubsummary.provider.tables;

import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMap;
import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMapColumns;
import net.gierach.structured_provider.ProviderContext;
import net.gierach.structured_provider.TableBasedProviderHandler;

import java.util.ArrayList;

public class LanguageRepoMapTable extends TableBasedProviderHandler {

    private static class InstanceHolder {
        public static final LanguageRepoMapTable sInstance = new LanguageRepoMapTable();
    }

    public static LanguageRepoMapTable getInstance() {
        return InstanceHolder.sInstance;
    }

    private LanguageRepoMapTable() {
        super(LanguageRepoMap.TABLE_NAME, LanguageRepoMap.CONTENT_TYPE, LanguageRepoMap.ENTRY_CONTENT_TYPE, LanguageRepoMap.CONTENT_URI);
    }

    @Override
    protected TableField[] getTableDefinition() {
        return new TableField[] {
                new AutonumberPrimaryKeyField(1),
                new IntegerField(LanguageRepoMapColumns.REPO_ID, 1),
                new IntegerField(LanguageRepoMapColumns.LANGUAGE_ID, 1),
                new IntegerField(LanguageRepoMapColumns.LANG_BYTES, 1),
                new IntegerField(LanguageRepoMapColumns.ON_SERVER, 1, 1L)
        };
    }

    @Override
    public String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds, createIndexCommand(mTableName, "language_repo_map_repo_id_idx", false, LanguageRepoMapColumns.REPO_ID));
            cmds.add(createIndexCommand(mTableName, "language_repo_map_language_id_idx", false, LanguageRepoMapColumns.LANGUAGE_ID));
        }

        return arrayListToArrayOrNull(cmds);
    }

    @Override
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return new String[] {
                LanguageRepoMapColumns.LANGUAGE_ID,
                LanguageRepoMapColumns.REPO_ID
        };
    }
}
