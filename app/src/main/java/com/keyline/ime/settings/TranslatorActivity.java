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
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.keyline.ime.R;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TranslatorActivity extends SettingsPageActivity {
    public static final String EXTRA_TEXT = "selected_text";
    private static final String[] LANGUAGE_CODES = {"auto", "fa", "en", "ar", "ru", "es", "tr", "fr", "de"};
    private static final int[] LANGUAGE_LABELS = {R.string.translate_auto, R.string.lang_persian, R.string.lang_english, R.string.lang_arabic, R.string.lang_russian, R.string.lang_spanish, R.string.lang_turkish, R.string.lang_french, R.string.lang_german};
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextInputEditText sourceText;
    private TextInputEditText resultText;
    private String sourceLanguage = "auto";
    private String targetLanguage = "fa";

    @Override protected int titleRes() { return R.string.translate_title; }

    @Override
    protected void build(LinearLayout content) {
        Prefs prefs = Prefs.get(this);
        targetLanguage = prefs.translateTarget();
        sourceLanguage = prefs.translateSource();
        ui.group(content, R.string.translate_target_group);
        LinearLayout languageCard = ui.card(content);
        ui.navRow(languageCard, R.drawable.ic_globe, getString(R.string.translate_source), languageLabel(sourceLanguage), true, () -> chooseLanguage(true));
        ui.navRow(languageCard, R.drawable.ic_globe, getString(R.string.translate_target), languageLabel(targetLanguage), true, () -> chooseLanguage(false));

        ui.group(content, R.string.translate_text_group);
        LinearLayout textCard = ui.card(content);
        TextInputLayout inputLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        inputLayout.setHint(getString(R.string.translate_input_hint));
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        sourceText = new TextInputEditText(this);
        sourceText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        sourceText.setSingleLine(false);
        sourceText.setMinLines(3);
        sourceText.setMaxLines(8);
        String selected = getIntent().getStringExtra(EXTRA_TEXT);
        if (selected != null) sourceText.setText(selected);
        inputLayout.addView(sourceText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldParams.setMargins(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(8));
        textCard.addView(inputLayout, fieldParams);
        ui.iconButton(textCard, R.string.translate_go, R.drawable.ic_translate, true, this::translate);

        ui.group(content, R.string.translate_result_group);
        LinearLayout resultCard = ui.card(content);
        TextInputLayout outputLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        outputLayout.setHint(getString(R.string.translate_result_hint));
        outputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        resultText = new TextInputEditText(this);
        resultText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        resultText.setSingleLine(false);
        resultText.setMinLines(3);
        resultText.setMaxLines(10);
        outputLayout.addView(resultText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        resultCard.addView(outputLayout, fieldParams);
        ui.iconButton(resultCard, R.string.translate_copy, R.drawable.ic_clipboard, false, this::copyResult);
        ui.body(resultCard, getString(R.string.translate_privacy_note), true);
    }

    private String languageLabel(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) if (LANGUAGE_CODES[i].equals(code)) return getString(LANGUAGE_LABELS[i]);
        return getString(R.string.lang_persian);
    }

    private void chooseLanguage(boolean source) {
        String current = source ? sourceLanguage : targetLanguage;
        CharSequence[] labels = new CharSequence[LANGUAGE_LABELS.length];
        int checked = 0;
        for (int i = 0; i < LANGUAGE_LABELS.length; i++) {
            labels[i] = getString(LANGUAGE_LABELS[i]);
            if (LANGUAGE_CODES[i].equals(current)) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle(source ? R.string.translate_source : R.string.translate_target)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (source) {
                        sourceLanguage = LANGUAGE_CODES[which];
                        Prefs.get(this).translateSource(sourceLanguage);
                    }
                    else {
                        targetLanguage = LANGUAGE_CODES[which];
                        if ("auto".equals(targetLanguage)) targetLanguage = "fa";
                        Prefs.get(this).translateTarget(targetLanguage);
                    }
                    if (sourceText != null && sourceText.getText() != null) getIntent().putExtra(EXTRA_TEXT, sourceText.getText().toString());
                    dialog.dismiss();
                    recreate();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void translate() {
        String text = sourceText == null || sourceText.getText() == null ? "" : sourceText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.translate_need_text, Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            String translated;
            try {
                String query = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + URLEncoder.encode(sourceLanguage, "UTF-8") + "&tl=" + URLEncoder.encode(targetLanguage, "UTF-8") + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
                HttpURLConnection connection = (HttpURLConnection) new URL(query).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream stream = connection.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    JSONArray segments = response.length() == 0 ? new JSONArray() : new JSONArray(response.toString()).getJSONArray(0);
                    StringBuilder output = new StringBuilder();
                    for (int i = 0; i < segments.length(); i++) {
                        JSONArray segment = segments.optJSONArray(i);
                        if (segment != null && !segment.isNull(0)) output.append(segment.optString(0));
                    }
                    translated = output.toString();
                } finally {
                    connection.disconnect();
                }
                if (translated.trim().isEmpty()) throw new IllegalStateException();
            } catch (Exception e) {
                translated = null;
            }
            String result = translated;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result == null) Toast.makeText(this, R.string.translate_failed, Toast.LENGTH_LONG).show();
                else resultText.setText(result);
            });
        });
    }

    private void copyResult() {
        String text = resultText == null || resultText.getText() == null ? "" : resultText.getText().toString();
        if (text.trim().isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("translation", text));
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
