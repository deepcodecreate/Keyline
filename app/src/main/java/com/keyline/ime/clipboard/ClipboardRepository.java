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

package com.keyline.ime.clipboard;

import android.content.Context;

import com.keyline.ime.db.AppDatabase;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.util.LinkDetector;

import java.util.ArrayList;
import java.util.List;

public final class ClipboardRepository {
    private ClipboardRepository() {}

    public static List<ClipEntity> all(Context ctx) {
        try {
            return AppDatabase.get(ctx).clipboardDao().all();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static final int MAX_LINKS_PER_CLIP = 5;

    public static void remember(Context ctx, String text) {
        if (text == null) return;
        String t = text.trim();
        if (t.isEmpty() || t.length() > 20000) return;
        try {
            ClipboardDao dao = AppDatabase.get(ctx).clipboardDao();
            long now = System.currentTimeMillis();

            List<String> links = new ArrayList<>();
            for (String l : LinkDetector.allLinks(t)) {
                if (!l.equals(t) && !links.contains(l)) links.add(l);
                if (links.size() >= MAX_LINKS_PER_CLIP) break;
            }

            upsert(dao, t, now);
            for (int i = 0; i < links.size(); i++) upsert(dao, links.get(i), now - 1 - i);

            int max = Math.max(5, Prefs.get(ctx).clipMax());
            List<ClipEntity> old = dao.unpinnedOldest();
            int overflow = old.size() - max;
            for (int i = 0; i < overflow; i++) {
                ClipEntity victim = old.get(i);
                if (!victim.pinned) dao.delete(victim);
            }
        } catch (Exception ignored) {}
    }

    private static void upsert(ClipboardDao dao, String text, long time) {
        ClipEntity existing = dao.find(text);
        if (existing != null) {
            existing.createdAt = time;
            dao.update(existing);
        } else {
            ClipEntity e = new ClipEntity();
            e.text = text;
            e.pinned = false;
            e.createdAt = time;
            dao.insert(e);
        }
    }

    public static void togglePin(Context ctx, ClipEntity e) {
        try {
            e.pinned = !e.pinned;
            AppDatabase.get(ctx).clipboardDao().update(e);
        } catch (Exception ignored) {}
    }

    public static void delete(Context ctx, ClipEntity e) {
        try {
            AppDatabase.get(ctx).clipboardDao().delete(e);
        } catch (Exception ignored) {}
    }

    public static void clear(Context ctx) {
        try {
            AppDatabase.get(ctx).clipboardDao().clearUnpinned();
        } catch (Exception ignored) {}
    }

    public static void clearUnpinned(Context ctx) {
        try {
            AppDatabase.get(ctx).clipboardDao().clearUnpinned();
        } catch (Exception ignored) {}
    }

    public static void restore(Context ctx, ClipEntity e) {
        try {
            ClipEntity c = new ClipEntity();
            c.text = e.text;
            c.pinned = e.pinned;
            c.createdAt = e.createdAt;
            AppDatabase.get(ctx).clipboardDao().insert(c);
        } catch (Exception ignored) {}
    }
}
