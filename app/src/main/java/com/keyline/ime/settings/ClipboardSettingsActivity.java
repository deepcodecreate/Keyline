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

import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.keyline.ime.R;
import com.keyline.ime.clipboard.ClipboardRepository;
import com.keyline.ime.util.Bg;

public class ClipboardSettingsActivity extends SettingsPageActivity {
    @Override protected int titleRes() { return R.string.cat_clipboard; }

    @Override
    protected void build(LinearLayout content) {
        Prefs prefs = Prefs.get(this);

        ui.group(content, R.string.section_clipboard);
        LinearLayout clip = ui.card(content);
        ui.sliderRow(clip, R.string.clip_max, 5, 100, prefs.clipMax(),
                value -> getString(R.string.clip_max_value, value), prefs::clipMax);
        ui.divider(clip);
        ui.switchRow(clip, R.string.clip_ignore, R.string.clip_ignore_sub,
                prefs.clipIgnoreSensitive(), prefs::clipIgnoreSensitive);
        ui.button(clip, R.string.clip_clear, false, this::confirmClearClipboard);

        ui.group(content, R.string.section_speech);
        LinearLayout speech = ui.card(content);
        ui.navRow(speech, R.drawable.ic_mic, getString(R.string.voice_lang), currentVoiceLabel(),
                true, this::chooseVoiceLanguage);
    }

    private static final String[] VOICE_VALUES =
            {"", "en-US", "fa-IR", "ar-SA", "ru-RU", "es-ES", "tr-TR", "fr-FR", "de-DE"};
    private static final int[] VOICE_LABELS = {R.string.voice_follow, R.string.lang_english,
            R.string.lang_persian, R.string.lang_arabic, R.string.lang_russian,
            R.string.lang_spanish, R.string.lang_turkish, R.string.lang_french,
            R.string.lang_german};

    private String currentVoiceLabel() {
        String current = Prefs.get(this).voiceLang();
        for (int i = 0; i < VOICE_VALUES.length; i++) {
            if (VOICE_VALUES[i].equals(current)) return getString(VOICE_LABELS[i]);
        }
        return getString(R.string.voice_follow);
    }

    private void chooseVoiceLanguage() {
        String[] values = VOICE_VALUES;
        int[] labels = VOICE_LABELS;
        CharSequence[] names = new CharSequence[labels.length];
        int checked = 0;
        for (int i = 0; i < labels.length; i++) {
            names[i] = getString(labels[i]);
            if (values[i].equals(Prefs.get(this).voiceLang())) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.voice_lang)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    Prefs.get(this).voiceLang(values[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void confirmClearClipboard() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.clip_clear_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> Bg.io(() -> {
                    ClipboardRepository.clear(getApplicationContext());
                }))
                .show();
    }

}
