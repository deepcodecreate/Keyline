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
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.keyline.ime.theme.KeyboardTheme;

public final class PopupController {
    private final Context context;
    private PopupWindow preview;
    private TextView previewText;
    private PopupWindow alternates;
    private LinearLayout altRow;
    private TextView[] altViews = new TextView[0];
    private String[] altValues = new String[0];
    private int selected = 0;
    private KeyboardTheme theme = new KeyboardTheme();

    public PopupController(Context context) {
        this.context = context;
    }

    public void setTheme(KeyboardTheme t) {
        if (t != null) theme = t;
    }

    public void showPreview(View anchor, float centerX, float topY, String text) {
        if (anchor.getWindowToken() == null) return;
        ensurePreview();
        previewText.setText(text);
        previewText.setTextColor(theme.popupText);
        ((GradientDrawable) previewText.getBackground()).setColor(theme.popup);
        previewText.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int w = Math.max(previewText.getMeasuredWidth(), dp(48));
        int h = Math.max(previewText.getMeasuredHeight(), dp(52));
        preview.setWidth(w);
        preview.setHeight(h);
        int x = clampedLeft(anchor, centerX, w);
        int y = Math.round(topY - h - dp(8));
        int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        int windowX = loc[0] + x;
        int windowY = loc[1] + y;
        if (preview.isShowing()) preview.update(windowX, windowY, w, h);
        else preview.showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, windowX, windowY);
    }

    public void hidePreview() {
        if (preview != null && preview.isShowing()) preview.dismiss();
    }

    public void showAlternates(View anchor, float centerX, float topY, String[] values) {
        if (anchor.getWindowToken() == null || values == null || values.length == 0) return;
        hidePreview();
        altValues = values;
        selected = 0;
        altRow = new LinearLayout(context);
        altRow.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dp(6);
        altRow.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(theme.popup);
        bg.setCornerRadius(dp(18));
        altRow.setBackground(bg);
        altViews = new TextView[values.length];
        for (int i = 0; i < values.length; i++) {
            TextView tv = new TextView(context);
            tv.setText(values[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setMinWidth(dp(42));
            tv.setMinHeight(dp(44));
            tv.setPadding(dp(6), dp(4), dp(6), dp(4));
            altRow.addView(tv);
            altViews[i] = tv;
        }
        paintAlt();
        alternates = new PopupWindow(altRow, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, false);
        alternates.setElevation(dp(8));
        alternates.setClippingEnabled(false);
        altRow.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int w = altRow.getMeasuredWidth();
        int h = altRow.getMeasuredHeight();
        alternates.setWidth(w);
        alternates.setHeight(h);
        int x = clampedLeft(anchor, centerX, w);
        int y = Math.round(topY - h - dp(10));
        int[] windowLoc = new int[2];
        anchor.getLocationInWindow(windowLoc);
        int windowX = windowLoc[0] + x;
        int windowY = windowLoc[1] + y;
        try {
            alternates.showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, windowX, windowY);
        } catch (Exception ignored) {}
    }

    public void highlightAt(float screenX) {
        if (altViews.length == 0) return;
        int idx = 0;
        float nearest = Float.MAX_VALUE;
        for (int i = 0; i < altViews.length; i++) {
            TextView cell = altViews[i];
            int[] location = new int[2];
            cell.getLocationOnScreen(location);
            float distance = Math.abs(screenX - (location[0] + cell.getWidth() / 2f));
            if (distance < nearest) {
                nearest = distance;
                idx = i;
            }
        }
        if (idx != selected) {
            selected = idx;
            paintAlt();
        }
    }

    public String selectedValue() {
        if (selected >= 0 && selected < altValues.length) return altValues[selected];
        return null;
    }

    public boolean alternatesShowing() {
        return alternates != null && alternates.isShowing();
    }

    public void hideAlternates() {
        if (alternates != null && alternates.isShowing()) alternates.dismiss();
        alternates = null;
        altViews = new TextView[0];
        altValues = new String[0];
    }

    public void hideAll() {
        hidePreview();
        hideAlternates();
    }

    private void paintAlt() {
        for (int i = 0; i < altViews.length; i++) {
            TextView tv = altViews[i];
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(dp(12));
            if (i == selected) {
                d.setColor(theme.accent);
                tv.setTextColor(0xFFFFFFFF);
            } else {
                d.setColor(0x00000000);
                tv.setTextColor(theme.popupText);
            }
            tv.setBackground(d);
        }
    }

    private void ensurePreview() {
        if (preview != null) return;
        previewText = new TextView(context);
        previewText.setGravity(Gravity.CENTER);
        previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        previewText.setPadding(dp(14), dp(8), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(theme.popup);
        previewText.setBackground(bg);
        preview = new PopupWindow(previewText, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, false);
        preview.setElevation(dp(10));
        preview.setClippingEnabled(false);
    }

    private int clampedLeft(View anchor, float centerX, int popupWidth) {
        int margin = dp(4);
        int maxLeft = Math.max(margin, anchor.getWidth() - popupWidth - margin);
        return Math.max(margin, Math.min(Math.round(centerX - popupWidth / 2f), maxLeft));
    }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }
}
