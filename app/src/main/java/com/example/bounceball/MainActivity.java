package com.example.bounceball;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import com.example.bounceball.colony.ColonyActivity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.example.bounceball.utils.EggHatchManager;
import android.widget.*;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.AdManager;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.LocaleManager;
import com.example.bounceball.utils.Strings;

public class MainActivity extends Activity implements GameView.GameStateListener {

    private GameView gameView;
    private GamePreferences prefs;

    private ImageView shopBtn, eggBtn, settingsBtn;
    private TextView tapText;
    private TextView recordText;
    private boolean inGame = false;

    private FrameLayout settingsOverlay;
    private FrameLayout shopOverlay;
    private FrameLayout eggOverlay;

    private ScrollView cosmeticsScrollView;
    private FrameLayout gachaOverlay;
    private FrameLayout eggContentFrame;
    private ObjectAnimator eggBtnAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleManager.applyLocale(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = new GamePreferences(this);

        AdManager.getInstance().initialize(this);

        UpgradeStats upgrades = UpgradeStats.fromPrefs(prefs.getRaw());

        FrameLayout root = new FrameLayout(this);

        gameView = new GameView(this, prefs, upgrades);
        gameView.setGameStateListener(this);
        root.addView(gameView, matchParentFl());

        LinearLayout btnCol = new LinearLayout(this);
        btnCol.setOrientation(LinearLayout.VERTICAL);
        btnCol.setPadding(0, dpToPx(50), dpToPx(18), 0);
        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        colLp.gravity = Gravity.TOP | Gravity.END;
        root.addView(btnCol, colLp);

        settingsBtn = makeImageBtn(R.drawable.ic_settings, v -> showOverlay(settingsOverlay));
        btnCol.addView(settingsBtn);

        shopBtn = makeImageBtn(R.drawable.ic_shop, v -> showOverlay(shopOverlay));
        btnCol.addView(shopBtn);

        eggBtn = makeImageBtn(R.drawable.ic_egg, v -> showEggOverlay());
        btnCol.addView(eggBtn);

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

        tapText = new TextView(this);
        tapText.setText(Strings.get("main.tap_to_play"));
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

        settingsOverlay = buildSettingsOverlay();
        shopOverlay     = buildShopOverlay();
        eggOverlay      = buildEggOverlay();
        gachaOverlay    = GachaPage.buildOverlay(this, prefs);
        root.addView(settingsOverlay, matchParentFl());
        root.addView(shopOverlay,     matchParentFl());
        root.addView(eggOverlay,      matchParentFl());
        root.addView(gachaOverlay,    matchParentFl());

        setContentView(root);
        refreshEggButton();

        if (prefs.getRaw().getBoolean("reopen_settings", false)) {
            prefs.getRaw().edit().putBoolean("reopen_settings", false).apply();
            showOverlay(settingsOverlay);
        }
    }

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
            settingsBtn.setVisibility(View.GONE);
        });
    }

    @Override
    public void onGameOver(float heightReached) {
        UpgradeStats upgrades = UpgradeStats.fromPrefs(prefs.getRaw());
        boolean newRecord = prefs.updateMaxHeight(heightReached);
        int goldEarned = (int) Math.floor(heightReached * upgrades.goldMultiplier);
        prefs.addGold(goldEarned);
        EggHatchManager.checkAndSetReady(prefs, heightReached);

        AdManager.getInstance().onGameOver(this);

        runOnUiThread(() -> {
            inGame = false;
            gameView.setHudVisible(false);
            shopBtn.setVisibility(View.VISIBLE);
            eggBtn.setVisibility(View.VISIBLE);
            settingsBtn.setVisibility(View.VISIBLE);
            tapText.setVisibility(View.VISIBLE);
            recordText.setVisibility(View.VISIBLE);
            refreshRecordDisplay();
            refreshEggButton();
            startPulse();

            if (newRecord) {
                Toast.makeText(this,
                        Strings.fmt("main.record_new_fmt", heightReached),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.example.bounceball.utils.ImmersiveHelper.enable(getWindow());
        if (gameView != null) gameView.loadBallSkin();
        if (gameView != null) gameView.loadBgSkin();
        refreshEggButton();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) com.example.bounceball.utils.ImmersiveHelper.enable(getWindow());
    }

    private void refreshRecordDisplay() {
        float maxH = prefs.getMaxHeight();
        if (maxH > 0f) {
            recordText.setText(Strings.fmt("main.record_fmt", maxH));
        } else {
            recordText.setText(Strings.get("main.record_none"));
        }
    }

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
        gameView.loadBgSkin();
        if (!inGame) {
            tapText.setVisibility(View.VISIBLE);
            recordText.setVisibility(View.VISIBLE);
            refreshRecordDisplay();
            startPulse();
        }
    }

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

        TextView title = new TextView(this);
        title.setText(Strings.get("settings.title"));
        title.setTextSize(26f);
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(24));
        inner.addView(title);

        addSettingsSectionHeader(inner, Strings.get("settings.section_sound"));

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);
        soundRow.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView soundLabel = new TextView(this);
        soundLabel.setText(Strings.get("settings.label_sfx"));
        soundLabel.setTextColor(Color.WHITE);
        soundLabel.setTextSize(16f);
        soundLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch soundSwitch = new Switch(this);
        soundSwitch.setChecked(prefs.isSoundEnabled());
        soundSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.setSoundEnabled(checked));

        soundRow.addView(soundLabel);
        soundRow.addView(soundSwitch);
        inner.addView(soundRow);

        addSettingsDivider(inner);

        addSettingsSectionHeader(inner, Strings.get("settings.section_language"));

        final String[][] LANGUAGES = SettingsActivity.LANGUAGES;

        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setGravity(Gravity.CENTER_VERTICAL);
        langRow.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        android.graphics.drawable.GradientDrawable langBg = new android.graphics.drawable.GradientDrawable();
        langBg.setColor(Color.parseColor("#1A2A3A"));
        langBg.setCornerRadius(dpToPx(8));
        langBg.setStroke(dpToPx(1), Color.parseColor("#2A3A4A"));
        langRow.setBackground(langBg);
        LinearLayout.LayoutParams langRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        langRowLp.setMargins(0, dpToPx(4), 0, 0);
        langRow.setLayoutParams(langRowLp);

        String currentLang = prefs.getLanguage();
        String currentName = currentLang;
        for (String[] l : LANGUAGES) { if (l[0].equals(currentLang)) { currentName = l[1]; break; } }

        TextView langValueTv = new TextView(this);
        langValueTv.setText(currentName);
        langValueTv.setTextColor(Color.WHITE);
        langValueTv.setTextSize(15f);
        langValueTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView langArrow = new TextView(this);
        langArrow.setText("▾");
        langArrow.setTextColor(Color.parseColor("#80DEEA"));
        langArrow.setTextSize(16f);

        langRow.addView(langValueTv);
        langRow.addView(langArrow);

        langRow.setOnClickListener(v -> {
            String[] names = new String[LANGUAGES.length];
            int checked = 0;
            String cur = prefs.getLanguage();
            for (int i = 0; i < LANGUAGES.length; i++) {
                names[i] = LANGUAGES[i][1];
                if (LANGUAGES[i][0].equals(cur)) checked = i;
            }
            new android.app.AlertDialog.Builder(this)
                    .setTitle(Strings.get("settings.section_language"))
                    .setSingleChoiceItems(names, checked, (dialog, which) -> {
                        String selectedCode = LANGUAGES[which][0];
                        dialog.dismiss();
                        if (!selectedCode.equals(prefs.getLanguage())) {
                            prefs.setLanguage(selectedCode);
                            prefs.getRaw().edit().putBoolean("reopen_settings", true).apply();
                            recreate();
                        }
                    })
                    .show();
        });

        inner.addView(langRow);
        addSettingsDivider(inner);

        addSettingsSectionHeader(inner, Strings.get("settings.section_progress"));

        TextView recView = new TextView(this);
        float maxH = prefs.getMaxHeight();
        recView.setText(maxH > 0f
                ? Strings.fmt("settings.record_best_fmt", maxH)
                : Strings.get("settings.record_none"));
        recView.setTextColor(Color.parseColor("#00E676"));
        recView.setTextSize(15f);
        recView.setPadding(0, dpToPx(8), 0, dpToPx(4));
        inner.addView(recView);

        TextView goldView = new TextView(this);
        goldView.setText(Strings.fmt("settings.gold_total_fmt", prefs.getGold()));
        goldView.setTextColor(Color.parseColor("#FFD700"));
        goldView.setTextSize(15f);
        goldView.setPadding(0, 0, 0, dpToPx(4));
        inner.addView(goldView);

        TextView diamView = new TextView(this);
        diamView.setText(Strings.fmt("settings.diamonds_fmt", prefs.getDiamonds()));
        diamView.setTextColor(Color.parseColor("#80DEEA"));
        diamView.setTextSize(15f);
        inner.addView(diamView);

        addSettingsDivider(inner);

        addSettingsSectionHeader(inner, Strings.get("settings.section_danger"));

        Button resetBtn = new Button(this);
        resetBtn.setText(Strings.get("settings.btn_reset"));
        resetBtn.setTextColor(Color.WHITE);
        resetBtn.setBackgroundColor(Color.parseColor("#7F0000"));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.setMargins(0, dpToPx(8), 0, 0);
        resetBtn.setLayoutParams(resetLp);
        resetBtn.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setTitle(Strings.get("settings.dialog_reset_title"))
                .setMessage(Strings.get("settings.dialog_reset_msg"))
                .setPositiveButton(Strings.get("settings.dialog_reset_yes"), (d, w) -> {
                    prefs.resetAll();
                    refreshRecordDisplay();
                    refreshEggButton();
                    hideOverlay(overlay);
                    Toast.makeText(this, Strings.get("settings.toast_reset_done"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(Strings.get("common.btn_cancel"), null)
                .show());
        inner.addView(resetBtn);

        Button backBtn = new Button(this);
        backBtn.setText(Strings.get("common.btn_back"));
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

    private FrameLayout buildEggOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(235, 5, 10, 20));
        eggContentFrame = new FrameLayout(this);
        overlay.addView(eggContentFrame, matchParentFl());
        return overlay;
    }

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

        LinearLayout tabRow1 = new LinearLayout(this);
        tabRow1.setOrientation(LinearLayout.HORIZONTAL);

        TextView upgradesTab  = makeTab(Strings.get("shop.tab_upgrades"));
        TextView cosmeticsTab = makeTab(Strings.get("shop.tab_cosmetics"));
        TextView gachaTab     = makeTab(Strings.get("shop.tab_gacha"));

        upgradesTab.setLayoutParams( new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cosmeticsTab.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        gachaTab.setLayoutParams(    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tabRow1.addView(upgradesTab);
        tabRow1.addView(cosmeticsTab);
        tabRow1.addView(gachaTab);

        sheet.addView(tabRow1, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

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
            goldTv.setText(Strings.fmt("common.currency_gold_fmt", prefs.getGold()));
            diamTv.setText(Strings.fmt("common.currency_diam_fmt", prefs.getDiamonds()));
        });
        refreshCurr.run();

        Object[][] UPGRADES = {
                {Strings.get("shop.upgrades.upg_air.name"),            Strings.get("shop.upgrades.upg_air.desc"),             50,  5,  10, "upg_air"},
                {Strings.get("shop.upgrades.upg_elastic.name"),        Strings.get("shop.upgrades.upg_elastic.desc"),         80,  8,  10, "upg_elastic"},
                {Strings.get("shop.upgrades.upg_boost.name"),          Strings.get("shop.upgrades.upg_boost.desc"),          150, 15,   5, "upg_boost"},
                {Strings.get("shop.upgrades.upg_boost_recharge.name"), Strings.get("shop.upgrades.upg_boost_recharge.desc"), 100, 10,   5, "upg_boost_recharge"},
                {Strings.get("shop.upgrades.upg_ink_reserve.name"),    Strings.get("shop.upgrades.upg_ink_reserve.desc"),     60,  6,  10, "upg_ink_reserve"},
                {Strings.get("shop.upgrades.upg_ink_eff.name"),        Strings.get("shop.upgrades.upg_ink_eff.desc"),         70,  7,  10, "upg_ink_eff"},
                {Strings.get("shop.upgrades.upg_gold_mult.name"),      Strings.get("shop.upgrades.upg_gold_mult.desc"),      200, 20, 100, "upg_gold_mult"},
                {Strings.get("shop.upgrades.upg_warp.name"),           Strings.get("shop.upgrades.upg_warp.desc"),           300, 30,   5, "upg_warp"},
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
        retourBtn.setText(Strings.get("common.btn_back"));
        retourBtn.setTextColor(Color.WHITE);
        retourBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));
        retourBtn.setOnClickListener(v -> hideOverlay(overlay));
        bottomBar.addView(retourBtn);
        sheet.addView(bottomBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView[] pageTabs = {upgradesTab, cosmeticsTab};
        View[]     pages    = {upgradesScroll, cosmeticsPage};

        Runnable activateUpgrades = () -> {
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
                pageTabs[i].setBackgroundColor(Color.parseColor(i == 0 ? "#111E2C" : "#0A1520"));
                pageTabs[i].setTextColor(Color.parseColor(i == 0 ? "#FFFFFF" : "#888888"));
                pageTabs[i].setAlpha(i == 0 ? 1f : 0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#0A1520"));
            gachaTab.setTextColor(Color.parseColor("#888888"));
            gachaTab.setAlpha(0.8f);
            refreshCurr.run();
        };
        Runnable activateCosmetics = () -> {
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == 1 ? View.VISIBLE : View.GONE);
                pageTabs[i].setBackgroundColor(Color.parseColor(i == 1 ? "#111E2C" : "#0A1520"));
                pageTabs[i].setTextColor(Color.parseColor(i == 1 ? "#FFFFFF" : "#888888"));
                pageTabs[i].setAlpha(i == 1 ? 1f : 0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#0A1520"));
            gachaTab.setTextColor(Color.parseColor("#888888"));
            gachaTab.setAlpha(0.8f);
            CosmeticsPage.refreshAll(cosmeticsPage);
        };

        upgradesTab.setOnClickListener(v -> activateUpgrades.run());
        cosmeticsTab.setOnClickListener(v -> activateCosmetics.run());

        gachaTab.setOnClickListener(v -> {
            gachaTab.setBackgroundColor(Color.parseColor("#111E2C"));
            gachaTab.setTextColor(Color.parseColor("#FFD700"));
            gachaTab.setAlpha(1f);
            if (gachaOverlay != null) {
                Object r = gachaOverlay.getTag(R.id.tag_refresh);
                if (r instanceof Runnable) ((Runnable) r).run();
                gachaOverlay.setVisibility(View.VISIBLE);
            }
        });

        activateUpgrades.run();

        return overlay;
    }

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
            levelView.setText(Strings.fmt("common.level_fmt", lvl, maxLvl));
            if (lvl >= maxLvl) {
                goldBtn.setText(Strings.get("common.level_max")); goldBtn.setEnabled(false);
                diamBtn.setText(Strings.get("common.level_max")); diamBtn.setEnabled(false);
            } else {
                goldBtn.setText(Strings.fmt("common.currency_gold_fmt", goldCost));
                diamBtn.setText(Strings.fmt("common.currency_diam_fmt", diamCost));
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

    private ImageView makeImageBtn(int resId, View.OnClickListener listener) {
        ImageView btn = new ImageView(this);
        btn.setImageResource(resId);
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int size = dpToPx(64);
        int pad = dpToPx(14);
        btn.setPadding(pad, pad, pad, pad);
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
        btn.setText(Strings.get("common.btn_back"));
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

    private void showEggOverlay() {
        showOverlay(eggOverlay);
        eggContentFrame.removeAllViews();
        if (EggHatchManager.shouldShowHatchAnimation(prefs)) {
            startHatchAnimation(eggContentFrame, () -> {
                EggHatchManager.completeHatch(prefs);
                refreshEggButton();
                runOnUiThread(() -> showPostHatchState(eggContentFrame));
            });
        } else if (prefs.hasHatched()) {
            showPostHatchState(eggContentFrame);
        } else {
            showLockedEggState(eggContentFrame);
        }
    }

    private void showLockedEggState(FrameLayout container) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(48));

        TextView eggTv = new TextView(this);
        eggTv.setText("🥚");
        eggTv.setTextSize(96f);
        eggTv.setGravity(Gravity.CENTER);
        layout.addView(eggTv);

        TextView hintTv = new TextView(this);
        hintTv.setText(Strings.get("egg.hint"));
        hintTv.setTextColor(Color.parseColor("#AAAAAA"));
        hintTv.setTextSize(18f);
        hintTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintLp.setMargins(0, dpToPx(24), 0, dpToPx(10));
        hintTv.setLayoutParams(hintLp);
        layout.addView(hintTv);

        float maxH = prefs.getMaxHeight();
        TextView progressTv = new TextView(this);
        progressTv.setText(Strings.fmt("egg.progress_fmt", maxH));
        progressTv.setTextColor(Color.parseColor("#FFD700"));
        progressTv.setTextSize(15f);
        progressTv.setGravity(Gravity.CENTER);
        layout.addView(progressTv);

        Button backBtn = makeRetourBtn(v -> hideOverlay(eggOverlay));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        backLp.topMargin = dpToPx(40);
        backBtn.setLayoutParams(backLp);
        layout.addView(backBtn);

        container.addView(layout, matchParentFl());
    }

    private void startHatchAnimation(FrameLayout container, Runnable onComplete) {
        container.removeAllViews();

        TextView eggView = new TextView(this);
        eggView.setText("🥚");
        eggView.setTextSize(96f);
        eggView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        elp.gravity = Gravity.CENTER;
        container.addView(eggView, elp);

        View flashView = new View(this);
        flashView.setBackgroundColor(Color.WHITE);
        flashView.setAlpha(0f);
        container.addView(flashView, matchParentFl());

        ObjectAnimator shake1 = ObjectAnimator.ofFloat(eggView, "translationX",
                0f, -8f, 8f, -8f, 8f, -6f, 6f, -6f, 6f, 0f);
        shake1.setDuration(1800);

        ObjectAnimator shake2 = ObjectAnimator.ofFloat(eggView, "translationX",
                0f, -22f, 22f, -28f, 28f, -22f, 22f, -32f, 32f, -26f, 26f, 0f);
        shake2.setDuration(1400);

        ObjectAnimator flashIn = ObjectAnimator.ofFloat(flashView, "alpha", 0f, 1f);
        flashIn.setDuration(350);

        ObjectAnimator flashOut = ObjectAnimator.ofFloat(flashView, "alpha", 1f, 0f);
        flashOut.setDuration(750);
        flashOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                eggView.setText("👽");
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(shake1, shake2, flashIn, flashOut);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onComplete.run();
            }
        });
        set.start();
    }

    private void showPostHatchState(FrameLayout container) {
        container.removeAllViews();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(48));

        TextView alienTv = new TextView(this);
        alienTv.setText("👽");
        alienTv.setTextSize(96f);
        alienTv.setGravity(Gravity.CENTER);
        layout.addView(alienTv);

        android.graphics.drawable.GradientDrawable bubbleBgD = new android.graphics.drawable.GradientDrawable();
        bubbleBgD.setColor(Color.parseColor("#0D2010"));
        bubbleBgD.setCornerRadius(dpToPx(16));
        bubbleBgD.setStroke(dpToPx(2), Color.parseColor("#00E676"));

        FrameLayout bubble = new FrameLayout(this);
        bubble.setBackground(bubbleBgD);
        bubble.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));

        TextView dialogTv = new TextView(this);
        dialogTv.setText(Strings.get("egg.alien_dialog"));
        dialogTv.setTextColor(Color.WHITE);
        dialogTv.setTextSize(20f);
        dialogTv.setGravity(Gravity.CENTER);
        bubble.addView(dialogTv);

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleLp.setMargins(0, dpToPx(24), 0, dpToPx(36));
        bubble.setLayoutParams(bubbleLp);
        layout.addView(bubble);

        Button colonyBtn = new Button(this);
        colonyBtn.setText(Strings.get("egg.btn_build_colony"));
        colonyBtn.setTextSize(17f);
        colonyBtn.setTextColor(Color.parseColor("#0A0A0A"));
        android.graphics.drawable.GradientDrawable colonyBg = new android.graphics.drawable.GradientDrawable();
        colonyBg.setColor(Color.parseColor("#FFD700"));
        colonyBg.setCornerRadius(dpToPx(12));
        colonyBtn.setBackground(colonyBg);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        colLp.setMargins(0, 0, 0, 0);
        colonyBtn.setLayoutParams(colLp);
        colonyBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ColonyActivity.class)));
        layout.addView(colonyBtn);

        Button backBtn = makeRetourBtn(v -> hideOverlay(eggOverlay));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        backLp.topMargin = dpToPx(20);
        backBtn.setLayoutParams(backLp);
        layout.addView(backBtn);

        container.addView(layout, matchParentFl());
    }

    private void refreshEggButton() {
        if (eggBtn == null) return;
        if (EggHatchManager.shouldShowHatchAnimation(prefs)) {
            GradientDrawable glowBg = new GradientDrawable();
            glowBg.setShape(GradientDrawable.OVAL);
            glowBg.setColor(Color.parseColor("#FFFDE7"));
            glowBg.setStroke(dpToPx(3), Color.parseColor("#FFD700"));
            eggBtn.setBackground(glowBg);
            if (eggBtnAnimator != null) eggBtnAnimator.cancel();
            eggBtnAnimator = ObjectAnimator.ofFloat(eggBtn, "alpha", 1f, 0.35f);
            eggBtnAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            eggBtnAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            eggBtnAnimator.setDuration(650);
            eggBtnAnimator.start();
        } else {
            if (eggBtnAnimator != null) {
                eggBtnAnimator.cancel();
                eggBtnAnimator = null;
            }
            eggBtn.setAlpha(1f);
            GradientDrawable normalBg = new GradientDrawable();
            normalBg.setShape(GradientDrawable.OVAL);
            normalBg.setColor(Color.WHITE);
            normalBg.setStroke(dpToPx(2), Color.parseColor("#CCCCCC"));
            eggBtn.setBackground(normalBg);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (gachaOverlay != null && gachaOverlay.getVisibility() == View.VISIBLE) {
            gachaOverlay.setVisibility(View.GONE);
        } else if (settingsOverlay != null && settingsOverlay.getVisibility() == View.VISIBLE) {
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