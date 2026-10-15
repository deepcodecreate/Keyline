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

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;
import com.keyline.ime.ime.KeyboardService;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends SettingsPageActivity {
    private TextView enabledStatus;
    private TextView selectedStatus;
    private TextView setupTitle;
    private LinearLayout stepsBox;

    @Override protected int titleRes() { return R.string.app_name; }
    @Override protected boolean showBack() { return false; }

    @Override
    protected void build(LinearLayout content) {
        buildSetupCard(content);

        List<SettingsUi.Tile> tiles = new ArrayList<>();
        tiles.add(new SettingsUi.Tile(R.drawable.ic_palette, getString(R.string.cat_theme),
                () -> open(ThemeSettingsActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_keyboard, getString(R.string.cat_typing),
                () -> open(TypingSettingsActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_clipboard, getString(R.string.cat_clipboard),
                () -> open(ClipboardSettingsActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_password, getString(R.string.vault_title),
                () -> open(CredentialVaultActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_translate, getString(R.string.translate_title),
                () -> open(TranslatorActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_globe, getString(R.string.manage_languages),
                () -> open(LanguageActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_book, getString(R.string.manage_dict),
                () -> open(DictionaryActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_backup, getString(R.string.cat_backup),
                () -> open(BackupSettingsActivity.class)));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_globe, getString(R.string.interface_language),
                this::chooseInterfaceLanguage));
        tiles.add(new SettingsUi.Tile(R.drawable.ic_info, getString(R.string.about_title),
                this::showAbout));
        ui.tileGrid(content, tiles);
    }

    private void showLastCrash() {
        java.io.File f = com.keyline.ime.KeylineApp.crashFile(this);
        if (!f.exists()) return;
        String text;
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) Math.min(f.length(), 12000)];
            int n = in.read(buf);
            in.close();
            text = new String(buf, 0, Math.max(0, n), "UTF-8");
        } catch (Throwable t) {
            f.delete();
            return;
        }
        f.delete();
        final String report = text;
        TextView tv = new TextView(this);
        tv.setText(report);
        tv.setTextSize(11f);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        tv.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(8));
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(tv);
        new MaterialAlertDialogBuilder(this)
                .setTitle("\u06AF\u0632\u0627\u0631\u0634 \u06A9\u0631\u0634")
                .setView(sv)
                .setPositiveButton("\u06A9\u067E\u06CC", (d, w) -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", report));
                })
                .setNegativeButton("\u0628\u0633\u062A\u0646", null)
                .show();
    }

    private void buildSetupCard(LinearLayout content) {
        int onContainer = ui.color(com.google.android.material.R.attr.colorOnPrimaryContainer,
                0xFF041E49);
        LinearLayout card = ui.tonalCard(content);
        card.setPadding(ui.dp(20), ui.dp(14), ui.dp(20), ui.dp(12));

        setupTitle = ui.titleText(getString(R.string.setup_title),
                com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        setupTitle.setTextColor(onContainer);
        card.addView(setupTitle);

        stepsBox = new LinearLayout(this);
        stepsBox.setOrientation(LinearLayout.VERTICAL);
        card.addView(stepsBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        enabledStatus = addStep(stepsBox, R.string.step_enable, this::openImeSettings);
        selectedStatus = addStep(stepsBox, R.string.step_select, this::showImePicker);

        TextInputLayout field = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        field.setHint(getString(R.string.test_hint));
        TextInputEditText test = new TextInputEditText(field.getContext());
        test.setSingleLine(false);
        test.setMinLines(1);
        test.setMaxLines(3);
        test.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.addView(test, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = ui.dp(8);
        card.addView(field, fieldParams);
    }

    private TextView addStep(LinearLayout parent, int titleRes, Runnable action) {
        int onContainer = ui.color(com.google.android.material.R.attr.colorOnPrimaryContainer,
                0xFF041E49);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ui.dp(44));
        TextView name = ui.titleText(getString(titleRes),
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        name.setTextColor(onContainer);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pill = ui.pill();
        ui.stylePill(pill, false);
        row.addView(pill);
        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_chevron);
        arrow.setImageTintList(ColorStateList.valueOf(onContainer));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(ui.dp(20), ui.dp(20));
        arrowParams.setMarginStart(ui.dp(8));
        row.addView(arrow, arrowParams);
        ui.applyRipple(row);
        row.setOnClickListener(v -> action.run());
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return pill;
    }

    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLastCrash();
        refreshSetupStatus();
    }

    private void refreshSetupStatus() {
        if (enabledStatus == null || selectedStatus == null) return;
        boolean enabled = false;
        boolean selected = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            List<InputMethodInfo> methods = imm.getEnabledInputMethodList();
            if (methods != null) {
                for (InputMethodInfo info : methods) {
                    if (getPackageName().equals(info.getPackageName())) {
                        enabled = true;
                        break;
                    }
                }
            }
        }
        String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName component = current == null ? null : ComponentName.unflattenFromString(current);
        if (component != null) {
            selected = getPackageName().equals(component.getPackageName())
                    && KeyboardService.class.getName().equals(component.getClassName());
        }
        ui.stylePill(enabledStatus, enabled);
        ui.stylePill(selectedStatus, selected);
        boolean ready = enabled && selected;
        stepsBox.setVisibility(ready ? android.view.View.GONE : android.view.View.VISIBLE);
        setupTitle.setText(ready ? R.string.setup_ready : R.string.setup_title);
    }

    private void openImeSettings() {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Settings.ACTION_SETTINGS);
        }
        startActivity(intent);
    }

    private void showImePicker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
        else Toast.makeText(this, R.string.status_needed, Toast.LENGTH_SHORT).show();
    }

    private void chooseInterfaceLanguage() {
        String current = Prefs.get(this).settingsLanguage();
        CharSequence[] labels = {getString(R.string.language_english), getString(R.string.language_persian)};
        int checked = "fa".equals(current) ? 1 : 0;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.interface_language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    Prefs.get(this).settingsLanguage(which == 1 ? "fa" : "en");
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.about_sub)
                .setPositiveButton(R.string.done, null)
                .show();
    }
}
