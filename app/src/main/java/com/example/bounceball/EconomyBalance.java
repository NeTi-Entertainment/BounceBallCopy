package com.example.bounceball;

public final class EconomyBalance {

    public static final int GOLD_PER_DIAMOND = 1000;

    private static final int[] AIR_GOLD = {
            80, 120, 170, 240, 340, 480, 680, 950, 1330, 1850
    };
    private static final int[] ELASTIC_GOLD = {
            100, 150, 220, 320, 460, 660, 950, 1350, 1900, 2700
    };
    private static final int[] INK_RESERVE_GOLD = {
            120, 180, 260, 380, 550, 800, 1150, 1650, 2350, 3350
    };
    private static final int[] INK_EFF_GOLD = {
            150, 230, 340, 500, 740, 1100, 1600, 2350, 3400, 5000
    };
    private static final int[] BOOST_GOLD = {
            250, 450, 800, 1400, 2400
    };
    private static final int[] BOOST_RECHARGE_GOLD = {
            200, 360, 650, 1150, 2000
    };
    private static final int[] WARP_GOLD = {
            500, 900, 1600, 2800, 5000
    };

    private EconomyBalance() {}

    public static int upgradeGoldCost(String key, int level, int fallbackBase) {
        if (level < 0) level = 0;
        switch (key) {
            case "upg_air":
                return tableCost(AIR_GOLD, level, fallbackBase);
            case "upg_elastic":
                return tableCost(ELASTIC_GOLD, level, fallbackBase);
            case "upg_boost":
                return tableCost(BOOST_GOLD, level, fallbackBase);
            case "upg_boost_recharge":
                return tableCost(BOOST_RECHARGE_GOLD, level, fallbackBase);
            case "upg_ink_reserve":
                return tableCost(INK_RESERVE_GOLD, level, fallbackBase);
            case "upg_ink_eff":
                return tableCost(INK_EFF_GOLD, level, fallbackBase);
            case "upg_warp":
                return tableCost(WARP_GOLD, level, fallbackBase);
            case "upg_gold_mult":
                return Math.max(1, Math.round(250f * (float) Math.pow(1.028f, level)));
            default:
                return Math.max(1, fallbackBase * (level + 1));
        }
    }

    public static int upgradeDiamondCost(String key, int level, int fallbackBase) {
        int goldCost = upgradeGoldCost(key, level, fallbackBase);
        return Math.max(level + 2, Math.max(1, (goldCost + 249) / 250));
    }

    public static int cosmeticGoldCost(String id, int dataGold, int dataDiamonds, String rarity) {
        if (dataGold == 0 && dataDiamonds == 0) return 0;

        if (isGachaRarity(rarity)) {
            return baseGachaGoldCost(id, rarity) * gachaGoldMultiplier(rarity);
        }

        if (id != null && id.startsWith("bg_")) {
            if ("legendary".equals(rarity)) return 55000;
            if ("rare".equals(rarity)) return 22000;
            return 8000;
        }

        if (id != null && id.startsWith("ball_elem_")) return 40000;

        if (id != null && id.startsWith("ball_")) {
            return dataGold >= 80 ? 1200 : 800;
        }

        int existingGold = Math.max(dataGold, dataDiamonds * GOLD_PER_DIAMOND);
        return Math.max(0, existingGold);
    }

    public static int cosmeticDiamondCost(String id, int dataGold, int dataDiamonds, String rarity) {
        int goldCost = cosmeticGoldCost(id, dataGold, dataDiamonds, rarity);
        return goldCost == 0 ? 0 : Math.max(5, diamondCostFromGold(goldCost));
    }

    public static int diamondCostFromGold(int goldCost) {
        return Math.max(1, (goldCost + GOLD_PER_DIAMOND - 1) / GOLD_PER_DIAMOND);
    }

    private static int tableCost(int[] costs, int level, int fallbackBase) {
        if (level < costs.length) return costs[level];
        return Math.max(1, fallbackBase * (level + 1));
    }

    private static boolean isGachaRarity(String rarity) {
        return "common".equals(rarity) || "rare".equals(rarity) || "legendary".equals(rarity);
    }

    private static int baseGachaGoldCost(String id, String rarity) {
        if (id != null && id.startsWith("bg_")) {
            if ("legendary".equals(rarity)) return 55000;
            if ("rare".equals(rarity)) return 22000;
            return 8000;
        }

        if (id != null && id.startsWith("ball_elem_")) return 40000;

        if ("legendary".equals(rarity)) return 75000;
        if ("rare".equals(rarity)) return 30000;
        return 9000;
    }

    private static int gachaGoldMultiplier(String rarity) {
        if ("legendary".equals(rarity)) return 10;
        if ("rare".equals(rarity)) return 5;
        return 2;
    }
}
