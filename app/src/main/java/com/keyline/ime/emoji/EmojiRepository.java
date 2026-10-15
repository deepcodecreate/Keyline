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

package com.keyline.ime.emoji;

import android.content.Context;

import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.settings.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class EmojiRepository {
    public static final class Emoji {
        public final String value;
        public final String keywords;
        public final boolean tones;
        public final String category;

        public Emoji(String value, String keywords, boolean tones, String category) {
            this.value = value;
            this.keywords = keywords == null ? "" : keywords;
            this.tones = tones;
            this.category = category;
        }
    }

    public static final class Item {
        public final boolean header;
        public final String title;
        public final Emoji emoji;

        public Item(String title) {
            header = true;
            this.title = title;
            emoji = null;
        }

        public Item(Emoji emoji) {
            header = false;
            title = null;
            this.emoji = emoji;
        }
    }

    private static List<Emoji> all;
    private static final String[] TONES = {"", "\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF"};

    private EmojiRepository() {}

    public static List<Emoji> all(Context ctx) {
        if (all != null) return all;
        List<Emoji> list = new ArrayList<>();
        try {
            String json = LayoutRepository.readAsset(ctx, "emoji/emoji.json");
            JSONObject root = new JSONObject(json);
            JSONArray cats = root.getJSONArray("categories");
            for (int i = 0; i < cats.length(); i++) {
                JSONObject c = cats.getJSONObject(i);
                String id = c.getString("id");
                JSONArray em = c.getJSONArray("emojis");
                for (int j = 0; j < em.length(); j++) {
                    JSONObject e = em.getJSONObject(j);
                    list.add(new Emoji(e.getString("e"), e.optString("k", ""), e.optInt("t", 0) == 1, id));
                }
            }
        } catch (Exception ignored) {}
        all = list;
        return list;
    }

    public static List<Item> browse(Context ctx, String recentTitle, java.util.Map<String, String> titles) {
        List<Item> items = new ArrayList<>();
        List<String> recents = recents(ctx);
        items.add(new Item(recentTitle));
        for (String r : recents) items.add(new Item(new Emoji(r, "", false, "recent")));
        String last = "";
        for (Emoji e : all(ctx)) {
            if (!e.category.equals(last)) {
                last = e.category;
                String title = titles.containsKey(last) ? titles.get(last) : last;
                items.add(new Item(title));
            }
            items.add(new Item(e));
        }
        return items;
    }

    public static int indexOfCategory(List<Item> items, String category, java.util.Map<String, String> titles) {
        String title = titles.get(category);
        if (title == null) return 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).header && title.equals(items.get(i).title)) return i;
        }
        return 0;
    }

    public static List<Item> search(Context ctx, String q) {
        List<Item> items = new ArrayList<>();
        if (q == null || q.trim().isEmpty()) return items;
        String n = q.trim().toLowerCase(java.util.Locale.ROOT);
        for (Emoji e : all(ctx)) {
            if (e.value.contains(n) || e.keywords.toLowerCase(java.util.Locale.ROOT).contains(n)) {
                items.add(new Item(e));
            }
        }
        return items;
    }

    public static List<String> recents(Context ctx) {
        return Prefs.get(ctx).readList(Prefs.get(ctx).emojiRecents().startsWith("[") ? com.keyline.ime.settings.PrefKeys.EMOJI_RECENTS : com.keyline.ime.settings.PrefKeys.EMOJI_RECENTS);
    }

    public static void pushRecent(Context ctx, String emoji) {
        List<String> cur = Prefs.get(ctx).readList(com.keyline.ime.settings.PrefKeys.EMOJI_RECENTS);
        cur.remove(emoji);
        cur.add(0, emoji);
        if (cur.size() > 40) cur = new ArrayList<>(cur.subList(0, 40));
        Prefs.get(ctx).writeList(com.keyline.ime.settings.PrefKeys.EMOJI_RECENTS, cur);
    }

    public static String withTone(String base, int toneIndex) {
        if (toneIndex <= 0 || toneIndex >= TONES.length) return base;
        return base.replace("\uFE0F", "") + TONES[toneIndex];
    }
}
