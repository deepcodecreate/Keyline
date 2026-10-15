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

package com.keyline.ime.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.keyline.ime.R;

import java.util.ArrayList;
import java.util.Locale;

public final class VoiceController {
    public interface Host {
        void onPartial(String text);
        void onFinal(String text);
        void onError(String message);
        void onListening(boolean listening);
    }

    private final Context context;
    private final Host host;
    private SpeechRecognizer recognizer;
    private boolean listening;

    public VoiceController(Context context, Host host) {
        this.context = context.getApplicationContext();
        this.host = host;
    }

    public boolean isListening() { return listening; }

    public void toggle(String localeTag) {
        if (listening) stop();
        else start(localeTag);
    }

    public void start(String localeTag) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            host.onError(context.getString(R.string.voice_unavailable));
            return;
        }
        ensure();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        String tag = (localeTag == null || localeTag.isEmpty()) ? Locale.getDefault().toLanguageTag() : localeTag;
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag);
        listening = true;
        host.onListening(true);
        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            host.onListening(false);
            host.onError(context.getString(R.string.voice_error));
        }
    }

    public void stop() {
        listening = false;
        host.onListening(false);
        if (recognizer != null) {
            try { recognizer.stopListening(); } catch (Exception ignored) {}
        }
    }

    public void destroy() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    private void ensure() {
        if (recognizer != null) return;
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override public void onError(int error) {
                listening = false;
                host.onListening(false);
                int msg;
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) msg = R.string.voice_no_permission;
                else if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) msg = R.string.voice_no_match;
                else if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) msg = R.string.voice_network;
                else msg = R.string.voice_error;
                host.onError(context.getString(msg));
            }

            @Override public void onResults(Bundle results) {
                listening = false;
                host.onListening(false);
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) host.onFinal(list.get(0));
                else host.onError(context.getString(R.string.voice_no_match));
            }

            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> list = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) host.onPartial(list.get(0));
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }
}
