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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;

public final class ColorPickerDialog {
    public interface Callback {
        void onPicked(int color);
    }

    private static final int[] PRESETS = {
            0xFFFFFFFF, 0xFFF1F3F4, 0xFFE8EAF0, 0xFFCFD8DC, 0xFF90A4AE, 0xFF5F6368,
            0xFF263238, 0xFF202124, 0xFF121212, 0xFF000000, 0xFFB3261E, 0xFFD81B60,
            0xFF6750A4, 0xFF3F51B5, 0xFF0B57D0, 0xFF00838F, 0xFF008577, 0xFF2E7D32,
            0xFF9E9D24, 0xFFFFB300, 0xFFE37400, 0xFF8D6E63, 0xFFFF80AB, 0xFF80D8FF
    };

    private ColorPickerDialog() {}

    public static void show(Context ctx, String title, int initial, boolean allowAlpha,
                            boolean allowAuto, Callback callback) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int dp8 = Math.round(8 * density);
        float[] hsv = new float[3];
        int startColor = initial == 0 ? 0xFF808080 : initial;
        Color.colorToHSV(startColor, hsv);
        int[] alpha = {allowAlpha ? Color.alpha(startColor) : 255};
        boolean[] syncing = {false};

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Math.round(20 * density), dp8, Math.round(20 * density), dp8);

        LinearLayout compare = new LinearLayout(ctx);
        View oldChip = chip(ctx, initial == 0 ? 0 : initial, density);
        View newChip = chip(ctx, startColor, density);
        compare.addView(oldChip, new LinearLayout.LayoutParams(0, Math.round(44 * density), 1f));
        compare.addView(newChip, new LinearLayout.LayoutParams(0, Math.round(44 * density), 1f));
        box.addView(compare, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ColorPickerView picker = new ColorPickerView(ctx);
        picker.setHsv(hsv[0], hsv[1], hsv[2]);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp8;
        box.addView(picker, plp);

        Slider alphaSlider = null;
        if (allowAlpha) {
            TextView l = new TextView(ctx);
            l.setText(R.string.te_alpha);
            l.setPadding(0, dp8, 0, 0);
            box.addView(l);
            alphaSlider = new Slider(ctx);
            alphaSlider.setValueFrom(0f);
            alphaSlider.setValueTo(100f);
            alphaSlider.setStepSize(1f);
            alphaSlider.setLabelBehavior(LabelFormatter.LABEL_GONE);
            alphaSlider.setValue(Math.round(alpha[0] * 100f / 255f));
            box.addView(alphaSlider);
        }

        TextInputLayout hexLayout = new TextInputLayout(ctx, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        hexLayout.setHint(ctx.getString(R.string.color_hex));
        TextInputEditText hex = new TextInputEditText(hexLayout.getContext());
        hex.setSingleLine(true);
        hex.setTextDirection(View.TEXT_DIRECTION_LTR);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
        hexLayout.addView(hex);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp8;
        box.addView(hexLayout, hlp);

        int[] current = {startColor};
        final Slider alphaFinal = alphaSlider;
        Runnable refresh = () -> {
            int c = Color.HSVToColor(alpha[0], hsv);
            current[0] = c;
            setChipColor(newChip, c, density);
        };
        java.util.function.Consumer<Boolean> writeHex = fromHex -> {
            if (fromHex) return;
            syncing[0] = true;
            int c = current[0];
            hex.setText(allowAlpha && alpha[0] != 255
                    ? String.format(java.util.Locale.ROOT, "#%08X", c)
                    : String.format(java.util.Locale.ROOT, "#%06X", c & 0xFFFFFF));
            hexLayout.setError(null);
            syncing[0] = false;
        };

        picker.setListener((h, s, v) -> {
            hsv[0] = h; hsv[1] = s; hsv[2] = v;
            refresh.run();
            writeHex.accept(false);
        });
        if (alphaFinal != null) {
            alphaFinal.addOnChangeListener((s, value, fromUser) -> {
                if (!fromUser) return;
                alpha[0] = Math.round(value * 255f / 100f);
                refresh.run();
                writeHex.accept(false);
            });
        }
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) {
                if (syncing[0]) return;
                String t = e.toString().trim();
                if (t.startsWith("#")) t = t.substring(1);
                if (t.length() != 6 && !(allowAlpha && t.length() == 8)) {
                    hexLayout.setError(t.isEmpty() ? null : ctx.getString(R.string.color_hex_invalid));
                    return;
                }
                try {
                    long v = Long.parseLong(t, 16);
                    int c = t.length() == 6 ? (0xFF000000 | (int) v) : (int) v;
                    hexLayout.setError(null);
                    Color.colorToHSV(c, hsv);
                    alpha[0] = allowAlpha ? Color.alpha(c) : 255;
                    picker.setHsv(hsv[0], hsv[1], hsv[2]);
                    if (alphaFinal != null) alphaFinal.setValue(Math.round(alpha[0] * 100f / 255f));
                    refresh.run();
                } catch (NumberFormatException ex) {
                    hexLayout.setError(ctx.getString(R.string.color_hex_invalid));
                }
            }
        });

        TextView presetsLabel = new TextView(ctx);
        presetsLabel.setText(R.string.color_presets);
        presetsLabel.setPadding(0, Math.round(14 * density), 0, dp8 / 2);
        box.addView(presetsLabel);
        for (int start = 0; start < PRESETS.length; start += 6) {
            LinearLayout row = new LinearLayout(ctx);
            for (int i = start; i < Math.min(start + 6, PRESETS.length); i++) {
                int color = PRESETS[i];
                View dot = new View(ctx);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(color);
                g.setStroke(Math.round(density), 0x55808080);
                dot.setBackground(g);
                dot.setContentDescription(String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF));
                dot.setOnClickListener(v -> {
                    Color.colorToHSV(color, hsv);
                    picker.setHsv(hsv[0], hsv[1], hsv[2]);
                    refresh.run();
                    writeHex.accept(false);
                });
                android.widget.FrameLayout cell = new android.widget.FrameLayout(ctx);
                int size = Math.round(34 * density);
                cell.addView(dot, new android.widget.FrameLayout.LayoutParams(size, size, Gravity.CENTER));
                row.addView(cell, new LinearLayout.LayoutParams(0, Math.round(44 * density), 1f));
            }
            box.addView(row);
        }

        refresh.run();
        writeHex.accept(false);

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(box);
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(scroll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, w) -> callback.onPicked(current[0]));
        if (allowAuto) b.setNeutralButton(R.string.te_auto, (d, w) -> callback.onPicked(0));
        b.show();
    }

    private static View chip(Context ctx, int color, float density) {
        View v = new View(ctx);
        setChipColor(v, color, density);
        return v;
    }

    private static void setChipColor(View v, int color, float density) {
        GradientDrawable under = new GradientDrawable();
        under.setColor(0xFFB0B0B0);
        under.setCornerRadius(12 * density);
        GradientDrawable top = new GradientDrawable();
        top.setColor(color);
        top.setCornerRadius(12 * density);
        top.setStroke(Math.round(density), 0x55808080);
        v.setBackground(new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{under, top}));
    }
}
