package net.gierach.githubsummary.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import net.gierach.githubsummary.R;
import net.gierach.githubsummary.RepoDetailsActivity;
import net.gierach.githubsummary.provider.ReposContract;

import java.text.NumberFormat;

public class RepoListAdapter extends CursorAdapter {

    private class ViewHolder {
        final View mHeaderView;
        final TextView mHeaderTitle;
        final TextView mHeaderCount;
        final TextView mTitle;
        final CheckBox mStarCount;

        long recordId;

        public ViewHolder(View view) {
            mHeaderView = view.findViewById(R.id.repo_list_item_header);
            mHeaderTitle = view.findViewById(R.id.repo_header_title);
            mHeaderCount = view.findViewById(R.id.repo_header_count);
            mTitle = view.findViewById(R.id.repo_name);
            mStarCount = view.findViewById(R.id.star_count_check);
            mStarCount.setEnabled(false);

            view.setTag(this);
        }


    }

    public static final String[] FIELD_NAMES = {
            ReposContract.RepoLanguageViewColumns._ID,
            ReposContract.RepoLanguageViewColumns.NAME,
            ReposContract.RepoLanguageViewColumns.LANGUAGE,
            ReposContract.RepoLanguageViewColumns.LANGUAGE_ID,
            ReposContract.RepoLanguageViewColumns.REPO_COUNT,
            ReposContract.RepoLanguageViewColumns.STARGAZER_COUNT
    };

    private final NumberFormat mNumberFormat;

    private final int[] columnIndexes;
    private final Activity mActivity;

    public RepoListAdapter(Activity context) {
        super(context, null, 0);
        this.mActivity = context;
        mNumberFormat = NumberFormat.getIntegerInstance();
        mNumberFormat.setGroupingUsed(true);

        columnIndexes = new int[FIELD_NAMES.length];
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor result = super.swapCursor(newCursor);

        if (newCursor != null) {
            for (int i = 0; i < columnIndexes.length; ++i) {
                columnIndexes[i] = newCursor.getColumnIndexOrThrow(FIELD_NAMES[i]);
            }
        } else {
            for (int i = 0; i < columnIndexes.length; ++i) {
                columnIndexes[i] = -1;
            }
        }

        return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View result = LayoutInflater.from(context).inflate(R.layout.repo_list_item, null);

        final ViewHolder viewHolder = new ViewHolder(result);

        result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = RepoDetailsActivity.createLaunchIntent(mActivity, viewHolder.recordId);

                mActivity.startActivity(intent);
            }
        });

        return result;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        viewHolder.recordId = cursor.getLong(columnIndexes[0]);
        viewHolder.mTitle.setText(cursor.getString(columnIndexes[1]));
        int starCount = cursor.getInt(columnIndexes[5]);
        viewHolder.mStarCount.setChecked(starCount > 0);
        viewHolder.mStarCount.setText(context.getString(R.string.count_format, mNumberFormat.format(starCount)));

        long languageId = cursor.getLong(columnIndexes[3]);
        String language = cursor.getString(columnIndexes[2]);
        int repoCount = cursor.getInt(columnIndexes[4]);
        if (!cursor.moveToPrevious() || cursor.getLong(columnIndexes[3]) != languageId) {
            viewHolder.mHeaderTitle.setText(language);
            viewHolder.mHeaderCount.setText(context.getString(R.string.count_format, mNumberFormat.format(repoCount)));
            viewHolder.mHeaderView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mHeaderView.setVisibility(View.GONE);
        }
    }
}
