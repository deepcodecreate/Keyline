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

package com.keyline.ime.ime;

import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import com.keyline.ime.suggest.SuggestionEngine;

import java.util.List;
import java.util.Locale;

public final class InputController {
    public static final int SHIFT_OFF = 0;
    public static final int SHIFT_SINGLE = 1;
    public static final int SHIFT_LOCK = 2;

    public interface Host {
        InputConnection connection();
        boolean canLearn();
        boolean suggestionsOn();
        boolean autoCapOn();
        boolean spellCheckOn();
        boolean autoCorrectOn();
        String languageId();
        Locale locale();
        void onSuggestions(List<String> words);
        void onShiftChanged();
        boolean doubleSpacePeriod();
    }

    private final Host host;
    private final SuggestionEngine suggestions = SuggestionEngine.get();
    private com.keyline.ime.suggest.SpellHelper spell;
    private boolean typo;
    private List<String> fixes = java.util.Collections.emptyList();

    private String composing = "";
    private int shift = SHIFT_OFF;
    private boolean autoShift;
    private boolean lastWasSpace;
    private boolean spaceAuto;
    private long lastSpaceAt;
    private long lastEditAt;
    private boolean password;
    private boolean email;
    private boolean url;
    private int capMode;
    private int enterAction = EditorInfo.IME_ACTION_UNSPECIFIED;
    private boolean noEnterAction;
    private EditorInfo info;

    public InputController(Host host) {
        this.host = host;
    }

    public void start(EditorInfo editorInfo) {
        info = editorInfo;
        composing = "";
        lastWasSpace = false;
        spaceAuto = false;
        lastSpaceAt = 0;
        password = false;
        email = false;
        url = false;
        capMode = 0;
        enterAction = EditorInfo.IME_ACTION_UNSPECIFIED;
        noEnterAction = false;
        shift = SHIFT_OFF;
        if (editorInfo == null) {
            publishSuggestions();
            host.onShiftChanged();
            return;
        }
        int type = editorInfo.inputType;
        int cls = type & EditorInfo.TYPE_MASK_CLASS;
        int var = type & EditorInfo.TYPE_MASK_VARIATION;
        int flags = type & EditorInfo.TYPE_MASK_FLAGS;
        password = var == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                || var == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || var == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || var == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD;
        email = var == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || var == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
        url = var == EditorInfo.TYPE_TEXT_VARIATION_URI;
        if (cls == EditorInfo.TYPE_CLASS_TEXT && !password) {
            if ((flags & EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) capMode = 3;
            else if ((flags & EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0) capMode = 2;
            else if ((flags & EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) capMode = 1;
            else if (host.autoCapOn() && !email && !url) capMode = 1;
        }
        if (capMode == 3) shift = SHIFT_LOCK;
        enterAction = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
        noEnterAction = (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public boolean isPassword() { return password; }
    public boolean isEmail() { return email; }
    public boolean isUrl() { return url; }
    public int shift() { return shift; }
    public boolean autoShift() { return autoShift && shift == SHIFT_OFF; }
    public boolean effectiveUpper() { return shift != SHIFT_OFF || autoShift(); }
    public int enterAction() { return enterAction; }
    public boolean noEnterAction() { return noEnterAction; }
    public String composing() { return composing; }
    public EditorInfo info() { return info; }

    public void toggleShift() {
        long now = android.os.SystemClock.uptimeMillis();
        if (shift == SHIFT_LOCK) shift = SHIFT_OFF;
        else if (shift == SHIFT_SINGLE && now - lastShiftTap < 380) shift = SHIFT_LOCK;
        else if (shift == SHIFT_SINGLE) shift = SHIFT_OFF;
        else shift = SHIFT_SINGLE;
        lastShiftTap = now;
        autoShift = false;
        host.onShiftChanged();
        publishSuggestions();
    }

    private long lastShiftTap;

    public void onChar(String text) {
        if (text == null || text.isEmpty()) return;
        if (" ".equals(text)) {
            onSpace();
            return;
        }
        if (isWordChar(text) && !password && host.suggestionsOn()) {
            InputConnection ic = host.connection();
            if (ic != null) {
                syncComposing(ic);
                ic.beginBatchEdit();
                if (composing.isEmpty()) ic.finishComposingText();
                composing += text;
                ic.setComposingText(composing, 1);
                ic.endBatchEdit();
            } else {
                composing += text;
            }
            touch();
            typo = false;
            fixes = java.util.Collections.emptyList();
            requestSpelling();
            if (shift == SHIFT_SINGLE) shift = SHIFT_OFF;
            autoShift = false;
            lastWasSpace = false;
            spaceAuto = false;
            publishSuggestions();
            host.onShiftChanged();
            return;
        }
        commitWord(false, true);
        InputConnection ic = host.connection();
        if (ic != null) {
            ic.beginBatchEdit();
            if (spaceAuto && isSpacePunctuation(text)) {
                CharSequence before = ic.getTextBeforeCursor(1, 0);
                if (before != null && before.length() == 1 && before.charAt(0) == ' ') {
                    ic.deleteSurroundingText(1, 0);
                }
            }
            ic.commitText(text, 1);
            ic.endBatchEdit();
        }
        touch();
        if (shift == SHIFT_SINGLE) shift = SHIFT_OFF;
        lastWasSpace = false;
        spaceAuto = false;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    private static boolean isSpacePunctuation(String t) {
        if (t.length() != 1) return false;
        switch (t.charAt(0)) {
            case '.': case ',': case '!': case '?': case ';': case ':':
            case '\u060C': case '\u061B': case '\u061F':
                return true;
            default:
                return false;
        }
    }

    private void touch() { lastEditAt = SystemClock.uptimeMillis(); }

    private void syncComposing(InputConnection ic) {
        if (composing.isEmpty() || ic == null) return;
        CharSequence before;
        try {
            before = ic.getTextBeforeCursor(composing.length(), 0);
        } catch (RuntimeException e) {
            return;
        }
        if (before == null) return;
        if (!composing.contentEquals(before)) {
            ic.finishComposingText();
            composing = "";
            typo = false;
            fixes = java.util.Collections.emptyList();
        }
    }

    public void onSpace() {
        InputConnection ic = host.connection();
        if (ic == null) return;
        long now = SystemClock.uptimeMillis();
        boolean hadWord = !composing.isEmpty();
        commitWord(true, true);
        if (!hadWord && lastWasSpace && !spaceAuto && !password && !email && !url
                && host.doubleSpacePeriod() && now - lastSpaceAt < 450) {
            CharSequence before = ic.getTextBeforeCursor(2, 0);
            if (before != null && before.length() == 2 && before.charAt(1) == ' '
                    && Character.isLetterOrDigit(before.charAt(0))) {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(1, 0);
                ic.commitText(". ", 1);
                ic.endBatchEdit();
                touch();
                lastSpaceAt = 0;
                lastWasSpace = true;
                spaceAuto = false;
                refreshAutoShift();
                publishSuggestions();
                host.onShiftChanged();
                return;
            }
        }
        ic.commitText(" ", 1);
        touch();
        lastWasSpace = true;
        lastSpaceAt = now;
        spaceAuto = false;
        if (shift == SHIFT_SINGLE) shift = SHIFT_OFF;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public void onBackspace(boolean wholeWord) {
        InputConnection ic = host.connection();
        if (ic == null) return;
        syncComposing(ic);
        ic.beginBatchEdit();
        if (composing.length() > 0) {
            if (wholeWord) composing = "";
            else composing = composing.substring(0, composing.length() - 1);
            typo = false;
            fixes = java.util.Collections.emptyList();
            ic.setComposingText(composing, 1);
        } else if (wholeWord) {
            CharSequence before = ic.getTextBeforeCursor(48, 0);
            int n = wordLengthBefore(before);
            if (n > 0) ic.deleteSurroundingText(n, 0);
        } else {
            CharSequence sel = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(sel)) ic.commitText("", 1);
            else deleteOneBefore(ic);
        }
        ic.endBatchEdit();
        touch();
        lastWasSpace = false;
        spaceAuto = false;
        requestSpelling();
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    private void deleteOneBefore(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(2, 0);
        if (before == null || before.length() == 0) {
            long now = SystemClock.uptimeMillis();
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0));
            return;
        }
        int n = 1;
        int len = before.length();
        if (len >= 2 && Character.isLowSurrogate(before.charAt(len - 1))
                && Character.isHighSurrogate(before.charAt(len - 2))) n = 2;
        ic.deleteSurroundingText(n, 0);
    }

    public void onEnter() {
        InputConnection ic = host.connection();
        if (ic == null) return;
        commitWord(false, true);
        if (!noEnterAction && enterAction != EditorInfo.IME_ACTION_NONE
                && enterAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(enterAction);
        } else {
            ic.commitText("\n", 1);
        }
        touch();
        lastWasSpace = false;
        spaceAuto = false;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public void commitRaw(String text) {
        if (text == null) return;
        commitWord(false);
        InputConnection ic = host.connection();
        if (ic != null) ic.commitText(text, 1);
        touch();
        lastWasSpace = text.endsWith(" ");
        spaceAuto = false;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public void pickSuggestion(String word) {
        InputConnection ic = host.connection();
        if (ic == null || word == null) return;
        ic.beginBatchEdit();
        ic.commitText(word + " ", 1);
        ic.endBatchEdit();
        if (host.canLearn()) suggestions.learn(contextSafe(), host.languageId(), word);
        composing = "";
        typo = false;
        fixes = java.util.Collections.emptyList();
        touch();
        lastWasSpace = true;
        spaceAuto = true;
        lastSpaceAt = 0;
        if (shift == SHIFT_SINGLE) shift = SHIFT_OFF;
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public void moveCursor(int delta) {
        if (delta == 0) return;
        commitWord(false);
        InputConnection ic = host.connection();
        if (ic == null) return;
        touch();
        spaceAuto = false;
        ExtractedTextRequest req = new ExtractedTextRequest();
        req.token = 1;
        req.hintMaxChars = 400;
        ExtractedText et = ic.getExtractedText(req, 0);
        if (et == null || et.text == null) {
            int code = delta < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
            int times = Math.abs(delta);
            for (int i = 0; i < times; i++) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
            }
        } else {
            int base = Math.max(0, et.startOffset);
            int a = Math.min(et.selectionStart, et.selectionEnd);
            int b = Math.max(et.selectionStart, et.selectionEnd);
            int from = a == b ? a : (delta < 0 ? a : b);
            int rel = Math.max(0, Math.min(et.text.length(), from + delta));
            if (rel > 0 && rel < et.text.length()
                    && Character.isLowSurrogate(et.text.charAt(rel))
                    && Character.isHighSurrogate(et.text.charAt(rel - 1))) {
                rel += delta > 0 ? 1 : -1;
            }
            int np = base + rel;
            ic.setSelection(np, np);
        }
        refreshAutoShift();
        publishSuggestions();
        host.onShiftChanged();
    }

    public void onSelectionUpdate(int selStart, int selEnd, int candStart, int candEnd) {
        if (!composing.isEmpty()) {
            boolean inside = candStart >= 0 && candEnd >= candStart
                    && selStart == selEnd && selStart >= candStart && selStart <= candEnd;
            boolean ourEcho = SystemClock.uptimeMillis() - lastEditAt < 350;
            if (!inside && !ourEcho) {
                InputConnection ic = host.connection();
                if (ic != null) ic.finishComposingText();
                composing = "";
                typo = false;
                fixes = java.util.Collections.emptyList();
                publishSuggestions();
            }
        }
        spaceAuto = spaceAuto && SystemClock.uptimeMillis() - lastEditAt < 350;
        refreshAutoShift();
        host.onShiftChanged();
    }

    public void finishComposingNow() {
        InputConnection ic = host.connection();
        if (ic != null && !composing.isEmpty()) {
            try { ic.finishComposingText(); } catch (RuntimeException ignored) { }
        }
        composing = "";
        typo = false;
        fixes = java.util.Collections.emptyList();
    }

    public void onCursorMovedExternally() {
        if (!composing.isEmpty()) {
            composing = "";
            typo = false;
            fixes = java.util.Collections.emptyList();
            publishSuggestions();
        }
        refreshAutoShift();
        host.onShiftChanged();
    }

    private android.content.Context appContext;

    public void bindContext(android.content.Context c) {
        appContext = c.getApplicationContext();
    }

    private android.content.Context contextSafe() {
        return appContext;
    }

    private void commitWord(boolean learnIt) {
        commitWord(learnIt, false);
    }

    private void commitWord(boolean learnIt, boolean allowCorrect) {
        InputConnection ic = host.connection();
        if (ic != null && composing.length() > 0) {
            if (allowCorrect && typo && !fixes.isEmpty() && composing.length() >= 3
                    && host.autoCorrectOn() && host.spellCheckOn()) {
                ic.commitText(matchCase(fixes.get(0), composing), 1);
            } else {
                ic.finishComposingText();
                if (learnIt && host.canLearn()) {
                    suggestions.learn(contextSafe(), host.languageId(), composing);
                }
            }
        }
        composing = "";
        typo = false;
        fixes = java.util.Collections.emptyList();
    }

    private static String matchCase(String fix, String typed) {
        if (typed.length() > 1 && typed.equals(typed.toUpperCase(Locale.ROOT))
                && !typed.equals(typed.toLowerCase(Locale.ROOT))) return fix.toUpperCase(Locale.ROOT);
        if (startsUpperStatic(typed) && fix.length() > 0) {
            return fix.substring(0, 1).toUpperCase(Locale.ROOT) + fix.substring(1);
        }
        return fix;
    }

    private static boolean startsUpperStatic(String s) {
        return !s.isEmpty() && Character.isUpperCase(s.codePointAt(0));
    }

    private void requestSpelling() {
        if (!host.spellCheckOn() || password || composing.length() < 2 || appContext == null) return;
        if (spell == null) spell = new com.keyline.ime.suggest.SpellHelper(appContext);
        final String word = composing;
        spell.check(host.languageId(), word, host.locale(), (w, result) -> {
            if (!w.equals(composing)) return;
            typo = result.typo;
            fixes = result.suggestions == null ? java.util.Collections.emptyList() : result.suggestions;
            InputConnection ic = host.connection();
            if (typo && ic != null) {
                android.text.SpannableString span = new android.text.SpannableString(composing);
                span.setSpan(new android.text.style.SuggestionSpan(appContext, host.locale(),
                                fixes.toArray(new String[0]),
                                android.text.style.SuggestionSpan.FLAG_MISSPELLED, null),
                        0, composing.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ic.setComposingText(span, 1);
            }
            publishSuggestions();
        });
    }

    public void shutdown() {
        if (spell != null) spell.close();
        spell = null;
    }

    private void publishSuggestions() {
        if (!host.suggestionsOn() || password || composing.isEmpty()) {
            host.onSuggestions(java.util.Collections.emptyList());
            return;
        }
        boolean upperAll = shift == SHIFT_LOCK;
        boolean cap = effectiveUpper() || startsUpper(composing);
        List<String> list = suggestions.suggest(host.languageId(), composing, host.locale(), upperAll, cap && !upperAll);
        if (typo && !fixes.isEmpty() && host.spellCheckOn()) {
            List<String> fixed = new java.util.ArrayList<>();
            fixed.add(composing);
            for (String f : fixes) {
                if (fixed.size() >= 3) break;
                fixed.add(matchCase(f, composing));
            }
            list = fixed;
        }
        host.onSuggestions(list);
    }

    private boolean startsUpper(String s) {
        if (s.isEmpty()) return false;
        int cp = s.codePointAt(0);
        return Character.isUpperCase(cp);
    }

    private void refreshAutoShift() {
        autoShift = false;
        if (!host.autoCapOn() || password || capMode == 0 || shift == SHIFT_LOCK) return;
        if (!composing.isEmpty()) return;
        if (capMode == 3) {
            shift = SHIFT_LOCK;
            return;
        }
        InputConnection ic = host.connection();
        if (ic == null) {
            autoShift = true;
            return;
        }
        CharSequence before = ic.getTextBeforeCursor(8, 0);
        if (before == null || before.length() == 0) {
            autoShift = true;
            return;
        }
        int i = before.length() - 1;
        char last = before.charAt(i);
        if (capMode == 2 && Character.isWhitespace(last)) {
            autoShift = true;
            return;
        }
        while (i >= 0 && Character.isWhitespace(before.charAt(i))) i--;
        if (i < 0) {
            autoShift = true;
            return;
        }
        char c = before.charAt(i);
        if (c == '.' || c == '!' || c == '?' || c == '؟' || c == '\n') autoShift = true;
    }

    private static boolean isWordChar(String text) {
        if (text.codePointCount(0, text.length()) != 1) return false;
        int cp = text.codePointAt(0);
        if (cp == '\'' || cp == '-' || cp == 0x200C || cp == 0x200D) return true;
        return Character.isLetter(cp);
    }

    private static int wordLengthBefore(CharSequence before) {
        if (before == null || before.length() == 0) return 0;
        int i = before.length() - 1;
        while (i >= 0 && Character.isWhitespace(before.charAt(i))) i--;
        while (i >= 0 && !Character.isWhitespace(before.charAt(i))) i--;
        return before.length() - (i + 1);
    }
}
