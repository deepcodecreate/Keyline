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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.Locale;

public class MenuPanel extends ScrollView {
    public interface Listener {
        void onOneHand();
        void onTextEdit();
        void onFloating();
        void onEmoji();
        void onNextLanguage();
        void onCredentials();
        void onTheme();
        void onSettings();
    }

    private static final int COLS = 4;

    private static final class Tile {
        final FrameLayout box;
        final ImageView icon;
        final TextView label;
        final int labelRes;
        boolean active;

        Tile(FrameLayout box, ImageView icon, TextView label, int labelRes) {
            this.labelRes = labelRes;
            this.box = box;
            this.icon = icon;
            this.label = label;
        }
    }

    private final LinearLayout column;
    private final Tile[] tiles;
    private Listener listener;
    private KeyboardTheme theme;
    private boolean oneHandOn;
    private boolean floatingOn;
    private String langTag = "";
    private Context strCtx;

    public MenuPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(8), dp(6), dp(8), dp(6));
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tiles = new Tile[] {
                tile(R.drawable.ic_one_hand, R.string.menu_one_hand, () -> { if (listener != null) listener.onOneHand(); }),
                tile(R.drawable.ic_text_edit, R.string.menu_text_edit, () -> { if (listener != null) listener.onTextEdit(); }),
                tile(R.drawable.ic_floating, R.string.menu_floating, () -> { if (listener != null) listener.onFloating(); }),
                tile(R.drawable.ic_palette, R.string.menu_theme, () -> { if (listener != null) listener.onTheme(); }),
                tile(R.drawable.ic_emoji, R.string.menu_emoji, () -> { if (listener != null) listener.onEmoji(); }),
                tile(R.drawable.ic_globe, R.string.menu_next_language, () -> { if (listener != null) listener.onNextLanguage(); }),
                tile(R.drawable.ic_password, R.string.menu_credentials, () -> { if (listener != null) listener.onCredentials(); }),
                tile(R.drawable.ic_settings, R.string.menu_settings, () -> { if (listener != null) listener.onSettings(); }),
        };
        LinearLayout row = null;
        for (int i = 0; i < tiles.length; i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                column.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(92)));
            }
            row.addView(tiles[i].box, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }
    }

    private Tile tile(int iconRes, int labelRes, Runnable action) {
        Context c = getContext();
        FrameLayout box = new FrameLayout(c);
        box.setClickable(true);
        box.setFocusable(false);
        box.setOnClickListener(v -> action.run());

        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(inner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout chip = new FrameLayout(c);
        ImageView icon = new ImageView(c);
        icon.setImageDrawable(AppCompatResources.getDrawable(c, iconRes));
        if (icon.getDrawable() != null) icon.getDrawable().mutate();
        chip.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(64), dp(44));
        clp.topMargin = dp(8);
        inner.addView(chip, clp);

        TextView label = new TextView(c);
        label.setText(labelRes);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(5);
        llp.leftMargin = dp(2);
        llp.rightMargin = dp(2);
        inner.addView(label, llp);

        box.setTag(chip);
        return new Tile(box, icon, label, labelRes);
    }

    public void setListener(Listener l) { listener = l; }

    public void setLanguage(Locale locale) {
        if (locale == null) locale = Locale.US;
        String tag = locale.toLanguageTag();
        if (tag.equals(langTag)) return;
        langTag = tag;
        Configuration cfg = new Configuration(getContext().getResources().getConfiguration());
        cfg.setLocale(locale);
        cfg.setLayoutDirection(locale);
        strCtx = getContext().createConfigurationContext(cfg);
        setLayoutDirection(TextUtils.getLayoutDirectionFromLocale(locale));
        for (Tile t : tiles) t.label.setText(strCtx.getString(t.labelRes));
    }

    public void setStates(boolean oneHand, boolean floating) {
        oneHandOn = oneHand;
        floatingOn = floating;
        restyle();
    }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(Color.TRANSPARENT);
        restyle();
    }

    private void restyle() {
        if (theme == null) return;
        for (int i = 0; i < tiles.length; i++) {
            boolean active = (i == 0 && oneHandOn) || (i == 2 && floatingOn);
            Tile t = tiles[i];
            t.active = active;
            View chip = (View) t.box.getTag();
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(active ? withAlpha(theme.accent, 0x38) : withAlpha(theme.toolbarIconColor(), 0x1F));
            if (active) bg.setStroke(dp(2), theme.accent);
            chip.setBackground(bg);
            if (t.icon.getDrawable() != null) {
                DrawableCompat.setTint(t.icon.getDrawable(), active ? theme.accent : theme.toolbarIconColor());
            }
            t.label.setTextColor(active ? theme.accent : theme.toolbarIconColor());
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
