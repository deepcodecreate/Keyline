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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Trie {
    private final Node root = new Node();
    private int size;

    public int size() { return size; }

    public void insert(String word, int freq) {
        if (word == null || word.isEmpty()) return;
        Node n = root;
        String lower = word.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            Node next = n.map.get(c);
            if (next == null) {
                next = new Node();
                n.map.put(c, next);
            }
            n = next;
        }
        if (n.word == null) size++;
        n.word = word;
        if (freq > n.freq) n.freq = freq;
    }

    public void collect(String prefix, List<Scored> out, int limit) {
        if (prefix == null) return;
        Node n = root;
        String p = prefix.toLowerCase(Locale.ROOT);
        for (int i = 0; i < p.length(); i++) {
            n = n.map.get(p.charAt(i));
            if (n == null) return;
        }
        walk(n, out);
        out.sort((a, b) -> Integer.compare(b.freq, a.freq));
        if (out.size() > limit) out.subList(limit, out.size()).clear();
    }

    public boolean contains(String word) {
        if (word == null || word.isEmpty()) return false;
        Node n = root;
        String l = word.toLowerCase(Locale.ROOT);
        for (int i = 0; i < l.length(); i++) {
            n = n.map.get(l.charAt(i));
            if (n == null) return false;
        }
        return n.word != null;
    }

    public List<Scored> fuzzy(String word, int maxDist, int limit) {
        List<Scored> out = new ArrayList<>();
        String w = word.toLowerCase(Locale.ROOT);
        int[] first = new int[w.length() + 1];
        for (int i = 0; i < first.length; i++) first[i] = i;
        for (Map.Entry<Character, Node> e : root.map.entrySet()) {
            fuzzyWalk(e.getValue(), e.getKey(), w, first, maxDist, out);
        }
        out.sort((a, b) -> a.dist != b.dist ? Integer.compare(a.dist, b.dist) : Integer.compare(b.freq, a.freq));
        if (out.size() > limit) out.subList(limit, out.size()).clear();
        return out;
    }

    private void fuzzyWalk(Node n, char c, String w, int[] prev, int max, List<Scored> out) {
        int len = w.length();
        int[] cur = new int[len + 1];
        cur[0] = prev[0] + 1;
        int min = cur[0];
        for (int i = 1; i <= len; i++) {
            int cost = w.charAt(i - 1) == c ? 0 : 1;
            cur[i] = Math.min(Math.min(prev[i] + 1, cur[i - 1] + 1), prev[i - 1] + cost);
            if (cur[i] < min) min = cur[i];
        }
        if (n.word != null && cur[len] <= max) out.add(new Scored(n.word, n.freq, cur[len]));
        if (min > max) return;
        for (Map.Entry<Character, Node> e : n.map.entrySet()) {
            fuzzyWalk(e.getValue(), e.getKey(), w, cur, max, out);
        }
    }

    private void walk(Node n, List<Scored> out) {
        if (n.word != null && n.freq > 0) out.add(new Scored(n.word, n.freq));
        for (Node c : n.map.values()) walk(c, out);
    }

    public static final class Scored {
        public final String word;
        public final int freq;
        public final int dist;

        public Scored(String word, int freq) {
            this(word, freq, 0);
        }

        public Scored(String word, int freq, int dist) {
            this.word = word;
            this.freq = freq;
            this.dist = dist;
        }
    }

    private static final class Node {
        final Map<Character, Node> map = new HashMap<>();
        String word;
        int freq;
    }

    public static List<Scored> empty() {
        return new ArrayList<>();
    }
}
