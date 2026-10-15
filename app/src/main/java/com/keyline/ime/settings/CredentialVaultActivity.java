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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;
import com.keyline.ime.security.CredentialRecord;
import com.keyline.ime.security.CredentialVault;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CredentialVaultActivity extends SettingsPageActivity {
    public static final String EXTRA_CURRENT_PACKAGE = "current_app_package";

    private final List<CredentialRecord> records = new ArrayList<>();
    private LinearLayout entryList;
    private boolean vaultAccessible;
    private String currentPackage = "";
    private java.util.Set<String> browserPackages = java.util.Collections.emptySet();

    @Override protected int titleRes() { return R.string.vault_title; }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void build(LinearLayout content) {
        currentPackage = getIntent().getStringExtra(EXTRA_CURRENT_PACKAGE);
        if (currentPackage == null) currentPackage = "";
        browserPackages = CredentialVault.browserPackages(this);
        Prefs prefs = Prefs.get(this);

        ui.group(content, R.string.vault_security_group);
        LinearLayout privacy = ui.card(content);
        ui.body(privacy, getString(R.string.vault_security_note), true);
        ui.switchRow(privacy, R.string.vault_email_toggle, R.string.vault_email_toggle_sub,
                prefs.emailSuggestions(), prefs::emailSuggestions);
        ui.divider(privacy);
        ui.switchRow(privacy, R.string.vault_login_toggle, R.string.vault_login_toggle_sub,
                prefs.loginSuggestions(), prefs::loginSuggestions);

        ui.group(content, R.string.vault_accounts_group);
        LinearLayout accounts = ui.card(content);
        readVault();
        if (vaultAccessible) {
            ui.iconButton(accounts, R.string.vault_add_email, R.drawable.ic_add, true,
                    this::showQuickEmail);
            ui.iconButton(accounts, R.string.vault_add, R.drawable.ic_add, false,
                    () -> showEditor(null));
        } else {
            ui.body(accounts, getString(R.string.vault_locked), true);
        }
        ui.divider(accounts);
        entryList = new LinearLayout(this);
        entryList.setOrientation(LinearLayout.VERTICAL);
        accounts.addView(entryList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        renderEntries();
    }

    private void readVault() {
        try {
            records.clear();
            records.addAll(CredentialVault.load(getApplicationContext()));
            vaultAccessible = true;
        } catch (Exception ignored) {
            records.clear();
            vaultAccessible = false;
        }
    }

    private void renderEntries() {
        if (entryList == null) return;
        entryList.removeAllViews();
        if (!vaultAccessible) return;
        if (records.isEmpty()) {
            ui.emptyState(entryList, R.drawable.ic_password, getString(R.string.vault_empty),
                    getString(R.string.vault_empty_sub));
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            CredentialRecord record = records.get(i);
            String app = record.appLabel == null || record.appLabel.isEmpty()
                    ? (record.appPackage == null || record.appPackage.isEmpty()
                    ? getString(R.string.vault_no_app) : record.appPackage)
                    : record.appLabel;
            String badge = record.title == null || record.title.isEmpty()
                    ? "@" : record.title.substring(0, 1);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            ImageView edit = ui.iconAction(R.drawable.ic_edit, getString(R.string.vault_edit),
                    () -> showEditor(record));
            ImageView delete = ui.iconAction(R.drawable.ic_delete, getString(R.string.vault_delete),
                    () -> confirmDelete(record));
            int actionSize = ui.dp(40);
            actions.addView(edit, new LinearLayout.LayoutParams(actionSize, actionSize));
            actions.addView(delete, new LinearLayout.LayoutParams(actionSize, actionSize));

            ui.listItem(entryList, badge, record.title,
                    app + "  ·  " + record.username, actions, () -> showEditor(record));
            if (i + 1 < records.size()) ui.divider(entryList);
        }
    }

    private void showEditor(CredentialRecord existing) {
        if (!vaultAccessible) return;
        List<AppChoice> apps = launcherApps();
        AppChoice[] selected = {initialApp(existing, apps)};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = ui.dp(16);
        form.setPadding(pad, ui.dp(4), pad, ui.dp(4));

        TextInputLayout titleBox = inputLayout(R.string.vault_entry_title_hint);
        TextInputEditText title = textField(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 128);
        titleBox.addView(title);
        form.addView(titleBox, matchParams());

        TextInputLayout usernameBox = inputLayout(R.string.vault_username_hint);
        TextInputEditText username = textField(InputType.TYPE_CLASS_TEXT, 256);
        usernameBox.addView(username);
        LinearLayout.LayoutParams usernameParams = matchParams();
        usernameParams.topMargin = ui.dp(8);
        form.addView(usernameBox, usernameParams);

        TextInputLayout passwordBox = inputLayout(R.string.vault_password_hint);
        passwordBox.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText password = textField(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD, 2048);
        passwordBox.addView(password);
        LinearLayout.LayoutParams passwordParams = matchParams();
        passwordParams.topMargin = ui.dp(8);
        form.addView(passwordBox, passwordParams);

        MaterialButton appButton = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        appButton.setAllCaps(false);
        appButton.setText(getString(R.string.vault_application) + ": " + appLabel(selected[0]));
        appButton.setOnClickListener(v -> chooseApp(apps, selected, appButton, title));
        LinearLayout.LayoutParams appParams = matchParams();
        appParams.topMargin = ui.dp(12);
        form.addView(appButton, appParams);

        if (existing != null) {
            title.setText(existing.title);
            username.setText(existing.username);
            password.setText(existing.password);
        } else {
            title.setText(defaultTitle(selected[0]));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.vault_add : R.string.vault_edit)
                .setView(scroll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String label = value(title);
                    String user = value(username);
                    titleBox.setError(null);
                    if (user.isEmpty()) {
                        usernameBox.setError(getString(R.string.vault_username_missing));
                        return;
                    }
                    usernameBox.setError(null);
                    if (label.isEmpty()) label = user;
                    CredentialRecord updated = new CredentialRecord(
                            existing == null ? CredentialVault.newId() : existing.id,
                            label, selected[0] == null ? "" : selected[0].packageName,
                            selected[0] == null ? "" : selected[0].label,
                            user, value(password));
                    List<CredentialRecord> candidate = new ArrayList<>(records);
                    int oldIndex = existing == null ? -1 : indexOf(existing.id);
                    if (oldIndex >= 0) candidate.set(oldIndex, updated);
                    else candidate.add(updated);
                    try {
                        CredentialVault.save(getApplicationContext(), candidate);
                        records.clear();
                        records.addAll(candidate);
                        renderEntries();
                        dialog.dismiss();
                    } catch (Exception ignoredSave) {
                        Toast.makeText(this, R.string.vault_save_error, Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void showQuickEmail() {
        if (!vaultAccessible) return;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = ui.dp(16);
        form.setPadding(pad, ui.dp(8), pad, ui.dp(4));
        TextInputLayout box = inputLayout(R.string.vault_username_hint);
        TextInputEditText email = textField(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 256);
        box.addView(email);
        form.addView(box, matchParams());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vault_add_email)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String user = value(email);
                    if (user.isEmpty()) {
                        box.setError(getString(R.string.vault_username_missing));
                        return;
                    }
                    CredentialRecord rec = new CredentialRecord(CredentialVault.newId(), user, "", "", user, "");
                    List<CredentialRecord> candidate = new ArrayList<>(records);
                    candidate.add(rec);
                    try {
                        CredentialVault.save(getApplicationContext(), candidate);
                        records.clear();
                        records.addAll(candidate);
                        renderEntries();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.vault_save_error, Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
        email.requestFocus();
    }

    private TextInputLayout inputLayout(int hintRes) {
        TextInputLayout layout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(getString(hintRes));
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        return layout;
    }

    private TextInputEditText textField(int inputType, int maxLength) {
        TextInputEditText field = new TextInputEditText(this);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        return field;
    }

    private void chooseApp(List<AppChoice> apps, AppChoice[] selected,
                           MaterialButton button, EditText title) {
        CharSequence[] labels = new CharSequence[apps.size() + 1];
        labels[0] = getString(R.string.vault_app_none);
        int checked = selected[0] == null ? 0 : -1;
        for (int i = 0; i < apps.size(); i++) {
            AppChoice app = apps.get(i);
            labels[i + 1] = app.label + "  (" + app.packageName + ")";
            if (selected[0] != null && app.packageName.equals(selected[0].packageName)) {
                checked = i + 1;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vault_choose_app)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    AppChoice previous = selected[0];
                    selected[0] = which == 0 ? null : apps.get(which - 1);
                    button.setText(getString(R.string.vault_application) + ": "
                            + appLabel(selected[0]));
                    if (title.getText() == null || title.getText().toString().trim().isEmpty()
                            || (previous != null && previous.label.equals(title.getText().toString()))) {
                        title.setText(defaultTitle(selected[0]));
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private AppChoice initialApp(CredentialRecord existing, List<AppChoice> apps) {
        String packageName = existing == null ? currentPackage : existing.appPackage;
        if (packageName == null || packageName.isEmpty()) return null;
        if (browserPackages.contains(packageName)) return null;
        for (AppChoice app : apps) if (packageName.equals(app.packageName)) return app;
        String label = existing != null && existing.appLabel != null && !existing.appLabel.isEmpty()
                ? existing.appLabel : packageLabel(packageName);
        return new AppChoice(packageName, label);
    }

    @SuppressWarnings("deprecation")
    private List<AppChoice> launcherApps() {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(query, 0);
        List<AppChoice> result = new ArrayList<>();
        for (ResolveInfo info : activities) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String packageName = info.activityInfo.packageName;
            if (browserPackages.contains(packageName)) continue;
            boolean duplicate = false;
            for (AppChoice item : result) {
                if (item.packageName.equals(packageName)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                CharSequence appLabel = info.loadLabel(getPackageManager());
                result.add(new AppChoice(packageName,
                        appLabel == null ? packageName : appLabel.toString()));
            }
        }
        Collections.sort(result, (a, b) -> Collator.getInstance().compare(a.label, b.label));
        return result;
    }

    private String packageLabel(String packageName) {
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException ignored) {
            return packageName;
        }
    }

    private String appLabel(AppChoice app) {
        return app == null ? getString(R.string.vault_no_app) : app.label;
    }

    private String defaultTitle(AppChoice app) {
        return app == null ? getString(R.string.vault_app_default) : app.label;
    }

    private void confirmDelete(CredentialRecord record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vault_delete)
                .setMessage(R.string.vault_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    List<CredentialRecord> candidate = new ArrayList<>(records);
                    int index = indexOf(record.id);
                    if (index >= 0) candidate.remove(index);
                    try {
                        CredentialVault.save(getApplicationContext(), candidate);
                        records.clear();
                        records.addAll(candidate);
                        renderEntries();
                    } catch (Exception ignored) {
                        Toast.makeText(this, R.string.vault_save_error, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private int indexOf(String id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    private String value(TextView field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private LinearLayout.LayoutParams matchParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static final class AppChoice {
        final String packageName;
        final String label;

        AppChoice(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
