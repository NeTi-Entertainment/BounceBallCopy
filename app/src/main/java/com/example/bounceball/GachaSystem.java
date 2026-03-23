package com.example.bounceball;

import android.graphics.Color;
import com.example.bounceball.utils.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GachaSystem {

    public static final int COST_GOLD      = 1;
    public static final int COST_DIAMONDS  = 50;

    public static final int FRAGS_COMMON    =  30;
    public static final int FRAGS_RARE      =  90;
    public static final int FRAGS_LEGENDARY = 150;

    public static final int POOL_COMMON    = 6;
    public static final int POOL_RARE      = 3;
    public static final int POOL_LEGENDARY = 1;
    public static final int POOL_SIZE      = POOL_COMMON + POOL_RARE + POOL_LEGENDARY;

    private static final float W_COMMON    = 12.0f;
    private static final float W_RARE      =  6.5f;
    private static final float W_LEGENDARY =  8.5f;

    private static final Random RNG = new Random();

    public static class SkinEntry {
        public final String id;
        public final String name;
        public final String colorHex;
        public final String rarity;

        public SkinEntry(String id, String name, String colorHex, String rarity) {
            this.id       = id;
            this.name     = name;
            this.colorHex = colorHex;
            this.rarity   = rarity;
        }
    }

    public static SkinEntry[] buildPool(List<SkinEntry> allSkins) {
        List<SkinEntry> commons     = filter(allSkins, "common");
        List<SkinEntry> rares       = filter(allSkins, "rare");
        List<SkinEntry> legendaries = filter(allSkins, "legendary");

        Collections.shuffle(commons,     RNG);
        Collections.shuffle(rares,       RNG);
        Collections.shuffle(legendaries, RNG);

        SkinEntry[] pool = new SkinEntry[POOL_SIZE];
        for (int i = 0; i < POOL_COMMON;    i++)
            pool[i]                          = pick(commons,     i);
        for (int i = 0; i < POOL_RARE;      i++)
            pool[POOL_COMMON + i]            = pick(rares,       i);
        for (int i = 0; i < POOL_LEGENDARY; i++)
            pool[POOL_COMMON + POOL_RARE + i] = pick(legendaries, i);

        return pool;
    }

    public static int spin(SkinEntry[] pool) {
        float[] weights = new float[POOL_SIZE];
        for (int i = 0; i < POOL_COMMON;    i++)
            weights[i]                           = W_COMMON;
        for (int i = 0; i < POOL_RARE;      i++)
            weights[POOL_COMMON + i]             = W_RARE;
        for (int i = 0; i < POOL_LEGENDARY; i++)
            weights[POOL_COMMON + POOL_RARE + i] = W_LEGENDARY;

        float total = 0f;
        for (float w : weights) total += w;

        float r = RNG.nextFloat() * total;
        float cumul = 0f;
        for (int i = 0; i < POOL_SIZE; i++) {
            cumul += weights[i];
            if (r < cumul) return i;
        }
        return POOL_SIZE - 1;
    }

    private static List<SkinEntry> filter(List<SkinEntry> all, String rarity) {
        List<SkinEntry> out = new ArrayList<>();
        for (SkinEntry s : all) if (rarity.equals(s.rarity)) out.add(s);
        return out;
    }

    private static SkinEntry pick(List<SkinEntry> list, int idx) {
        if (list.isEmpty())
            return new SkinEntry("ball_basic", Strings.get("gacha.fallback_name"), "#CCCCCC", "common");
        return list.get(idx % list.size());
    }

    public static int getFragmentThreshold(String rarity) {
        if (rarity == null) return Integer.MAX_VALUE;
        switch (rarity) {
            case "rare":      return FRAGS_RARE;
            case "legendary": return FRAGS_LEGENDARY;
            default:          return FRAGS_COMMON;
        }
    }

    public static String getRarityLabel(String rarity) {
        if (rarity == null) return "";
        switch (rarity) {
            case "rare":      return Strings.get("gacha.rarity_rare");
            case "legendary": return Strings.get("gacha.rarity_legendary");
            default:          return Strings.get("gacha.rarity_common");
        }
    }

    public static int getRarityColor(String rarity) {
        if (rarity == null) return Color.parseColor("#AAAAAA");
        switch (rarity) {
            case "rare":      return Color.parseColor("#5B9BD5");
            case "legendary": return Color.parseColor("#FFD700");
            default:          return Color.parseColor("#AAAAAA");
        }
    }

    public static int getRarityBgColor(String rarity) {
        if (rarity == null) return Color.parseColor("#1A2A3A");
        switch (rarity) {
            case "rare":      return Color.parseColor("#0D1E30");
            case "legendary": return Color.parseColor("#2A1E00");
            default:          return Color.parseColor("#1A2A3A");
        }
    }
}