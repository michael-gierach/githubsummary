package net.gierach.githubsummary.protocol;

import android.util.Base64;
import android.util.Log;

import net.gierach.githubsummary.model.LanguageData;
import net.gierach.githubsummary.model.RepoData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GitHubProtocol {

    private static final String TAG = "GitHubProtocol";

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    private static final String GITHUB_MIME_TYPE = "application/vnd.github.v3+json";
    private static final MediaType JSON_MIME_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String GITHUB_PROD_HOST = "https://api.github.com";


    public static String getUserDisplayNameBasicAuthentication(String username, String password) throws IOException, GitHubProtocolException {
        HashMap<String, String> headerMap = new HashMap<>();

        addBasicAuthorizationToHeaderMap(headerMap, username, password);

        try {
            URL url = new URL(GITHUB_PROD_HOST + "/user");
            String jsonStr = getJsonObjectOkio(url, headerMap);
            Log.d(TAG, "::getUserDisplayName GET response body: " + jsonStr);
            JSONObject jsonObject = new JSONObject(jsonStr);

            return jsonObject.getString("name");
        } catch (JSONException e) {

        }

        return "";
    }

    public static boolean getUserRepos(List<RepoData> list, String username, String password) throws IOException, GitHubProtocolException {
        boolean finished = true;
        HashMap<String, String> headerMap = new HashMap<>();

        addBasicAuthorizationToHeaderMap(headerMap, username, password);

        try {
            URL url = new URL(GITHUB_PROD_HOST + "/user/repos?per_page=100");
            String jsonStr = getJsonObjectOkio(url, headerMap);
            Log.d(TAG, "::getUserRepos GET response body: " + jsonStr);
            JSONArray jsonArray = new JSONArray(jsonStr);

            for (int i = 0; i < jsonArray.length(); ++i) {
                JSONObject object = jsonArray.getJSONObject(i);

                RepoData repoData = new RepoData();
                repoData.description = object.getString("description");
                repoData.isPrivate = object.getBoolean("private");
                repoData.serverId = Integer.toString(object.getInt("id"));
                repoData.onServer = true;
                repoData.languagesUrl = object.getString("languages_url");
                repoData.name = object.getString("name");
                repoData.stargazerCount = object.getInt("stargazers_count");
                JSONObject owner = object.getJSONObject("owner");
                repoData.owner = owner.getString("login");
                repoData.ownerType = owner.getString("type");

                list.add(repoData);
            }
        } catch (JSONException e) {

        }

        return finished;
    }

    public static List<LanguageData> getRepoLanguages(String languageUrl, String username, String password) throws IOException, GitHubProtocolException {
        List<LanguageData> results = new ArrayList<>();
        HashMap<String, String> headerMap = new HashMap<>();

        addBasicAuthorizationToHeaderMap(headerMap, username, password);

        try {
            URL url = new URL(languageUrl);
            String jsonStr = getJsonObjectOkio(url, headerMap);
            Log.d(TAG, "::getRepoLanguages GET response body: " + jsonStr);
            JSONObject object = new JSONObject(jsonStr);
            JSONArray jsonArray = object.names();

            if (jsonArray != null) {

                for (int i = 0; i < jsonArray.length(); ++i) {
                    LanguageData languageData = new LanguageData();
                    languageData.language = jsonArray.getString(i);
                    languageData.byteCount = object.getInt(languageData.language);

                    results.add(languageData);
                }
            } else {
                LanguageData languageData = new LanguageData();
                languageData.language = "None";
                languageData.byteCount = 0;
                results.add(languageData);
            }
        } catch (JSONException e) {

        }

        return results;
    }

    private static void addBasicAuthorizationToHeaderMap(HashMap<String, String> headerMap, String username, String password) {
        String authorization = username + ':' + password;
        authorization = "Basic " + Base64.encodeToString(authorization.getBytes(CHARSET_UTF8), Base64.NO_WRAP | Base64.URL_SAFE);

        headerMap.put("Authorization", authorization);
    }

    private static String postJsonObjectToServerOkio(URL url, JSONObject jsonObject, HashMap<String, String> headerMap) throws IOException, GitHubProtocolException {

        OkHttpClient client = new OkHttpClient();

        Log.d(TAG, "POST " + url + " " + jsonObject.toString());
        byte[] payload = jsonObject.toString().getBytes(CHARSET_UTF8);
        RequestBody requestBody = RequestBody.create(JSON_MIME_TYPE, payload);
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        builder.addHeader("Accept", GITHUB_MIME_TYPE);
        builder.post(requestBody);

        Request request = builder.build();

        Response response = client.newCall(request).execute();
        int responseCode = response.code();
        if (responseCode != 200 && responseCode != 201 && responseCode != 202) {
            throw new GitHubProtocolException(responseCode, response.message());
        } else {
            return new String(response.body().bytes(), CHARSET_UTF8);
        }
    }

    private static String getJsonObjectOkio(URL url, HashMap<String, String> headerMap) throws IOException, GitHubProtocolException {
        OkHttpClient client = new OkHttpClient();

        Log.d(TAG, "GET " + url);

        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        builder.addHeader("Accept", GITHUB_MIME_TYPE);
        builder.get();

        Request request = builder.build();

        Response response = client.newCall(request).execute();
        int responseCode = response.code();
        if (responseCode != 200 && responseCode != 201 && responseCode != 202) {
            throw new GitHubProtocolException(responseCode, response.message());
        } else {
            return new String(response.body().bytes(), CHARSET_UTF8);
        }
    }


}
