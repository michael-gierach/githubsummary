package net.gierach.githubsummary;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import net.gierach.githubsummary.provider.ReposContract;

import java.text.NumberFormat;

public class RepoDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "RepoDetails";

    private static final String LOADER_KEY_DATA = "DATA";
    private static final int MAIN_LOADER_ID = 1;

    public static Intent createLaunchIntent(Context context, long repoId) {
        Intent intent = new Intent(context, RepoDetailsActivity.class);
        intent.setData(ContentUris.withAppendedId(ReposContract.RepoLanguageView.CONTENT_URI, repoId));
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getData() == null) {
            finish();
        }
        setContentView(R.layout.activity_repo_details);



        Bundle bundle = new Bundle();
        bundle.putParcelable(LOADER_KEY_DATA, getIntent().getData());

        getLoaderManager().initLoader(MAIN_LOADER_ID, bundle, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == MAIN_LOADER_ID) {
            Uri data = args.getParcelable(LOADER_KEY_DATA);

            CursorLoader loader = new CursorLoader(this, data,
                    new String[]{
                            ReposContract.RepoLanguageViewColumns.NAME,
                            ReposContract.RepoLanguageViewColumns.DESCRIPTION,
                            ReposContract.RepoLanguageViewColumns.OWNER,
                            ReposContract.RepoLanguageViewColumns.OWNER_TYPE,
                            ReposContract.RepoLanguageViewColumns.LANGUAGE,
                            ReposContract.RepoLanguageViewColumns.IS_PRIVATE,
                            ReposContract.RepoLanguageViewColumns.STARGAZER_COUNT
                    }, null, null, ReposContract.RepoLanguageViewColumns.LANGUAGE);
            loader.setUpdateThrottle(250);

            return loader;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || !data.moveToFirst())
        {
            finish();
        }

        TextView valueField = findViewById(R.id.detail_name_text);
        valueField.setText(data.getString(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.NAME)));

        valueField = findViewById(R.id.detail_description_text);
        valueField.setText(data.getString(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.DESCRIPTION)));

        valueField = findViewById(R.id.detail_owner_text);
        valueField.setText(data.getString(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.OWNER)));

        valueField = findViewById(R.id.detail_owner_type_text);
        valueField.setText(data.getString(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.OWNER_TYPE)));

        valueField = findViewById(R.id.detail_is_private_text);
        boolean isPrivate = data.getInt(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.IS_PRIVATE)) != 0;
        valueField.setText(isPrivate ? R.string.private_yes : R.string.private_yes);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(true);

        int starCount = data.getInt(data.getColumnIndex(ReposContract.RepoLanguageViewColumns.STARGAZER_COUNT));
        CheckBox stars = findViewById(R.id.detail_star_count);
        stars.setChecked(starCount > 0);
        stars.setText(numberFormat.format(starCount));

        ViewGroup languagesLayout = findViewById(R.id.detail_language_layout);
        languagesLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        int languageColumn = data.getColumnIndex(ReposContract.RepoLanguageViewColumns.LANGUAGE);
        do {
            TextView languageView = (TextView)inflater.inflate(R.layout.language_text_view, null);
            languageView.setText(data.getString(languageColumn));
            languagesLayout.addView(languageView);
        } while (data.moveToNext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
