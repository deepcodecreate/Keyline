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

package com.keyline.ime.translate;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.theme.KeyboardTheme;
import com.keyline.ime.util.Bg;

public class TranslatePanel extends LinearLayout {
    public interface Listener {
        void onReplace(String translated);
        void onCopy(String translated);
        void onClose();
    }

    private static final String[] CODES = {"fa", "en", "ar", "ru", "es", "tr", "fr", "de"};
    private static final String[] NAMES = {"فارسی", "English", "العربية", "Русский", "Español", "Türkçe", "Français", "Deutsch"};

    private final TextView[] chips = new TextView[CODES.length];
    private final TextView source;
    private final TextView result;
    private final TextView status;
    private final TextView replace;
    private final TextView copy;
    private final ImageButton close;
    private final LinearLayout card;
    private Listener listener;
    private KeyboardTheme theme;
    private String text = "";
    private String translated = "";
    private int ticket;

    public TranslatePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(4), dp(8), dp(6));

        LinearLayout head = new LinearLayout(context);
        head.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.HorizontalScrollView chipScroll = new android.widget.HorizontalScrollView(context);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(context);
        for (int i = 0; i < CODES.length; i++) {
            final String code = CODES[i];
            TextView c = new TextView(context);
            c.setText(NAMES[i]);
            c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(12), 0, dp(12), 0);
            c.setOnClickListener(v -> {
                Prefs.get(getContext()).translateTarget(code);
                styleChips();
                run();
            });
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(28));
            lp.setMarginEnd(dp(6));
            chipRow.addView(c, lp);
            chips[i] = c;
        }
        chipScroll.addView(chipRow);
        head.addView(chipScroll, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        close = new ImageButton(context);
        Object d = AppCompatResources.getDrawable(context, R.drawable.ic_close);
        if (d != null) close.setImageDrawable(((android.graphics.drawable.Drawable) d).mutate());
        close.setBackground(null);
        close.setContentDescription(context.getString(R.string.cd_close));
        close.setOnClickListener(v -> { if (listener != null) listener.onClose(); });
        addView(head, new LayoutParams(LayoutParams.MATCH_PARENT, dp(36)));

        card = new LinearLayout(context);
        card.setOrientation(VERTICAL);
        card.setPadding(dp(12), dp(8), dp(12), dp(8));
        source = new TextView(context);
        source.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        source.setMaxLines(2);
        source.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(source, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ScrollView sv = new ScrollView(context);
        result = new TextView(context);
        result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        result.setTextIsSelectable(false);
        result.setPadding(0, dp(6), 0, 0);
        sv.addView(result);
        card.addView(sv, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        status = new TextView(context);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        card.addView(status, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LayoutParams cardLp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.topMargin = dp(4);
        addView(card, cardLp);

        LinearLayout actions = new LinearLayout(context);
        copy = button(context.getString(R.string.translate_copy));
        replace = button(context.getString(R.string.translate_replace));
        copy.setOnClickListener(v -> { if (!translated.isEmpty() && listener != null) listener.onCopy(translated); });
        replace.setOnClickListener(v -> { if (!translated.isEmpty() && listener != null) listener.onReplace(translated); });
        LayoutParams a = new LayoutParams(0, dp(40), 1f);
        a.setMarginEnd(dp(8));
        actions.addView(copy, a);
        actions.addView(replace, new LayoutParams(0, dp(40), 1f));
        LayoutParams alp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(6);
        addView(actions, alp);
    }

    public void setListener(Listener l) { listener = l; }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(t.background);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(t.glassColor());
        g.setStroke(dp(1), t.stroke != 0 ? t.stroke : t.glassStroke());
        card.setBackground(g);
        source.setTextColor(t.hint);
        result.setTextColor(t.text);
        status.setTextColor(t.hint);
        if (close.getDrawable() != null) DrawableCompat.setTint(close.getDrawable(), t.text);
        styleChips();
        stylePrimary(replace, true);
        stylePrimary(copy, false);
    }

    public void show(String selected) {
        text = selected == null ? "" : selected.trim();
        run();
    }

    private void run() {
        final int my = ++ticket;
        translated = "";
        source.setText(text);
        replace.setEnabled(false);
        copy.setEnabled(false);
        replace.setAlpha(0.4f);
        copy.setAlpha(0.4f);
        if (text.isEmpty()) {
            result.setText("");
            status.setText(R.string.translate_need_text);
            return;
        }
        result.setText("…");
        status.setText(R.string.translate_working);
        final String target = Prefs.get(getContext()).translateTarget();
        styleChips();
        Bg.io(() -> {
            String out = null;
            boolean failed = false;
            try {
                out = Translator.translateKeepingLinks(text, target);
            } catch (Exception e) {
                failed = true;
            }
            final String fo = out;
            final boolean ff = failed;
            Bg.main(() -> {
                if (my != ticket) return;
                if (ff || fo == null || fo.isEmpty()) {
                    result.setText("");
                    status.setText(R.string.translate_failed);
                    return;
                }
                translated = fo;
                result.setText(fo);
                status.setText("");
                replace.setEnabled(true);
                copy.setEnabled(true);
                replace.setAlpha(1f);
                copy.setAlpha(1f);
            });
        });
    }

    private TextView button(String label) {
        TextView b = new TextView(getContext());
        b.setText(label);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return b;
    }

    private void stylePrimary(TextView b, boolean primary) {
        if (theme == null) return;
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(20));
        g.setColor(primary ? theme.accent : theme.glassColor());
        if (!primary) g.setStroke(dp(1), theme.stroke != 0 ? theme.stroke : theme.glassStroke());
        b.setBackground(g);
        b.setTextColor(primary ? 0xFFFFFFFF : theme.text);
    }

    private void styleChips() {
        if (theme == null) return;
        String cur = Prefs.get(getContext()).translateTarget();
        for (int i = 0; i < chips.length; i++) {
            boolean on = CODES[i].equals(cur);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(on ? theme.accent : theme.glassColor());
            if (!on) g.setStroke(dp(1), theme.stroke != 0 ? theme.stroke : theme.glassStroke());
            chips[i].setBackground(g);
            chips[i].setTextColor(on ? 0xFFFFFFFF : theme.text);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
