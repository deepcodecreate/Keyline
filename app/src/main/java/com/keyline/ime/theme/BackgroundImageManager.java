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

package com.keyline.ime.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.keyline.ime.settings.Prefs;

import java.io.File;
import java.io.FileOutputStream;

public final class BackgroundImageManager {
    private BackgroundImageManager() {}

    private static final Paint COVER_PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private static final Rect COVER_SRC = new Rect();

    public static int[] targetSize(Context c) {
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int screenW = Math.min(dm.widthPixels, dm.heightPixels);
        int screenH = Math.max(dm.widthPixels, dm.heightPixels);
        float density = dm.density;
        int imeH = Math.round(screenH * Prefs.get(c).height(false));
        int minH = Math.round(180 * density);
        if (imeH < minH) imeH = minH;
        int keysH = Math.max(Math.round(120 * density), imeH);
        float k = Math.min(1f, 1080f / screenW);
        return new int[]{Math.max(1, Math.round(screenW * k)), Math.max(1, Math.round(keysH * k))};
    }

    public static void drawCover(Canvas canvas, Bitmap bmp, RectF dst) {
        if (bmp == null || bmp.isRecycled() || dst.width() <= 0 || dst.height() <= 0) return;
        float scale = Math.max(dst.width() / bmp.getWidth(), dst.height() / bmp.getHeight());
        float srcW = dst.width() / scale;
        float srcH = dst.height() / scale;
        int left = Math.round((bmp.getWidth() - srcW) / 2f);
        int top = Math.round((bmp.getHeight() - srcH) / 2f);
        COVER_SRC.set(Math.max(0, left), Math.max(0, top),
                Math.min(bmp.getWidth(), Math.round(left + srcW)),
                Math.min(bmp.getHeight(), Math.round(top + srcH)));
        canvas.drawBitmap(bmp, COVER_SRC, dst, COVER_PAINT);
    }

    public static File original(Context c) {
        return new File(c.getFilesDir(), "keyboard_bg_original.png");
    }

    public static File display(Context c) {
        return new File(c.getFilesDir(), "keyboard_bg.png");
    }

    public static void saveCropped(Context c, Bitmap cropped) throws Exception {
        writePng(original(c), cropped);
        rebuild(c, cropped);
        Prefs.get(c).hasBg(true);
    }

    public static void rebuildAsync(Context c, Runnable done) {
        Bitmap src = BitmapFactory.decodeFile(original(c).getAbsolutePath());
        if (src == null) {
            if (done != null) done.run();
            return;
        }
        try {
            rebuild(c, src);
        } catch (Exception ignored) {
        } finally {
            src.recycle();
        }
        if (done != null) done.run();
    }

    private static void rebuild(Context c, Bitmap cropped) throws Exception {
        int blur = Prefs.get(c).bgBlur();
        Bitmap out = cropped;
        boolean temp = false;
        if (blur > 0) {
            out = blur(cropped, blur);
            temp = out != cropped;
        }
        writePng(display(c), out);
        if (temp) out.recycle();
    }

    public static Bitmap loadDisplay(Context c) {
        if (!Prefs.get(c).hasBg()) return null;
        File f = display(c);
        if (!f.exists()) return null;
        return BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    public static void remove(Context c) {
        original(c).delete();
        display(c).delete();
        Prefs.get(c).hasBg(false);
    }

    private static void writePng(File f, Bitmap b) throws Exception {
        FileOutputStream os = new FileOutputStream(f);
        b.compress(Bitmap.CompressFormat.PNG, 92, os);
        os.flush();
        os.close();
    }

    public static Bitmap blur(Bitmap src, int radius) {
        int rw = Math.max(1, radius);
        int w = Math.min(src.getWidth(), 540);
        int h = Math.max(1, src.getHeight() * w / src.getWidth());
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        int r = Math.max(1, Math.round(rw * (w / (float) src.getWidth())));
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        box(px, w, h, r);
        box(px, w, h, r);
        small.setPixels(px, 0, w, 0, 0, w, h);
        Bitmap big = Bitmap.createScaledBitmap(small, src.getWidth(), src.getHeight(), true);
        if (small != src && small != big) small.recycle();
        return big;
    }

    private static void box(int[] px, int w, int h, int r) {
        int[] tmp = new int[px.length];
        int win = r * 2 + 1;
        for (int y = 0; y < h; y++) {
            int rs = 0, gs = 0, bs = 0;
            int row = y * w;
            for (int i = -r; i <= r; i++) {
                int c = px[row + clamp(i, 0, w - 1)];
                rs += (c >> 16) & 0xFF;
                gs += (c >> 8) & 0xFF;
                bs += c & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                tmp[row + x] = 0xFF000000 | ((rs / win) << 16) | ((gs / win) << 8) | (bs / win);
                int c1 = px[row + clamp(x - r, 0, w - 1)];
                int c2 = px[row + clamp(x + r + 1, 0, w - 1)];
                rs += ((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF);
                gs += ((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF);
                bs += (c2 & 0xFF) - (c1 & 0xFF);
            }
        }
        for (int x = 0; x < w; x++) {
            int rs = 0, gs = 0, bs = 0;
            for (int i = -r; i <= r; i++) {
                int c = tmp[clamp(i, 0, h - 1) * w + x];
                rs += (c >> 16) & 0xFF;
                gs += (c >> 8) & 0xFF;
                bs += c & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                px[y * w + x] = 0xFF000000 | ((rs / win) << 16) | ((gs / win) << 8) | (bs / win);
                int c1 = tmp[clamp(y - r, 0, h - 1) * w + x];
                int c2 = tmp[clamp(y + r + 1, 0, h - 1) * w + x];
                rs += ((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF);
                gs += ((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF);
                bs += (c2 & 0xFF) - (c1 & 0xFF);
            }
        }
    }

    private static int clamp(int v, int a, int b) {
        return v < a ? a : (Math.min(v, b));
    }
}
