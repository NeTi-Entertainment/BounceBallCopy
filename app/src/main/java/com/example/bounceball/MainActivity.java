package com.example.bounceball;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.*;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.AdManager;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.LocaleManager;

public class MainActivity extends Activity implements GameView.GameStateListener {

    private GameView gameView;
    private GamePreferences prefs;

    private TextView shopBtn, eggBtn;
    private TextView tapText;
    private TextView recordText;   // affiche le record en permanence sur l'écran
    private boolean inGame = false;

    private FrameLayout settingsOverlay;
    private FrameLayout shopOverlay;
    private FrameLayout eggOverlay;

    private ScrollView cosmeticsScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── 1. Applique la langue sauvegardée AVANT tout affichage ──
        LocaleManager.applyLocale(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = new GamePreferences(this);

        // ── 2. Initialisation AdMob (une seule fois au lancement) ──
        AdManager.getInstance().initialize(this);

        UpgradeStats upgrades = UpgradeStats.fromPrefs(prefs.getRaw());

        FrameLayout root = new FrameLayout(this);

        gameView = new GameView(this, prefs, upgrades);
        gameView.setGameStateListener(this);
        root.addView(gameView, matchParentFl());

        // ── Boutons ronds en haut à droite ──
        LinearLayout btnCol = new LinearLayout(this);
        btnCol.setOrientation(LinearLayout.VERTICAL);
        btnCol.setPadding(0, dpToPx(50), dpToPx(18), 0);
        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        colLp.gravity = Gravity.TOP | Gravity.END;
        root.addView(btnCol, colLp);

        btnCol.addView(makeRoundBtn("⚙", v -> showOverlay(settingsOverlay)));
        shopBtn = makeRoundBtn("🏪", v -> showOverlay(shopOverlay));
        btnCol.addView(shopBtn);
        eggBtn = makeRoundBtn("🥚", v -> showOverlay(eggOverlay));
        btnCol.addView(eggBtn);

        // ── Record d'ascension affiché en permanence ──
        recordText = new TextView(this);
        recordText.setTextColor(Color.parseColor("#888888"));
        recordText.setTextSize(16f);
        recordText.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams recLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        recLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        recLp.bottomMargin = dpToPx(130);
        root.addView(recordText, recLp);
        refreshRecordDisplay();

        // ── Texte "Appuyer pour jouer" ──
        tapText = new TextView(this);
        tapText.setText("Appuyer pour jouer");
        tapText.setTextColor(Color.parseColor("#222222"));
        tapText.setTextSize(22f);
        tapText.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tapLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        tapLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tapLp.bottomMargin = dpToPx(80);
        root.addView(tapText, tapLp);
        startPulse();

        // ── Overlays (construits APRÈS les vues qu'ils référencent) ──
        settingsOverlay = buildSettingsOverlay();
        shopOverlay     = buildShopOverlay();
        eggOverlay      = buildEggOverlay();
        root.addView(settingsOverlay, matchParentFl());
        root.addView(shopOverlay,     matchParentFl());
        root.addView(eggOverlay,      matchParentFl());

        setContentView(root);
    }

    // ══════════════════════════════════════════════════════
    // CALLBACKS JEU
    // ══════════════════════════════════════════════════════

    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            inGame = true;
            gameView.setHudVisible(true);
            tapText.clearAnimation();
            tapText.setVisibility(View.GONE);
            recordText.setVisibility(View.GONE);
            shopBtn.setVisibility(View.GONE);
            eggBtn.setVisibility(View.GONE);
        });
    }

    @Override
    public void onGameOver(float heightReached) {
        // ── 3. Sauvegarde le progrès (record + or) ──
        UpgradeStats upgrades = UpgradeStats.fromPrefs(prefs.getRaw());
        boolean newRecord = prefs.updateMaxHeight(heightReached);       // sauvegarde record
        int goldEarned = (int) Math.floor(heightReached * upgrades.goldMultiplier);
        prefs.addGold(goldEarned);                                      // sauvegarde or

        // ── Pub interstitielle ──
        AdManager.getInstance().onGameOver(this);

        runOnUiThread(() -> {
            inGame = false;
            gameView.setHudVisible(false);
            shopBtn.setVisibility(View.VISIBLE);
            eggBtn.setVisibility(View.VISIBLE);
            tapText.setVisibility(View.VISIBLE);
            recordText.setVisibility(View.VISIBLE);
            refreshRecordDisplay();   // met à jour le record affiché
            startPulse();

            // Toast si nouveau record
            if (newRecord) {
                Toast.makeText(this,
                        String.format("🏆 Nouveau record : %.1f m !", heightReached),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.loadBallSkin();
    }

    /** Met à jour le TextView du record avec la valeur persistée. */
    private void refreshRecordDisplay() {
        float maxH = prefs.getMaxHeight();
        if (maxH > 0f) {
            recordText.setText(String.format("🏆 Record : %.1f m", maxH));
        } else {
            recordText.setText("Aucun record encore");
        }
    }

    // ══════════════════════════════════════════════════════
    // OVERLAYS
    // ══════════════════════════════════════════════════════

    private void showOverlay(FrameLayout overlay) {
        tapText.clearAnimation();
        tapText.setVisibility(View.GONE);
        recordText.setVisibility(View.GONE);
        overlay.setVisibility(View.VISIBLE);
        if (overlay == shopOverlay && cosmeticsScrollView != null) {
            CosmeticsPage.refreshAll(cosmeticsScrollView);
        }
    }

    private void hideOverlay(FrameLayout overlay) {
        overlay.setVisibility(View.GONE);
        gameView.loadBallSkin();
        if (!inGame) {
            tapText.setVisibility(View.VISIBLE);
            recordText.setVisibility(View.VISIBLE);
            refreshRecordDisplay();
            startPulse();
        }
    }

    // ── PARAMÈTRES ─────────────────────────────────────────
    private FrameLayout buildSettingsOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(220, 10, 20, 40));

        ScrollView scroll = new ScrollView(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        inner.setPadding(dpToPx(32), dpToPx(60), dpToPx(32), dpToPx(40));

        // Titre
        TextView title = new TextView(this);
        title.setText("⚙  Paramètres");
        title.setTextSize(26f);
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(24));
        inner.addView(title);

        // ─── Section SON ───────────────────────────────────
        addSettingsSectionHeader(inner, "🔊 Son");

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);
        soundRow.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView soundLabel = new TextView(this);
        soundLabel.setText("Effets sonores");
        soundLabel.setTextColor(Color.WHITE);
        soundLabel.setTextSize(16f);
        soundLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch soundSwitch = new Switch(this);
        soundSwitch.setChecked(prefs.isSoundEnabled());
        // ── Sauvegarde le choix son immédiatement ──
        soundSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.setSoundEnabled(checked));

        soundRow.addView(soundLabel);
        soundRow.addView(soundSwitch);
        inner.addView(soundRow);

        addSettingsDivider(inner);

        // ─── Section LANGUE ────────────────────────────────
        addSettingsSectionHeader(inner, "🌍 Langue");

        String[] langNames = {"Français", "English", "Español", "Deutsch", "日本語"};
        String[] langCodes = {"fr", "en", "es", "de", "ja"};
        String currentLang = prefs.getLanguage();

        RadioGroup langGroup = new RadioGroup(this);
        langGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < langNames.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(langNames[i]);
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(15f);
            rb.setTag(langCodes[i]);
            rb.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
            if (langCodes[i].equals(currentLang)) rb.setChecked(true);
            langGroup.addView(rb);
        }

        langGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                String code = (String) selected.getTag();
                prefs.setLanguage(code);          // ── 4. Sauvegarde la langue ──
                LocaleManager.applyLocale(this);  // applique immédiatement
                recreate();                       // redémarre pour rafraîchir les textes
            }
        });

        inner.addView(langGroup);
        addSettingsDivider(inner);

        // ─── Section PROGRESSION ───────────────────────────
        addSettingsSectionHeader(inner, "📊 Progression");

        TextView recView = new TextView(this);
        float maxH = prefs.getMaxHeight();
        recView.setText(maxH > 0f
                ? String.format("🏆 Meilleure hauteur : %.1f m", maxH)
                : "Aucun record enregistré");
        recView.setTextColor(Color.parseColor("#00E676"));
        recView.setTextSize(15f);
        recView.setPadding(0, dpToPx(8), 0, dpToPx(4));
        inner.addView(recView);

        TextView goldView = new TextView(this);
        goldView.setText("⬡ Or total : " + prefs.getGold());
        goldView.setTextColor(Color.parseColor("#FFD700"));
        goldView.setTextSize(15f);
        goldView.setPadding(0, 0, 0, dpToPx(4));
        inner.addView(goldView);

        TextView diamView = new TextView(this);
        diamView.setText("◆ Diamants : " + prefs.getDiamonds());
        diamView.setTextColor(Color.parseColor("#80DEEA"));
        diamView.setTextSize(15f);
        inner.addView(diamView);

        addSettingsDivider(inner);

        // ─── Bouton Retour ─────────────────────────────────
        Button backBtn = new Button(this);
        backBtn.setText("← Retour");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setBackgroundColor(Color.parseColor("#1B5E20"));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backLp.setMargins(0, dpToPx(24), 0, 0);
        backBtn.setLayoutParams(backLp);
        backBtn.setOnClickListener(v -> hideOverlay(overlay));
        inner.addView(backBtn);

        scroll.addView(inner);
        overlay.addView(scroll, matchParentFl());
        return overlay;
    }

    private void addSettingsSectionHeader(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#80DEEA"));
        tv.setTextSize(14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(16), 0, dpToPx(4));
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private void addSettingsDivider(LinearLayout parent) {
        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dpToPx(14), 0, dpToPx(6));
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(Color.parseColor("#2A3A4A"));
        parent.addView(divider);
    }

    // ── ŒUF ───────────────────────────────────────────────
    private FrameLayout buildEggOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(210, 10, 20, 40));
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        TextView tv = new TextView(this);
        tv.setText("🥚  Éclosion");
        tv.setTextSize(28f);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        inner.addView(tv);
        inner.addView(makeRetourBtn(v -> hideOverlay(overlay)));
        overlay.addView(inner, matchParentFl());
        return overlay;
    }

    // ── SHOP ──────────────────────────────────────────────
    private FrameLayout buildShopOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(170, 0, 0, 0));

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(Color.parseColor("#0D1B2A"));
        sheetBg.setCornerRadius(dpToPx(16));
        sheet.setBackground(sheetBg);

        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        sheetLp.topMargin    = dpToPx(48);
        sheetLp.bottomMargin = dpToPx(16);
        sheetLp.leftMargin   = dpToPx(12);
        sheetLp.rightMargin  = dpToPx(12);
        overlay.addView(sheet, sheetLp);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView upgradesTab  = makeTab("upgrades");
        TextView cosmeticsTab = makeTab("cosmétiques");
        upgradesTab.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cosmeticsTab.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabRow.addView(upgradesTab);
        tabRow.addView(cosmeticsTab);
        sheet.addView(tabRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(Color.parseColor("#111E2C"));
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout upgradesPage = new LinearLayout(this);
        upgradesPage.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currRow = new LinearLayout(this);
        currRow.setOrientation(LinearLayout.HORIZONTAL);
        currRow.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(6));
        TextView goldTv = new TextView(this);
        goldTv.setTextColor(Color.parseColor("#FFD700"));
        goldTv.setTextSize(15f);
        TextView diamTv = new TextView(this);
        diamTv.setTextColor(Color.parseColor("#80DEEA"));
        diamTv.setTextSize(15f);
        diamTv.setPadding(dpToPx(20), 0, 0, 0);
        currRow.addView(goldTv);
        currRow.addView(diamTv);
        upgradesPage.addView(currRow);

        Runnable refreshCurr = () -> runOnUiThread(() -> {
            goldTv.setText("⬡ " + prefs.getGold() + " Or");
            diamTv.setText("◆ " + prefs.getDiamonds() + " Diam");
        });
        refreshCurr.run();

        Object[][] UPGRADES = {
                {"Résistance à l'air",   "Réduit la traînée aérodynamique",       50,  5,  10, "upg_air"},
                {"Élasticité",           "Trampoline plus rebondissant",           80,  8,  10, "upg_elastic"},
                {"Boost Fusée",          "Double tap en montée pour booster",     150, 15,   5, "upg_boost"},
                {"Recharge Boost",       "Recharge le boost plus vite",           100, 10,   5, "upg_boost_recharge"},
                {"Réserve d'encre",      "Plus d'encre disponible par partie",     60,  6,  10, "upg_ink_reserve"},
                {"Efficacité Encre",     "Consomme moins d'encre par pixel",       70,  7,  10, "upg_ink_eff"},
                {"Multiplicateur d'or",  "×1.01 or par hauteur atteinte",         200, 20, 100, "upg_gold_mult"},
                {"Warp",                 "Portails de téléportation vers le haut", 300, 30,   5, "upg_warp"},
        };
        for (Object[] upg : UPGRADES) {
            upgradesPage.addView(buildUpgradeRow(upg, refreshCurr));
        }

        ScrollView upgradesScroll = new ScrollView(this);
        upgradesScroll.addView(upgradesPage);
        content.addView(upgradesScroll, matchParentFl());

        cosmeticsScrollView = CosmeticsPage.build(this, prefs);
        ScrollView cosmeticsPage = cosmeticsScrollView;
        cosmeticsPage.setVisibility(View.GONE);
        content.addView(cosmeticsPage, matchParentFl());
        sheet.addView(content);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(0, dpToPx(10), 0, dpToPx(14));
        bottomBar.setBackgroundColor(Color.parseColor("#0D1B2A"));
        Button retourBtn = new Button(this);
        retourBtn.setText("Retour");
        retourBtn.setTextColor(Color.WHITE);
        retourBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));
        retourBtn.setOnClickListener(v -> hideOverlay(overlay));
        bottomBar.addView(retourBtn);
        sheet.addView(bottomBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Runnable selUpgrades = () -> {
            upgradesScroll.setVisibility(View.VISIBLE);
            cosmeticsPage.setVisibility(View.GONE);
            upgradesTab.setBackgroundColor(Color.parseColor("#111E2C"));
            upgradesTab.setTextColor(Color.WHITE);
            upgradesTab.setAlpha(1f);
            cosmeticsTab.setBackgroundColor(Color.parseColor("#0A1520"));
            cosmeticsTab.setTextColor(Color.parseColor("#888888"));
            cosmeticsTab.setAlpha(0.8f);
            refreshCurr.run();
        };
        Runnable selCosmetics = () -> {
            upgradesScroll.setVisibility(View.GONE);
            cosmeticsPage.setVisibility(View.VISIBLE);
            CosmeticsPage.refreshAll(cosmeticsPage);
            cosmeticsTab.setBackgroundColor(Color.parseColor("#111E2C"));
            cosmeticsTab.setTextColor(Color.WHITE);
            cosmeticsTab.setAlpha(1f);
            upgradesTab.setBackgroundColor(Color.parseColor("#0A1520"));
            upgradesTab.setTextColor(Color.parseColor("#888888"));
            upgradesTab.setAlpha(0.8f);
        };
        upgradesTab.setOnClickListener(v -> selUpgrades.run());
        cosmeticsTab.setOnClickListener(v -> selCosmetics.run());
        selUpgrades.run();

        return overlay;
    }

    // ══════════════════════════════════════════════════════
    // HELPERS UI
    // ══════════════════════════════════════════════════════

    private View buildUpgradeRow(Object[] upg, Runnable refreshCurr) {
        String name    = (String) upg[0];
        String desc    = (String) upg[1];
        int goldBase   = (int)    upg[2];
        int diamBase   = (int)    upg[3];
        int maxLvl     = (int)    upg[4];
        String key     = (String) upg[5];

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        row.setBackgroundColor(Color.parseColor("#1A2A3A"));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(dpToPx(8), dpToPx(6), dpToPx(8), 0);
        row.setLayoutParams(rowLp);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(15f);
        row.addView(nameView);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setTextSize(12f);
        row.addView(descView);

        TextView levelView = new TextView(this);
        levelView.setTextColor(Color.parseColor("#00E676"));
        levelView.setTextSize(12f);
        row.addView(levelView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dpToPx(4), 0, 0);

        Button goldBtn = new Button(this);
        Button diamBtn = new Button(this);
        SharedPreferences raw = prefs.getRaw();

        Runnable refresh = () -> {
            int lvl      = raw.getInt(key, 0);
            int goldCost = goldBase * (lvl + 1);
            int diamCost = (int) (diamBase * (lvl + 1) * 0.5f);
            levelView.setText("Niveau: " + lvl + " / " + maxLvl);
            if (lvl >= maxLvl) {
                goldBtn.setText("MAX"); goldBtn.setEnabled(false);
                diamBtn.setText("MAX"); diamBtn.setEnabled(false);
            } else {
                goldBtn.setText("⬡ " + goldCost + " Or");
                diamBtn.setText("◆ " + diamCost + " Diam");
                goldBtn.setEnabled(prefs.getGold() >= goldCost);
                diamBtn.setEnabled(prefs.getDiamonds() >= diamCost);
            }
            refreshCurr.run();
        };

        goldBtn.setTextColor(Color.parseColor("#FFD700"));
        goldBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));
        goldBtn.setTextSize(12f);
        goldBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        goldBtn.setOnClickListener(v -> {
            int lvl = raw.getInt(key, 0);
            int cost = goldBase * (lvl + 1);
            if (prefs.getGold() >= cost && lvl < maxLvl) {
                prefs.spendGold(cost);
                raw.edit().putInt(key, lvl + 1).apply();
                refresh.run();
            }
        });

        diamBtn.setTextColor(Color.parseColor("#80DEEA"));
        diamBtn.setBackgroundColor(Color.parseColor("#1A1A3A"));
        diamBtn.setTextSize(12f);
        diamBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(dpToPx(10), 0, 0, 0);
        diamBtn.setLayoutParams(dLp);
        diamBtn.setOnClickListener(v -> {
            int lvl  = raw.getInt(key, 0);
            int cost = (int) (diamBase * (lvl + 1) * 0.5f);
            if (prefs.getDiamonds() >= cost && lvl < maxLvl) {
                prefs.spendDiamonds(cost);
                raw.edit().putInt(key, lvl + 1).apply();
                refresh.run();
            }
        });

        btnRow.addView(goldBtn);
        btnRow.addView(diamBtn);
        row.addView(btnRow);
        refresh.run();
        return row;
    }

    private TextView makeTab(String label) {
        TextView tab = new TextView(this);
        tab.setText(label);
        tab.setTextSize(16f);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        return tab;
    }

    private TextView makeRoundBtn(String icon, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(icon);
        btn.setTextSize(28f);
        btn.setGravity(Gravity.CENTER);
        int size = dpToPx(64);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.topMargin = dpToPx(12);
        btn.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.WHITE);
        bg.setStroke(dpToPx(2), Color.parseColor("#CCCCCC"));
        btn.setBackground(bg);
        btn.setOnClickListener(listener);
        return btn;
    }

    private Button makeRetourBtn(View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText("Retour");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#1B3A1B"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(40);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    private void startPulse() {
        AlphaAnimation pulse = new AlphaAnimation(0.1f, 1f);
        pulse.setDuration(950);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        tapText.startAnimation(pulse);
    }

    private FrameLayout.LayoutParams matchParentFl() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (settingsOverlay != null && settingsOverlay.getVisibility() == View.VISIBLE) {
            hideOverlay(settingsOverlay);
        } else if (shopOverlay != null && shopOverlay.getVisibility() == View.VISIBLE) {
            hideOverlay(shopOverlay);
        } else if (eggOverlay != null && eggOverlay.getVisibility() == View.VISIBLE) {
            hideOverlay(eggOverlay);
        } else {
            super.onBackPressed();
        }
    }
}