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
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager {

    private static final String TAG = "AdManager";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private static AdManager instance;

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean isRewardedLoading = false;
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

    public void initialize(Context context) {
        if (isInitialized) return;
        MobileAds.initialize(context, initializationStatus -> {
            isInitialized = true;
            Log.d(TAG, "AdMob initialized");
            loadInterstitial(context);
            loadRewarded(context);
        });
    }

    public void loadInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitial loaded");
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitial(context);
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
                Log.w(TAG, "Interstitial load failed: " + loadAdError.getMessage());
                interstitialAd = null;
            }
        });
    }

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
            activity.runOnUiThread(() -> interstitialAd.show(activity));
        }
    }

    public boolean isRewardedReady() {
        return rewardedAd != null;
    }

    public void loadRewarded(Context context) {
        if (isRewardedLoading || rewardedAd != null) return;
        isRewardedLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                isRewardedLoading = false;
                Log.d(TAG, "Rewarded loaded");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                rewardedAd = null;
                isRewardedLoading = false;
                Log.w(TAG, "Rewarded load failed: " + loadAdError.getMessage());
            }
        });
    }

    public void showRewarded(Activity activity, RewardedCallback callback) {
        activity.runOnUiThread(() -> {
            if (rewardedAd == null) {
                loadRewarded(activity);
                if (callback != null) callback.onUnavailable();
                return;
            }

            RewardedAd adToShow = rewardedAd;
            rewardedAd = null;
            final boolean[] earnedReward = {false};
            adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    loadRewarded(activity);
                    if (callback == null) return;
                    if (earnedReward[0]) callback.onRewardEarned();
                    else callback.onClosedWithoutReward();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    loadRewarded(activity);
                    if (callback != null) callback.onUnavailable();
                }
            });

            adToShow.show(activity, rewardItem -> earnedReward[0] = true);
        });
    }

    public interface RewardedCallback {
        void onRewardEarned();
        void onUnavailable();
        void onClosedWithoutReward();
    }

    private void resetNextAdThreshold() {
        nextAdAt = 3 + (int)(Math.random() * 3);
    }
}
