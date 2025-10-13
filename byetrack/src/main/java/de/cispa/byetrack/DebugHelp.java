package de.cispa.byetrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONArray;

import java.util.Map;
public final class DebugHelp {

    public static String displayFinalTokens(Context context) {
        SharedPreferences finalPrefs = context.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);
        String out = displayCapabilities(finalPrefs, context, true);
        return "Current Final Tokens:\n\n" + out;
    }


    public static String displayWildcardTokens(Context context) {
        SharedPreferences wildcardPrefs = context.getSharedPreferences(Constants.CAPSTORAGE_WILDCARD, Context.MODE_PRIVATE);
        String out = displayCapabilities(wildcardPrefs, context, false);
        return "Current Wildcard Tokens:\n\n" + out;
    }
    private static String displayCapabilities(SharedPreferences sharedPrefs, Context context, boolean isFinal) {
        Map<String, ?> allCaps = sharedPrefs.getAll();

        StringBuilder builder = new StringBuilder();

        if (allCaps.isEmpty()) {
            builder.append("(none stored)");
            return builder.toString();
        }

        try {

            for (Map.Entry<String, ?> entry : allCaps.entrySet()) {
                String domain = entry.getKey();
                builder.append("Domain: ").append(domain).append("\n");
                String tokensJson = (String) entry.getValue();
                JSONArray tokens = new JSONArray(tokensJson);

                Bundle tokenToCookieName = ByetrackClient.getTokenCookieNames(context, domain);

                for (int i = 0; i < tokens.length(); i++) {
                    String token = tokens.getString(i);
                    String compressedToken = token.substring(0, Math.min(30, token.length()));

                    builder.append("(").append(i + 1).append(")\t").append(compressedToken).append("...\n");
                    if (isFinal) {
                        String cookieName = tokenToCookieName.getString(token);
                        String cookieValue = ByetrackClient.getTokenCookieValue(context, token);
                        builder.append("\t\t-> ").append(cookieName).append(" = ").append(cookieValue).append("\n");
                    }
                }

                builder.append("\n");
            }

        } catch (Exception e) {
            return null;
        }

        return builder.toString();
    }

    public static void clearTokenStorage(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().apply();
    }

}
