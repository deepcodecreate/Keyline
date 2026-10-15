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

package com.keyline.ime.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.keyline.ime.theme.BackgroundImageManager;
import com.keyline.ime.theme.KeyboardTheme;

public class KeyboardPreviewView extends View {
    private static final String[][] ROWS = {
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L"},
            {"Z", "X", "C", "V", "B", "N", "M"}
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF kb = new RectF();
    private final RectF keys = new RectF();
    private final RectF key = new RectF();
    private KeyboardTheme theme;
    private Bitmap background;
    private int alpha = 100;
    private boolean hide;
    private Typeface font;
    private float fontScale = 1f;
    private int dim;

    public KeyboardPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void bind(KeyboardTheme theme, Bitmap background, int alpha, boolean hide, int dim) {
        this.theme = theme;
        this.background = background;
        this.alpha = alpha;
        this.hide = hide;
        this.dim = dim;
        invalidate();
    }

    public void setFont(Typeface tf) {
        font = tf;
        invalidate();
    }

    public void setFontScale(float scale) {
        fontScale = scale;
        invalidate();
    }

    private static final int KIND_NORMAL = 0;
    private static final int KIND_FIXED = 1;
    private static final int KIND_ENTER = 2;

    private void drawKey(Canvas canvas, RectF r, int base, String label) {
        drawKey(canvas, r, base, label,
                base == theme.special ? theme.specialTextColor() : theme.text, KIND_NORMAL);
    }

    private void drawKey(Canvas canvas, RectF r, int base, String label, int labelColor, int kind) {
        if (kind == KIND_FIXED && !hide) kind = KIND_NORMAL;
        if (kind == KIND_NORMAL) {
            if (!hide && alpha > 0) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(theme.withAlpha(base, alpha));
                canvas.drawRoundRect(r, dp(5), dp(5), paint);
                if (theme.stroke != 0 && alpha > 30) strokeKey(canvas, r, dp(5));
            }
        } else {
            float rad = Math.min(r.width(), r.height()) / 2f;
            if (kind == KIND_FIXED && hide) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor((theme.toolbarIconColor() & 0x00FFFFFF) | (0x66 << 24));
                canvas.drawRoundRect(r, rad, rad, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(theme.withAlpha(base, 100));
                canvas.drawRoundRect(r, rad, rad, paint);
                if (theme.stroke != 0) strokeKey(canvas, r, rad);
            }
        }
        if (label != null) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(labelColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(font);
            paint.setTextSize(Math.min(r.height() * 0.42f, dp(14)) * fontScale);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(label, r.centerX(), r.centerY() - (fm.ascent + fm.descent) / 2f, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (theme == null) return;
        try {
            drawPreview(canvas);
        } catch (RuntimeException ignored) {
        }
    }

    private void drawPreview(Canvas canvas) {
        float r = dp(16);
        kb.set(dp(2), dp(2), getWidth() - dp(2), getHeight() - dp(2));

        canvas.save();
        clip.reset();
        clip.addRoundRect(kb, r, r, Path.Direction.CW);
        canvas.clipPath(clip);

        float stripH = kb.height() * 0.15f;
        boolean hasImage = background != null && !background.isRecycled();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.background);
        canvas.drawRect(kb, paint);
        if (hasImage) {
            BackgroundImageManager.drawCover(canvas, background, kb);
            if (dim > 0) {
                paint.setColor(Math.round(255f * dim / 100f) << 24);
                canvas.drawRect(kb, paint);
            }
        } else {
            paint.setColor(theme.toolbar);
            canvas.drawRect(kb.left, kb.top, kb.right, kb.top + stripH, paint);
        }
        paint.setColor(theme.toolbarIconColor());
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(kb.left + dp(22) + i * dp(30), kb.top + stripH / 2f, dp(5), paint);
        }
        keys.set(kb.left, kb.top + stripH, kb.right, kb.bottom);

        float gap = dp(4);
        float pad = dp(5);
        float rowH = (keys.height() - pad * 2 - gap * 3) / 4f;
        float top = keys.top + pad;
        float unit = (keys.width() - pad * 2 - gap * 9) / 10f;
        for (int row = 0; row < 3; row++) {
            String[] letters = ROWS[row];
            boolean wide = row == 2;
            float rowW = letters.length * unit + (letters.length - 1) * gap;
            float x = keys.left + (keys.width() - rowW) / 2f;
            if (wide) {
                float sw = unit * 1.4f;
                key.set(keys.left + pad, top, keys.left + pad + sw, top + rowH);
                drawKey(canvas, key, theme.special, null);
                key.set(keys.right - pad - sw, top, keys.right - pad, top + rowH);
                drawKey(canvas, key, theme.special, null);
            }
            for (String letter : letters) {
                key.set(x, top, x + unit, top + rowH);
                drawKey(canvas, key, theme.key, letter);
                x += unit + gap;
            }
            top += rowH + gap;
        }
        float left = keys.left + pad;
        float inset = rowH * 0.185f;
        float sw = unit * 1.5f;
        key.set(left, top + inset, left + sw, top + rowH - inset);
        drawKey(canvas, key, theme.special, "123", theme.specialTextColor(), KIND_FIXED);
        left += sw + gap;
        key.set(left, top, left + unit, top + rowH);
        drawKey(canvas, key, theme.special, null);
        left += unit + gap;
        float enterW = unit * 1.5f;
        key.set(left, top + inset, keys.right - pad - enterW - gap, top + rowH - inset);
        drawKey(canvas, key, theme.key, null, theme.text, KIND_FIXED);
        key.set(keys.right - pad - enterW, top + inset, keys.right - pad, top + rowH - inset);
        drawKey(canvas, key, theme.actionKeyColor(), "\u21B5", theme.actionKeyTextColor(), KIND_ENTER);
        canvas.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(theme.stroke == 0 ? 0x22000000 : theme.stroke);
        canvas.drawRoundRect(kb, r, r, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void strokeKey(Canvas canvas, RectF r, float rad) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(theme.stroke);
        canvas.drawRoundRect(r, rad, rad, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private float dp(int v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
