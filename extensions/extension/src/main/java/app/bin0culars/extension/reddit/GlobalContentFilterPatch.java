package app.bin0culars.extension.reddit;

import android.os.Handler;
import android.os.Looper;
import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class GlobalContentFilterPatch {

    public static final String PREF_KEY = "global_content_filter";

    public static final String CHOICE_SFW = "sfw";
    public static final String CHOICE_NSFW = "nsfw";

    public static final String NSFW_TYPEAHEAD_SECTION = "nsfw_typeahead_section";

    private static final Set<Object> PAGERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static final Map<String, Integer> THIN_PAGE_COUNTERS = new HashMap<>();

    public static void registerPager(Object pager) {
        if (pager != null) {
            PAGERS.add(pager);
        }
    }

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

    private static int searchPostNsfw(Object element, int scratch) {
        // Modified during patching.
        return -1;
    }

    private static int searchCommunityNsfw(Object element, int scratch) {
        // Modified during patching.
        return -1;
    }

    private static int linkOver18(Object link, int scratch) {
        // Modified during patching.
        return -1;
    }

    private static boolean isFirstPage(Object request, int scratch) {
        // Modified during patching.
        return false;
    }

    private static Object pagerFeedType(Object pager, int scratch) {
        // Modified during patching.
        return null;
    }

    private static void pagerRetry(Object pager, int scratch) {
        // Modified during patching.
    }

    private static String typeaheadSectionId(Object element, int scratch) {
        // Modified during patching.
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterPage(List page, Object dataSource, Object request) {
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
            Object pageFeedType = null;
            if (dataSource != null) {
                pageFeedType = feedType(dataSource);
                if (pageFeedType instanceof Enum && "PDP".equals(((Enum<?>) pageFeedType).name())) {
                    return page;
                }
            }
            int total = 0;
            int kept = 0;
            List result = new ArrayList(page.size());
            for (Object element : page) {
                if (element == null) {
                    continue;
                }
                if (isPostContainer(element, 0)) {
                    total++;
                    if (isNsfw(element, 0) == isNsfw) {
                        result.add(element);
                        kept++;
                    }
                } else {
                    int postNsfw = searchPostNsfw(element, 0);
                    if (postNsfw >= 0) {
                        total++;
                        if ((postNsfw == 1) == isNsfw) {
                            result.add(element);
                            kept++;
                        }
                    } else {
                        int commNsfw = searchCommunityNsfw(element, 0);
                        if (commNsfw >= 0) {
                            if (!isSfw || commNsfw != 1) {
                                result.add(element);
                            }
                        } else {
                            String sectionId = typeaheadSectionId(element, 0);
                            if (sectionId != null) {
                                if (!isSfw || !NSFW_TYPEAHEAD_SECTION.equals(sectionId)) {
                                    result.add(element);
                                }
                            } else {
                                result.add(element);
                            }
                        }
                    }
                }
            }

            if (total >= 1 && pageFeedType != null) {
                String feedTypeName = pageFeedType instanceof Enum
                        ? ((Enum<?>) pageFeedType).name()
                        : pageFeedType.toString();
                boolean shouldRetry = false;
                synchronized (THIN_PAGE_COUNTERS) {
                    if (isFirstPage(request, 0) || kept >= 3) {
                        THIN_PAGE_COUNTERS.put(feedTypeName, 0);
                    }
                    if (kept < 3) {
                        Integer countObj = THIN_PAGE_COUNTERS.get(feedTypeName);
                        int count = countObj != null ? countObj : 0;
                        if (count < 10) {
                            THIN_PAGE_COUNTERS.put(feedTypeName, count + 1);
                            shouldRetry = true;
                        }
                    }
                }
                if (shouldRetry) {
                    final Object targetFeedType = pageFeedType;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            List<Object> pagersCopy;
                            synchronized (PAGERS) {
                                pagersCopy = new ArrayList<>(PAGERS);
                            }
                            for (Object pager : pagersCopy) {
                                if (pager != null && pagerFeedType(pager, 0) == targetFeedType) {
                                    pagerRetry(pager, 0);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    });
                }
            }

            return result;
        } catch (Throwable ignored) {
            return page;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List filterMediaLinks(List links) {
        try {
            if (links == null || links.isEmpty()) {
                return links;
            }
            String choice = BinocularsMods.getChoice(PREF_KEY);
            boolean isNsfw = CHOICE_NSFW.equals(choice);
            boolean isSfw = CHOICE_SFW.equals(choice);
            if (!isNsfw && !isSfw) {
                return links;
            }
            List result = new ArrayList(links.size());
            for (Object item : links) {
                int over18 = linkOver18(item, 0);
                if (over18 < 0) {
                    result.add(item);
                } else if ((over18 == 1) == isNsfw) {
                    result.add(item);
                }
            }
            return result;
        } catch (Throwable ignored) {
            return links;
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
