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
import android.net.Uri;

import com.keyline.ime.db.AppDatabase;
import com.keyline.ime.db.UserWordEntity;
import com.keyline.ime.layout.LayoutRepository;
import com.keyline.ime.suggest.SuggestionEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class BackupManager {
    private BackupManager() {}

    public static void exportTo(Context ctx, Uri uri) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        JSONObject prefs = new JSONObject();
        SharedPreferences sp = Prefs.get(ctx).raw();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Float || v instanceof String) {
                prefs.put(e.getKey(), v);
            }
        }
        root.put("prefs", prefs);
        JSONArray layouts = new JSONArray();
        for (String id : Prefs.get(ctx).imported()) {
            File f = LayoutRepository.importedFile(ctx, id);
            if (!f.exists()) continue;
            JSONObject item = new JSONObject();
            item.put("id", id);
            item.put("json", LayoutRepository.readStream(new java.io.FileInputStream(f)));
            layouts.put(item);
        }
        root.put("layouts", layouts);
        JSONArray words = new JSONArray();
        for (UserWordEntity w : AppDatabase.get(ctx).userWordDao().all()) {
            JSONObject o = new JSONObject();
            o.put("lang", w.lang);
            o.put("word", w.word);
            o.put("frequency", w.frequency);
            o.put("source", w.source);
            words.put(o);
        }
        root.put("words", words);
        OutputStream os = ctx.getContentResolver().openOutputStream(uri);
        if (os == null) throw new IllegalStateException("output");
        os.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
    }

    public static void restoreFrom(Context ctx, Uri uri) throws Exception {
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) throw new IllegalStateException("input");
        String json = LayoutRepository.readStream(in);
        JSONObject root = new JSONObject(json);
        JSONObject prefs = root.getJSONObject("prefs");
        SharedPreferences.Editor ed = Prefs.get(ctx).raw().edit();
        java.util.Iterator<String> keys = prefs.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = prefs.get(k);
            if (v instanceof Boolean) ed.putBoolean(k, (Boolean) v);
            else if (v instanceof Integer) ed.putInt(k, (Integer) v);
            else if (v instanceof Long) ed.putLong(k, (Long) v);
            else if (v instanceof Double) ed.putFloat(k, ((Double) v).floatValue());
            else if (v instanceof String) ed.putString(k, (String) v);
        }
        ed.apply();
        JSONArray layouts = root.optJSONArray("layouts");
        if (layouts != null) {
            for (int i = 0; i < layouts.length(); i++) {
                JSONObject item = layouts.getJSONObject(i);
                String id = item.getString("id");
                String body = item.getString("json");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(LayoutRepository.importedFile(ctx, id));
                fos.write(body.getBytes(StandardCharsets.UTF_8));
                fos.close();
            }
        }
        LayoutRepository.invalidate();
        JSONArray words = root.optJSONArray("words");
        java.util.List<UserWordEntity> list = new java.util.ArrayList<>();
        if (words != null) {
            for (int i = 0; i < words.length(); i++) {
                JSONObject o = words.getJSONObject(i);
                UserWordEntity w = new UserWordEntity();
                w.lang = o.getString("lang");
                w.word = o.getString("word");
                w.frequency = o.optInt("frequency", 1);
                w.source = o.optString("source", "manual");
                list.add(w);
            }
        }
        SuggestionEngine.get().replaceAll(ctx, list);
    }
}
