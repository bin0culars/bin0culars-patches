package app.bin0culars.extension.reddit;

import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.util.ArrayList;
import java.util.List;

public final class AutoModCommentsPatch {

    private static final String KEY = "automod_comments";
    private static final String CHOICE_COLLAPSE = "collapse";
    private static final String CHOICE_HIDE = "hide";
    private static final String AUTOMOD_AUTHOR = "AutoModerator";

    private AutoModCommentsPatch() {}

    public static List<?> filterComments(List<?> comments) {
        if (comments == null || comments.isEmpty()) {
            return comments;
        }

        String choice = BinocularsMods.getChoice(KEY);
        if (CHOICE_COLLAPSE.equals(choice)) {
            for (Object item : comments) {
                if (isTarget(item)) {
                    collapse(item, true);
                }
            }
            return comments;
        }

        if (CHOICE_HIDE.equals(choice)) {
            List<Object> result = withoutTargets(comments);
            if (result.size() == comments.size()) {
                return comments;
            }
            return result;
        }

        return comments;
    }

    public static List<?> filterTrees(List<?> trees) {
        if (trees == null || trees.isEmpty()) {
            return trees;
        }

        String choice = BinocularsMods.getChoice(KEY);
        if (CHOICE_COLLAPSE.equals(choice)) {
            for (Object tree : trees) {
                ArrayList<Object> comments = tree == null ? null : treeComments(tree);
                if (comments == null) {
                    continue;
                }
                for (Object item : comments) {
                    if (isTarget(item)) {
                        collapse(item, true);
                    }
                }
            }
            return trees;
        }

        if (CHOICE_HIDE.equals(choice)) {
            List<Object> result = null;
            for (int i = 0; i < trees.size(); i++) {
                Object tree = trees.get(i);
                ArrayList<Object> comments = tree == null ? null : treeComments(tree);
                if (comments != null) {
                    List<Object> kept = withoutTargets(comments);
                    if (kept.isEmpty()) {
                        if (result == null) {
                            result = new ArrayList<>(trees.subList(0, i));
                        }
                        continue;
                    }
                    if (kept.size() != comments.size()) {
                        comments.clear();
                        comments.addAll(kept);
                    }
                }
                if (result != null) {
                    result.add(tree);
                }
            }
            return result != null ? result : trees;
        }

        return trees;
    }

    private static List<Object> withoutTargets(List<?> comments) {
        List<Object> result = new ArrayList<>(comments.size());
        boolean skipping = false;
        int hideDepth = -1;

        for (Object item : comments) {
            if (skipping) {
                int depth = depthOf(item);
                if (depth > hideDepth) {
                    continue;
                }
                skipping = false;
            }

            if (isTarget(item)) {
                skipping = true;
                hideDepth = depthOf(item);
                continue;
            }

            result.add(item);
        }

        return result;
    }

    private static boolean isTarget(Object item) {
        return isComment(item)
                && (AUTOMOD_AUTHOR.equalsIgnoreCase(authorOf(item))
                        || isApp(item)
                        || isStickied(item));
    }

    private static boolean isApp(Object item) {
        Object type = accountTypeOf(item);
        return type != null && type == appAccountType();
    }

    private static boolean isComment(Object item) {
        // Modified during patching.
        return false;
    }

    private static Object accountTypeOf(Object comment) {
        // Modified during patching.
        return null;
    }

    private static Object appAccountType() {
        // Modified during patching.
        return null;
    }

    private static boolean isStickied(Object comment) {
        // Modified during patching.
        return false;
    }

    private static String authorOf(Object comment) {
        // Modified during patching.
        return null;
    }

    private static int depthOf(Object comment) {
        // Modified during patching.
        return 0;
    }

    private static void collapse(Object comment, boolean collapsed) {
        // Modified during patching.
    }

    private static ArrayList<Object> treeComments(Object tree) {
        // Modified during patching.
        return null;
    }
}
