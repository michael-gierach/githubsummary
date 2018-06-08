package net.gierach.githubsummary.provider.views;

import net.gierach.githubsummary.provider.ReposContract;
import net.gierach.githubsummary.provider.ReposContract.LanguageCountView;
import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMap;
import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMapColumns;
import net.gierach.githubsummary.provider.ReposContract.Languages;
import net.gierach.githubsummary.provider.ReposContract.LanguageColumns;
import net.gierach.githubsummary.provider.ReposContract.Repos;
import net.gierach.githubsummary.provider.ReposContract.RepoColumns;
import net.gierach.structured_provider.ProviderContext;
import net.gierach.structured_provider.StructuredViewBasedProviderHandler;

public class LanguageCountViewHandler extends StructuredViewBasedProviderHandler {

    private static class InstanceHolder {
        public static final LanguageCountViewHandler sInstance = new LanguageCountViewHandler();
    }

    public static LanguageCountViewHandler getInstance() {
        return InstanceHolder.sInstance;
    }

    private LanguageCountViewHandler() {
        super(LanguageCountView.VIEW_NAME, LanguageCountView.CONTENT_TYPE, LanguageCountView.ENTRY_CONTENT_TYPE, LanguageCountView.CONTENT_URI);
    }

    @Override
    protected SelectionField[] getSelectionFields(int version) {
        return new SelectionField[] {
                new SimpleSelectionField(Languages.TABLE_NAME, "*"),
                new SimpleSelectionField(Repos.TABLE_NAME, RepoColumns.USER_ID, ReposContract.LanguageCountViewColumns.USER_ID),
                new SimpleSelectionField(LanguageRepoMap.TABLE_NAME, LanguageRepoMapColumns.REPO_ID, ReposContract.LanguageCountViewColumns.REPO_COUNT, "COUNT", false)
        };
    }

    @Override
    protected String getFromClause(int version) {
        return Languages.TABLE_NAME + " INNER JOIN " + LanguageRepoMap.TABLE_NAME + " ON " + Languages.TABLE_NAME + '.' + LanguageColumns._ID + '=' + LanguageRepoMap.TABLE_NAME + '.' + LanguageRepoMapColumns.LANGUAGE_ID +
                " INNER JOIN " + Repos.TABLE_NAME + " ON " + Repos.TABLE_NAME + '.' + RepoColumns._ID + '=' + LanguageRepoMap.TABLE_NAME + '.' + LanguageRepoMapColumns.REPO_ID;
    }

    @Override
    protected String getGroupByClause(int version) {
        return Languages.TABLE_NAME + '.' + LanguageColumns.LANGUAGE + ',' + Repos.TABLE_NAME + '.' + RepoColumns.USER_ID;
    }

    @Override
    public String getPrimaryTableName(ProviderContext providerContext) {
        return Languages.TABLE_NAME;
    }

    @Override
    public String[] getDependentTables() {
        return new String[] {
                Languages.TABLE_NAME,
                Repos.TABLE_NAME,
                LanguageRepoMap.TABLE_NAME
        };
    }

    @Override
    public String[] getDependentViews() {
        return null;
    }

    @Override
    public boolean hasChangesOnUpgrade(int oldVersion, int newVersion) {
        return oldVersion < 1;
    }
}
