package net.gierach.structured_provider;

import android.net.Uri;

public class StructuredProviderContract {
    public static final String QUERY_STR_LIMIT = "limit";
    public static final String QUERY_STR_INSERT_OR_UPDATE = "insertOrUpdate";
    public static final String QUERY_STR_FTS_FILTER = FullTextSearchJoiner.QUERY_STR_FTS_FILTER;
    public static final String QUERY_STR_HAVING = "having";

    public interface BaseSearchColumns {

        String _ID = "docid";
    }

    public static Uri makeInsertOrUpdateUri(Uri baseUri) {
        return baseUri.buildUpon().appendQueryParameter(QUERY_STR_INSERT_OR_UPDATE, Boolean.TRUE.toString()).build();
    }
}
