package de.cispa.byetrack;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Set;

public class ByetrackClient {
    private static final String LOGTAG = "ByetrackClient";
    private static final String AUTH_TOKENINFO = "content://org.mozilla.provider.tokeninfo";

    /**
     * Attaches Tokens and extra data to an intent if existent, otherwise Intent remains clean
     * @param intent The intent to attach the tokens and additional data to.
     * @param context The context of the app to fetch tokens from.
     * @param uri The Uri to launch and get the host for.
     */
    public static void attachTokens(Intent intent, Context context, Uri uri, @Nullable Set<String> additionalHosts) {
        TokenManager.initPrefs(context);
        String domainName = uri.getHost();

        // Get prefs on demand (no statics)
        String wildcardTokens = TokenManager.getWildcardTokens(domainName, additionalHosts);
        String finalTokens = TokenManager.getFinalTokens(domainName, additionalHosts);

        Bundle byetrackData = new Bundle();
        byetrackData.putString("wildcard_tokens", wildcardTokens);
        byetrackData.putString("final_tokens", finalTokens);
        byetrackData.putInt("package_uid", context.getApplicationInfo().uid);

        intent.putExtra(Constants.BYETRACK_DATA, byetrackData);
    }

    public static Bundle getTokenCookieNames(Context context, String host) {
        TokenManager.initPrefs(context);
        String tokensStr = TokenManager.getFinalTokens(host, null);
        Log.i(LOGTAG, "Getting tokens for " + host + ": " + tokensStr);
        if (tokensStr == null) {
            Log.w(LOGTAG, "No tokens for " + host);
            return Bundle.EMPTY;
        }
        return context.getContentResolver().call(Uri.parse(AUTH_TOKENINFO), "get_token_cookie_names", tokensStr, null);
    }

    public static String getTokenCookieValue(Context context, String tokenStr) {
        Bundle tokenValue = context.getContentResolver().call(Uri.parse(AUTH_TOKENINFO), "get_token_cookie_value", tokenStr, null);
        assert tokenValue != null;
        return tokenValue.getString("value");
    }

    public static void writeTokenCookieValue(Context context, String tokenStr, String cookieValue) {
        Bundle valueToWrite = new Bundle();
        valueToWrite.putString("value", cookieValue);
        Bundle tokenInfo = context.getContentResolver().call(Uri.parse(AUTH_TOKENINFO), "write_token_cookie_value", tokenStr, valueToWrite);
        // replace updated token with original (outdated one)
        assert tokenInfo != null;
        String domain = tokenInfo.getString("domain");
        String updated_Token = tokenInfo.getString("updated_token");
        Log.d(LOGTAG, "[Byetrack] Updated " + tokenStr + " to " + updated_Token);
        TokenManager.replaceToken(domain, tokenStr, updated_Token);
    }

    public static void startActivityInBrowser(Context context, Intent intent, Bundle startAnimationBundle) {
        // Wrap into a PendingIntent so the browser can launch it
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Send this PendingIntent to your browser
        Bundle bundle = new Bundle();
        bundle.putParcelable("intent", pendingIntent);
        bundle.putBundle("startAnimationBundle", startAnimationBundle);

        context.getContentResolver().call(
                Uri.parse("content://org.mozilla.fenix.debug.customtabprovider"),
                "launch_custom_tab",
                null,
                bundle
        );

    }
}
