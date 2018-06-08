package net.gierach.githubsummary.provider.tables;

import net.gierach.githubsummary.provider.ReposContract;
import net.gierach.githubsummary.provider.ReposContract.RepoColumns;
import net.gierach.githubsummary.provider.ReposContract.Repos;
import net.gierach.structured_provider.ProviderContext;
import net.gierach.structured_provider.TableBasedProviderHandler;

import java.util.ArrayList;

public class ReposTable extends TableBasedProviderHandler {

    private static class InstanceHolder {
        public static final ReposTable sInstance = new ReposTable();
    }

    public static ReposTable getInstance() {
        return InstanceHolder.sInstance;
    }

    private ReposTable() {
        super(Repos.TABLE_NAME, Repos.CONTENT_TYPE, Repos.ENTRY_CONTENT_TYPE, Repos.CONTENT_URI);
    }

    @Override
    protected TableField[] getTableDefinition() {
        return new TableField[] {
                new AutonumberPrimaryKeyField(1),
                new TextField(RepoColumns.SERVER_ID, 1),
                new IntegerField(RepoColumns.USER_ID, 1),
                new TextField(RepoColumns.OWNER, 1),
                new TextField(RepoColumns.OWNER_TYPE, 1),
                new IntegerField(RepoColumns.STARGAZER_COUNT, 1, 0L),
                new TextField(RepoColumns.NAME, 1),
                new IntegerField(RepoColumns.IS_PRIVATE, 1, 0L),
                new TextField(RepoColumns.DESCRIPTION, 1),
                new TextField(RepoColumns.LANGUAGES_URL, 1),
                new IntegerField(RepoColumns.ON_SERVER, 1, 1L),
                new IntegerField(RepoColumns.NEED_LANG_SYNC, 1, 1L)
        };
    }

    @Override
    public String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds, createIndexCommand(mTableName, "repos_server_id_idx", true, RepoColumns.SERVER_ID));
        }

        return arrayListToArrayOrNull(cmds);
    }

    @Override
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return new String[]{
            RepoColumns.SERVER_ID
        };
    }

    @Override
    public String[] getUpgradeTriggerCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds, createDeleteTrigger(mTableName, "repos_delete_trigger",
                    "DELETE FROM " + ReposContract.LanguageRepoMap.TABLE_NAME + " WHERE " + ReposContract.LanguageRepoMapColumns.REPO_ID + " = OLD." + RepoColumns._ID + ';'
            ));
        }

        return arrayListToArrayOrNull(cmds);
    }
}
