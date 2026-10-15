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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LayoutModel {
    public static final int SCHEMA = 2;

    public int schema = SCHEMA;
    public String kind = "letters";
    public String id = "en";
    public String name = "English";
    public String locale = "en";
    public String direction = "ltr";
    public boolean hasShift = true;
    public String spaceLabel = "English";
    public boolean bundled = true;
    public String digits = "latin";

    public List<List<KeySpec>> main = new ArrayList<>();
    public List<List<KeySpec>> shifted;
    public List<List<KeySpec>> numbers;
    public List<List<KeySpec>> symbols;
    public List<List<KeySpec>> symbols2;
    public List<KeySpec> bottom;
    public List<KeySpec> bottomNumbers;

    public String labelLetters;
    public String labelNumbers;
    public String labelSymbols;
    public String labelSymbols2;

    public boolean rtl() {
        return "rtl".equalsIgnoreCase(direction);
    }

    public Locale javaLocale() {
        if (locale == null || locale.isEmpty()) return Locale.US;
        return Locale.forLanguageTag(locale.replace('_', '-'));
    }

    public LayoutModel copy() {
        LayoutModel m = new LayoutModel();
        m.schema = schema;
        m.kind = kind;
        m.id = id;
        m.name = name;
        m.locale = locale;
        m.direction = direction;
        m.hasShift = hasShift;
        m.spaceLabel = spaceLabel;
        m.bundled = bundled;
        m.digits = digits;
        m.main = copyRows(main);
        m.shifted = copyRows(shifted);
        m.numbers = copyRows(numbers);
        m.symbols = copyRows(symbols);
        m.symbols2 = copyRows(symbols2);
        m.bottom = copyRow(bottom);
        m.bottomNumbers = copyRow(bottomNumbers);
        m.labelLetters = labelLetters;
        m.labelNumbers = labelNumbers;
        m.labelSymbols = labelSymbols;
        m.labelSymbols2 = labelSymbols2;
        return m;
    }

    public static List<List<KeySpec>> copyRows(List<List<KeySpec>> rows) {
        if (rows == null) return null;
        List<List<KeySpec>> out = new ArrayList<>();
        for (List<KeySpec> r : rows) out.add(copyRow(r));
        return out;
    }

    public static List<KeySpec> copyRow(List<KeySpec> row) {
        if (row == null) return null;
        List<KeySpec> out = new ArrayList<>();
        for (KeySpec k : row) out.add(k.copy());
        return out;
    }
}
