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

import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.keyline.ime.R;
import com.keyline.ime.util.Bg;

public class BackupSettingsActivity extends SettingsPageActivity {
    private final ActivityResultLauncher<String> backupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::exportBackup);
    private final ActivityResultLauncher<String[]> restoreLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::restoreBackup);

    @Override protected int titleRes() { return R.string.cat_backup; }

    @Override
    protected void build(LinearLayout content) {
        ui.group(content, R.string.cat_backup);
        LinearLayout card = ui.card(content);
        ui.navRow(card, R.drawable.ic_backup, getString(R.string.backup),
                getString(R.string.backup_sub), false,
                () -> backupLauncher.launch("keyline-backup.json"));
        ui.divider(card);
        ui.navRow(card, R.drawable.ic_paste, getString(R.string.restore),
                getString(R.string.restore_sub), false,
                () -> restoreLauncher.launch(new String[]{"application/json", "text/*", "*/*"}));
    }

    private void exportBackup(Uri uri) {
        if (uri == null) return;
        Bg.io(() -> {
            try {
                BackupManager.exportTo(getApplicationContext(), uri);
            } catch (Exception e) {
                Bg.main(() -> Toast.makeText(this, R.string.restore_fail, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void restoreBackup(Uri uri) {
        if (uri == null) return;
        Bg.io(() -> {
            try {
                BackupManager.restoreFrom(getApplicationContext(), uri);
                Bg.main(this::recreate);
            } catch (Exception e) {
                Bg.main(() -> Toast.makeText(this, R.string.restore_fail, Toast.LENGTH_LONG).show());
            }
        });
    }

}
