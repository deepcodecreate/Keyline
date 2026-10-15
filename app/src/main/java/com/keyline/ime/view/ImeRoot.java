/*
 * Copyright 2026 deepcodecreate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keyline.ime.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.theme.BackgroundImageManager;

public class ImeRoot extends FrameLayout {
    public interface Callbacks {
        void onFloatMoved(float fx, float fy);
        void onDock();
        void onOneHandSwap();
        void onOneHandFull();
    }

    public static final int ONE_HAND_OFF = 0;
    public static final int ONE_HAND_KEYS_LEFT = 1;
    public static final int ONE_HAND_KEYS_RIGHT = 2;

    private static final int BTN_NONE = 0, BTN_SWAP = 1, BTN_FULL = 2;

    private Bitmap image;
    private int dim;
    private final RectF dst = new RectF();
    private boolean glassDark = true;
    private final RectF glass = new RectF();
    private final Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chromePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private View toolbarView;

    private LinearLayout card;
    private View host;
    private FloatHandle handle;
    private int chromeColor = Color.TRANSPARENT;
    private int iconColor = 0xFF888888;

    private boolean floating;
    private float floatX = 0.5f;
    private float floatY = 1f;
    private float dragFreeX;
    private float dragFreeY;
    private int oneHand = ONE_HAND_OFF;

    private float ohAmount;

    private float ohSide;
    private ValueAnimator ohAnim;
    private static final int DRAG_NONE = 0, DRAG_ONE_HAND = 1, DRAG_FLOAT = 2;
    private int drag = DRAG_NONE;
    private int liveWidthPct = -1;
    private float liveScale = -1f;
    private float dragLeft;
    private float dragTop;
    private final RectF oneHandGrip = new RectF();
    private final RectF floatGrip = new RectF();
    private final Paint gripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Callbacks callbacks;

    private final RectF swapRect = new RectF();
    private final RectF fullRect = new RectF();
    private int pressedButton = BTN_NONE;
    private Drawable swapIcon;
    private Drawable fullIcon;

    public ImeRoot(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        swapIcon = icon(context, R.drawable.ic_swap_horiz);
        fullIcon = icon(context, R.drawable.ic_open_in_full);
    }

    private static Drawable icon(Context c, int res) {
        Drawable d = AppCompatResources.getDrawable(c, res);
        return d == null ? null : DrawableCompat.wrap(d.mutate());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        card = findViewById(R.id.card);
        host = findViewById(R.id.host);
        handle = findViewById(R.id.float_handle);
        if (handle != null) {
            handle.setListener(new FloatHandle.Listener() {
                @Override public void onDragBy(float dx, float dy) { dragFloat(dx, dy); }
                @Override public void onDragEnd() {
                    if (callbacks != null) callbacks.onFloatMoved(floatX, floatY);
                }
                @Override public void onDock() {
                    if (callbacks != null) callbacks.onDock();
                }
            });
        }
        if (card != null) {
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(14));
                }
            });
        }
        applyMode();
    }

    public void setCallbacks(Callbacks c) { callbacks = c; }

    public void setBackgroundImage(Bitmap bmp, int dimPercent) {
        image = bmp;
        dim = dimPercent;
        invalidate();
    }

    public void setDim(int dimPercent) {
        if (dim == dimPercent) return;
        dim = dimPercent;
        invalidate();
    }

    public void setGlassDark(boolean dark) {
        if (glassDark != dark) {
            glassDark = dark;
            invalidate();
        }
    }

    public void setIconColor(int color) {
        iconColor = color;
        if (handle != null) handle.setColor(color);
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        chromeColor = color;
        applyMode();
    }

    public boolean isFloating() { return floating; }

    public void setFloating(boolean on, float fx, float fy) {
        fx = clamp01(fx);
        fy = clamp01(fy);
        boolean changed = on != floating || (on && (fx != floatX || fy != floatY));
        floating = on;
        floatX = fx;
        floatY = fy;
        if (changed) {
            applyMode();
            requestLayout();
        }
    }

    public void setOneHand(int mode) {
        if (mode == oneHand) return;
        int before = oneHand;
        oneHand = mode;
        float tAmt = mode == ONE_HAND_OFF ? 0f : 1f;
        float tSide = mode == ONE_HAND_KEYS_RIGHT ? 1f : (mode == ONE_HAND_KEYS_LEFT ? 0f : ohSide);
        if (ohAnim != null) ohAnim.cancel();
        boolean canAnimate = !floating && isLaidOut() && getWidth() > 0 && isShown();
        if (!canAnimate) {
            ohAmount = tAmt;
            ohSide = tSide;
            applyMode();
            requestLayout();
            invalidate();
            return;
        }
        if (before == ONE_HAND_OFF) ohSide = tSide;
        final float fromAmt = ohAmount;
        final float fromSide = ohSide;
        final float toAmt = tAmt;
        final float toSide = tSide;
        ohAnim = ValueAnimator.ofFloat(0f, 1f);
        ohAnim.setDuration(260);
        ohAnim.setInterpolator(new DecelerateInterpolator(1.6f));
        ohAnim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            ohAmount = fromAmt + (toAmt - fromAmt) * f;
            ohSide = fromSide + (toSide - fromSide) * f;
            requestLayout();
            invalidate();
        });
        ohAnim.start();
    }

    private void applyMode() {
        if (card == null) {
            super.setBackgroundColor(chromeColor);
            return;
        }
        if (floating) {

            super.setBackgroundColor(Color.TRANSPARENT);
        } else {
            super.setBackgroundColor(chromeColor);
        }
        card.setClipToOutline(floating);
        if (handle != null) handle.setVisibility(floating ? VISIBLE : GONE);
        invalidate();
    }

    private void dragFloat(float dx, float dy) {
        if (dragFreeX > 0) floatX = clamp01(floatX + dx / dragFreeX);
        if (dragFreeY > 0) floatY = clamp01(floatY + dy / dragFreeY);
        requestLayout();
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public boolean floatingTouchRect(Rect out) {
        if (!floating || card == null || card.getWidth() == 0) return false;
        int[] loc = new int[2];
        card.getLocationInWindow(loc);
        int pad = dp(2);
        out.set(loc[0] - pad, loc[1] - pad, loc[0] + card.getWidth() + pad, loc[1] + card.getHeight() + pad);
        return true;
    }

    private void drawToolbarBand(Canvas canvas, float offX, float offY) {
        if (toolbarView == null) {
            View tb = findViewById(R.id.toolbar);
            if (tb != null && tb.getParent() instanceof View) toolbarView = (View) tb.getParent();
        }
        View strip = toolbarView;
        if (strip == null || strip.getVisibility() != VISIBLE) return;
        glass.set(strip.getLeft() + offX, strip.getTop() + offY, strip.getRight() + offX, strip.getBottom() + offY);
        glassPaint.setStyle(Paint.Style.FILL);
        glassPaint.setColor(glassDark
                ? com.keyline.ime.theme.KeyboardTheme.GLASS_DARK
                : com.keyline.ime.theme.KeyboardTheme.GLASS_LIGHT);
        canvas.drawRect(glass, glassPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float l = 0, t = 0, r = getWidth(), b = getHeight();
        boolean cardReady = card != null && card.getWidth() > 0;
        if (floating && cardReady) {
            l = card.getLeft();
            t = card.getTop();
            r = card.getRight();
            b = card.getBottom();
        }
        int save = canvas.save();
        if (floating && cardReady) {
            clip.reset();
            clip.addRoundRect(l, t, r, b, dp(14), dp(14), Path.Direction.CW);
            canvas.clipPath(clip);
            chromePaint.setStyle(Paint.Style.FILL);
            chromePaint.setColor(chromeColor);
            canvas.drawRect(l, t, r, b, chromePaint);
        }
        if (image != null && !image.isRecycled()) {
            dst.set(l, t, r, b);
            BackgroundImageManager.drawCover(canvas, image, dst);
            if (dim > 0) canvas.drawColor(Math.round(255f * (dim / 100f)) << 24);
        }
        try {
            drawToolbarBand(canvas, cardReady ? card.getLeft() : 0, cardReady ? card.getTop() : 0);
        } catch (Throwable ignored) {
        }
        canvas.restoreToCount(save);
        if (floating && cardReady) {
            chromePaint.setStyle(Paint.Style.STROKE);
            chromePaint.setStrokeWidth(dp(1));
            chromePaint.setColor(glassDark ? 0x55FFFFFF : 0x44000000);
            canvas.drawRoundRect(l + 0.5f, t + 0.5f, r - 0.5f, b - 0.5f, dp(14), dp(14), chromePaint);
        }
        if (!floating && ohAmount > 0.01f) drawOneHandStrip(canvas);
    }

    private float stripAlpha() {
        return ohAmount * Math.abs(2f * ohSide - 1f);
    }

    private void drawOneHandStrip(Canvas canvas) {
        if (host == null || card == null) return;
        float a = stripAlpha();
        if (a <= 0.01f) return;
        layoutStripButtons();
        drawStripButton(canvas, swapRect, swapIcon, pressedButton == BTN_SWAP, a);
        drawStripButton(canvas, fullRect, fullIcon, pressedButton == BTN_FULL, a);
    }

    private void drawStripButton(Canvas canvas, RectF rect, Drawable icon, boolean pressed, float alpha) {
        chromePaint.setStyle(Paint.Style.FILL);
        int fillA = Math.round((pressed ? 0x44 : 0x22) * alpha);
        chromePaint.setColor((iconColor & 0x00FFFFFF) | (fillA << 24));
        canvas.drawRoundRect(rect, rect.width() / 2f, rect.width() / 2f, chromePaint);
        if (icon != null) {
            int s = dp(22);
            int cx = Math.round(rect.centerX());
            int cy = Math.round(rect.centerY());
            icon.setBounds(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
            DrawableCompat.setTint(icon, iconColor);
            icon.setAlpha(Math.round(255 * alpha));
            icon.draw(canvas);
        }
    }

    private int stripWidth() {
        return stripWidthFor(getWidth());
    }

    private void layoutStripButtons() {
        int w = getWidth();
        int sw = stripWidth();

        float cx = ohSide > 0.5f ? sw / 2f : w - sw / 2f;
        float cy = card.getTop() + host.getTop() + host.getHeight() / 2f;
        float half = Math.min(dp(22), sw / 2f - dp(5));
        float gap = dp(40);
        swapRect.set(cx - half, cy - gap - half, cx + half, cy - gap + half);
        fullRect.set(cx - half, cy + gap - half, cx + half, cy + gap + half);
    }

    private int hitButton(float x, float y) {
        if (stripAlpha() < 0.6f) return BTN_NONE;
        if (swapRect.contains(x, y)) return BTN_SWAP;
        if (fullRect.contains(x, y)) return BTN_FULL;
        return BTN_NONE;
    }

    private void layoutGrips() {
        if (card == null || host == null) return;
        if (floating) {
            float l = card.getLeft();
            float t = card.getTop();
            floatGrip.set(l, t, l + dp(44), t + dp(30));
        } else if (ohAmount > 0.01f && stripAlpha() > 0.6f) {
            boolean keysRight = ohSide > 0.5f;
            float edge = keysRight ? card.getLeft() : card.getRight();
            float cy = card.getTop() + host.getTop() + host.getHeight() / 2f;
            float out = keysRight ? -dp(28) : dp(28);
            float in = keysRight ? dp(8) : -dp(8);
            oneHandGrip.set(Math.min(edge + out, edge + in), cy - dp(34), Math.max(edge + out, edge + in), cy + dp(34));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            layoutGrips();
            if (floating && floatGrip.contains(e.getX(), e.getY())) {
                drag = DRAG_FLOAT;
                dragLeft = card.getRight();
                dragTop = card.getTop();
                return true;
            }
            if (!floating && ohAmount > 0.01f && stripAlpha() > 0.6f && oneHandGrip.contains(e.getX(), e.getY())) {
                drag = DRAG_ONE_HAND;
                return true;
            }
        }
        return super.onInterceptTouchEvent(e);
    }

    private boolean handleDrag(MotionEvent e) {
        int w = getWidth();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                if (drag == DRAG_ONE_HAND) {
                    float strip = ohSide > 0.5f ? e.getX() : w - e.getX();
                    int pct = Math.round(100f * (1f - strip / w));
                    liveWidthPct = Math.max(60, Math.min(90, pct));
                    requestLayout();
                    invalidate();
                } else {
                    boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                    float base = w * (land ? 0.55f : 0.82f);
                    float cw = dragLeft - e.getX();
                    liveScale = Math.max(0.6f, Math.min(1.3f, cw / base));
                    requestLayout();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Prefs prefs = Prefs.get(getContext());
                if (drag == DRAG_ONE_HAND && liveWidthPct > 0) {
                    prefs.oneHandWidth(liveWidthPct);
                } else if (drag == DRAG_FLOAT && liveScale > 0) {
                    prefs.floatScale(Math.round(liveScale * 100f));
                    if (callbacks != null) callbacks.onFloatMoved(floatX, floatY);
                }
                drag = DRAG_NONE;
                liveWidthPct = -1;
                liveScale = -1f;
                requestLayout();
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (card == null || card.getWidth() <= 0) return;
        layoutGrips();
        gripPaint.setStyle(Paint.Style.STROKE);
        gripPaint.setStrokeCap(Paint.Cap.ROUND);
        gripPaint.setStrokeWidth(dp(2));
        gripPaint.setColor((iconColor & 0x00FFFFFF) | (drag != DRAG_NONE ? 0xCC000000 : 0x88000000));
        if (floating) {
            float x = floatGrip.left + dp(12);
            float y = floatGrip.top + dp(12);
            canvas.drawLine(x + dp(9), y + dp(1), x + dp(1), y - dp(7), gripPaint);
            canvas.drawLine(x + dp(9), y + dp(7), x - dp(5), y - dp(7), gripPaint);
        } else if (ohAmount > 0.01f && stripAlpha() > 0.6f) {
            float cx = oneHandGrip.centerX();
            float cy = oneHandGrip.centerY();
            gripPaint.setStrokeWidth(dp(4));
            canvas.drawLine(cx, cy - dp(16), cx, cy + dp(16), gripPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (drag != DRAG_NONE) return handleDrag(e);
        if (floating || (oneHand == ONE_HAND_OFF && ohAmount <= 0.01f)) return super.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                layoutStripButtons();
                pressedButton = hitButton(e.getX(), e.getY());
                if (pressedButton != BTN_NONE) {
                    invalidate();
                    return true;
                }
                return super.onTouchEvent(e);
            case MotionEvent.ACTION_UP:
                if (pressedButton != BTN_NONE) {
                    int hit = hitButton(e.getX(), e.getY());
                    int was = pressedButton;
                    pressedButton = BTN_NONE;
                    invalidate();
                    if (hit == was && callbacks != null) {
                        if (was == BTN_SWAP) callbacks.onOneHandSwap();
                        else callbacks.onOneHandFull();
                    }
                    return true;
                }
                return super.onTouchEvent(e);
            case MotionEvent.ACTION_CANCEL:
                if (pressedButton != BTN_NONE) {
                    pressedButton = BTN_NONE;
                    invalidate();
                    return true;
                }
                return super.onTouchEvent(e);
            default:
                return pressedButton != BTN_NONE || super.onTouchEvent(e);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) width = getResources().getDisplayMetrics().widthPixels;
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int docked = Math.round(screenH * Prefs.get(getContext()).height(land));
        int min = dp(land ? 150 : 180);
        if (docked < min) docked = min;

        int height = docked;
        if (card != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();
            if (floating) {
                int avail = MeasureSpec.getSize(heightMeasureSpec);
                int full = avail > dp(300) ? avail : screenH;
                height = Math.max(full, docked);
                int handleH = dp(22);
                float fs = liveScale > 0 ? liveScale : Prefs.get(getContext()).floatScale() / 100f;
                int cw = Math.min(width, Math.round(width * (land ? 0.55f : 0.82f) * fs));
                int ch = Math.round(docked * 0.92f * fs) + handleH;
                if (ch > height) ch = height;
                float freeX = Math.max(0, width - cw);
                float freeY = Math.max(0, height - ch);
                dragFreeX = freeX;
                dragFreeY = freeY;
                lp.width = cw;
                lp.height = ch;
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
                if (drag == DRAG_FLOAT) {
                    if (freeX > 0) floatX = clamp01((dragLeft - cw) / freeX);
                    if (freeY > 0) floatY = clamp01(dragTop / freeY);
                }
                lp.leftMargin = Math.round(freeX * floatX);
                lp.topMargin = Math.round(freeY * floatY);
                lp.rightMargin = 0;
                lp.bottomMargin = 0;
            } else {

                int strip = Math.round(stripWidthFor(width) * ohAmount);
                lp.width = LayoutParams.MATCH_PARENT;
                lp.height = LayoutParams.MATCH_PARENT;
                lp.gravity = android.view.Gravity.NO_GRAVITY;
                lp.leftMargin = Math.round(strip * ohSide);
                lp.topMargin = 0;
                lp.rightMargin = strip - lp.leftMargin;
                lp.bottomMargin = 0;
            }
            if (host != null && host.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams hp = (LinearLayout.LayoutParams) host.getLayoutParams();
                hp.leftMargin = 0;
                hp.rightMargin = 0;
            }
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    private int stripWidthFor(int width) {
        int pct = liveWidthPct > 0 ? liveWidthPct : Prefs.get(getContext()).oneHandWidth();
        return Math.round(width * (100 - pct) / 100f);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
