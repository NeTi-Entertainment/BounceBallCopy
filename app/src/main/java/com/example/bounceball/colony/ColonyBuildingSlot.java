package com.example.bounceball.colony;

public class ColonyBuildingSlot {

    public static final long NO_UPGRADE = -1L;

    public final int           slotIndex;
    public final ColonyBuilding type;

    private int  level;
    private long upgradeStartTime;

    public ColonyBuildingSlot(int slotIndex, ColonyBuilding type, int level, long upgradeStartTime) {
        this.slotIndex         = slotIndex;
        this.type              = type;
        this.level             = level;
        this.upgradeStartTime  = upgradeStartTime;
    }

    public int getLevel() { return level; }

    public boolean isBuilt() { return level > 0; }

    public boolean isMaxLevel() { return level >= ColonyBuilding.MAX_LEVEL; }

    public boolean isUpgrading() { return upgradeStartTime != NO_UPGRADE; }

    public long getUpgradeStartTime() { return upgradeStartTime; }

    public int getNextLevel() { return level + 1; }

    public long getUpgradeDurationMillis() {
        return type.getDurationSecondsForLevel(getNextLevel()) * 1000L;
    }

    public long getUpgradeEndTime() {
        if (!isUpgrading()) return NO_UPGRADE;
        return upgradeStartTime + getUpgradeDurationMillis();
    }

    public long getRemainingMillis() {
        if (!isUpgrading()) return 0L;
        return Math.max(0L, getUpgradeEndTime() - System.currentTimeMillis());
    }

    public boolean isUpgradeComplete() {
        return isUpgrading() && System.currentTimeMillis() >= getUpgradeEndTime();
    }

    void applyLevelUp() {
        level++;
        upgradeStartTime = NO_UPGRADE;
    }

    void beginUpgrade() {
        upgradeStartTime = System.currentTimeMillis();
    }

    void cancelUpgrade() {
        upgradeStartTime = NO_UPGRADE;
    }
}