package com.example.bounceball.utils;

public class EggHatchManager {

    public static final float HATCH_HEIGHT_THRESHOLD = 1000f;

    public static void checkAndSetReady(GamePreferences prefs, float height) {
        if (!prefs.hasHatched() && !prefs.isHatchReady() && height >= HATCH_HEIGHT_THRESHOLD) {
            prefs.setHatchReady(true);
        }
    }

    public static boolean shouldShowHatchAnimation(GamePreferences prefs) {
        return prefs.isHatchReady() && !prefs.hasHatched();
    }

    public static void completeHatch(GamePreferences prefs) {
        prefs.setHatched(true);
        prefs.setHatchReady(false);
    }
}
