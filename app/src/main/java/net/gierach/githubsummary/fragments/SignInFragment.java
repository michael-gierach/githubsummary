package net.gierach.githubsummary.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.gierach.githubsummary.R;
import net.gierach.githubsummary.model.UserAccount;
import net.gierach.githubsummary.model.UserAccountDao;
import net.gierach.githubsummary.service.RepoFetchService;

public class SignInFragment extends Fragment implements UserAccountDao.Listener {
    private static final String SAVE_STATE_USERNAME = "SAVE_STATE_USERNAME";

    private EditText mUserNameText;
    private EditText mPasswordText;
    private Button mSignInButton;
    private View mProgressLayout;
    private UserAccount mValidatingAccount = null;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_sign_in, null);

        mUserNameText = result.findViewById(R.id.edit_text_username);
        mPasswordText = result.findViewById(R.id.edit_text_password);
        mSignInButton = result.findViewById(R.id.sign_in_button);
        mProgressLayout = result.findViewById(R.id.progressLayout);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignInButtonClicked();
            }
        });

        mUserNameText.setImeActionLabel(getString(R.string.next), EditorInfo.IME_ACTION_NEXT);
        mUserNameText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (!TextUtils.isEmpty(v.getText())) {
                        mPasswordText.requestFocus();
                    }

                    return true;
                }

                return false;
            }
        });

        mPasswordText.setImeActionLabel(getString(R.string.sign_in), EditorInfo.IME_ACTION_GO);
        mPasswordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (!TextUtils.isEmpty(v.getText())) {
                        onSignInButtonClicked();
                    }

                    return true;
                }

                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_STATE_USERNAME)) {
            mUserNameText.setText(savedInstanceState.getString(SAVE_STATE_USERNAME));
        } else {
            UserAccount currentAccount = UserAccountDao.getInstance(getActivity()).getCurrentAccount();
            if (currentAccount != null) {
                mUserNameText.setText(currentAccount.getUsername());
            }
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVE_STATE_USERNAME, mUserNameText.getText().toString());
    }

    @Override
    public void onStart() {
        super.onStart();

        UserAccountDao.getInstance(getActivity()).registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        UserAccountDao.getInstance(getActivity()).unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        mUserNameText = null;
        mPasswordText = null;
        mSignInButton = null;
        mProgressLayout = null;

        super.onDestroyView();
    }

    private void onSignInButtonClicked() {
        if (mUserNameText == null || mValidatingAccount != null) {
            return;
        }
        if (!TextUtils.isEmpty(mUserNameText.getText()) && !TextUtils.isEmpty(mPasswordText.getText())) {
            UserAccountDao userAccountDao = UserAccountDao.getInstance(getActivity());
            String username = mUserNameText.getText().toString();
            String password = mPasswordText.getText().toString();
            mValidatingAccount = userAccountDao.getAccountByUsername(username);
            if (mValidatingAccount != null) {
                mValidatingAccount.setPassword(password);
                userAccountDao.updateUserAccount(mValidatingAccount);
                userAccountDao.setCurrentAccount(mValidatingAccount);
            } else {
                mValidatingAccount = new UserAccount(username, password);
                userAccountDao.addUserAccount(mValidatingAccount);
            }

            RepoFetchService.validateUserCredentials(getActivity(), username);
        }
    }

    @Override
    public void onCurrentAccountChanged(UserAccount userAccount) {

    }

    @Override
    public void onAccountInvalidated(UserAccount userAccount) {
        if (userAccount == mValidatingAccount) {
            mValidatingAccount = null;
            if (mProgressLayout != null) {
                mProgressLayout.setVisibility(View.GONE);
            }

            Toast.makeText(getActivity(), R.string.sign_in_failed, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onAccountValidated(UserAccount userAccount) {
        if (userAccount == mValidatingAccount) {
            mValidatingAccount = null;
            if (mProgressLayout != null) {
                mProgressLayout.setVisibility(View.GONE);
            }
        }
    }
}
