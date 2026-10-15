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

package com.keyline.ime;

import android.app.Application;

import androidx.emoji2.bundled.BundledEmojiCompatConfig;
import androidx.emoji2.text.EmojiCompat;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class KeylineApp extends Application {
    public static File crashFile(android.content.Context c) {
        return new File(c.getFilesDir(), "last_crash.txt");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("Thread: " + thread.getName());
                pw.println("Time: " + new java.util.Date());
                error.printStackTrace(pw);
                pw.flush();
                java.io.FileOutputStream os = new java.io.FileOutputStream(crashFile(this));
                os.write(sw.toString().getBytes("UTF-8"));
                os.close();
            } catch (Throwable ignored) {
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
        EmojiCompat.init(new BundledEmojiCompatConfig(this));
    }
}
