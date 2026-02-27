package com.example.bounceball.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Gère les publicités interstitielles Google Ads.
 * Affiche une pub toutes les 3 à 5 parties (aléatoire).
 *
 * IMPORTANT: Remplacez AD_UNIT_ID par votre vrai ID AdMob avant publication.
 * Pour les tests, utilisez : "ca-app-pub-3940256099942544/1033173712"
 *
 * Ajout dans build.gradle (app) :
 *   implementation 'com.google.android.gms:play-services-ads:23.0.0'
 *
 * Ajout dans AndroidManifest.xml (dans <application>) :
 *   <meta-data
 *       android:name="com.google.android.gms.ads.APPLICATION_ID"
 *       android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
 */
public class AdManager {

    private static final String TAG = "AdManager";
    // ID de test Google — remplacez par votre vrai ID en production
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    private static AdManager instance;

    private InterstitialAd interstitialAd;
    private int gameCount = 0;
    private int nextAdAt;
    private boolean isInitialized = false;

    private AdManager() {
        resetNextAdThreshold();
    }

    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    /** Initialise le SDK AdMob. À appeler une fois depuis MainActivity. */
    public void initialize(Context context) {
        if (isInitialized) return;
        MobileAds.initialize(context, initializationStatus -> {
            isInitialized = true;
            Log.d(TAG, "AdMob initialisé");
            loadInterstitial(context);
        });
    }

    /** Charge une nouvelle pub interstitielle en avance. */
    public void loadInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitielle chargée");
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitial(context); // précharge la suivante
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        interstitialAd = null;
                        loadInterstitial(context);
                    }
                });
            }
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.w(TAG, "Échec chargement pub : " + loadAdError.getMessage());
                interstitialAd = null;
            }
        });
    }

    /**
     * À appeler à chaque fin de partie.
     * Affiche une pub si le seuil est atteint.
     */
    public void onGameOver(Activity activity) {
        gameCount++;
        if (gameCount >= nextAdAt) {
            showIfReady(activity);
            gameCount = 0;
            resetNextAdThreshold();
        }
    }

    public void showIfReady(Activity activity) {
        if (interstitialAd != null) {
            activity.runOnUiThread(() -> {
                interstitialAd.show(activity);
            });
        }
    }

    /** Seuil aléatoire entre 3 et 5 parties. */
    private void resetNextAdThreshold() {
        nextAdAt = 3 + (int)(Math.random() * 3); // 3, 4 ou 5
    }
}
