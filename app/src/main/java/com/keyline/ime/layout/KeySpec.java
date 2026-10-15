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

public final class KeySpec {
    public String label = "";
    public String code;
    public String action;
    public String[] popup = new String[0];
    public String hint;
    public float width = 1f;
    public String style;
    public String when;

    public boolean isAction() {
        return action != null && action.length() > 0;
    }

    public String output(boolean upper, java.util.Locale locale) {
        if (code != null) return code;
        if (label == null) return "";
        if (upper && !isAction()) return label.toUpperCase(locale);
        return label;
    }

    public KeySpec copy() {
        KeySpec k = new KeySpec();
        k.label = label;
        k.code = code;
        k.action = action;
        k.popup = popup == null ? new String[0] : popup.clone();
        k.hint = hint;
        k.width = width;
        k.style = style;
        k.when = when;
        return k;
    }
}
