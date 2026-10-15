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

package com.keyline.ime.theme;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import com.keyline.ime.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FontManager {
    public static final String DEFAULT = "default";
    private static final String ASSET_PREFIX = "asset:";
    private static final String CUSTOM_PREFIX = "custom:";
    private static final String ASSET_DIR = "fonts";
    private static final long MAX_BYTES = 24L * 1024L * 1024L;

    private static final String[] SYSTEM_IDS = {
            "sans", "sans-medium", "sans-light", "condensed", "serif", "mono", "casual", "cursive"};
    private static final String[] SYSTEM_FAMILIES = {
            "sans-serif", "sans-serif-medium", "sans-serif-light", "sans-serif-condensed",
            "serif", "monospace", "casual", "cursive"};
    private static final int[] SYSTEM_LABELS = {
            R.string.font_sans, R.string.font_sans_medium, R.string.font_sans_light,
            R.string.font_condensed, R.string.font_serif, R.string.font_mono,
            R.string.font_casual, R.string.font_cursive};

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private FontManager() {}

    public static final class Option {
        public final String id;
        public final String label;
        public final boolean custom;

        Option(String id, String label, boolean custom) {
            this.id = id;
            this.label = label;
            this.custom = custom;
        }
    }

    public static List<Option> options(Context c) {
        List<Option> out = new ArrayList<>();
        out.add(new Option(DEFAULT, c.getString(R.string.font_default), false));
        for (int i = 0; i < SYSTEM_IDS.length; i++) {
            out.add(new Option(SYSTEM_IDS[i], c.getString(SYSTEM_LABELS[i]), false));
        }
        try {
            String[] names = c.getAssets().list(ASSET_DIR);
            if (names != null) {
                Arrays.sort(names);
                for (String n : names) {
                    if (isFontName(n)) out.add(new Option(ASSET_PREFIX + n, stripExt(n), false));
                }
            }
        } catch (IOException ignored) {
        }
        File[] files = dir(c).listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (f.isFile() && isFontName(f.getName())) {
                    out.add(new Option(CUSTOM_PREFIX + f.getName(), stripExt(f.getName()), true));
                }
            }
        }
        return out;
    }

    public static String label(Context c, String id) {
        for (Option o : options(c)) {
            if (o.id.equals(id)) return o.label;
        }
        return c.getString(R.string.font_default);
    }

    public static synchronized Typeface typeface(Context c, String id) {
        if (id == null || DEFAULT.equals(id)) return null;
        if (CACHE.containsKey(id)) return CACHE.get(id);
        Typeface t = load(c.getApplicationContext(), id);
        CACHE.put(id, t);
        return t;
    }

    private static Typeface load(Context c, String id) {
        try {
            for (int i = 0; i < SYSTEM_IDS.length; i++) {
                if (SYSTEM_IDS[i].equals(id)) return Typeface.create(SYSTEM_FAMILIES[i], Typeface.NORMAL);
            }
            if (id.startsWith(ASSET_PREFIX)) {
                String name = id.substring(ASSET_PREFIX.length());
                if (!isPlainName(name)) return null;
                return Typeface.createFromAsset(c.getAssets(), ASSET_DIR + "/" + name);
            }
            if (id.startsWith(CUSTOM_PREFIX)) {
                String name = id.substring(CUSTOM_PREFIX.length());
                if (!isPlainName(name)) return null;
                File f = new File(dir(c), name);
                if (!f.isFile()) return null;
                return Typeface.createFromFile(f);
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    public static String importFont(Context c, Uri uri) {
        String name = safeName(displayName(c, uri));
        File dir = dir(c);
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        File out = new File(dir, name);
        boolean ok = false;
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in != null) {
                byte[] buf = new byte[16384];
                long total = 0;
                int n;
                ok = true;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_BYTES) {
                        ok = false;
                        break;
                    }
                    os.write(buf, 0, n);
                }
            }
        } catch (IOException | RuntimeException e) {
            ok = false;
        }
        if (!ok || !valid(out)) {
            out.delete();
            return null;
        }
        synchronized (FontManager.class) {
            CACHE.remove(CUSTOM_PREFIX + name);
        }
        return CUSTOM_PREFIX + name;
    }

    public static boolean remove(Context c, String id) {
        if (id == null || !id.startsWith(CUSTOM_PREFIX)) return false;
        String name = id.substring(CUSTOM_PREFIX.length());
        if (!isPlainName(name)) return false;
        boolean gone = new File(dir(c), name).delete();
        synchronized (FontManager.class) {
            CACHE.remove(id);
        }
        return gone;
    }

    private static boolean valid(File f) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                return new Typeface.Builder(f).build() != null;
            }
            Typeface t = Typeface.createFromFile(f);
            return t != null && t != Typeface.DEFAULT;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static File dir(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "fonts");
    }

    private static boolean isFontName(String n) {
        String l = n.toLowerCase(Locale.ROOT);
        return l.endsWith(".ttf") || l.endsWith(".otf");
    }

    private static boolean isPlainName(String n) {
        return !n.isEmpty() && new File(n).getName().equals(n) && !n.startsWith(".");
    }

    private static String stripExt(String n) {
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String displayName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                String n = cur.getString(0);
                if (n != null && !n.isEmpty()) return n;
            }
        } catch (RuntimeException ignored) {
        }
        String last = uri.getLastPathSegment();
        return last == null ? "font" : last;
    }

    private static String safeName(String raw) {
        String base = new File(raw).getName();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char ch = base.charAt(i);
            boolean fine = Character.isLetterOrDigit(ch) || ch == '.' || ch == '-' || ch == '_' || ch == ' ';
            sb.append(fine ? ch : '_');
        }
        String s = sb.toString().trim();
        while (s.startsWith(".")) s = s.substring(1);
        if (s.isEmpty()) s = "font";
        if (!isFontName(s)) s = s + ".ttf";
        if (s.length() > 80) s = s.substring(s.length() - 80);
        while (s.startsWith(".")) s = s.substring(1);
        return isFontName(s) ? s : "font.ttf";
    }
}
