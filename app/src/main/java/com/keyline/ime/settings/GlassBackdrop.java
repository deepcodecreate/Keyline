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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

final class GlassBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int base;
    private final int c1;
    private final int c2;
    private final int c3;
    private RadialGradient g1;
    private RadialGradient g2;
    private RadialGradient g3;

    GlassBackdrop(Context ctx) {
        super(ctx);
        base = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0xFFFDFCFF);
        c1 = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, 0xFF1A73E8);
        c2 = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimaryContainer, 0xFFD3E3FD);
        c3 = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorTertiaryContainer, 0xFFFFD8EC);
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(RenderEffect.createBlurEffect(70f, 70f, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float r = Math.max(w, h) * 0.75f;
        g1 = new RadialGradient(w * 0.10f, h * 0.08f, r, ColorUtils.setAlphaComponent(c2, 0xE6),
                Color.TRANSPARENT, Shader.TileMode.CLAMP);
        g2 = new RadialGradient(w * 0.98f, h * 0.45f, r * 0.85f, ColorUtils.setAlphaComponent(c1, 0x55),
                Color.TRANSPARENT, Shader.TileMode.CLAMP);
        g3 = new RadialGradient(w * 0.15f, h * 1.0f, r * 0.9f, ColorUtils.setAlphaComponent(c3, 0xB0),
                Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(base);
        if (g1 == null) return;
        paint.setShader(g1);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(g2);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(g3);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }
}
