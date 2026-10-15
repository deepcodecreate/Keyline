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

import android.view.View;

public final class TopBarFade {
    private static final int DURATION = 140;

    private TopBarFade() {}

    public static void set(View v, boolean show) {
        if (v == null) return;
        Object cur = v.getTag(com.keyline.ime.R.id.top_bar_target);
        Boolean target = cur instanceof Boolean ? (Boolean) cur : null;
        if (target != null && target == show) return;
        v.setTag(com.keyline.ime.R.id.top_bar_target, show);
        v.animate().cancel();
        if (show) {
            boolean wasHidden = v.getVisibility() != View.VISIBLE;
            if (wasHidden) {
                v.setAlpha(0f);
                v.setTranslationY(v.getHeight() * 0.25f);
                v.setVisibility(View.VISIBLE);
            }
            v.animate().alpha(1f).translationY(0f).setDuration(DURATION).setListener(null).start();
        } else {
            if (v.getVisibility() != View.VISIBLE) return;
            v.animate().alpha(0f).translationY(-v.getHeight() * 0.25f).setDuration(DURATION)
                    .withEndAction(() -> {
                        if (Boolean.FALSE.equals(v.getTag(com.keyline.ime.R.id.top_bar_target))) {
                            v.setVisibility(View.GONE);
                            v.setTranslationY(0f);
                            v.setAlpha(1f);
                        }
                    }).start();
        }
    }
}
