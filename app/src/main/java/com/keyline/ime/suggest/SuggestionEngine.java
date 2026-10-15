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

package com.keyline.ime.suggest;

import android.content.Context;

import com.keyline.ime.db.AppDatabase;
import com.keyline.ime.db.UserWordEntity;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.util.Bg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SuggestionEngine {
    private static final SuggestionEngine I = new SuggestionEngine();
    private final Map<String, Trie> dicts = new HashMap<>();
    private final Map<String, Map<String, UserWordEntity>> users = new HashMap<>();

    public static SuggestionEngine get() {
        return I;
    }

    public void warm(Context ctx, String lang) {
        Bg.io(() -> {
            ensureDict(ctx, lang);
            ensureUsers(ctx, lang);
        });
    }

    public List<String> suggest(String lang, String prefix, Locale locale, boolean upperAll, boolean capFirst) {
        List<String> out = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return out;
        Trie trie = dicts.get(lang);
        Map<String, UserWordEntity> user = users.get(lang);
        Map<String, Integer> scores = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        if (trie != null) {
            List<Trie.Scored> found = new ArrayList<>();
            trie.collect(prefix, found, 12);
            for (Trie.Scored s : found) {
                String key = s.word.toLowerCase(locale);
                scores.put(key, s.freq);
                display.put(key, s.word);
            }
        }
        if (user != null) {
            String p = prefix.toLowerCase(locale);
            for (UserWordEntity w : user.values()) {
                String key = w.word.toLowerCase(locale);
                if (!key.startsWith(p)) continue;
                int boost = w.frequency + ("manual".equals(w.source) ? 80000 : 2000);
                Integer cur = scores.get(key);
                scores.put(key, cur == null ? boost : cur + boost);
                display.put(key, w.word);
            }
        }
        List<String> keys = new ArrayList<>(scores.keySet());
        keys.sort((a, b) -> Integer.compare(scores.get(b), scores.get(a)));
        for (String k : keys) {
            if (out.size() >= 3) break;
            String shown = display.get(k);
            if (shown == null) continue;
            if (upperAll) shown = shown.toUpperCase(locale);
            else if (capFirst && shown.length() > 0) {
                shown = shown.substring(0, 1).toUpperCase(locale) + shown.substring(1);
            }
            if (!out.contains(shown)) out.add(shown);
        }
        return out;
    }

    public static final class Spell {
        public final boolean typo;
        public final List<String> suggestions;
        public Spell(boolean typo, List<String> suggestions) {
            this.typo = typo;
            this.suggestions = suggestions;
        }
    }

    public boolean isUserWord(String lang, String word) {
        Map<String, UserWordEntity> user = users.get(lang);
        return user != null && word != null && user.containsKey(word.toLowerCase(Locale.ROOT));
    }

    public Spell dictionarySpell(String lang, String word, Locale locale) {
        Trie trie = dicts.get(lang);
        if (trie == null || trie.size() < 2000 || word == null) return null;
        if (trie.contains(word) || isUserWord(lang, word)) return new Spell(false, new ArrayList<>());
        int max = word.length() <= 3 ? 1 : 2;
        List<String> out = new ArrayList<>();
        for (Trie.Scored sc : trie.fuzzy(word, max, 3)) out.add(sc.word);
        return new Spell(true, out);
    }

    public void learn(Context ctx, String lang, String word) {
        if (word == null) return;
        String clean = word.trim();
        if (clean.length() < 2) return;
        for (int i = 0; i < clean.length(); ) {
            int cp = clean.codePointAt(i);
            if (!Character.isLetter(cp) && cp != '\'' && cp != '-' && cp != 0x200C) return;
            i += Character.charCount(cp);
        }
        Map<String, UserWordEntity> map = users.computeIfAbsent(lang, k -> new HashMap<>());
        String key = clean.toLowerCase(Locale.ROOT);
        UserWordEntity e = map.get(key);
        if (e == null) {
            e = new UserWordEntity();
            e.lang = lang;
            e.word = clean;
            e.frequency = 1;
            e.source = "learned";
            map.put(key, e);
        } else if (!"manual".equals(e.source)) {
            e.frequency += 1;
            e.word = clean;
        } else {
            e.frequency += 1;
        }
        UserWordEntity copy = e;
        if (ctx == null) return;
        Bg.io(() -> {
            try {
                AppDatabase.get(ctx).userWordDao().upsert(copy);
            } catch (Exception ignored) {}
        });
    }

    public void addManual(Context ctx, String lang, String word) {
        UserWordEntity e = new UserWordEntity();
        e.lang = lang;
        e.word = word.trim();
        e.frequency = 50;
        e.source = "manual";
        users.computeIfAbsent(lang, k -> new HashMap<>()).put(e.word.toLowerCase(Locale.ROOT), e);
        Bg.io(() -> {
            try {
                AppDatabase.get(ctx).userWordDao().upsert(e);
            } catch (Exception ignored) {}
        });
    }

    public void deleteWord(Context ctx, String lang, String word) {
        Map<String, UserWordEntity> map = users.get(lang);
        if (map != null) map.remove(word.toLowerCase(Locale.ROOT));
        Bg.io(() -> {
            try {
                AppDatabase.get(ctx).userWordDao().delete(lang, word);
            } catch (Exception ignored) {}
        });
    }

    public void clearLearned(Context ctx) {
        for (Map<String, UserWordEntity> map : users.values()) {
            map.entrySet().removeIf(en -> !"manual".equals(en.getValue().source));
        }
        Bg.io(() -> {
            try {
                AppDatabase.get(ctx).userWordDao().clearLearned();
            } catch (Exception ignored) {}
        });
    }

    public void replaceAll(Context ctx, List<UserWordEntity> words) {
        users.clear();
        for (UserWordEntity w : words) {
            users.computeIfAbsent(w.lang, k -> new HashMap<>()).put(w.word.toLowerCase(Locale.ROOT), w);
        }
        Bg.io(() -> {
            try {
                AppDatabase.get(ctx).userWordDao().clearAll();
                for (UserWordEntity w : words) AppDatabase.get(ctx).userWordDao().upsert(w);
            } catch (Exception ignored) {}
        });
    }

    private void ensureDict(Context ctx, String lang) {
        if (dicts.containsKey(lang)) return;
        Trie t = new Trie();
        InputStream in = null;
        try {
            java.io.File f = new java.io.File(ctx.getFilesDir(), "dictionaries/" + lang + ".txt");
            if (f.exists()) in = new java.io.FileInputStream(f);
            else in = ctx.getAssets().open("dictionaries/" + lang + ".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int rank = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                String w = parts[0];
                int freq = parts.length > 1 ? safeInt(parts[1], 100000 - rank) : (100000 - rank);
                t.insert(w, freq);
                rank++;
                if (rank > 200000) break;
            }
            br.close();
        } catch (Exception ignored) {
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
        dicts.put(lang, t);
    }

    private void ensureUsers(Context ctx, String lang) {
        Map<String, UserWordEntity> map = users.computeIfAbsent(lang, k -> new HashMap<>());
        try {
            for (UserWordEntity w : AppDatabase.get(ctx).userWordDao().forLang(lang)) {
                String key = w.word.toLowerCase(Locale.ROOT);
                if (!map.containsKey(key)) map.put(key, w);
            }
        } catch (Exception ignored) {}
    }

    public void invalidateUsers() {
        users.clear();
    }

    private static int safeInt(String s, int d) {
        try { return Integer.parseInt(s); } catch (Exception e) { return d; }
    }

    @SuppressWarnings("unused")
    private static String assetExistsNote(Context ctx, String lang) {
        return LayoutRepository.readAsset(ctx, "dictionaries/" + lang + ".txt") == null ? "0" : "1";
    }
}
