package de.cispa.byetrack;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class AppPipeProvider extends ContentProvider {
    private static final String LOGTAG = "AppPipeProvider";
    public static final String METHOD_GET_PIPE = "GET_PIPE";
    public static final String METHOD_GET_CHANNEL = "GET_CHANNEL";
    public static final String EXTRA_PIPE = "de.cispa.byetrack.EXTRA_PIPE";
    public static final String EXTRA_CHANNEL = "de.cispa.byetrack.EXTRA_CHANNEL";
    public static final int PI_FLAGS_ONESHOT = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE;
    public static final int PI_FLAGS_CHANNEL = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;

    @Override
    public Bundle call(@NonNull String method, String arg, Bundle extras) {
        final Context ctx = getContext();
        if (ctx == null) return null;

        // Verify the caller is the trusted INSTALLER
        int callingUid = Binder.getCallingUid();
        if (!verifyCallerIsTrusted(callingUid)) {
            Log.w(LOGTAG, "Untrusted caller UID=" + callingUid);
            return null;
        }

        if (METHOD_GET_CHANNEL.equals(method)) {
            PendingIntent channel = makeBasePI(ctx, 0, PI_FLAGS_CHANNEL);
            Bundle out = new Bundle();
            out.putParcelable(EXTRA_CHANNEL, channel);
            return out;
        }

        if (METHOD_GET_PIPE.equals(method)) {
            PendingIntent pipe = makeBasePI(ctx, 0, PI_FLAGS_ONESHOT);

            Bundle out = new Bundle();
            out.putParcelable(EXTRA_PIPE, pipe);
            return out;
        }

        return null;
    }

    private boolean verifyCallerIsTrusted(int uid) {
        PackageManager pm = getContext().getPackageManager();
        String packageName = pm.getNameForUid(uid);

        return Objects.equals(packageName, "de.cispa.custominstaller");
    }

    private PendingIntent makeBasePI(Context ctx, int reqCode, int flags) {
        Intent base = new Intent()
                .setComponent(new ComponentName(
                        ctx.getPackageName(), "de.cispa.byetrack.TokenReceiver"));
        // no session required for zero changes to TokenReceiver
        return PendingIntent.getBroadcast(ctx, reqCode, base, flags);
    }


    // Required but unused methods for ContentProvider
    @Override public boolean onCreate() { return true;}
    @Override public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Nullable @Override public String getType(@NonNull Uri uri) { return null; }
    @Nullable @Override public Uri insert(@NonNull Uri uri, ContentValues values) { return null; }
    @Override public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }

}
