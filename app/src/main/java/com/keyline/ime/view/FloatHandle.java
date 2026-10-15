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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;

public class FloatHandle extends View {
    public interface Listener {
        void onDragBy(float dx, float dy);
        void onDragEnd();
        void onDock();
    }

    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pillRect = new RectF();
    private final Drawable dockIcon;
    private Listener listener;
    private float lastX;
    private float lastY;
    private boolean dragging;
    private boolean downOnDock;
    private int color = 0xFF888888;

    public FloatHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        Drawable d = AppCompatResources.getDrawable(context, R.drawable.ic_keyboard);
        dockIcon = d == null ? null : DrawableCompat.wrap(d.mutate());
        setContentDescription(context.getString(R.string.cd_drag));
    }

    public void setListener(Listener l) { listener = l; }

    public void setColor(int c) {
        color = c;
        invalidate();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private boolean inDockZone(float x) {

        return x > getWidth() - dp(44);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        pill.setColor((color & 0x00FFFFFF) | 0x77000000);
        pillRect.set(cx - dp(22), cy - dp(2), cx + dp(22), cy + dp(2));
        canvas.drawRoundRect(pillRect, dp(2), dp(2), pill);
        if (dockIcon != null) {
            int s = Math.round(dp(18));
            int r = getWidth() - Math.round(dp(12));
            int t = Math.round(cy - s / 2f);
            dockIcon.setBounds(r - s, t, r, t + s);
            DrawableCompat.setTint(dockIcon, color);
            dockIcon.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getRawX();
                lastY = e.getRawY();
                dragging = false;
                downOnDock = inDockZone(e.getX());
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (downOnDock) return true;
                float dx = e.getRawX() - lastX;
                float dy = e.getRawY() - lastY;
                if (!dragging && Math.abs(dx) + Math.abs(dy) < dp(3)) return true;
                dragging = true;
                lastX = e.getRawX();
                lastY = e.getRawY();
                if (listener != null) listener.onDragBy(dx, dy);
                return true;
            case MotionEvent.ACTION_UP:
                if (downOnDock && inDockZone(e.getX()) && listener != null) listener.onDock();
                else if (dragging && listener != null) listener.onDragEnd();
                dragging = false;
                downOnDock = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragging && listener != null) listener.onDragEnd();
                dragging = false;
                downOnDock = false;
                return true;
            default:
                return true;
        }
    }
}
