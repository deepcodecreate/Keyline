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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.keyline.ime.R;
import com.keyline.ime.theme.BackgroundImageManager;
import com.keyline.ime.theme.FontManager;
import com.keyline.ime.theme.ThemeManager;
import com.keyline.ime.util.Bg;

public class ThemeSettingsActivity extends SettingsPageActivity {
    private TextView backgroundStatus;
    private KeyboardPreviewView preview;
    private Bitmap previewBitmap;
    private int liveAlpha;
    private int liveDim;
    private int liveFontSize;
    private String shownFont;

    private final ActivityResultLauncher<String[]> imageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::openCropper);
    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshBackgroundStatus();
                    reloadPreviewBitmap();
                }
            });

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) recreate();
            });

    @Override protected int titleRes() { return R.string.cat_theme; }

    @Override
    protected void build(LinearLayout content) {
        Prefs prefs = Prefs.get(this);
        liveAlpha = prefs.keyAlpha();
        liveDim = prefs.bgDim();
        liveFontSize = prefs.fontSize();
        shownFont = prefs.font();

        ui.group(content, R.string.theme_preview);
        LinearLayout previewCard = ui.card(content);
        preview = new KeyboardPreviewView(this, null);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(210));
        plp.setMargins(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14));
        previewCard.addView(preview, plp);

        ui.group(content, R.string.section_themes);
        LinearLayout themes = ui.card(content);
        ui.navRow(themes, 0, getString(R.string.theme), currentThemeLabel(), true, this::chooseTheme);
        ui.divider(themes);
        ui.navRow(themes, 0, getString(R.string.theme_create), null, true, this::createCustomTheme);

        ui.group(content, R.string.section_keys);
        LinearLayout keys = ui.card(content);
        LinearLayout alphaRow = ui.sliderRow(keys, R.string.key_alpha, 0, 100, prefs.keyAlpha(),
                value -> getString(R.string.key_alpha_value, value),
                value -> { prefs.keyAlpha(value); liveAlpha = value; refreshPreview(); },
                value -> { liveAlpha = value; refreshPreview(); });
        ui.divider(keys);
        ui.switchRow(keys, R.string.hide_key_bg, R.string.hide_key_bg_sub,
                prefs.hideKeyBg(), value -> {
                    prefs.hideKeyBg(value);
                    ui.setRowEnabled(alphaRow, !value);
                    refreshPreview();
                });
        ui.setRowEnabled(alphaRow, !prefs.hideKeyBg());
        ui.divider(keys);
        ui.sliderRow(keys, R.string.key_font_size, 70, 140, prefs.fontSize(),
                value -> getString(R.string.key_font_size_value, value),
                value -> { prefs.fontSize(value); liveFontSize = value; refreshPreview(); },
                value -> { liveFontSize = value; refreshPreview(); });
        ui.divider(keys);
        ui.navRow(keys, 0, getString(R.string.key_font), FontManager.label(this, prefs.font()), true,
                () -> startActivity(new Intent(this, FontSettingsActivity.class)));

        ui.group(content, R.string.section_background);
        LinearLayout bg = ui.card(content);
        ui.navRow(bg, 0, getString(R.string.bg_image), getString(R.string.bg_pick), true,
                () -> imageLauncher.launch(new String[]{"image/*"}));
        backgroundStatus = ui.body(bg, getString(R.string.bg_none), true);
        ui.sliderRow(bg, R.string.bg_dim, 0, 80, prefs.bgDim(),
                value -> value + "%",
                value -> { prefs.bgDim(value); liveDim = value; refreshPreview(); },
                value -> { liveDim = value; refreshPreview(); });
        ui.sliderRow(bg, R.string.bg_blur, 0, 30, prefs.bgBlur(),
                value -> Integer.toString(value), value -> {
                    Prefs.get(this).bgBlur(value);
                    Bg.io(() -> BackgroundImageManager.rebuildAsync(getApplicationContext(),
                            () -> {
                                Prefs.get(getApplicationContext()).notifyBackgroundChanged();
                                Bg.main(this::reloadPreviewBitmap);
                            }));
                });
        ui.button(bg, R.string.bg_remove, false, this::confirmRemoveBackground);
        refreshBackgroundStatus();
        reloadPreviewBitmap();
    }

    private void refreshPreview() {
        if (preview == null) return;
        Prefs p = Prefs.get(this);
        preview.setFont(FontManager.typeface(this, p.font()));
        preview.setFontScale(liveFontSize / 100f);
        preview.bind(ThemeManager.resolve(this), previewBitmap, liveAlpha, p.hideKeyBg(), liveDim);
    }

    private void reloadPreviewBitmap() {
        refreshPreview();
        Bg.io(() -> {
            Bitmap bmp = BackgroundImageManager.loadDisplay(getApplicationContext());
            Bg.main(() -> {
                if (isDestroyed()) {
                    if (bmp != null) bmp.recycle();
                    return;
                }
                previewBitmap = bmp;
                refreshPreview();
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (preview != null) preview.bind(null, null, 100, false, 0);
        previewBitmap = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBackgroundStatus();
        if (shownFont != null && !shownFont.equals(Prefs.get(this).font())) {
            recreate();
        } else {
            refreshPreview();
        }
    }

    private String currentThemeLabel() {
        String current = Prefs.get(this).theme();
        if ("custom".equals(current) && ThemeManager.hasCustomTheme(this)) {
            return getString(R.string.theme_custom_named, ThemeManager.customThemeName(this));
        }
        return getString(ThemeManager.labelRes(current));
    }

    private void chooseTheme() {
        java.util.List<String> themeIds = new java.util.ArrayList<>(
                java.util.Arrays.asList(ThemeManager.IDS));
        if (ThemeManager.hasCustomTheme(this)) themeIds.add("custom");
        String[] ids = themeIds.toArray(new String[0]);
        CharSequence[] labels = new CharSequence[ids.length];
        int checked = 0;
        String current = Prefs.get(this).theme();
        for (int i = 0; i < ids.length; i++) {
            labels[i] = "custom".equals(ids[i])
                    ? getString(R.string.theme_custom_named, ThemeManager.customThemeName(this))
                    : getString(ThemeManager.labelRes(ids[i]));
            if (ids[i].equals(current)) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    Prefs.get(this).theme(ids[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.theme_create, (dialog, which) -> createCustomTheme())
                .show();
    }

    private void createCustomTheme() {
        editorLauncher.launch(new Intent(this, ThemeEditorActivity.class));
    }

    private void confirmRemoveBackground() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.bg_remove)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    BackgroundImageManager.remove(this);
                    refreshBackgroundStatus();
                    reloadPreviewBitmap();
                })
                .show();
    }

    private void refreshBackgroundStatus() {
        if (backgroundStatus == null) return;
        backgroundStatus.setText(Prefs.get(this).hasBg() ? R.string.bg_image : R.string.bg_none);
    }

    private void openCropper(Uri uri) {
        if (uri == null) return;
        Intent intent = new Intent(this, CropActivity.class);
        int[] target = BackgroundImageManager.targetSize(this);
        intent.putExtra("outW", target[0]);
        intent.putExtra("outH", target[1]);
        intent.setData(uri);
        intent.setClipData(ClipData.newUri(getContentResolver(), getString(R.string.bg_image), uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropLauncher.launch(intent);
    }
}
