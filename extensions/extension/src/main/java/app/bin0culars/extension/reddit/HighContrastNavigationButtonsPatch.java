package app.bin0culars.extension.reddit;

import android.graphics.Color;
import app.bin0culars.extension.reddit.settings.BinocularsMods;

public final class HighContrastNavigationButtonsPatch {

    private static final boolean ENABLED =
            BinocularsMods.isEnabled("high_contrast_navigation_buttons");

    private static final long BLACK = 0xFF000000L << 32;
    private static final long WHITE = 0xFFFFFFFFL << 32;

    private HighContrastNavigationButtonsPatch() {}

    public static long getUnselectedButtonColor(long color) {
        if (!ENABLED) {
            return color;
        }

        try {
            // Relative luminance where black and white have equal contrast; lighter grey implies a dark theme.
            boolean dark = BinocularsMods.isEnabled("glass_navigation")
                    ? GlassNavPatch.glassIsDark()
                    : Color.luminance((int) (color >>> 32)) > 0.179f;
            return dark ? WHITE : BLACK;
        } catch (Throwable ignored) {
            return color;
        }
    }
}
