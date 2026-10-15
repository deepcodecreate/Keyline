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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class LayoutSerializer {
    private LayoutSerializer() {}

    public static String toJson(LayoutModel m) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            line(sb, "schema", String.valueOf(LayoutModel.SCHEMA));
            if (!"letters".equals(m.kind)) line(sb, "kind", q(m.kind));
            line(sb, "id", q(m.id));
            line(sb, "name", q(m.name));
            line(sb, "locale", q(m.locale));
            line(sb, "direction", q(m.direction));
            line(sb, "hasShift", String.valueOf(m.hasShift));
            line(sb, "spaceLabel", q(m.spaceLabel == null ? m.name : m.spaceLabel));
            line(sb, "digits", q(m.digits == null ? "latin" : m.digits));
            JSONObject labels = new JSONObject();
            if (m.labelLetters != null) labels.put("letters", m.labelLetters);
            if (m.labelNumbers != null) labels.put("numbers", m.labelNumbers);
            if (m.labelSymbols != null) labels.put("symbols", m.labelSymbols);
            if (m.labelSymbols2 != null) labels.put("symbols2", m.labelSymbols2);
            if (labels.length() > 0) line(sb, "pageLabels", labels.toString());
            sb.append("  \"pages\": {\n");
            boolean first = true;
            first = pageBlock(sb, "main", m.main, first);
            first = pageBlock(sb, "shifted", m.shifted, first);
            first = pageBlock(sb, "numbers", m.numbers, first);
            first = pageBlock(sb, "symbols", m.symbols, first);
            pageBlock(sb, "symbols2", m.symbols2, first);
            sb.append("\n  }");
            if (m.bottom != null) {
                sb.append(",\n  \"bottom\": ").append(rowJson(m.bottom));
            }
            if (m.bottomNumbers != null) {
                sb.append(",\n  \"bottomNumbers\": ").append(rowJson(m.bottomNumbers));
            }
            sb.append("\n}\n");
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append("  \"").append(key).append("\": ").append(value).append(",\n");
    }

    private static String q(String s) {
        return JSONObject.quote(s == null ? "" : s);
    }

    private static boolean pageBlock(StringBuilder sb, String name, List<List<KeySpec>> rows, boolean first)
            throws Exception {
        if (rows == null) return first;
        if (!first) sb.append(",\n");
        sb.append("    \"").append(name).append("\": [\n");
        for (int i = 0; i < rows.size(); i++) {
            sb.append("      ").append(rowJson(rows.get(i)));
            if (i < rows.size() - 1) sb.append(',');
            sb.append('\n');
        }
        sb.append("    ]");
        return false;
    }

    private static String rowJson(List<KeySpec> row) throws Exception {
        JSONArray a = new JSONArray();
        for (KeySpec k : row) a.put(keyJson(k));
        return a.toString().replace("\\/", "/");
    }

    private static JSONObject keyJson(KeySpec k) throws Exception {
        JSONObject o = new JSONObject();
        if (k.isAction()) o.put("action", k.action);
        if (k.label != null && !k.label.isEmpty()) o.put("label", k.label);
        if (k.code != null) o.put("code", k.code);
        if (k.popup != null && k.popup.length > 0) {
            JSONArray p = new JSONArray();
            for (String s : k.popup) p.put(s);
            o.put("popup", p);
        }
        if (k.hint != null) o.put("hint", k.hint);
        if (k.style != null) o.put("style", k.style);
        if (k.when != null) o.put("when", k.when);
        if (Math.abs(k.width - 1f) > 0.001f) o.put("width", Math.round(k.width * 100f) / 100.0);
        return o;
    }
}
