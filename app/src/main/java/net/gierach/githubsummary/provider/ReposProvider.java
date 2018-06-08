package net.gierach.githubsummary.provider;

import android.content.Context;

import net.gierach.githubsummary.provider.tables.LanguageRepoMapTable;
import net.gierach.githubsummary.provider.tables.LanguagesTable;
import net.gierach.githubsummary.provider.tables.ReposTable;
import net.gierach.githubsummary.provider.tables.UserAccountsTable;
import net.gierach.githubsummary.provider.views.LanguageCountViewHandler;
import net.gierach.githubsummary.provider.views.RepoLanguageViewHandler;
import net.gierach.structured_provider.DatabaseOpenHelper;
import net.gierach.structured_provider.ProviderHandlerDependencyGraph;
import net.gierach.structured_provider.StructuredContentProvider;

public class ReposProvider extends StructuredContentProvider {

    private static final int DB_VERSION = 1;

    private static class ReposDatabaseOpenHelper extends DatabaseOpenHelper {

        public ReposDatabaseOpenHelper(Context context) {
            super(context, "repos.db", null, DB_VERSION);
        }

        @Override
        protected ProviderHandlerDependencyGraph instantiateDependencyGraph() {

            ProviderHandlerDependencyGraph graph = new ProviderHandlerDependencyGraph();

            graph.addTableHandler(ReposTable.getInstance());
            graph.addTableHandler(LanguagesTable.getInstance());
            graph.addTableHandler(LanguageRepoMapTable.getInstance());
            graph.addTableHandler(UserAccountsTable.getInstance());

            graph.addViewHandler(LanguageCountViewHandler.getInstance());
            graph.addViewHandler(RepoLanguageViewHandler.getInstance());

            return graph;
        }
    }

    public ReposProvider() {
        super(ReposContract.AUTHORITY);
    }

    @Override
    protected void registerProviderHandlerPaths() {
        registerPatterns(ReposContract.Repos.PATH, ReposTable.getInstance());
        registerPatterns(ReposContract.Languages.PATH, LanguagesTable.getInstance());
        registerPatterns(ReposContract.LanguageRepoMap.PATH, LanguageRepoMapTable.getInstance());
        registerPatterns(ReposContract.LanguageCountView.PATH, LanguageCountViewHandler.getInstance());
        registerPatterns(ReposContract.RepoLanguageView.PATH, RepoLanguageViewHandler.getInstance());
        registerPatterns(ReposContract.UserAccounts.PATH, UserAccountsTable.getInstance());
    }

    @Override
    protected DatabaseOpenHelper instantiateDatabaseOpenHelper() {
        return new ReposDatabaseOpenHelper(getContext());
    }
}
