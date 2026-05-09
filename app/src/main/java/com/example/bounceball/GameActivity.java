package com.example.bounceball;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.AdManager;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.LocaleManager;
import com.example.bounceball.utils.ImmersiveHelper;

public class GameActivity extends Activity {

    private GameView gameView;
    private GamePreferences prefs;
    private UpgradeStats upgrades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.applyLocale(this);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        prefs = new GamePreferences(this);
        upgrades = UpgradeStats.fromPrefs(prefs.getRaw());

        gameView = new GameView(this, prefs, upgrades);

        // Écoute les événements de jeu pour les pubs et la sauvegarde
        gameView.setGameStateListener(new GameView.GameStateListener() {
            @Override
            public void onGameStarted() {
                // optionnel : HUD visible
            }

            @Override
            public void onGameOver(float heightReached, long durationMillis) {
                // 1. Sauvegarde le record d'ascension
                boolean newRecord = prefs.updateMaxHeight(heightReached);

                // 2. Ajoute l'or gagné (avec multiplicateur)
                int goldEarned = (int) Math.floor(heightReached * upgrades.goldMultiplier);
                prefs.addGold(goldEarned);

                // 3. Gère les pubs interstitielles
                AdManager.getInstance().onGameOver(GameActivity.this);
            }
        });

        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImmersiveHelper.enable(getWindow());
        gameView.reloadUpgrades();
        upgrades = UpgradeStats.fromPrefs(prefs.getRaw());
        gameView.loadBallSkin();
        gameView.loadBgSkin();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ImmersiveHelper.enable(getWindow());
    }
}
