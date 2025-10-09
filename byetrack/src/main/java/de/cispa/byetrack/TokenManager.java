package de.cispa.byetrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

final class TokenManager {
    private static final String LOGTAG = "TokenManager";

    // Store in field to prevent read from disk every time
    private static SharedPreferences storage_isAmbient;
    private static SharedPreferences storage_wildcard;
    private static SharedPreferences storage_final;

    static void initPrefs(Context context) {
        if (storage_wildcard == null) {
            storage_wildcard = context.getSharedPreferences(Constants.CAPSTORAGE_WILDCARD, Context.MODE_PRIVATE);
        }
        if (storage_final == null)
            storage_final = context.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

        if (storage_isAmbient == null)
            storage_isAmbient = context.getSharedPreferences(Constants.ISAMBIENT, Context.MODE_PRIVATE);
    }

    static void storeIsAmbient(boolean isAmbient) {

        SharedPreferences.Editor editor = storage_isAmbient.edit();
        editor.clear().apply();
        editor.putBoolean(Constants.ISAMBIENT, isAmbient);
        editor.apply();
    }

    static void storeWildcardTokens(String tokenJson) {
        SharedPreferences.Editor editor_wildcard = storage_wildcard.edit();
        editor_wildcard.clear().apply(); // clear tokens, so in case of policy change, only new tokens will  be stored!
        storage_final.edit().clear().apply(); // also clear finals

        try {
            JSONObject tokens = new JSONObject(tokenJson);

            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                editor_wildcard.putString(domain, domainTokens.toString());
                Log.d(LOGTAG, "Queued for " + domain + ": " + domainTokens);
            }

            editor_wildcard.apply(); // apply instead of commit for async write (StrictMode policy violation)
            Log.d(LOGTAG, "Wildcard Tokens stored");
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to parse tokens", e);
        }
    }

    static void storeFinalTokens(String tokenJson) {
        final JSONObject parsed;
        try {
            parsed = new JSONObject(tokenJson);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to parse tokenJson", e);
            return;
        }

        // Expect exactly one domain key
        String domain = parsed.keys().hasNext() ? parsed.keys().next() : null;
        if (domain == null) {
            Log.w(LOGTAG, "No domain key in tokenJson");
            return;
        }

        JSONArray incoming = parsed.optJSONArray(domain);
        if (incoming == null) {
            Log.w(LOGTAG, "Value for domain " + domain + " is not a JSON array");
            return;
        }

        // Load existing tokens for that domain
        JSONArray existing;
        try {
            String existingStr = storage_final.getString(domain, "[]");
            existing = new JSONArray(existingStr);
        } catch (Exception e) {
            Log.w(LOGTAG, "Corrupt stored tokens for " + domain + " -> resetting", e);
            existing = new JSONArray();
        }

        // Merge existing with incoming tokens
        for (int i = 0; i < incoming.length(); i++) {
            existing.put(incoming.opt(i));
        }

        // Persist asynchronously (due to Firefox StrictMode policy)
        storage_final.edit().putString(domain, existing.toString()).apply();

        Log.d(LOGTAG, "Stored tokens for " + domain + ": " + existing);
    }

    static String getWildcardTokens(String domainName, Set<String> additionalHosts) {
        boolean isAmbient = storage_isAmbient.getBoolean(Constants.ISAMBIENT, false);
        Log.d(LOGTAG, isAmbient ? "Ambient: true" : "Ambient: false");
        if (isAmbient) {
            return storage_wildcard.getString("*", "error retrieving ambient token");
        }

        return getTokens(domainName, additionalHosts, storage_wildcard);
    }

    static String getFinalTokens(String domainName, Set<String> additionalHosts) {
        return getTokens(domainName, additionalHosts, storage_final);
    }

    static void replaceToken(String domain, String tokenOld, String tokenNew) {
        String tokensStr = storage_final.getString(domain, "");
        try {
            JSONArray tokens = new JSONArray(tokensStr);
            for (int i = 0; i < tokens.length(); i++) {
                String token = tokens.getString(i);
                if (token.equals(tokenOld)) {
                    tokens.put(i, tokenNew);
                    break;
                }
            }
            storage_final.edit().putString(domain, tokens.toString()).apply();
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to replace tokens", e);
        }
    }

    private static String getTokens(String domainName, Set<String> additionalHosts, SharedPreferences prefs) {
        String tokens = prefs.getString(domainName, "");
        if (additionalHosts == null || additionalHosts.isEmpty())
            return tokens;

        // Merge
        String tokensStrForHost;
        for (String host : additionalHosts) {
            tokensStrForHost = prefs.getString(host, "");
            tokens = mergeJsonString(tokens, tokensStrForHost);
        }
        Log.d(LOGTAG, "[Byetrack] Merged Token String: " + tokens);

        return tokens;
    }

    private static String mergeJsonString(String json1, String json2) {
        if (json1.isEmpty()) return json2;
        if (json2.isEmpty()) return json1;

        try {
            JSONArray arr1 = new JSONArray(json1);
            JSONArray arr2 = new JSONArray(json2);

            for (int i = 0; i < arr2.length(); i++) {
                String token = arr2.getString(i);
                arr1.put(token);
            }

            Log.d(LOGTAG, "Size of Merged Token String: " + arr1.length());
            return arr1.toString();

        } catch (JSONException e) {
            Log.e(LOGTAG, "Failed to merge JSON strings", e);
            return "";
        }
    }

}
