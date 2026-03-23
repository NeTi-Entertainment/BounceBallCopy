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
    public String getLanguage() { return prefs.getString("language", "en"); }

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

    // ─────────────────────────────────────────
    // FRAGMENTS (gacha)
    // Clé stockée : "frag_<skinId>"
    // ─────────────────────────────────────────
    public int getFragments(String skinId) {
        return prefs.getInt("frag_" + skinId, 0);
    }

    public void addFragments(String skinId, int amount) {
        prefs.edit().putInt("frag_" + skinId, getFragments(skinId) + amount).apply();
    }

    public void spendFragments(String skinId, int amount) {
        prefs.edit().putInt("frag_" + skinId, Math.max(0, getFragments(skinId) - amount)).apply();
    }

    // ─────────────────────────────────────────
    // ÉCLOSION
    // egg_hatched  : animation d'éclosion définitivement jouée
    // hatch_ready  : seuil 1000m atteint, animation pas encore jouée
    // ─────────────────────────────────────────
    public boolean hasHatched() { return prefs.getBoolean("egg_hatched", false); }

    public void setHatched(boolean v) { prefs.edit().putBoolean("egg_hatched", v).apply(); }

    public boolean isHatchReady() { return prefs.getBoolean("hatch_ready", false); }

    public void setHatchReady(boolean v) { prefs.edit().putBoolean("hatch_ready", v).apply(); }

    // ─────────────────────────────────────────
    // MÉTAL RARE (débloqué après éclosion)
    // ─────────────────────────────────────────
    public int getRareMetal() { return prefs.getInt("rare_metal", 0); }

    public void addRareMetal(int amount) {
        prefs.edit().putInt("rare_metal", getRareMetal() + amount).apply();
    }

    public void spendRareMetal(int amount) {
        prefs.edit().putInt("rare_metal", Math.max(0, getRareMetal() - amount)).apply();
    }

    // ─────────────────────────────────────────
    // ALIENS COLLECTÉS (colons de la base)
    // ─────────────────────────────────────────
    public int getAlienCount() { return prefs.getInt("alien_count", 0); }

    public void addAlien(int amount) {
        prefs.edit().putInt("alien_count", getAlienCount() + amount).apply();
    }

    // ─────────────────────────────────────────
    // RESET TOTAL
    // ─────────────────────────────────────────
    public void resetAll() {
        prefs.edit().clear().apply();
    }
}