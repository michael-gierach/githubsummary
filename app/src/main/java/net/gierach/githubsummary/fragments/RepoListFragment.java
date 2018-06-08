package net.gierach.githubsummary.fragments;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.gierach.githubsummary.R;
import net.gierach.githubsummary.model.SyncingStateManager;
import net.gierach.githubsummary.model.UserAccount;
import net.gierach.githubsummary.model.UserAccountDao;
import net.gierach.githubsummary.provider.ReposContract;
import net.gierach.githubsummary.service.RepoFetchService;

import java.util.concurrent.TimeUnit;

public class RepoListFragment extends Fragment implements
        SyncingStateManager.Listener,
        LoaderManager.LoaderCallbacks<Cursor>,
        UserAccountDao.Listener
{

    private static final String TAG = "RepoListFragment";

    private static final int LOADER_REPO_LIST = 1;

    private static final String SAVE_STATE_AUTO_SYNC_TIMESTAMP = "auto_sync_timestamp";
    private static final long AUTO_SYNC_THRESHOLD = TimeUnit.MINUTES.toMillis(10);

    private ListView mList;
    private View mEmptyListLayout;
    private ProgressBar mEmptyProgress;
    private TextView mEmptyStatus;
    private View mSyncingStatus;

    private RepoListAdapter mListAdapter;

    private UserAccount mUserAccount;

    private long lastAutoSync = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repo_list, null);

        mListAdapter = new RepoListAdapter(getActivity());
        mList = view.findViewById(android.R.id.list);
        mList.setAdapter(mListAdapter);
        mEmptyListLayout = view.findViewById(R.id.emptyListLayout);
        mEmptyProgress = mEmptyListLayout.findViewById(R.id.progressBar);
        mEmptyStatus = mEmptyListLayout.findViewById(R.id.progressText);
        mSyncingStatus = view.findViewById(R.id.repo_list_status);
        mList.setEmptyView(mEmptyListLayout);

        if (savedInstanceState != null) {
            lastAutoSync = savedInstanceState.getLong(SAVE_STATE_AUTO_SYNC_TIMESTAMP, lastAutoSync);
        }

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_REPO_LIST) {

            long userId = 0;
            if (mUserAccount != null && mUserAccount.getRecordId() != null) {
                userId = mUserAccount.getRecordId();
            }
            return new CursorLoader(getActivity(), ReposContract.RepoLanguageView.CONTENT_URI,
                    RepoListAdapter.FIELD_NAMES,
                    ReposContract.RepoLanguageViewColumns.USER_ID + "=?",
                    new String[] {Long.toString(userId)},
                    ReposContract.RepoLanguageViewColumns.REPO_COUNT + " DESC," +
                            ReposContract.RepoLanguageViewColumns.LANGUAGE_ID + ',' +
                            ReposContract.RepoLanguageViewColumns.STARGAZER_COUNT + " DESC"
            );
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mListAdapter != null) {
            mListAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mListAdapter != null) {
            mListAdapter.swapCursor(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.repo_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_refresh:
                if (mUserAccount != null) {
                    RepoFetchService.syncUserRepos(getActivity(), mUserAccount);
                }
                break;
            case R.id.menu_item_sign_out:
                if (mUserAccount != null) {
                    UserAccountDao.getInstance(getActivity()).removeAccount(mUserAccount);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onSyncingStartedForUsername(String username) {
        if (mUserAccount != null && mUserAccount.getUsername().equals(username)) {
            setSyncingState(true);
        }
    }

    @Override
    public void onSyncingFinishedForUsername(String username) {
        if (mUserAccount != null && mUserAccount.getUsername().equals(username)) {
            setSyncingState(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mList = null;
        mEmptyListLayout = null;
        mEmptyProgress = null;
        mEmptyStatus = null;
        mSyncingStatus = null;
        mListAdapter = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        UserAccountDao userAccountDao = UserAccountDao.getInstance(getActivity());
        mUserAccount = userAccountDao.getCurrentAccount();
        userAccountDao.registerListener(this);

        setSyncingState(SyncingStateManager.getInstance().isUserAccountSyncing(mUserAccount));
        SyncingStateManager.getInstance().registerListener(this);

        LoaderManager loaderManager = getLoaderManager();
        if (mUserAccount != null) {
            Loader<Cursor> listLoader = loaderManager.getLoader(LOADER_REPO_LIST);
            if (listLoader != null) {
                loaderManager.restartLoader(LOADER_REPO_LIST, null, this);
            } else {
                loaderManager.initLoader(LOADER_REPO_LIST, null, this);
            }
            long now = System.currentTimeMillis();
            if ((now - lastAutoSync) >= AUTO_SYNC_THRESHOLD) {
                lastAutoSync = now;
                RepoFetchService.syncUserRepos(getActivity(), mUserAccount);
            }
        } else {
            loaderManager.destroyLoader(LOADER_REPO_LIST);
        }


    }

    @Override
    public void onStop() {
        super.onStop();

        UserAccountDao.getInstance(getActivity()).unregisterListener(this);
        getLoaderManager().destroyLoader(LOADER_REPO_LIST);
        SyncingStateManager.getInstance().unregisterListener(this);
    }

    private void setSyncingState(boolean syncing) {
        if (mSyncingStatus != null) {
            mSyncingStatus.setVisibility(syncing ? View.VISIBLE : View.GONE);
            mEmptyProgress.setVisibility(syncing ? View.VISIBLE : View.INVISIBLE);
            mEmptyStatus.setText(syncing ? R.string.syncing_repos : R.string.no_repos);
        }

    }

    @Override
    public void onCurrentAccountChanged(UserAccount userAccount) {
        mUserAccount = userAccount;
        getLoaderManager().restartLoader(LOADER_REPO_LIST, null, this);
    }

    @Override
    public void onAccountInvalidated(UserAccount userAccount) {

    }

    @Override
    public void onAccountValidated(UserAccount userAccount) {

    }
}
