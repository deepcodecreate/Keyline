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

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public final class Prefs {
    private static Prefs instance;
    private final SharedPreferences sp;

    private Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PrefKeys.FILE, Context.MODE_PRIVATE);
        if (!sp.contains(PrefKeys.LANG_ORDER)) {
            editDefault();
        }
    }

    public static Prefs get(Context ctx) {
        if (instance == null) instance = new Prefs(ctx);
        return instance;
    }

    public SharedPreferences raw() {
        return sp;
    }

    public void listen(SharedPreferences.OnSharedPreferenceChangeListener l) {
        sp.registerOnSharedPreferenceChangeListener(l);
    }

    public void unlisten(SharedPreferences.OnSharedPreferenceChangeListener l) {
        sp.unregisterOnSharedPreferenceChangeListener(l);
    }

    private void editDefault() {
        sp.edit()
                .putString(PrefKeys.THEME, "system")
                .putFloat(PrefKeys.HEIGHT_PORT, 0.35f)
                .putFloat(PrefKeys.HEIGHT_LAND, 0.46f)
                .putInt(PrefKeys.KEY_ALPHA, 100)
                .putBoolean(PrefKeys.HIDE_KEY_BG, false)
                .putBoolean(PrefKeys.PREVIEW, true)
                .putBoolean(PrefKeys.LONG_PRESS, true)
                .putBoolean(PrefKeys.VIBRATE, true)
                .putInt(PrefKeys.VIBRATE_STRENGTH, 40)
                .putBoolean(PrefKeys.SOUND, false)
                .putBoolean(PrefKeys.SUGGESTIONS, true)
                .putBoolean(PrefKeys.AUTO_CAP, true)
                .putBoolean(PrefKeys.SPELL_CHECK, true)
                .putBoolean(PrefKeys.AUTO_CORRECT, false)
                .putBoolean(PrefKeys.INCOGNITO, false)
                .putBoolean(PrefKeys.EMAIL_SUGGESTIONS, true)
                .putBoolean(PrefKeys.LOGIN_SUGGESTIONS, true)
                .putInt(PrefKeys.LP_DELAY, 320)
                .putInt(PrefKeys.REPEAT_MS, 55)
                .putInt(PrefKeys.CLIP_MAX, 25)
                .putBoolean(PrefKeys.CLIP_IGNORE, true)
                .putInt(PrefKeys.BG_DIM, 28)
                .putInt(PrefKeys.BG_BLUR, 0)
                .putBoolean(PrefKeys.HAS_BG, false)
                .putString(PrefKeys.VOICE_LANG, "")
                .putString(PrefKeys.TRANSLATE_TARGET, "fa")
                .putString(PrefKeys.LANG_ORDER, "[\"en\",\"fa\",\"ar\",\"ru\",\"es\",\"tr\",\"fr\",\"de\"]")
                .putString(PrefKeys.LANG_ENABLED, "[\"en\",\"fa\"]")
                .putString(PrefKeys.ACTIVE_LANG, "en")
                .putString(PrefKeys.IMPORTED, "[]")
                .putString(PrefKeys.EMOJI_RECENTS, "[]")
                .apply();
    }

    public String theme() { return sp.getString(PrefKeys.THEME, "system"); }
    public void theme(String v) { sp.edit().putString(PrefKeys.THEME, v).apply(); }

    public float height(boolean landscape) {
        float d = landscape ? 0.46f : 0.35f;
        return sp.getFloat(landscape ? PrefKeys.HEIGHT_LAND : PrefKeys.HEIGHT_PORT, d);
    }

    public void height(boolean landscape, float ratio) {
        sp.edit().putFloat(landscape ? PrefKeys.HEIGHT_LAND : PrefKeys.HEIGHT_PORT, ratio).apply();
    }

    public int keyAlpha() { return sp.getInt(PrefKeys.KEY_ALPHA, 100); }
    public void keyAlpha(int v) { sp.edit().putInt(PrefKeys.KEY_ALPHA, v).apply(); }

    public boolean hideKeyBg() { return sp.getBoolean(PrefKeys.HIDE_KEY_BG, false); }
    public void hideKeyBg(boolean v) { sp.edit().putBoolean(PrefKeys.HIDE_KEY_BG, v).apply(); }

    public String font() { return sp.getString(PrefKeys.FONT, "default"); }
    public void font(String v) { sp.edit().putString(PrefKeys.FONT, v == null ? "default" : v).apply(); }

    public int fontSize() { return sp.getInt(PrefKeys.FONT_SIZE, 100); }
    public void fontSize(int v) { sp.edit().putInt(PrefKeys.FONT_SIZE, Math.max(70, Math.min(140, v))).apply(); }

    public boolean preview() { return sp.getBoolean(PrefKeys.PREVIEW, true); }
    public void preview(boolean v) { sp.edit().putBoolean(PrefKeys.PREVIEW, v).apply(); }

    public boolean longPress() { return sp.getBoolean(PrefKeys.LONG_PRESS, true); }
    public void longPress(boolean v) { sp.edit().putBoolean(PrefKeys.LONG_PRESS, v).apply(); }

    public boolean vibrate() { return sp.getBoolean(PrefKeys.VIBRATE, true); }
    public void vibrate(boolean v) { sp.edit().putBoolean(PrefKeys.VIBRATE, v).apply(); }

    public int vibrateStrength() { return sp.getInt(PrefKeys.VIBRATE_STRENGTH, 40); }
    public void vibrateStrength(int v) { sp.edit().putInt(PrefKeys.VIBRATE_STRENGTH, v).apply(); }

    public boolean sound() { return sp.getBoolean(PrefKeys.SOUND, false); }
    public void sound(boolean v) { sp.edit().putBoolean(PrefKeys.SOUND, v).apply(); }

    public boolean suggestions() { return sp.getBoolean(PrefKeys.SUGGESTIONS, true); }
    public void suggestions(boolean v) { sp.edit().putBoolean(PrefKeys.SUGGESTIONS, v).apply(); }

    public boolean spellCheck() { return sp.getBoolean(PrefKeys.SPELL_CHECK, true); }
    public void spellCheck(boolean v) { sp.edit().putBoolean(PrefKeys.SPELL_CHECK, v).apply(); }

    public boolean autoCorrect() { return sp.getBoolean(PrefKeys.AUTO_CORRECT, false); }
    public void autoCorrect(boolean v) { sp.edit().putBoolean(PrefKeys.AUTO_CORRECT, v).apply(); }

    public boolean doubleSpacePeriod() { return sp.getBoolean(PrefKeys.DOUBLE_SPACE, true); }
    public void doubleSpacePeriod(boolean v) { sp.edit().putBoolean(PrefKeys.DOUBLE_SPACE, v).apply(); }

    public boolean autoCap() { return sp.getBoolean(PrefKeys.AUTO_CAP, true); }
    public void autoCap(boolean v) { sp.edit().putBoolean(PrefKeys.AUTO_CAP, v).apply(); }

    public boolean incognito() { return sp.getBoolean(PrefKeys.INCOGNITO, false); }
    public void incognito(boolean v) { sp.edit().putBoolean(PrefKeys.INCOGNITO, v).apply(); }

    public boolean emailSuggestions() { return sp.getBoolean(PrefKeys.EMAIL_SUGGESTIONS, true); }
    public void emailSuggestions(boolean v) { sp.edit().putBoolean(PrefKeys.EMAIL_SUGGESTIONS, v).apply(); }

    public boolean loginSuggestions() { return sp.getBoolean(PrefKeys.LOGIN_SUGGESTIONS, true); }
    public void loginSuggestions(boolean v) { sp.edit().putBoolean(PrefKeys.LOGIN_SUGGESTIONS, v).apply(); }

    public String settingsLanguage() { return sp.getString(PrefKeys.SETTINGS_LANGUAGE, "en"); }
    public void settingsLanguage(String value) { sp.edit().putString(PrefKeys.SETTINGS_LANGUAGE, "fa".equals(value) ? "fa" : "en").apply(); }

    public String translateSource() { return sp.getString(PrefKeys.TRANSLATE_SOURCE, "auto"); }
    public void translateSource(String value) { sp.edit().putString(PrefKeys.TRANSLATE_SOURCE, value == null || value.isEmpty() ? "auto" : value).apply(); }

    public String translateTarget() { return sp.getString(PrefKeys.TRANSLATE_TARGET, "fa"); }
    public void translateTarget(String value) {
        sp.edit().putString(PrefKeys.TRANSLATE_TARGET,
                value == null || value.isEmpty() ? "fa" : value).apply();
    }

    public int longPressDelay() { return sp.getInt(PrefKeys.LP_DELAY, 320); }
    public void longPressDelay(int v) { sp.edit().putInt(PrefKeys.LP_DELAY, v).apply(); }

    public int repeatMs() { return sp.getInt(PrefKeys.REPEAT_MS, 55); }
    public void repeatMs(int v) { sp.edit().putInt(PrefKeys.REPEAT_MS, v).apply(); }

    public int clipMax() { return sp.getInt(PrefKeys.CLIP_MAX, 25); }
    public void clipMax(int v) { sp.edit().putInt(PrefKeys.CLIP_MAX, v).apply(); }

    public boolean clipIgnoreSensitive() { return sp.getBoolean(PrefKeys.CLIP_IGNORE, true); }
    public void clipIgnoreSensitive(boolean v) { sp.edit().putBoolean(PrefKeys.CLIP_IGNORE, v).apply(); }

    public int bgDim() { return sp.getInt(PrefKeys.BG_DIM, 28); }
    public void bgDim(int v) { sp.edit().putInt(PrefKeys.BG_DIM, v).apply(); }

    public int bgBlur() { return sp.getInt(PrefKeys.BG_BLUR, 0); }
    public void bgBlur(int v) { sp.edit().putInt(PrefKeys.BG_BLUR, v).apply(); }

    public boolean hasBg() { return sp.getBoolean(PrefKeys.HAS_BG, false); }
    public void hasBg(boolean v) { sp.edit().putBoolean(PrefKeys.HAS_BG, v).apply(); }

    public void notifyBackgroundChanged() {
        Object old = sp.getAll().get(PrefKeys.BG_REFRESH);
        long next = (old instanceof Number ? ((Number) old).longValue() : 0L) + 1L;
        sp.edit().putLong(PrefKeys.BG_REFRESH, next).apply();
    }

    public String voiceLang() { return sp.getString(PrefKeys.VOICE_LANG, ""); }
    public void voiceLang(String v) { sp.edit().putString(PrefKeys.VOICE_LANG, v == null ? "" : v).apply(); }

    public String activeLang() { return sp.getString(PrefKeys.ACTIVE_LANG, "en"); }
    public void activeLang(String v) { sp.edit().putString(PrefKeys.ACTIVE_LANG, v).apply(); }

    public List<String> langOrder() { return readList(PrefKeys.LANG_ORDER); }
    public void langOrder(List<String> ids) { writeList(PrefKeys.LANG_ORDER, ids); }

    public List<String> enabledLangs() { return readList(PrefKeys.LANG_ENABLED); }
    public void enabledLangs(List<String> ids) { writeList(PrefKeys.LANG_ENABLED, ids); }

    public List<String> imported() { return readList(PrefKeys.IMPORTED); }
    public void imported(List<String> ids) { writeList(PrefKeys.IMPORTED, ids); }

    public boolean oneHand() { return sp.getBoolean(PrefKeys.ONE_HAND, false); }
    public void oneHand(boolean v) { sp.edit().putBoolean(PrefKeys.ONE_HAND, v).apply(); }

    public boolean oneHandRight() { return sp.getBoolean(PrefKeys.ONE_HAND_RIGHT, true); }
    public void oneHandRight(boolean v) { sp.edit().putBoolean(PrefKeys.ONE_HAND_RIGHT, v).apply(); }

    public int oneHandWidth() { return sp.getInt(PrefKeys.ONE_HAND_WIDTH, 83); }
    public void oneHandWidth(int v) { sp.edit().putInt(PrefKeys.ONE_HAND_WIDTH, v).apply(); }

    public int floatScale() { return sp.getInt(PrefKeys.FLOAT_SCALE, 100); }
    public void floatScale(int v) { sp.edit().putInt(PrefKeys.FLOAT_SCALE, v).apply(); }

    public boolean floating() { return sp.getBoolean(PrefKeys.FLOATING, false); }
    public void floating(boolean v) { sp.edit().putBoolean(PrefKeys.FLOATING, v).apply(); }

    public float floatX() { return sp.getFloat(PrefKeys.FLOAT_X, 0.5f); }
    public float floatY() { return sp.getFloat(PrefKeys.FLOAT_Y, 1f); }
    public void floatPos(float x, float y) {
        sp.edit().putFloat(PrefKeys.FLOAT_X, x).putFloat(PrefKeys.FLOAT_Y, y).apply();
    }

    public String emojiRecents() { return sp.getString(PrefKeys.EMOJI_RECENTS, "[]"); }
    public void emojiRecents(String json) { sp.edit().putString(PrefKeys.EMOJI_RECENTS, json).apply(); }

    public List<String> readList(String key) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Exception ignored) {}
        return out;
    }

    public void writeList(String key, List<String> ids) {
        JSONArray a = new JSONArray();
        for (String id : ids) a.put(id);
        sp.edit().putString(key, a.toString()).apply();
    }
}
