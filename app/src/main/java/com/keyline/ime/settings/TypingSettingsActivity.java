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

package com.keyline.ime.settings;

import android.widget.LinearLayout;

import com.keyline.ime.R;

public class TypingSettingsActivity extends SettingsPageActivity {
    @Override protected int titleRes() { return R.string.cat_typing; }

    @Override
    protected void build(LinearLayout content) {
        Prefs prefs = Prefs.get(this);

        ui.group(content, R.string.section_size);
        LinearLayout size = ui.card(content);
        ui.sliderRow(size, R.string.height, 20, 70, Math.round(prefs.height(false) * 100),
                value -> getString(R.string.height_value, value),
                value -> Prefs.get(this).height(false, value / 100f));
        ui.divider(size);
        ui.sliderRow(size, R.string.height, 20, 70, Math.round(prefs.height(true) * 100),
                value -> getString(R.string.height_land_value, value),
                value -> Prefs.get(this).height(true, value / 100f));
        ui.divider(size);
        ui.sliderRow(size, R.string.one_hand_size, 60, 90, prefs.oneHandWidth(),
                value -> getString(R.string.width_value, value),
                value -> Prefs.get(this).oneHandWidth(value));
        ui.divider(size);
        ui.sliderRow(size, R.string.float_size, 60, 130, prefs.floatScale(),
                value -> getString(R.string.size_value, value),
                value -> Prefs.get(this).floatScale(value));

        ui.group(content, R.string.section_feedback);
        LinearLayout feedback = ui.card(content);
        ui.switchRow(feedback, R.string.vibrate, 0, prefs.vibrate(), on -> {
            prefs.vibrate(on);
            if (on) com.keyline.ime.util.Haptics.tap(this, null, prefs.vibrateStrength());
        });
        ui.divider(feedback);
        ui.sliderRow(feedback, R.string.vibrate_strength, 1, 100, prefs.vibrateStrength(),
                value -> Integer.toString(value), value -> {
                    prefs.vibrateStrength(value);
                    com.keyline.ime.util.Haptics.tap(this, null, value);
                });
        ui.divider(feedback);
        ui.switchRow(feedback, R.string.sound, 0, prefs.sound(), prefs::sound);
        ui.divider(feedback);
        ui.switchRow(feedback, R.string.preview_popup, 0, prefs.preview(), prefs::preview);

        ui.group(content, R.string.section_input);
        LinearLayout input = ui.card(content);
        ui.switchRow(input, R.string.suggestions, R.string.suggestions_sub,
                prefs.suggestions(), prefs::suggestions);
        ui.divider(input);
        ui.switchRow(input, R.string.spell_check, R.string.spell_check_sub,
                prefs.spellCheck(), prefs::spellCheck);
        ui.divider(input);
        ui.switchRow(input, R.string.auto_correct, R.string.auto_correct_sub,
                prefs.autoCorrect(), prefs::autoCorrect);
        ui.divider(input);
        ui.switchRow(input, R.string.auto_cap, 0, prefs.autoCap(), prefs::autoCap);
        ui.divider(input);
        ui.switchRow(input, R.string.double_space_period, R.string.double_space_period_sub,
                prefs.doubleSpacePeriod(), prefs::doubleSpacePeriod);
        ui.divider(input);
        ui.switchRow(input, R.string.incognito, R.string.incognito_sub,
                prefs.incognito(), prefs::incognito);

        ui.group(content, R.string.section_touch);
        LinearLayout touch = ui.card(content);
        ui.switchRow(touch, R.string.longpress_popup, 0, prefs.longPress(), prefs::longPress);
        ui.divider(touch);
        ui.sliderRow(touch, R.string.lp_delay, 150, 800, prefs.longPressDelay(),
                value -> getString(R.string.lp_delay_value, value), prefs::longPressDelay);
        ui.divider(touch);
        ui.sliderRow(touch, R.string.repeat_speed, 25, 150, prefs.repeatMs(),
                value -> getString(R.string.repeat_value, value), prefs::repeatMs);
    }
}
