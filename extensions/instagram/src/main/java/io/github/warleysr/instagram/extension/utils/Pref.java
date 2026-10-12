package io.github.warleysr.instagram.extension.utils;

import android.content.SharedPreferences;

public class Pref {
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on_while_playing";
    private static final String KEY_CAROUSEL_AUTO_SCROLL = "carousel_auto_scroll";

    public static boolean keepScreenOnWhilePlaying() {
        SharedPreferences sp = AppContext.getPreferences();
        if (sp != null) {
            return sp.getBoolean(KEY_KEEP_SCREEN_ON, true);
        }
        return true;
    }

    public static void setKeepScreenOnWhilePlaying(boolean enabled) {
        SharedPreferences sp = AppContext.getPreferences();
        if (sp != null) {
            sp.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply();
        }
    }

    public static boolean carouselAutoScroll() {
        SharedPreferences sp = AppContext.getPreferences();
        if (sp != null) {
            return sp.getBoolean(KEY_CAROUSEL_AUTO_SCROLL, false);
        }
        return false;
    }

    public static void setCarouselAutoScroll(boolean enabled) {
        SharedPreferences sp = AppContext.getPreferences();
        if (sp != null) {
            sp.edit().putBoolean(KEY_CAROUSEL_AUTO_SCROLL, enabled).apply();
        }
    }
}
