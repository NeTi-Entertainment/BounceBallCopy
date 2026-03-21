package com.example.bounceball.colony;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import com.example.bounceball.utils.ImmersiveHelper;

public class ColonyActivity extends Activity {

    private GamePreferences prefs;
    private ColonyBuildingSlot[] slots;
    private ColonyView colonyView;

    private TextView tvPop, tvO2, tvWater, tvFood;
    private TextView tvGold, tvMetal, tvAlien;
    private TextView tvComplete;
    private TextView btnNextColony;
    private LinearLayout page1;
    private LinearLayout page2;
    private boolean isOnPage2 = false;

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
        ImmersiveHelper.enable(getWindow());
        if (slots != null) {
            ColonyManager.checkAndCompleteAll(slots, prefs);
            refreshStats();
            refreshResources();
            colonyView.setSlots(slots);
        }
        startTicker();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ImmersiveHelper.enable(getWindow());
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
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.parseColor("#0A130A"));

        page1 = new LinearLayout(this);
        page1.setOrientation(LinearLayout.VERTICAL);
        page1.setBackgroundColor(Color.parseColor("#0A130A"));
        page1.addView(buildStatBar());
        page1.addView(buildCompletionBanner());
        colonyView = new ColonyView(this, slots);
        colonyView.setOnSlotTappedListener(this::showSlotDialog);
        page1.addView(colonyView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        page1.addView(buildBottomBar());

        page2 = buildPlaceholderPage();
        page2.setVisibility(View.GONE);

        rootFrame.addView(page1, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(page2, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        btnNextColony = new TextView(this);
        btnNextColony.setText("▶");
        btnNextColony.setTextSize(28f);
        btnNextColony.setTextColor(Color.parseColor("#FFD700"));
        btnNextColony.setGravity(Gravity.CENTER);
        btnNextColony.setPadding(dpToPx(10), dpToPx(20), dpToPx(10), dpToPx(20));
        btnNextColony.setVisibility(View.GONE);
        GradientDrawable arrowBg = new GradientDrawable();
        arrowBg.setColor(Color.parseColor("#1B3A1B"));
        arrowBg.setCornerRadius(dpToPx(8));
        btnNextColony.setBackground(arrowBg);
        FrameLayout.LayoutParams arrowLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        arrowLp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        arrowLp.rightMargin = dpToPx(8);
        btnNextColony.setLayoutParams(arrowLp);
        btnNextColony.setOnClickListener(v -> slideToPage2());
        rootFrame.addView(btnNextColony);

        setContentView(rootFrame);
        refreshStats();
    }

    private View buildCompletionBanner() {
        tvComplete = new TextView(this);
        tvComplete.setText("✨ Colonie complète ! Une nouvelle planète vous attend.");
        tvComplete.setTextColor(Color.parseColor("#FFD700"));
        tvComplete.setTextSize(13f);
        tvComplete.setGravity(Gravity.CENTER);
        tvComplete.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        tvComplete.setBackgroundColor(Color.parseColor("#1B3A00"));
        tvComplete.setVisibility(View.GONE);
        return tvComplete;
    }

    private LinearLayout buildPlaceholderPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(Color.parseColor("#050D1A"));

        TextView planetTv = new TextView(this);
        planetTv.setText("🪐");
        planetTv.setTextSize(96f);
        planetTv.setGravity(Gravity.CENTER);
        page.addView(planetTv);

        TextView titleTv = new TextView(this);
        titleTv.setText("Nouvelle colonie");
        titleTv.setTextSize(24f);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dpToPx(16), 0, dpToPx(8));
        titleTv.setLayoutParams(tlp);
        page.addView(titleTv);

        TextView subTv = new TextView(this);
        subTv.setText("Bientôt disponible…");
        subTv.setTextSize(16f);
        subTv.setTextColor(Color.parseColor("#888888"));
        subTv.setGravity(Gravity.CENTER);
        page.addView(subTv);

        TextView backArrow = new TextView(this);
        backArrow.setText("◀  Retour à la lune");
        backArrow.setTextSize(15f);
        backArrow.setTextColor(Color.parseColor("#80DEEA"));
        backArrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, dpToPx(48), 0, 0);
        backArrow.setLayoutParams(blp);
        backArrow.setOnClickListener(v -> slideToPage1());
        page.addView(backArrow);

        return page;
    }

    private void slideToPage2() {
        if (isOnPage2) return;
        int w = page1.getWidth();
        page2.setVisibility(View.VISIBLE);
        page2.setTranslationX(w);
        ObjectAnimator a1 = ObjectAnimator.ofFloat(page1, "translationX", 0f, -w);
        ObjectAnimator a2 = ObjectAnimator.ofFloat(page2, "translationX", w, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(a1, a2);
        set.setDuration(380);
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                page1.setVisibility(View.GONE);
                btnNextColony.setVisibility(View.GONE);
                isOnPage2 = true;
            }
        });
        set.start();
    }

    private void slideToPage1() {
        if (!isOnPage2) return;
        int w = page2.getWidth();
        page1.setVisibility(View.VISIBLE);
        page1.setTranslationX(-w);
        ObjectAnimator a1 = ObjectAnimator.ofFloat(page1, "translationX", -w, 0f);
        ObjectAnimator a2 = ObjectAnimator.ofFloat(page2, "translationX", 0f, w);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(a1, a2);
        set.setDuration(380);
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                page2.setVisibility(View.GONE);
                isOnPage2 = false;
                checkCompletion();
            }
        });
        set.start();
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

        tvPop   = makeStatChip("👥", "0");
        tvO2    = makeStatChip("💨", "0");
        tvWater = makeStatChip("💧", "0");
        tvFood  = makeStatChip("🌾", "0");

        tvPop.setLayoutParams(  new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvO2.setLayoutParams(   new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvWater.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvFood.setLayoutParams( new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        bar.addView(tvPop);
        bar.addView(tvO2);
        bar.addView(tvWater);
        bar.addView(tvFood);

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

        boolean o2ok    = s.oxygenCapacity  == 0 || s.oxygenCapacity  >= pop + 1 || pop == 0;
        boolean waterOk = s.waterCapacity   == 0 || s.waterCapacity   >= pop + 1 || pop == 0;
        boolean foodOk  = s.foodCapacity    == 0 || s.foodCapacity    >= pop + 1 || pop == 0;

        tvO2.setText("💨\n"   + s.oxygenCapacity);
        tvO2.setTextColor(Color.parseColor(o2ok    ? "#A5D6A7" : "#EF9A9A"));

        tvWater.setText("💧\n" + s.waterCapacity);
        tvWater.setTextColor(Color.parseColor(waterOk ? "#A5D6A7" : "#EF9A9A"));

        tvFood.setText("🌾\n"  + s.foodCapacity);
        tvFood.setTextColor(Color.parseColor(foodOk   ? "#A5D6A7" : "#EF9A9A"));

        checkCompletion();
    }

    private void checkCompletion() {
        if (isOnPage2) return;
        boolean complete = ColonyManager.isComplete(slots, prefs.getAlienCount());
        tvComplete.setVisibility(complete ? View.VISIBLE : View.GONE);
        btnNextColony.setVisibility(complete ? View.VISIBLE : View.GONE);
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

        bar.addView(tvGold);
        bar.addView(tvMetal);

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
        if (isOnPage2) {
            slideToPage1();
        } else {
            finish();
        }
    }
}