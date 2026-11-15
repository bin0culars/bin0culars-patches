package app.bin0culars.extension.reddit;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class FeedAutoScrollPatch {

    private static WeakReference<Object> currentFeedState;
    private static final List<WeakReference<Object>> homeSinks = new ArrayList<>();
    private static WeakReference<Object> currentGridSink;
    private static boolean homeTabSelected = true;
    private static boolean navBarShown = true;
    private static boolean activityResumed = false;
    private static SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;
    private static WeakReference<View> overlayButtonRef;
    private static WeakReference<AutoScrollOverlayLayout> overlayContainerRef;
    private static TimeAnimator timeAnimator;
    private static boolean isScrolling = false;
    private static float scrolledPx = 0f;
    private static long startTimeMs = -1L;
    private static WindowInsets lastInsets;

    public static final class AutoScrollOverlayLayout extends FrameLayout {

        private final Rect hitRect = new Rect();
        private boolean touchingButton = false;

        public AutoScrollOverlayLayout(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            try {
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    View button = overlayButtonRef != null ? overlayButtonRef.get() : null;
                    if (button != null && button.getVisibility() == View.VISIBLE) {
                        button.getHitRect(hitRect);
                        touchingButton = hitRect.contains((int) ev.getX(), (int) ev.getY());
                    } else {
                        touchingButton = false;
                    }
                    if (!touchingButton) {
                        if (isScrolling) {
                            stopAutoScroll();
                        }
                        return false;
                    }
                }
                if (touchingButton) {
                    boolean handled = super.dispatchTouchEvent(ev);
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        touchingButton = false;
                    }
                    return handled;
                }
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    private static float dispatchScroll(Object state, float delta) {
        // Modified during patching.
        return 0f;
    }

    public static void setGridFeedType(Object sink, Object feedType) {
        try {
            if (sink == null) {
                return;
            }
            synchronized (homeSinks) {
                for (int i = homeSinks.size() - 1; i >= 0; i--) {
                    Object s = homeSinks.get(i).get();
                    if (s == null || s == sink) {
                        homeSinks.remove(i);
                    }
                }
                if (feedType instanceof Enum) {
                    String name = ((Enum<?>) feedType).name();
                    if ("HOME".equals(name)) {
                        homeSinks.add(new WeakReference<>(sink));
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onGridComposing(Object sink) {
        try {
            currentGridSink = new WeakReference<>(sink);
        } catch (Throwable ignored) {
        }
    }

    public static void onListFeed(Object state, Object feedType) {
        try {
            if (state == null || !(feedType instanceof Enum)) {
                return;
            }
            String name = ((Enum<?>) feedType).name();
            if ("HOME".equals(name)) {
                currentFeedState = new WeakReference<>(state);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onGridState(Object state) {
        try {
            if (state == null || currentGridSink == null) {
                return;
            }
            Object sink = currentGridSink.get();
            if (sink == null) {
                return;
            }
            boolean matches = false;
            synchronized (homeSinks) {
                for (int i = 0; i < homeSinks.size(); i++) {
                    if (homeSinks.get(i).get() == sink) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                currentFeedState = new WeakReference<>(state);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onTabSelected(Object tab) {
        try {
            if (tab instanceof Enum) {
                homeTabSelected = "Home".equals(((Enum<?>) tab).name());
            } else {
                homeTabSelected = false;
            }
            if (!homeTabSelected) {
                stopAutoScroll();
            }
            updateButtonVisibility();
        } catch (Throwable ignored) {
        }
    }

    public static void onNavBarFraction(float fraction) {
        try {
            navBarShown = fraction > 0.5f;
            if (!isTablet()) {
                updateButtonVisibility();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onActivityResumed(Activity activity) {
        try {
            if (activity == null) {
                return;
            }

            if (prefChangeListener == null) {
                Context appContext = activity.getApplicationContext();
                if (appContext != null) {
                    SharedPreferences prefs = appContext.getSharedPreferences("bin0culars_mods", Context.MODE_PRIVATE);
                    prefChangeListener = (sharedPreferences, key) -> {
                        try {
                            if ("feed_auto_scroll".equals(key)) {
                                updateButtonVisibility();
                            }
                        } catch (Throwable ignored) {
                        }
                    };
                    prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
                }
            }

            ViewGroup parent = null;
            int containerId = activity.getResources().getIdentifier("frame_container", "id", activity.getPackageName());
            if (containerId != 0) {
                View container = activity.findViewById(containerId);
                if (container instanceof ViewGroup) {
                    parent = (ViewGroup) container;
                }
            }
            if (parent == null) {
                parent = activity.findViewById(android.R.id.content);
            }
            if (parent == null) {
                return;
            }

            final String overlayTag = "bin0culars_auto_scroll_overlay";
            final String buttonTag = "bin0culars_auto_scroll_button";

            View existing = parent.findViewWithTag(overlayTag);
            AutoScrollOverlayLayout overlay;
            if (existing instanceof AutoScrollOverlayLayout) {
                overlay = (AutoScrollOverlayLayout) existing;
                View btn = overlay.findViewWithTag(buttonTag);
                if (btn != null) {
                    overlayButtonRef = new WeakReference<>(btn);
                }
                parent.bringChildToFront(overlay);
            } else {
                overlay = new AutoScrollOverlayLayout(activity);
                overlay.setTag(overlayTag);
                overlay.setClickable(false);
                overlay.setFocusable(false);
                overlay.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                ImageButton button = createButton(activity);
                button.setTag(buttonTag);
                button.setVisibility(View.GONE);
                overlay.addView(button);
                overlayButtonRef = new WeakReference<>(button);

                overlay.setOnApplyWindowInsetsListener((v, insets) -> {
                    try {
                        lastInsets = insets;
                        updateButtonLayout();
                    } catch (Throwable ignored) {
                    }
                    return v.onApplyWindowInsets(insets);
                });

                parent.addView(overlay);
            }

            overlayContainerRef = new WeakReference<>(overlay);
            activityResumed = true;

            updateButtonLayout();
            updateButtonIcon();
            updateButtonVisibility();
        } catch (Throwable ignored) {
        }
    }

    public static void onActivityPaused(Activity activity) {
        try {
            activityResumed = false;
            stopAutoScroll();
            updateButtonVisibility();
        } catch (Throwable ignored) {
        }
    }

    private static ImageButton createButton(Context context) {
        ImageButton button = new ImageButton(context);
        int size = dp(context, 48);
        button.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        int pad = dp(context, 12);
        button.setPadding(pad, pad, pad, pad);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        updateButtonBackground(button, context);

        button.setOnClickListener(v -> {
            try {
                if (isScrolling) {
                    stopAutoScroll();
                } else {
                    startAutoScroll();
                }
            } catch (Throwable ignored) {
            }
        });

        return button;
    }

    private static void updateButtonBackground(ImageButton button, Context context) {
        try {
            int textColorPrimary = resolveColor(context, android.R.attr.textColorPrimary);
            if (textColorPrimary == 0) {
                textColorPrimary = Color.WHITE;
            }

            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.OVAL);

            if (!isTablet(context)) {
                int bgColor = resolveColor(context, android.R.attr.colorBackground);
                if (bgColor == 0) {
                    bgColor = Color.WHITE;
                }
                bgShape.setColor(bgColor);
                button.setElevation(dp(context, 6));
                button.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            } else {
                button.setElevation(0f);
                int tintAlpha = (int) (255 * 0.12f);
                int bgColor = Color.argb(
                        tintAlpha,
                        Color.red(textColorPrimary),
                        Color.green(textColorPrimary),
                        Color.blue(textColorPrimary)
                );
                bgShape.setColor(bgColor);
            }

            int rippleAlpha = (int) (255 * 0.25f);
            int rippleColor = Color.argb(
                    rippleAlpha,
                    Color.red(textColorPrimary),
                    Color.green(textColorPrimary),
                    Color.blue(textColorPrimary)
            );
            RippleDrawable rippleDrawable = new RippleDrawable(
                    ColorStateList.valueOf(rippleColor),
                    bgShape,
                    bgShape
            );
            button.setBackground(rippleDrawable);
        } catch (Throwable ignored) {
        }
    }

    private static void startAutoScroll() {
        try {
            if (!BinocularsMods.isEnabled("feed_auto_scroll")) {
                return;
            }
            Object state = currentFeedState != null ? currentFeedState.get() : null;
            if (state == null) {
                return;
            }
            if (isScrolling) {
                return;
            }
            View btn = overlayButtonRef != null ? overlayButtonRef.get() : null;
            if (btn == null) {
                return;
            }
            Context context = btn.getContext();

            String choice = BinocularsMods.getChoice("feed_auto_scroll_speed");
            float dpPerSec = 80f;
            if ("slow".equals(choice)) {
                dpPerSec = 40f;
            } else if ("fast".equals(choice)) {
                dpPerSec = 160f;
            } else if ("very_fast".equals(choice)) {
                dpPerSec = 280f;
            }

            final float speed = dpPerSec;
            final float density = context.getResources().getDisplayMetrics().density;
            final float speedPxPerMs = (speed * density) / 1000f;

            isScrolling = true;
            scrolledPx = 0f;
            startTimeMs = -1L;
            updateButtonIcon();

            if (timeAnimator != null) {
                timeAnimator.cancel();
            }
            timeAnimator = new TimeAnimator();
            timeAnimator.setTimeListener((animation, totalTime, deltaTime) -> {
                try {
                    if (!isScrolling) {
                        animation.cancel();
                        return;
                    }
                    Object curState = currentFeedState != null ? currentFeedState.get() : null;
                    if (curState == null) {
                        stopAutoScroll();
                        return;
                    }
                    if (startTimeMs < 0) {
                        startTimeMs = totalTime;
                    } else if (deltaTime > 100) {
                        startTimeMs += deltaTime;
                    }
                    float target = speedPxPerMs * (totalTime - startTimeMs);
                    float delta = target - scrolledPx;
                    if (delta > 0f) {
                        dispatchScroll(curState, delta);
                        scrolledPx = target;
                    }
                } catch (Throwable ignored) {
                }
            });
            timeAnimator.start();
        } catch (Throwable ignored) {
        }
    }

    private static void stopAutoScroll() {
        try {
            isScrolling = false;
            scrolledPx = 0f;
            startTimeMs = -1L;
            if (timeAnimator != null) {
                timeAnimator.cancel();
                timeAnimator = null;
            }
            updateButtonIcon();
        } catch (Throwable ignored) {
        }
    }

    private static void updateButtonIcon() {
        try {
            View v = overlayButtonRef != null ? overlayButtonRef.get() : null;
            if (!(v instanceof ImageButton)) {
                return;
            }
            ImageButton button = (ImageButton) v;
            Context context = button.getContext();
            String drawableName = isScrolling ? "icon_stop_fill" : "icon_play_fill";
            int resId = getDrawableId(context, drawableName);
            if (resId != 0) {
                button.setImageResource(resId);
            }
            int textColorPrimary = resolveColor(context, android.R.attr.textColorPrimary);
            if (textColorPrimary != 0) {
                button.setColorFilter(textColorPrimary);
            }
            String descKey = isScrolling ? "bin0culars_auto_scroll_stop" : "bin0culars_auto_scroll_start";
            String desc = getString(context, descKey);
            if (!desc.isEmpty()) {
                button.setContentDescription(desc);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void updateButtonLayout() {
        try {
            View button = overlayButtonRef != null ? overlayButtonRef.get() : null;
            View overlay = overlayContainerRef != null ? overlayContainerRef.get() : null;
            if (button == null || overlay == null) {
                return;
            }
            Context context = button.getContext();
            if (button instanceof ImageButton) {
                updateButtonBackground((ImageButton) button, context);
            }
            int size = dp(context, 48);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.BOTTOM | Gravity.LEFT;

            int bottomInset = getSystemInsetBottom(overlay);
            int leftInset = getSystemInsetLeft(overlay);

            if (isTablet(context)) {
                int leftMargin = leftInset + dp(context, 24);
                int bottomMargin = bottomInset + dp(context, 24);
                lp.leftMargin = leftMargin;
                lp.bottomMargin = bottomMargin;
            } else {
                int leftMargin = leftInset + dp(context, 16);
                int bottomMargin = bottomInset + dp(context, 72);
                lp.leftMargin = leftMargin;
                lp.bottomMargin = bottomMargin;
            }
            button.setLayoutParams(lp);
        } catch (Throwable ignored) {
        }
    }

    private static void updateButtonVisibility() {
        try {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                new Handler(Looper.getMainLooper()).post(FeedAutoScrollPatch::updateButtonVisibility);
                return;
            }
            View button = overlayButtonRef != null ? overlayButtonRef.get() : null;
            if (button == null) {
                return;
            }
            Context context = button.getContext();
            boolean enabled = BinocularsMods.isEnabled("feed_auto_scroll");
            boolean tablet = isTablet(context);
            boolean visible = enabled && homeTabSelected && (tablet || navBarShown) && activityResumed;

            if (!visible && isScrolling) {
                stopAutoScroll();
            }

            int newVisibility = visible ? View.VISIBLE : View.GONE;
            if (button.getVisibility() != newVisibility) {
                button.setVisibility(newVisibility);
                if (visible) {
                    updateButtonLayout();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isTablet(Context context) {
        try {
            if (context != null) {
                return context.getResources().getConfiguration().screenWidthDp >= 600;
            }
        } catch (Throwable ignored) {
        }
        return Resources.getSystem().getConfiguration().screenWidthDp >= 600;
    }

    private static boolean isTablet() {
        View btn = overlayButtonRef != null ? overlayButtonRef.get() : null;
        Context ctx = btn != null ? btn.getContext() : null;
        return isTablet(ctx);
    }

    private static int getSystemInsetLeft(View view) {
        try {
            WindowInsets insets = (Build.VERSION.SDK_INT >= 23 && view != null) ? view.getRootWindowInsets() : null;
            if (insets == null) {
                insets = lastInsets;
            }
            if (insets != null) {
                if (Build.VERSION.SDK_INT >= 30) {
                    return insets.getInsets(WindowInsets.Type.systemBars()).left;
                } else {
                    return insets.getSystemWindowInsetLeft();
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int getSystemInsetBottom(View view) {
        try {
            WindowInsets insets = (Build.VERSION.SDK_INT >= 23 && view != null) ? view.getRootWindowInsets() : null;
            if (insets == null) {
                insets = lastInsets;
            }
            if (insets != null) {
                if (Build.VERSION.SDK_INT >= 30) {
                    return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                } else {
                    return insets.getSystemWindowInsetBottom();
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static String getString(Context context, String name) {
        try {
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            if (id != 0) {
                return context.getString(id);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static int getDrawableId(Context context, String name) {
        try {
            return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int dp(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    private static int resolveColor(Context context, int attr) {
        try {
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return typedValue.data;
                }
                if (typedValue.resourceId != 0) {
                    return context.getColor(typedValue.resourceId);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
