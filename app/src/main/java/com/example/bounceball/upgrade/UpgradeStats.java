package com.example.bounceball.upgrade;

public class UpgradeStats {
    // Ball
    public int airResistance = 0;  // 0-10: reduces drag
    public int weight = 0;         // 0-5: affects physics

    // Trampoline
    public int elasticity = 0;     // 0-10: spring force bonus

    // Boost
    public int boostLevel = 0;     // 0-5: boost power
    public int boostRecharge = 0;  // 0-5: faster recharge

    // Ink
    public int inkReserve = 5;     // 0-10: more ink per game
    public int inkEfficiency = 0;  // 0-10: less ink consumed per px

    // Gold
    public float goldMultiplier = 1f; // stacked from upgrades

    // Warp
    public int warpLevel = 0;      // 0 = disabled, 1-5 = distance

    public static UpgradeStats fromPrefs(android.content.SharedPreferences prefs) {
        UpgradeStats s = new UpgradeStats();
        s.airResistance = prefs.getInt("upg_air", 0);
        s.weight = prefs.getInt("upg_weight", 0);
        s.elasticity = prefs.getInt("upg_elastic", 0);
        s.boostLevel = prefs.getInt("upg_boost", 0);
        s.boostRecharge = prefs.getInt("upg_boost_recharge", 0);
        s.inkReserve = prefs.getInt("upg_ink_reserve", 0);
        s.inkEfficiency = prefs.getInt("upg_ink_eff", 0);
        s.warpLevel = prefs.getInt("upg_warp", 0);
        // Gold multiplier: stacked (1 + 0.01 per purchase, upgradable many times)
        int goldUpg = prefs.getInt("upg_gold_mult", 0);
        s.goldMultiplier = (float) Math.pow(1.01f, goldUpg);
        return s;
    }

    public void saveToPrefs(android.content.SharedPreferences.Editor editor) {
        editor.putInt("upg_air", airResistance);
        editor.putInt("upg_weight", weight);
        editor.putInt("upg_elastic", elasticity);
        editor.putInt("upg_boost", boostLevel);
        editor.putInt("upg_boost_recharge", boostRecharge);
        editor.putInt("upg_ink_reserve", inkReserve);
        editor.putInt("upg_ink_eff", inkEfficiency);
        editor.putInt("upg_warp", warpLevel);
    }
}
