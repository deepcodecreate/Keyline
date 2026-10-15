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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.keyline.ime.R;
import com.keyline.ime.settings.Prefs;
import com.keyline.ime.settings.PrefKeys;

import org.json.JSONObject;

public final class ThemeManager {
    public static final String[] IDS = {
            "system", "light", "amoled", "dynamic", "blue", "green", "purple", "orange",
            "rose", "teal", "cyan", "indigo"
    };

    private ThemeManager() {}

    public static boolean isNight(Context context) {
        int n = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return n == Configuration.UI_MODE_NIGHT_YES;
    }

    public static KeyboardTheme resolve(Context context) {
        return resolveId(context, Prefs.get(context).theme());
    }

    public static KeyboardTheme resolveId(Context context, String id) {
        String requested = id;
        boolean night = isNight(context);
        if ("custom".equals(id)) {
            KeyboardTheme custom = customTheme(context);
            if (custom != null) return custom;
            id = "system";
        }
        if ("dynamic".equals(id)) {
            KeyboardTheme dyn = dynamic(context, night);
            if (dyn != null) return dyn;
            id = "system";
        }
        int style;
        switch (id) {
            case "light":
                style = R.style.Kb_Light;
                break;
            case "amoled":
                style = R.style.Kb_Amoled;
                break;
            case "blue":
                style = R.style.Kb_Blue;
                break;
            case "green":
                style = R.style.Kb_Green;
                break;
            case "purple":
                style = R.style.Kb_Purple;
                break;
            case "orange":
                style = R.style.Kb_Orange;
                break;
            case "rose":
                style = R.style.Kb_Rose;
                break;
            case "teal":
                style = R.style.Kb_Teal;
                break;
            case "cyan":
                style = R.style.Kb_Cyan;
                break;
            case "indigo":
                style = R.style.Kb_Indigo;
                break;
            case "system":
            default:
                style = night ? R.style.Kb_Dark : R.style.Kb_Light;
                break;
        }
        KeyboardTheme t = fromStyle(context, style);
        t.dark = night || "amoled".equals(id) || "light".equals(id) == false && night;
        if ("light".equals(id)) t.dark = false;
        if ("amoled".equals(id)) t.dark = true;
        if ("system".equals(requested)) t.dark = night;
        return t;
    }

    private static KeyboardTheme fromStyle(Context context, int style) {
        KeyboardTheme t = new KeyboardTheme();
        TypedArray a = context.obtainStyledAttributes(style, R.styleable.KeyboardTheme);
        t.background = a.getColor(R.styleable.KeyboardTheme_keyboardBackground, 0xFFE8EAED);
        t.key = a.getColor(R.styleable.KeyboardTheme_keyColor, 0xFFFFFFFF);
        t.special = a.getColor(R.styleable.KeyboardTheme_specialKeyColor, 0xFFD5D8DC);
        t.text = a.getColor(R.styleable.KeyboardTheme_keyTextColor, 0xFF202124);
        t.hint = a.getColor(R.styleable.KeyboardTheme_keyHintColor, 0xFF5F6368);
        t.accent = a.getColor(R.styleable.KeyboardTheme_accentColor, 0xFF1A73E8);
        t.popup = a.getColor(R.styleable.KeyboardTheme_popupColor, 0xFFFFFFFF);
        t.popupText = a.getColor(R.styleable.KeyboardTheme_popupTextColor, t.text);
        t.toolbar = a.getColor(R.styleable.KeyboardTheme_toolbarColor, t.background);
        t.suggestion = a.getColor(R.styleable.KeyboardTheme_suggestionColor, t.background);
        t.pressed = a.getColor(R.styleable.KeyboardTheme_pressedOverlay, 0x14000000);
        t.stroke = a.getColor(R.styleable.KeyboardTheme_strokeColor, 0x14000000);
        a.recycle();
        t.dark = isDark(t.background);
        return t;
    }

    private static KeyboardTheme dynamic(Context context, boolean night) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;
        try {
            Context wrapped = DynamicColors.wrapContextIfAvailable(context);
            KeyboardTheme t = new KeyboardTheme();
            int surface = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorSurface, night ? 0xFF1B1B1F : 0xFFFFFBFE);
            int surfaceVar = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorSurfaceVariant, night ? 0xFF2B2B31 : 0xFFE7E0EC);
            int onSurface = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorOnSurface, night ? 0xFFE6E1E5 : 0xFF1C1B1F);
            int primary = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorPrimary, 0xFF1A73E8);
            int outline = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorOutline, 0x22000000);
            t.background = surface;
            t.toolbar = surface;
            t.suggestion = surface;
            t.key = night ? lift(surface, 0x18) : 0xFFFFFFFF;
            t.special = surfaceVar;
            t.text = onSurface;
            t.hint = MaterialColors.getColor(wrapped, com.google.android.material.R.attr.colorOnSurfaceVariant, onSurface);
            t.accent = primary;
            t.popup = t.key;
            t.popupText = onSurface;
            t.pressed = night ? 0x22FFFFFF : 0x14000000;
            t.stroke = outline & 0x00FFFFFF | 0x22000000;
            t.dark = night || isDark(surface);
            return t;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int lift(int color, int delta) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + delta);
        int g = Math.min(255, ((color >> 8) & 0xFF) + delta);
        int b = Math.min(255, (color & 0xFF) + delta);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static boolean isDark(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        double l = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return l < 0.55;
    }

    public static int labelRes(String id) {
        switch (id) {
            case "custom": return R.string.theme_custom;
            case "light": return R.string.theme_light;
            case "amoled": return R.string.theme_amoled;
            case "dynamic": return R.string.theme_dynamic;
            case "blue": return R.string.theme_blue;
            case "green": return R.string.theme_green;
            case "purple": return R.string.theme_purple;
            case "orange": return R.string.theme_orange;
            case "rose": return R.string.theme_rose;
            case "teal": return R.string.theme_teal;
            case "cyan": return R.string.theme_cyan;
            case "indigo": return R.string.theme_indigo;
            case "system":
            default: return R.string.theme_system;
        }
    }

    public static boolean hasCustomTheme(Context context) {
        return Prefs.get(context).raw().contains(PrefKeys.CUSTOM_THEME_JSON);
    }

    public static String customThemeName(Context context) {
        try {
            String json = Prefs.get(context).raw().getString(PrefKeys.CUSTOM_THEME_JSON, null);
            return json == null ? "" : new JSONObject(json).optString("name", "");
        } catch (Exception ignored) {
            return "";
        }
    }

    public static KeyboardTheme customTheme(Context context) {
        try {
            String json = Prefs.get(context).raw().getString(PrefKeys.CUSTOM_THEME_JSON, null);
            if (json == null) return null;
            JSONObject o = new JSONObject(json);
            KeyboardTheme t = new KeyboardTheme();
            t.background = o.getInt("background");
            t.key = o.getInt("key");
            t.special = o.getInt("special");
            t.text = o.getInt("text");
            t.hint = o.getInt("hint");
            t.accent = o.getInt("accent");
            t.popup = o.getInt("popup");
            t.popupText = o.getInt("popupText");
            t.toolbar = o.getInt("toolbar");
            t.suggestion = o.getInt("suggestion");
            t.pressed = o.getInt("pressed");
            t.stroke = o.getInt("stroke");
            t.dark = o.optBoolean("dark", isDark(t.background));
            t.toolbarIcon = o.optInt("toolbarIcon", 0);
            t.suggestionText = o.optInt("suggestionText", 0);
            t.specialText = o.optInt("specialText", 0);
            t.actionKey = o.optInt("actionKey", 0);
            t.actionKeyText = o.optInt("actionKeyText", 0);
            return t;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void saveCustomTheme(Context context, String name, KeyboardTheme t) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("background", t.background);
            o.put("key", t.key);
            o.put("special", t.special);
            o.put("text", t.text);
            o.put("hint", t.hint);
            o.put("accent", t.accent);
            o.put("popup", t.popup);
            o.put("popupText", t.popupText);
            o.put("toolbar", t.toolbar);
            o.put("suggestion", t.suggestion);
            o.put("pressed", t.pressed);
            o.put("stroke", t.stroke);
            o.put("dark", t.dark);
            o.put("toolbarIcon", t.toolbarIcon);
            o.put("suggestionText", t.suggestionText);
            o.put("specialText", t.specialText);
            o.put("actionKey", t.actionKey);
            o.put("actionKeyText", t.actionKeyText);
            Prefs.get(context).raw().edit()
                    .putString(PrefKeys.CUSTOM_THEME_JSON, o.toString())
                    .putString(PrefKeys.THEME, "custom")
                    .apply();
        } catch (Exception ignored) {
        }
    }
}
