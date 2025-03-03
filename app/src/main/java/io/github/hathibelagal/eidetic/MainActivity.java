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
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

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
                    gridParams.setMargins(4, 0, 4, 0);

                    b.setLayoutParams(gridParams);
                    b.setPadding(0, 0, 0, 0);
                    b.setOnTouchListener(MainActivity.this);
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

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        // Get references to views
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button neutralButton = dialogView.findViewById(R.id.dialog_neutral_button);
        Button positiveButton = dialogView.findViewById(R.id.dialog_positive_button);
        Button negativeButton = dialogView.findViewById(R.id.dialog_negative_button);

        // Set content
        if (status == WIN) {
            data.incrementStreak();
            createdRecord = data.updateFastestTime(timeTaken);
            updateAdditionalSpeech(createdRecord, timeTaken);
            neutralButton.setVisibility(View.VISIBLE);
        }

        data.updateStats(status == WIN);

        titleView.setText(status == WIN ?
                String.format(Locale.ENGLISH, "🤩 You win!\n🙌 Streak: %d", data.getStreak()) :
                "😖 Game over!");
        messageView.setText(status == WIN ?
                String.format(Locale.ENGLISH, getString(createdRecord ? R.string.success_message_record :
                        R.string.success_message), timeTaken, previousRecord) :
                getString(R.string.game_over_message));

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetGrid();
        });
        negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        if (status == WIN) {
            if (!data.areSoundsOn()) {
                neutralButton.setEnabled(false);
            }
            neutralButton.setOnClickListener(v ->
                    speaker.say(String.format(Locale.ENGLISH, getString(R.string.tts_time_taken), timeTaken) +
                            ". " + additionalSpeech));
        }

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
        boolean hardMode = data.isHardModeOn();
        menu.findItem(R.id.menu_sfx).setTitle(sfx ? getString(R.string.menu_sfx_off_title) : getString(R.string.menu_sfx_on_title));
        menu.findItem(R.id.menu_difficulty).setTitle(!hardMode ? getString(R.string.hard_mode) : getString(R.string.easy_mode));

        return super.onCreateOptionsMenu(menu);
    }

    private void reloadGame() {
        data.resetStreak();
        data.resetStars();
        resetGrid();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_reload) {
            reloadGame();
        } else if (item.getItemId() == R.id.menu_language) {
            showChangeLanguageDialog();
        } else if (item.getItemId() == R.id.menu_stats) {
            showStatsDialog();
        } else if (item.getItemId() == R.id.menu_sfx) {
            data.toggleSounds();
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_difficulty) {
            data.toggleDifficulty();
            invalidateOptionsMenu();
            reloadGame();
            Toast.makeText(MainActivity.this, data.isHardModeOn() ? "HARD MODE" : "EASY MODE", Toast.LENGTH_SHORT).show();
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
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showStatsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        Button positiveButton = dialogView.findViewById(R.id.dialog_positive_button);
        Button negativeButton = dialogView.findViewById(R.id.dialog_negative_button);
        Button neutralButton = dialogView.findViewById(R.id.dialog_neutral_button);

        titleView.setText(R.string.your_stats);
        positiveButton.setText(R.string.close);
        negativeButton.setVisibility(View.GONE);
        neutralButton.setVisibility(View.GONE);

        Map<String, Integer> stats = data.getStats();
        Integer nGames = stats.get("N_GAMES");
        Integer nWins = stats.get("N_WON");
        if (nGames == null || nWins == null) {
            return;
        }
        Float winRate = (float) (100.0 * nWins / nGames);
        String statsText = String.format(Locale.ENGLISH,
                "Total games played: %d\nNumber of wins: %d\nWin rate: %.2f %%",
                nGames, nWins, winRate);
        messageView.setText(statsText);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        positiveButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        NumberButton b = (NumberButton) view;
        int value = b.getValue();
        if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (!gameStarted && value != expectedNumber) {
            speaker.playErrorTone();
            Toast.makeText(MainActivity.this, "Please start with 1", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (value == expectedNumber) {
            b.setOnTouchListener(null);
            if (!gameStarted) {
                gameStarted = true;
                MainActivity.this.activatePuzzleMode();
            }
            expectedNumber += 1;
            speaker.playTone(b.getValue(), false);

            b.setScaleX(0.90f);
            b.setScaleY(0.90f);
            b.setAlpha(0f);
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
        return true;
    }
}