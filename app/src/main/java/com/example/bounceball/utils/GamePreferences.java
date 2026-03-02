package com.example.bounceball.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralise toutes les préférences et la sauvegarde du jeu.
 *
 * Données sauvegardées :
 *  - or (gold)
 *  - diamants (diamonds)
 *  - record d'ascension (max_height)
 *  - upgrades (upg_*)
 *  - son activé/désactivé (sound_enabled)
 *  - langue (language)
 *  - nombre de parties jouées pour les pubs (game_count_ads)
 */
public class GamePreferences {
    private static final String PREF_NAME = "TrampolineGamePrefs";
    private SharedPreferences prefs;

    public GamePreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────────
    // OR
    // ─────────────────────────────────────────
    public int getGold() { return prefs.getInt("gold", 0); }

    public void addGold(int amount) {
        prefs.edit().putInt("gold", getGold() + amount).commit();
    }

    public void spendGold(int amount) {
        prefs.edit().putInt("gold", Math.max(0, getGold() - amount)).commit();
    }

    // ─────────────────────────────────────────
    // DIAMANTS
    // ─────────────────────────────────────────
    public int getDiamonds() { return prefs.getInt("diamonds", 0); }

    public void addDiamonds(int amount) {
        prefs.edit().putInt("diamonds", getDiamonds() + amount).apply();
    }

    public void spendDiamonds(int amount) {
        prefs.edit().putInt("diamonds", Math.max(0, getDiamonds() - amount)).apply();
    }

    // ─────────────────────────────────────────
    // RECORD D'ASCENSION (sauvegarde persistante)
    // ─────────────────────────────────────────
    public float getMaxHeight() { return prefs.getFloat("max_height", 0f); }

    /**
     * Met à jour le record si la nouvelle hauteur est supérieure.
     * @return true si un nouveau record est établi
     */
    public boolean updateMaxHeight(float h) {
        if (h > getMaxHeight()) {
            prefs.edit().putFloat("max_height", h).apply();
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────
    // SON
    // ─────────────────────────────────────────
    public boolean isSoundEnabled() { return prefs.getBoolean("sound_enabled", true); }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply();
    }

    // ─────────────────────────────────────────
    // LANGUE
    // ─────────────────────────────────────────
    /** Retourne le code langue (ex: "fr", "en"). Par défaut : "fr". */
    public String getLanguage() { return prefs.getString("language", "fr"); }

    public void setLanguage(String langCode) {
        prefs.edit().putString("language", langCode).apply();
    }

    // ─────────────────────────────────────────
    // COMPTEUR DE PARTIES (pour les pubs)
    // ─────────────────────────────────────────
    public int getAdGameCount() { return prefs.getInt("game_count_ads", 0); }

    public void incrementAdGameCount() {
        prefs.edit().putInt("game_count_ads", getAdGameCount() + 1).apply();
    }

    public void resetAdGameCount() {
        prefs.edit().putInt("game_count_ads", 0).apply();
    }

    // ─────────────────────────────────────────
    // ACCÈS BRUT (pour UpgradeStats etc.)
    // ─────────────────────────────────────────
    public SharedPreferences getRaw() { return prefs; }
}