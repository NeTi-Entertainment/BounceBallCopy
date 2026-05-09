package com.example.bounceball;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import com.example.bounceball.colony.ColonyActivity;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
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
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

public class MainActivity extends Activity implements GameView.GameStateListener {

    private static final String[] DIAMOND_PRODUCT_IDS = {
            "diamonds_25", "diamonds_80", "diamonds_230", "diamonds_480", "diamonds_1000"
    };
    private static final int[] DIAMOND_TOTAL_AMOUNTS = {25, 80, 230, 480, 1000};
    private static final int[] DIAMOND_BONUS_PERCENT = {0, 10, 20, 35, 50};
    private static final String[] DIAMOND_PRICE_KEYS = {
            "shop.diamond_price_5", "shop.diamond_price_20", "shop.diamond_price_50",
            "shop.diamond_price_100", "shop.diamond_price_1000"
    };
    private static final String DONATION_PRODUCT_ID = "donation_custom_amount";

    private GameView gameView;
    private GamePreferences prefs;
    private BillingClient billingClient;

    private ImageView shopBtn, eggBtn, settingsBtn;
    private TextView tapText;
    private TextView recordText;
    private boolean inGame = false;

    private FrameLayout settingsOverlay;
    private FrameLayout shopOverlay;
    private FrameLayout eggOverlay;

    private ScrollView cosmeticsScrollView;
    private FrameLayout gachaOverlay;
    private FrameLayout resultOverlay;
    private FrameLayout eggContentFrame;
    private ObjectAnimator eggBtnAnimator;

    private boolean reviveUsedThisRun = false;
    private float continuedFromHeight = 0f;
    private long continuedDurationMs = 0L;
    private int accumulatedRunGoldEarned = 0;
    private float pendingResultHeight = 0f;
    private long pendingResultDurationMs = 0L;
    private int pendingResultGoldEarned = 0;
    private Runnable openDiamondShopPage;

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
        setupBillingClient();

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
        resultOverlay   = buildResultOverlay();
        root.addView(settingsOverlay, matchParentFl());
        root.addView(shopOverlay,     matchParentFl());
        root.addView(eggOverlay,      matchParentFl());
        root.addView(gachaOverlay,    matchParentFl());
        root.addView(resultOverlay,   matchParentFl());

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
    public void onGameOver(float heightReached, long durationMillis) {
        UpgradeStats upgrades = UpgradeStats.fromPrefs(prefs.getRaw());
        float goldHeight = Math.max(0f, heightReached - continuedFromHeight);
        int goldEarned = (int) Math.floor(goldHeight * upgrades.goldMultiplier);
        prefs.addGold(goldEarned);
        pendingResultGoldEarned = accumulatedRunGoldEarned + goldEarned;
        pendingResultHeight = heightReached;
        pendingResultDurationMs = durationMillis;

        boolean newRecord = prefs.updateMaxHeight(heightReached);
        EggHatchManager.checkAndSetReady(prefs, heightReached);

        runOnUiThread(() -> {
            inGame = false;
            gameView.setHudVisible(false);
            shopBtn.setVisibility(View.GONE);
            eggBtn.setVisibility(View.GONE);
            settingsBtn.setVisibility(View.GONE);
            tapText.clearAnimation();
            tapText.setVisibility(View.GONE);
            recordText.setVisibility(View.GONE);
            refreshRecordDisplay();
            refreshEggButton();
            showResultOverlay(
                    heightReached,
                    prefs.getMaxHeight(),
                    pendingResultGoldEarned,
                    prefs.getGold(),
                    durationMillis,
                    !reviveUsedThisRun);

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
        Object r = overlay.getTag(R.id.tag_refresh);
        if (r instanceof Runnable) ((Runnable) r).run();
        overlay.setVisibility(View.VISIBLE);

    //    tapText.clearAnimation();
    //    tapText.setVisibility(View.GONE);
    //    recordText.setVisibility(View.GONE);
    //    if (overlay == settingsOverlay) {
    //        Object r = overlay.getTag(R.id.tag_refresh);
    //        if (r instanceof Runnable) ((Runnable) r).run();
    //    }
    //    overlay.setVisibility(View.VISIBLE);
    //    if (overlay == shopOverlay && cosmeticsScrollView != null) {
    //        CosmeticsPage.refreshAll(cosmeticsScrollView);
    //    }
    }

    public void refreshShopUI() {
        if (shopOverlay != null) {
            Object r = shopOverlay.getTag(R.id.tag_refresh);
            if (r instanceof Runnable) ((Runnable) r).run();
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

    private FrameLayout buildResultOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        return overlay;
    }

    private void showResultOverlay(float heightReached, float maxHeight, int goldEarned,
                                   int totalGold, long durationMillis, boolean canRevive) {
        resultOverlay.removeAllViews();
        resultOverlay.setVisibility(View.VISIBLE);

        LinearLayout statsCol = new LinearLayout(this);
        statsCol.setOrientation(LinearLayout.VERTICAL);
        statsCol.setGravity(Gravity.CENTER_HORIZONTAL);
        statsCol.setPadding(dpToPx(28), 0, dpToPx(28), 0);
        FrameLayout.LayoutParams statsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        statsLp.gravity = Gravity.CENTER;
        resultOverlay.addView(statsCol, statsLp);

        TextView title = makeResultText(Strings.get("result.title"), 30f, Color.WHITE);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        statsCol.addView(title);

        statsCol.addView(makeResultText(Strings.fmt("result.height_fmt", heightReached), 20f, Color.WHITE));
        statsCol.addView(makeResultText(Strings.fmt("result.record_fmt", maxHeight), 18f, Color.parseColor("#FFD700")));
        TextView goldResultView = makeResultText(Strings.fmt("result.gold_fmt", goldEarned, totalGold),
                18f, Color.parseColor("#FFD700"));
        statsCol.addView(goldResultView);
        statsCol.addView(makeResultText(Strings.fmt("result.duration_fmt", formatDuration(durationMillis)),
                18f, Color.WHITE));

        if (canRevive) {
            Button continueBtn = new Button(this);
            continueBtn.setText(Strings.get("result.revive_btn"));
            continueBtn.setTextSize(17f);
            continueBtn.setTextColor(Color.parseColor("#0A0A0A"));
            setRoundedBackground(continueBtn, "#FFD700", 12);
            LinearLayout.LayoutParams continueLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            continueLp.setMargins(0, dpToPx(22), 0, 0);
            continueBtn.setLayoutParams(continueLp);
            continueBtn.setOnClickListener(v -> {
                continueBtn.setEnabled(false);
                continueBtn.setText(Strings.get("result.ad_loading"));
                AdManager.getInstance().showRewarded(this, new AdManager.RewardedCallback() {
                    @Override
                    public void onRewardEarned() {
                        runOnUiThread(() -> continueFromResult());
                    }

                    @Override
                    public void onUnavailable() {
                        runOnUiThread(() -> {
                            continueBtn.setEnabled(true);
                            continueBtn.setText(Strings.get("result.revive_btn"));
                            Toast.makeText(MainActivity.this,
                                    Strings.get("result.ad_unavailable"),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onClosedWithoutReward() {
                        runOnUiThread(() -> {
                            continueBtn.setEnabled(true);
                            continueBtn.setText(Strings.get("result.revive_btn"));
                        });
                    }
                });
            });
            statsCol.addView(continueBtn);
        }

        if (goldEarned > 0) {
            Button doubleGoldBtn = new Button(this);
            doubleGoldBtn.setText(Strings.get("result.double_gold_btn"));
            doubleGoldBtn.setTextSize(17f);
            doubleGoldBtn.setTextColor(Color.parseColor("#0A0A0A"));
            setRoundedBackground(doubleGoldBtn, "#FFC107", 12);
            LinearLayout.LayoutParams doubleLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            doubleLp.setMargins(0, dpToPx(12), 0, 0);
            doubleGoldBtn.setLayoutParams(doubleLp);
            doubleGoldBtn.setOnClickListener(v -> {
                doubleGoldBtn.setEnabled(false);
                doubleGoldBtn.setText(Strings.get("result.ad_loading"));
                AdManager.getInstance().showRewarded(this, new AdManager.RewardedCallback() {
                    @Override
                    public void onRewardEarned() {
                        runOnUiThread(() -> {
                            int extraGold = pendingResultGoldEarned;
                            prefs.addGold(extraGold);
                            pendingResultGoldEarned += extraGold;
                            goldResultView.setText(Strings.fmt("result.gold_fmt",
                                    pendingResultGoldEarned, prefs.getGold()));
                            Toast.makeText(MainActivity.this,
                                    Strings.fmt("result.double_gold_toast", pendingResultGoldEarned),
                                    Toast.LENGTH_SHORT).show();
                            returnToMainMenuFromResults(false);
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        runOnUiThread(() -> {
                            doubleGoldBtn.setEnabled(true);
                            doubleGoldBtn.setText(Strings.get("result.double_gold_btn"));
                            Toast.makeText(MainActivity.this,
                                    Strings.get("result.ad_unavailable"),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onClosedWithoutReward() {
                        runOnUiThread(() -> {
                            doubleGoldBtn.setEnabled(true);
                            doubleGoldBtn.setText(Strings.get("result.double_gold_btn"));
                        });
                    }
                });
            });
            statsCol.addView(doubleGoldBtn);
        }

        Button menuBtn = new Button(this);
        menuBtn.setText(Strings.get("result.main_menu"));
        menuBtn.setTextSize(20f);
        menuBtn.setTextColor(Color.WHITE);
        setRoundedBackground(menuBtn, "#1B3A1B", 14);
        menuBtn.setOnClickListener(v -> returnToMainMenuFromResults());
        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        menuLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        menuLp.leftMargin = dpToPx(36);
        menuLp.rightMargin = dpToPx(36);
        menuLp.bottomMargin = dpToPx(42);
        resultOverlay.addView(menuBtn, menuLp);
    }

    private TextView makeResultText(String text, float sizeSp, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(5f, 0f, 2f, Color.BLACK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(7), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void continueFromResult() {
        reviveUsedThisRun = true;
        continuedFromHeight = pendingResultHeight;
        continuedDurationMs = pendingResultDurationMs;
        accumulatedRunGoldEarned = pendingResultGoldEarned;

        resultOverlay.setVisibility(View.GONE);
        gameView.prepareContinueFrom(continuedFromHeight, continuedDurationMs);
        gameView.setHudVisible(true);
        inGame = true;

        shopBtn.setVisibility(View.GONE);
        eggBtn.setVisibility(View.GONE);
        settingsBtn.setVisibility(View.GONE);
        recordText.setVisibility(View.GONE);
        tapText.setVisibility(View.VISIBLE);
        startPulse();
    }

    private void returnToMainMenuFromResults() {
        returnToMainMenuFromResults(true);
    }

    private void returnToMainMenuFromResults(boolean allowInterstitial) {
        resultOverlay.setVisibility(View.GONE);
        if (allowInterstitial) {
            AdManager.getInstance().onGameOver(this);
        }

        reviveUsedThisRun = false;
        continuedFromHeight = 0f;
        continuedDurationMs = 0L;
        accumulatedRunGoldEarned = 0;
        pendingResultHeight = 0f;
        pendingResultDurationMs = 0L;
        pendingResultGoldEarned = 0;

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
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds);
    }

    private FrameLayout buildSettingsOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Color.argb(220, 10, 20, 40));

        FrameLayout pageHost = new FrameLayout(this);
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

        Button aboutBtn = new Button(this);
        aboutBtn.setText(Strings.get("settings.about_title"));
        aboutBtn.setTextColor(Color.WHITE);
        setRoundedBackground(aboutBtn, "#1A2A3A", 10);
        LinearLayout.LayoutParams aboutLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aboutLp.setMargins(0, dpToPx(8), 0, 0);
        aboutBtn.setLayoutParams(aboutLp);
        inner.addView(aboutBtn);

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
        setRoundedBackground(backBtn, "#1B5E20", 10);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backLp.setMargins(0, dpToPx(24), 0, 0);
        backBtn.setLayoutParams(backLp);
        backBtn.setOnClickListener(v -> hideOverlay(overlay));
        inner.addView(backBtn);

        scroll.addView(inner);
        pageHost.addView(scroll, matchParentFl());

        final ScrollView[] aboutPageRef = new ScrollView[1];
        ScrollView aboutPage = buildAboutSettingsPage(() -> {
            if (aboutPageRef[0] != null) aboutPageRef[0].setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);
        });
        aboutPageRef[0] = aboutPage;
        aboutPage.setVisibility(View.GONE);
        pageHost.addView(aboutPage, matchParentFl());
        Runnable resetSettingsPage = () -> {
            aboutPage.setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);
        };
        overlay.setTag(R.id.tag_refresh, resetSettingsPage);

        aboutBtn.setOnClickListener(v -> {
            scroll.setVisibility(View.GONE);
            aboutPage.setVisibility(View.VISIBLE);
        });

        overlay.addView(pageHost, matchParentFl());
        return overlay;
    }

    private ScrollView buildAboutSettingsPage(Runnable onBack) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        inner.setPadding(dpToPx(32), dpToPx(60), dpToPx(32), dpToPx(40));

        Button backBtn = new Button(this);
        backBtn.setText(Strings.get("common.btn_back"));
        backBtn.setTextColor(Color.WHITE);
        setRoundedBackground(backBtn, "#1B5E20", 10);
        backBtn.setOnClickListener(v -> onBack.run());
        inner.addView(backBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(Strings.get("settings.about_title"));
        title.setTextSize(26f);
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dpToPx(28), 0, dpToPx(18));
        title.setLayoutParams(titleLp);
        inner.addView(title);

        TextView aboutText = new TextView(this);
        aboutText.setText(Strings.get("settings.about_body"));
        aboutText.setTextColor(Color.WHITE);
        aboutText.setTextSize(16f);
        aboutText.setLineSpacing(dpToPx(3), 1.05f);
        aboutText.setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16));
        setRoundedBackground(aboutText, "#1A2A3A", 12);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.setMargins(0, 0, 0, dpToPx(20));
        aboutText.setLayoutParams(textLp);
        inner.addView(aboutText);

        TextView donationLabel = new TextView(this);
        donationLabel.setText(Strings.get("settings.donation_label"));
        donationLabel.setTextColor(Color.parseColor("#80DEEA"));
        donationLabel.setTextSize(14f);
        LinearLayout.LayoutParams donationLabelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        donationLabelLp.setMargins(0, 0, 0, dpToPx(6));
        donationLabel.setLayoutParams(donationLabelLp);
        inner.addView(donationLabel);

        EditText donationAmountInput = new EditText(this);
        donationAmountInput.setHint(Strings.get("settings.donation_hint"));
        donationAmountInput.setSingleLine(true);
        donationAmountInput.setTextColor(Color.WHITE);
        donationAmountInput.setHintTextColor(Color.parseColor("#88FFFFFF"));
        donationAmountInput.setTextSize(18f);
        donationAmountInput.setGravity(Gravity.CENTER);
        donationAmountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        donationAmountInput.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789,."));
        setRoundedBackground(donationAmountInput, "#111E2C", 10);
        LinearLayout.LayoutParams amountLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountLp.setMargins(0, 0, 0, dpToPx(10));
        donationAmountInput.setLayoutParams(amountLp);
        inner.addView(donationAmountInput);

        Button coffeeBtn = new Button(this);
        coffeeBtn.setText(Strings.get("settings.donation_cta"));
        coffeeBtn.setTextColor(Color.parseColor("#2A1800"));
        setRoundedBackground(coffeeBtn, "#FFD27A", 12);
        coffeeBtn.setOnClickListener(v -> launchDonationPayment(donationAmountInput.getText().toString()));
        inner.addView(coffeeBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView donationNote = new TextView(this);
        donationNote.setText(Strings.get("settings.donation_note"));
        donationNote.setTextColor(Color.parseColor("#777777"));
        donationNote.setTextSize(11f);
        donationNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams donationNoteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        donationNoteLp.setMargins(0, dpToPx(10), 0, 0);
        donationNote.setLayoutParams(donationNoteLp);
        inner.addView(donationNote);

        scroll.addView(inner);
        return scroll;
    }

    private void launchDonationPayment(String rawAmount) {
        String normalized = rawAmount == null ? "" : rawAmount.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            Toast.makeText(this, Strings.get("settings.donation_empty"), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            float amount = Float.parseFloat(normalized);
            if (amount <= 0f) {
                Toast.makeText(this, Strings.get("settings.donation_invalid"), Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, Strings.get("settings.donation_invalid"), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,
                Strings.get("settings.donation_unconfigured"),
                Toast.LENGTH_LONG).show();
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
        sheetBg.setCornerRadius(dpToPx(18));
        sheet.setBackground(sheetBg);
        sheet.setClipToOutline(true);

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

        Button buyDiamondsBtn = new Button(this);
        buyDiamondsBtn.setText(Strings.get("shop.buy_diamonds"));
        buyDiamondsBtn.setTextColor(Color.parseColor("#80DEEA"));
        buyDiamondsBtn.setTextSize(15f);
        setRoundedBackground(buyDiamondsBtn, "#10263A", 8);
        LinearLayout.LayoutParams buyDiamondsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buyDiamondsLp.setMargins(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(6));
        buyDiamondsBtn.setLayoutParams(buyDiamondsLp);
        sheet.addView(buyDiamondsBtn);

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

        ScrollView diamondsPage = buildDiamondsShopPage(refreshCurr);
        diamondsPage.setVisibility(View.GONE);
        content.addView(diamondsPage, matchParentFl());

        sheet.addView(content);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(0, dpToPx(10), 0, dpToPx(14));
        bottomBar.setBackgroundColor(Color.parseColor("#0D1B2A"));
        Button retourBtn = new Button(this);
        retourBtn.setText(Strings.get("common.btn_back"));
        retourBtn.setTextColor(Color.WHITE);
        setRoundedBackground(retourBtn, "#1B3A1B", 10);
        retourBtn.setOnClickListener(v -> hideOverlay(overlay));
        bottomBar.addView(retourBtn);
        sheet.addView(bottomBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView[] pageTabs = {upgradesTab, cosmeticsTab};
        View[]     pages    = {upgradesScroll, cosmeticsPage, diamondsPage};

        Runnable activateUpgrades = () -> {
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            }
            for (int i = 0; i < pageTabs.length; i++) {
                pageTabs[i].setBackgroundColor(Color.parseColor(i == 0 ? "#111E2C" : "#0A1520"));
                pageTabs[i].setTextColor(Color.parseColor(i == 0 ? "#FFFFFF" : "#888888"));
                pageTabs[i].setAlpha(i == 0 ? 1f : 0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#0A1520"));
            gachaTab.setTextColor(Color.parseColor("#888888"));
            gachaTab.setAlpha(0.8f);
            setRoundedBackground(buyDiamondsBtn, "#10263A", 8);
            buyDiamondsBtn.setTextColor(Color.parseColor("#80DEEA"));
            refreshCurr.run();
        };
        Runnable activateCosmetics = () -> {
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == 1 ? View.VISIBLE : View.GONE);
            }
            for (int i = 0; i < pageTabs.length; i++) {
                pageTabs[i].setBackgroundColor(Color.parseColor(i == 1 ? "#111E2C" : "#0A1520"));
                pageTabs[i].setTextColor(Color.parseColor(i == 1 ? "#FFFFFF" : "#888888"));
                pageTabs[i].setAlpha(i == 1 ? 1f : 0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#0A1520"));
            gachaTab.setTextColor(Color.parseColor("#888888"));
            gachaTab.setAlpha(0.8f);
            setRoundedBackground(buyDiamondsBtn, "#10263A", 8);
            buyDiamondsBtn.setTextColor(Color.parseColor("#80DEEA"));
            CosmeticsPage.refreshAll(cosmeticsPage);
        };
        Runnable activateDiamonds = () -> {
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == 2 ? View.VISIBLE : View.GONE);
            }
            for (TextView tab : pageTabs) {
                tab.setBackgroundColor(Color.parseColor("#0A1520"));
                tab.setTextColor(Color.parseColor("#888888"));
                tab.setAlpha(0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#0A1520"));
            gachaTab.setTextColor(Color.parseColor("#888888"));
            gachaTab.setAlpha(0.8f);
            setRoundedBackground(buyDiamondsBtn, "#1A3D5A", 8);
            buyDiamondsBtn.setTextColor(Color.WHITE);
            refreshCurr.run();
        };
        openDiamondShopPage = () -> {
            if (gachaOverlay != null) gachaOverlay.setVisibility(View.GONE);
            if (shopOverlay != null && shopOverlay.getVisibility() != View.VISIBLE) {
                showOverlay(shopOverlay);
            }
            activateDiamonds.run();
        };

        upgradesTab.setOnClickListener(v -> activateUpgrades.run());
        cosmeticsTab.setOnClickListener(v -> activateCosmetics.run());
        buyDiamondsBtn.setOnClickListener(v -> activateDiamonds.run());

        gachaTab.setOnClickListener(v -> {
            for (TextView tab : pageTabs) {
                tab.setBackgroundColor(Color.parseColor("#0A1520"));
                tab.setTextColor(Color.parseColor("#888888"));
                tab.setAlpha(0.8f);
            }
            gachaTab.setBackgroundColor(Color.parseColor("#111E2C"));
            gachaTab.setTextColor(Color.parseColor("#FFD700"));
            gachaTab.setAlpha(1f);
            setRoundedBackground(buyDiamondsBtn, "#10263A", 8);
            buyDiamondsBtn.setTextColor(Color.parseColor("#80DEEA"));
            if (gachaOverlay != null) {
                Object r = gachaOverlay.getTag(R.id.tag_refresh);
                if (r instanceof Runnable) ((Runnable) r).run();
                gachaOverlay.setVisibility(View.VISIBLE);
            }
        });

        activateUpgrades.run();

        Runnable refreshShop = () -> {
            refreshCurr.run();
            for (int i = 0; i < upgradesPage.getChildCount(); i++) {
                View child = upgradesPage.getChildAt(i);
                Object r = child.getTag(R.id.tag_refresh);
                if (r instanceof Runnable) ((Runnable) r).run();
            }
            if (cosmeticsScrollView != null) {
                CosmeticsPage.refreshAll(cosmeticsScrollView);
            }
        };
        overlay.setTag(R.id.tag_refresh, refreshShop);

        return overlay;
    }

    private ScrollView buildDiamondsShopPage(Runnable refreshCurr) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(60));

        TextView info = new TextView(this);
        info.setText(Strings.get("shop.diamond_shop_info"));
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14f);
        info.setPadding(0, 0, 0, dpToPx(10));
        layout.addView(info);

        for (int i = 0; i < DIAMOND_PRODUCT_IDS.length; i++) {
            layout.addView(buildDiamondPackRow(i, refreshCurr));
        }

        TextView legal = new TextView(this);
        legal.setText(Strings.get("shop.diamond_shop_legal"));
        legal.setTextColor(Color.parseColor("#777777"));
        legal.setTextSize(11f);
        legal.setPadding(0, dpToPx(18), 0, 0);
        layout.addView(legal);

        scroll.addView(layout);
        return scroll;
    }

    private View buildDiamondPackRow(int index, Runnable refreshCurr) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        setRoundedBackground(row, "#1A2A3A", 10);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dpToPx(8), 0, 0);
        row.setLayoutParams(rowLp);

        int totalAmount = DIAMOND_TOTAL_AMOUNTS[index];
        int bonusPercent = DIAMOND_BONUS_PERCENT[index];
        int bonusAmount = bonusPercent > 0
                ? Math.round(totalAmount * bonusPercent / (100f + bonusPercent))
                : 0;

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(Strings.fmt("shop.diamond_pack_title_fmt", totalAmount));
        title.setTextColor(Color.parseColor("#80DEEA"));
        title.setTextSize(17f);
        textCol.addView(title);

        TextView bonus = new TextView(this);
        if (bonusPercent > 0) {
            bonus.setText(Strings.fmt("shop.diamond_pack_bonus_fmt", bonusPercent, bonusAmount, totalAmount));
            bonus.setTextColor(Color.parseColor("#FFD700"));
        } else {
            bonus.setText(Strings.fmt("shop.diamond_pack_total_fmt", totalAmount));
            bonus.setTextColor(Color.parseColor("#AAAAAA"));
        }
        bonus.setTextSize(12f);
        textCol.addView(bonus);

        row.addView(textCol);

        Button buyBtn = new Button(this);
        buyBtn.setText(Strings.get(DIAMOND_PRICE_KEYS[index]));
        buyBtn.setTextColor(Color.WHITE);
        buyBtn.setTextSize(13f);
        setRoundedBackground(buyBtn, "#1565C0", 8);
        buyBtn.setOnClickListener(v -> {
            launchDiamondPurchase(index);
            refreshCurr.run();
        });
        row.addView(buyBtn);
        return row;
    }

    public void showInsufficientDiamondsPopup() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(Strings.get("shop.insufficient_diam_title"))
                .setMessage(Strings.get("shop.insufficient_diam_msg"))
                .setNegativeButton(Strings.get("common.btn_close"), null)
                .setPositiveButton(Strings.get("shop.buy_diamonds"), (dialog, which) -> openDiamondPurchasePage())
                .show();
    }

    public void openDiamondPurchasePage() {
        if (openDiamondShopPage != null) {
            openDiamondShopPage.run();
        }
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
        setRoundedBackground(row, "#1A2A3A", 10);
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
            int goldCost = EconomyBalance.upgradeGoldCost(key, lvl, goldBase);
            int diamCost = EconomyBalance.upgradeDiamondCost(key, lvl, goldBase);
            levelView.setText(Strings.fmt("common.level_fmt", lvl, maxLvl));
            if (lvl >= maxLvl) {
                goldBtn.setText(Strings.get("common.level_max")); goldBtn.setEnabled(false);
                diamBtn.setText(Strings.get("common.level_max")); diamBtn.setEnabled(false);
            } else {
                goldBtn.setText(Strings.fmt("common.currency_gold_fmt", goldCost));
                diamBtn.setText(Strings.fmt("common.currency_diam_fmt", diamCost));
                goldBtn.setEnabled(prefs.getGold() >= goldCost);
                diamBtn.setEnabled(true);
            }
            refreshCurr.run();
        };

        goldBtn.setTextColor(Color.parseColor("#FFD700"));
        setRoundedBackground(goldBtn, "#1B3A1B", 8);
        goldBtn.setTextSize(12f);
        goldBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        goldBtn.setOnClickListener(v -> {
            int lvl = raw.getInt(key, 0);
            int cost = EconomyBalance.upgradeGoldCost(key, lvl, goldBase);
            if (prefs.getGold() >= cost && lvl < maxLvl) {
                prefs.spendGold(cost);
                raw.edit().putInt(key, lvl + 1).apply();
                refresh.run();
            }
        });

        diamBtn.setTextColor(Color.parseColor("#80DEEA"));
        setRoundedBackground(diamBtn, "#1A1A3A", 8);
        diamBtn.setTextSize(12f);
        diamBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.setMargins(dpToPx(10), 0, 0, 0);
        diamBtn.setLayoutParams(dLp);
        diamBtn.setOnClickListener(v -> {
            int lvl  = raw.getInt(key, 0);
            int cost = EconomyBalance.upgradeDiamondCost(key, lvl, goldBase);
            if (prefs.getDiamonds() >= cost && lvl < maxLvl) {
                prefs.spendDiamonds(cost);
                raw.edit().putInt(key, lvl + 1).apply();
                refresh.run();
            } else if (lvl < maxLvl) {
                showInsufficientDiamondsPopup();
            }
        });

        btnRow.addView(goldBtn);
        btnRow.addView(diamBtn);
        row.addView(btnRow);
        row.setTag(R.id.tag_refresh, refresh);
        refresh.run();
        return row;
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase);
                        }
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {}

            @Override
            public void onBillingServiceDisconnected() {}
        });
    }

    private void launchDiamondPurchase(int index) {
        launchInAppProduct(DIAMOND_PRODUCT_IDS[index]);
    }

    private void launchInAppProduct(String productId) {
        if (billingClient == null || !billingClient.isReady()) {
            Toast.makeText(this, Strings.get("shop.gems_unavail"), Toast.LENGTH_SHORT).show();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Arrays.asList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                ))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && !productDetailsList.isEmpty()) {
                ProductDetails productDetails = productDetailsList.get(0);
                List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                        Arrays.asList(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .build()
                        );
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();
                runOnUiThread(() -> billingClient.launchBillingFlow(this, flowParams));
            } else {
                runOnUiThread(() -> Toast.makeText(this,
                        Strings.get("shop.product_unconfigured"),
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED
                || purchase.getProducts() == null
                || purchase.getProducts().isEmpty()) {
            return;
        }

        String productId = purchase.getProducts().get(0);
        for (int i = 0; i < DIAMOND_PRODUCT_IDS.length; i++) {
            if (DIAMOND_PRODUCT_IDS[i].equals(productId)) {
                int totalAmount = DIAMOND_TOTAL_AMOUNTS[i];
                prefs.addDiamonds(totalAmount);
                runOnUiThread(() -> Toast.makeText(this,
                        Strings.fmt("shop.diamonds_received_fmt", totalAmount),
                        Toast.LENGTH_SHORT).show());
                consumePurchase(purchase);
                return;
            }
        }

        if (DONATION_PRODUCT_ID.equals(productId)) {
            runOnUiThread(() -> Toast.makeText(this,
                    Strings.get("settings.donation_thanks"),
                    Toast.LENGTH_SHORT).show());
            consumePurchase(purchase);
        }
    }

    private void consumePurchase(Purchase purchase) {
        if (billingClient != null && billingClient.isReady()) {
            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {});
        } else if (billingClient != null && !purchase.isAcknowledged()) {
            AcknowledgePurchaseParams ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(ackParams, result -> {});
        }
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
        setRoundedBackground(btn, "#1B3A1B", 10);
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

    private void setRoundedBackground(View view, String colorHex, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorHex));
        bg.setCornerRadius(dpToPx(radiusDp));
        view.setBackground(bg);
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

        FrameLayout layout = new FrameLayout(this);
        layout.setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(48));

        int tailH = dpToPx(18);
        int tailW = dpToPx(32);
        int corner = dpToPx(16);
        int stroke = dpToPx(2);

        Drawable bubbleDrawable = new Drawable() {
            private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path  path       = new Path();
            private final RectF bodyRect   = new RectF();
            { fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(Color.argb(220, 245, 255, 250));
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(stroke);
                strokePaint.setColor(Color.parseColor("#00E676"));
                strokePaint.setStrokeJoin(Paint.Join.ROUND); }

            @Override public void draw(Canvas canvas) {
                android.graphics.Rect b = getBounds();
                float half = stroke / 2f;
                float tailCenterX = b.left + b.width() * 0.34f;
                float tailTipX = tailCenterX + tailW * 1.05f;
                float bodyBottom = b.bottom - tailH;
                bodyRect.set(b.left + half, b.top + half, b.right - half, bodyBottom);
                float left = bodyRect.left;
                float top = bodyRect.top;
                float right = bodyRect.right;
                float bottom = bodyRect.bottom;
                float radius = corner;
                float tailLeft = tailCenterX - tailW * 0.42f;
                float tailRight = tailCenterX + tailW * 0.30f;
                path.reset();
                path.moveTo(left + radius, top);
                path.lineTo(right - radius, top);
                path.quadTo(right, top, right, top + radius);
                path.lineTo(right, bottom - radius);
                path.quadTo(right, bottom, right - radius, bottom);
                path.lineTo(tailRight, bottom);
                path.cubicTo(
                        tailCenterX + tailW * 0.50f, bottom + tailH * 0.10f,
                        tailCenterX + tailW * 0.72f, b.bottom - tailH * 0.05f,
                        tailTipX, b.bottom - half);
                path.cubicTo(
                        tailCenterX + tailW * 0.58f, b.bottom - tailH * 0.05f,
                        tailCenterX - tailW * 0.10f, bottom + tailH * 0.18f,
                        tailLeft, bottom);
                path.lineTo(left + radius, bottom);
                path.quadTo(left, bottom, left, bottom - radius);
                path.lineTo(left, top + radius);
                path.quadTo(left, top, left + radius, top);
                path.close();
                canvas.drawPath(path, fillPaint);
                canvas.drawPath(path, strokePaint);
            }
            @Override public void setAlpha(int a) {
                fillPaint.setAlpha(a); strokePaint.setAlpha(a);
            }
            @Override public void setColorFilter(android.graphics.ColorFilter cf) {
                fillPaint.setColorFilter(cf); strokePaint.setColorFilter(cf);
            }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };

        FrameLayout bubble = new FrameLayout(this);
        bubble.setBackground(bubbleDrawable);
        bubble.setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14) + tailH);
        bubble.setVisibility(View.GONE);
        bubble.setAlpha(0f);

        TextView dialogTv = new TextView(this);
        dialogTv.setTextColor(Color.parseColor("#0A2010"));
        dialogTv.setTextSize(16f);
        dialogTv.setGravity(Gravity.CENTER);
        dialogTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        dialogTv.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        dialogTv.setLineSpacing(0f, 1.25f);
        bubble.addView(dialogTv);

        FrameLayout.LayoutParams fixedBubbleLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        fixedBubbleLp.gravity = Gravity.CENTER;
        fixedBubbleLp.leftMargin = dpToPx(4);
        fixedBubbleLp.rightMargin = dpToPx(4);
        bubble.setTranslationY(-dpToPx(150));
        layout.addView(bubble, fixedBubbleLp);

        LinearLayout alienControls = new LinearLayout(this);
        alienControls.setOrientation(LinearLayout.VERTICAL);
        alienControls.setGravity(Gravity.CENTER_HORIZONTAL);
        alienControls.setTranslationY(dpToPx(24));
        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.CENTER;

        ImageView alienTv = new ImageView(this);
        alienTv.setImageResource(R.drawable.alien_head);
        LinearLayout.LayoutParams alienLp = new LinearLayout.LayoutParams(dpToPx(120), dpToPx(120));
        alienLp.gravity = Gravity.CENTER;
        alienTv.setLayoutParams(alienLp);
        alienTv.setClickable(true);
        alienTv.setFocusable(true);
        alienControls.addView(alienTv);

        String[] quotes = loadIdleQuotes();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] hideRunnable = {null};

        alienTv.setOnClickListener(v -> {
            String quote = quotes[(int) (Math.random() * quotes.length)];
            dialogTv.setText(quote);

            if (hideRunnable[0] != null) handler.removeCallbacks(hideRunnable[0]);

            bubble.setVisibility(View.VISIBLE);
            bubble.animate().cancel();
            bubble.setAlpha(0f);
            bubble.setScaleX(0.88f);
            bubble.setScaleY(0.88f);
            bubble.setPivotX(bubble.getWidth() / 2f);
            bubble.setPivotY(bubble.getHeight());
            bubble.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            hideRunnable[0] = () -> bubble.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> bubble.setVisibility(View.GONE))
                    .start();
            handler.postDelayed(hideRunnable[0], 7000);
        });

        Button colonyBtn = new Button(this);
        colonyBtn.setText(Strings.get("egg.btn_build_colony"));
        colonyBtn.setTextSize(17f);
        colonyBtn.setTextColor(Color.parseColor("#0A0A0A"));
        GradientDrawable colonyBg = new GradientDrawable();
        colonyBg.setColor(Color.parseColor("#FFD700"));
        colonyBg.setCornerRadius(dpToPx(12));
        colonyBtn.setBackground(colonyBg);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        colLp.setMargins(0, dpToPx(24), 0, 0);
        colonyBtn.setLayoutParams(colLp);
        colonyBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ColonyActivity.class)));
        alienControls.addView(colonyBtn);

        Button backBtn = makeRetourBtn(v -> hideOverlay(eggOverlay));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        backLp.topMargin = dpToPx(20);
        backBtn.setLayoutParams(backLp);
        alienControls.addView(backBtn);

        layout.addView(alienControls, controlsLp);

        container.addView(layout, matchParentFl());
    }

    private String[] loadIdleQuotes() {
        String lang = normalizeIdleQuoteLang(prefs.getLanguage());
        String[] result = tryLoadIdleQuotes(lang);
        if (result == null) {
            String loadedLang = normalizeIdleQuoteLang(Strings.currentLang());
            if (!loadedLang.equals(lang)) result = tryLoadIdleQuotes(loadedLang);
        }
        if (result == null && !lang.equals("en")) result = tryLoadIdleQuotes("en");
        if (result == null && !lang.equals("fr")) result = tryLoadIdleQuotes("fr");
        return result != null ? result : new String[]{"..."};
    }

    private String normalizeIdleQuoteLang(String lang) {
        if (lang == null || lang.trim().isEmpty()) return "en";
        return lang.trim().toLowerCase().replace('-', '_');
    }

    private String[] tryLoadIdleQuotes(String lang) {
        try {
            String normalizedLang = normalizeIdleQuoteLang(lang);
            String path = "idle_quotes/" + normalizedLang + "_idle_quotes.json";
            InputStream is = getAssets().open(path);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
            is.close();
            String json = buffer.toString(StandardCharsets.UTF_8.name());
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray(normalizedLang + "_idle_quotes");
            if (arr == null) arr = root.optJSONArray("idle_quotes");
            if (arr == null) arr = root.optJSONArray("quotes");
            if (arr == null) {
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    Object value = root.opt(keys.next());
                    if (value instanceof JSONArray) {
                        arr = (JSONArray) value;
                        break;
                    }
                }
            }
            if (arr == null || arr.length() == 0) return null;
            String[] out = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) out[i] = arr.getString(i);
            return out;
        } catch (Exception e) {
            return null;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (resultOverlay != null && resultOverlay.getVisibility() == View.VISIBLE) {
            returnToMainMenuFromResults();
        } else if (gachaOverlay != null && gachaOverlay.getVisibility() == View.VISIBLE) {
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
