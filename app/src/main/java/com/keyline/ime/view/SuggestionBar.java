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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.List;

public class SuggestionBar extends LinearLayout {
    public interface Listener {
        void onPick(String word);
        void onMic();
        void onMenu();
    }

    private final TextView[] slots = new TextView[3];
    private final View[] dividers = new View[2];
    private final ImageButton mic;
    private final ImageButton menu;
    private Listener listener;
    private KeyboardTheme theme;
    private boolean micActive;

    public SuggestionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        mic = iconButton(R.drawable.ic_mic, R.string.cd_mic);
        mic.setOnClickListener(v -> { if (listener != null) listener.onMic(); });
        LayoutParams mlp = new LayoutParams(dp(30), dp(30));
        mlp.setMarginStart(dp(6));
        mlp.setMarginEnd(dp(4));
        addView(mic, mlp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        addView(row, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        for (int i = 0; i < 3; i++) {
            TextView tv = new TextView(context);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setTypeface(Typeface.DEFAULT);
            tv.setSingleLine(true);
            int pad = dp(4);
            tv.setPadding(pad, 0, pad, 0);
            row.addView(tv, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            slots[i] = tv;
            final int idx = i;
            tv.setOnClickListener(v -> {
                Object tag = slots[idx].getTag();
                if (tag instanceof String && listener != null) listener.onPick((String) tag);
            });
            if (i < 2) {
                View d = new View(context);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(1), dp(24));
                dlp.gravity = Gravity.CENTER_VERTICAL;
                row.addView(d, dlp);
                dividers[i] = d;
            }
        }

        menu = iconButton(R.drawable.ic_apps, R.string.cd_menu);
        menu.setOnClickListener(v -> { if (listener != null) listener.onMenu(); });
        LayoutParams nlp = new LayoutParams(dp(30), dp(30));
        nlp.setMarginStart(dp(4));
        nlp.setMarginEnd(dp(6));
        addView(menu, nlp);
    }

    private ImageButton iconButton(int icon, int cd) {
        ImageButton b = new ImageButton(getContext());
        b.setImageDrawable(AppCompatResources.getDrawable(getContext(), icon));
        if (b.getDrawable() != null) b.getDrawable().mutate();
        b.setContentDescription(getContext().getString(cd));
        b.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(6), dp(6), dp(6), dp(6));
        return b;
    }

    public void setListener(Listener l) { listener = l; }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(Color.TRANSPARENT);
        for (TextView tv : slots) {
            tv.setTextColor(t.suggestionTextColor());
            tv.setBackgroundColor(Color.TRANSPARENT);
        }
        for (View d : dividers) d.setBackgroundColor(withAlpha(t.toolbarIconColor(), 0x55));
        styleIcons();
    }

    public void setMicActive(boolean active) {
        if (micActive == active) return;
        micActive = active;
        styleIcons();
    }

    private void styleIcons() {
        if (theme == null) return;
        int icon = theme.toolbarIconColor();
        if (menu.getDrawable() != null) DrawableCompat.setTint(menu.getDrawable(), icon);
        if (mic.getDrawable() != null) {
            DrawableCompat.setTint(mic.getDrawable(), micActive ? theme.accent : icon);
        }
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(micActive ? withAlpha(theme.accent, 0x33) : withAlpha(icon, 0x26));
        mic.setBackground(new RippleDrawable(ColorStateList.valueOf(withAlpha(theme.text, 0x33)), disc, ovalMask()));
        GradientDrawable none = new GradientDrawable();
        none.setShape(GradientDrawable.OVAL);
        none.setColor(Color.TRANSPARENT);
        menu.setBackground(new RippleDrawable(ColorStateList.valueOf(withAlpha(theme.text, 0x33)), none, ovalMask()));
    }

    private static GradientDrawable ovalMask() {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.BLACK);
        return mask;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void setWords(List<String> words) {
        for (int i = 0; i < 3; i++) {
            String w = words != null && i < words.size() ? words.get(i) : "";
            slots[i].setText(w);
            slots[i].setTag(w.isEmpty() ? null : w);
            if (theme != null) {
                slots[i].setTextColor(i == 1 && !w.isEmpty() ? theme.accent : theme.suggestionTextColor());
                slots[i].setTypeface(i == 1 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            }
        }
    }

    public boolean hasWords() {
        for (TextView slot : slots) {
            if (slot.getTag() instanceof String) return true;
        }
        return false;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
