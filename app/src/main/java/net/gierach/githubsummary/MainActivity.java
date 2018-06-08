package net.gierach.githubsummary;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.gierach.githubsummary.fragments.RepoListFragment;
import net.gierach.githubsummary.fragments.SignInFragment;
import net.gierach.githubsummary.model.UserAccount;
import net.gierach.githubsummary.model.UserAccountDao;

public class MainActivity extends AppCompatActivity implements UserAccountDao.Listener {

    private static final String TAG_FRAG_SIGN_IN = "SignInFragment";
    private static final String TAG_FRAG_REPO_LIST = "RepoListFragment";

    private static final String SAVE_STATE_MODE = "mode";

    private static final int MODE_NONE = 0;
    private static final int MODE_SIGN_IN = 1;
    private static final int MODE_REPO_LIST = 2;

    private int mMode = MODE_NONE;
    private boolean canPerformFragmentTransactions = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UserAccountDao.getInstance(this).registerListener(this);

        canPerformFragmentTransactions = true;
        if (savedInstanceState != null) {
            mMode = savedInstanceState.getInt(SAVE_STATE_MODE, MODE_NONE);
        }
        checkMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        UserAccountDao.getInstance(this).unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        canPerformFragmentTransactions = true;
        checkMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        canPerformFragmentTransactions = true;
        checkMode();
    }

    private void checkMode() {
        UserAccountDao userAccountDao = UserAccountDao.getInstance(this);
        UserAccount currentAccount = userAccountDao.getCurrentAccount();
        if (currentAccount == null || !currentAccount.isValidated()) {
            showSignInFragment();
        } else {
            showRepoListFragment();
        }
    }

    private void showSignInFragment() {
        if (mMode != MODE_SIGN_IN && canPerformFragmentTransactions) {
            mMode = MODE_SIGN_IN;
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            Fragment remove = fragmentManager.findFragmentByTag(TAG_FRAG_REPO_LIST);
            if (remove != null) {
                transaction.remove(remove);
            }
            transaction.add(R.id.content, Fragment.instantiate(this, SignInFragment.class.getName()), TAG_FRAG_SIGN_IN);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
        }
    }

    private void showRepoListFragment() {
        if (mMode != MODE_REPO_LIST && canPerformFragmentTransactions) {
            mMode = MODE_REPO_LIST;
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            Fragment remove = fragmentManager.findFragmentByTag(TAG_FRAG_SIGN_IN);
            if (remove != null) {
                transaction.remove(remove);
            }
            transaction.add(R.id.content, Fragment.instantiate(this, RepoListFragment.class.getName()), TAG_FRAG_REPO_LIST);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            transaction.commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        canPerformFragmentTransactions = false;
        outState.putInt(SAVE_STATE_MODE, mMode);
    }

    @Override
    public void onCurrentAccountChanged(UserAccount userAccount) {

    }

    @Override
    public void onAccountInvalidated(UserAccount userAccount) {
        UserAccount currentAccount = UserAccountDao.getInstance(this).getCurrentAccount();
        if (userAccount == currentAccount || currentAccount == null) {
            showSignInFragment();
        }
    }

    @Override
    public void onAccountValidated(UserAccount userAccount) {
        if (userAccount == UserAccountDao.getInstance(this).getCurrentAccount()) {
            showRepoListFragment();
        }
    }
}
