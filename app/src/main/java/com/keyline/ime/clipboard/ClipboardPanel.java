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

package com.keyline.ime.clipboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;
import com.keyline.ime.util.Bg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClipboardPanel extends LinearLayout {
    public interface Listener {
        void onPaste(String text);
        void onClose();
        void onChanged();
    }

    private static final int F_ALL = 0, F_PINNED = 1, F_LINKS = 2;
    private static final int[] CHIP_NAMES = {R.string.clip_filter_all, R.string.clip_filter_pinned, R.string.clip_filter_links};

    private final ImageButton close;
    private final TextView[] chips = new TextView[3];
    private final ImageButton sweep;
    private final RecyclerView list;
    private final LinearLayout empty;
    private final ImageView emptyIcon;
    private final TextView emptyTitle;
    private final TextView emptyHint;
    private final TextView undoBar;
    private final Adapter adapter = new Adapter();
    private final List<ClipEntity> all = new ArrayList<>();
    private final List<ClipEntity> shown = new ArrayList<>();
    private Listener listener;
    private KeyboardTheme theme;
    private Context strCtx;
    private String langTag = "";
    private int chip = F_ALL;
    private ClipEntity lastDeleted;
    private long actionId = -1;
    private final Runnable hideUndo = new Runnable() {
        @Override public void run() { undoBar.setVisibility(GONE); lastDeleted = null; }
    };

    public ClipboardPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        strCtx = context;
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(2), dp(8), dp(2));

        LinearLayout chipRow = new LinearLayout(context);
        chipRow.setOrientation(HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView c = new TextView(context);
            c.setText(str(CHIP_NAMES[i]));
            c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(12), 0, dp(12), 0);
            c.setOnClickListener(v -> { chip = idx; actionId = -1; applyFilter(); styleChips(); });
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(28));
            lp.setMarginEnd(dp(8));
            chipRow.addView(c, lp);
            chips[i] = c;
        }
        View spacer = new View(context);
        chipRow.addView(spacer, new LayoutParams(0, 1, 1f));
        sweep = new ImageButton(context);
        sweep.setImageDrawable(tintable(R.drawable.ic_delete_sweep));
        sweep.setBackgroundResource(rippleRes());
        sweep.setContentDescription(str(R.string.cd_clear_unpinned));
        sweep.setOnClickListener(v -> Bg.io(() -> {
            ClipboardRepository.clearUnpinned(context);
            publish();
        }));
        chipRow.addView(sweep, new LayoutParams(dp(34), dp(34)));
        close = new ImageButton(context);
        close.setImageDrawable(tintable(R.drawable.ic_close));
        close.setBackgroundResource(rippleRes());
        close.setContentDescription(str(R.string.cd_close));
        close.setOnClickListener(v -> { if (listener != null) listener.onClose(); });
        addView(chipRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));

        empty = new LinearLayout(context);
        empty.setOrientation(VERTICAL);
        empty.setGravity(Gravity.CENTER);
        emptyIcon = new ImageView(context);
        emptyIcon.setImageDrawable(tintable(R.drawable.ic_clipboard));
        empty.addView(emptyIcon, new LayoutParams(dp(36), dp(36)));
        emptyTitle = new TextView(context);
        emptyTitle.setText(str(R.string.clip_empty));
        emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        emptyTitle.setGravity(Gravity.CENTER);
        LayoutParams etlp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        etlp.topMargin = dp(8);
        empty.addView(emptyTitle, etlp);
        emptyHint = new TextView(context);
        emptyHint.setText(str(R.string.clip_empty_hint));
        emptyHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        emptyHint.setGravity(Gravity.CENTER);
        empty.addView(emptyHint, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(empty, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        list = new RecyclerView(context);
        list.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        list.setAdapter(adapter);
        list.setClipToPadding(false);
        list.setPadding(0, dp(2), 0, dp(2));
        list.setItemAnimator(null);
        list.setOverScrollMode(OVER_SCROLL_NEVER);
        addView(list, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        attachSwipe();

        undoBar = new TextView(context);
        undoBar.setText(undoText());
        undoBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        undoBar.setGravity(Gravity.CENTER);
        undoBar.setVisibility(GONE);
        undoBar.setOnClickListener(v -> {
            ClipEntity e = lastDeleted;
            removeCallbacks(hideUndo);
            undoBar.setVisibility(GONE);
            lastDeleted = null;
            if (e != null) Bg.io(() -> {
                ClipboardRepository.restore(context, e);
                publish();
            });
        });
        addView(undoBar, new LayoutParams(LayoutParams.MATCH_PARENT, dp(34)));

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView r, int state) {
                if (state != RecyclerView.SCROLL_STATE_IDLE) dismissMenu();
            }
        });
        styleAll();
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
        for (int i = 0; i < 3; i++) chips[i].setText(str(CHIP_NAMES[i]));
        sweep.setContentDescription(str(R.string.cd_clear_unpinned));
        close.setContentDescription(str(R.string.cd_close));
        emptyTitle.setText(str(R.string.clip_empty));
        emptyHint.setText(str(R.string.clip_empty_hint));
        undoBar.setText(undoText());
        adapter.notifyDataSetChanged();
    }

    private String str(int res) { return strCtx.getString(res); }

    private String undoText() { return str(R.string.clip_deleted) + "  •  " + str(R.string.clip_undo); }

    private String relativeTime(long then) {
        long diff = Math.max(0, System.currentTimeMillis() - then);
        long min = diff / DateUtils.MINUTE_IN_MILLIS;
        if (min < 1) return str(R.string.clip_time_now);
        if (min < 60) return strCtx.getString(R.string.clip_time_min, (int) min);
        long hours = min / 60;
        if (hours < 24) return strCtx.getString(R.string.clip_time_hour, (int) hours);
        return strCtx.getString(R.string.clip_time_day, (int) (hours / 24));
    }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(t.background);
        styleAll();
        adapter.notifyDataSetChanged();
    }

    public void load() {
        Bg.io(this::publish);
    }

    private void publish() {
        List<ClipEntity> items = ClipboardRepository.all(getContext());
        Bg.main(() -> setItems(items));
    }

    public void setItems(List<ClipEntity> items) {
        all.clear();
        if (items != null) all.addAll(items);
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        boolean anyUnpinned = false;
        for (ClipEntity e : all) {
            if (!e.pinned) anyUnpinned = true;
            if (chip == F_PINNED && !e.pinned) continue;
            if (chip == F_LINKS && !isLink(e.text)) continue;
            shown.add(e);
        }
        java.util.Collections.sort(shown, (x, y) -> Boolean.compare(y.pinned, x.pinned));
        boolean none = shown.isEmpty();
        empty.setVisibility(none ? VISIBLE : GONE);
        list.setVisibility(none ? GONE : VISIBLE);
        sweep.setVisibility(anyUnpinned ? VISIBLE : INVISIBLE);
        adapter.notifyDataSetChanged();
    }

    private static boolean isLink(String s) {
        return com.keyline.ime.util.LinkDetector.hasLink(s);
    }

    private void styleAll() {
        if (theme == null) return;
        tint(close, theme.text);
        tint(sweep, theme.hint);
        emptyIcon.setColorFilter(theme.hint);
        emptyTitle.setTextColor(theme.text);
        emptyHint.setTextColor(theme.hint);
        undoBar.setTextColor(theme.accent);
        undoBar.setBackground(round(theme.glassColor(), 12, dp(1), edge()));
        styleChips();
    }

    private void styleChips() {
        if (theme == null) return;
        for (int i = 0; i < 3; i++) {
            boolean on = i == chip;
            chips[i].setTextColor(on ? theme.accent : theme.text);
            chips[i].setBackground(on
                    ? round(theme.withAlpha(theme.accent, 18), 16, dp(1), theme.accent)
                    : round(theme.glassColor(), 16, dp(1), edge()));
        }
    }

    private int edge() {
        return theme.stroke != 0 ? theme.stroke : theme.glassStroke();
    }

    private GradientDrawable round(int color, int radiusDp, int strokePx, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    private android.graphics.drawable.Drawable tintable(int res) {
        android.graphics.drawable.Drawable d = AppCompatResources.getDrawable(getContext(), res);
        return d == null ? null : d.mutate();
    }

    private void tint(ImageButton b, int color) {
        if (b.getDrawable() != null) DrawableCompat.setTint(b.getDrawable(), color);
    }

    private int rippleRes() {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        return tv.resourceId;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void deleteItem(ClipEntity e) {
        int pos = shown.indexOf(e);
        lastDeleted = e;
        actionId = -1;
        menu = null;
        menuRoot = null;
        undoBar.setVisibility(VISIBLE);
        removeCallbacks(hideUndo);
        postDelayed(hideUndo, 4000);
        if (pos >= 0) {
            shown.remove(pos);
            adapter.notifyItemRemoved(pos);
        }
        all.remove(e);
        if (shown.isEmpty()) { empty.setVisibility(VISIBLE); list.setVisibility(GONE); }
        Bg.io(() -> {
            ClipboardRepository.delete(getContext(), e);
            publish();
        });
    }

    private android.widget.PopupWindow menu;
    private View menuRoot;
    private boolean menuClosing;
    private float touchX, touchY;

    private void dismissMenu() {
        if (menu == null) return;
        android.widget.PopupWindow m = menu;
        menu = null;
        menuRoot = null;
        menuClosing = false;
        try { m.dismiss(); } catch (RuntimeException ignored) { }
        clearHighlight();
    }

    private void clearHighlight() {
        if (actionId == -1) return;
        int old = adapter.indexOfId(actionId);
        actionId = -1;
        if (old >= 0) adapter.notifyItemChanged(old);
    }

    private void closeMenu(Runnable after) {
        if (menu == null || menuRoot == null || menuClosing) {
            if (after != null) after.run();
            return;
        }
        menuClosing = true;
        final android.widget.PopupWindow m = menu;
        menuRoot.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(110)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    if (menu == m) dismissMenu();
                    if (after != null) after.run();
                }).start();
    }

    private View menuItem(Context c, int icon, String label, int color, Runnable action) {
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setPadding(dp(11), dp(11), dp(11), dp(11));
        iv.setContentDescription(label);
        iv.setClickable(true);
        iv.setFocusable(true);
        iv.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(theme.withAlpha(color, 24)), null,
                new android.graphics.drawable.ColorDrawable(0xFFFFFFFF)));
        android.graphics.drawable.Drawable d = tintable(icon);
        if (d != null) {
            DrawableCompat.setTint(d, color);
            iv.setImageDrawable(d);
        }
        iv.setOnClickListener(v -> closeMenu(action));
        return iv;
    }

    private void showMenu(View card, ClipEntity e) {
        if (theme == null) return;
        dismissMenu();
        Context c = getContext();

        int surface = theme.popup != 0 ? theme.popup : theme.key;
        int content = theme.popup != 0 && theme.popupText != 0 ? theme.popupText : theme.text;

        LinearLayout items = new LinearLayout(c);
        items.setOrientation(VERTICAL);
        GradientDrawable bg = round(surface, 14, dp(1), theme.stroke != 0 ? theme.stroke : theme.withAlpha(content, 14));
        items.setBackground(bg);
        items.setClipToOutline(true);
        items.setElevation(dp(6));

        View pinItem = menuItem(c, e.pinned ? R.drawable.ic_pin_off : R.drawable.ic_pin,
                str(e.pinned ? R.string.clip_action_unpin : R.string.clip_action_pin), theme.accent, () -> {
                    clearHighlight();
                    Bg.io(() -> {
                        ClipboardRepository.togglePin(getContext(), e);
                        publish();
                    });
                });
        View delItem = menuItem(c, R.drawable.ic_delete, str(R.string.delete), c.getColor(R.color.danger), () -> deleteItem(e));
        View divider = new View(c);
        divider.setBackgroundColor(theme.withAlpha(content, 12));
        items.addView(pinItem, new LayoutParams(dp(44), dp(44)));
        LayoutParams dlp = new LayoutParams(dp(24), 1);
        dlp.gravity = Gravity.CENTER_HORIZONTAL;
        items.addView(divider, dlp);
        items.addView(delItem, new LayoutParams(dp(44), dp(44)));

        int shadow = dp(8);
        android.widget.FrameLayout root = new android.widget.FrameLayout(c);
        root.setPadding(shadow, shadow, shadow, shadow);
        root.setClipToPadding(false);
        root.setClipChildren(false);
        root.setLayoutDirection(getLayoutDirection());
        root.addView(items, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int mw = root.getMeasuredWidth(), mh = root.getMeasuredHeight();

        int[] loc = new int[2];
        card.getLocationInWindow(loc);
        View rv = card.getRootView();
        int winW = rv.getWidth() > 0 ? rv.getWidth() : getResources().getDisplayMetrics().widthPixels;
        int winH = rv.getHeight() > 0 ? rv.getHeight() : getResources().getDisplayMetrics().heightPixels;
        int cardL = loc[0], cardR = loc[0] + card.getWidth(), cardT = loc[1], cardH = card.getHeight();
        int gap = dp(4) - shadow;
        int rightX = cardR + gap;
        int leftX = cardL - mw - gap;
        boolean preferRight = (cardL + cardR) / 2 < winW / 2;
        int left;
        boolean sideRight;
        if (preferRight && rightX + mw <= winW) { left = rightX; sideRight = true; }
        else if (leftX >= 0) { left = leftX; sideRight = false; }
        else if (rightX + mw <= winW) { left = rightX; sideRight = true; }
        else {
            left = Math.max(0, Math.min(winW - mw, (cardL + cardR) / 2 - mw / 2));
            sideRight = preferRight;
        }
        int top = cardT + (cardH - mh) / 2;
        top = Math.max(0, Math.min(Math.max(0, winH - mh), top));
        final float pivX = sideRight ? shadow : mw - shadow;
        final float pivY = Math.max(0, Math.min(mh, cardT + cardH / 2f - top));

        android.widget.PopupWindow pw = new android.widget.PopupWindow(root, mw, mh, false);
        pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        pw.setOutsideTouchable(true);
        pw.setClippingEnabled(false);
        pw.setOnDismissListener(() -> {
            if (menu == pw) { menu = null; menuRoot = null; menuClosing = false; clearHighlight(); }
        });

        int old = adapter.indexOfId(actionId);
        actionId = e.id;
        if (old >= 0) adapter.notifyItemChanged(old);
        int now = adapter.indexOfId(e.id);
        if (now >= 0) adapter.notifyItemChanged(now);

        menu = pw;
        menuRoot = root;
        menuClosing = false;
        root.setPivotX(pivX);
        root.setPivotY(pivY);
        root.setAlpha(0f);
        root.setScaleX(0.7f);
        root.setScaleY(0.7f);
        try {
            pw.showAtLocation(card, Gravity.TOP | Gravity.LEFT, left, top);
        } catch (RuntimeException ex) {
            menu = null; menuRoot = null; clearHighlight();
            return;
        }
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
        for (int i = 0; i < items.getChildCount(); i++) {
            View ch = items.getChildAt(i);
            ch.setAlpha(0f);
            ch.setScaleX(0.6f);
            ch.setScaleY(0.6f);
            ch.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(40L + i * 40L).setDuration(160)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
        }
    }

    @Override protected void onDetachedFromWindow() {
        dismissMenu();
        super.onDetachedFromWindow();
    }

    @Override protected void onVisibilityChanged(@NonNull View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        if (visibility != VISIBLE) dismissMenu();
    }

    private void attachSwipe() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override public int getSwipeDirs(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h) {
                int pos = h.getBindingAdapterPosition();
                if (pos < 0 || pos >= shown.size() || shown.get(pos).pinned) return 0;
                return super.getSwipeDirs(r, h);
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder h, int dir) {
                int pos = h.getBindingAdapterPosition();
                if (pos < 0 || pos >= shown.size()) return;
                deleteItem(shown.get(pos));
            }

            @Override public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h,
                                              float dX, float dY, int state, boolean active) {
                h.itemView.setAlpha(1f - Math.min(1f, Math.abs(dX) / Math.max(1f, h.itemView.getWidth())) * 0.7f);
                super.onChildDraw(c, r, h, dX, dY, state, active);
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(list);
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context c = parent.getContext();
            android.widget.FrameLayout card = new android.widget.FrameLayout(c);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            StaggeredGridLayoutManager.LayoutParams rlp = new StaggeredGridLayoutManager.LayoutParams(
                    StaggeredGridLayoutManager.LayoutParams.MATCH_PARENT,
                    StaggeredGridLayoutManager.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(dp(3), dp(3), dp(3), dp(3));
            card.setLayoutParams(rlp);

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(VERTICAL);
            TextView tv = new TextView(c);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setMaxLines(5);
            tv.setMinLines(2);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(tv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            TextView meta = new TextView(c);
            meta.setVisibility(GONE);
            col.addView(meta, new LayoutParams(0, 0));
            card.addView(col, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));

            ImageButton pin = new ImageButton(c);
            pin.setBackground(null);
            pin.setPadding(dp(4), dp(4), dp(4), dp(4));
            pin.setScaleType(ImageView.ScaleType.FIT_CENTER);
            pin.setRotation(-35f);
            android.widget.FrameLayout.LayoutParams plp0 = new android.widget.FrameLayout.LayoutParams(dp(24), dp(24),
                    Gravity.TOP | Gravity.END);
            plp0.topMargin = -dp(6);
            plp0.setMarginEnd(-dp(8));
            card.addView(pin, plp0);

            return new Holder(card, tv, meta, pin, col);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            ClipEntity e = shown.get(position);
            h.itemView.setAlpha(1f);
            ViewGroup.LayoutParams lp0 = h.itemView.getLayoutParams();
            if (lp0 instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) lp0).setFullSpan(false);
            }
            h.text.setText(e.text);
            h.meta.setText(relativeTime(e.createdAt));
            h.pin.setContentDescription(str(R.string.cd_pin));
            h.pin.setImageDrawable(tintable(e.pinned ? R.drawable.ic_pin : R.drawable.ic_pin_off));
            if (theme != null) {
                h.itemView.setBackground(round(theme.glassColor(), 14, dp(1), edge()));
                h.text.setTextColor(isLink(e.text) ? theme.accent : theme.text);
                h.meta.setTextColor(theme.hint);
                if (h.pin.getDrawable() != null) DrawableCompat.setTint(h.pin.getDrawable(), e.pinned ? theme.accent : theme.hint);
            }
            boolean acting = e.id == actionId;
            h.pin.setVisibility(e.pinned ? VISIBLE : GONE);
            if (acting && theme != null) {
                h.itemView.setBackground(round(theme.withAlpha(theme.accent, 12), 14, dp(1), theme.accent));
            }
            h.itemView.setOnTouchListener((v, ev) -> {
                if (ev.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    touchX = ev.getX();
                    touchY = ev.getY();
                }
                return false;
            });
            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onPaste(e.text); });
            h.itemView.setOnLongClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                showMenu(v, e);
                return true;
            });
            h.pin.setOnClickListener(v -> Bg.io(() -> {
                ClipboardRepository.togglePin(getContext(), e);
                publish();
            }));
        }

        private int indexOfId(long id) {
            for (int i = 0; i < shown.size(); i++) if (shown.get(i).id == id) return i;
            return -1;
        }

        @Override public int getItemCount() { return shown.size(); }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;
        final TextView meta;
        final ImageButton pin;
        final View col;
        Holder(View v, TextView text, TextView meta, ImageButton pin, View col) {
            super(v);
            this.text = text;
            this.meta = meta;
            this.pin = pin;
            this.col = col;
        }
    }
}
