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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    public interface Listener {
        void onColor(float hue, float sat, float val);
    }

    private final float[] hsv = {0f, 1f, 1f};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF square = new RectF();
    private final RectF strip = new RectF();
    private final float density = getResources().getDisplayMetrics().density;
    private Shader hueShader;
    private Shader svShader;
    private float shaderHue = -1f;
    private int touching;
    private Listener listener;

    public ColorPickerView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setListener(Listener l) { listener = l; }

    public void setHsv(float h, float s, float v) {
        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = Math.round(w * 0.62f + 56 * density);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float pad = 4 * density;
        float stripH = 28 * density;
        square.set(pad, pad, w - pad, h - stripH - 20 * density);
        strip.set(pad, h - stripH - pad, w - pad, h - pad);
        int[] colors = new int[7];
        for (int i = 0; i < 7; i++) colors[i] = Color.HSVToColor(new float[]{i * 60f, 1f, 1f});
        hueShader = new LinearGradient(strip.left, 0, strip.right, 0, colors, null, Shader.TileMode.CLAMP);
        shaderHue = -1f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (square.isEmpty()) return;
        if (shaderHue != hsv[0]) {
            shaderHue = hsv[0];
            Shader sat = new LinearGradient(square.left, 0, square.right, 0, Color.WHITE,
                    Color.HSVToColor(new float[]{hsv[0], 1f, 1f}), Shader.TileMode.CLAMP);
            Shader val = new LinearGradient(0, square.top, 0, square.bottom, Color.TRANSPARENT,
                    Color.BLACK, Shader.TileMode.CLAMP);
            svShader = new ComposeShader(sat, val, PorterDuff.Mode.SRC_OVER);
        }
        float r = 10 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(svShader);
        canvas.drawRoundRect(square, r, r, paint);
        paint.setShader(hueShader);
        canvas.drawRoundRect(strip, strip.height() / 2f, strip.height() / 2f, paint);
        paint.setShader(null);

        float cx = square.left + hsv[1] * square.width();
        float cy = square.top + (1f - hsv[2]) * square.height();
        thumb(canvas, cx, cy, Color.HSVToColor(hsv));
        float hx = strip.left + hsv[0] / 360f * strip.width();
        thumb(canvas, hx, strip.centerY(), Color.HSVToColor(new float[]{hsv[0], 1f, 1f}));
    }

    private void thumb(Canvas canvas, float x, float y, int fill) {
        float rad = 11 * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawCircle(x, y, rad, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * density);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, rad, paint);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0x66000000);
        canvas.drawCircle(x, y, rad + 1.5f * density, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (square.contains(e.getX(), e.getY())) touching = 1;
                else if (e.getY() >= strip.top - 10 * density) touching = 2;
                else touching = 0;
                if (touching == 0) return false;
                getParent().requestDisallowInterceptTouchEvent(true);
                update(e);
                return true;
            case MotionEvent.ACTION_MOVE:
                update(e);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = 0;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return true;
        }
    }

    private void update(MotionEvent e) {
        if (touching == 1) {
            hsv[1] = clamp((e.getX() - square.left) / square.width());
            hsv[2] = 1f - clamp((e.getY() - square.top) / square.height());
        } else if (touching == 2) {
            hsv[0] = clamp((e.getX() - strip.left) / strip.width()) * 360f;
        }
        invalidate();
        if (listener != null) listener.onColor(hsv[0], hsv[1], hsv[2]);
    }

    private static float clamp(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
}
