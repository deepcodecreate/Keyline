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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

public final class CursorHelper {
    private CursorHelper() {}

    public static CharSequence selectedOrAll(InputConnection ic) {
        if (ic == null) return "";
        CharSequence sel = ic.getSelectedText(0);
        if (!TextUtils.isEmpty(sel)) return sel;
        ExtractedTextRequest req = new ExtractedTextRequest();
        req.token = 1;
        ExtractedText et = ic.getExtractedText(req, 0);
        if (et != null && et.text != null) return et.text;
        return "";
    }

    public static boolean copy(Context ctx, InputConnection ic) {
        CharSequence text = selectedOrAll(ic);
        if (TextUtils.isEmpty(text)) return false;
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return false;
        cm.setPrimaryClip(ClipData.newPlainText("keyline", text));
        return true;
    }

    public static CharSequence pasteText(Context ctx) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null) return "";
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        if (item == null) return "";
        CharSequence t = item.coerceToText(ctx);
        return t == null ? "" : t;
    }
}
