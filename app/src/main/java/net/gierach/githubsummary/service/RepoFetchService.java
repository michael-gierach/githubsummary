package net.gierach.githubsummary.service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import net.gierach.githubsummary.model.LanguageData;
import net.gierach.githubsummary.model.RepoData;
import net.gierach.githubsummary.model.SyncingStateManager;
import net.gierach.githubsummary.model.UserAccount;
import net.gierach.githubsummary.model.UserAccountDao;
import net.gierach.githubsummary.protocol.GitHubProtocol;
import net.gierach.githubsummary.protocol.GitHubProtocolException;
import net.gierach.githubsummary.provider.ReposContract;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoFetchService extends IntentService {

    private static final String TAG = "RepoFetchService";

    private static final String ACTION_SYNC_USER_REPOS = "net.gierach.githubsummary.service.SYNC_USER_REPOS";
    private static final String ACTION_VALIDATE_USER_CREDENTIALS = "net.gierach.githubsummary.service.VALIDATE_USER_ACCOUNT";
    private static final String EXTRA_USERNAME = "username";

    public static void validateUserCredentials(Context context, String username) {
        Intent intent = new Intent(context, RepoFetchService.class);
        intent.setAction(ACTION_VALIDATE_USER_CREDENTIALS);
        intent.putExtra(EXTRA_USERNAME, username);

        context.startService(intent);
    }

    public static void syncUserRepos(Context context, UserAccount userAccount) {
        Intent intent = new Intent(context, RepoFetchService.class);
        intent.setAction(ACTION_SYNC_USER_REPOS);
        intent.putExtra(EXTRA_USERNAME, userAccount.getUsername());

        context.startService(intent);
    }

    public RepoFetchService() {
        super("RepoFetchService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            if (ACTION_SYNC_USER_REPOS.equals(intent.getAction())) {
                String username = intent.getStringExtra(EXTRA_USERNAME);
                UserAccount userAccount = UserAccountDao.getInstance(this).getAccountByUsername(username);
                if (userAccount != null && userAccount.isValidated() && userAccount.getRecordId() != null) {
                    performSyncUserRepos(userAccount);
                }
            } else if (ACTION_VALIDATE_USER_CREDENTIALS.equals(intent.getAction())) {
                performValidateUserCredentials(intent.getStringExtra(EXTRA_USERNAME));
            }
        }
    }

    private void performSyncUserRepos(UserAccount userAccount) {
        SyncingStateManager.getInstance().syncingStartedForUserAccount(userAccount);

        try {
            List<RepoData> repoDataList = new ArrayList<>();
            GitHubProtocol.getUserRepos(repoDataList, userAccount.getUsername(), userAccount.getPassword());

            saveRepoDataList(userAccount, repoDataList);

            performSyncRepoLanguages(userAccount);
        } catch (IOException e) {
            Log.e(TAG, "::performSyncUserRepos IOException", e);
        } catch (GitHubProtocolException e) {
            Log.e(TAG, "::performSyncUserRepos GitHubProtocolException", e);
            if (e.httpStatusCode == HttpURLConnection.HTTP_FORBIDDEN ||
                    e.httpStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                    e.httpStatusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                UserAccountDao.getInstance(this).invalidateUserAccount(userAccount);
            }
        }

        SyncingStateManager.getInstance().syncingFinishedForUserAccount(userAccount);
    }

    private void performValidateUserCredentials(String username) {
        UserAccount userAccount = UserAccountDao.getInstance(this).getAccountByUsername(username);
        if (userAccount != null) {
            boolean validated = false;

            try {
                String displayName = GitHubProtocol.getUserDisplayNameBasicAuthentication(userAccount.getUsername(), userAccount.getPassword());
                userAccount.setDisplayName(displayName);
                validated = true;
            } catch (IOException e) {
                Log.e(TAG, "::performValidateUserCredentials IOException", e);
            } catch (GitHubProtocolException e) {
                Log.e(TAG, "::performValidateUserCredentials GitHubProtocolException", e);
            }

            if (validated) {
                UserAccountDao.getInstance(this).validateUserAccount(userAccount);
            } else {
                UserAccountDao.getInstance(this).invalidateUserAccount(userAccount);
            }
        }
    }

    private void saveRepoDataList(UserAccount userAccount, List<RepoData> repoDataList) {
        if (userAccount.getRecordId() == null) {
            return;
        }
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        long userId = userAccount.getRecordId();

        Uri insertUri = ReposContract.makeInsertOrUpdateUri(ReposContract.Repos.CONTENT_URI);

        final String[] userIdSelection = new String[]{userAccount.getRecordId().toString()};

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ReposContract.Repos.CONTENT_URI);
        builder.withValue(ReposContract.RepoColumns.ON_SERVER, 0);
        builder.withSelection(ReposContract.RepoColumns.USER_ID + "=?", userIdSelection);
        operationList.add(builder.build());

        for (int i = 0; i < repoDataList.size(); ++i) {
            RepoData repoData = repoDataList.get(i);

            builder = ContentProviderOperation.newInsert(insertUri);
            builder.withValues(repoData.getContentValues(userId));
            operationList.add(builder.build());
        }

        builder = ContentProviderOperation.newDelete(ReposContract.Repos.CONTENT_URI);
        builder.withSelection(ReposContract.RepoColumns.USER_ID + "=? AND " + ReposContract.RepoColumns.ON_SERVER + "=0", userIdSelection);
        operationList.add(builder.build());

        try {
            getContentResolver().applyBatch(ReposContract.AUTHORITY, operationList);
        } catch (OperationApplicationException | RemoteException e) {
            Log.e(TAG, "::saveRepoDataList Error writing to DB.", e);
        }
    }

    private void performSyncRepoLanguages(UserAccount userAccount) {
        HashMap<String, Long> languageIdMap = new HashMap<>();
        Cursor langCursor = getContentResolver().query(ReposContract.Languages.CONTENT_URI, new String[]{ReposContract.LanguageColumns._ID, ReposContract.LanguageColumns.LANGUAGE}, null, null, null);
        if (langCursor != null) {
            if (langCursor.moveToFirst()) {
                do {
                    languageIdMap.put(langCursor.getString(1), langCursor.getLong(0));
                } while (langCursor.moveToNext());
            }

            langCursor.close();
        }

        Cursor cursor = getContentResolver().query(ReposContract.Repos.CONTENT_URI,
                new String[]{ReposContract.RepoColumns._ID, ReposContract.RepoColumns.LANGUAGES_URL},
                ReposContract.RepoColumns.USER_ID + "=? AND " + ReposContract.RepoColumns.NEED_LANG_SYNC + "=1",
                new String[]{userAccount.getRecordId().toString()}, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        String languagesUrl = cursor.getString(1);
                        long repoId = cursor.getLong(0);

                        List<LanguageData> languageDataList = GitHubProtocol.getRepoLanguages(languagesUrl, userAccount.getUsername(), userAccount.getPassword());
                        saveRepoLanguageData(languageDataList, userAccount, repoId, languageIdMap);
                    } while (cursor.moveToNext());
                }
            } catch (IOException e) {
                Log.e(TAG, "::performSyncRepoLanguages IOException", e);
            } catch (GitHubProtocolException e) {
                Log.e(TAG, "::performSyncRepoLanguages GitHubProtocolException", e);
            }

            cursor.close();
        }
    }

    private void saveRepoLanguageData(List<LanguageData> languageDataList, UserAccount userAccount, long repoId, HashMap<String, Long> languageIdMap) {
        HashMap<String, Integer> insertedLanguageMap = new HashMap<>();

        String[] repoIdParams = new String[]{Long.toString(repoId)};
        Uri languageInsertUri = ReposContract.makeInsertOrUpdateUri(ReposContract.Languages.CONTENT_URI);
        Uri insertUri = ReposContract.makeInsertOrUpdateUri(ReposContract.LanguageRepoMap.CONTENT_URI);
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ReposContract.LanguageRepoMap.CONTENT_URI);
        builder.withValue(ReposContract.LanguageRepoMapColumns.ON_SERVER, 0);
        builder.withSelection(ReposContract.LanguageRepoMapColumns.REPO_ID + "=?", repoIdParams);
        operationList.add(builder.build());

        for (int i = 0; i < languageDataList.size(); ++i) {
            LanguageData languageData = languageDataList.get(i);

            builder = ContentProviderOperation.newInsert(insertUri);
            ContentValues values = new ContentValues();
            values.put(ReposContract.LanguageRepoMapColumns.ON_SERVER, true);
            values.put(ReposContract.LanguageRepoMapColumns.LANG_BYTES, languageData.byteCount);
            values.put(ReposContract.LanguageRepoMapColumns.REPO_ID, repoId);

            Long languageId = languageIdMap.get(languageData.language);
            if (languageId == null) {
                Integer index = insertedLanguageMap.get(languageData.language);
                if (index == null) {
                    index = operationList.size();
                    insertedLanguageMap.put(languageData.language, index);
                    ContentProviderOperation.Builder langBuilder = ContentProviderOperation.newInsert(languageInsertUri);
                    langBuilder.withValues(languageData.getContentValues());
                    operationList.add(langBuilder.build());
                }
                builder.withValueBackReference(ReposContract.LanguageRepoMapColumns.LANGUAGE_ID, index);
            } else {
                values.put(ReposContract.LanguageRepoMapColumns.LANGUAGE_ID, languageId);
            }
            builder.withValues(values);
            operationList.add(builder.build());
        }

        builder = ContentProviderOperation.newDelete(ReposContract.LanguageRepoMap.CONTENT_URI);
        builder.withSelection(ReposContract.LanguageRepoMapColumns.REPO_ID + "=? AND " + ReposContract.LanguageRepoMapColumns.ON_SERVER + "=0", repoIdParams);
        operationList.add(builder.build());

        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ReposContract.AUTHORITY, operationList);

            for (Map.Entry<String, Integer> entry : insertedLanguageMap.entrySet()) {
                languageIdMap.put(entry.getKey(), ContentUris.parseId(results[entry.getValue()].uri));
            }
        } catch (OperationApplicationException | RemoteException e) {

        }
    }
}
