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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LayoutValidator {
    public static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            "shift", "backspace", "enter", "space", "lang", "emoji",
            "to_numbers", "to_symbols", "to_symbols2", "to_letters"
    ));
    public static final Set<String> STYLES = new HashSet<>(Arrays.asList("normal", "special", "accent"));
    public static final Set<String> WHEN = new HashSet<>(Arrays.asList("signed", "decimal"));

    public static final String DIGITS_LATIN = "0123456789";
    public static final String DIGITS_PERSIAN = "۰۱۲۳۴۵۶۷۸۹";
    public static final String DIGITS_ARABIC = "٠١٢٣٤٥٦٧٨٩";

    public static final class Result {
        public final boolean ok;
        public final String error;
        public final LayoutModel layout;

        Result(boolean ok, String error, LayoutModel layout) {
            this.ok = ok;
            this.error = error;
            this.layout = layout;
        }
    }

    private LayoutValidator() {}

    public static String digitChars(String digits) {
        if (digits == null || digits.equals("latin")) return DIGITS_LATIN;
        if (digits.equals("persian")) return DIGITS_PERSIAN;
        if (digits.equals("arabic")) return DIGITS_ARABIC;
        return digits.codePointCount(0, digits.length()) == 10 ? digits : DIGITS_LATIN;
    }

    public static Result parse(String json) {
        if (json == null || json.trim().isEmpty()) return fail("File is empty.");
        try {
            JSONObject o = new JSONObject(json);
            LayoutModel m = new LayoutModel();
            m.schema = o.optInt("schema", 1);
            if (m.schema < 1 || m.schema > LayoutModel.SCHEMA) {
                return fail("\"schema\" " + m.schema + " is not supported (use 1 or 2).");
            }
            m.kind = o.optString("kind", "letters");
            if (!m.kind.equals("letters") && !m.kind.equals("pad")) {
                return fail("\"kind\" must be \"letters\" or \"pad\".");
            }
            if (!o.has("id")) return fail("Missing \"id\".");
            m.id = o.getString("id").trim();
            if (!m.id.matches("[A-Za-z0-9_\\-]{1,32}")) {
                return fail("\"id\" must be 1–32 letters, digits, _ or -.");
            }
            if (!o.has("name") || o.getString("name").trim().isEmpty()) return fail("Missing \"name\".");
            m.name = o.getString("name").trim();
            m.spaceLabel = o.optString("spaceLabel", m.name);
            m.direction = o.optString("direction", "ltr").toLowerCase(Locale.US);
            if (!m.direction.equals("ltr") && !m.direction.equals("rtl")) {
                return fail("\"direction\" must be \"ltr\" or \"rtl\".");
            }
            m.locale = o.optString("locale", m.id);
            try {
                Locale.forLanguageTag(m.locale.replace('_', '-'));
            } catch (Exception e) {
                return fail("\"locale\" is not a valid language tag.");
            }
            m.hasShift = o.optBoolean("hasShift", true);

            m.digits = o.optString("digits", "latin");
            if (!m.digits.equals("latin") && !m.digits.equals("persian") && !m.digits.equals("arabic")
                    && m.digits.codePointCount(0, m.digits.length()) != 10) {
                return fail("\"digits\" must be latin, persian, arabic or exactly ten characters.");
            }

            if (o.has("pageLabels") && !o.isNull("pageLabels")) {
                JSONObject pl = o.getJSONObject("pageLabels");
                m.labelLetters = label(pl, "letters");
                m.labelNumbers = label(pl, "numbers");
                m.labelSymbols = label(pl, "symbols");
                m.labelSymbols2 = label(pl, "symbols2");
            }

            if (!o.has("pages")) return fail("Missing \"pages\".");
            JSONObject pages = o.getJSONObject("pages");
            if (!pages.has("main")) return fail("Missing \"pages.main\".");
            m.main = readRows(pages.getJSONArray("main"), "pages.main");
            m.shifted = optRows(pages, "shifted");
            m.numbers = optRows(pages, "numbers");
            m.symbols = optRows(pages, "symbols");
            m.symbols2 = optRows(pages, "symbols2");
            if (o.has("bottom") && !o.isNull("bottom")) {
                m.bottom = readRow(o.getJSONArray("bottom"), "bottom");
            }
            if (o.has("bottomNumbers") && !o.isNull("bottomNumbers")) {
                m.bottomNumbers = readRow(o.getJSONArray("bottomNumbers"), "bottomNumbers");
            }

            String problem = m.kind.equals("pad") ? null : usability(m);
            if (problem != null) return fail(problem);
            return new Result(true, null, m);
        } catch (Fail f) {
            return fail(f.getMessage());
        } catch (Exception e) {
            return fail("Invalid JSON: " + e.getMessage());
        }
    }

    private static String usability(LayoutModel m) {
        List<List<KeySpec>> all = new ArrayList<>(m.main);
        if (m.bottom != null) all.add(m.bottom);
        if (!hasAction(all, "space")) return "The layout needs a \"space\" key (in pages.main or bottom).";
        if (!hasAction(all, "backspace")) return "The layout needs a \"backspace\" key.";
        if (!hasAction(all, "enter")) return "The layout needs an \"enter\" key (usually in bottom).";
        return null;
    }

    private static boolean hasAction(List<List<KeySpec>> rows, String action) {
        for (List<KeySpec> r : rows) for (KeySpec k : r) if (action.equals(k.action)) return true;
        return false;
    }

    private static String label(JSONObject o, String key) {
        return o.has(key) && !o.isNull(key) ? o.optString(key, null) : null;
    }

    private static List<List<KeySpec>> optRows(JSONObject pages, String key) throws Exception {
        if (!pages.has(key) || pages.isNull(key)) return null;
        return readRows(pages.getJSONArray(key), "pages." + key);
    }

    private static List<List<KeySpec>> readRows(JSONArray rows, String where) throws Exception {
        if (rows.length() < 1 || rows.length() > 8) throw new Fail(where + " must have 1–8 rows.");
        List<List<KeySpec>> out = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            if (!(rows.get(i) instanceof JSONArray)) throw new Fail(where + "[" + i + "] is not an array.");
            out.add(readRow(rows.getJSONArray(i), where + "[" + i + "]"));
        }
        return out;
    }

    private static List<KeySpec> readRow(JSONArray row, String where) throws Exception {
        if (row.length() < 1 || row.length() > 16) throw new Fail(where + " must have 1–16 keys.");
        List<KeySpec> keys = new ArrayList<>();
        for (int i = 0; i < row.length(); i++) {
            String at = where + "[" + i + "]";
            if (!(row.get(i) instanceof JSONObject)) throw new Fail(at + " must be an object.");
            JSONObject k = row.getJSONObject(i);
            KeySpec spec = new KeySpec();
            if (k.has("label") && !k.isNull("label")) spec.label = k.getString("label");
            if (k.has("code") && !k.isNull("code")) spec.code = k.getString("code");
            if (k.has("action") && !k.isNull("action")) {
                spec.action = k.getString("action");
                if (!ACTIONS.contains(spec.action)) {
                    throw new Fail(at + ": unknown action \"" + spec.action + "\". Allowed: "
                            + new java.util.TreeSet<>(ACTIONS) + ".");
                }
            }
            if (!spec.isAction() && (spec.label == null || spec.label.isEmpty()) && spec.code == null) {
                throw new Fail(at + ": needs \"label\", \"code\" or \"action\".");
            }
            if (k.has("width") && !k.isNull("width")) {
                double w = k.getDouble("width");
                if (w < 0.2 || w > 8) throw new Fail(at + ": \"width\" must be between 0.2 and 8.");
                spec.width = (float) w;
            }
            if (k.has("popup") && !k.isNull("popup")) {
                JSONArray p = k.getJSONArray("popup");
                if (p.length() > 16) throw new Fail(at + ": \"popup\" has more than 16 items.");
                spec.popup = new String[p.length()];
                for (int j = 0; j < p.length(); j++) {
                    spec.popup[j] = p.getString(j);
                    if (spec.popup[j].isEmpty()) throw new Fail(at + ": \"popup\" contains an empty item.");
                }
            }
            if (k.has("hint") && !k.isNull("hint")) spec.hint = k.getString("hint");
            if (k.has("style") && !k.isNull("style")) {
                spec.style = k.getString("style");
                if (!STYLES.contains(spec.style)) throw new Fail(at + ": \"style\" must be normal, special or accent.");
            }
            if (k.has("when") && !k.isNull("when")) {
                spec.when = k.getString("when");
                if (!WHEN.contains(spec.when)) throw new Fail(at + ": \"when\" must be signed or decimal.");
            }
            keys.add(spec);
        }
        return keys;
    }

    private static Result fail(String msg) {
        return new Result(false, msg, null);
    }

    private static final class Fail extends Exception {
        Fail(String m) { super(m); }
    }
}
