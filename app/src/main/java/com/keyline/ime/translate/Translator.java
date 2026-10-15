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

package com.keyline.ime.translate;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class Translator {
    private Translator() {}

    public static final class Result {
        public final String text;
        public final String source;
        Result(String text, String source) { this.text = text; this.source = source; }
    }

    public static Result translate(String text, String target) throws Exception {
        String q = text.length() > 4000 ? text.substring(0, 4000) : text;
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                + URLEncoder.encode(target, "UTF-8") + "&dt=t&q=" + URLEncoder.encode(q, "UTF-8");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            if (c.getResponseCode() != 200) throw new java.io.IOException("HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONArray root = new JSONArray(out.toString("UTF-8"));
            JSONArray parts = root.getJSONArray(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                JSONArray p = parts.optJSONArray(i);
                if (p != null && !p.isNull(0)) sb.append(p.getString(0));
            }
            String src = root.length() > 2 && !root.isNull(2) ? root.optString(2, "") : "";
            return new Result(sb.toString(), src);
        } finally {
            c.disconnect();
        }
    }

    public static String translateKeepingLinks(String text, String target) throws Exception {
        java.util.List<com.keyline.ime.util.LinkDetector.Segment> parts =
                com.keyline.ime.util.LinkDetector.split(text);
        boolean anyLink = false;
        for (com.keyline.ime.util.LinkDetector.Segment p : parts) anyLink |= p.link;
        if (!anyLink) return translate(text, target).text;
        StringBuilder sb = new StringBuilder();
        for (com.keyline.ime.util.LinkDetector.Segment p : parts) {
            if (p.link) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
                sb.append('\u200E').append(p.text).append('\u200E');
                continue;
            }
            String words = p.text.trim();
            if (words.isEmpty()) continue;
            boolean hasLetter = false;
            for (int i = 0; i < words.length() && !hasLetter; i++) hasLetter = Character.isLetter(words.charAt(i));
            String piece = hasLetter ? translate(words, target).text.trim() : words;
            if (piece.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(piece);
        }
        return sb.toString();
    }
}
