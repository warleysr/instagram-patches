package io.github.warleysr.instagram.extension.patches;

import io.github.warleysr.instagram.extension.utils.Pref;

@SuppressWarnings("unused")
public class KeepScreenOnPatch {

    // Called when the video player starts playing, with the value of the server side
    // setting that decides whether the video view keeps the screen on while playing.
    public static boolean keepScreenOnWhilePlaying(boolean original) {
        return original || Pref.keepScreenOnWhilePlaying();
    }

    // Called when the video completes, after the player turned keep screen on off.
    // Feed videos loop without reporting that playback started again, so keep it on.
    public static boolean keepScreenOnWhileLooping(boolean isLooping) {
        return isLooping && Pref.keepScreenOnWhilePlaying();
    }
}
