package net.gierach.githubsummary.provider.views;

import net.gierach.githubsummary.provider.ReposContract.RepoLanguageViewColumns;
import net.gierach.githubsummary.provider.ReposContract.LanguageCountView;
import net.gierach.githubsummary.provider.ReposContract.LanguageCountViewColumns;
import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMap;
import net.gierach.githubsummary.provider.ReposContract.LanguageRepoMapColumns;
import net.gierach.githubsummary.provider.ReposContract.RepoColumns;
import net.gierach.githubsummary.provider.ReposContract.RepoLanguageView;
import net.gierach.githubsummary.provider.ReposContract.Repos;
import net.gierach.structured_provider.StructuredViewBasedProviderHandler;

public class RepoLanguageViewHandler extends StructuredViewBasedProviderHandler {

    private static class InstanceHolder {
        public static final RepoLanguageViewHandler sInstance = new RepoLanguageViewHandler();
    }

    public static RepoLanguageViewHandler getInstance() {
        return InstanceHolder.sInstance;
    }

    private RepoLanguageViewHandler() {
        super(RepoLanguageView.VIEW_NAME, RepoLanguageView.CONTENT_TYPE, RepoLanguageView.ENTRY_CONTENT_TYPE, RepoLanguageView.CONTENT_URI);
    }

    @Override
    protected SelectionField[] getSelectionFields(int version) {
        return new SelectionField[] {
                new SimpleSelectionField(Repos.TABLE_NAME, "*"),
                new SimpleSelectionField(LanguageCountView.VIEW_NAME, LanguageCountViewColumns._ID, RepoLanguageViewColumns.LANGUAGE_ID),
                new SimpleSelectionField(LanguageCountView.VIEW_NAME, LanguageCountViewColumns.LANGUAGE),
                new SimpleSelectionField(LanguageCountView.VIEW_NAME, LanguageCountViewColumns.REPO_COUNT)
        };
    }

    @Override
    protected String getFromClause(int version) {
        return Repos.TABLE_NAME + " INNER JOIN " + LanguageRepoMap.TABLE_NAME + " ON " + Repos.TABLE_NAME + '.' + RepoColumns._ID + '=' + LanguageRepoMap.TABLE_NAME + '.' + LanguageRepoMapColumns.REPO_ID +
                " INNER JOIN " + LanguageCountView.VIEW_NAME + " ON " + LanguageRepoMap.TABLE_NAME + '.' + LanguageRepoMapColumns.LANGUAGE_ID + '=' + LanguageCountView.VIEW_NAME + '.' + LanguageCountViewColumns._ID;
    }

    @Override
    public String[] getDependentTables() {
        return new String[] {
                Repos.TABLE_NAME,
                LanguageRepoMap.TABLE_NAME
        };
    }

    @Override
    public String[] getDependentViews() {
        return new String[] {
                LanguageCountView.VIEW_NAME
        };
    }

    @Override
    public boolean hasChangesOnUpgrade(int oldVersion, int newVersion) {
        return oldVersion < 1;
    }
}
