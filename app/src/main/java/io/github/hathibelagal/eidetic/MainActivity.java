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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

    private static final int WIN = 1;
    private static final int LOSE = 0;
    private final int MAX_VALUE = 9;
    private final int nRows = 6;
    private final int nCols = 3;
    private final ArrayList<Button> buttons = new ArrayList<>(9);
    private final List<Integer> sequence = IntStream.range(1, 10).boxed().collect(Collectors.toList());

    private boolean gameStarted = false;
    private int expectedNumber = 1;
    private GridLayout grid;
    private View gridContainer;

    private long startTime = 0;

    private SavedData data;

    private Speaker speaker;

    private String additionalSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        data = new SavedData(this);
        grid = findViewById(R.id.grid);
        gridContainer = findViewById(R.id.grid_container);

        speaker = new Speaker(this, data);

        resetGrid();
    }

    private void resetGrid() {
        invalidateOptionsMenu();
        gridContainer.setBackgroundResource(R.drawable.game_background);
        startTime = new Date().getTime();
        grid.removeAllViews();
        buttons.clear();
        generateSequence();
        createButtons();
        expectedNumber = 1;
        gameStarted = false;
        additionalSpeech = "";
    }

    private void generateSequence() {
        Collections.shuffle(sequence);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createButtons() {
        int k = 0;
        List<Point> taken = new ArrayList<>();
        while (true) {
            for (int i = 0; i < nRows; i++) {
                COLS:
                for (int j = 0; j < nCols; j++) {
                    if (k >= MAX_VALUE) {
                        return;
                    }
                    if (Math.random() > 0.5) {
                        continue;
                    }
                    for (Point p : taken) {
                        if (p.x == i && p.y == j) {
                            continue COLS;
                        }
                    }
                    taken.add(new Point(i, j));
                    NumberButton b = new NumberButton(MainActivity.this);
                    b.setText(getMappedString(sequence.get(k)));
                    b.setValue(sequence.get(k));

                    GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
                    gridParams.height = 0;
                    gridParams.width = 0;
                    gridParams.rowSpec = GridLayout.spec(i, 1f);
                    gridParams.columnSpec = GridLayout.spec(j, 1f);

                    b.setLayoutParams(gridParams);
                    b.setOnTouchListener((view, motionEvent) -> {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            if (!gameStarted && b.getValue() != expectedNumber) {
                                speaker.playErrorTone();
                                Toast.makeText(MainActivity.this, "Please start with 1", Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            if (b.getValue() == expectedNumber) {
                                b.setOnTouchListener(null);
                                if (!gameStarted) {
                                    gameStarted = true;
                                    MainActivity.this.activatePuzzleMode();
                                }
                                expectedNumber += 1;
                                speaker.playTone(b.getValue(), false);
                                b.setVisibility(View.INVISIBLE);

                                if (expectedNumber == MAX_VALUE + 1) {
                                    speaker.playTone(ToneGenerator.TONE_DTMF_A, true);
                                    MainActivity.this.showRestart(WIN);
                                }
                            } else {
                                speaker.playTone(ToneGenerator.TONE_DTMF_0, true);
                                if (data.getStarsAvailable() > 0) {
                                    data.decrementStarsAvailable();
                                    invalidateOptionsMenu();
                                } else {
                                    data.resetStreak();
                                    data.resetStars();
                                    MainActivity.this.showRestart(LOSE);
                                }
                            }
                        }
                        return true;
                    });
                    buttons.add(b);
                    grid.addView(b);

                    k++;
                }
            }
        }
    }

    private CharSequence getMappedString(int i) {
        return LangUtils.getTranslation(data.getLanguage(), i);
    }


    private void showRestart(int status) {
        gridContainer.setBackgroundResource(status == WIN ?
                R.drawable.win_background : R.drawable.lose_background);
        int timeTaken = (int) TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - startTime);
        boolean createdRecord = false;
        int previousRecord = data.getFastestTime();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (status == WIN) {
            data.incrementStreak();
            createdRecord = data.updateFastestTime(timeTaken);
            updateAdditionalSpeech(createdRecord, timeTaken);
            builder.setNeutralButton("Speak 👄", null);
        }

        data.updateStats(status == WIN);

        builder.setMessage(status == WIN ? String.format(Locale.ENGLISH, getString(createdRecord ? R.string.success_message_record : R.string.success_message), timeTaken, previousRecord) : getString(R.string.game_over_message));
        builder.setTitle(status == WIN ? String.format(Locale.ENGLISH, "🤩 You win!\n🙌 Streak: %d", data.getStreak()) : "😖 Game over!");
        builder.setPositiveButton("Yes", (dialogInterface, i) -> resetGrid());
        builder.setNegativeButton("No", (dialogInterface, i) -> finish());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button info = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (!data.areSoundsOn()) {
                info.setEnabled(false);
            }
            info.setOnClickListener(view -> speaker.say(String.format(Locale.ENGLISH, getString(R.string.tts_time_taken), timeTaken) + ". " + additionalSpeech));
        });
        dialog.show();
    }

    private void updateAdditionalSpeech(boolean createdRecord, int timeTaken) {
        if (createdRecord) {
            additionalSpeech += getString(R.string.tts_new_record);
        }

        int streak = data.getStreak();
        switch (streak) {
            case 5:
                additionalSpeech += getString(R.string.tts_streak5);
                break;
            case 10:
                additionalSpeech += getString(R.string.tts_streak10);
                break;
            case 25:
                additionalSpeech += getString(R.string.tts_streak25);
                break;
        }

        if (timeTaken == 5) {
            additionalSpeech += getString(R.string.tts_so_fast);
        } else if (timeTaken < 5) {
            additionalSpeech += getString(R.string.tts_super_fast);
        }
    }

    private void activatePuzzleMode() {
        for (Button b : buttons) {
            b.setText("?");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_menu, menu);
        int nStars = data.getStarsAvailable();

        menu.findItem(R.id.menu_star_1).setVisible(nStars >= 2);
        menu.findItem(R.id.menu_star_2).setVisible(nStars >= 1);

        boolean sfx = data.areSoundsOn();
        menu.findItem(R.id.menu_sfx).setTitle(sfx ? getString(R.string.menu_sfx_off_title) : getString(R.string.menu_sfx_on_title));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_reload) {
            data.resetStreak();
            data.resetStars();
            resetGrid();
        } else if (item.getItemId() == R.id.menu_language) {
            showChangeLanguageDialog();
        } else if (item.getItemId() == R.id.menu_stats) {
            showStatsDialog();
        } else if (item.getItemId() == R.id.menu_sfx) {
            data.toggleSounds();
            invalidateOptionsMenu();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChangeLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Change language to...");
        builder.setItems(new CharSequence[]{"English", "Hindi", "Japanese", "Khmer"}, (dialogInterface, i) -> {
            data.setLanguage(i);
            if (!gameStarted) {
                for (Button b : buttons) {
                    int v = ((NumberButton) b).getValue();
                    b.setText(getMappedString(v));
                }
            }
        });
        builder.create().show();
    }

    private void showStatsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Your stats");
        builder.setPositiveButton("Close", null);
        Map<String, Integer> stats = data.getStats();
        List<CharSequence> statItems = new ArrayList<>();
        Integer nGames = stats.get("N_GAMES");
        Integer nWins = stats.get("N_WON");
        if (nGames == null || nWins == null) {
            return;
        }
        Float winRate = (float) (100.0 * nWins / nGames);
        statItems.add(String.format(Locale.ENGLISH, "Total games played: %d", nGames));
        statItems.add(String.format(Locale.ENGLISH, "Number of wins: %d", nWins));
        statItems.add(String.format(Locale.ENGLISH, "Win rate: %.2f %%", winRate));
        builder.setItems(statItems.toArray(new CharSequence[0]), null);
        builder.create().show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration c = new Configuration(newBase.getResources().getConfiguration());
        c.fontScale = 1.0f;
        applyOverrideConfiguration(c);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onDestroy() {
        if (speaker != null) {
            speaker.releaseResources();
        }
        super.onDestroy();
    }
}