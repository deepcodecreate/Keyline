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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.keyline.ime.R;
import com.keyline.ime.theme.BackgroundImageManager;
import com.keyline.ime.theme.KeyboardTheme;
import com.keyline.ime.theme.ThemeManager;
import com.keyline.ime.util.Bg;

import java.util.ArrayList;
import java.util.List;

public class ThemeEditorActivity extends SettingsPageActivity {
    private interface Getter { int get(KeyboardTheme t); }
    private interface Setter { void set(KeyboardTheme t, int c); }

    private static final class Field {
        final int labelRes;
        final Getter getter;
        final Setter setter;
        final boolean alpha;
        final boolean optional;
        View swatch;
        TextView value;

        Field(int labelRes, Getter g, Setter s, boolean alpha, boolean optional) {
            this.labelRes = labelRes;
            this.getter = g;
            this.setter = s;
            this.alpha = alpha;
            this.optional = optional;
        }
    }

    private final List<Field> fields = new ArrayList<>();
    private KeyboardTheme theme;
    private KeyboardTheme saved;
    private String name = "";
    private KeyboardPreviewView preview;
    private Bitmap previewBitmap;
    private EditText nameField;
    private boolean dirty;

    @Override protected int titleRes() { return R.string.te_title; }

    @Override
    protected View createBody() {
        Prefs prefs = Prefs.get(this);
        KeyboardTheme existing = ThemeManager.customTheme(this);
        theme = existing != null ? existing : ThemeManager.resolve(this).copy();
        saved = theme.copy();
        name = existing != null ? ThemeManager.customThemeName(this) : "";

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        preview = new KeyboardPreviewView(this, null);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(168));
        plp.setMargins(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        column.addView(preview, plp);

        NestedScrollView scroll = new NestedScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, ui.dp(32));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout top = ui.card(content);
        nameField = new EditText(this);
        nameField.setSingleLine(true);
        nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        nameField.setHint(R.string.theme_name_hint);
        nameField.setText(name);
        nameField.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(8));
        top.addView(nameField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ui.divider(top);
        ui.navRow(top, 0, getString(R.string.te_start_from), getString(R.string.te_start_from_sub),
                true, this::chooseBase);
        if (prefs.hasBg()) ui.body(top, getString(R.string.te_image_note), true);

        ui.group(content, R.string.te_sec_surfaces);
        LinearLayout surfaces = ui.card(content);
        row(surfaces, new Field(R.string.te_background, t -> t.background, (t, c) -> t.background = c, false, false));
        row(surfaces, new Field(R.string.te_toolbar, t -> t.toolbar, (t, c) -> t.toolbar = c, true, false));
        row(surfaces, new Field(R.string.te_suggestion, t -> t.suggestion, (t, c) -> t.suggestion = c, true, false));

        ui.group(content, R.string.te_sec_keys);
        LinearLayout keys = ui.card(content);
        row(keys, new Field(R.string.te_key, t -> t.key, (t, c) -> t.key = c, false, false));
        row(keys, new Field(R.string.te_special, t -> t.special, (t, c) -> t.special = c, false, false));
        row(keys, new Field(R.string.te_pressed, t -> t.pressed, (t, c) -> t.pressed = c, true, false));
        row(keys, new Field(R.string.te_stroke, t -> t.stroke, (t, c) -> t.stroke = c, true, false));

        ui.group(content, R.string.te_sec_text);
        LinearLayout texts = ui.card(content);
        row(texts, new Field(R.string.te_text, t -> t.text, (t, c) -> t.text = c, false, false));
        row(texts, new Field(R.string.te_special_text, t -> t.specialText, (t, c) -> t.specialText = c, false, true));
        row(texts, new Field(R.string.te_hint, t -> t.hint, (t, c) -> t.hint = c, true, false));
        row(texts, new Field(R.string.te_toolbar_icon, t -> t.toolbarIcon, (t, c) -> t.toolbarIcon = c, false, true));
        row(texts, new Field(R.string.te_suggestion_text, t -> t.suggestionText, (t, c) -> t.suggestionText = c, false, true));

        ui.group(content, R.string.te_sec_action);
        LinearLayout action = ui.card(content);
        row(action, new Field(R.string.te_action, t -> t.actionKey, (t, c) -> t.actionKey = c, false, true));
        row(action, new Field(R.string.te_action_text, t -> t.actionKeyText, (t, c) -> t.actionKeyText = c, false, true));

        ui.group(content, R.string.te_sec_accent);
        LinearLayout accent = ui.card(content);
        row(accent, new Field(R.string.te_accent, t -> t.accent, (t, c) -> t.accent = c, false, false));
        row(accent, new Field(R.string.te_popup, t -> t.popup, (t, c) -> t.popup = c, false, false));
        row(accent, new Field(R.string.te_popup_text, t -> t.popupText, (t, c) -> t.popupText = c, false, false));

        ui.button(content, R.string.te_save, true, this::save);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmLeave(); }
        });

        refreshAll();
        loadBackdrop();
        return column;
    }

    private void row(LinearLayout parent, Field f) {
        if (!fields.isEmpty() && parent.getChildCount() > 0) ui.divider(parent);
        fields.add(f);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ui.dp(20), ui.dp(12), ui.dp(20), ui.dp(12));
        TextView label = ui.titleText(getString(f.labelRes),
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        f.value = ui.titleText("", com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        f.value.setTextDirection(View.TEXT_DIRECTION_LTR);
        f.value.setTextColor(ui.onSurfaceVariant());
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.setMarginEnd(ui.dp(12));
        row.addView(f.value, vlp);
        f.swatch = new View(this);
        row.addView(f.swatch, new LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)));
        ui.applyRipple(row);
        row.setOnClickListener(v -> pick(f));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void pick(Field f) {
        int current = f.getter.get(theme);
        int shown = current != 0 ? current : autoColor(f);
        ColorPickerDialog.show(this, getString(R.string.theme_color_pick, getString(f.labelRes)),
                shown, f.alpha, f.optional, color -> {
                    f.setter.set(theme, color);
                    dirty = true;
                    refreshAll();
                });
    }

    private int autoColor(Field f) {
        KeyboardTheme probe = theme.copy();
        f.setter.set(probe, 0);
        if (f.labelRes == R.string.te_toolbar_icon) return probe.toolbarIconColor();
        if (f.labelRes == R.string.te_suggestion_text) return probe.suggestionTextColor();
        if (f.labelRes == R.string.te_special_text) return probe.specialTextColor();
        if (f.labelRes == R.string.te_action) return probe.actionKeyColor();
        if (f.labelRes == R.string.te_action_text) return probe.actionKeyTextColor();
        return 0xFF808080;
    }

    private void refreshAll() {
        theme.dark = luminance(theme.background) < 0.55;
        for (Field f : fields) {
            int c = f.getter.get(theme);
            boolean auto = c == 0 && f.optional;
            int shown = auto ? autoColor(f) : c;
            GradientDrawable under = new GradientDrawable();
            under.setShape(GradientDrawable.OVAL);
            under.setColor(0xFFB0B0B0);
            GradientDrawable top = new GradientDrawable();
            top.setShape(GradientDrawable.OVAL);
            top.setColor(shown);
            top.setStroke(ui.dp(1), ui.color(com.google.android.material.R.attr.colorOutline, 0xFF74777F));
            f.swatch.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{under, top}));
            f.value.setText(auto ? getString(R.string.te_auto)
                    : (f.alpha && Color.alpha(c) != 255
                    ? String.format(java.util.Locale.ROOT, "#%08X", c)
                    : String.format(java.util.Locale.ROOT, "#%06X", c & 0xFFFFFF)));
        }
        Prefs p = Prefs.get(this);
        preview.bind(theme, previewBitmap, p.keyAlpha(), p.hideKeyBg(), p.bgDim());
    }

    private static double luminance(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
    }

    private void loadBackdrop() {
        if (!Prefs.get(this).hasBg()) return;
        Bg.io(() -> {
            Bitmap bmp = BackgroundImageManager.loadDisplay(getApplicationContext());
            Bg.main(() -> {
                if (isDestroyed()) {
                    if (bmp != null) bmp.recycle();
                    return;
                }
                previewBitmap = bmp;
                refreshAll();
            });
        });
    }

    private void chooseBase() {
        String[] ids = ThemeManager.IDS;
        CharSequence[] labels = new CharSequence[ids.length];
        for (int i = 0; i < ids.length; i++) labels[i] = getString(ThemeManager.labelRes(ids[i]));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.te_start_from)
                .setItems(labels, (d, which) -> {
                    theme = ThemeManager.resolveId(this, ids[which]).copy();
                    dirty = true;
                    refreshAll();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void save() {
        String themeName = nameField.getText().toString().trim();
        if (themeName.isEmpty()) {
            nameField.setError(getString(R.string.theme_name_required));
            nameField.requestFocus();
            return;
        }
        theme.dark = luminance(theme.background) < 0.55;
        ThemeManager.saveCustomTheme(this, themeName, theme);
        if (!ThemeManager.hasCustomTheme(this)) {
            Toast.makeText(this, R.string.theme_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        dirty = false;
        setResult(RESULT_OK);
        finish();
    }

    private void confirmLeave() {
        if (!dirty) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.te_discard_title)
                .setMessage(R.string.te_discard_msg)
                .setNegativeButton(R.string.te_keep, null)
                .setPositiveButton(R.string.te_discard, (d, w) -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        if (preview != null) preview.bind(null, null, 100, false, 0);
        previewBitmap = null;
        super.onDestroy();
    }
}
