package net.gierach.githubsummary.provider.tables;

import net.gierach.githubsummary.provider.ReposContract;
import net.gierach.githubsummary.provider.ReposContract.UserAccounts;
import net.gierach.structured_provider.ProviderContext;
import net.gierach.structured_provider.TableBasedProviderHandler;

import java.util.ArrayList;

public class UserAccountsTable extends TableBasedProviderHandler {

    private static class InstanceHolder {
        public static final UserAccountsTable sInstance = new UserAccountsTable();
    }

    public static UserAccountsTable getInstance() {
        return InstanceHolder.sInstance;
    }

    private UserAccountsTable() {
        super(UserAccounts.TABLE_NAME, UserAccounts.CONTENT_TYPE, UserAccounts.ENTRY_CONTENT_TYPE, UserAccounts.CONTENT_URI);
    }

    @Override
    protected TableField[] getTableDefinition() {
        return new TableField[] {
                new AutonumberPrimaryKeyField(1),
                new TextField(ReposContract.UserAccountColumns.USERNAME, 1),
                new BinaryField(ReposContract.UserAccountColumns.PASSWORD_ENC, 1),
                new IntegerField(ReposContract.UserAccountColumns.LAST_USED, 1),
                new TextField(ReposContract.UserAccountColumns.DISPLAY_NAME, 1, ""),
                new IntegerField(ReposContract.UserAccountColumns.IS_VALIDATED, 1)
        };
    }

    @Override
    protected String[] getInsertOrUpdateKeyFields(ProviderContext providerContext) {
        return new String[] {ReposContract.UserAccountColumns.USERNAME};
    }

    @Override
    public String[] getUpgradeIndexCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds, createIndexCommand(mTableName, "users_username_idx", true, ReposContract.UserAccountColumns.USERNAME));
        }

        return arrayListToArrayOrNull(cmds);
    }

    @Override
    public String[] getUpgradeTriggerCommands(int oldVersion, int newVersion) {
        ArrayList<String> cmds = null;

        if (oldVersion < 1) {
            cmds = createArrayListAndAdd(cmds,
                    createDeleteTrigger(mTableName, "users_delete_trigger",
                            "DELETE FROM " + ReposContract.Repos.TABLE_NAME +
                                    " WHERE " + ReposContract.Repos.TABLE_NAME + '.' + ReposContract.RepoColumns.USER_ID + "= OLD._id;")
            );

        }

        return arrayListToArrayOrNull(cmds);
    }
}
