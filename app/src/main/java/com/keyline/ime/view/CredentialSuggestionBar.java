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

package com.keyline.ime.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.keyline.ime.R;
import com.keyline.ime.security.CredentialRecord;
import com.keyline.ime.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;

public final class CredentialSuggestionBar extends LinearLayout {
    public interface Listener {
        void onFillCredential(CredentialRecord record, boolean passwordField);
    }

    private final TextView[] slots = new TextView[3];
    private final CredentialRecord[] records = new CredentialRecord[3];
    private Listener listener;
    private KeyboardTheme theme;
    private boolean passwordField;

    public CredentialSuggestionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < slots.length; i++) {
            TextView slot = new TextView(context);
            slot.setGravity(Gravity.CENTER);
            slot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            slot.setTypeface(Typeface.DEFAULT);
            slot.setSingleLine(true);
            slot.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            int pad = dp(5);
            slot.setPadding(pad, 0, pad, 0);
            slot.setVisibility(GONE);
            addView(slot, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            slots[i] = slot;
            final int index = i;
            slot.setOnClickListener(v -> {
                CredentialRecord record = records[index];
                if (record != null && listener != null) {
                    listener.onFillCredential(record, passwordField);
                }
            });
        }
        setVisibility(GONE);
    }

    public void setListener(Listener value) {
        listener = value;
    }

    public void apply(KeyboardTheme value) {
        theme = value;
        setBackgroundColor(android.graphics.Color.TRANSPARENT);
        for (TextView slot : slots) {
            slot.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            slot.setTextColor(passwordField ? value.accent : value.text);
        }
    }

    public void setEntries(List<CredentialRecord> entries, boolean isPasswordField) {
        passwordField = isPasswordField;
        boolean any = false;
        for (int i = 0; i < slots.length; i++) {
            CredentialRecord record = entries != null && i < entries.size() ? entries.get(i) : null;
            records[i] = record;
            TextView slot = slots[i];
            if (record == null) {
                slot.setText("");
                slot.setTag(null);
                slot.setContentDescription(null);
                slot.setVisibility(GONE);
                continue;
            }
            String label = isPasswordField
                    ? record.title + " · " + record.username
                    : record.username;
            slot.setText(label);
            slot.setTag(record.id);
            slot.setContentDescription(isPasswordField
                    ? getContext().getString(R.string.vault_fill_login) + ": "
                            + record.title + ", " + record.username
                    : record.username);
            slot.setVisibility(VISIBLE);
            if (theme != null) {
                slot.setTextColor(isPasswordField ? theme.accent : theme.text);
            }
            any = true;
        }
        setVisibility(any ? VISIBLE : GONE);
    }

    public void clear() {
        setEntries(new ArrayList<>(), false);
    }

    public boolean hasEntries() {
        for (CredentialRecord record : records) if (record != null) return true;
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
