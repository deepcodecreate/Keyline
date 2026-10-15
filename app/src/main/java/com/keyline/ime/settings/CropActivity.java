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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.keyline.ime.R;
import com.keyline.ime.theme.BackgroundImageManager;
import com.keyline.ime.util.Bg;

import java.io.InputStream;

public class CropActivity extends PersianActivity {
    private CropView cropView;
    private MaterialButton saveButton;
    private Bitmap source;
    private int outW = 1080;
    private int outH = 400;

    private static final int MAX_DECODE_SIDE = 2400;
    private static final long MAX_DECODE_PIXELS = 8_000_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outW = getIntent().getIntExtra("outW", 1080);
        outH = getIntent().getIntExtra("outH", 400);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.addView(new SettingsUi(this).header(getString(R.string.crop_title), true,
                R.drawable.ic_close, this::finish),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        cropView = new CropView(this, null);
        root.addView(cropView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView hint = new TextView(this);
        hint.setText(R.string.crop_hint);
        hint.setTextSize(13);
        hint.setGravity(android.view.Gravity.CENTER);
        int horizontalPad = dp(16);
        hint.setPadding(horizontalPad, dp(4), horizontalPad, dp(4));
        root.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        saveButton = new MaterialButton(this);
        saveButton.setText(R.string.crop_save);
        saveButton.setEnabled(false);
        int m = dp(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(m, m, m, m);
        root.addView(saveButton, lp);
        setContentView(root);
        saveButton.setOnClickListener(v -> saveCrop());
        load(getIntent().getData());
    }

    private void load(Uri uri) {
        if (uri == null) {
            fail();
            return;
        }
        Bg.io(() -> {
            try {
                Bitmap raw = decodeImage(uri);
                if (raw == null) {
                    Bg.main(this::fail);
                    return;
                }
                Bitmap finalRaw = raw;
                Bg.main(() -> {
                    source = finalRaw;
                    cropView.setBitmap(source, outW / (float) Math.max(1, outH));
                    cropView.post(() -> {
                        if (!isFinishing() && saveButton != null) {
                            saveButton.setEnabled(cropView.isReady());
                        }
                    });
                });
            } catch (Exception e) {
                Bg.main(this::fail);
            }
        });
    }

    private Bitmap decodeImage(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sample = 1;
        while (bounds.outWidth / sample > MAX_DECODE_SIDE
                || bounds.outHeight / sample > MAX_DECODE_SIDE
                || ((long) bounds.outWidth / sample) * ((long) bounds.outHeight / sample) > MAX_DECODE_PIXELS) {
            if (sample >= 1 << 15) break;
            sample <<= 1;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap decoded;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            decoded = BitmapFactory.decodeStream(in, null, options);
        }
        if (decoded == null) return null;

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Exception ignored) {
        }
        Matrix transform = orientationMatrix(orientation);
        if (transform.isIdentity()) return decoded;
        Bitmap oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), transform, true);
        if (oriented != decoded) decoded.recycle();
        return oriented;
    }

    private Matrix orientationMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default:
                break;
        }
        return matrix;
    }

    private void saveCrop() {
        Bitmap cropped = cropView.export(outW, outH);
        if (cropped == null || source == null) {
            fail();
            return;
        }
        Bg.io(() -> {
            try {
                BackgroundImageManager.saveCropped(this, cropped);
                Bg.main(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                Bg.main(this::fail);
            } finally {
                if (!cropped.isRecycled()) cropped.recycle();
            }
        });
    }

    private void fail() {
        Toast.makeText(this, R.string.crop_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (source != null && !source.isRecycled()) source.recycle();
        source = null;
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
