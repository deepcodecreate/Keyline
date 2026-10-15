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

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.Nullable;

public final class Haptics {
    private static Vibrator cached;

    private Haptics() {}

    private static Vibrator vibrator(Context ctx) {
        if (cached != null) return cached;
        Context app = ctx.getApplicationContext();
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                android.os.VibratorManager vm = (android.os.VibratorManager)
                        app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) cached = vm.getDefaultVibrator();
            }
            if (cached == null) cached = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}
        return cached;
    }

    @SuppressWarnings("deprecation")
    public static void tap(Context ctx, @Nullable View fallback, int strength) {
        int s = Math.max(1, Math.min(100, strength));
        Vibrator vib = vibrator(ctx);
        try {
            if (vib != null && vib.hasVibrator()) {
                long ms = 2 + Math.round(s * 0.16);
                if (Build.VERSION.SDK_INT >= 26) {
                    boolean amplitude = vib.hasAmplitudeControl();
                    if (!amplitude) ms += 4;
                    double curve = Math.pow(s / 100.0, 2.0);
                    int amp = amplitude
                            ? Math.max(1, Math.min(255, (int) Math.round(curve * 255)))
                            : VibrationEffect.DEFAULT_AMPLITUDE;
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    vib.vibrate(VibrationEffect.createOneShot(ms, amp), attrs);
                } else {
                    vib.vibrate(ms);
                }
                return;
            }
        } catch (Exception ignored) {
        }
        if (fallback != null) {
            fallback.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            | HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        }
    }
}
