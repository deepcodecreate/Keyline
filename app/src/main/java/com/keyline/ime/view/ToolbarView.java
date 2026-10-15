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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;

public class ToolbarView extends HorizontalScrollView {
    public interface Listener {
        void onClipboard();
        void onEmoji();
        void onMic();
        void onTranslate();
        void onUndo();
        void onCollapse();
        void onMenu();
    }

    private final LinearLayout row;
    private final ImageButton mic;
    private final ImageButton translate;
    private final ImageButton undo;
    private final ImageButton clip;
    private final ImageButton emoji;
    private final ImageButton collapse;
    private KeyboardTheme theme;
    private Listener listener;
    private boolean keysMode = true;

    public ToolbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);
        setFillViewport(true);

        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mic = btn(R.drawable.ic_mic, R.string.cd_mic, v -> { if (listener != null) listener.onMic(); });
        translate = btn(R.drawable.ic_translate, R.string.cd_translate, v -> { if (listener != null) listener.onTranslate(); });
        undo = btn(R.drawable.ic_undo, R.string.cd_undo, v -> { if (listener != null) listener.onUndo(); });
        clip = btn(R.drawable.ic_clipboard, R.string.cd_clipboard, v -> { if (listener != null) listener.onClipboard(); });
        emoji = btn(R.drawable.ic_emoji, R.string.cd_emoji, v -> { if (listener != null) listener.onEmoji(); });
        collapse = btn(R.drawable.ic_apps, R.string.cd_menu, v -> {
            if (listener == null) return;
            if (keysMode) listener.onMenu(); else listener.onCollapse();
        });
    }

    public void setListener(Listener l) { this.listener = l; }

    public void setKeysMode(boolean keys) {
        if (keysMode == keys) return;
        keysMode = keys;
        collapse.setImageDrawable(AppCompatResources.getDrawable(getContext(), keys ? R.drawable.ic_apps : R.drawable.ic_arrow_right));
        if (collapse.getDrawable() != null) collapse.getDrawable().mutate();
        collapse.setContentDescription(getContext().getString(keys ? R.string.cd_menu : R.string.cd_collapse));
        tint(collapse, false);
    }

    public void apply(KeyboardTheme t, boolean clipOn, boolean emojiOn, boolean translateOn, boolean micOn) {
        this.theme = t;
        setBackgroundColor(Color.TRANSPARENT);
        row.setBackgroundColor(Color.TRANSPARENT);
        tint(mic, micOn);
        tint(translate, false);
        tint(undo, false);
        tint(clip, clipOn);
        tint(emoji, emojiOn);
        tint(collapse, false);
        ring(mic, micOn);
        ring(translate, false);
        ring(undo, false);
        ring(clip, clipOn);
        ring(emoji, emojiOn);
        ring(collapse, false);
    }

    private void ring(ImageButton b, boolean active) {
        if (theme == null) return;
        int icon = theme.toolbarIconColor();
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(active ? withAlpha(theme.accent, 0x33) : withAlpha(icon, 0x26));
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.BLACK);
        b.setBackground(new RippleDrawable(
                ColorStateList.valueOf(withAlpha(theme.text, 0x33)), disc, mask));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void tint(ImageButton b, boolean active) {
        if (theme == null || b.getDrawable() == null) return;
        DrawableCompat.setTint(b.getDrawable(), active ? theme.accent : theme.toolbarIconColor());
    }

    private ImageButton btn(int icon, int cd, View.OnClickListener click) {
        ImageButton b = new ImageButton(getContext());
        b.setImageDrawable(AppCompatResources.getDrawable(getContext(), icon));
        if (b.getDrawable() != null) b.getDrawable().mutate();
        b.setContentDescription(getContext().getString(cd));
        b.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(6), dp(6), dp(6), dp(6));
        b.setOnClickListener(click);
        FrameLayout cell = new FrameLayout(getContext());
        cell.addView(b, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
