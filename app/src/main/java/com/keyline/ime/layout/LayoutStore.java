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

import android.content.Context;

import com.keyline.ime.settings.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class LayoutStore {
    private LayoutStore() {}

    public static String save(Context ctx, LayoutModel model, boolean addToKeyboard) {
        if (LayoutRepository.isBundled(model.id)) {
            return "\"" + model.id + "\" is reserved for a built-in language. Pick another id.";
        }
        String json = LayoutSerializer.toJson(model);
        LayoutValidator.Result check = LayoutValidator.parse(json);
        if (!check.ok) return check.error;
        try {
            File out = LayoutRepository.importedFile(ctx, model.id);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            return "Could not write the file.";
        }
        Prefs prefs = Prefs.get(ctx);
        List<String> imported = prefs.imported();
        if (!imported.contains(model.id)) imported.add(model.id);
        prefs.imported(imported);
        List<String> order = prefs.langOrder();
        if (!order.contains(model.id)) order.add(model.id);
        prefs.langOrder(order);
        if (addToKeyboard) {
            List<String> enabled = prefs.enabledLangs();
            if (!enabled.contains(model.id)) enabled.add(model.id);
            prefs.enabledLangs(enabled);
        }
        LayoutRepository.invalidate();
        return null;
    }

    public static File writeShareFile(Context ctx, LayoutModel model) throws Exception {
        File cache = new File(ctx.getCacheDir(), "keyline-" + model.id + ".json");
        try (FileOutputStream fos = new FileOutputStream(cache)) {
            fos.write(LayoutSerializer.toJson(model).getBytes(StandardCharsets.UTF_8));
        }
        return cache;
    }

    public static String freeId(Context ctx, String base) {
        String clean = base == null ? "custom" : base.replaceAll("[^A-Za-z0-9_\\-]", "-");
        if (clean.isEmpty()) clean = "custom";
        if (clean.length() > 28) clean = clean.substring(0, 28);
        String id = clean;
        int n = 2;
        while (LayoutRepository.isBundled(id) || LayoutRepository.importedFile(ctx, id).exists()) {
            id = clean + "-" + n++;
        }
        return id;
    }
}
