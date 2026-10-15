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

package com.keyline.ime.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkDetector {
    private LinkDetector() {}

    private static final String TLD = "com|net|org|edu|gov|mil|int|info|biz|io|ai|app|dev|co|me|tv|xyz|online|site|shop|store"
            + "|tech|cloud|link|live|blog|news|pro|art|club|top|fun|vip|win|cafe|page|space|website|host|mobi|name|asia|wiki"
            + "|agency|digital|network|media|design|school|academy|center|world|today|life|email|chat|team|zone|click"
            + "|ir|uk|de|fr|es|it|ru|tr|ae|in|cn|jp|kr|us|ca|au|nl|se|ch|gl|ly|be|to|cc|ws|gg|eu|pl|br|mx|ua|ro|gr|pt"
            + "|il|sa|pk|af|iq|az|kz|vn|th|sg|hk|tw|nz|za|ng|eg|dk|fi|cz|hu|by|kg|uz|tj|ge|lv|lt|ee|sk|si|hr|rs|bg";

    private static final String HOST_CH =
            "[^\\s<>\"/?#\\u0600-\\u06FF\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\u00AB\\u00BB\\u201C\\u201D\\u2018\\u2019\\u2026]";
    private static final String PATH_CH =
            "[^\\s<>\"\\u060C\\u061B\\u061F\\u06D4\\u200B\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\u00AB\\u00BB\\u201C\\u201D\\u2018\\u2019\\u2026]";
    private static final String PATH = "(?:[/?#]" + PATH_CH + "*)?";

    private static final String OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final String IPV4 = "(?:" + OCTET + "\\.){3}" + OCTET;

    private static final Pattern URL = Pattern.compile(
            "(?<![@\\w.\\-])(?:"
                    + "[a-z][a-z0-9+.\\-]{1,19}://" + HOST_CH + "+" + PATH
                    + "|www\\." + HOST_CH + "+" + PATH
                    + "|(?:[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?\\.)+(?:" + TLD + ")(?![\\w@\\-]|\\.[a-z0-9])(?::\\d{1,5}(?!\\d))?" + PATH
                    + "|" + IPV4 + "(?!\\.?\\d)(?:(?::\\d{1,5}(?!\\d))" + PATH + "|[/?#]" + PATH_CH + "+)"
                    + "|localhost:\\d{1,5}(?!\\d)" + PATH
                    + ")",
            Pattern.CASE_INSENSITIVE);

    private static final String TRAILING = ".,;:!?'\"*`~>\u060C\u061B\u061F\u06D4\u00AB\u00BB\u201C\u201D\u2018\u2019\u2026\u200B\u200C\u200D\u200E\u200F";

    private static List<int[]> spans(String text) {
        List<int[]> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        Matcher m = URL.matcher(text);
        while (m.find()) {
            int start = m.start();
            int end = trimEnd(text, start, m.end());
            if (end > start && looksValid(text, start, end)) out.add(new int[]{start, end});
        }
        return out;
    }

    private static int trimEnd(String t, int s, int e) {
        while (e - s > 1) {
            char c = t.charAt(e - 1);
            if (c == ')') { if (count(t, s, e, '(') >= count(t, s, e, ')')) break; }
            else if (c == ']') { if (count(t, s, e, '[') >= count(t, s, e, ']')) break; }
            else if (c == '}') { if (count(t, s, e, '{') >= count(t, s, e, '}')) break; }
            else if (TRAILING.indexOf(c) < 0) break;
            e--;
        }
        return e;
    }

    private static int count(String t, int s, int e, char c) {
        int n = 0;
        for (int i = s; i < e; i++) if (t.charAt(i) == c) n++;
        return n;
    }

    private static boolean looksValid(String t, int s, int e) {
        int i = t.indexOf("://", s);
        if (i < 0 || i >= e) return true;
        int h = i + 3;
        if (h >= e) return false;
        char c = t.charAt(h);
        return Character.isLetterOrDigit(c) || c == '[';
    }

    public static boolean hasLink(String text) {
        return !spans(text).isEmpty();
    }

    public static String firstLink(String text) {
        List<int[]> s = spans(text);
        return s.isEmpty() ? null : text.substring(s.get(0)[0], s.get(0)[1]);
    }

    public static List<String> allLinks(String text) {
        List<String> out = new ArrayList<>();
        for (int[] s : spans(text)) out.add(text.substring(s[0], s[1]));
        return out;
    }

    public static String normalize(String link) {
        if (link == null) return null;
        String l = link.trim();
        if (l.isEmpty() || l.contains("://")) return l;
        return "https://" + l;
    }

    public static final class Segment {
        public final String text;
        public final boolean link;
        Segment(String text, boolean link) { this.text = text; this.link = link; }
    }

    public static List<Segment> split(String text) {
        List<Segment> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        int pos = 0;
        for (int[] s : spans(text)) {
            if (s[0] < pos) continue;
            if (s[0] > pos) out.add(new Segment(text.substring(pos, s[0]), false));
            out.add(new Segment(text.substring(s[0], s[1]), true));
            pos = s[1];
        }
        if (pos < text.length()) out.add(new Segment(text.substring(pos), false));
        return out;
    }
}
