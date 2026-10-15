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

package com.keyline.ime.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.keyline.ime.R;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public final class SettingsUi {
    private final Context ctx;

    public SettingsUi(Context ctx) {
        this.ctx = ctx;
    }

    public int dp(float value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    public int color(@AttrRes int attr, int fallback) {
        return MaterialColors.getColor(ctx, attr, fallback);
    }

    public int onSurface() {
        return color(com.google.android.material.R.attr.colorOnSurface, 0xFF1A1C1E);
    }

    public int onSurfaceVariant() {
        return color(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF44474E);
    }

    private TextView text(CharSequence value, int appearance, int color) {
        TextView view = new TextView(ctx);
        TextViewCompat.setTextAppearance(view, appearance);
        view.setTextColor(color);
        view.setText(value);
        return view;
    }

    private static LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public View header(CharSequence title, boolean back, @DrawableRes int backIcon,
                       @Nullable Runnable onBack) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(20), dp(8));
        row.setMinimumHeight(dp(68));

        if (back) {
            ImageView button = new ImageView(ctx);
            button.setImageResource(backIcon);
            button.setImageTintList(ColorStateList.valueOf(onSurface()));
            button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int pad = dp(11);
            button.setPadding(pad, pad, pad, pad);
            GradientDrawable fill = new GradientDrawable();
            fill.setShape(GradientDrawable.OVAL);
            fill.setColor(color(com.google.android.material.R.attr.colorSurfaceContainerHighest,
                    0xFFE4E1E9));
            button.setBackground(new android.graphics.drawable.RippleDrawable(
                    ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(
                            color(com.google.android.material.R.attr.colorPrimary, 0xFF4A55C9),
                            0x38)), fill, null));
            button.setContentDescription(ctx.getString(R.string.cd_close));
            button.setClickable(true);
            button.setFocusable(true);
            if (onBack != null) button.setOnClickListener(v -> onBack.run());
            row.addView(button, new LinearLayout.LayoutParams(dp(46), dp(46)));
        } else {
            ImageView mark = new ImageView(ctx);
            mark.setImageResource(R.drawable.ic_keyboard);
            mark.setImageTintList(ColorStateList.valueOf(color(
                    com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF)));
            mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int pad = dp(11);
            mark.setPadding(pad, pad, pad, pad);
            GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{color(com.google.android.material.R.attr.colorPrimary, 0xFF4A55C9),
                            color(com.google.android.material.R.attr.colorTertiary, 0xFF7D4B6A)});
            fill.setCornerRadius(dp(16));
            mark.setBackground(fill);
            row.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));
        }

        TextView label = text(title, back
                ? com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
                : com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium,
                onSurface());
        label.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(dp(14));
        row.addView(label, lp);
        return row;
    }

    public void group(@NonNull LinearLayout parent, int titleRes) {
        group(parent, ctx.getString(titleRes));
    }

    public void group(@NonNull LinearLayout parent, CharSequence title) {
        TextView label = text(title,
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge,
                color(com.google.android.material.R.attr.colorPrimary, 0xFF1A73E8));
        label.setPadding(dp(28), dp(20), dp(28), dp(8));
        parent.addView(label, match());
    }

    public LinearLayout card(@NonNull LinearLayout parent) {
        return card(parent, color(com.google.android.material.R.attr.colorSurfaceContainerLow,
                0xFFF5F2FA), true);
    }

    public LinearLayout tonalCard(@NonNull LinearLayout parent) {
        return card(parent, color(com.google.android.material.R.attr.colorPrimaryContainer,
                0xFFD3E3FD), false);
    }

    private static int glass(int color, int alpha) {
        return androidx.core.graphics.ColorUtils.setAlphaComponent(color, alpha);
    }

    private LinearLayout card(LinearLayout parent, int background, boolean outlined) {
        MaterialCardView card = new MaterialCardView(ctx);
        card.setRadius(dp(28));
        card.setCardElevation(0);
        card.setCardBackgroundColor(glass(background, outlined ? 0xC8 : 0xE0));
        card.setStrokeWidth(outlined ? dp(1) : 0);
        card.setStrokeColor(glass(color(com.google.android.material.R.attr.colorOutlineVariant,
                0xFFC7C5D0), 0x90));
        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.VERTICAL);
        card.addView(inner, match());
        LinearLayout.LayoutParams lp = match();
        lp.setMargins(dp(16), 0, dp(16), dp(4));
        parent.addView(card, lp);
        return inner;
    }

    public void divider(@NonNull LinearLayout parent) {
        View line = new View(ctx);
        line.setBackgroundColor(color(com.google.android.material.R.attr.colorOutlineVariant, 0xFFE1E3E6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(20), 0, dp(20), 0);
        parent.addView(line, lp);
    }

    private LinearLayout textColumn(CharSequence title, @Nullable CharSequence subtitle) {
        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(text(title,
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge, onSurface()));
        if (subtitle != null && subtitle.length() > 0) {
            column.addView(text(subtitle,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                    onSurfaceVariant()));
        }
        return column;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        return row;
    }

    private View iconBadge(@DrawableRes int icon) {
        return iconBadge(icon, 12);
    }

    private int badgeIndex;

    private View iconBadge(@DrawableRes int icon, int padDp) {
        int slot = badgeIndex++ % 3;
        int fillAttr = slot == 0 ? com.google.android.material.R.attr.colorPrimaryContainer
                : slot == 1 ? com.google.android.material.R.attr.colorTertiaryContainer
                : com.google.android.material.R.attr.colorSecondaryContainer;
        int onAttr = slot == 0 ? com.google.android.material.R.attr.colorOnPrimaryContainer
                : slot == 1 ? com.google.android.material.R.attr.colorOnTertiaryContainer
                : com.google.android.material.R.attr.colorOnSecondaryContainer;
        ImageView image = new ImageView(ctx);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(color(onAttr, 0xFF1B1B21)));
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int pad = dp(padDp);
        image.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        bg.setColor(color(fillAttr, 0xFFE1E3FF));
        image.setBackground(bg);
        return image;
    }

    public void applyRipple(View row) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        row.setClickable(true);
        row.setFocusable(true);
    }

    public void navRow(@NonNull LinearLayout parent, @DrawableRes int icon, CharSequence title,
                       @Nullable CharSequence subtitle, boolean chevron, Runnable click) {
        LinearLayout row = row();
        if (icon != 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMarginEnd(dp(16));
            row.addView(iconBadge(icon), lp);
        }
        row.addView(textColumn(title, subtitle), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (chevron) {
            ImageView arrow = new ImageView(ctx);
            arrow.setImageResource(R.drawable.ic_chevron);
            arrow.setImageTintList(ColorStateList.valueOf(onSurfaceVariant()));
            row.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(24)));
        }
        applyRipple(row);
        row.setOnClickListener(v -> click.run());
        parent.addView(row, match());
    }

    public ImageView iconAction(@DrawableRes int icon, CharSequence description,
                                @Nullable Runnable click) {
        ImageView button = new ImageView(ctx);
        button.setImageResource(icon);
        button.setImageTintList(ColorStateList.valueOf(onSurfaceVariant()));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int pad = dp(8);
        button.setPadding(pad, pad, pad, pad);
        android.util.TypedValue tv = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
                tv, true)) {
            button.setBackgroundResource(tv.resourceId);
        }
        button.setContentDescription(description);
        button.setClickable(true);
        button.setFocusable(true);
        if (click != null) button.setOnClickListener(v -> click.run());
        return button;
    }

    private View letterBadge(String letters) {
        TextView badge = new TextView(ctx);
        TextViewCompat.setTextAppearance(badge,
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        badge.setText(letters);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(color(
                com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF0B1470));
        badge.setTextDirection(View.TEXT_DIRECTION_LTR);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        bg.setColor(color(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFE1E3FF));
        badge.setBackground(bg);
        return badge;
    }

    public LinearLayout listItem(@NonNull LinearLayout parent, @Nullable String badgeLetters,
                                 CharSequence title, @Nullable CharSequence subtitle,
                                 @Nullable View trailing, @Nullable Runnable click) {
        LinearLayout row = row();
        if (badgeLetters != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMarginEnd(dp(16));
            row.addView(letterBadge(badgeLetters), lp);
        }
        LinearLayout column = textColumn(title, subtitle);
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            child.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            child.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
        row.addView(column, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (trailing != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(dp(8));
            row.addView(trailing, lp);
        }
        if (click != null) {
            applyRipple(row);
            row.setOnClickListener(v -> click.run());
        }
        parent.addView(row, match());
        return row;
    }

    public void emptyState(@NonNull LinearLayout parent, @DrawableRes int icon,
                           CharSequence title, @Nullable CharSequence hint) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(32), dp(24), dp(32));
        ImageView image = new ImageView(ctx);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(onSurfaceVariant()));
        image.setAlpha(0.7f);
        box.addView(image, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView head = text(title,
                com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
                onSurface());
        head.setGravity(Gravity.CENTER);
        head.setPadding(0, dp(12), 0, 0);
        box.addView(head, match());
        if (hint != null && hint.length() > 0) {
            TextView sub = text(hint,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                    onSurfaceVariant());
            sub.setGravity(Gravity.CENTER);
            sub.setPadding(0, dp(4), 0, 0);
            box.addView(sub, match());
        }
        parent.addView(box, match());
    }

    public static final class Tile {
        final int icon;
        final CharSequence title;
        final Runnable click;

        public Tile(@DrawableRes int icon, CharSequence title, Runnable click) {
            this.icon = icon;
            this.title = title;
            this.click = click;
        }
    }

    public void tileGrid(@NonNull LinearLayout parent, @NonNull List<Tile> tiles) {
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout line = new LinearLayout(ctx);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(dp(12), i == 0 ? dp(10) : dp(4), dp(12), dp(4));
            line.addView(tile(tiles.get(i)), tileParams());
            if (i + 1 < tiles.size()) line.addView(tile(tiles.get(i + 1)), tileParams());
            parent.addView(line, match());
        }
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private View tile(Tile t) {
        MaterialCardView card = new MaterialCardView(ctx);
        card.setRadius(dp(24));
        card.setCardElevation(0);
        card.setCardBackgroundColor(glass(color(
                com.google.android.material.R.attr.colorSurfaceContainerLow, 0xFFF5F2FA), 0xC8));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(glass(color(com.google.android.material.R.attr.colorOutlineVariant,
                0xFFC7C5D0), 0x90));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> t.click.run());

        LinearLayout inner = new LinearLayout(ctx);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(12), dp(8), dp(12), dp(8));
        inner.setMinimumHeight(dp(64));
        LinearLayout.LayoutParams badge = new LinearLayout.LayoutParams(dp(40), dp(40));
        badge.setMarginEnd(dp(10));
        inner.addView(iconBadge(t.icon, 10), badge);
        TextView label = text(t.title,
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, onSurface());
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        inner.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(inner, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    public void switchRow(@NonNull LinearLayout parent, int titleRes, int subtitleRes,
                          boolean checked, Consumer<Boolean> save) {
        LinearLayout row = row();
        row.addView(textColumn(ctx.getString(titleRes),
                subtitleRes == 0 ? null : ctx.getString(subtitleRes)),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialSwitch toggle = new MaterialSwitch(ctx);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, value) -> save.accept(value));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(dp(12));
        row.addView(toggle, lp);
        applyRipple(row);
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        parent.addView(row, match());
    }

    public LinearLayout sliderRow(@NonNull LinearLayout parent, int titleRes, int min, int max, int initial,
                                  IntFunction<String> formatter, IntConsumer save) {
        return sliderRow(parent, titleRes, min, max, initial, formatter, save, null);
    }

    public LinearLayout sliderRow(@NonNull LinearLayout parent, int titleRes, int min, int max, int initial,
                                  IntFunction<String> formatter, IntConsumer save, @Nullable IntConsumer live) {
        int start = Math.max(min, Math.min(max, initial));
        LinearLayout block = new LinearLayout(ctx);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(20), dp(14), dp(20), dp(6));
        LinearLayout header = new LinearLayout(ctx);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(ctx.getString(titleRes),
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge, onSurface()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text(formatter.apply(start),
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge,
                color(com.google.android.material.R.attr.colorPrimary, 0xFF1A73E8));
        header.addView(value);
        block.addView(header, match());

        Slider slider = new Slider(ctx);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(1f);
        slider.setTickVisible(false);
        slider.setValue(start);
        slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        slider.setContentDescription(ctx.getString(titleRes));
        slider.addOnChangeListener((s, v, fromUser) -> {
            value.setText(formatter.apply(Math.round(v)));
            if (live != null && fromUser) live.accept(Math.round(v));
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider s) {}
            @Override public void onStopTrackingTouch(@NonNull Slider s) {
                save.accept(Math.round(s.getValue()));
            }
        });
        block.addView(slider, match());
        parent.addView(block, match());
        return block;
    }

    public void setRowEnabled(@NonNull View row, boolean enabled) {
        row.setAlpha(enabled ? 1f : 0.38f);
        setEnabledDeep(row, enabled);
    }

    private void setEnabledDeep(View v, boolean enabled) {
        v.setEnabled(enabled);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) setEnabledDeep(g.getChildAt(i), enabled);
        }
    }

    public void button(@NonNull LinearLayout parent, int textRes, boolean tonal, Runnable click) {
        int style = tonal
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle;
        MaterialButton button = new MaterialButton(ctx, null, style);
        button.setText(textRes);
        button.setOnClickListener(v -> click.run());
        LinearLayout.LayoutParams lp = match();
        lp.setMargins(dp(16), dp(6), dp(16), dp(6));
        parent.addView(button, lp);
    }

    public MaterialButton iconButton(@NonNull LinearLayout parent, int textRes,
                                     @DrawableRes int icon, boolean filled, Runnable click) {
        int style = filled
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle;
        MaterialButton button = new MaterialButton(ctx, null, style);
        button.setText(textRes);
        button.setIconResource(icon);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconPadding(dp(8));
        button.setOnClickListener(v -> click.run());
        LinearLayout.LayoutParams lp = match();
        lp.setMargins(dp(16), dp(12), dp(16), dp(4));
        parent.addView(button, lp);
        return button;
    }

    public MaterialButton tonalButton(@NonNull LinearLayout parent, int textRes, Runnable click) {
        MaterialButton button = new MaterialButton(ctx, null,
                com.google.android.material.R.attr.materialButtonStyle);
        button.setText(textRes);
        button.setBackgroundTintList(ColorStateList.valueOf(color(
                com.google.android.material.R.attr.colorPrimary, 0xFF1A73E8)));
        button.setTextColor(color(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF));
        button.setOnClickListener(v -> click.run());
        LinearLayout.LayoutParams lp = match();
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(button, lp);
        return button;
    }

    public TextView body(@NonNull LinearLayout parent, CharSequence value, boolean secondary) {
        TextView view = text(value,
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                secondary ? onSurfaceVariant() : onSurface());
        view.setPadding(dp(20), dp(4), dp(20), dp(10));
        parent.addView(view, match());
        return view;
    }

    public void stylePill(TextView pill, boolean done) {
        int tone = ctx.getColor(done ? R.color.ok : R.color.warn);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(100));
        bg.setColor((tone & 0x00FFFFFF) | 0x26000000);
        pill.setBackground(bg);
        pill.setTextColor(tone);
        pill.setPadding(dp(12), dp(4), dp(12), dp(4));
        pill.setText(done ? R.string.status_done : R.string.status_needed);
    }

    public TextView pill() {
        TextView pill = new TextView(ctx);
        TextViewCompat.setTextAppearance(pill,
                com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        return pill;
    }

    public TextView titleText(CharSequence value, int appearance) {
        return text(value, appearance, onSurface());
    }
}
