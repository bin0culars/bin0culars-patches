package app.bin0culars.extension.reddit;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.widget.FrameLayout;

public final class GlassSearchButton extends View {

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private LinearGradient edgeShader;
    private int cachedEdgeHeight = -1;
    private boolean cachedEdgeDark;
    private int navBarInsetPx;

    public GlassSearchButton(Context context) {
        super(context);
        setContentDescription("Search");
        setClickable(true);
        setFocusable(true);

        circlePaint.setStyle(Paint.Style.FILL);
        edgePaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        setClipToOutline(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ShapeDrawable mask = new ShapeDrawable(new OvalShape());
            setForeground(new RippleDrawable(ColorStateList.valueOf(0x33888888), null, mask));
        }

        setOnClickListener(v -> GlassSearch.invokeSearchAction());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        WindowInsets rootInsets = getRootWindowInsets();
        if (rootInsets != null) {
            readBottomInset(rootInsets);
            updateLayoutParams();
        }
        requestApplyInsets();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        readBottomInset(insets);
        updateLayoutParams();
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        GlassSearch.onConfigurationChanged();
    }

    private void readBottomInset(WindowInsets insets) {
        if (insets == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            navBarInsetPx = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        } else {
            navBarInsetPx = insets.getSystemWindowInsetBottom();
        }
    }

    void updateLayoutParams() {
        ViewGroup.LayoutParams rawLp = getLayoutParams();
        if (!(rawLp instanceof FrameLayout.LayoutParams)) {
            return;
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawLp;
        int diameter = Math.round(GlassNavPatch.capsuleHeightPx());
        ViewParent parent = getParent();
        float parentWidth = (parent instanceof View && ((View) parent).getWidth() > 0)
                ? ((View) parent).getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        int endMargin = Math.round(GlassNavPatch.capsuleSideGapPx(parentWidth));
        float density = GlassNavPatch.getDensity();
        int capsuleOffset = Math.round(
                (GlassNavPatch.BAR_ROW_HEIGHT_DP * density - GlassNavPatch.capsuleHeightPx()) / 2f);
        int bottomMargin = Math.max(0, navBarInsetPx + capsuleOffset + GlassBlur.liftPx());

        if (lp.width != diameter
                || lp.height != diameter
                || lp.gravity != (Gravity.BOTTOM | Gravity.END)
                || lp.bottomMargin != bottomMargin
                || lp.getMarginEnd() != endMargin) {
            lp.width = diameter;
            lp.height = diameter;
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.bottomMargin = bottomMargin;
            lp.setMarginEnd(endMargin);
            setLayoutParams(lp);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f;
        float density = GlassNavPatch.getDensity();
        boolean isDark = GlassNavPatch.glassIsDark();

        circlePaint.setColor((int) (GlassNavPatch.getCapsuleColor() >>> 32));
        canvas.drawCircle(cx, cy, radius, circlePaint);

        if (GlassBlur.blurActive()) {
            float strokeWidth = GlassBlur.EDGE_DP * density;
            edgePaint.setStrokeWidth(strokeWidth);
            if (edgeShader == null || cachedEdgeHeight != h || cachedEdgeDark != isDark) {
                int colorStart = isDark ? 0x40FFFFFF : 0xB3FFFFFF;
                int colorEnd = isDark ? 0x0DFFFFFF : 0x14000000;
                edgeShader = new LinearGradient(0f, 0f, 0f, h, colorStart, colorEnd, Shader.TileMode.CLAMP);
                edgePaint.setShader(edgeShader);
                cachedEdgeHeight = h;
                cachedEdgeDark = isDark;
            }
            canvas.drawCircle(cx, cy, Math.max(0f, radius - strokeWidth / 2f), edgePaint);
        }

        iconPaint.setColor(isDark ? Color.WHITE : Color.BLACK);
        iconPaint.setStrokeWidth(2.0f * density);
        float lensRadius = 7.0f * density;
        float lensCx = cx - 2.0f * density;
        float lensCy = cy - 2.0f * density;
        canvas.drawCircle(lensCx, lensCy, lensRadius, iconPaint);
        float offset = lensRadius * 0.70710678f;
        canvas.drawLine(lensCx + offset, lensCy + offset, cx + 9.0f * density, cy + 9.0f * density, iconPaint);
    }
}
