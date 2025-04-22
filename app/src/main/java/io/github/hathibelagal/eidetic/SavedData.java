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
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class SavedData {

    private final int MAX_STARS = 2;
    SharedPreferences prefs;

    SavedData(Activity context) {
        prefs = context.getPreferences(Context.MODE_PRIVATE);
    }

    void incrementStreak() {
        int currentStreak = prefs.getInt("STREAK", 0) + 1;
        prefs.edit().putInt("STREAK", currentStreak).apply();
        if(currentStreak >= 5 && currentStreak % 5 == 0) {
            incrementStarsAvailable();
        }
    }

    boolean areSoundsOn() {
        return prefs.getBoolean("SFX", true);
    }

    @SuppressLint("ApplySharedPref")
    void toggleSounds() {
        prefs.edit().putBoolean("SFX", !areSoundsOn()).commit();
    }

    boolean isHardModeOn() {
        return prefs.getBoolean("HARD_MODE", false);
    }

    void toggleDifficulty() {
        prefs.edit().putBoolean("HARD_MODE", !isHardModeOn()).apply();
    }

    void updateStats(boolean won) {
        int nGames = prefs.getInt("N_GAMES", 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("N_GAMES", nGames);
        if (won) {
            int nWon = prefs.getInt("N_WON", 0) + 1;
            editor.putInt("N_WON", nWon);
        }
        editor.apply();
    }

    Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("N_GAMES", prefs.getInt("N_GAMES", 0));
        stats.put("N_WON", prefs.getInt("N_WON", 0));
        return stats;
    }

    int getLanguage() {
        return prefs.getInt("LANGUAGE", 0);
    }

    void setLanguage(int l) {
        prefs.edit().putInt("LANGUAGE", l).apply();
    }

    void resetStreak() {
        prefs.edit().remove("STREAK").apply();
    }

    int getStreak() {
        return prefs.getInt("STREAK", 0);
    }

    boolean updateFastestTime(int time) {
        int oldFastestTime = prefs.getInt("TIME", Integer.MAX_VALUE);
        if (time < oldFastestTime) {
            prefs.edit().putInt("TIME", time).apply();
            return true;
        }
        return false;
    }

    int getFastestTime() {
        return prefs.getInt("TIME", 0);
    }

    int getStarsAvailable() {
        return prefs.getInt("STARS", MAX_STARS);
    }

    @SuppressLint("ApplySharedPref")
    void resetStars() {
        if(isHardModeOn()) {
            prefs.edit().putInt("STARS", 0).commit();
        } else {
            prefs.edit().putInt("STARS", MAX_STARS).commit();
        }
    }

    @SuppressLint("ApplySharedPref")
    void decrementStarsAvailable() {
        prefs.edit().putInt("STARS", getStarsAvailable() - 1).commit();
    }
    @SuppressLint("ApplySharedPref")
    void incrementStarsAvailable() {
        if (getStarsAvailable() >= MAX_STARS) {
            return;
        }
        prefs.edit().putInt("STARS", getStarsAvailable() + 1).commit();
    }
}
