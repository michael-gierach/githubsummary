package net.gierach.githubsummary.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.gierach.githubsummary.provider.ReposContract.UserAccountColumns;

public class UserAccount {
    private Long recordId;
    private final String username;
    private String password;
    private String displayName;
    private long lastUsed;
    private Boolean isValidated;


    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public UserAccount(Context context, Cursor cursor) {
        this.recordId = cursor.getLong(cursor.getColumnIndex(UserAccountColumns._ID));
        this.username = cursor.getString(cursor.getColumnIndex(UserAccountColumns.USERNAME));
        this.password = SecurityHelper.getInstance(context).decryptPassword(this.username, cursor.getBlob(cursor.getColumnIndex(UserAccountColumns.PASSWORD_ENC)));
        this.displayName = cursor.getString(cursor.getColumnIndex(UserAccountColumns.DISPLAY_NAME));
        this.lastUsed = cursor.getLong(cursor.getColumnIndex(UserAccountColumns.LAST_USED));
        int column = cursor.getColumnIndex(UserAccountColumns.IS_VALIDATED);
        if (!cursor.isNull(column)) {
            this.isValidated = cursor.getInt(column) != 0;
        }

    }

    ContentValues getContentValues(Context context) {
        ContentValues values = new ContentValues();
        values.put(UserAccountColumns.USERNAME, username);
        values.put(UserAccountColumns.PASSWORD_ENC, SecurityHelper.getInstance(context).encryptPassword(username, this.password));
        values.put(UserAccountColumns.DISPLAY_NAME, displayName);
        values.put(UserAccountColumns.LAST_USED, lastUsed);
        if (isValidated != null) {
            values.put(UserAccountColumns.IS_VALIDATED, isValidated ? 1 : 0);
        } else {
            values.putNull(UserAccountColumns.IS_VALIDATED);
        }

        return values;
    }

    public Long getRecordId() {
        return this.recordId;
    }

    void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.isValidated = null;
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void resetLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    public Boolean isValidated() {
        return this.isValidated;
    }

    void setValidated(boolean validated) {
        this.isValidated = validated;
    }
}
