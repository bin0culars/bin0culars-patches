package app.bin0culars.extension.reddit;

import android.R;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import app.bin0culars.extension.reddit.settings.BinocularsMods;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import kotlin.jvm.functions.Function0;

public final class CommentEmojiReplyPatch {

    private static final String SETTING_KEY = "comment_emoji_reply";
    private static final String PREFS_NAME = "bin0culars_comment_emoji_reply";
    private static final String COUNT_PREFIX = "count_";
    private static final String TS_PREFIX = "ts_";
    private static final String[] DEFAULT_EMOJIS = {
        "👍", "❤️", "😂", "😮", "😢", "🙏"
    };
    private static final String[] PICKER_EMOJIS = {
        "👍", "👎", "❤️", "😂", "😮", "😢",
        "🙏", "🔥", "💯", "👏", "🎉", "🤩",
        "😍", "🥰", "🤔", "🤷", "🙄", "😭",
        "😡", "🤯", "😱", "🥳", "😎", "💀",
        "🤡", "👀", "🫡", "🤝", "🙌", "💪",
        "🫶", "💔", "💖", "✨", "⭐", "⚡",
        "🚀", "🏆", "🎯", "💡", "✅", "❌",
        "😅", "🤣", "😊", "🥲", "😌", "😏",
        "😬", "🫠", "🤨", "😴", "🤢", "🤮",
        "☕", "🍻", "🍿", "🐶", "🐱", "🌈"
    };

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<Activity> WRAPPED_ACTIVITIES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Class<?>, LambdaMeta> LAMBDA_META_CACHE = new HashMap<>();
    private static final LambdaMeta INVALID_LAMBDA_META = new LambdaMeta(null, null, null);

    private static boolean lifecycleRegistered;
    private static WeakReference<Activity> currentActivityRef;
    private static WeakReference<Object> commentRepositoryRef;
    private static WeakReference<PopupWindow> activePopupRef;
    private static WeakReference<Dialog> activeDialogRef;
    private static Method cachedCreateMarkdownMethod;
    private static Constructor<?> cachedReplyEventConstructor;
    private static Method cachedGetCommentIdMethod;
    private static Class<?> cachedCommentClass;
    private static List<String> cachedTopEmojis;

    private static float lastTouchRawX;
    private static float lastTouchRawY;
    private static boolean hasTouchPoint;

    private CommentEmojiReplyPatch() {}

    private static String createMarkdownContClassName() {
        return "";
    }

    private static String replyEventClassName() {
        return "";
    }

    public static void setCommentRepository(Object repository) {
        if (repository != null) {
            commentRepositoryRef = new WeakReference<>(repository);
        }
    }

    public static void onActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            currentActivityRef = new WeakReference<>(activity);
            wrapWindowCallback(activity);
            registerLifecycleCallbacks(activity.getApplication());
        } catch (Throwable ignored) {
        }
    }

    public static Function0<?> wrapLongPress(final Function0<?> original) {
        if (original == null || !BinocularsMods.isEnabled(SETTING_KEY)) {
            return original;
        }
        return new LongPressWrapper(original);
    }

    private static final class LambdaMeta {
        final Field commentIdField;
        final Field eventSinkField;
        final Method onEventMethod;

        LambdaMeta(Field commentIdField, Field eventSinkField, Method onEventMethod) {
            this.commentIdField = commentIdField;
            this.eventSinkField = eventSinkField;
            this.onEventMethod = onEventMethod;
        }
    }

    private static LambdaMeta getLambdaMeta(Class<?> lambdaClass) {
        LambdaMeta cached = LAMBDA_META_CACHE.get(lambdaClass);
        if (cached != null) {
            return cached;
        }
        LambdaMeta resolved = resolveLambdaMeta(lambdaClass);
        LAMBDA_META_CACHE.put(lambdaClass, resolved);
        return resolved;
    }

    private static LambdaMeta resolveLambdaMeta(Class<?> lambdaClass) {
        Field commentIdField = null;
        int stringFieldCount = 0;
        Field eventSinkField = null;
        Method onEventMethod = null;

        Class<?> clazz = lambdaClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (fieldType == String.class) {
                    stringFieldCount++;
                    commentIdField = field;
                } else if (eventSinkField == null && !fieldType.isPrimitive()) {
                    Method candidate = findOnEventMethod(fieldType);
                    if (candidate != null) {
                        eventSinkField = field;
                        onEventMethod = candidate;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (stringFieldCount != 1 || commentIdField == null || eventSinkField == null || onEventMethod == null) {
            return INVALID_LAMBDA_META;
        }
        commentIdField.setAccessible(true);
        eventSinkField.setAccessible(true);
        return new LambdaMeta(commentIdField, eventSinkField, onEventMethod);
    }

    private static final class LongPressWrapper implements Function0<Object> {
        private final Function0<?> original;

        LongPressWrapper(Function0<?> original) {
            this.original = original;
        }

        @Override
        public Object invoke() {
            if (!BinocularsMods.isEnabled(SETTING_KEY)) {
                return original.invoke();
            }
            try {
                LambdaMeta meta = getLambdaMeta(original.getClass());
                if (meta != INVALID_LAMBDA_META) {
                    Object idVal = meta.commentIdField.get(original);
                    Object sinkVal = meta.eventSinkField.get(original);
                    if (idVal instanceof String && !((String) idVal).isEmpty() && sinkVal != null) {
                        showEmojiBarOnMain((String) idVal, sinkVal, meta.onEventMethod);
                        return null;
                    }
                }
            } catch (Throwable ignored) {
            }
            return original.invoke();
        }
    }

    private static void registerLifecycleCallbacks(Application app) {
        if (app == null || lifecycleRegistered) {
            return;
        }
        lifecycleRegistered = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity a) {}

            @Override
            public void onActivityResumed(Activity a) {
                currentActivityRef = new WeakReference<>(a);
                wrapWindowCallback(a);
            }

            @Override
            public void onActivityPaused(Activity a) {
                WeakReference<Activity> ref = currentActivityRef;
                if (ref != null && ref.get() == a) {
                    currentActivityRef = null;
                    dismissActivePopups();
                }
            }

            @Override
            public void onActivityStopped(Activity a) {}

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity a) {
                WRAPPED_ACTIVITIES.remove(a);
            }
        });
    }

    private static void wrapWindowCallback(Activity activity) {
        if (activity == null || WRAPPED_ACTIVITIES.contains(activity)) {
            return;
        }
        try {
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            Window.Callback existing = window.getCallback();
            if (existing == null) {
                return;
            }
            if (Proxy.isProxyClass(existing.getClass())
                    && Proxy.getInvocationHandler(existing) instanceof TouchTrackingCallback) {
                WRAPPED_ACTIVITIES.add(activity);
                return;
            }
            Window.Callback wrapped = (Window.Callback) Proxy.newProxyInstance(
                    CommentEmojiReplyPatch.class.getClassLoader(),
                    new Class<?>[]{Window.Callback.class},
                    new TouchTrackingCallback(existing)
            );
            window.setCallback(wrapped);
            WRAPPED_ACTIVITIES.add(activity);
        } catch (Throwable ignored) {
        }
    }

    private static final class TouchTrackingCallback implements InvocationHandler {
        private final Window.Callback delegate;

        TouchTrackingCallback(Window.Callback delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name) && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if ("dispatchTouchEvent".equals(name)
                    && args != null
                    && args.length == 1
                    && args[0] instanceof MotionEvent) {
                MotionEvent ev = (MotionEvent) args[0];
                if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    lastTouchRawX = ev.getRawX();
                    lastTouchRawY = ev.getRawY();
                    hasTouchPoint = true;
                }
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    private static Method findOnEventMethod(Class<?> type) {
        if (type == null || type == Object.class) {
            return null;
        }
        Class<?> curr = type;
        while (curr != null && curr != Object.class) {
            for (Method m : curr.getDeclaredMethods()) {
                if ("onEvent".equals(m.getName()) && m.getReturnType() == void.class) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1 && !params[0].isPrimitive()) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            }
            for (Class<?> iface : curr.getInterfaces()) {
                Method fromIface = findOnEventMethod(iface);
                if (fromIface != null) {
                    return fromIface;
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static Activity getCurrentActivity() {
        WeakReference<Activity> ref = currentActivityRef;
        if (ref == null) {
            return null;
        }
        Activity activity = ref.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
    }

    private static void dismissActivePopups() {
        WeakReference<PopupWindow> popupRef = activePopupRef;
        activePopupRef = null;
        if (popupRef != null) {
            PopupWindow popup = popupRef.get();
            if (popup != null && popup.isShowing()) {
                try {
                    popup.dismiss();
                } catch (Throwable ignored) {
                }
            }
        }
        WeakReference<Dialog> dialogRef = activeDialogRef;
        activeDialogRef = null;
        if (dialogRef != null) {
            Dialog dialog = dialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                try {
                    dialog.dismiss();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void runOnMain(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            MAIN_HANDLER.post(task);
        }
    }

    private static void showEmojiBarOnMain(
            final String commentId,
            final Object eventSink,
            final Method onEventMethod) {
        runOnMain(() -> showEmojiBar(commentId, eventSink, onEventMethod));
    }

    private static void showEmojiBar(
            final String commentId,
            final Object eventSink,
            final Method onEventMethod) {
        try {
            Activity activity = getCurrentActivity();
            if (activity == null) {
                return;
            }
            Window window = activity.getWindow();
            View decorView = window != null ? window.getDecorView() : null;
            if (decorView == null || decorView.getWindowToken() == null) {
                return;
            }

            dismissActivePopups();

            int[] colors = resolveSurfaceColors(activity);
            int surfaceColor = colors[0];
            int textColor = colors[1];
            int borderColor = colors[2];

            int shadowPad = dp(activity, 12);
            FrameLayout wrapper = new FrameLayout(activity);
            wrapper.setClipToPadding(false);
            wrapper.setClipChildren(false);
            wrapper.setPadding(shadowPad, shadowPad, shadowPad, shadowPad);

            final LinearLayout pill = new LinearLayout(activity);
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            int hPad = dp(activity, 8);
            int vPad = dp(activity, 4);
            pill.setPadding(hPad, vPad, hPad, vPad);

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setShape(GradientDrawable.RECTANGLE);
            pillBg.setCornerRadius(dp(activity, 24));
            pillBg.setColor(surfaceColor);
            pillBg.setStroke(dp(activity, 1), borderColor);
            pill.setBackground(pillBg);
            pill.setElevation(dp(activity, 8));

            final PopupWindow popup = new PopupWindow(
                    wrapper,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popup.setOutsideTouchable(true);
            popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popup.setElevation(0f);

            int targetSize = dp(activity, 44);
            int rippleColor = Color.argb(38, Color.red(textColor), Color.green(textColor), Color.blue(textColor));

            List<String> topEmojis = getTopEmojis(activity);
            for (final String emoji : topEmojis) {
                TextView emojiView = new TextView(activity);
                emojiView.setLayoutParams(new LinearLayout.LayoutParams(targetSize, targetSize));
                emojiView.setGravity(Gravity.CENTER);
                emojiView.setIncludeFontPadding(false);
                emojiView.setText(emoji);
                emojiView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                emojiView.setBackground(createCircleRipple(0, rippleColor));
                emojiView.setOnClickListener(v -> {
                    dismissActivePopups();
                    onEmojiPicked(activity, commentId, eventSink, onEventMethod, emoji);
                });
                pill.addView(emojiView);
            }

            int plusCircleFill = Color.argb(22, Color.red(textColor), Color.green(textColor), Color.blue(textColor));
            TextView plusView = new TextView(activity);
            plusView.setGravity(Gravity.CENTER);
            plusView.setIncludeFontPadding(false);
            plusView.setText("+");
            plusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            plusView.setTypeface(Typeface.DEFAULT_BOLD);
            plusView.setTextColor(textColor);
            String moreDesc = getString(activity, "bin0culars_emoji_reply_more");
            if (!moreDesc.isEmpty()) {
                plusView.setContentDescription(moreDesc);
            }
            int circleInset = dp(activity, 4);
            GradientDrawable plusCircle = new GradientDrawable();
            plusCircle.setShape(GradientDrawable.OVAL);
            plusCircle.setColor(plusCircleFill);
            RippleDrawable plusBg = new RippleDrawable(ColorStateList.valueOf(rippleColor), plusCircle, plusCircle);
            plusView.setBackground(plusBg);
            LinearLayout.LayoutParams plusHolderParams = new LinearLayout.LayoutParams(
                    dp(activity, 36),
                    dp(activity, 36)
            );
            plusHolderParams.setMargins(circleInset, circleInset, circleInset, circleInset);
            plusView.setLayoutParams(plusHolderParams);
            plusView.setOnClickListener(v -> {
                dismissActivePopups();
                showEmojiPickerDialog(activity, commentId, eventSink, onEventMethod);
            });
            pill.addView(plusView);

            wrapper.addView(pill, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            wrapper.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int wrapperW = wrapper.getMeasuredWidth();
            int wrapperH = wrapper.getMeasuredHeight();
            int pillW = wrapperW - 2 * shadowPad;
            int pillH = wrapperH - 2 * shadowPad;

            Rect visibleFrame = new Rect();
            decorView.getWindowVisibleDisplayFrame(visibleFrame);
            if (visibleFrame.width() <= 0 || visibleFrame.height() <= 0) {
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                visibleFrame.set(0, 0, dm.widthPixels, dm.heightPixels);
            }

            float touchX = hasTouchPoint ? lastTouchRawX : visibleFrame.centerX();
            float touchY = hasTouchPoint ? lastTouchRawY : visibleFrame.centerY();

            int screenMargin = dp(activity, 10);
            int minPillX = visibleFrame.left + screenMargin;
            int maxPillX = Math.max(minPillX, visibleFrame.right - screenMargin - pillW);
            int pillX = Math.round(touchX - pillW / 2f);
            pillX = Math.max(minPillX, Math.min(maxPillX, pillX));

            int minPillY = visibleFrame.top + screenMargin;
            int maxPillY = Math.max(minPillY, visibleFrame.bottom - screenMargin - pillH);
            int offsetAbove = dp(activity, 64);
            int pillY = Math.round(touchY - offsetAbove - pillH / 2f);
            if (pillY < minPillY) {
                pillY = Math.round(touchY + dp(activity, 36));
            }
            pillY = Math.max(minPillY, Math.min(maxPillY, pillY));

            activePopupRef = new WeakReference<>(popup);
            popup.setOnDismissListener(() -> {
                WeakReference<PopupWindow> ref = activePopupRef;
                if (ref != null && ref.get() == popup) {
                    activePopupRef = null;
                    hasTouchPoint = false;
                }
            });

            decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            popup.showAtLocation(decorView, Gravity.NO_GRAVITY, pillX - shadowPad, pillY - shadowPad);

            pill.setAlpha(0f);
            pill.setScaleX(0.82f);
            pill.setScaleY(0.82f);
            pill.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } catch (Throwable ignored) {
        }
    }

    private static void showEmojiPickerDialog(
            final Activity activity,
            final String commentId,
            final Object eventSink,
            final Method onEventMethod) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            int[] colors = resolveSurfaceColors(activity);
            int surfaceColor = colors[0];
            int textColor = colors[1];
            int borderColor = colors[2];

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 16));

            GradientDrawable sheetBg = new GradientDrawable();
            sheetBg.setShape(GradientDrawable.RECTANGLE);
            float r = dp(activity, 20);
            sheetBg.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, 0f, 0f});
            sheetBg.setColor(surfaceColor);
            sheetBg.setStroke(dp(activity, 1), borderColor);
            container.setBackground(sheetBg);

            View handle = new View(activity);
            LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 4));
            handleParams.gravity = Gravity.CENTER_HORIZONTAL;
            handleParams.bottomMargin = dp(activity, 10);
            handle.setLayoutParams(handleParams);
            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setShape(GradientDrawable.RECTANGLE);
            handleBg.setCornerRadius(dp(activity, 2));
            handleBg.setColor(Color.argb(64, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
            handle.setBackground(handleBg);
            container.addView(handle);

            String title = getString(activity, "bin0culars_emoji_reply_more");
            if (!title.isEmpty()) {
                TextView titleView = new TextView(activity);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                titleParams.setMargins(dp(activity, 6), 0, dp(activity, 6), dp(activity, 8));
                titleView.setLayoutParams(titleParams);
                titleView.setText(title);
                titleView.setTextColor(textColor);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                titleView.setTypeface(Typeface.DEFAULT_BOLD);
                container.addView(titleView);
            }

            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            int maxScrollHeight = Math.max(dp(activity, 140), Math.min(dp(activity, 360), (int) (dm.heightPixels * 0.45f)));

            ScrollView scrollView = new ScrollView(activity);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    maxScrollHeight
            ));

            LinearLayout grid = new LinearLayout(activity);
            grid.setOrientation(LinearLayout.VERTICAL);

            int columns = 6;
            int cellHeight = dp(activity, 48);
            int rippleColor = Color.argb(38, Color.red(textColor), Color.green(textColor), Color.blue(textColor));

            LinearLayout currentRow = null;
            for (int i = 0; i < PICKER_EMOJIS.length; i++) {
                if (i % columns == 0) {
                    currentRow = new LinearLayout(activity);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    grid.addView(currentRow);
                }
                final String emoji = PICKER_EMOJIS[i];
                TextView cell = new TextView(activity);
                cell.setLayoutParams(new LinearLayout.LayoutParams(0, cellHeight, 1f));
                cell.setGravity(Gravity.CENTER);
                cell.setIncludeFontPadding(false);
                cell.setText(emoji);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                cell.setBackground(createCircleRipple(0, rippleColor));
                cell.setOnClickListener(v -> {
                    dismissActivePopups();
                    onEmojiPicked(activity, commentId, eventSink, onEventMethod, emoji);
                });
                currentRow.addView(cell);
            }

            scrollView.addView(grid);
            container.addView(scrollView);

            int sheetWidth = Math.min(dm.widthPixels, dp(activity, 480));
            dialog.setContentView(container);
            dialog.setCanceledOnTouchOutside(true);
            Window dialogWindow = dialog.getWindow();
            if (dialogWindow != null) {
                dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogWindow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                dialogWindow.setLayout(sheetWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            activeDialogRef = new WeakReference<>(dialog);
            dialog.setOnDismissListener(d -> {
                WeakReference<Dialog> ref = activeDialogRef;
                if (ref != null && ref.get() == dialog) {
                    activeDialogRef = null;
                }
            });
            dialog.show();
        } catch (Throwable ignored) {
        }
    }

    private static List<String> getTopEmojis(Context context) {
        if (cachedTopEmojis != null) {
            return cachedTopEmojis;
        }
        List<String> result = new ArrayList<>(DEFAULT_EMOJIS.length);
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            List<EmojiScore> scored = new ArrayList<>();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.startsWith(COUNT_PREFIX) && entry.getValue() instanceof Number) {
                    int count = ((Number) entry.getValue()).intValue();
                    if (count > 0) {
                        String emoji = key.substring(COUNT_PREFIX.length());
                        Object tsObj = all.get(TS_PREFIX + emoji);
                        long ts = tsObj instanceof Number ? ((Number) tsObj).longValue() : 0L;
                        scored.add(new EmojiScore(emoji, count, ts));
                    }
                }
            }
            Collections.sort(scored, (a, b) -> {
                if (b.count != a.count) {
                    return Integer.compare(b.count, a.count);
                }
                return Long.compare(b.timestamp, a.timestamp);
            });
            for (EmojiScore item : scored) {
                if (!result.contains(item.emoji)) {
                    result.add(item.emoji);
                    if (result.size() == DEFAULT_EMOJIS.length) {
                        cachedTopEmojis = result;
                        return result;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        for (String def : DEFAULT_EMOJIS) {
            if (!result.contains(def)) {
                result.add(def);
                if (result.size() == DEFAULT_EMOJIS.length) {
                    break;
                }
            }
        }
        cachedTopEmojis = result;
        return result;
    }

    private static final class EmojiScore {
        final String emoji;
        final int count;
        final long timestamp;

        EmojiScore(String emoji, int count, long timestamp) {
            this.emoji = emoji;
            this.count = count;
            this.timestamp = timestamp;
        }
    }

    private static void recordEmojiUsage(Context context, String emoji) {
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int nextCount = prefs.getInt(COUNT_PREFIX + emoji, 0) + 1;
            prefs.edit()
                    .putInt(COUNT_PREFIX + emoji, nextCount)
                    .putLong(TS_PREFIX + emoji, System.currentTimeMillis())
                    .apply();
            cachedTopEmojis = null;
            getTopEmojis(context);
        } catch (Throwable ignored) {
        }
    }

    private static void onEmojiPicked(
            final Context context,
            final String commentId,
            final Object eventSink,
            final Method onEventMethod,
            final String emoji) {
        final Context appContext = context.getApplicationContext();
        recordEmojiUsage(appContext, emoji);

        Object repo = commentRepositoryRef != null ? commentRepositoryRef.get() : null;
        if (repo == null) {
            showFailureToast(appContext);
            return;
        }

        String contClassName = createMarkdownContClassName();
        if (contClassName == null || contClassName.isEmpty()) {
            showFailureToast(appContext);
            return;
        }

        ClassLoader cl = repo.getClass().getClassLoader();
        Class<?> contImplClass;
        Constructor<?> contCtor;
        Object emptyCoroutineContext;
        try {
            contImplClass = Class.forName(contClassName, false, cl);
            contCtor = findCreateMarkdownContConstructor(contImplClass, repo);
            Class<?> emptyCtxClass = Class.forName("kotlin.coroutines.EmptyCoroutineContext", true, cl);
            emptyCoroutineContext = emptyCtxClass.getField("INSTANCE").get(null);
        } catch (Throwable ignored) {
            showFailureToast(appContext);
            return;
        }
        if (contCtor == null || emptyCoroutineContext == null) {
            showFailureToast(appContext);
            return;
        }

        Method createMethod = getCreateMarkdownMethod(repo.getClass(), contImplClass);
        if (createMethod == null) {
            showFailureToast(appContext);
            return;
        }

        Object commentParentType = null;
        Object[] enumConstants = createMethod.getParameterTypes()[0].getEnumConstants();
        if (enumConstants != null) {
            for (Object constant : enumConstants) {
                if ("COMMENT".equals(((Enum<?>) constant).name())) {
                    commentParentType = constant;
                    break;
                }
            }
        }
        if (commentParentType == null) {
            showFailureToast(appContext);
            return;
        }

        final boolean[] handled = { false };
        Class<?> continuationInterface = contCtor.getParameterTypes()[1];
        Object completionProxy = createContinuationProxy(
                cl,
                continuationInterface,
                emptyCoroutineContext,
                handled,
                appContext,
                commentId,
                eventSink,
                onEventMethod,
                emoji);

        try {
            Object contInstance = contCtor.newInstance(repo, completionProxy);
            Field labelField = findIntField(contImplClass, "label");
            if (labelField == null) {
                showFailureToast(appContext);
                return;
            }
            // Start in state 1 so resume returns the result instead of restarting with null args.
            labelField.setInt(contInstance, 1);

            Object ret = createMethod.invoke(repo, commentParentType, commentId, emoji, null, null, null, contInstance);
            if ((ret == null || !"COROUTINE_SUSPENDED".equals(ret.toString())) && !handled[0]) {
                handled[0] = true;
                postResultToMain(appContext, commentId, eventSink, onEventMethod, emoji, ret);
            }
        } catch (Throwable ignored) {
            if (!handled[0]) {
                handled[0] = true;
                showFailureToast(appContext);
            }
        }
    }

    private static Constructor<?> findCreateMarkdownContConstructor(Class<?> contImplClass, Object repo) {
        Constructor<?> fallback = null;
        for (Constructor<?> c : contImplClass.getDeclaredConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == 2 && params[0].isInstance(repo)) {
                if (params[1].isInterface()) {
                    c.setAccessible(true);
                    return c;
                }
                if (fallback == null) {
                    fallback = c;
                }
            }
        }
        if (fallback != null) {
            fallback.setAccessible(true);
        }
        return fallback;
    }

    private static Object createContinuationProxy(
            ClassLoader cl,
            Class<?> continuationInterface,
            final Object emptyCoroutineContext,
            final boolean[] handled,
            final Context appContext,
            final String commentId,
            final Object eventSink,
            final Method onEventMethod,
            final String emoji) {
        return Proxy.newProxyInstance(
                cl,
                new Class<?>[] { continuationInterface },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        try {
                            String name = method.getName();
                            if ("getContext".equals(name) && (args == null || args.length == 0)) {
                                return emptyCoroutineContext;
                            }
                            if ("resumeWith".equals(name) && args != null && args.length == 1) {
                                if (!handled[0]) {
                                    handled[0] = true;
                                    postResultToMain(appContext, commentId, eventSink, onEventMethod, emoji, args[0]);
                                }
                                return null;
                            }
                            if ("hashCode".equals(name) && (args == null || args.length == 0)) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(name) && args != null && args.length == 1) {
                                return proxy == args[0];
                            }
                            if ("toString".equals(name) && (args == null || args.length == 0)) {
                                return "EmojiReplyContinuation@" + Integer.toHexString(System.identityHashCode(proxy));
                            }
                        } catch (Throwable ignored) {
                        }
                        return null;
                    }
                });
    }

    private static Field findIntField(Class<?> cls, String name) {
        Class<?> curr = cls;
        while (curr != null && curr != Object.class) {
            try {
                Field f = curr.getDeclaredField(name);
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static Method getCreateMarkdownMethod(Class<?> repoClass, Class<?> contImplClass) {
        Method cached = cachedCreateMarkdownMethod;
        if (cached != null && cached.getDeclaringClass().isAssignableFrom(repoClass)) {
            return cached;
        }
        Class<?> curr = repoClass;
        while (curr != null && curr != Object.class) {
            for (Method m : curr.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 7
                        && "com.reddit.domain.model.comment.CreateCommentParentType".equals(params[0].getName())
                        && params[1] == String.class
                        && params[2] == String.class
                        && List.class.isAssignableFrom(params[5])
                        && params[6].isAssignableFrom(contImplClass)) {
                    m.setAccessible(true);
                    cachedCreateMarkdownMethod = m;
                    return m;
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static void postResultToMain(
            final Context appContext,
            final String parentCommentId,
            final Object eventSink,
            final Method onEventMethod,
            final String emoji,
            final Object result) {
        runOnMain(() -> handlePostResult(appContext, parentCommentId, eventSink, onEventMethod, emoji, result));
    }

    private static void handlePostResult(
            Context appContext,
            String parentCommentId,
            Object eventSink,
            Method onEventMethod,
            String emoji,
            Object result) {
        if (result == null) {
            showFailureToast(appContext);
            return;
        }

        Object comment = extractComment(result);
        if (comment == null) {
            showFailureToast(appContext);
            return;
        }

        dispatchReplyEvent(eventSink, onEventMethod, parentCommentId, comment);
        showSuccessToast(appContext, emoji);
    }

    private static Object extractComment(Object result) {
        try {
            Class<?> commentClass = getCommentClass(result.getClass().getClassLoader());
            if (commentClass == null) {
                return null;
            }
            if (commentClass.isInstance(result)) {
                return result;
            }
            for (Field field : result.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object val = field.get(result);
                if (val != null && commentClass.isInstance(val)) {
                    return val;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void dispatchReplyEvent(
            Object eventSink,
            Method onEventMethod,
            String parentCommentId,
            Object comment) {
        if (eventSink == null || onEventMethod == null) {
            return;
        }
        try {
            ClassLoader cl = comment.getClass().getClassLoader();
            Class<?> commentClass = getCommentClass(cl);
            if (commentClass == null) {
                return;
            }
            String newCommentId = getCommentId(comment, commentClass, cl);
            Constructor<?> ctor = getReplyEventConstructor(cl, commentClass);
            if (ctor == null) {
                return;
            }
            Object event = ctor.newInstance(comment, -1, null, parentCommentId, newCommentId, false, true);
            onEventMethod.invoke(eventSink, event);
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> getCommentClass(ClassLoader cl) {
        Class<?> cached = cachedCommentClass;
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> cls = cl != null
                    ? Class.forName("com.reddit.domain.model.Comment", false, cl)
                    : Class.forName("com.reddit.domain.model.Comment");
            cachedCommentClass = cls;
            return cls;
        } catch (Throwable ignored) {
            try {
                Class<?> cls = Class.forName("com.reddit.domain.model.Comment");
                cachedCommentClass = cls;
                return cls;
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private static String getCommentId(Object comment, Class<?> commentClass, ClassLoader cl) {
        try {
            Method m = cachedGetCommentIdMethod;
            if (m == null) {
                Class<?> extClass = cl != null
                        ? Class.forName("com.reddit.domain.model.RedditIdentifierExtensionsKt", false, cl)
                        : Class.forName("com.reddit.domain.model.RedditIdentifierExtensionsKt");
                try {
                    m = extClass.getMethod("getCommentId", commentClass);
                } catch (NoSuchMethodException ignored) {
                    for (Method candidate : extClass.getDeclaredMethods()) {
                        if ("getCommentId".equals(candidate.getName())) {
                            Class<?>[] params = candidate.getParameterTypes();
                            if (params.length == 1 && params[0].isAssignableFrom(commentClass)) {
                                m = candidate;
                                break;
                            }
                        }
                    }
                }
                if (m != null) {
                    m.setAccessible(true);
                    cachedGetCommentIdMethod = m;
                }
            }
            if (m != null) {
                Object res = m.invoke(null, comment);
                if (res instanceof String) {
                    return (String) res;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Constructor<?> getReplyEventConstructor(ClassLoader cl, Class<?> commentClass) {
        Constructor<?> cached = cachedReplyEventConstructor;
        if (cached != null) {
            return cached;
        }
        String className = replyEventClassName();
        if (className == null || className.isEmpty()) {
            return null;
        }
        try {
            Class<?> eventClass = cl != null
                    ? Class.forName(className, false, cl)
                    : Class.forName(className);
            for (Constructor<?> ctor : eventClass.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 7
                        && params[0].isAssignableFrom(commentClass)
                        && params[1] == int.class
                        && !params[2].isPrimitive()
                        && params[3] == String.class
                        && params[4] == String.class
                        && params[5] == boolean.class
                        && params[6] == boolean.class) {
                    ctor.setAccessible(true);
                    cachedReplyEventConstructor = ctor;
                    return ctor;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void showSuccessToast(Context context, String emoji) {
        String template = getString(context, "bin0culars_emoji_reply_sent");
        if (!template.isEmpty()) {
            showToast(context, String.format(Locale.getDefault(), template, emoji));
        }
    }

    private static void showFailureToast(Context context) {
        String message = getString(context, "bin0culars_emoji_reply_failed");
        if (!message.isEmpty()) {
            showToast(context, message);
        }
    }

    private static void showToast(final Context context, final String message) {
        if (context == null || message == null || message.isEmpty()) {
            return;
        }
        final Context target = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        runOnMain(() -> {
            try {
                Toast.makeText(target, message, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
    }

    private static int[] resolveSurfaceColors(Context context) {
        int bgColor = resolveColor(context, R.attr.colorBackgroundFloating);
        if (bgColor == 0 || Color.alpha(bgColor) < 255) {
            bgColor = resolveColor(context, R.attr.colorBackground);
        }
        if (bgColor == 0 || Color.alpha(bgColor) < 255) {
            bgColor = resolveColor(context, R.attr.windowBackground);
        }

        boolean nightMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (bgColor == 0 || Color.alpha(bgColor) < 255) {
            bgColor = nightMode ? 0xFF222426 : 0xFFFFFFFF;
        } else {
            bgColor = 0xFF000000 | (bgColor & 0x00FFFFFF);
        }

        int r = Color.red(bgColor);
        int g = Color.green(bgColor);
        int b = Color.blue(bgColor);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        boolean darkSurface = luminance < 0.5;
        if (darkSurface && r < 24 && g < 24 && b < 24) {
            // Lift pure black surfaces slightly so the floating pill stays distinct from OLED backgrounds.
            bgColor = 0xFF222426;
        }

        int textColor = resolveColor(context, R.attr.textColorPrimary);
        if (textColor == 0) {
            textColor = darkSurface ? 0xFFE8EAED : 0xFF1A1A1A;
        }
        int borderColor = darkSurface ? 0x26FFFFFF : 0x18000000;
        return new int[]{bgColor, textColor, borderColor};
    }

    private static int resolveColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            }
            if (typedValue.resourceId != 0) {
                try {
                    return context.getColor(typedValue.resourceId);
                } catch (Throwable ignored) {
                }
            }
        }
        return 0;
    }

    private static RippleDrawable createCircleRipple(int fillColor, int rippleColor) {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        GradientDrawable content = null;
        if (fillColor != 0) {
            content = new GradientDrawable();
            content.setShape(GradientDrawable.OVAL);
            content.setColor(fillColor);
        }
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
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

    private static int dp(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
