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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.keyline.ime.R;
import com.keyline.ime.layout.KeySpec;
import com.keyline.ime.layout.LayoutModel;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KeyboardView extends View {
    public static final int PAGE_LETTERS = 0;
    public static final int PAGE_NUM = 1;
    public static final int PAGE_SYM = 2;
    public static final int PAGE_SYM2 = 3;
    public static final int PAGE_PAD = 4;

    public interface EditListener {
        void onKeyTapped(int row, int col, KeySpec spec);
    }

    public interface Listener {
        void onText(String text);
        void onAction(String action);
        void onBackspace(boolean wholeWord);
        void onCursorMove(int delta);
    }

    private static final class Key {
        KeySpec spec;
        boolean hidden;
        int row, col;
        final RectF draw = new RectF();
        final RectF hit = new RectF();
        int icon;
        float level;
        RectF from;
        boolean fresh;
    }

    private static final class Pointer {
        int key = -1;
        float x, y, downX, downY;
        boolean popup;
        boolean cursor;
        boolean wordCut;
        boolean spaceLong;
        boolean repeating;
        long downAt;
        int cursorSteps;
        Runnable longRun;
        Runnable repeatRun;
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<Pointer> pointers = new SparseArray<>();
    private final android.util.SparseArray<Drawable> iconCache = new android.util.SparseArray<>();
    private final List<Key> keys = new ArrayList<>();
    private final PopupController popups;

    private LayoutModel letters;
    private LayoutModel numbers;
    private LayoutModel symbols;
    private LayoutModel symbols2;
    private LayoutModel pad;
    private boolean padSigned, padDecimal;
    private final List<Boolean> rowShared = new ArrayList<>();
    private EditListener editListener;
    private int selRow = -1, selCol = -1;
    private KeyboardTheme theme = new KeyboardTheme();
    private Listener listener;
    private Bitmap background;
    private int page = PAGE_LETTERS;
    private int shift;
    private boolean autoShift;
    private int enterIcon = R.drawable.ic_enter;
    private int keyAlpha = 100;
    private boolean hideBg;
    private int glassTint;
    private float fontScale = 1f;
    private int dim = 28;
    private boolean previewOn = true;
    private boolean longOn = true;
    private Locale locale = Locale.US;
    private String spaceLabel = "";
    private int iconTint = 0xFF202124;

    public KeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        popups = new PopupController(context);
        text.setTextAlign(Paint.Align.CENTER);
        hint.setTextAlign(Paint.Align.RIGHT);
        setClickable(true);
        setHapticFeedbackEnabled(false);
    }

    public void setListener(Listener l) { listener = l; }
    public PopupController popups() { return popups; }

    public void setLayouts(LayoutModel letters, LayoutModel numbers, LayoutModel symbols, LayoutModel symbols2) {
        if (this.letters != null && letters != null
                && !java.util.Objects.equals(this.letters.id, letters.id)) {
            animateNextLayout = true;
            pendingSlide = dp(26);
        }
        this.letters = letters;
        this.numbers = numbers;
        this.symbols = symbols;
        this.symbols2 = symbols2;
        if (letters != null) {
            locale = letters.javaLocale();
            spaceLabel = letters.spaceLabel != null ? letters.spaceLabel : letters.name;
        }
        relayout();
    }

    public void setPad(LayoutModel pad, boolean signed, boolean decimal) {
        this.pad = pad;
        this.padSigned = signed;
        this.padDecimal = decimal;
        if (page == PAGE_PAD) relayout();
    }

    public void setEditListener(EditListener l) {
        editListener = l;
        invalidate();
    }

    public void setSelectedKey(int row, int col) {
        selRow = row;
        selCol = col;
        invalidate();
    }

    public void setPage(int page) {
        boolean changed = this.page != page;
        this.page = page;
        if (changed) animateNextLayout = true;
        popups.hideAll();
        relayout();
    }

    public int page() { return page; }

    public void setShift(int shift, boolean auto) {
        boolean wasUpper = upper();
        this.shift = shift;
        this.autoShift = auto && (letters == null || letters.shifted == null);
        if (wasUpper != upper()) {
            popups.hideAll();
            relayout();
        } else {
            invalidate();
        }
    }

    public void setEnterIcon(int res) {
        this.enterIcon = res;
        relayout();
    }

    public void setTheme(KeyboardTheme t) {
        theme = t;
        popups.setTheme(t);
        iconTint = t.text;
        invalidate();
    }

    public void setKeyAlpha(int alpha) { keyAlpha = alpha; invalidate(); }
    public void setFont(Typeface tf) {
        text.setTypeface(tf);
        hint.setTypeface(tf);
        invalidate();
    }
    public void setFontScale(float scale) { fontScale = scale; invalidate(); }
    public void setGlassTint(int color) { if (glassTint != color) { glassTint = color; invalidate(); } }

    public void setHideKeyBackground(boolean hide) {
        if (hideBg == hide) return;
        hideBg = hide;

        if (getWidth() > 0 && getHeight() > 0) relayout();
        invalidate();
    }

    private boolean ownLook(Key k) { return isEnter(k) || (hideBg && keepsBackground(k)); }
    public void setDim(int dimPercent) { dim = dimPercent; invalidate(); }
    public void setPreviewEnabled(boolean on) { previewOn = on; }
    public void setLongPressEnabled(boolean on) { longOn = on; }

    public void setBackgroundBitmap(Bitmap bmp) {
        if (background != null && background != bmp) background.recycle();
        background = bmp;
    }

    public void relayout() {
        boolean animate = animateNextLayout || transition < 1f;
        animateNextLayout = false;
        if (getWidth() == 0 || getHeight() == 0) return;
        List<Key> old = null;
        if (animate && isShown() && !keys.isEmpty()) {
            old = new ArrayList<>(keys);
            for (Key o : old) currentRect(o, o.draw);
        }
        keys.clear();
        List<List<KeySpec>> rows = currentRows();
        if (rows.isEmpty()) {
            cancelTransition();
            invalidate();
            return;
        }
        float gap = dp(5);
        float pad = dp(3);
        int n = rows.size();
        boolean hasBottomRow = hasBottom && n > 1;
        float rowH = (getHeight() - pad * 2 - gap * (n - 1)) / n;
        float y = pad;
        int gridRows = rows.size() - (hasBottomRow ? 1 : 0);
        float maxWeight = 0f;
        float needWeight = 0f;
        for (int r = 0; r < gridRows; r++) {
            maxWeight = Math.max(maxWeight, rowWeight(rows.get(r)));
            needWeight = Math.max(needWeight, minRowWeight(rows.get(r)));
        }
        if (maxWeight <= 0) maxWeight = 1;
        float refWeight = (page == PAGE_PAD || page == PAGE_SYM || page == PAGE_SYM2) ? 0f : letterWeight();
        if (refWeight > 0 && needWeight <= refWeight + 0.001f) maxWeight = refWeight;
        float gx = maxWeight > 11.5f ? dp(2.5f) : (maxWeight > 10.5f ? dp(4) : gap);
        float edge = maxWeight > 11.5f ? dp(2) : pad;
        float full = getWidth() - edge * 2;
        float slot = (full - gx * (maxWeight - 1)) / maxWeight;
        int rowIndex = -1;
        for (List<KeySpec> row : rows) {
            rowIndex++;
            boolean shared = rowIndex < rowShared.size() && rowShared.get(rowIndex);
            boolean grid = rowIndex < gridRows;
            float[] widths = new float[row.size()];
            float weight = 0f;
            for (int i = 0; i < widths.length; i++) {
                widths[i] = row.get(i).width;
                weight += widths[i];
            }
            if (weight <= 0) weight = 1;
            float x = edge;
            float unit;
            if (grid) {
                if (weight > maxWeight + 0.001f && row.size() > 2
                        && row.get(0).isAction() && row.get(row.size() - 1).isAction()) {
                    float cut = (weight - maxWeight) / 2f;
                    widths[0] -= cut;
                    widths[widths.length - 1] -= cut;
                    weight = maxWeight;
                } else if (weight < maxWeight - 0.001f && row.size() > 2
                        && row.get(0).isAction() && row.get(row.size() - 1).isAction()) {
                    float extra = (maxWeight - weight) / 2f;
                    widths[0] += extra;
                    widths[widths.length - 1] += extra;
                    weight = maxWeight;
                }
                unit = slot;
                float rowW = slot * weight + gx * (weight - 1);
                x = edge + Math.max(0f, (full - rowW) / 2f);
            } else {
                unit = (full - gx * (row.size() - 1)) / weight;
            }
            int colIndex = -1;
            for (KeySpec spec : row) {
                colIndex++;
                float kw = grid ? unit * widths[colIndex] + gx * (widths[colIndex] - 1)
                                : unit * widths[colIndex];
                Key k = new Key();
                k.spec = resolve(spec, shared);
                k.row = rowIndex;
                k.col = colIndex;
                k.hidden = hiddenByField(spec);
                float inset = dp(2);
                if (rowIndex == rows.size() - 1 && hideBg && keepsBackground(k)) inset = Math.max(inset, rowH * 0.185f);
                k.draw.set(x, y + inset, x + kw, y + rowH - inset);
                k.hit.set(x - gx / 2f, y, x + kw + gx / 2f, y + rowH);
                k.icon = iconFor(k.spec);
                keys.add(k);
                x += kw + gx;
            }
            y += rowH + gap;
        }
        if (old != null) startTransition(old);
        else cancelTransition();
        invalidate();
    }

    private boolean animateNextLayout;
    private float pendingSlide;
    private float slideNow;
    private float transition = 1f;
    private android.animation.ValueAnimator transAnim;
    private final List<Key> ghosts = new ArrayList<>();
    private final RectF savedRect = new RectF();

    private static String sigOf(Key k) {
        if (k.spec.isAction()) return "a:" + k.spec.action;
        return "l:" + (k.spec.label == null ? "" : k.spec.label.toLowerCase(Locale.ROOT));
    }

    private void currentRect(Key k, RectF out) {
        if (transition < 1f && k.from != null) {
            float t = transition;
            out.set(k.from.left + (k.draw.left - k.from.left) * t,
                    k.from.top + (k.draw.top - k.from.top) * t,
                    k.from.right + (k.draw.right - k.from.right) * t,
                    k.from.bottom + (k.draw.bottom - k.from.bottom) * t);
        } else {
            out.set(k.draw);
        }
    }

    private void cancelTransition() {
        if (transAnim != null) {
            transAnim.cancel();
            transAnim = null;
        }
        transition = 1f;
        slideNow = 0f;
        pendingSlide = 0f;
        ghosts.clear();
        for (Key k : keys) {
            k.from = null;
            k.fresh = false;
        }
    }

    private void startTransition(List<Key> old) {
        if (transAnim != null) {
            transAnim.cancel();
            transAnim = null;
        }
        ghosts.clear();
        java.util.Map<String, java.util.ArrayDeque<Key>> pool = new java.util.HashMap<>();
        for (Key o : old) {
            if (o.hidden) continue;
            java.util.ArrayDeque<Key> q = pool.get(sigOf(o));
            if (q == null) {
                q = new java.util.ArrayDeque<>();
                pool.put(sigOf(o), q);
            }
            q.add(o);
        }
        for (Key k : keys) {
            k.from = null;
            k.fresh = false;
            if (k.hidden) continue;
            java.util.ArrayDeque<Key> q = pool.get(sigOf(k));
            Key m = q == null ? null : q.pollFirst();
            if (m == null) {
                k.fresh = true;
            } else if (!m.draw.equals(k.draw)) {
                k.from = new RectF(m.draw);
            }
        }
        for (java.util.ArrayDeque<Key> q : pool.values()) ghosts.addAll(q);
        transition = 0f;
        if (pendingSlide != 0f) {
            slideNow = pendingSlide;
            pendingSlide = 0f;
        }
        final android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(slideNow != 0f ? 280 : 220);
        a.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        a.addUpdateListener(anim -> {
            transition = (Float) anim.getAnimatedValue();
            if (transition >= 1f) {
                transition = 1f;
                slideNow = 0f;
                ghosts.clear();
                for (Key k : keys) {
                    k.from = null;
                    k.fresh = false;
                }
            }
            invalidate();
        });
        transAnim = a;
        a.start();
    }

    private float radiusFor(Key k) {
        if (k.spec.isAction()) {
            switch (k.spec.action) {
                case "space":
                    if (!hideBg) return dp(7);
                    return Math.min(k.draw.width(), k.draw.height()) / 2f;
                case "to_numbers": case "to_letters": case "to_symbols": case "to_symbols2": case "enter":
                    return Math.min(k.draw.width(), k.draw.height()) / 2f;
                default: break;
            }
        }
        return dp(7);
    }

    private static boolean isEnter(Key k) {
        return "enter".equals(k.spec.action);
    }

    private float labelBasis(Key k) {
        if (hideBg && keepsBackground(k) && k.hit.height() > 0f) return k.hit.height() - dp(4);
        return k.draw.height();
    }

    private void strokeKey(Canvas canvas, RectF r, float rad) {
        fill.setStyle(Paint.Style.STROKE);
        fill.setStrokeWidth(dp(1));
        fill.setColor(theme.stroke);
        canvas.drawRoundRect(r, rad, rad, fill);
        fill.setStyle(Paint.Style.FILL);
    }

    private static boolean keepsBackground(Key k) {
        if (!k.spec.isAction()) return false;
        switch (k.spec.action) {
            case "space": case "enter": case "to_numbers": case "to_letters": case "to_symbols": case "to_symbols2":
                return true;
            default:
                return false;
        }
    }

    private static float minRowWeight(List<KeySpec> row) {
        float w = rowWeight(row);
        if (row.size() > 2 && row.get(0).isAction() && row.get(row.size() - 1).isAction()) {
            w -= Math.max(0f, row.get(0).width - 1f) + Math.max(0f, row.get(row.size() - 1).width - 1f);
        }
        return w;
    }

    private float letterWeight() {
        if (letters == null) return 0f;
        float w = 0f;
        for (List<KeySpec> row : letters.main) w = Math.max(w, rowWeight(row));
        if (letters.shifted != null) {
            for (List<KeySpec> row : letters.shifted) w = Math.max(w, rowWeight(row));
        }
        return w;
    }

    private static float rowWeight(List<KeySpec> row) {
        float w = 0f;
        for (KeySpec k : row) w += k.width;
        return w;
    }

    private boolean hasBottom;

    private List<List<KeySpec>> currentRows() {
        rowShared.clear();
        hasBottom = false;
        List<List<KeySpec>> rows = new ArrayList<>();
        if (page == PAGE_PAD && pad != null) {
            rows.addAll(pad.main);
            hasBottom = false;
            for (int i = 0; i < rows.size(); i++) rowShared.add(false);
            return rows;
        }
        List<List<KeySpec>> base = letters == null ? new ArrayList<>() : letters.main;
        List<KeySpec> bottom = letters == null ? null : letters.bottom;
        boolean baseShared = false, bottomShared = false;
        if (page == PAGE_NUM || page == PAGE_SYM || page == PAGE_SYM2) {
            LayoutModel shared = page == PAGE_NUM ? numbers : page == PAGE_SYM ? symbols : symbols2;
            List<List<KeySpec>> own = null;
            if (letters != null) {
                own = page == PAGE_NUM ? letters.numbers : page == PAGE_SYM ? letters.symbols : letters.symbols2;
            }
            if (own != null) {
                base = own;
                if (letters.bottomNumbers != null) {
                    bottom = letters.bottomNumbers;
                } else if (shared != null) {
                    bottom = shared.bottom;
                    bottomShared = true;
                }
            } else if (shared != null) {
                base = shared.main;
                bottom = shared.bottom;
                baseShared = true;
                bottomShared = true;
            }
        } else if (letters != null && letters.shifted != null && upper()) {
            base = letters.shifted;
        }
        if (base != null) {
            rows.addAll(base);
            for (int i = 0; i < base.size(); i++) rowShared.add(baseShared);
        }
        if (bottom != null && !bottom.isEmpty()) {
            rows.add(bottom);
            rowShared.add(bottomShared);
            hasBottom = true;
        }
        return rows;
    }

    private KeySpec resolve(KeySpec s, boolean shared) {
        if (!s.isAction() || letters == null) return s;
        String label = null;
        switch (s.action) {
            case "to_letters": label = letters.labelLetters; break;
            case "to_numbers": label = letters.labelNumbers; break;
            case "to_symbols": label = letters.labelSymbols; break;
            case "to_symbols2": label = letters.labelSymbols2; break;
            default: break;
        }
        if (label == null) return s;
        boolean empty = s.label == null || s.label.isEmpty();
        if (!shared && !empty) return s;
        KeySpec c = s.copy();
        c.label = label;
        return c;
    }

    private boolean hiddenByField(KeySpec s) {
        if (s.when == null || page != PAGE_PAD) return false;
        if ("signed".equals(s.when)) return !padSigned;
        if ("decimal".equals(s.when)) return !padDecimal;
        return false;
    }

    private static int contrastOn(int color) {
        double lum = (0.299 * ((color >> 16) & 0xFF) + 0.587 * ((color >> 8) & 0xFF)
                + 0.114 * (color & 0xFF)) / 255.0;
        return lum > 0.6 ? 0xFF202124 : 0xFFFFFFFF;
    }

    private int baseColor(KeySpec spec) {
        if ("accent".equals(spec.style)) return theme.actionKeyColor();
        if ("special".equals(spec.style)) return theme.special;
        if ("normal".equals(spec.style)) return theme.key;
        return spec.isAction() && !"space".equals(spec.action) ? theme.special : theme.key;
    }

    private boolean upper() {
        if (page != PAGE_LETTERS || letters == null) return false;
        if (letters.shifted != null) return shift != 0;
        return letters.hasShift && (shift != 0 || autoShift);
    }

    private boolean caseUpper() {
        return page == PAGE_LETTERS && letters != null && letters.shifted == null && letters.hasShift && (shift != 0 || autoShift);
    }

    private int iconFor(KeySpec spec) {
        if (!spec.isAction()) return "\u200c".equals(spec.code) ? R.drawable.ic_zwnj : 0;
        switch (spec.action) {
            case "shift":
                if (shift == 2) return R.drawable.ic_caps;
                if (shift == 1 || autoShift) return R.drawable.ic_shift_on;
                return R.drawable.ic_shift;
            case "backspace": return R.drawable.ic_backspace;
            case "enter": return enterIcon;
            case "lang": return R.drawable.ic_globe;
            case "emoji": return R.drawable.ic_emoji;
            default: return 0;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        relayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean up = caseUpper();
        long nowMs = SystemClock.uptimeMillis();
        float dt = lastDrawMs == 0 ? 16f : Math.min(50f, nowMs - lastDrawMs);
        lastDrawMs = nowMs;
        boolean animating = false;
        boolean moving = transition < 1f;
        if (moving) {
            float outA = 1f - Math.min(1f, transition / 0.55f);
            for (Key g : ghosts) {
                canvas.save();
                canvas.translate(-slideNow * transition, 0f);
                animating |= drawKey(canvas, g, -1, dt, up, outA, 1f - 0.08f * transition);
                canvas.restore();
            }
        }
        for (int i = 0; i < keys.size(); i++) {
            Key k = keys.get(i);
            if (k.hidden) continue;
            if (moving && (k.from != null || k.fresh)) {
                savedRect.set(k.draw);
                float alpha = 1f;
                float scale = 1f;
                if (k.fresh) {
                    alpha = Math.max(0f, Math.min(1f, (transition - 0.2f) / 0.8f));
                    scale = 0.92f + 0.08f * transition;
                } else {
                    currentRect(k, k.draw);
                }
                canvas.save();
                if (k.fresh) canvas.translate(slideNow * (1f - transition), 0f);
                animating |= drawKey(canvas, k, i, dt, up, alpha, scale);
                canvas.restore();
                k.draw.set(savedRect);
            } else {
                animating |= drawKey(canvas, k, i, dt, up, 1f, 1f);
            }
        }
        if (animating) postInvalidateOnAnimation();
    }

    private long lastDrawMs;

    private boolean drawKey(Canvas canvas, Key k, int index, float dt, boolean up, float alpha, float scale) {
        boolean animating = false;
        boolean layer = alpha < 0.999f;
        if (layer) {
            if (alpha <= 0.01f) return false;
            canvas.saveLayerAlpha(k.draw.left - dp(4), k.draw.top - dp(4), k.draw.right + dp(4),
                    k.draw.bottom + dp(4), Math.round(alpha * 255));
        }
            float rad = radiusFor(k);
            float target = index >= 0 && isPressedKey(index) ? 1f : 0f;
            if (k.level < target) k.level = Math.min(target, k.level + dt / 35f);
            else if (k.level > target) k.level = Math.max(target, k.level - dt / 110f);
            if (k.level != target) animating = true;
            canvas.save();
            float sc = (1f - 0.025f * k.level) * scale;
            canvas.scale(sc, sc, k.draw.centerX(), k.draw.centerY());
            boolean fixedKey = ownLook(k);
            boolean enterKey = isEnter(k);
            boolean accentKey = "accent".equals(k.spec.style) || (hideBg && enterKey);
            int base = accentKey ? theme.actionKeyColor() : baseColor(k.spec);
            if (fixedKey && hideBg && !enterKey) {

                int tint = glassTint != 0 ? glassTint : theme.toolbarIconColor();
                fill.setColor((tint & 0x00FFFFFF) | (0x66 << 24));
                canvas.drawRoundRect(k.draw, rad, rad, fill);
            } else if (fixedKey) {
                fill.setColor(theme.withAlpha(base, 100));
                canvas.drawRoundRect(k.draw, rad, rad, fill);
                if (theme.stroke != 0) strokeKey(canvas, k.draw, rad);
            } else if (!hideBg && keyAlpha > 0) {
                fill.setColor(theme.withAlpha(base, accentKey ? Math.max(keyAlpha, 90) : keyAlpha));
                canvas.drawRoundRect(k.draw, rad, rad, fill);
                if (theme.stroke != 0 && keyAlpha > 30) strokeKey(canvas, k.draw, rad);
            }
            if (k.level > 0f) {
                int pc = theme.pressed;
                fill.setColor((pc & 0x00FFFFFF) | (Math.round(((pc >>> 24) & 0xFF) * k.level) << 24));
                canvas.drawRoundRect(k.draw, rad, rad, fill);
            }
            if (k.row == selRow && k.col == selCol && editListener != null) {
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(dp(2));
                fill.setColor(theme.accent);
                canvas.drawRoundRect(k.draw, rad, rad, fill);
                fill.setStyle(Paint.Style.FILL);
            }
            int icon = iconFor(k.spec);
            if (icon != 0) {
                int color = "enter".equals(k.spec.action) && enterIcon != R.drawable.ic_enter ? theme.accent : theme.text;
                if (accentKey) color = theme.actionKeyTextColor();
                else if (color == theme.text && base == theme.special) color = theme.specialTextColor();
                if ("shift".equals(k.spec.action) && (shift != 0 || autoShift)) color = theme.accent;
                drawIcon(canvas, icon, k.draw, color);
            } else {
                String label = "space".equals(k.spec.action) ? spaceLabel : k.spec.output(up, locale);
                if (label == null) label = "";
                float factor = label.length() > 3 ? (fixedKey ? 0.22f : 0.28f) : (fixedKey ? 0.42f : 0.46f);
                float ts = labelBasis(k) * factor * fontScale;
                text.setTextSize(ts);
                text.setColor(accentKey ? theme.actionKeyTextColor()
                        : (base == theme.special ? theme.specialTextColor() : theme.text));
                Paint.FontMetrics fm = text.getFontMetrics();
                float ty = k.draw.centerY() - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(label, k.draw.centerX(), ty, text);
                String h = k.spec.hint;
                if (h == null && k.spec.popup.length > 0 && k.spec.label != null && k.spec.label.length() <= 2) {
                    h = k.spec.popup[0];
                }
                if (!k.spec.isAction() && h != null && !h.isEmpty()) {
                    hint.setTextSize(k.draw.height() * 0.25f * fontScale);
                    hint.setColor(theme.hint);
                    if (up) h = h.toUpperCase(locale);
                    canvas.drawText(h, k.draw.right - dp(7), k.draw.top + hint.getTextSize() + dp(2), hint);
                }
            }
            canvas.restore();
        if (layer) canvas.restore();
        return animating;
    }

    private boolean isPressedKey(int index) {
        for (int i = 0; i < pointers.size(); i++) {
            if (pointers.valueAt(i).key == index && !pointers.valueAt(i).cursor) return true;
        }
        return false;
    }

    private float iconDp(int res) {
        if (res == R.drawable.ic_globe) return 25f;
        if (res == R.drawable.ic_zwnj) return 22.5f;
        if (res == R.drawable.ic_emoji) return 25f;
        if (res == R.drawable.ic_shift || res == R.drawable.ic_shift_on || res == R.drawable.ic_caps) return 28f;
        if (res == R.drawable.ic_backspace) return 24.5f;
        if (res == R.drawable.ic_enter) return 21.5f;
        return 21f;
    }

    private void drawIcon(Canvas canvas, int res, RectF r, int color) {
        Drawable d = iconCache.get(res);
        if (d == null) {
            Drawable raw = AppCompatResources.getDrawable(getContext(), res);
            if (raw == null) return;
            d = raw.mutate();
            iconCache.put(res, d);
        }
        DrawableCompat.setTint(d, color);
        int s = Math.round(Math.min(dp(iconDp(res)), Math.min(r.width(), r.height()) * 0.85f));
        int cx = Math.round(r.centerX());
        int cy = Math.round(r.centerY());
        d.setBounds(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
        boolean flip = (res == R.drawable.ic_enter || res == R.drawable.ic_backspace)
                && letters != null && letters.rtl();
        if (flip) {
            canvas.save();
            canvas.scale(-1f, 1f, cx, cy);
            d.draw(canvas);
            canvas.restore();
        } else {
            d.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (editListener != null) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                int hit = keyAt(e.getX(), e.getY());
                if (hit >= 0) {
                    Key k = keys.get(hit);
                    selRow = k.row;
                    selCol = k.col;
                    invalidate();
                    editListener.onKeyTapped(k.row, k.col, k.spec);
                }
            }
            return true;
        }
        int action = e.getActionMasked();
        int index = e.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            down(e.getPointerId(index), e.getX(index), e.getY(index));
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                move(e.getPointerId(i), e.getX(i), e.getY(i));
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            up(e.getPointerId(index), e.getX(index), e.getY(index));
        } else if (action == MotionEvent.ACTION_CANCEL) {
            cancelAll();
        }
        return true;
    }

    private void down(int id, float x, float y) {
        Pointer p = new Pointer();
        p.x = p.downX = x;
        p.y = p.downY = y;
        p.downAt = SystemClock.uptimeMillis();
        p.key = keyAt(x, y);
        pointers.put(id, p);
        if (p.key < 0) return;
        Key k = keys.get(p.key);
        feedback();
        if ("backspace".equals(actionOf(k))) {
            if (listener != null) listener.onBackspace(false);
            startRepeat(p);
        } else {
            showPreview(k);
            scheduleLong(id, p);
        }
        invalidate();
    }

    private void move(int id, float x, float y) {
        Pointer p = pointers.get(id);
        if (p == null) return;
        p.x = x;
        p.y = y;
        if (p.key < 0) return;
        Key origin = keys.get(p.key);
        if (p.popup || popups.alternatesShowing()) {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            popups.highlightAt(loc[0] + x);
            return;
        }
        if ("space".equals(actionOf(origin)) && Math.abs(x - p.downX) > dp(18)) {
            p.cursor = true;
            hidePreview();
            cancelLong(p);
            int step = (int) ((x - p.downX) / dp(14));
            int delta = step - p.cursorSteps;
            if (delta != 0 && listener != null) {
                listener.onCursorMove(delta);
                p.cursorSteps = step;
            }
            return;
        }
        if ("backspace".equals(actionOf(origin)) && !p.wordCut && (p.downX - x) > dp(48)) {
            p.wordCut = true;
            stopRepeat(p);
            if (listener != null) listener.onBackspace(true);
            return;
        }
        int nk = keyUnder(x, y);
        if (!p.cursor && nk != p.key && nk >= 0 && !"backspace".equals(actionOf(origin))) {
            p.key = nk;
            p.downAt = SystemClock.uptimeMillis();
            cancelLong(p);
            scheduleLong(id, p);
            showPreview(keys.get(nk));
            invalidate();
        }
    }

    private void up(int id, float x, float y) {
        Pointer p = pointers.get(id);
        pointers.remove(id);
        if (p == null) return;
        cancelLong(p);
        stopRepeat(p);
        hidePreview();
        if (p.popup || popups.alternatesShowing()) {
            String v = popups.selectedValue();
            popups.hideAlternates();
            if (v != null && listener != null) listener.onText(v);
            invalidate();
            return;
        }
        popups.hideAlternates();
        if (p.cursor || p.wordCut || p.spaceLong) {
            invalidate();
            return;
        }
        if (p.key < 0 || p.key >= keys.size()) return;
        Key k = keys.get(p.key);
        if ("backspace".equals(actionOf(k))) {
            invalidate();
            return;
        }
        fire(k);
        invalidate();
    }

    private void fire(Key k) {
        if (listener == null) return;
        KeySpec s = k.spec;
        if (s.isAction()) {
            switch (s.action) {
                case "to_numbers": setPage(PAGE_NUM); return;
                case "to_symbols": setPage(PAGE_SYM); return;
                case "to_symbols2": setPage(PAGE_SYM2); return;
                case "to_letters": setPage(PAGE_LETTERS); return;
                case "space": listener.onAction("space"); return;
                default: listener.onAction(s.action); return;
            }
        }
        listener.onText(s.output(caseUpper(), locale));
    }

    private void scheduleLong(int id, Pointer p) {
        Key start = p.key >= 0 && p.key < keys.size() ? keys.get(p.key) : null;
        boolean onSpace = start != null && "space".equals(actionOf(start));
        if (!longOn && !onSpace) return;
        p.longRun = () -> {
            Pointer cur = pointers.get(id);
            if (cur != p || p.key < 0) return;
            Key k = keys.get(p.key);
            if ("space".equals(actionOf(k))) {
                if (p.cursor || Math.hypot(p.x - p.downX, p.y - p.downY) > dp(16)) return;
                p.spaceLong = true;
                hidePreview();
                if (listener != null) listener.onAction("#switch_keyboard");
                invalidate();
                return;
            }
            if (!longOn) return;
            if (k.spec.popup == null || k.spec.popup.length == 0) return;
            if (Math.hypot(p.x - p.downX, p.y - p.downY) > dp(16)) return;
            String[] vals = new String[k.spec.popup.length];
            boolean up = caseUpper();
            for (int i = 0; i < vals.length; i++) {
                vals[i] = up ? k.spec.popup[i].toUpperCase(locale) : k.spec.popup[i];
            }
            p.popup = true;
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            popups.showAlternates(this, k.draw.centerX(), k.draw.top, vals);
            popups.highlightAt(loc[0] + p.x);
        };
        handler.postDelayed(p.longRun, Prefs.get(getContext()).longPressDelay());
    }

    private void startRepeat(Pointer p) {
        p.repeating = true;
        p.repeatRun = new Runnable() {
            int n = 0;
            @Override public void run() {
                if (!p.repeating) return;
                n++;
                boolean words = n > 24;
                if (listener != null) listener.onBackspace(words);
                int delay = words ? 140 : Math.max(28, Prefs.get(getContext()).repeatMs() - n * 5);
                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(p.repeatRun, 380);
    }

    private void stopRepeat(Pointer p) {
        p.repeating = false;
        if (p.repeatRun != null) handler.removeCallbacks(p.repeatRun);
    }

    private void cancelLong(Pointer p) {
        if (p.longRun != null) handler.removeCallbacks(p.longRun);
    }

    private void cancelAll() {
        for (int i = 0; i < pointers.size(); i++) {
            Pointer p = pointers.valueAt(i);
            cancelLong(p);
            stopRepeat(p);
        }
        pointers.clear();
        popups.hideAll();
        invalidate();
    }

    private void showPreview(Key k) {
        if (!previewOn || k.spec.isAction()) {
            hidePreview();
            return;
        }
        String label = k.spec.output(caseUpper(), locale);
        popups.showPreview(this, k.draw.centerX(), k.draw.top, label);
    }

    private void hidePreview() { popups.hidePreview(); }

    private int keyAt(float x, float y) {
        for (int i = 0; i < keys.size(); i++) {
            if (!keys.get(i).hidden && keys.get(i).hit.contains(x, y)) return i;
        }
        if (x < 0 || y < 0 || x > getWidth() || y > getHeight()) return -1;
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < keys.size(); i++) {
            Key k = keys.get(i);
            if (k.hidden) continue;
            float dx = Math.max(Math.max(k.draw.left - x, x - k.draw.right), 0f);
            float dy = Math.max(Math.max(k.draw.top - y, y - k.draw.bottom), 0f);
            float d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private int keyUnder(float x, float y) {
        for (int i = 0; i < keys.size(); i++) {
            Key k = keys.get(i);
            if (!k.hidden && k.draw.contains(x, y)) return i;
        }
        return -1;
    }

    private String actionOf(Key k) {
        return k.spec.isAction() ? k.spec.action : "";
    }

    private void feedback() {
        if (listener != null) listener.onAction("#feedback");
    }

    public void dismissPopups() {
        cancelAll();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
