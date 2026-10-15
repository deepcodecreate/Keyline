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

import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;
import com.keyline.ime.db.AppDatabase;
import com.keyline.ime.db.UserWordEntity;
import com.keyline.ime.layout.LayoutModel;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.suggest.SuggestionEngine;
import com.keyline.ime.util.Bg;

import java.util.ArrayList;
import java.util.List;

public class DictionaryActivity extends SettingsPageActivity {
    private static final int PAGE = 40;

    private final List<UserWordEntity> words = new ArrayList<>();
    private LinearLayout body;
    private String lang;
    private int visible = PAGE;
    private boolean loaded;

    @Override protected int titleRes() { return R.string.manage_dict; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lang = Prefs.get(this).activeLang();
        super.onCreate(savedInstanceState);
        load();
    }

    @Override
    protected void build(LinearLayout content) {
        body = content;
        render();
    }

    private void load() {
        Bg.io(() -> {
            List<UserWordEntity> list = new ArrayList<>();
            try {
                list.addAll(AppDatabase.get(this).userWordDao().forLang(lang));
            } catch (Exception ignored) {}
            Bg.main(() -> {
                words.clear();
                words.addAll(list);
                loaded = true;
                render();
            });
        });
    }

    private String languageName() {
        LayoutModel model = LayoutRepository.load(this, lang);
        return model == null ? lang : model.name;
    }

    private void render() {
        if (body == null) return;
        body.removeAllViews();

        ui.group(body, R.string.dict_language);
        LinearLayout languageCard = ui.card(body);
        ui.navRow(languageCard, R.drawable.ic_globe, languageName(),
                getString(R.string.dict_language_sub), true, this::pickLanguage);

        ui.iconButton(body, R.string.add_word, R.drawable.ic_add, true, () -> edit(null));

        if (!loaded) return;

        ui.group(body, getString(R.string.dict_words_count, words.size()));
        LinearLayout card = ui.card(body);
        if (words.isEmpty()) {
            ui.emptyState(card, R.drawable.ic_book, getString(R.string.empty_dict),
                    getString(R.string.empty_dict_hint));
            return;
        }

        int shown = Math.min(visible, words.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) ui.divider(card);
            addWordRow(card, words.get(i));
        }
        if (shown < words.size()) {
            ui.button(card, R.string.show_more, false, () -> {
                visible += PAGE;
                render();
            });
        }

        ui.iconButton(body, R.string.clear_learned, R.drawable.ic_delete_sweep, false,
                this::confirmClearLearned);
    }

    private void addWordRow(LinearLayout card, UserWordEntity word) {
        boolean manual = "manual".equals(word.source);
        String sub = manual
                ? getString(R.string.word_manual)
                : getString(R.string.word_learned, word.frequency);
        android.view.View delete = ui.iconAction(R.drawable.ic_delete,
                getString(R.string.cd_delete), () -> {
                    SuggestionEngine.get().deleteWord(this, word.lang, word.word);
                    words.remove(word);
                    render();
                });
        ui.listItem(card, null, word.word, sub, delete, manual ? () -> edit(word) : null);
    }

    private void pickLanguage() {
        List<LayoutModel> all = LayoutRepository.all(this);
        CharSequence[] names = new CharSequence[all.size()];
        int checked = 0;
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).name;
            if (all.get(i).id.equals(lang)) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.languages_title)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String picked = all.get(which).id;
                    if (picked.equals(lang)) return;
                    lang = picked;
                    visible = PAGE;
                    loaded = false;
                    render();
                    load();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void edit(UserWordEntity existing) {
        TextInputLayout field = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        field.setHint(getString(R.string.word_hint));
        TextInputEditText input = new TextInputEditText(field.getContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        if (existing != null) {
            input.setText(existing.word);
            input.setSelection(input.length());
        }
        field.addView(input, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(ui.dp(24), ui.dp(8), ui.dp(24), 0);
        holder.addView(field, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.add_word : R.string.edit_word)
                .setView(holder)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String text = String.valueOf(input.getText()).trim();
                    if (text.isEmpty()) return;
                    if (existing != null) {
                        SuggestionEngine.get().deleteWord(this, existing.lang, existing.word);
                    }
                    SuggestionEngine.get().addManual(this, lang, text);
                    load();
                })
                .show();
        input.requestFocus();
    }

    private void confirmClearLearned() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_learned)
                .setMessage(R.string.clear_learned_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    SuggestionEngine.get().clearLearned(this);
                    load();
                })
                .show();
    }
}
