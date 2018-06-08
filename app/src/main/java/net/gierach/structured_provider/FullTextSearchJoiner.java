
package net.gierach.structured_provider;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.text.TextUtils;

public class FullTextSearchJoiner {

    public static final String QUERY_STR_FTS_FILTER = "ftsfilter";

    private final String mFtsTableName;
    private final String mJoinFrom;
    private final String mJoinField;

    public FullTextSearchJoiner(String ftsTableName, String joinFrom, String joinField) {
        this.mFtsTableName = ftsTableName;
        this.mJoinFrom = joinFrom;
        this.mJoinField = joinField;
    }

    public boolean hasSearchTerm(ProviderContext providerContext) {
        String searchTerm = providerContext.uri.getQueryParameter(QUERY_STR_FTS_FILTER);

        return (!TextUtils.isEmpty(searchTerm));
    }

    public Cursor openSearchCursor(
            ProviderContext providerContext,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String orderBy,
            String groupBy,
            String having,
            String limit) {
        String searchTerm = providerContext.uri.getQueryParameter(QUERY_STR_FTS_FILTER);

        if (TextUtils.isEmpty(searchTerm)) {
            return null;
        }

        searchTerm = createEscapedSearchTerm(searchTerm);

        selection = DatabaseUtils.concatenateWhere(selection, this.mFtsTableName + " MATCH ?");
        selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {searchTerm});

        return providerContext.db.query(this.mJoinFrom
                + " INNER JOIN "
                + this.mFtsTableName
                + " ON "
                + this.mJoinFrom
                + '.'
                + this.mJoinField
                + '='
                + this.mFtsTableName
                + ".docid", projection, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    private String createEscapedSearchTerm(String searchTerm) {
        StringBuilder sb = new StringBuilder(searchTerm);

        for (int i = 0; i < sb.length(); ++i) {
            char temp = sb.charAt(i);
            if (temp == '\'') {
                sb.insert(i++, '\'');
            } else if (temp == '"') {
                sb.deleteCharAt(i--);
            }
        }

        return sb.toString();
    }
}
