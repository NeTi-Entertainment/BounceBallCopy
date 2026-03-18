package com.example.bounceball.colony;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.example.bounceball.utils.GamePreferences;

public class ColonyActivity extends Activity {

    private GamePreferences prefs;
    private ColonyBuildingSlot[] slots;
    private ColonyView colonyView;

    private TextView tvPop, tvO2, tvWater, tvFood, tvDefense;
    private TextView tvGold, tvMetal, tvAlien;

    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private Runnable tickerRunnable;
    private AlertDialog activeDialog;
    private CountDownTimer activeCountDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = new GamePreferences(this);
        slots = ColonyManager.loadSlots(prefs);
        ColonyManager.checkAndCompleteAll(slots, prefs);

        buildUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (slots != null) {
            ColonyManager.checkAndCompleteAll(slots, prefs);
            refreshStats();
            refreshResources();
            colonyView.setSlots(slots);
        }
        startTicker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTicker();
        if (activeCountDown != null) {
            activeCountDown.cancel();
            activeCountDown = null;
        }
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0A130A"));

        root.addView(buildStatBar());

        colonyView = new ColonyView(this, slots);
        colonyView.setOnSlotTappedListener(this::showSlotDialog);
        root.addView(colonyView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildBottomBar());

        setContentView(root);
        refreshStats();
    }

    // ── Ticker 1s — rafraîchit la vue pendant les constructions ──

    private void startTicker() {
        stopTicker();
        tickerRunnable = new Runnable() {
            @Override
            public void run() {
                boolean anyUpgrading = false;
                for (ColonyBuildingSlot slot : slots) {
                    if (slot.isUpgrading()) {
                        anyUpgrading = true;
                        if (ColonyManager.checkAndComplete(slot, prefs)) {
                            refreshStats();
                            refreshResources();
                        }
                    }
                }
                colonyView.setSlots(slots);
                if (anyUpgrading) {
                    tickerHandler.postDelayed(this, 1000L);
                }
            }
        };
        tickerHandler.postDelayed(tickerRunnable, 1000L);
    }

    private void stopTicker() {
        if (tickerRunnable != null) {
            tickerHandler.removeCallbacks(tickerRunnable);
            tickerRunnable = null;
        }
    }

    // ── Barre de stats colonie (haut) ──────────────────────

    private View buildStatBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.parseColor("#0D1A0D"));
        bar.setPadding(dpToPx(4), dpToPx(10), dpToPx(4), dpToPx(10));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        tvPop     = makeStatChip("👥", "0");
        tvO2      = makeStatChip("💨", "0");
        tvWater   = makeStatChip("💧", "0");
        tvFood    = makeStatChip("🌾", "0");
        tvDefense = makeStatChip("🛡", "0");

        tvPop.setLayoutParams(    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvO2.setLayoutParams(     new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvWater.setLayoutParams(  new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvFood.setLayoutParams(   new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvDefense.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(tvPop);
        bar.addView(tvO2);
        bar.addView(tvWater);
        bar.addView(tvFood);
        bar.addView(tvDefense);

        return bar;
    }

    private TextView makeStatChip(String icon, String value) {
        TextView tv = new TextView(this);
        tv.setText(icon + "\n" + value);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(2), 0, dpToPx(2), 0);
        return tv;
    }

    private void refreshStats() {
        int alienCount = prefs.getAlienCount();
        ColonyManager.ColonyStats s = ColonyManager.computeStats(slots, alienCount);
        int pop = s.effectivePopulation;

        tvPop.setText("👥\n" + pop + " / " + s.populationCapacity);
        tvPop.setTextColor(Color.WHITE);

        boolean o2ok    = s.oxygenCapacity  >= pop + 1 || pop == 0;
        boolean waterOk = s.waterCapacity   >= pop + 1 || pop == 0;
        boolean foodOk  = s.foodCapacity    >= pop + 1 || pop == 0;

        tvO2.setText("💨\n"   + s.oxygenCapacity);
        tvO2.setTextColor(Color.parseColor(o2ok    ? "#A5D6A7" : "#EF9A9A"));

        tvWater.setText("💧\n" + s.waterCapacity);
        tvWater.setTextColor(Color.parseColor(waterOk ? "#A5D6A7" : "#EF9A9A"));

        tvFood.setText("🌾\n"  + s.foodCapacity);
        tvFood.setTextColor(Color.parseColor(foodOk   ? "#A5D6A7" : "#EF9A9A"));

        tvDefense.setText("🛡\n" + s.defenseRating);
        tvDefense.setTextColor(Color.WHITE);
    }

    // ── Barre de ressources (bas) ───────────────────────────

    private View buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(16));
        bar.setBackgroundColor(Color.parseColor("#0D1A0D"));

        tvGold = new TextView(this);
        tvGold.setTextColor(Color.parseColor("#FFD700"));
        tvGold.setTextSize(13f);

        tvMetal = new TextView(this);
        tvMetal.setTextColor(Color.parseColor("#80DEEA"));
        tvMetal.setTextSize(13f);
        LinearLayout.LayoutParams metalLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        metalLp.setMargins(dpToPx(16), 0, 0, 0);
        tvMetal.setLayoutParams(metalLp);

        tvAlien = new TextView(this);
        tvAlien.setTextColor(Color.parseColor("#A5D6A7"));
        tvAlien.setTextSize(13f);
        LinearLayout.LayoutParams alienLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alienLp.setMargins(dpToPx(16), 0, 0, 0);
        tvAlien.setLayoutParams(alienLp);

        bar.addView(tvGold);
        bar.addView(tvMetal);
        bar.addView(tvAlien);

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        Button backBtn = new Button(this);
        backBtn.setText("← Retour");
        backBtn.setTextColor(Color.WHITE);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(Color.parseColor("#1B3A1B"));
        backBg.setCornerRadius(dpToPx(8));
        backBtn.setBackground(backBg);
        backBtn.setTextSize(13f);
        backBtn.setPadding(dpToPx(18), dpToPx(6), dpToPx(18), dpToPx(6));
        backBtn.setOnClickListener(v -> finish());
        bar.addView(backBtn);

        refreshResources();
        return bar;
    }

    private void refreshResources() {
        tvGold.setText("⬡ " + prefs.getGold());
        tvMetal.setText("⚙ " + prefs.getRareMetal());
        tvAlien.setText("👽 " + prefs.getAlienCount());
    }

    // ── Dialog détail bâtiment (avec upgrade) ─────────────

    private void showSlotDialog(int slotIndex) {
        ColonyBuildingSlot slot = slots[slotIndex];
        ColonyBuilding type = slot.type;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(16));
        layout.setBackgroundColor(Color.parseColor("#0D1A0D"));

        TextView titleTv = new TextView(this);
        titleTv.setText(type.icon + "  " + type.displayName);
        titleTv.setTextSize(20f);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setGravity(Gravity.CENTER);
        layout.addView(titleTv);

        TextView levelTv = new TextView(this);
        if (slot.isBuilt()) {
            levelTv.setText("Niveau " + slot.getLevel() + " / " + ColonyBuilding.MAX_LEVEL);
            levelTv.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            levelTv.setText("Emplacement vide");
            levelTv.setTextColor(Color.parseColor("#888888"));
        }
        levelTv.setTextSize(15f);
        levelTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lvLp.setMargins(0, dpToPx(6), 0, dpToPx(14));
        levelTv.setLayoutParams(lvLp);
        layout.addView(levelTv);

        layout.addView(makeDialogDivider());

        if (slot.isBuilt()) {
            addDialogRow(layout, type.statLabel,
                    String.valueOf(type.getStatAtLevel(slot.getLevel())), "#A5D6A7");
        }

        if (slot.isMaxLevel()) {
            layout.addView(makeDialogDivider());
            TextView maxTv = new TextView(this);
            maxTv.setText("✨ Niveau maximum atteint");
            maxTv.setTextColor(Color.parseColor("#FFD700"));
            maxTv.setTextSize(13f);
            maxTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mLp.setMargins(0, dpToPx(12), 0, 0);
            maxTv.setLayoutParams(mLp);
            layout.addView(maxTv);

        } else if (slot.isUpgrading()) {
            layout.addView(makeDialogDivider());
            int next = slot.getNextLevel();
            addDialogRow(layout, "→ Niveau " + next,
                    String.valueOf(type.getStatAtLevel(next)), "#80DEEA");
            layout.addView(makeDialogDivider());

            TextView timerLabel = new TextView(this);
            timerLabel.setText("⏳ Construction en cours…");
            timerLabel.setTextColor(Color.parseColor("#FFF176"));
            timerLabel.setTextSize(13f);
            timerLabel.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlLp.setMargins(0, dpToPx(12), 0, dpToPx(4));
            timerLabel.setLayoutParams(tlLp);
            layout.addView(timerLabel);

            TextView timerTv = new TextView(this);
            timerTv.setTextColor(Color.parseColor("#FFF176"));
            timerTv.setTextSize(22f);
            timerTv.setGravity(Gravity.CENTER);
            timerTv.setText(formatDuration(slot.getRemainingMillis()));
            LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ttLp.setMargins(0, 0, 0, dpToPx(8));
            timerTv.setLayoutParams(ttLp);
            layout.addView(timerTv);

            long remaining = slot.getRemainingMillis();
            if (activeCountDown != null) activeCountDown.cancel();
            activeCountDown = new CountDownTimer(remaining, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timerTv.setText(formatDuration(millisUntilFinished));
                }
                @Override
                public void onFinish() {
                    if (activeDialog != null && activeDialog.isShowing()) {
                        activeDialog.dismiss();
                    }
                    ColonyManager.checkAndCompleteAll(slots, prefs);
                    refreshStats();
                    refreshResources();
                    colonyView.setSlots(slots);
                    startTicker();
                }
            };
            activeCountDown.start();

        } else {
            int next = slot.getNextLevel();
            int goldCost  = type.getGoldCostForLevel(next);
            int metalCost = type.getMetalCostForLevel(next);
            long durSecs  = type.getDurationSecondsForLevel(next);

            layout.addView(makeDialogDivider());
            addDialogRow(layout, "→ Niveau " + next,
                    String.valueOf(type.getStatAtLevel(next)), "#80DEEA");
            layout.addView(makeDialogDivider());
            addDialogRow(layout, "⬡ Coût",   goldCost + " or",   "#FFD700");
            if (metalCost > 0) {
                boolean hasEnoughMetal = prefs.getRareMetal() >= metalCost;
                addDialogRow(layout, "⚙ Métal rare", metalCost + " métal",
                        hasEnoughMetal ? "#80DEEA" : "#EF9A9A");
            }
            addDialogRow(layout, "⏱ Durée", formatDuration(durSecs * 1000L), "#AAAAAA");

            layout.addView(makeDialogDivider());

            boolean canUpgrade = ColonyManager.canStartUpgrade(slot, prefs);
            Button upgradeBtn = new Button(this);
            String btnLabel = slot.isBuilt()
                    ? "⬆  Améliorer (Lv" + slot.getLevel() + " → " + next + ")"
                    : "🔨  Construire";
            upgradeBtn.setText(btnLabel);
            upgradeBtn.setTextSize(15f);
            upgradeBtn.setTextColor(Color.parseColor(canUpgrade ? "#0A0A0A" : "#777777"));
            GradientDrawable upgBg = new GradientDrawable();
            upgBg.setColor(Color.parseColor(canUpgrade ? "#4CAF50" : "#1E3A1E"));
            upgBg.setCornerRadius(dpToPx(10));
            upgradeBtn.setBackground(upgBg);
            upgradeBtn.setEnabled(canUpgrade);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, dpToPx(14), 0, dpToPx(4));
            upgradeBtn.setLayoutParams(btnLp);

            upgradeBtn.setOnClickListener(v -> {
                if (ColonyManager.startUpgrade(slot, prefs)) {
                    if (activeDialog != null && activeDialog.isShowing()) {
                        activeDialog.dismiss();
                    }
                    refreshResources();
                    colonyView.setSlots(slots);
                    startTicker();
                    showSlotDialog(slotIndex);
                }
            });
            layout.addView(upgradeBtn);

            if (!canUpgrade) {
                TextView cantTv = new TextView(this);
                boolean goldOk  = prefs.getGold() >= goldCost;
                boolean metalOk = metalCost == 0 || prefs.getRareMetal() >= metalCost;
                if (!goldOk)  cantTv.setText("⬡ Or insuffisant (" + prefs.getGold() + " / " + goldCost + ")");
                else if (!metalOk) cantTv.setText("⚙ Métal rare insuffisant (" + prefs.getRareMetal() + " / " + metalCost + ")");
                cantTv.setTextColor(Color.parseColor("#EF9A9A"));
                cantTv.setTextSize(12f);
                cantTv.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cLp.setMargins(0, dpToPx(4), 0, 0);
                cantTv.setLayoutParams(cLp);
                layout.addView(cantTv);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Fermer", (d, w) -> {
                    if (activeCountDown != null) {
                        activeCountDown.cancel();
                        activeCountDown = null;
                    }
                });
        activeDialog = builder.show();
    }

    private void addDialogRow(LinearLayout parent, String label, String value, String valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dpToPx(8), 0, 0);
        row.setLayoutParams(rowLp);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextColor(Color.parseColor("#AAAAAA"));
        labelTv.setTextSize(13f);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(labelTv);

        TextView valueTv = new TextView(this);
        valueTv.setText(value);
        valueTv.setTextColor(Color.parseColor(valueColor));
        valueTv.setTextSize(13f);
        valueTv.setGravity(Gravity.END);
        row.addView(valueTv);

        parent.addView(row);
    }

    private View makeDialogDivider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dpToPx(10), 0, dpToPx(2));
        v.setLayoutParams(lp);
        v.setBackgroundColor(Color.parseColor("#1E3A1E"));
        return v;
    }

    // ── Helpers ────────────────────────────────────────────

    private String formatDuration(long millis) {
        long totalSecs = millis / 1000L;
        if (totalSecs < 60)   return totalSecs + "s";
        long minutes = totalSecs / 60;
        long secs    = totalSecs % 60;
        if (minutes < 60)     return minutes + "m " + secs + "s";
        long hours = minutes / 60;
        long mins  = minutes % 60;
        return hours + "h " + mins + "m";
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}