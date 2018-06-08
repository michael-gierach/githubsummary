package net.gierach.githubsummary.model;

import android.content.ContentValues;

import net.gierach.githubsummary.provider.ReposContract;

public class LanguageData {
    public String language;
    public int byteCount;

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put(ReposContract.LanguageColumns.LANGUAGE, language);

        return values;
    }
}
