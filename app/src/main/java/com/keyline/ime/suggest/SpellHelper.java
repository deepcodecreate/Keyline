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
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpellHelper implements SpellCheckerSession.SpellCheckerSessionListener {
    public interface Callback {
        void onResult(String word, SuggestionEngine.Spell result);
    }

    private static final class Req {
        final String word;
        final Callback cb;
        Req(String word, Callback cb) { this.word = word; this.cb = cb; }
    }

    private final Context ctx;
    private SpellCheckerSession session;
    private String sessionTag = "";
    private int seq;
    private final Map<Integer, Req> pending = new LinkedHashMap<>();

    public SpellHelper(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void check(String lang, String word, Locale locale, Callback cb) {
        SuggestionEngine.Spell offline = SuggestionEngine.get().dictionarySpell(lang, word, locale);
        if (offline != null) {
            cb.onResult(word, offline);
            return;
        }
        if (SuggestionEngine.get().isUserWord(lang, word)) {
            cb.onResult(word, new SuggestionEngine.Spell(false, new ArrayList<>()));
            return;
        }
        SpellCheckerSession s = session(locale);
        if (s == null) return;
        int id = ++seq;
        pending.put(id, new Req(word, cb));
        while (pending.size() > 8) pending.remove(pending.keySet().iterator().next());
        try {
            s.getSentenceSuggestions(new TextInfo[]{new TextInfo(word, id, id)}, 5);
        } catch (RuntimeException e) {
            pending.remove(id);
        }
    }

    private SpellCheckerSession session(Locale locale) {
        String tag = locale.toLanguageTag();
        if (session != null && tag.equals(sessionTag) && session.isSessionDisconnected() == false) return session;
        close();
        try {
            TextServicesManager tsm = (TextServicesManager) ctx.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (tsm == null) return null;
            session = tsm.newSpellCheckerSession(null, locale, this, false);
            sessionTag = tag;
        } catch (RuntimeException e) {
            session = null;
        }
        return session;
    }

    public void close() {
        if (session != null) {
            try { session.close(); } catch (RuntimeException ignored) {}
        }
        session = null;
        sessionTag = "";
        pending.clear();
    }

    @Override public void onGetSuggestions(SuggestionsInfo[] results) { }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        if (results == null) return;
        for (SentenceSuggestionsInfo sentence : results) {
            if (sentence == null || sentence.getSuggestionsCount() < 1) continue;
            SuggestionsInfo info = sentence.getSuggestionsInfoAt(0);
            if (info == null) continue;
            Req req = pending.remove(info.getSequence());
            if (req == null && !pending.isEmpty()) {
                Integer last = null;
                for (Integer k : pending.keySet()) last = k;
                req = pending.remove(last);
            }
            if (req == null) continue;
            int attrs = info.getSuggestionsAttributes();
            boolean typo = (attrs & SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0;
            List<String> sugg = new ArrayList<>();
            for (int i = 0; i < info.getSuggestionsCount() && sugg.size() < 3; i++) {
                String t = info.getSuggestionAt(i);
                if (t != null && !t.isEmpty() && !t.equals(req.word)) sugg.add(t);
            }
            req.cb.onResult(req.word, new SuggestionEngine.Spell(typo, sugg));
        }
    }
}
