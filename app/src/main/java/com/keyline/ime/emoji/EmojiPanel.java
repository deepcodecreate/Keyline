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

package com.keyline.ime.emoji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.emoji2.widget.EmojiTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.keyline.ime.R;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmojiPanel extends LinearLayout {
    public interface Listener {
        void onEmoji(String emoji);
        void onBackspace();
        void onLetters();
    }

    private static final String[] CATS = {
            "recent", "smileys", "people", "animals", "food", "activity", "travel", "objects", "symbols", "flags"
    };
    private static final int[] ICONS = {
            R.drawable.ic_emo_recent, R.drawable.ic_emo_smile, R.drawable.ic_emo_people, R.drawable.ic_emo_animals,
            R.drawable.ic_emo_food, R.drawable.ic_emo_activity, R.drawable.ic_emo_travel, R.drawable.ic_emo_objects,
            R.drawable.ic_emo_symbols, R.drawable.ic_emo_flags
    };
    private static final int COLUMNS = 8;

    private final RecyclerView grid;
    private final LinearLayout bar;
    private final LinearLayout tabs;
    private final HorizontalScrollView tabScroll;
    private final TextView abc;
    private final ImageButton del;
    private final Adapter adapter = new Adapter();
    private final GridLayoutManager glm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Listener listener;
    private KeyboardTheme theme;
    private Context strCtx;
    private String langTag = "";
    private List<EmojiRepository.Item> items = new ArrayList<>();
    private final Map<String, String> titles = new HashMap<>();
    private final ImageButton[] catButtons = new ImageButton[CATS.length];
    private final int[] catStart = new int[CATS.length];
    private int selected = 1;

    private final Runnable repeatDelete = new Runnable() {
        @Override public void run() {
            if (listener != null) listener.onBackspace();
            handler.postDelayed(this, 55);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    public EmojiPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        strCtx = context;
        fillTitles();

        grid = new RecyclerView(context);
        glm = new GridLayoutManager(context, COLUMNS);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                if (position < 0 || position >= items.size()) return 1;
                return items.get(position).header ? COLUMNS : 1;
            }
        });
        grid.setLayoutManager(glm);
        grid.setAdapter(adapter);
        grid.setPadding(dp(4), 0, dp(4), 0);
        grid.setClipToPadding(false);
        grid.setOverScrollMode(OVER_SCROLL_NEVER);
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) { syncTabToScroll(); }
        });
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        bar = new LinearLayout(context);
        bar.setOrientation(HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(2), dp(8), dp(2));

        abc = new TextView(context);
        abc.setText(strCtx.getString(R.string.back_letters));
        abc.setGravity(Gravity.CENTER);
        abc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        abc.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        abc.setOnClickListener(v -> { if (listener != null) listener.onLetters(); });
        bar.addView(abc, new LayoutParams(dp(52), dp(36)));

        tabScroll = new HorizontalScrollView(context);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setOverScrollMode(OVER_SCROLL_NEVER);
        tabs = new LinearLayout(context);
        tabs.setOrientation(HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < CATS.length; i++) {
            ImageButton b = new ImageButton(context);
            b.setImageDrawable(AppCompatResources.getDrawable(context, ICONS[i]));
            if (b.getDrawable() != null) b.getDrawable().mutate();
            b.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            b.setPadding(dp(10), dp(8), dp(10), dp(10));
            b.setContentDescription(titles.get(CATS[i]));
            final int idx = i;
            b.setOnClickListener(v -> { select(idx); scrollTo(CATS[idx]); });
            LayoutParams lp = new LayoutParams(dp(42), dp(40));
            lp.setMargins(dp(1), 0, dp(1), 0);
            tabs.addView(b, lp);
            catButtons[i] = b;
        }
        tabScroll.addView(tabs, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.MATCH_PARENT));
        LayoutParams tlp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        tlp.setMargins(dp(6), 0, dp(6), 0);
        bar.addView(tabScroll, tlp);

        del = new ImageButton(context);
        del.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_backspace));
        if (del.getDrawable() != null) del.getDrawable().mutate();
        del.setScaleType(ImageButton.ScaleType.CENTER);
        del.setContentDescription(strCtx.getString(R.string.cd_backspace));
        del.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    if (listener != null) listener.onBackspace();
                    handler.postDelayed(repeatDelete, 400);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    handler.removeCallbacks(repeatDelete);
                    return true;
                default:
                    return true;
            }
        });
        bar.addView(del, new LayoutParams(dp(52), dp(36)));
        addView(bar, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));
    }

    public void setListener(Listener l) { listener = l; }

    private void fillTitles() {
        titles.clear();
        titles.put("recent", strCtx.getString(R.string.emoji_recents));
        titles.put("smileys", strCtx.getString(R.string.emoji_smileys));
        titles.put("people", strCtx.getString(R.string.emoji_people));
        titles.put("animals", strCtx.getString(R.string.emoji_animals));
        titles.put("food", strCtx.getString(R.string.emoji_food));
        titles.put("activity", strCtx.getString(R.string.emoji_activity));
        titles.put("travel", strCtx.getString(R.string.emoji_travel));
        titles.put("objects", strCtx.getString(R.string.emoji_objects));
        titles.put("symbols", strCtx.getString(R.string.emoji_symbols));
        titles.put("flags", strCtx.getString(R.string.emoji_flags));
    }

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
        fillTitles();
        abc.setText(strCtx.getString(R.string.back_letters));
        del.setContentDescription(strCtx.getString(R.string.cd_backspace));
        for (int i = 0; i < CATS.length; i++) catButtons[i].setContentDescription(titles.get(CATS[i]));
        if (getVisibility() == VISIBLE) reload();
    }

    public void apply(KeyboardTheme t) {
        theme = t;
        setBackgroundColor(t.background);
        bar.setBackgroundColor(t.toolbar);
        abc.setTextColor(t.accent);
        abc.setBackground(flat(t));
        del.setBackground(flat(t));
        if (del.getDrawable() != null) DrawableCompat.setTint(del.getDrawable(), t.text);
        styleTabs();
        adapter.notifyDataSetChanged();
    }

    public void reload() {
        items = EmojiRepository.browse(getContext(), strCtx.getString(R.string.emoji_recents), titles);
        for (int i = 0; i < CATS.length; i++) {
            catStart[i] = i == 0 ? 0 : EmojiRepository.indexOfCategory(items, CATS[i], titles);
        }
        adapter.notifyDataSetChanged();
        boolean hasRecents = !EmojiRepository.recents(getContext()).isEmpty();
        select(hasRecents ? 0 : 1);
        grid.scrollToPosition(0);
        if (!hasRecents) scrollTo("smileys");
    }

    private void scrollTo(String cat) {
        if ("recent".equals(cat)) {
            grid.scrollToPosition(0);
            return;
        }
        int idx = EmojiRepository.indexOfCategory(items, cat, titles);
        glm.scrollToPositionWithOffset(idx, 0);
    }

    private void syncTabToScroll() {
        int first = glm.findFirstVisibleItemPosition();
        if (first < 0) return;
        int cur = 0;
        for (int i = 0; i < CATS.length; i++) {
            if (i > 0 && catStart[i] == 0) continue;
            if (catStart[i] <= first) cur = i;
        }
        if (cur != selected) select(cur);
    }

    private void select(int index) {
        selected = index;
        styleTabs();
        View b = catButtons[index];
        int target = b.getLeft() - (tabScroll.getWidth() - b.getWidth()) / 2;
        tabScroll.smoothScrollTo(Math.max(0, target), 0);
    }

    private android.graphics.drawable.Drawable flat(KeyboardTheme t) {
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(18));
        mask.setColor(0xFF000000);
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(t.withAlpha(t.text, 20)), null, mask);
    }

    private android.graphics.drawable.Drawable underline(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(2));
        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{g});
        ld.setLayerInset(0, dp(13), dp(37), dp(13), 0);
        return ld;
    }

    private void styleTabs() {
        if (theme == null) return;
        for (int i = 0; i < catButtons.length; i++) {
            boolean on = i == selected;
            catButtons[i].setBackground(on ? underline(theme.accent) : null);
            if (catButtons[i].getDrawable() != null) {
                DrawableCompat.setTint(catButtons[i].getDrawable(), on ? theme.accent : theme.hint);
                catButtons[i].setAlpha(on ? 1f : 0.85f);
            }
        }
    }

    private GradientDrawable pill(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(18));
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemViewType(int position) {
            return items.get(position).header ? 0 : 1;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            if (viewType == 0) {
                TextView tv = new TextView(parent.getContext());
                tv.setPadding(dp(12), dp(12), dp(12), dp(4));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                tv.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                return new HeaderHolder(tv);
            }
            EmojiTextView tv = new EmojiTextView(parent.getContext());
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(26);
            TypedValue out = new TypedValue();
            parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, out, true);
            tv.setBackgroundResource(out.resourceId);
            tv.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, dp(44)));
            return new EmojiHolder(tv);
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            EmojiRepository.Item item = items.get(position);
            if (holder instanceof HeaderHolder) {
                TextView tv = (TextView) holder.itemView;
                tv.setText(item.title);
                if (theme != null) tv.setTextColor(theme.hint);
            } else {
                EmojiTextView tv = (EmojiTextView) holder.itemView;
                tv.setText(item.emoji.value);
                tv.setOnClickListener(v -> choose(item.emoji.value));
                tv.setOnLongClickListener(v -> {
                    if (!item.emoji.tones) return false;
                    showTones(v, item.emoji.value);
                    return true;
                });
            }
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private void choose(String emoji) {
        EmojiRepository.pushRecent(getContext(), emoji);
        if (listener != null) listener.onEmoji(emoji);
    }

    private void showTones(View anchor, String base) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(theme == null ? 0xFFFFFFFF : theme.popup);
        row.setBackground(bg);
        for (int i = 0; i < 6; i++) {
            final String pick = EmojiRepository.withTone(base, i);
            EmojiTextView tv = new EmojiTextView(getContext());
            tv.setText(pick);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(22);
            tv.setPadding(dp(8), dp(4), dp(8), dp(4));
            tv.setOnClickListener(v -> {
                tonePopup.dismiss();
                choose(pick);
            });
            row.addView(tv);
        }
        tonePopup = new android.widget.PopupWindow(row, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
        tonePopup.setElevation(dp(8));
        tonePopup.showAsDropDown(anchor, 0, -dp(56));
    }

    private android.widget.PopupWindow tonePopup;

    private static class HeaderHolder extends RecyclerView.ViewHolder {
        HeaderHolder(View v) { super(v); }
    }

    private static class EmojiHolder extends RecyclerView.ViewHolder {
        EmojiHolder(View v) { super(v); }
    }
}
