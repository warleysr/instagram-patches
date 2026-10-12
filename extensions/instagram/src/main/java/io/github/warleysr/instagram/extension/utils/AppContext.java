package io.github.warleysr.instagram.extension.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class AppContext {
    private static Context appContext;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static Context getContext() {
        if (appContext != null) {
            return appContext;
        }
        try {
            appContext = (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
        } catch (Throwable ignored) {
        }
        return appContext;
    }

    public static void setContext(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static SharedPreferences getPreferences() {
        Context context = getContext();
        if (context != null) {
            return context.getSharedPreferences("instagram_custom_patches", Context.MODE_PRIVATE);
        }
        return null;
    }

    public static void showToastShort(String message) {
        mainHandler.post(() -> {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static int getIdentifier(String defType, String name) {
        Context context = getContext();
        if (context != null) {
            return context.getResources().getIdentifier(name, defType, context.getPackageName());
        }
        return 0;
    }

    public static String getString(String name, String fallback) {
        Context context = getContext();
        if (context != null) {
            int id = getIdentifier("string", name);
            if (id != 0) {
                try {
                    return context.getString(id);
                } catch (Throwable ignored) {
                }
            }
        }
        return fallback;
    }
}
