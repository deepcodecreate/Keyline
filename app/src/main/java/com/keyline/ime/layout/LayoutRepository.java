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

package com.keyline.ime.layout;

import android.content.Context;

import com.keyline.ime.settings.Prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LayoutRepository {
    public static final String[] BUNDLED = {"en", "fa", "ar", "ru", "es", "tr", "fr", "de"};
    private static final Map<String, LayoutModel> CACHE = new LinkedHashMap<>();

    private LayoutRepository() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static boolean isBundled(String id) {
        for (String b : BUNDLED) if (b.equals(id)) return true;
        return false;
    }

    public static LayoutModel load(Context ctx, String id) {
        LayoutModel cached = CACHE.get(id);
        if (cached != null) return cached;
        String json = readLanguage(ctx, id);
        if (json == null) return null;
        LayoutValidator.Result r = LayoutValidator.parse(json);
        if (!r.ok) return null;
        r.layout.bundled = isBundled(id);
        CACHE.put(id, r.layout);
        return r.layout;
    }

    public static LayoutModel functional(Context ctx, String name) {
        String key = "#" + name;
        LayoutModel cached = CACHE.get(key);
        if (cached != null) return cached;
        String json = readAsset(ctx, "layouts/" + name + ".json");
        if (json == null) return null;
        LayoutValidator.Result r = LayoutValidator.parse(json);
        if (!r.ok) return null;
        CACHE.put(key, r.layout);
        return r.layout;
    }

    public static List<LayoutModel> all(Context ctx) {
        List<LayoutModel> out = new ArrayList<>();
        List<String> order = Prefs.get(ctx).langOrder();
        List<String> seen = new ArrayList<>();
        for (String id : order) add(ctx, out, seen, id);
        for (String id : BUNDLED) add(ctx, out, seen, id);
        for (String id : Prefs.get(ctx).imported()) add(ctx, out, seen, id);
        return out;
    }

    private static void add(Context ctx, List<LayoutModel> out, List<String> seen, String id) {
        if (id == null || seen.contains(id)) return;
        LayoutModel m = load(ctx, id);
        if (m != null) {
            seen.add(id);
            out.add(m);
        }
    }

    public static List<String> enabled(Context ctx) {
        List<String> en = Prefs.get(ctx).enabledLangs();
        List<String> out = new ArrayList<>();
        for (String id : en) {
            if (load(ctx, id) != null) out.add(id);
        }
        if (out.isEmpty()) out.add("en");
        return out;
    }

    public static File importedFile(Context ctx, String id) {
        File dir = new File(ctx.getFilesDir(), "layouts");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, id + ".json");
    }

    public static String readLanguage(Context ctx, String id) {
        File f = importedFile(ctx, id);
        if (f.exists()) {
            try {
                return readStream(new FileInputStream(f));
            } catch (Exception ignored) {}
        }
        return readAsset(ctx, "layouts/" + id + ".json");
    }

    public static String readAsset(Context ctx, String path) {
        try {
            return readStream(ctx.getAssets().open(path));
        } catch (Exception e) {
            return null;
        }
    }

    public static String readStream(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }
}
