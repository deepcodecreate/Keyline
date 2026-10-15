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

import android.graphics.Typeface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.res.ColorStateList;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.keyline.ime.R;
import com.keyline.ime.theme.FontManager;
import com.keyline.ime.util.Bg;

import java.util.List;

public class FontSettingsActivity extends SettingsPageActivity {
    private LinearLayout host;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    @Override
    protected int titleRes() {
        return R.string.key_font;
    }

    @Override
    protected void build(LinearLayout content) {
        host = content;
        render();
    }

    private void render() {
        host.removeAllViews();
        String selected = Prefs.get(this).font();
        List<FontManager.Option> all = FontManager.options(this);

        ui.group(host, R.string.font_group_builtin);
        LinearLayout builtin = ui.card(host);
        boolean first = true;
        for (FontManager.Option option : all) {
            if (option.custom) continue;
            if (!first) ui.divider(builtin);
            first = false;
            builtin.addView(fontRow(option, selected), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ui.group(host, R.string.font_group_custom);
        LinearLayout custom = ui.card(host);
        first = true;
        for (FontManager.Option option : all) {
            if (!option.custom) continue;
            if (!first) ui.divider(custom);
            first = false;
            custom.addView(fontRow(option, selected), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (first) ui.body(custom, getString(R.string.font_custom_empty), true);

        ui.button(host, R.string.font_import, true, () -> picker.launch(new String[]{"*/*"}));
    }

    private View fontRow(FontManager.Option option, String selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ui.dp(20), ui.dp(12), ui.dp(12), ui.dp(12));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(this);
        name.setText(option.label);
        name.setTextColor(ui.onSurfaceVariant());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        column.addView(name);

        TextView sample = new TextView(this);
        sample.setText(R.string.font_sample);
        sample.setTextColor(ui.onSurface());
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        Typeface tf = FontManager.typeface(this, option.id);
        sample.setTypeface(tf == null ? Typeface.DEFAULT : tf);
        column.addView(sample);

        row.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (option.custom) {
            ImageView delete = new ImageView(this);
            delete.setImageResource(R.drawable.ic_delete);
            delete.setImageTintList(ColorStateList.valueOf(ui.onSurfaceVariant()));
            delete.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10));
            delete.setContentDescription(getString(R.string.delete));
            delete.setOnClickListener(v -> confirmRemove(option));
            row.addView(delete, new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)));
        }

        MaterialRadioButton radio = new MaterialRadioButton(this);
        radio.setChecked(option.id.equals(selected));
        radio.setClickable(false);
        radio.setFocusable(false);
        row.addView(radio, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ui.applyRipple(row);
        row.setOnClickListener(v -> {
            Prefs.get(this).font(option.id);
            render();
        });
        return row;
    }

    private void confirmRemove(FontManager.Option option) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.font_remove_confirm, option.label))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    Prefs prefs = Prefs.get(this);
                    if (option.id.equals(prefs.font())) prefs.font(FontManager.DEFAULT);
                    FontManager.remove(this, option.id);
                    render();
                })
                .show();
    }

    private void onPicked(Uri uri) {
        if (uri == null) return;
        android.content.Context app = getApplicationContext();
        Bg.io(() -> {
            String id = FontManager.importFont(app, uri);
            Bg.main(() -> {
                if (isDestroyed()) return;
                if (id == null) {
                    Toast.makeText(this, R.string.font_import_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                Prefs.get(this).font(id);
                render();
            });
        });
    }
}
