package com.example.bounceball;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.ImmersiveHelper;
import com.example.bounceball.utils.LocaleManager;
import com.example.bounceball.utils.Strings;
import com.android.billingclient.api.*;
import java.util.Arrays;
import java.util.List;

public class UpgradeActivity extends Activity {

    private GamePreferences prefs;
    private UpgradeStats upgrades;
    private TextView goldText, diamondText;

    private BillingClient billingClient;

    private static final String[] SKU_IDS = {
            "diamonds_100", "diamonds_500", "diamonds_1200", "diamonds_2500"
    };
    private static final int[] DIAMOND_AMOUNTS = {100, 500, 1200, 2500};
    private static final String[] PRICE_FALLBACK = {"0,99 €", "3,99 €", "8,99 €", "17,99 €"};

    private LinearLayout upgradesContent;
    private LinearLayout shopContent;
    private Button tabUpgradesBtn, tabShopBtn;

    private Object[][] buildUpgradesData() {
        return new Object[][] {
                {Strings.get("shop.upgrades.upg_air.name"),            Strings.get("shop.upgrades.upg_air.desc"),             50,  5,  10, "upg_air"},
                {Strings.get("shop.upgrades.upg_elastic.name"),        Strings.get("shop.upgrades.upg_elastic.desc"),         80,  8,  10, "upg_elastic"},
                {Strings.get("shop.upgrades.upg_boost.name"),          Strings.get("shop.upgrades.upg_boost.desc"),          150, 15,   5, "upg_boost"},
                {Strings.get("shop.upgrades.upg_boost_recharge.name"), Strings.get("shop.upgrades.upg_boost_recharge.desc"), 100, 10,   5, "upg_boost_recharge"},
                {Strings.get("shop.upgrades.upg_ink_reserve.name"),    Strings.get("shop.upgrades.upg_ink_reserve.desc"),     60,  6,  10, "upg_ink_reserve"},
                {Strings.get("shop.upgrades.upg_ink_eff.name"),        Strings.get("shop.upgrades.upg_ink_eff.desc"),         70,  7,  10, "upg_ink_eff"},
                {Strings.get("shop.upgrades.upg_gold_mult.name"),      Strings.get("shop.upgrades.upg_gold_mult.desc"),      200, 20, 100, "upg_gold_mult"},
                {Strings.get("shop.upgrades.upg_warp.name"),           Strings.get("shop.upgrades.upg_warp.desc"),           300, 30,   5, "upg_warp"},
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.applyLocale(this);

        prefs = new GamePreferences(this);
        upgrades = UpgradeStats.fromPrefs(prefs.getRaw());

        setupBillingClient();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D1B2A"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(30, 60, 30, 0);

        TextView title = new TextView(this);
        title.setText(Strings.get("shop.title"));
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setTextSize(28f);
        title.setPadding(0, 0, 0, 8);
        header.addView(title);

        LinearLayout currRow = new LinearLayout(this);
        currRow.setOrientation(LinearLayout.HORIZONTAL);
        goldText = new TextView(this);
        goldText.setTextColor(Color.parseColor("#FFD700"));
        goldText.setTextSize(18f);
        goldText.setPadding(0, 0, 40, 0);
        diamondText = new TextView(this);
        diamondText.setTextColor(Color.parseColor("#80DEEA"));
        diamondText.setTextSize(18f);
        currRow.addView(goldText);
        currRow.addView(diamondText);
        header.addView(currRow);
        updateCurrencyDisplay();

        float maxH = prefs.getMaxHeight();
        TextView recordView = new TextView(this);
        recordView.setText(Strings.fmt("shop.record_fmt", maxH));
        recordView.setTextColor(Color.parseColor("#AAAAAA"));
        recordView.setTextSize(14f);
        recordView.setPadding(0, 4, 0, 0);
        header.addView(recordView);

        root.addView(header);

        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(0, 16, 0, 0);

        tabUpgradesBtn = makeTabButton(Strings.get("shop.tab_upgrades"), true);
        tabShopBtn     = makeTabButton(Strings.get("shop.tab_buy_gems"), false);
        tabBar.addView(tabUpgradesBtn);
        tabBar.addView(tabShopBtn);
        root.addView(tabBar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollLp);

        LinearLayout contentWrapper = new LinearLayout(this);
        contentWrapper.setOrientation(LinearLayout.VERTICAL);

        upgradesContent = buildUpgradesContent();
        shopContent = buildShopContent();
        shopContent.setVisibility(View.GONE);

        contentWrapper.addView(upgradesContent);
        contentWrapper.addView(shopContent);
        scroll.addView(contentWrapper);
        root.addView(scroll);

        Button playBtn = new Button(this);
        playBtn.setText(Strings.get("shop.btn_play"));
        playBtn.setTextColor(Color.WHITE);
        playBtn.setBackgroundColor(Color.parseColor("#1B5E20"));
        playBtn.setPadding(20, 20, 20, 20);
        playBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });
        root.addView(playBtn);

        Button settingsBtn = new Button(this);
        settingsBtn.setText(Strings.get("shop.btn_settings"));
        settingsBtn.setTextColor(Color.parseColor("#AAAAAA"));
        settingsBtn.setBackgroundColor(Color.parseColor("#1A2A3A"));
        settingsBtn.setPadding(20, 12, 20, 12);
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        root.addView(settingsBtn);

        tabUpgradesBtn.setOnClickListener(v -> switchTab(true));
        tabShopBtn.setOnClickListener(v -> switchTab(false));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImmersiveHelper.enable(getWindow());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ImmersiveHelper.enable(getWindow());
    }

    private LinearLayout buildUpgradesContent() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 10, 30, 60);

        for (Object[] upg : buildUpgradesData()) {
            layout.addView(buildUpgradeRow(upg));
        }
        return layout;
    }

    private View buildUpgradeRow(Object[] upg) {
        String name    = (String) upg[0];
        String desc    = (String) upg[1];
        int goldBase   = (int)    upg[2];
        int diamBase   = (int)    upg[3];
        int maxLvl     = (int)    upg[4];
        String key     = (String) upg[5];

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(20, 20, 20, 20);
        row.setBackgroundColor(Color.parseColor("#1A2A3A"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 0);
        row.setLayoutParams(params);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(18f);
        row.addView(nameView);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setTextSize(13f);
        row.addView(descView);

        TextView levelView = new TextView(this);
        levelView.setTextColor(Color.parseColor("#00E676"));
        levelView.setTextSize(14f);
        row.addView(levelView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button goldBtn = new Button(this);
        Button diamBtn = new Button(this);

        Runnable refresh = () -> {
            int lvl      = prefs.getRaw().getInt(key, 0);
            int goldCost = goldBase * (lvl + 1);
            int diamCost = diamBase * (lvl + 1);
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
            updateCurrencyDisplay();
        };

        goldBtn.setTextColor(Color.parseColor("#FFD700"));
        goldBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));
        goldBtn.setPadding(16, 8, 16, 8);
        goldBtn.setOnClickListener(v -> {
            int lvl = prefs.getRaw().getInt(key, 0);
            int cost = goldBase * (lvl + 1);
            if (prefs.getGold() >= cost && lvl < maxLvl) {
                prefs.spendGold(cost);
                prefs.getRaw().edit().putInt(key, lvl + 1).apply();
                refresh.run();
            }
        });

        diamBtn.setTextColor(Color.parseColor("#80DEEA"));
        diamBtn.setBackgroundColor(Color.parseColor("#1A1A3A"));
        diamBtn.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams diamLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        diamLp.setMargins(16, 0, 0, 0);
        diamBtn.setLayoutParams(diamLp);
        diamBtn.setOnClickListener(v -> {
            int lvl  = prefs.getRaw().getInt(key, 0);
            int cost = (int)(diamBase * (lvl + 1) * 0.5f);
            if (prefs.getDiamonds() >= cost && lvl < maxLvl) {
                prefs.spendDiamonds(cost);
                prefs.getRaw().edit().putInt(key, lvl + 1).apply();
                refresh.run();
            }
        });

        btnRow.addView(goldBtn);
        btnRow.addView(diamBtn);
        row.addView(btnRow);
        refresh.run();
        return row;
    }

    private LinearLayout buildShopContent() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 60);

        TextView info = new TextView(this);
        info.setText(Strings.get("shop.gems_info"));
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14f);
        info.setPadding(0, 0, 0, 20);
        layout.addView(info);

        String[] labels = {
                Strings.get("shop.gem_packs.pack_100"),
                Strings.get("shop.gem_packs.pack_500"),
                Strings.get("shop.gem_packs.pack_1200"),
                Strings.get("shop.gem_packs.pack_2500")
        };
        String[] bonusLabels = {
                Strings.get("shop.gem_packs.bonus_none"),
                Strings.get("shop.gem_packs.bonus_5"),
                Strings.get("shop.gem_packs.bonus_20"),
                Strings.get("shop.gem_packs.bonus_50")
        };

        for (int i = 0; i < SKU_IDS.length; i++) {
            layout.addView(buildShopItem(i, labels[i], bonusLabels[i]));
        }

        TextView legal = new TextView(this);
        legal.setText(Strings.get("shop.gems_legal"));
        legal.setTextColor(Color.parseColor("#666666"));
        legal.setTextSize(11f);
        legal.setPadding(0, 30, 0, 0);
        layout.addView(legal);

        return layout;
    }

    private View buildShopItem(int index, String label, String bonus) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 20, 20, 20);
        row.setBackgroundColor(Color.parseColor("#1A2A3A"));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 10, 0, 0);
        row.setLayoutParams(rowLp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textLp);

        TextView diamLabel = new TextView(this);
        diamLabel.setText(label);
        diamLabel.setTextColor(Color.parseColor("#80DEEA"));
        diamLabel.setTextSize(18f);
        textCol.addView(diamLabel);

        if (!bonus.isEmpty()) {
            TextView bonusView = new TextView(this);
            bonusView.setText(bonus);
            bonusView.setTextColor(Color.parseColor("#FFD700"));
            bonusView.setTextSize(12f);
            textCol.addView(bonusView);
        }

        row.addView(textCol);

        Button buyBtn = new Button(this);
        buyBtn.setText(PRICE_FALLBACK[index]);
        buyBtn.setTextColor(Color.WHITE);
        buyBtn.setBackgroundColor(Color.parseColor("#1565C0"));
        buyBtn.setPadding(24, 12, 24, 12);
        final int idx = index;
        buyBtn.setOnClickListener(v -> launchPurchase(idx));
        row.addView(buyBtn);

        return row;
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
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

    private void launchPurchase(int index) {
        if (!billingClient.isReady()) {
            Toast.makeText(this, Strings.get("shop.gems_unavail"), Toast.LENGTH_SHORT).show();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Arrays.asList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SKU_IDS[index])
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
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

        String productId = purchase.getProducts().get(0);
        for (int i = 0; i < SKU_IDS.length; i++) {
            if (SKU_IDS[i].equals(productId)) {
                prefs.addDiamonds(DIAMOND_AMOUNTS[i]);
                runOnUiThread(this::updateCurrencyDisplay);

                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    billingClient.acknowledgePurchase(ackParams, result -> {});
                }
                break;
            }
        }
    }

    private Button makeTabButton(String text, boolean active) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(active ? Color.parseColor("#FFD700") : Color.parseColor("#AAAAAA"));
        btn.setBackgroundColor(active ? Color.parseColor("#1A2A3A") : Color.parseColor("#0D1B2A"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void switchTab(boolean showUpgrades) {
        upgradesContent.setVisibility(showUpgrades ? View.VISIBLE : View.GONE);
        shopContent.setVisibility(showUpgrades ? View.GONE : View.VISIBLE);
        tabUpgradesBtn.setTextColor(showUpgrades ? Color.parseColor("#FFD700") : Color.parseColor("#AAAAAA"));
        tabUpgradesBtn.setBackgroundColor(showUpgrades ? Color.parseColor("#1A2A3A") : Color.parseColor("#0D1B2A"));
        tabShopBtn.setTextColor(!showUpgrades ? Color.parseColor("#80DEEA") : Color.parseColor("#AAAAAA"));
        tabShopBtn.setBackgroundColor(!showUpgrades ? Color.parseColor("#1A2A3A") : Color.parseColor("#0D1B2A"));
    }

    private void updateCurrencyDisplay() {
        goldText.setText(Strings.fmt("common.currency_gold_fmt", prefs.getGold()));
        diamondText.setText(Strings.fmt("common.currency_diam_fmt", prefs.getDiamonds()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) billingClient.endConnection();
    }
}