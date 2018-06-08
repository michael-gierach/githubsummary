package net.gierach.githubsummary.model;

import android.content.ContentValues;

import net.gierach.githubsummary.provider.ReposContract;

public class RepoData {
    public String serverId;
    public String name;
    public String owner;
    public String ownerType;
    public String languagesUrl;
    public boolean onServer;
    public int stargazerCount;
    public boolean isPrivate;
    public String description;

    public ContentValues getContentValues(long userId) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(ReposContract.RepoColumns.USER_ID, userId);
        contentValues.put(ReposContract.RepoColumns.SERVER_ID, serverId);
        contentValues.put(ReposContract.RepoColumns.NAME, name);
        contentValues.put(ReposContract.RepoColumns.OWNER, owner);
        contentValues.put(ReposContract.RepoColumns.OWNER_TYPE, ownerType);
        contentValues.put(ReposContract.RepoColumns.LANGUAGES_URL, languagesUrl);
        contentValues.put(ReposContract.RepoColumns.NEED_LANG_SYNC, true);
        contentValues.put(ReposContract.RepoColumns.ON_SERVER, onServer);
        contentValues.put(ReposContract.RepoColumns.STARGAZER_COUNT, stargazerCount);
        contentValues.put(ReposContract.RepoColumns.IS_PRIVATE, isPrivate);
        contentValues.put(ReposContract.RepoColumns.DESCRIPTION, description);

        return contentValues;
    }
}
