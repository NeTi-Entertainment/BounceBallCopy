package com.example.bounceball.colony;

import android.content.SharedPreferences;
import com.example.bounceball.utils.GamePreferences;

public class ColonyManager {

    private static final ColonyBuilding[] SLOT_LAYOUT = {
            ColonyBuilding.HOUSE,
            ColonyBuilding.OXYGEN,
            ColonyBuilding.WATER,
            ColonyBuilding.FOOD,
            ColonyBuilding.DEFENSE
    };

    public static final int SLOT_COUNT = SLOT_LAYOUT.length;

    // ─── Clés SharedPreferences ───────────────────────────
    private static String keyLevel(int slotIndex) {
        return "colony_slot_" + slotIndex + "_level";
    }

    private static String keyUpgradeStart(int slotIndex) {
        return "colony_slot_" + slotIndex + "_upgrade_start";
    }

    // ─── Chargement ───────────────────────────────────────
    public static ColonyBuildingSlot[] loadSlots(GamePreferences prefs) {
        SharedPreferences raw = prefs.getRaw();
        ColonyBuildingSlot[] slots = new ColonyBuildingSlot[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            int  level             = raw.getInt(keyLevel(i), 0);
            long upgradeStartTime  = raw.getLong(keyUpgradeStart(i), ColonyBuildingSlot.NO_UPGRADE);
            slots[i] = new ColonyBuildingSlot(i, SLOT_LAYOUT[i], level, upgradeStartTime);
        }
        return slots;
    }

    private static void saveSlot(GamePreferences prefs, ColonyBuildingSlot slot) {
        prefs.getRaw().edit()
                .putInt( keyLevel(slot.slotIndex),         slot.getLevel())
                .putLong(keyUpgradeStart(slot.slotIndex),  slot.getUpgradeStartTime())
                .apply();
    }

    // ─── Upgrade — démarrage ──────────────────────────────
    public static boolean canStartUpgrade(ColonyBuildingSlot slot, GamePreferences prefs) {
        if (slot.isMaxLevel())  return false;
        if (slot.isUpgrading()) return false;
        int next = slot.getNextLevel();
        int goldNeeded  = slot.type.getGoldCostForLevel(next);
        int metalNeeded = slot.type.getMetalCostForLevel(next);
        return prefs.getGold() >= goldNeeded && prefs.getRareMetal() >= metalNeeded;
    }

    public static boolean startUpgrade(ColonyBuildingSlot slot, GamePreferences prefs) {
        if (!canStartUpgrade(slot, prefs)) return false;
        int next = slot.getNextLevel();
        prefs.spendGold(       slot.type.getGoldCostForLevel(next));
        prefs.spendRareMetal(  slot.type.getMetalCostForLevel(next));
        slot.beginUpgrade();
        saveSlot(prefs, slot);
        return true;
    }

    // ─── Upgrade — vérification et finalisation ───────────
    public static boolean checkAndComplete(ColonyBuildingSlot slot, GamePreferences prefs) {
        if (!slot.isUpgradeComplete()) return false;
        slot.applyLevelUp();
        saveSlot(prefs, slot);
        return true;
    }

    public static void checkAndCompleteAll(ColonyBuildingSlot[] slots, GamePreferences prefs) {
        for (ColonyBuildingSlot slot : slots) {
            checkAndComplete(slot, prefs);
        }
    }

    // ─── Stats globales ───────────────────────────────────
    public static ColonyStats computeStats(ColonyBuildingSlot[] slots, int alienCount) {
        int population    = 0;
        int oxygen        = 0;
        int water         = 0;
        int food          = 0;
        int defense       = 0;
        int builtCount    = 0;

        for (ColonyBuildingSlot slot : slots) {
            if (!slot.isBuilt()) continue;
            builtCount++;
            int stat = slot.type.getStatAtLevel(slot.getLevel());
            switch (slot.type) {
                case HOUSE:   population += stat; break;
                case OXYGEN:  oxygen     += stat; break;
                case WATER:   water      += stat; break;
                case FOOD:    food       += stat; break;
                case DEFENSE: defense    += stat; break;
                default: break;
            }
        }

        int effectivePopulation = Math.min(alienCount, population);
        return new ColonyStats(population, oxygen, water, food, defense,
                effectivePopulation, builtCount);
    }

    // ─── Peut-on collecter un alien supplémentaire ? ──────
    public static boolean canCollectAlien(ColonyBuildingSlot[] slots, int alienCount) {
        ColonyStats s = computeStats(slots, alienCount);
        if (alienCount >= s.populationCapacity) return false;
        int needed = alienCount + 1;
        return s.oxygenCapacity >= needed
                && s.waterCapacity  >= needed
                && s.foodCapacity   >= needed;
    }

    // ─── Colonie complète ? ───────────────────────────────
    public static boolean isComplete(ColonyBuildingSlot[] slots, int alienCount) {
        for (ColonyBuildingSlot slot : slots) {
            if (!slot.isMaxLevel()) return false;
        }
        ColonyStats s = computeStats(slots, alienCount);
        return alienCount >= s.populationCapacity
                && s.oxygenCapacity  >= alienCount
                && s.waterCapacity   >= alienCount
                && s.foodCapacity    >= alienCount;
    }

    // ─── Données de slot layout (pour l'UI) ───────────────
    public static ColonyBuilding getBuildingTypeForSlot(int slotIndex) {
        return SLOT_LAYOUT[slotIndex];
    }

    // ─── Stats snapshot ───────────────────────────────────
    public static class ColonyStats {
        public final int populationCapacity;
        public final int oxygenCapacity;
        public final int waterCapacity;
        public final int foodCapacity;
        public final int defenseRating;
        public final int effectivePopulation;
        public final int builtCount;

        ColonyStats(int populationCapacity, int oxygenCapacity, int waterCapacity,
                    int foodCapacity, int defenseRating,
                    int effectivePopulation, int builtCount) {
            this.populationCapacity = populationCapacity;
            this.oxygenCapacity     = oxygenCapacity;
            this.waterCapacity      = waterCapacity;
            this.foodCapacity       = foodCapacity;
            this.defenseRating      = defenseRating;
            this.effectivePopulation = effectivePopulation;
            this.builtCount         = builtCount;
        }
    }
}