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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class CropView extends View {
    private Bitmap bitmap;
    private float aspect = 2.4f;
    private float scale = 1f;
    private float tx;
    private float ty;
    private final RectF crop = new RectF();
    private final Paint shade = new Paint();
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector scaler;
    private float lastX;
    private float lastY;
    private boolean ready;

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        shade.setColor(0x99000000);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(dp(2));
        frame.setColor(Color.WHITE);
        scaler = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float next = scale * detector.getScaleFactor();
                float min = minScale();
                if (next < min) next = min;
                if (next > min * 6f) next = min * 6f;
                float cx = crop.centerX();
                float cy = crop.centerY();
                tx = cx - (cx - tx) * (next / scale);
                ty = cy - (cy - ty) * (next / scale);
                scale = next;
                clamp();
                invalidate();
                return true;
            }
        });
    }

    public void setBitmap(Bitmap bitmap, float aspect) {
        this.bitmap = bitmap;
        this.aspect = aspect <= 0 ? 2f : aspect;
        ready = false;
        if (getWidth() > 0 && getHeight() > 0) {
            layoutCrop();
            if (bitmap != null) resetTransform();
        }
        requestLayout();
        invalidate();
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutCrop();
        if (bitmap != null) resetTransform();
    }

    private void layoutCrop() {
        float maxW = getWidth() * 0.92f;
        float maxH = getHeight() * 0.78f;
        float cw = maxW;
        float ch = cw / aspect;
        if (ch > maxH) {
            ch = maxH;
            cw = ch * aspect;
        }
        float left = (getWidth() - cw) / 2f;
        float top = (getHeight() - ch) / 2f;
        crop.set(left, top, left + cw, top + ch);
    }

    private void resetTransform() {
        scale = minScale();
        float dw = bitmap.getWidth() * scale;
        float dh = bitmap.getHeight() * scale;
        tx = crop.centerX() - dw / 2f;
        ty = crop.centerY() - dh / 2f;
        ready = true;
        clamp();
    }

    private float minScale() {
        if (bitmap == null || crop.width() == 0) return 1f;
        return Math.max(crop.width() / bitmap.getWidth(), crop.height() / bitmap.getHeight());
    }

    private void clamp() {
        if (bitmap == null) return;
        float dw = bitmap.getWidth() * scale;
        float dh = bitmap.getHeight() * scale;
        if (tx > crop.left) tx = crop.left;
        if (ty > crop.top) ty = crop.top;
        if (tx + dw < crop.right) tx = crop.right - dw;
        if (ty + dh < crop.bottom) ty = crop.bottom - dh;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF111111);
        if (bitmap == null || !ready) return;
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.restore();
        canvas.drawRect(0, 0, getWidth(), crop.top, shade);
        canvas.drawRect(0, crop.bottom, getWidth(), getHeight(), shade);
        canvas.drawRect(0, crop.top, crop.left, crop.bottom, shade);
        canvas.drawRect(crop.right, crop.top, getWidth(), crop.bottom, shade);
        canvas.drawRect(crop, frame);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaler.onTouchEvent(event);
        if (event.getPointerCount() == 1 && !scaler.isInProgress()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    tx += event.getX() - lastX;
                    ty += event.getY() - lastY;
                    lastX = event.getX();
                    lastY = event.getY();
                    clamp();
                    invalidate();
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    public Bitmap export(int outW, int outH) {
        if (bitmap == null || !ready || scale <= 0f) return null;
        float x = (crop.left - tx) / scale;
        float y = (crop.top - ty) / scale;
        float w = crop.width() / scale;
        float h = crop.height() / scale;
        int left = Math.max(0, Math.min(bitmap.getWidth() - 1, (int) Math.floor(x)));
        int top = Math.max(0, Math.min(bitmap.getHeight() - 1, (int) Math.floor(y)));
        int right = Math.max(left + 1, Math.min(bitmap.getWidth(), (int) Math.ceil(x + w)));
        int bottom = Math.max(top + 1, Math.min(bitmap.getHeight(), (int) Math.ceil(y + h)));
        Bitmap sub = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
        Bitmap scaled = Bitmap.createScaledBitmap(sub, Math.max(1, outW), Math.max(1, outH), true);
        if (scaled != sub) sub.recycle();
        return scaled;
    }

    private float dp(int v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
