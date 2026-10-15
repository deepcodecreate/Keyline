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

public final class KeyboardTheme {
    public int background;
    public int key;
    public int special;
    public int text;
    public int hint;
    public int accent;
    public int popup;
    public int popupText;
    public int toolbar;
    public int suggestion;
    public int pressed;
    public int stroke;
    public boolean dark;

    public int toolbarIcon;
    public int suggestionText;
    public int specialText;
    public int actionKey;
    public int actionKeyText;

    public int withAlpha(int color, int alphaPercent) {
        int a = Math.max(0, Math.min(255, Math.round(255f * (alphaPercent / 100f))));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static final int GLASS_DARK = 0x4D000000;
    public static final int GLASS_LIGHT = 0x59FFFFFF;

    public int glassColor() { return dark ? GLASS_DARK : GLASS_LIGHT; }

    public int glassStroke() { return dark ? 0x22FFFFFF : 0x1F000000; }

    public int toolbarIconColor() { return toolbarIcon != 0 ? toolbarIcon : text; }
    public int suggestionTextColor() { return suggestionText != 0 ? suggestionText : text; }
    public int specialTextColor() { return specialText != 0 ? specialText : text; }
    public int actionKeyColor() { return actionKey != 0 ? actionKey : accent; }
    public int actionKeyTextColor() {
        if (actionKeyText != 0) return actionKeyText;
        int c = actionKeyColor();
        double lum = (0.299 * ((c >> 16) & 0xFF) + 0.587 * ((c >> 8) & 0xFF) + 0.114 * (c & 0xFF)) / 255.0;
        return lum > 0.6 ? 0xFF202124 : 0xFFFFFFFF;
    }

    public KeyboardTheme copy() {
        KeyboardTheme t = new KeyboardTheme();
        t.background = background; t.key = key; t.special = special; t.text = text;
        t.hint = hint; t.accent = accent; t.popup = popup; t.popupText = popupText;
        t.toolbar = toolbar; t.suggestion = suggestion; t.pressed = pressed; t.stroke = stroke;
        t.dark = dark; t.toolbarIcon = toolbarIcon; t.suggestionText = suggestionText;
        t.specialText = specialText; t.actionKey = actionKey; t.actionKeyText = actionKeyText;
        return t;
    }

    public KeyboardTheme overImage() {
        KeyboardTheme t = copy();
        t.background = 0;
        t.toolbar = 0;
        t.suggestion = 0;
        return t;
    }
}
