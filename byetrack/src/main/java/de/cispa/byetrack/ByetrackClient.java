package de.cispa.byetrack;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ByetrackClient {
    private static final String LOGTAG = "ByetrackClient";

    /**
     * Attaches Tokens and extra data to an intent if existent, otherwise Intent remains clean
     * @param intent The intent to attach the tokens and additional data to.
     * @param context The context of the app to fetch tokens from.
     * @param uri The Uri to launch and get the host for.
     */
    public static void attachTokens(Intent intent, Context context, Uri uri) {
        TokenManager.initPrefs(context);
        String domainName = uri.getHost();

        // Get prefs on demand (no statics)
        String wildcardTokens = TokenManager.getWildcardTokens(domainName);
        String finalTokens = TokenManager.getFinalTokens(domainName);

        Bundle byetrackData = new Bundle();
        byetrackData.putString("wildcard_tokens", wildcardTokens);
        byetrackData.putString("final_tokens", finalTokens);
        byetrackData.putInt("package_uid", context.getApplicationInfo().uid);

        intent.putExtra(Constants.BYETRACK_DATA, byetrackData);
    }
}
