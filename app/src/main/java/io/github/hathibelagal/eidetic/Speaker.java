/* Copyright 2024 Ashraff Hathibelagal
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

package io.github.hathibelagal.eidetic;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class Speaker implements TextToSpeech.OnInitListener {
    private final TextToSpeech tts;
    private final Activity context;
    private final int MAX_VOLUME = 100;
    private final ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, MAX_VOLUME);
    private final Bundle ttsParams = new Bundle();
    private final SavedData data;
    private boolean ttsReady = false;

    public Speaker(Activity context, SavedData data) {
        this.context = context;
        this.data = data;
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int i) {
        if (i == TextToSpeech.SUCCESS) {
            if (tts.setLanguage(Locale.ENGLISH) == TextToSpeech.LANG_AVAILABLE) {
                tts.setSpeechRate(1.2f);
                ttsReady = true;
                say(context.getString(R.string.tts_ready));
            }
        }
    }

    public void say(String s) {
        if (!data.areSoundsOn()) {
            return;
        }
        if (!ttsReady) {
            return;
        }
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, ttsParams, null);
    }

    void playTone(int tone, boolean isLong) {
        if (!data.areSoundsOn()) {
            return;
        }
        toneGenerator.stopTone();
        toneGenerator.startTone(tone, isLong ? 200 : 75);
    }

    void playErrorTone() {
        if (!data.areSoundsOn()) {
            return;
        }
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 30);
    }

    public void releaseResources() {
        tts.stop();
        tts.shutdown();
    }
}
