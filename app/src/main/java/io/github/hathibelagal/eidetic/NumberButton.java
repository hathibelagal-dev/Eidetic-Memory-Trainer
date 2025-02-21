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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ToneGenerator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

public class NumberButton extends androidx.appcompat.widget.AppCompatButton {

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    private int value = -1;

    public NumberButton(Context context) {
        super(context);
        setTextSize(55f);
        setTextColor(Color.WHITE);
        setBackground(AppCompatResources.getDrawable(context, R.drawable.number_button_ready));

        setElevation(4f);
        setStateListAnimator(null);

        setShadowLayer(2f, 1f, 1f, Color.argb(128, 0, 0, 0));
    }

    public NumberButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
