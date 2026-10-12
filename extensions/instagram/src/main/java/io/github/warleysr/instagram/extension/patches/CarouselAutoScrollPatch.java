package io.github.warleysr.instagram.extension.patches;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Adapter;

import java.lang.ref.WeakReference;

import io.github.warleysr.instagram.extension.utils.AppContext;
import io.github.warleysr.instagram.extension.utils.Pref;

import com.instagram.common.ui.widget.reboundviewpager.ReboundViewPager;

@SuppressWarnings("unused")
public class CarouselAutoScrollPatch {
    private static final String TAG = "CarouselAutoScroll";
    private static final long IMAGE_PAGE_DURATION_MS = 5000;

    // A carousel must be at least this visible to scroll it, so posts leaving the screen do not move.
    private static final float MIN_VISIBLE_FRACTION = 0.5f;

    private static int carouselPagerId;
    private static int carouselVideoPageId;

    // Carousel scrolled by the last auto scroll. It keeps scrolling past image pages
    // as long as it stays on that page, and stops once the user swipes or leaves it.
    private static WeakReference<ReboundViewPager> chainPager = new WeakReference<>(null);
    private static int chainPage = -1;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable imagePageTimeout = CarouselAutoScrollPatch::onImagePageTimeout;

    public static boolean isEnabled() {
        return Pref.carouselAutoScroll();
    }

    private static boolean isPortuguese() {
        return "pt".equalsIgnoreCase(java.util.Locale.getDefault().getLanguage());
    }

    public static void toggle() {
        boolean enabled = !isEnabled();
        Pref.setCarouselAutoScroll(enabled);
        if (!enabled) cancelChain();
        String defaultToast;
        if (isPortuguese()) {
            defaultToast = enabled ? "Rolagem automática do carrossel ativada" : "Rolagem automática do carrossel desativada";
        } else {
            defaultToast = enabled ? "Carousel auto scroll enabled" : "Carousel auto scroll disabled";
        }
        String toast = AppContext.getString(
                enabled ? "piko_carousel_auto_scroll_enabled" : "piko_carousel_auto_scroll_disabled",
                defaultToast
        );
        AppContext.showToastShort(toast);
    }

    public static String menuTitle() {
        boolean enabled = isEnabled();
        String defaultTitle;
        if (isPortuguese()) {
            defaultTitle = enabled ? "Rolagem automática do carrossel: ativada" : "Rolagem automática do carrossel: desativada";
        } else {
            defaultTitle = enabled ? "Carousel auto scroll: on" : "Carousel auto scroll: off";
        }
        return AppContext.getString(
                enabled ? "piko_carousel_auto_scroll_on" : "piko_carousel_auto_scroll_off",
                defaultTitle
        );
    }

    /**
     * Injection point. Called on every completion of a video, including each loop of a looping video.
     *
     * @param videoView View the video is rendered on, or null if the player has none.
     */
    public static void onVideoCompleted(View videoView) {
        if (videoView == null || !isEnabled()) return;
        AppContext.setContext(videoView.getContext());

        // Player callbacks are not on the main thread.
        handler.post(() -> {
            try {
                ReboundViewPager pager = findCarouselPager(videoView);
                if (pager == null || !isMostlyVisible(pager)) return;

                // Neighbour pages may have their own player, only follow the page on screen.
                View activePage = pager.getCurrentActiveView();
                if (activePage != null && !isDescendant(videoView, activePage)) return;

                scrollToNextPage(pager);
            } catch (Exception ex) {
                Log.e(TAG, "onVideoCompleted failure", ex);
            }
        });
    }

    /**
     * Replaced by the patch with a call to the obfuscated method that animates the pager to a page.
     */
    private static void smoothScrollToPage(ReboundViewPager pager, int page) {
        Log.d(TAG, "smoothScrollToPage was not patched");
    }

    private static void scrollToNextPage(ReboundViewPager pager) {
        Adapter adapter = pager.getAdapter();
        int page = pager.getCurrentDataIndex();
        if (adapter == null || page < 0 || page >= adapter.getCount() - 1) {
            cancelChain();
            return;
        }

        int nextPage = page + 1;
        smoothScrollToPage(pager, nextPage);

        chainPager = new WeakReference<>(pager);
        chainPage = nextPage;
        handler.removeCallbacks(imagePageTimeout);
        handler.postDelayed(imagePageTimeout, IMAGE_PAGE_DURATION_MS);
    }

    private static void onImagePageTimeout() {
        try {
            ReboundViewPager pager = chainPager.get();
            if (pager == null || !isEnabled() || !isMostlyVisible(pager)
                    || pager.getCurrentDataIndex() != chainPage) {
                cancelChain();
                return;
            }

            // A video page scrolls on its own once the video completes.
            if (isVideoPage(pager.getCurrentActiveView())) return;

            scrollToNextPage(pager);
        } catch (Exception ex) {
            Log.e(TAG, "onImagePageTimeout failure", ex);
        }
    }

    private static void cancelChain() {
        handler.removeCallbacks(imagePageTimeout);
        chainPager = new WeakReference<>(null);
        chainPage = -1;
    }

    private static ReboundViewPager findCarouselPager(View view) {
        if (carouselPagerId == 0) {
            carouselPagerId = AppContext.getIdentifier("id", "carousel_viewpager");
        }

        // Stories and other screens also use this pager, so match the feed carousel id.
        ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent instanceof ReboundViewPager && ((View) parent).getId() == carouselPagerId) {
                return (ReboundViewPager) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static boolean isVideoPage(View page) {
        if (page == null) return false;
        if (carouselVideoPageId == 0) {
            carouselVideoPageId = AppContext.getIdentifier("id", "carousel_video_media_group");
        }

        View videoGroup = page.findViewById(carouselVideoPageId);
        return videoGroup != null && videoGroup.getVisibility() == View.VISIBLE;
    }

    private static boolean isDescendant(View view, View ancestor) {
        ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent == ancestor) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private static boolean isMostlyVisible(View view) {
        if (!view.isShown() || view.getWidth() == 0 || view.getHeight() == 0) return false;

        Rect visible = new Rect();
        if (!view.getGlobalVisibleRect(visible)) return false;

        float visibleArea = (float) visible.width() * visible.height();
        return visibleArea / ((float) view.getWidth() * view.getHeight()) >= MIN_VISIBLE_FRACTION;
    }
}
