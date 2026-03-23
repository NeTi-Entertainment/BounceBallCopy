package com.example.bounceball.colony;

import com.example.bounceball.utils.Strings;

public enum ColonyBuilding {

    HOUSE(
            "colony.buildings.house.name",
            "🏠",
            "colony.buildings.house.stat",
            new int[]{10, 18, 28, 40, 52, 64, 74, 82, 90, 98},
            new int[]{100,  250,  500,  900, 1500, 2500, 4000, 6000,  9000, 14000},
            new int[]{  0,    0,    0,    0,    0,    1,    2,    4,     6,    10},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    ),

    OXYGEN(
            "colony.buildings.oxygen.name",
            "💨",
            "colony.buildings.oxygen.stat",
            new int[]{8, 17, 28, 40, 54, 70, 88, 108, 130, 154},
            new int[]{120,  280,  550,  950, 1600, 2700, 4200, 6200,  9500, 15000},
            new int[]{  0,    0,    0,    0,    0,    1,    2,    4,     6,    10},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    ),

    WATER(
            "colony.buildings.water.name",
            "💧",
            "colony.buildings.water.stat",
            new int[]{8, 17, 28, 40, 54, 70, 88, 108, 130, 154},
            new int[]{120,  280,  550,  950, 1600, 2700, 4200, 6200,  9500, 15000},
            new int[]{  0,    0,    0,    0,    0,    1,    2,    4,     6,    10},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    ),

    FOOD(
            "colony.buildings.food.name",
            "🌾",
            "colony.buildings.food.stat",
            new int[]{8, 17, 28, 40, 54, 70, 88, 108, 130, 154},
            new int[]{110,  260,  520,  920, 1550, 2600, 4100, 6100,  9200, 14500},
            new int[]{  0,    0,    0,    0,    0,    1,    2,    4,     6,    10},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    ),

    DEFENSE(
            "colony.buildings.defense.name",
            "🛡",
            "colony.buildings.defense.stat",
            new int[]{5, 11, 18, 26, 35, 45, 56, 68, 81, 95},
            new int[]{150,  320,  620, 1050, 1750, 2900, 4500, 6800, 10000, 16000},
            new int[]{  0,    0,    0,    0,    0,    2,    3,    5,     8,    12},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    ),

    ENTERTAINMENT(
            "colony.buildings.entertainment.name",
            "🎉",
            "colony.buildings.entertainment.stat",
            new int[]{4, 9, 15, 22, 30, 39, 49, 60, 72, 85},
            new int[]{ 80,  200,  420,  800, 1400, 2400, 3800, 5800,  8800, 13500},
            new int[]{  0,    0,    0,    0,    0,    1,    2,    3,     5,     8},
            new long[]{ 5,   15,   45,  120,  300,  780, 2040, 5520,  7200, 43200}
    );

    public final String nameKey;
    public final String icon;
    public final String statKey;

    private final int[]  statPerLevel;
    private final int[]  goldCostPerLevel;
    private final int[]  metalCostPerLevel;
    private final long[] durationSecondsPerLevel;

    ColonyBuilding(String nameKey, String icon, String statKey,
                   int[] statPerLevel, int[] goldCostPerLevel,
                   int[] metalCostPerLevel, long[] durationSecondsPerLevel) {
        this.nameKey                 = nameKey;
        this.icon                    = icon;
        this.statKey                 = statKey;
        this.statPerLevel            = statPerLevel;
        this.goldCostPerLevel        = goldCostPerLevel;
        this.metalCostPerLevel       = metalCostPerLevel;
        this.durationSecondsPerLevel = durationSecondsPerLevel;
    }

    public String getDisplayName() { return Strings.get(nameKey); }
    public String getStatLabel()   { return Strings.get(statKey); }

    public static final int MAX_LEVEL = 10;

    public int getStatAtLevel(int level) {
        if (level <= 0) return 0;
        return statPerLevel[Math.min(level, MAX_LEVEL) - 1];
    }

    public int getGoldCostForLevel(int targetLevel) {
        if (targetLevel < 1 || targetLevel > MAX_LEVEL) return 0;
        return goldCostPerLevel[targetLevel - 1];
    }

    public int getMetalCostForLevel(int targetLevel) {
        if (targetLevel < 1 || targetLevel > MAX_LEVEL) return 0;
        return metalCostPerLevel[targetLevel - 1];
    }

    public long getDurationSecondsForLevel(int targetLevel) {
        if (targetLevel < 1 || targetLevel > MAX_LEVEL) return 0;
        return durationSecondsPerLevel[targetLevel - 1];
    }
}