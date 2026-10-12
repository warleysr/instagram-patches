package io.github.warleysr.instagram.extension.patches;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;

import io.github.warleysr.instagram.extension.utils.AppContext;

import com.instagram.feed.media.mediaoption.MediaOption$Option;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FeedButton {
    private static final String TAG = "CarouselAutoScroll";

    public static MediaOption$Option carouselAutoScrollOverflowButton() {
        int drawableIconId = AppContext.getIdentifier("drawable", "instagram_slideshow_pano_outline_24");
        return new MediaOption$Option("CAROUSEL_AUTO_SCROLL", 504, drawableIconId);
    }

    public static MediaOption$Option[] addToMenuOptionArray() {
        MediaOption$Option[] originalArray = MediaOption$Option.$values();
        if (originalArray == null) {
            return new MediaOption$Option[]{MediaOption$Option.CAROUSEL_AUTO_SCROLL};
        }
        int originalLength = originalArray.length;
        MediaOption$Option[] newButtonArray = new MediaOption$Option[originalLength + 1];
        System.arraycopy(originalArray, 0, newButtonArray, 0, originalLength);
        newButtonArray[originalLength] = MediaOption$Option.CAROUSEL_AUTO_SCROLL;
        return newButtonArray;
    }

    private static Class<?> getEnumButtonClass() throws Exception {
        return Class.forName("X.6zl");
    }

    private static Object enumNormalButton() throws Exception {
        return getEnumButtonClass().getMethod("valueOf", String.class).invoke(null, "NORMAL");
    }

    private static void addButton(MediaOption$Option overflowButton, String overflowButtonText, Object buttonAdderObject, ArrayList buttonlist) throws Exception {
        Class<?> clazz = buttonAdderObject.getClass();
        Method method = clazz.getDeclaredMethod(
                "A00",
                getEnumButtonClass(),
                MediaOption$Option.class,
                clazz,
                CharSequence.class,
                ArrayList.class,
                boolean.class
        );
        method.setAccessible(true);
        method.invoke(null, enumNormalButton(), overflowButton, buttonAdderObject, overflowButtonText, buttonlist, false);
    }

    public static void addFeedOverflowButton(Object buttonAdderObject, ArrayList buttonlist) {
        try {
            addButton(MediaOption$Option.CAROUSEL_AUTO_SCROLL, CarouselAutoScrollPatch.menuTitle(), buttonAdderObject, buttonlist);
        } catch (Exception e) {
            Log.e(TAG, "Error in addFeedOverflowButton", e);
        }
    }

    public static boolean onButtonClicked(MediaOption$Option pressedButton) {
        if (pressedButton != null && "CAROUSEL_AUTO_SCROLL".equals(pressedButton.toString())) {
            CarouselAutoScrollPatch.toggle();
            return true;
        }
        if (MediaOption$Option.CAROUSEL_AUTO_SCROLL.equals(pressedButton)) {
            CarouselAutoScrollPatch.toggle();
            return true;
        }
        return false;
    }
}
