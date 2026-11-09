package app.bin0culars.extension.reddit.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class BinocularsMods {

    private static final String PREFS_NAME = "bin0culars_mods";
    private static final String KEY_TABLET_GRID_FEED = "tablet_grid_feed";

    private static Context appContext;

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

    public static boolean isTabletGridEnabled() {
        try {
            Context context = getApplicationContext();
            if (context == null) {
                return true;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_TABLET_GRID_FEED, true);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void saveTabletGridEnabled(Context context, boolean enabled) {
        try {
            Context app = appContext;
            if (app == null && context != null) {
                app = context.getApplicationContext();
                appContext = app;
            }
            if (app == null) {
                app = getApplicationContext();
            }
            if (app != null) {
                SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_TABLET_GRID_FEED, enabled).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean openIfOurs(Activity activity, Uri uri) {
        if (activity == null || uri == null) {
            return false;
        }
        if (appContext == null) {
            appContext = activity.getApplicationContext();
        }
        try {
            int resId = activity.getResources().getIdentifier("content_policy_uri", "string", activity.getPackageName());
            if (resId == 0) {
                return false;
            }
            String contentPolicyUri = activity.getString(resId);
            if (contentPolicyUri.equals(uri.toString()) || uri.equals(Uri.parse(contentPolicyUri))) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    showSettings(activity);
                } else {
                    activity.runOnUiThread(() -> showSettings(activity));
                }
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void showSettings(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_DayNight);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Context dialogContext = dialog.getContext();
        int textColorPrimary = resolveColor(dialogContext, android.R.attr.textColorPrimary);
        int textColorSecondary = resolveColor(dialogContext, android.R.attr.textColorSecondary);
        int colorBackground = resolveColor(dialogContext, android.R.attr.colorBackground);
        if (colorBackground == 0) {
            colorBackground = resolveColor(dialogContext, android.R.attr.windowBackground);
        }

        LinearLayout rootLayout = new LinearLayout(dialogContext);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        if (colorBackground != 0) {
            rootLayout.setBackgroundColor(colorBackground);
        }
        rootLayout.setFitsSystemWindows(true);

        LinearLayout topBar = new LinearLayout(dialogContext);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(dialogContext, 56)
        ));
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(dialogContext, 4), 0, dp(dialogContext, 16), 0);

        ImageButton backButton = new ImageButton(dialogContext);
        backButton.setLayoutParams(new LinearLayout.LayoutParams(
                dp(dialogContext, 48),
                dp(dialogContext, 48)
        ));
        backButton.setContentDescription(getString(dialogContext, "bin0culars_back"));
        backButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        backButton.setPadding(0, 0, 0, 0);

        TypedValue homeUpValue = new TypedValue();
        int backResId = 0;
        if (dialogContext.getTheme().resolveAttribute(android.R.attr.homeAsUpIndicator, homeUpValue, true)) {
            backResId = homeUpValue.resourceId;
        }
        if (backResId == 0) {
            backResId = dialogContext.getResources().getIdentifier("ic_arrow_back", "drawable", "android");
        }
        if (backResId == 0) {
            backResId = dialogContext.getResources().getIdentifier("action_bar_up_indicator", "drawable", "android");
        }
        if (backResId != 0) {
            backButton.setImageResource(backResId);
        }
        if (textColorPrimary != 0) {
            backButton.setColorFilter(textColorPrimary);
        }

        TypedValue selectableBorderless = new TypedValue();
        if (dialogContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, selectableBorderless, true)) {
            backButton.setBackgroundResource(selectableBorderless.resourceId);
        } else {
            backButton.setBackground(null);
        }
        backButton.setOnClickListener(v -> dialog.dismiss());
        topBar.addView(backButton);

        TextView titleView = new TextView(dialogContext);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMarginStart(dp(dialogContext, 8));
        titleParams.leftMargin = dp(dialogContext, 8);
        titleView.setLayoutParams(titleParams);
        titleView.setText(getString(dialogContext, "bin0culars_mods_title"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        if (textColorPrimary != 0) {
            titleView.setTextColor(textColorPrimary);
        }
        topBar.addView(titleView);

        rootLayout.addView(topBar);

        LinearLayout row = new LinearLayout(dialogContext);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(dialogContext, 56));
        row.setPadding(dp(dialogContext, 16), dp(dialogContext, 12), dp(dialogContext, 16), dp(dialogContext, 12));
        row.setClickable(true);
        row.setFocusable(true);

        TypedValue selectableBg = new TypedValue();
        if (dialogContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectableBg, true)) {
            row.setBackgroundResource(selectableBg.resourceId);
        }

        LinearLayout textContainer = new LinearLayout(dialogContext);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        textContainerParams.setMarginEnd(dp(dialogContext, 16));
        textContainerParams.rightMargin = dp(dialogContext, 16);
        textContainer.setLayoutParams(textContainerParams);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView rowTitle = new TextView(dialogContext);
        rowTitle.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        rowTitle.setText(getString(dialogContext, "bin0culars_tablet_grid_title"));
        rowTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (textColorPrimary != 0) {
            rowTitle.setTextColor(textColorPrimary);
        }
        textContainer.addView(rowTitle);

        TextView rowSummary = new TextView(dialogContext);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        summaryParams.topMargin = dp(dialogContext, 2);
        rowSummary.setLayoutParams(summaryParams);
        rowSummary.setText(getString(dialogContext, "bin0culars_tablet_grid_summary"));
        rowSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        if (textColorSecondary != 0) {
            rowSummary.setTextColor(textColorSecondary);
        }
        textContainer.addView(rowSummary);

        row.addView(textContainer);

        Switch switchView = new Switch(dialogContext);
        switchView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        switchView.setFocusable(false);
        switchView.setClickable(false);
        switchView.setChecked(isTabletGridEnabled());

        row.setOnClickListener(v -> {
            boolean newState = !switchView.isChecked();
            switchView.setChecked(newState);
            saveTabletGridEnabled(dialogContext, newState);
        });

        row.addView(switchView);
        rootLayout.addView(row);

        TextView hintBanner = new TextView(dialogContext);
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.setMargins(dp(dialogContext, 16), dp(dialogContext, 8), dp(dialogContext, 16), 0);
        bannerParams.setMarginStart(dp(dialogContext, 16));
        bannerParams.setMarginEnd(dp(dialogContext, 16));
        hintBanner.setLayoutParams(bannerParams);
        hintBanner.setText(getString(dialogContext, "bin0culars_restart_hint"));
        hintBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        if (textColorSecondary != 0) {
            hintBanner.setTextColor(textColorSecondary);
        }
        hintBanner.setPadding(dp(dialogContext, 12), dp(dialogContext, 8), dp(dialogContext, 12), dp(dialogContext, 8));

        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setShape(GradientDrawable.RECTANGLE);
        bannerBg.setCornerRadius(dp(dialogContext, 8));
        int tintColor = textColorPrimary != 0
                ? Color.argb(20, Color.red(textColorPrimary), Color.green(textColorPrimary), Color.blue(textColorPrimary))
                : 0x14888888;
        bannerBg.setColor(tintColor);
        hintBanner.setBackground(bannerBg);

        rootLayout.addView(hintBanner);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
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

    private static int resolveColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
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
}
