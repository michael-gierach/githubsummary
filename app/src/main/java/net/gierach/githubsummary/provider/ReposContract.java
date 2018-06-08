package net.gierach.githubsummary.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import net.gierach.structured_provider.StructuredProviderContract;

public class ReposContract extends StructuredProviderContract {

    public static final String AUTHORITY = "net.gierach.githubsummary";

    /**
     * Builds a content type String
     */
    private static String buildContentType(String path) {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.githubsummary." + path;
    }

    /**
     * Builds an entry content type String
     */
    private static String buildEntryContentType(String path) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.githubsummary." + path;
    }

    /**
     * Builds a content uri
     *
     * @param path the path of the content
     * @return the content uri
     */
    private static Uri buildContentUri(String path) {
        return Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + path);
    }

    public interface UserAccountColumns extends BaseColumns {
        String USERNAME = "username";
        String PASSWORD_ENC = "password_enc";
        String DISPLAY_NAME = "display_name";
        String LAST_USED = "last_used";
        String IS_VALIDATED = "is_validated";
    }

    public static class UserAccounts {
        public static final String TABLE_NAME = "users";
        public static final String PATH = "users";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }

    public interface LanguageColumns extends BaseColumns {
        String LANGUAGE = "language";
    }

    public static class Languages {
        public static final String TABLE_NAME = "languages";
        public static final String PATH = "languages";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }

    public interface RepoColumns extends BaseColumns {
        String SERVER_ID = "server_id";
        String USER_ID = "user_id";
        String OWNER = "owner";
        String OWNER_TYPE = "owner_type";
        String STARGAZER_COUNT = "stargazer_count";
        String NAME = "name";
        String IS_PRIVATE = "is_private";
        String DESCRIPTION = "description";
        String LANGUAGES_URL = "languages_url";
        String ON_SERVER = "on_server";
        String NEED_LANG_SYNC = "need_lang_sync";
    }

    public static class Repos {
        public static final String TABLE_NAME = "repos";
        public static final String PATH = "repos";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }

    public interface LanguageRepoMapColumns extends BaseColumns {
        String LANGUAGE_ID = "language_id";
        String REPO_ID = "repo_id";
        String LANG_BYTES = "lang_bytes";
        String ON_SERVER = "on_server";
    }

    public static class LanguageRepoMap {
        public static final String TABLE_NAME = "language_repo_map";
        public static final String PATH = "language_repo_map";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }

    public interface LanguageCountViewColumns extends LanguageColumns {
        String USER_ID = "user_id";
        String REPO_COUNT = "repo_count";
    }

    public static class LanguageCountView {
        public static final String VIEW_NAME = "language_count";
        public static final String PATH = "language_count";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }

    public interface RepoLanguageViewColumns extends RepoColumns {
        String LANGUAGE_ID = "language_id";
        String LANGUAGE = "language";
        String REPO_COUNT = "repo_count";
    }

    public static class RepoLanguageView {
        public static final String VIEW_NAME = "repo_language_view";
        public static final String PATH = "repo_language_view";
        public static final String CONTENT_TYPE = buildContentType(PATH);
        public static final String ENTRY_CONTENT_TYPE = buildEntryContentType(PATH);
        public static final Uri CONTENT_URI = buildContentUri(PATH);
    }
}
