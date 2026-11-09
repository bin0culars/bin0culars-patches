package app.bin0culars.extension.reddit;

import android.content.res.Resources;
import app.bin0culars.extension.reddit.settings.BinocularsMods;

public final class TabletGridFeedPatch {

    private static void callMasonry(
            Object state,
            Object sink,
            Object f1,
            Object modifier,
            Object padding,
            float spacing,
            Object arrangement,
            Object footer,
            boolean fullSpan,
            Object stateSlot,
            Object header,
            Object composer,
            int changed0,
            int changed1) {
        // Modified during patching.
    }

    private static Object newPadding(
            float start,
            float top,
            float end,
            float bottom,
            Object scratch) {
        // Modified during patching.
        return null;
    }

    private static Object footer(Object scratch) {
        // Modified during patching.
        return null;
    }

    public static int columns() {
        int widthDp = Resources.getSystem().getConfiguration().screenWidthDp;
        if (widthDp < 600) {
            return 1;
        }
        if (widthDp < 900) {
            return 2;
        }
        return 3;
    }

    public static int columnsForWidth(int availablePx, int original) {
        float dp = availablePx / Resources.getSystem().getDisplayMetrics().density;
        return dp >= 840f ? Math.max(original, 3) : original;
    }

    public static boolean masonryFeed(
            Object state,
            Object sink,
            Object f1,
            Object modifier,
            Object feedType,
            Object header,
            Object stateSlot,
            Object composer) {
        if (!BinocularsMods.isTabletGridEnabled()) {
            return false;
        }
        if (columns() <= 1) {
            return false;
        }
        if (!(feedType instanceof Enum)) {
            return false;
        }
        String name = ((Enum<?>) feedType).name();
        switch (name) {
            case "HOME":
            case "POPULAR":
            case "LATEST":
            case "NEWS":
            case "SUBREDDIT":
            case "FOLLOWING":
            case "CUSTOM":
                break;
            default:
                return false;
        }
        callMasonry(
                state,
                sink,
                f1,
                modifier,
                newPadding(16f, 0f, 16f, 16f, null),
                0f,
                null,
                footer(null),
                true,
                stateSlot,
                header,
                composer,
                0,
                0);
        return true;
    }
}
