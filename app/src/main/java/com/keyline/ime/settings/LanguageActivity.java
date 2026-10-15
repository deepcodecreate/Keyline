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
import android.net.Uri;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.keyline.ime.R;
import com.keyline.ime.layout.LayoutModel;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.layout.LayoutValidator;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LanguageActivity extends SettingsPageActivity {
    private final List<LayoutModel> items = new ArrayList<>();
    private LinearLayout body;
    private final ActivityResultLauncher<String[]> openDoc = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onImport);

    @Override protected int titleRes() { return R.string.languages_title; }

    @Override
    protected void build(LinearLayout content) {
        body = content;
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (body != null) reload();
    }

    private void openEditor(String sourceId, boolean copy) {
        Intent intent = new Intent(this, LayoutEditorActivity.class);
        intent.putExtra(LayoutEditorActivity.EXTRA_SOURCE, sourceId);
        intent.putExtra(LayoutEditorActivity.EXTRA_COPY, copy);
        startActivity(intent);
    }

    private void chooseStart() {
        List<String> ids = new ArrayList<>();
        List<CharSequence> names = new ArrayList<>();
        ids.add("");
        names.add(getString(R.string.lang_create_blank));
        for (LayoutModel m : items) {
            ids.add(m.id);
            names.add(m.name);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lang_create_title)
                .setItems(names.toArray(new CharSequence[0]),
                        (dialog, which) -> openEditor(ids.get(which), true))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void reload() {
        items.clear();
        items.addAll(LayoutRepository.all(this));
        render();
    }

    private void render() {
        if (body == null) return;
        body.removeAllViews();

        ui.body(body, getString(R.string.lang_hint), true);

        ui.group(body, R.string.lang_list_title);
        LinearLayout list = ui.card(body);
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) ui.divider(list);
            addLanguageRow(list, i);
        }

        ui.group(body, R.string.lang_add_title);
        LinearLayout add = ui.card(body);
        ui.navRow(add, R.drawable.ic_edit, getString(R.string.lang_create),
                getString(R.string.lang_create_sub), true, this::chooseStart);
        ui.divider(add);
        ui.navRow(add, R.drawable.ic_add, getString(R.string.import_json),
                getString(R.string.import_json_sub), false,
                () -> openDoc.launch(new String[]{"application/json", "text/*", "*/*"}));
        ui.divider(add);
        ui.navRow(add, R.drawable.ic_send, getString(R.string.export_template),
                getString(R.string.export_template_sub), false, this::shareTemplate);
    }

    private void addLanguageRow(LinearLayout list, int index) {
        LayoutModel m = items.get(index);
        Prefs prefs = Prefs.get(this);

        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setChecked(prefs.enabledLangs().contains(m.id));
        toggle.setContentDescription(getString(R.string.lang_enabled));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            List<String> en = new ArrayList<>(prefs.enabledLangs());
            if (checked) {
                if (!en.contains(m.id)) en.add(m.id);
            } else {
                if (en.size() <= 1) {
                    toast(getString(R.string.need_one_lang));
                    button.setChecked(true);
                    return;
                }
                en.remove(m.id);
                if (m.id.equals(prefs.activeLang()) && !en.isEmpty()) {
                    prefs.activeLang(en.get(0));
                }
            }
            prefs.enabledLangs(en);
        });

        View more = ui.iconAction(R.drawable.ic_more, getString(R.string.more_options), null);
        more.setOnClickListener(v -> showMenu(v, index, m));

        LinearLayout trailing = new LinearLayout(this);
        trailing.setGravity(Gravity.CENTER_VERTICAL);
        trailing.addView(toggle);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(ui.dp(40), ui.dp(40));
        moreParams.setMarginStart(ui.dp(4));
        trailing.addView(more, moreParams);

        String letters = m.id.length() > 3 ? m.id.substring(0, 3) : m.id;
        String sub = (m.bundled ? getString(R.string.bundled) : getString(R.string.imported))
                + "  \u00B7  " + m.id;
        ui.listItem(list, letters.toUpperCase(java.util.Locale.ROOT), m.name, sub, trailing,
                () -> toggle.setChecked(!toggle.isChecked()));
    }

    private void showMenu(View anchor, int index, LayoutModel m) {
        PopupMenu menu = new PopupMenu(this, anchor);
        Menu entries = menu.getMenu();
        MenuItem up = entries.add(0, 1, 0, R.string.move_up);
        MenuItem down = entries.add(0, 2, 1, R.string.move_down);
        up.setEnabled(index > 0);
        down.setEnabled(index < items.size() - 1);
        entries.add(0, 4, 2, m.bundled ? R.string.lang_edit_copy : R.string.lang_edit);
        if (!m.bundled) entries.add(0, 3, 3, R.string.delete);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: move(index, -1); return true;
                case 2: move(index, 1); return true;
                case 3: confirmDelete(m); return true;
                case 4: openEditor(m.id, m.bundled); return true;
                default: return false;
            }
        });
        menu.show();
    }

    private void onImport(Uri uri) {
        if (uri == null) return;
        try {
            String json = LayoutRepository.readStream(getContentResolver().openInputStream(uri));
            LayoutValidator.Result result = LayoutValidator.parse(json);
            if (!result.ok || result.layout == null) {
                toast(getString(R.string.import_fail) + "\n" + result.error);
                return;
            }
            if (LayoutRepository.isBundled(result.layout.id)) {
                toast(getString(R.string.import_fail) + "\n" + "id \"" + result.layout.id + "\" is reserved.");
                return;
            }
            boolean existed = LayoutRepository.importedFile(this, result.layout.id).exists();
            File out = LayoutRepository.importedFile(this, result.layout.id);
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
            List<String> imported = Prefs.get(this).imported();
            if (!imported.contains(result.layout.id)) imported.add(result.layout.id);
            Prefs.get(this).imported(imported);
            List<String> order = Prefs.get(this).langOrder();
            if (!order.contains(result.layout.id)) order.add(result.layout.id);
            Prefs.get(this).langOrder(order);
            List<String> enabled = Prefs.get(this).enabledLangs();
            if (!enabled.contains(result.layout.id)) enabled.add(result.layout.id);
            Prefs.get(this).enabledLangs(enabled);
            LayoutRepository.invalidate();
            reload();
        } catch (Exception e) {
            toast(getString(R.string.import_fail));
        }
    }

    private void shareTemplate() {
        try {
            String json = LayoutRepository.readAsset(this, "layouts/template.json");
            if (json == null) return;
            File cache = new File(getCacheDir(), "keyline-template.json");
            FileOutputStream fos = new FileOutputStream(cache);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", cache);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.export_template)));
        } catch (Exception e) {
            toast(getString(R.string.import_fail));
        }
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    private void move(int index, int dir) {
        List<String> order = new ArrayList<>();
        for (LayoutModel m : items) order.add(m.id);
        int j = index + dir;
        if (j < 0 || j >= order.size()) return;
        String tmp = order.get(index);
        order.set(index, order.get(j));
        order.set(j, tmp);
        Prefs.get(this).langOrder(order);
        LayoutRepository.invalidate();
        reload();
    }

    private void confirmDelete(LayoutModel m) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_lang_title)
                .setMessage(getString(R.string.delete_lang_msg, m.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    LayoutRepository.importedFile(this, m.id).delete();
                    List<String> imported = new ArrayList<>(Prefs.get(this).imported());
                    imported.remove(m.id);
                    Prefs.get(this).imported(imported);
                    List<String> order = new ArrayList<>(Prefs.get(this).langOrder());
                    order.remove(m.id);
                    Prefs.get(this).langOrder(order);
                    List<String> en = new ArrayList<>(Prefs.get(this).enabledLangs());
                    en.remove(m.id);
                    if (en.isEmpty()) en.add("en");
                    Prefs.get(this).enabledLangs(en);
                    if (m.id.equals(Prefs.get(this).activeLang())) Prefs.get(this).activeLang(en.get(0));
                    LayoutRepository.invalidate();
                    reload();
                })
                .show();
    }
}
