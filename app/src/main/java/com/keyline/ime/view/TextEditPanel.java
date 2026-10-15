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
import android.text.TextUtils;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextEditPanel extends LinearLayout {
    public interface Listener {
        InputConnection connection();

        void onEditPrepare();
        void onClose();
    }

    private static final int REPEAT_FIRST_MS = 380;
    private static final int REPEAT_MS = 55;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<FrameLayout> keys = new ArrayList<>();
    private final List<ImageView> icons = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private final List<FrameLayout> cdKeys = new ArrayList<>();
    private final List<Integer> cdRes = new ArrayList<>();
    private String langTag = "";
    private FrameLayout selectKey;
    private TextView selectLabel;
    private Listener listener;
    private KeyboardTheme theme;
    private boolean selecting;

    public TextEditPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setPadding(dp(8), dp(6), dp(8), dp(6));

        LinearLayout pad = new LinearLayout(context);
        pad.setOrientation(VERTICAL);
        pad.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout r1 = rowOf(context);
        LinearLayout r2 = rowOf(context);
        LinearLayout r3 = rowOf(context);
        r1.addView(textKey(context, R.string.te_home, () -> key(KeyEvent.KEYCODE_MOVE_HOME), false), cell());
        r1.addView(iconKey(context, R.drawable.ic_up, R.string.te_up, () -> key(KeyEvent.KEYCODE_DPAD_UP), true), cell());
        r1.addView(textKey(context, R.string.te_end, () -> key(KeyEvent.KEYCODE_MOVE_END), false), cell());
        r2.addView(iconKey(context, R.drawable.ic_arrow_left, R.string.te_left, () -> key(KeyEvent.KEYCODE_DPAD_LEFT), true), cell());
        selectKey = textKey(context, R.string.te_select, this::toggleSelect, false);
        selectLabel = labels.get(labels.size() - 1);
        r2.addView(selectKey, cell());
        r2.addView(iconKey(context, R.drawable.ic_arrow_right, R.string.te_right, () -> key(KeyEvent.KEYCODE_DPAD_RIGHT), true), cell());
        r3.addView(textKey(context, R.string.te_back, () -> { if (listener != null) listener.onClose(); }, false), cell());
        r3.addView(iconKey(context, R.drawable.ic_down, R.string.te_down, () -> key(KeyEvent.KEYCODE_DPAD_DOWN), true), cell());
        r3.addView(iconKey(context, R.drawable.ic_backspace, R.string.cd_backspace, () -> key(KeyEvent.KEYCODE_DEL), true), cell());
        pad.addView(r1, rowLp());
        pad.addView(r2, rowLp());
        pad.addView(r3, rowLp());
        addView(pad, new LayoutParams(0, LayoutParams.MATCH_PARENT, 3f));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(VERTICAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        actions.addView(iconKey(context, R.drawable.ic_select_all, R.string.cd_select_all,
                () -> menu(android.R.id.selectAll), false), rowLp());
        actions.addView(iconKey(context, R.drawable.ic_cut, R.string.cd_cut,
                () -> menu(android.R.id.cut), false), rowLp());
        actions.addView(iconKey(context, R.drawable.ic_copy, R.string.cd_copy,
                () -> menu(android.R.id.copy), false), rowLp());
        actions.addView(iconKey(context, R.drawable.ic_paste, R.string.cd_paste,
                () -> menu(android.R.id.paste), false), rowLp());
        LayoutParams alp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        alp.setMarginStart(dp(6));
        addView(actions, alp);
    }

    private LinearLayout rowOf(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(HORIZONTAL);
        return r;
    }

    private LinearLayout.LayoutParams rowLp() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    }

    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        return lp;
    }

    private FrameLayout iconKey(Context c, int iconRes, int cd, Runnable action, boolean repeat) {
        FrameLayout k = baseKey(c, action, repeat);
        ImageView iv = new ImageView(c);
        iv.setImageDrawable(AppCompatResources.getDrawable(c, iconRes));
        if (iv.getDrawable() != null) iv.getDrawable().mutate();
        k.addView(iv, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        k.setContentDescription(c.getString(cd));
        cdKeys.add(k);
        cdRes.add(cd);
        icons.add(iv);
        return k;
    }

    private FrameLayout textKey(Context c, int textRes, Runnable action, boolean repeat) {
        FrameLayout k = baseKey(c, action, repeat);
        TextView tv = new TextView(c);
        tv.setText(textRes);
        tv.setTag(textRes);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        k.addView(tv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        labels.add(tv);
        return k;
    }

    private FrameLayout baseKey(Context c, Runnable action, boolean repeat) {
        FrameLayout k = new FrameLayout(c);
        k.setClickable(true);
        k.setFocusable(false);
        keys.add(k);
        final Runnable tick = new Runnable() {
            @Override public void run() {
                action.run();
                handler.postDelayed(this, REPEAT_MS);
            }
        };
        k.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    action.run();
                    if (repeat) handler.postDelayed(tick, REPEAT_FIRST_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    handler.removeCallbacks(tick);
                    return true;
                default:
                    return true;
            }
        });
        return k;
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
        Context sc = getContext().createConfigurationContext(cfg);
        setLayoutDirection(TextUtils.getLayoutDirectionFromLocale(locale));
        for (TextView tv : labels) {
            if (tv.getTag() instanceof Integer) tv.setText(sc.getString((Integer) tv.getTag()));
        }
        for (int i = 0; i < cdKeys.size(); i++) cdKeys.get(i).setContentDescription(sc.getString(cdRes.get(i)));
    }

    public void reset() {
        handler.removeCallbacksAndMessages(null);
        selecting = false;
        restyle();
    }

    private void toggleSelect() {
        selecting = !selecting;
        restyle();
    }

    private void key(int code) {
        if (listener == null) return;
        InputConnection ic = listener.connection();
        if (ic == null) return;
        listener.onEditPrepare();
        boolean shift = selecting && code != KeyEvent.KEYCODE_DEL;
        long now = SystemClock.uptimeMillis();
        int meta = shift ? KeyEvent.META_SHIFT_ON : 0;
        if (shift) {
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0));
        }
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta));
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, meta));
        if (shift) {
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0));
        }
    }

    private void menu(int id) {
        if (listener == null) return;
        InputConnection ic = listener.connection();
        if (ic == null) return;
        listener.onEditPrepare();
        ic.performContextMenuAction(id);
        if (id == android.R.id.selectAll) {

            selecting = true;
            restyle();
        } else if (id == android.R.id.cut || id == android.R.id.copy || id == android.R.id.paste) {
            selecting = false;
            restyle();
        }
    }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(Color.TRANSPARENT);
        restyle();
    }

    private void restyle() {
        if (theme == null) return;
        int fg = theme.toolbarIconColor();
        for (FrameLayout k : keys) {
            boolean on = k == selectKey && selecting;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(on ? withAlpha(theme.accent, 0x44) : withAlpha(fg, 0x22));
            if (on) bg.setStroke(dp(2), theme.accent);
            k.setBackground(bg);
        }
        for (ImageView iv : icons) {
            if (iv.getDrawable() != null) DrawableCompat.setTint(iv.getDrawable(), fg);
        }
        for (TextView tv : labels) {
            tv.setTextColor(tv == selectLabel && selecting ? theme.accent : fg);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
