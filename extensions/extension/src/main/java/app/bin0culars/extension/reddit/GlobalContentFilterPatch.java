package app.bin0culars.extension.reddit;

import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.util.ArrayList;
import java.util.List;

public final class GlobalContentFilterPatch {

    public static final String PREF_KEY = "global_content_filter";

    public static final String CHOICE_SFW = "sfw";
    public static final String CHOICE_DEFAULT = "default";
    public static final String CHOICE_NSFW = "nsfw";

    private static boolean isPostContainer(Object element, int scratch) {
        // Modified during patching.
        return false;
    }

    private static List<?> children(Object element, int scratch) {
        // Modified during patching.
        return null;
    }

    private static List<?> indicatorList(Object element, int scratch) {
        // Modified during patching.
        return null;
    }

    private static Object feedType(Object dataSource) {
        // Modified during patching.
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterPage(List page, Object dataSource) {
        try {
            if (page == null || page.isEmpty()) {
                return page;
            }
            String choice = BinocularsMods.getChoice(PREF_KEY);
            boolean isNsfw = CHOICE_NSFW.equals(choice);
            boolean isSfw = CHOICE_SFW.equals(choice);
            if (!isNsfw && !isSfw) {
                return page;
            }
            if (dataSource != null) {
                Object ft = feedType(dataSource);
                if (ft instanceof Enum && "PDP".equals(((Enum<?>) ft).name())) {
                    return page;
                }
            }
            List result = new ArrayList(page.size());
            for (Object element : page) {
                if (element == null) {
                    continue;
                }
                if (!isPostContainer(element, 0)) {
                    result.add(element);
                } else if (isNsfw(element, 0) == isNsfw) {
                    result.add(element);
                }
            }
            return result;
        } catch (Throwable ignored) {
            return page;
        }
    }

    private static boolean isNsfw(Object element, int depth) {
        if (element == null || depth > 4) {
            return false;
        }
        List<?> indicators = indicatorList(element, 0);
        if (indicators != null && !indicators.isEmpty()) {
            for (Object indicator : indicators) {
                if (indicator instanceof Enum && "NSFW".equals(((Enum<?>) indicator).name())) {
                    return true;
                }
            }
        }
        List<?> kids = children(element, 0);
        if (kids != null && !kids.isEmpty()) {
            for (Object child : kids) {
                if (isNsfw(child, depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
