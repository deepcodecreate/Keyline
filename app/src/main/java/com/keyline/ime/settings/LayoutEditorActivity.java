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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;
import com.keyline.ime.layout.KeySpec;
import com.keyline.ime.layout.LayoutModel;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.layout.LayoutSerializer;
import com.keyline.ime.layout.LayoutStore;
import com.keyline.ime.layout.LayoutValidator;
import com.keyline.ime.theme.KeyboardTheme;
import com.keyline.ime.theme.ThemeManager;
import com.keyline.ime.view.KeyboardView;
import com.keyline.ime.view.ToolbarView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;

public class LayoutEditorActivity extends SettingsPageActivity {
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_COPY = "copy";

    private static final int TAB_LETTERS = 0;
    private static final int TAB_SHIFT = 1;
    private static final int TAB_NUMBERS = 2;
    private static final int TAB_SYMBOLS = 3;
    private static final int TAB_SYMBOLS2 = 4;

    private LayoutModel work;
    private LayoutModel sharedNumbers;
    private LayoutModel sharedSymbols;
    private LayoutModel sharedSymbols2;
    private KeyboardView preview;
    private LinearLayout controls;
    private int tab = TAB_LETTERS;
    private int selRow = -1;
    private int selCol = -1;
    private boolean dirty;

    @Override protected int titleRes() { return R.string.editor_title; }

    @Override
    protected View createBody() {
        loadWorking();
        sharedNumbers = LayoutRepository.functional(this, "numbers");
        sharedSymbols = LayoutRepository.functional(this, "symbols");
        sharedSymbols2 = LayoutRepository.functional(this, "symbols2");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(buildPreview(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(300)));

        NestedScrollView scroll = new NestedScrollView(this);
        scroll.setFillViewport(true);
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, ui.dp(4), 0, ui.dp(16));
        scroll.addView(controls, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        box.addView(buildActionBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!dirty) {
                    finish();
                    return;
                }
                new MaterialAlertDialogBuilder(LayoutEditorActivity.this)
                        .setTitle(R.string.editor_discard_title)
                        .setMessage(R.string.editor_discard_msg)
                        .setNegativeButton(R.string.editor_keep, null)
                        .setPositiveButton(R.string.editor_discard, (d, w) -> finish())
                        .show();
            }
        });

        renderControls();
        return box;
    }

    private void loadWorking() {
        String source = getIntent().getStringExtra(EXTRA_SOURCE);
        boolean copy = getIntent().getBooleanExtra(EXTRA_COPY, false);
        boolean blank = source == null || source.isEmpty();
        LayoutModel base = null;
        if (!blank) base = LayoutRepository.load(this, source);
        if (base == null) {
            blank = true;
            LayoutValidator.Result r = LayoutValidator.parse(
                    LayoutRepository.readAsset(this, "layouts/template.json"));
            base = r.layout;
        }
        work = base.copy();
        work.bundled = false;
        work.kind = "letters";
        boolean bundled = LayoutRepository.isBundled(work.id);
        if (blank || copy || bundled) {
            String seed = blank ? work.id : work.id + "-custom";
            work.id = LayoutStore.freeId(this, seed);
            if (!blank) work.name = work.name + " (custom)";
        }
        if (work.digits == null) work.digits = "latin";
    }

    private View buildPreview() {
        KeyboardTheme theme = ThemeManager.resolve(this);
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(theme.background);

        ToolbarView toolbar = new ToolbarView(this, null);
        toolbar.apply(theme, false, false, false, false);
        frame.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(44)));

        preview = new KeyboardView(this, null);
        Prefs prefs = Prefs.get(this);
        preview.setTheme(theme);
        preview.setKeyAlpha(prefs.keyAlpha());
        preview.setHideKeyBackground(prefs.hideKeyBg());
        preview.setFont(com.keyline.ime.theme.FontManager.typeface(this, prefs.font()));
        preview.setFontScale(prefs.fontSize() / 100f);
        preview.setEditListener((row, col, spec) -> {
            selRow = row;
            selCol = col;
            renderControls();
        });
        frame.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        refreshPreview();
        return frame;
    }

    private View buildActionBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(12));
        bar.setBackgroundColor(ui.color(
                com.google.android.material.R.attr.colorSurfaceContainer, 0xFFF1F3F6));

        MaterialButton export = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        export.setText(R.string.editor_export);
        export.setIconResource(R.drawable.ic_send);
        export.setOnClickListener(v -> exportJson());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ep.setMarginEnd(ui.dp(8));
        bar.addView(export, ep);

        MaterialButton add = new MaterialButton(this);
        add.setText(R.string.editor_add);
        add.setIconResource(R.drawable.ic_keyboard);
        add.setOnClickListener(v -> saveAndAdd());
        bar.addView(add, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        return bar;
    }

    private List<List<KeySpec>> rowsOf(int t) {
        switch (t) {
            case TAB_SHIFT: return work.shifted;
            case TAB_NUMBERS: return work.numbers;
            case TAB_SYMBOLS: return work.symbols;
            case TAB_SYMBOLS2: return work.symbols2;
            default: return work.main;
        }
    }

    private void setRows(int t, List<List<KeySpec>> rows) {
        switch (t) {
            case TAB_SHIFT: work.shifted = rows; break;
            case TAB_NUMBERS: work.numbers = rows; break;
            case TAB_SYMBOLS: work.symbols = rows; break;
            case TAB_SYMBOLS2: work.symbols2 = rows; break;
            default: work.main = rows; break;
        }
    }

    private List<KeySpec> bottomOf(int t) {
        return t <= TAB_SHIFT ? work.bottom : work.bottomNumbers;
    }

    private List<KeySpec> rowAt(int r) {
        List<List<KeySpec>> rows = rowsOf(tab);
        if (rows == null || r < 0) return null;
        if (r < rows.size()) return rows.get(r);
        return r == rows.size() ? bottomOf(tab) : null;
    }

    private boolean isBottomRow(int r) {
        List<List<KeySpec>> rows = rowsOf(tab);
        return rows != null && r == rows.size();
    }

    private KeySpec selected() {
        List<KeySpec> row = rowAt(selRow);
        if (row == null || selCol < 0 || selCol >= row.size()) return null;
        return row.get(selCol);
    }

    private void refreshPreview() {
        if (preview == null) return;
        preview.setLayouts(work, sharedNumbers, sharedSymbols, sharedSymbols2);
        preview.setPage(tab >= TAB_NUMBERS
                ? (tab == TAB_NUMBERS ? KeyboardView.PAGE_NUM
                : tab == TAB_SYMBOLS ? KeyboardView.PAGE_SYM : KeyboardView.PAGE_SYM2)
                : KeyboardView.PAGE_LETTERS);
        preview.setShift(tab == TAB_SHIFT ? 1 : 0, false);
        preview.setSelectedKey(selRow, selCol);
    }

    private void changed() {
        dirty = true;
        refreshPreview();
    }

    private void renderControls() {
        controls.removeAllViews();
        refreshPreview();

        ui.group(controls, R.string.editor_pages);
        chipRow(controls, new String[]{
                getString(R.string.editor_tab_letters), getString(R.string.editor_tab_shift),
                getString(R.string.editor_tab_numbers), getString(R.string.editor_tab_symbols),
                getString(R.string.editor_tab_symbols2)}, tab, index -> {
            tab = index;
            selRow = -1;
            selCol = -1;
            renderControls();
        });

        renderPageState();
        renderKeyInspector();
        renderBasics();
        renderPageLabels();

        ui.group(controls, R.string.editor_advanced);
        LinearLayout tools = ui.card(controls);
        ui.navRow(tools, R.drawable.ic_code, getString(R.string.editor_show_json),
                getString(R.string.editor_show_json_sub), false, this::showJson);
    }

    private void renderPageState() {
        boolean own = rowsOf(tab) != null;
        if (tab == TAB_LETTERS) return;
        LinearLayout card = ui.card(controls);
        if (!own) {
            String note = tab == TAB_SHIFT
                    ? getString(R.string.editor_shift_auto)
                    : getString(R.string.editor_page_shared);
            ui.body(card, note, true);
            ui.navRow(card, R.drawable.ic_edit, getString(R.string.editor_customize), null, false,
                    this::customizeCurrentPage);
        } else {
            ui.navRow(card, R.drawable.ic_delete, getString(
                    tab == TAB_SHIFT ? R.string.editor_remove_shift : R.string.editor_use_shared),
                    null, false, () -> {
                        setRows(tab, null);
                        selRow = -1;
                        selCol = -1;
                        changed();
                        renderControls();
                    });
        }
    }

    private void customizeCurrentPage() {
        if (tab == TAB_SHIFT) {
            List<List<KeySpec>> rows = LayoutModel.copyRows(work.main);
            Locale locale = work.javaLocale();
            for (List<KeySpec> row : rows) {
                for (KeySpec k : row) {
                    if (!k.isAction() && k.code == null && k.label != null) {
                        k.label = k.label.toUpperCase(locale);
                    }
                }
            }
            work.shifted = rows;
        } else {
            LayoutModel shared = tab == TAB_NUMBERS ? sharedNumbers
                    : tab == TAB_SYMBOLS ? sharedSymbols : sharedSymbols2;
            if (shared == null) return;
            List<List<KeySpec>> rows = LayoutModel.copyRows(shared.main);
            if (tab == TAB_NUMBERS) localizeDigits(rows);
            setRows(tab, rows);
            if (work.bottomNumbers == null && shared.bottom != null) {
                work.bottomNumbers = LayoutModel.copyRow(shared.bottom);
                for (KeySpec k : work.bottomNumbers) {
                    if (!k.isAction()) continue;
                    if ("to_letters".equals(k.action) && work.labelLetters != null) k.label = work.labelLetters;
                    if ("to_numbers".equals(k.action) && work.labelNumbers != null) k.label = work.labelNumbers;
                    if ("to_symbols".equals(k.action) && work.labelSymbols != null) k.label = work.labelSymbols;
                    if ("to_symbols2".equals(k.action) && work.labelSymbols2 != null) k.label = work.labelSymbols2;
                }
            }
        }
        selRow = -1;
        selCol = -1;
        changed();
        renderControls();
    }

    private void localizeDigits(List<List<KeySpec>> rows) {
        String chars = LayoutValidator.digitChars(work.digits);
        int[] cps = chars.codePoints().toArray();
        if (cps.length != 10) return;
        for (List<KeySpec> row : rows) {
            for (KeySpec k : row) {
                if (k.isAction() || k.label == null || k.label.length() != 1) continue;
                char c = k.label.charAt(0);
                if (c < '0' || c > '9') continue;
                String local = new String(Character.toChars(cps[c - '0']));
                if (!local.equals(k.label)) {
                    k.popup = new String[]{k.label};
                    k.label = local;
                }
            }
        }
    }

    private void renderKeyInspector() {
        ui.group(controls, R.string.editor_key);
        LinearLayout card = ui.card(controls);
        KeySpec key = selected();
        if (key == null) {
            boolean shared = rowsOf(tab) == null;
            ui.body(card, getString(shared ? R.string.editor_hint_shared : R.string.editor_hint_tap), true);
            if (!shared) {
                ui.divider(card);
                ui.navRow(card, R.drawable.ic_add, getString(R.string.editor_row_add), null, false,
                        () -> addRowAfter(rowsOf(tab).size() - 1));
            }
            return;
        }

        LinearLayout form = padded(card);
        field(form, R.string.editor_f_label, key.label, text -> {
            key.label = text;
            changed();
        });
        field(form, R.string.editor_f_code, key.code == null ? "" : key.code, text -> {
            key.code = text.isEmpty() ? null : text;
            changed();
        });
        field(form, R.string.editor_f_popup, join(key.popup), text -> {
            key.popup = split(text);
            changed();
        });
        field(form, R.string.editor_f_hint, key.hint == null ? "" : key.hint, text -> {
            if (text.isEmpty()) key.hint = null;
            else if (text.trim().isEmpty()) key.hint = "";
            else key.hint = text.trim();
            changed();
        });

        ui.divider(card);
        ui.navRow(card, R.drawable.ic_keyboard, getString(R.string.editor_f_action),
                key.isAction() ? key.action : getString(R.string.editor_action_none), false,
                () -> pickAction(key));

        LinearLayout sizing = padded(card);
        TextView widthLabel = ui.titleText(getString(R.string.editor_f_width),
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        sizing.addView(widthLabel);
        Slider width = new Slider(this);
        width.setValueFrom(5f);
        width.setValueTo(50f);
        width.setStepSize(1f);
        width.setLabelFormatter(v -> String.format(Locale.US, "%.1f", v / 10f));
        width.setValue(Math.max(5, Math.min(50, Math.round(key.width * 10f))));
        width.addOnChangeListener((s, value, fromUser) -> {
            if (!fromUser) return;
            key.width = value / 10f;
            changed();
        });
        sizing.addView(width, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView styleLabel = ui.titleText(getString(R.string.editor_f_style),
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        sizing.addView(styleLabel);
        String[] styles = {null, "normal", "special", "accent"};
        int current = 0;
        for (int i = 0; i < styles.length; i++) {
            if ((styles[i] == null && key.style == null)
                    || (styles[i] != null && styles[i].equals(key.style))) current = i;
        }
        chipRow(sizing, new String[]{
                getString(R.string.editor_style_auto), getString(R.string.editor_style_normal),
                getString(R.string.editor_style_special), getString(R.string.editor_style_accent)},
                current, index -> {
                    key.style = styles[index];
                    changed();
                    renderControls();
                });

        ui.divider(card);
        ui.navRow(card, R.drawable.ic_add, getString(R.string.editor_key_add), null, false,
                this::addKeyAfter);
        ui.divider(card);
        ui.navRow(card, R.drawable.ic_arrow_left, getString(R.string.editor_key_left), null, false,
                () -> moveKey(-1));
        ui.divider(card);
        ui.navRow(card, R.drawable.ic_arrow_right, getString(R.string.editor_key_right), null, false,
                () -> moveKey(1));
        ui.divider(card);
        ui.navRow(card, R.drawable.ic_delete, getString(R.string.editor_key_delete), null, false,
                this::deleteKey);

        if (isBottomRow(selRow)) {
            ui.divider(card);
            ui.body(card, getString(R.string.editor_bottom_note), true);
        } else {
            ui.divider(card);
            ui.navRow(card, R.drawable.ic_add, getString(R.string.editor_row_add), null, false,
                    () -> addRowAfter(selRow));
            ui.divider(card);
            ui.navRow(card, R.drawable.ic_up, getString(R.string.editor_row_up), null, false,
                    () -> moveRow(-1));
            ui.divider(card);
            ui.navRow(card, R.drawable.ic_down, getString(R.string.editor_row_down), null, false,
                    () -> moveRow(1));
            ui.divider(card);
            ui.navRow(card, R.drawable.ic_delete, getString(R.string.editor_row_delete), null, false,
                    this::deleteRow);
        }
    }

    private void renderBasics() {
        ui.group(controls, R.string.editor_basics);
        LinearLayout card = ui.card(controls);
        LinearLayout form = padded(card);
        field(form, R.string.editor_f_name, work.name, text -> {
            work.name = text;
            dirty = true;
        });
        field(form, R.string.editor_f_id, work.id, text -> {
            work.id = text.trim();
            dirty = true;
        });
        field(form, R.string.editor_f_locale, work.locale, text -> {
            work.locale = text.trim();
            changed();
        });
        field(form, R.string.editor_f_space, work.spaceLabel, text -> {
            work.spaceLabel = text;
            changed();
        });

        LinearLayout choices = padded(card);
        choices.addView(ui.titleText(getString(R.string.editor_direction),
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge));
        chipRow(choices, new String[]{getString(R.string.editor_ltr), getString(R.string.editor_rtl)},
                work.rtl() ? 1 : 0, index -> {
                    work.direction = index == 1 ? "rtl" : "ltr";
                    changed();
                    renderControls();
                });
        choices.addView(ui.titleText(getString(R.string.editor_digits),
                com.google.android.material.R.style.TextAppearance_Material3_LabelLarge));
        String[] digitValues = {"latin", "persian", "arabic"};
        int digitIndex = 0;
        for (int i = 0; i < digitValues.length; i++) if (digitValues[i].equals(work.digits)) digitIndex = i;
        chipRow(choices, new String[]{getString(R.string.editor_digits_latin),
                getString(R.string.editor_digits_persian), getString(R.string.editor_digits_arabic)},
                digitIndex, index -> {
                    work.digits = digitValues[index];
                    dirty = true;
                    renderControls();
                });
        ui.divider(card);
        ui.switchRow(card, R.string.editor_has_shift, R.string.editor_has_shift_sub, work.hasShift,
                value -> {
                    work.hasShift = value;
                    changed();
                });
    }

    private void renderPageLabels() {
        ui.group(controls, R.string.editor_labels);
        LinearLayout card = ui.card(controls);
        ui.body(card, getString(R.string.editor_labels_sub), true);
        LinearLayout form = padded(card);
        field(form, R.string.editor_f_lbl_letters, nz(work.labelLetters), text -> {
            work.labelLetters = text.isEmpty() ? null : text;
            changed();
        });
        field(form, R.string.editor_f_lbl_numbers, nz(work.labelNumbers), text -> {
            work.labelNumbers = text.isEmpty() ? null : text;
            changed();
        });
        field(form, R.string.editor_f_lbl_symbols, nz(work.labelSymbols), text -> {
            work.labelSymbols = text.isEmpty() ? null : text;
            work.labelSymbols2 = work.labelSymbols;
            changed();
        });
    }

    private void addKeyAfter() {
        List<KeySpec> row = rowAt(selRow);
        if (row == null) return;
        if (row.size() >= 16) {
            toast(getString(R.string.editor_limit));
            return;
        }
        KeySpec k = new KeySpec();
        k.label = "•";
        int at = Math.min(selCol + 1, row.size());
        row.add(at, k);
        selCol = at;
        changed();
        renderControls();
    }

    private void moveKey(int delta) {
        List<KeySpec> row = rowAt(selRow);
        int to = selCol + delta;
        if (row == null || to < 0 || to >= row.size()) return;
        KeySpec k = row.remove(selCol);
        row.add(to, k);
        selCol = to;
        changed();
        renderControls();
    }

    private void deleteKey() {
        List<KeySpec> row = rowAt(selRow);
        if (row == null || selCol < 0 || selCol >= row.size()) return;
        if (row.size() == 1) {
            if (isBottomRow(selRow) || rowsOf(tab).size() <= 1) {
                toast(getString(R.string.editor_limit));
                return;
            }
            rowsOf(tab).remove(selRow);
            selRow = Math.min(selRow, rowsOf(tab).size() - 1);
            selCol = 0;
        } else {
            row.remove(selCol);
            selCol = Math.min(selCol, row.size() - 1);
        }
        changed();
        renderControls();
    }

    private void addRowAfter(int index) {
        List<List<KeySpec>> rows = rowsOf(tab);
        if (rows == null) return;
        if (rows.size() >= 8) {
            toast(getString(R.string.editor_limit));
            return;
        }
        KeySpec k = new KeySpec();
        k.label = "•";
        List<KeySpec> row = new ArrayList<>();
        row.add(k);
        int at = Math.max(0, Math.min(index + 1, rows.size()));
        rows.add(at, row);
        selRow = at;
        selCol = 0;
        changed();
        renderControls();
    }

    private void moveRow(int delta) {
        List<List<KeySpec>> rows = rowsOf(tab);
        int to = selRow + delta;
        if (rows == null || selRow < 0 || selRow >= rows.size() || to < 0 || to >= rows.size()) return;
        List<KeySpec> row = rows.remove(selRow);
        rows.add(to, row);
        selRow = to;
        changed();
        renderControls();
    }

    private void deleteRow() {
        List<List<KeySpec>> rows = rowsOf(tab);
        if (rows == null || selRow < 0 || selRow >= rows.size()) return;
        if (rows.size() <= 1) {
            toast(getString(R.string.editor_limit));
            return;
        }
        rows.remove(selRow);
        selRow = Math.min(selRow, rows.size() - 1);
        selCol = 0;
        changed();
        renderControls();
    }

    private void pickAction(KeySpec key) {
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.editor_action_none));
        names.addAll(new TreeSet<>(LayoutValidator.ACTIONS));
        int checked = 0;
        for (int i = 1; i < names.size(); i++) if (names.get(i).equals(key.action)) checked = i;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_f_action)
                .setSingleChoiceItems(names.toArray(new CharSequence[0]), checked, (dialog, which) -> {
                    dialog.dismiss();
                    key.action = which == 0 ? null : names.get(which);
                    changed();
                    renderControls();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String problem() {
        if (work.name == null || work.name.trim().isEmpty()) return getString(R.string.editor_need_name);
        LayoutValidator.Result r = LayoutValidator.parse(LayoutSerializer.toJson(work));
        if (!r.ok) return r.error;
        if (LayoutRepository.isBundled(work.id)) return getString(R.string.editor_id_reserved, work.id);
        return null;
    }

    private void saveAndAdd() {
        String err = problem();
        if (err == null) err = LayoutStore.save(this, work, true);
        if (err != null) {
            showProblem(err);
            return;
        }
        dirty = false;
    }

    private void exportJson() {
        String err = problem();
        if (err != null) {
            showProblem(err);
            return;
        }
        try {
            File file = LayoutStore.writeShareFile(this, work);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.editor_export)));
        } catch (Exception e) {
            toast(getString(R.string.import_fail));
        }
    }

    private void showProblem(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_invalid)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showJson() {
        String json = LayoutSerializer.toJson(work);
        TextView view = new TextView(this);
        view.setText(json);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(12f);
        view.setTextIsSelectable(true);
        view.setTextDirection(View.TEXT_DIRECTION_LTR);
        view.setPadding(ui.dp(24), ui.dp(8), ui.dp(24), ui.dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(view);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_show_json)
                .setView(scroll)
                .setNegativeButton(R.string.editor_close, null)
                .setPositiveButton(R.string.editor_copy, (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("layout", json));
                })
                .show();
    }

    private LinearLayout padded(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        parent.addView(box, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private void field(LinearLayout parent, int hintRes, String value, Consumer<String> onChange) {
        TextInputLayout layout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(getString(hintRes));
        TextInputEditText edit = new TextInputEditText(layout.getContext());
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edit.setText(value == null ? "" : value);
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) { onChange.accept(e.toString()); }
        });
        layout.addView(edit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = ui.dp(8);
        parent.addView(layout, lp);
    }

    private void chipRow(LinearLayout parent, String[] labels, int selected,
                         java.util.function.IntConsumer onPick) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ui.dp(parent == controls ? 16 : 0), ui.dp(4), ui.dp(16), ui.dp(4));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Chip chip = new Chip(this);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(i == selected);
            chip.setOnClickListener(v -> onPick.accept(index));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(ui.dp(8));
            row.addView(chip, lp);
        }
        scroll.addView(row);
        parent.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String join(String[] items) {
        if (items == null || items.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(items[i]);
        }
        return sb.toString();
    }

    private static String[] split(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return new String[0];
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 16) {
            String[] cut = new String[16];
            System.arraycopy(parts, 0, cut, 0, 16);
            return cut;
        }
        return parts;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
