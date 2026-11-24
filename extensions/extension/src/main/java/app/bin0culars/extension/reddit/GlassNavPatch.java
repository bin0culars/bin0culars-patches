package app.bin0culars.extension.reddit;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.util.List;
import kotlin.jvm.functions.Function1;

public final class GlassNavPatch {

    // Reddit's button row height.
    public static final float BAR_ROW_HEIGHT_DP = 50f;

    private static final float RAIL_THRESHOLD_HEIGHT_DP = 500f;
    private static final float RAIL_HEADER_HEIGHT_DP = 60f;
    private static final float RAIL_TAB_HEIGHT_DP = 64f;
    private static final float RAIL_GAP_DP = 12f;

    private static final long[] COLOR_CACHE = new long[18];

    private static volatile Context appContext;
    private static volatile float cachedDensity = -1f;
    private static volatile Boolean cachedIsDark;
    private static volatile int railTabCount;

    static {
        for (int t = 0; t < 3; t++) {
            for (int o = 0; o < 3; o++) {
                for (int d = 0; d < 2; d++) {
                    int idx = (t * 6) + (o * 2) + d;
                    COLOR_CACHE[idx] = computeColor(t, o, d == 1);
                }
            }
        }
    }

    private static final Function1<Object, Object> BAR_DRAW =
            new Function1<Object, Object>() {
                @Override
                public Object invoke(Object scope) {
                    try {
                        if (BinocularsMods.isEnabled("glass_navigation")) {
                            drawBarCapsule(scope);
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        try {
                            drawContent(scope);
                        } catch (Throwable ignored) {
                        }
                    }
                    return null;
                }
            };

    private static final Function1<Object, Object> RAIL_DRAW =
            new Function1<Object, Object>() {
                @Override
                public Object invoke(Object scope) {
                    try {
                        if (BinocularsMods.isEnabled("glass_navigation")) {
                            drawRailCapsule(scope);
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        try {
                            drawContent(scope);
                        } catch (Throwable ignored) {
                        }
                    }
                    return null;
                }
            };

    private GlassNavPatch() {}

    private static void updateDarkTheme(long color) {
        try {
            int argb = (int) (color >>> 32);
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
            cachedIsDark = luminance < 0.5;
        } catch (Throwable ignored) {
        }
    }

    public static long barColor(long original) {
        try {
            updateDarkTheme(original);

            if (!BinocularsMods.isEnabled("glass_navigation")) {
                return original;
            }
            return original & 0x00FFFFFFFFFFFFFFL;
        } catch (Throwable ignored) {
            return original;
        }
    }

    public static long barBackdrop(long color) {
        try {
            if (BinocularsMods.isEnabled("glass_navigation")) {
                return color & 0x00FFFFFFFFFFFFFFL;
            }
            return color;
        } catch (Throwable ignored) {
            return color;
        }
    }

    public static void railThemeColor(long color) {
        try {
            updateDarkTheme(color);
        } catch (Throwable ignored) {
        }
    }

    public static void railTabs(Object tabs) {
        try {
            if (tabs instanceof List) {
                railTabCount = ((List<?>) tabs).size();
            } else {
                railTabCount = 0;
            }
        } catch (Throwable ignored) {
            railTabCount = 0;
        }
    }

    public static float barElevation(float original) {
        try {
            if (BinocularsMods.isEnabled("glass_navigation")) {
                return 0f;
            }
            return original;
        } catch (Throwable ignored) {
            return original;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Function1 barDraw(Function1 original) {
        try {
            if (BinocularsMods.isEnabled("glass_navigation")) {
                return BAR_DRAW;
            }
            return original;
        } catch (Throwable ignored) {
            return original;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Function1 railDraw() {
        try {
            return RAIL_DRAW;
        } catch (Throwable ignored) {
            return RAIL_DRAW;
        }
    }

    public static boolean padContentForBottomNav(boolean original) {
        try {
            if (BinocularsMods.isEnabled("glass_navigation")) {
                return false;
            }
            return original;
        } catch (Throwable ignored) {
            return original;
        }
    }

    private static Context getApplicationContext() {
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

    private static boolean isAppNightMode() {
        try {
            Context context = getApplicationContext();
            Resources res = context != null ? context.getResources() : Resources.getSystem();
            int uiMode = res.getConfiguration().uiMode;
            return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isDarkTheme() {
        Boolean dark = cachedIsDark;
        if (dark != null) {
            return dark;
        }
        return isAppNightMode();
    }

    private static long computeColor(int tintIdx, int opacityIdx, boolean isDark) {
        boolean effectiveDark;
        if (tintIdx == 1) {
            effectiveDark = false;
        } else if (tintIdx == 2) {
            effectiveDark = true;
        } else {
            effectiveDark = isDark;
        }
        int rgb = effectiveDark ? 0x1C1C1E : 0xF2F2F7;
        int alpha;
        if (opacityIdx == 0) {
            alpha = 0x8C;
        } else if (opacityIdx == 2) {
            alpha = 0xE0;
        } else {
            alpha = 0xB8;
        }
        int argb = (alpha << 24) | rgb;
        return ((long) argb & 0xFFFFFFFFL) << 32;
    }

    private static long getCapsuleColor() {
        try {
            String tint = BinocularsMods.getChoice("glass_navigation_tint");
            int tintIdx;
            if ("light".equals(tint)) {
                tintIdx = 1;
            } else if ("dark".equals(tint)) {
                tintIdx = 2;
            } else {
                tintIdx = 0;
            }

            String opacity = BinocularsMods.getChoice("glass_navigation_opacity");
            int opacityIdx;
            if ("clear".equals(opacity)) {
                opacityIdx = 0;
            } else if ("frosted".equals(opacity)) {
                opacityIdx = 2;
            } else {
                opacityIdx = 1;
            }

            int isDarkIdx = isDarkTheme() ? 1 : 0;
            int index = (tintIdx * 6) + (opacityIdx * 2) + isDarkIdx;
            return COLOR_CACHE[index];
        } catch (Throwable ignored) {
            return COLOR_CACHE[1];
        }
    }

    private static float getDensity() {
        if (cachedDensity <= 0f) {
            try {
                Context context = getApplicationContext();
                Resources res = context != null ? context.getResources() : Resources.getSystem();
                cachedDensity = res.getDisplayMetrics().density;
            } catch (Throwable ignored) {
                return 1.0f;
            }
        }
        return cachedDensity;
    }

    private static long packFloats(float x, float y) {
        return ((long) Float.floatToRawIntBits(x) << 32) | (Float.floatToRawIntBits(y) & 0xFFFFFFFFL);
    }

    private static void drawBarCapsule(Object scope) {
        long size = drawScopeSize(scope);
        float width = Float.intBitsToFloat((int) (size >>> 32));
        float height = Float.intBitsToFloat((int) size);

        float density = getDensity();
        float left = 20f * density;
        float capsuleWidth = width - 40f * density;
        float capsuleHeight = (BAR_ROW_HEIGHT_DP + 10f) * density;
        float top = (BAR_ROW_HEIGHT_DP * density - capsuleHeight) / 2f;

        if (capsuleWidth <= 0f || capsuleHeight <= 0f) {
            return;
        }

        float radius = capsuleHeight / 2f;
        long topLeft = packFloats(left, top);
        long capsuleSize = packFloats(capsuleWidth, capsuleHeight);
        long cornerRadius = packFloats(radius, radius);
        long color = getCapsuleColor();

        drawRoundRect(scope, color, topLeft, capsuleSize, cornerRadius, null, 1.0f, 0xF0);
    }

    private static void drawRailCapsule(Object scope) {
        long size = drawScopeSize(scope);
        float width = Float.intBitsToFloat((int) (size >>> 32));
        float height = Float.intBitsToFloat((int) size);

        float density = getDensity();
        float heightDp = height / density;

        float left = RAIL_GAP_DP * density;
        float capsuleWidth = width - (2f * RAIL_GAP_DP * density);
        float top;
        float capsuleHeight;

        int n = railTabCount;
        if (heightDp > RAIL_THRESHOLD_HEIGHT_DP && n > 0) {
            float tabsHeight = (n * RAIL_TAB_HEIGHT_DP) + ((n - 1) * RAIL_GAP_DP);
            float tabsTop = RAIL_HEADER_HEIGHT_DP + (heightDp - RAIL_HEADER_HEIGHT_DP - tabsHeight) / 2f;
            top = (tabsTop - RAIL_GAP_DP) * density;
            capsuleHeight = (tabsHeight + (2f * RAIL_GAP_DP)) * density;
        } else {
            top = 16f * density;
            capsuleHeight = height - 32f * density;
        }

        if (capsuleWidth <= 0f || capsuleHeight <= 0f) {
            return;
        }

        float radius = capsuleWidth / 2f;
        long topLeft = packFloats(left, top);
        long capsuleSize = packFloats(capsuleWidth, capsuleHeight);
        long cornerRadius = packFloats(radius, radius);
        long color = getCapsuleColor();

        drawRoundRect(scope, color, topLeft, capsuleSize, cornerRadius, null, 1.0f, 0xF0);
    }

    private static void drawRoundRect(
            Object scope,
            long color,
            long topLeft,
            long size,
            long radius,
            Object style,
            float alpha,
            int mask) {}

    private static long drawScopeSize(Object scope) {
        return 0L;
    }

    private static void drawContent(Object scope) {}
}
