package com.example.bounceball;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.bounceball.utils.GamePreferences;
import java.util.List;

/**
 * Menu Gacha en superposition (FrameLayout overlay).
 * Appeler GachaPage.buildOverlay(context, prefs) → FrameLayout à ajouter au root.
 *
 * Visuel : roue ronde 10 camemberts, aiguille rouge à 12h fixe.
 * Animation en 2 phases après clic sur Spin :
 *   1. Révélation des skins un par un dans les camemberts (180 ms/slice)
 *   2. Rotation de la roue avec décélération (ease-out cubic, ~2.8 s)
 * Résultat : +1 fragment du skin stoppé sous l'aiguille.
 */
public class GachaPage {

    private static final String PREF_POOL = "gacha_pool";

    private static GachaSystem.SkinEntry[] sPool;
    private static boolean                 sAnimating = false;

    private static WheelView sWheelView;
    private static TextView  sResultText;
    private static TextView  sGoldTv;
    private static TextView  sDiamTv;
    private static Button    sGoldBtn;
    private static Button    sDiamBtn;

    // ──────────────────────────────────────────────────
    // VUE PERSONNALISÉE : ROUE
    // ──────────────────────────────────────────────────
    static class WheelView extends View {

        float wheelRotation = 0f;
        int   filledCount   = 0;
        final GachaSystem.SkinEntry[] skins = new GachaSystem.SkinEntry[GachaSystem.POOL_SIZE];

        private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleRim   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WheelView(Context ctx) {
            super(ctx);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dpToPx(1.5f));
            borderPaint.setColor(Color.parseColor("#2A3A4A"));
            needleFill.setStyle(Paint.Style.FILL);
            needleFill.setColor(Color.parseColor("#FF2020"));
            needleRim.setStyle(Paint.Style.STROKE);
            needleRim.setStrokeWidth(dpToPx(1.5f));
            needleRim.setColor(Color.parseColor("#AA0000"));
            centerPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth()  / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) * 0.86f;

            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                float start  = -90f + i * 36f + wheelRotation;
                float center = (float) Math.toRadians(start + 18f);

                // Fond du camembert
                fillPaint.setStyle(Paint.Style.FILL);
                if (i < filledCount && skins[i] != null) {
                    fillPaint.setColor(GachaSystem.getRarityBgColor(skins[i].rarity));
                } else {
                    fillPaint.setColor(Color.parseColor("#F0EBE0")); // blanc cassé vide
                }
                canvas.drawArc(oval, start, 36f, true, fillPaint);

                // Bordure entre tranches
                canvas.drawArc(oval, start, 36f, true, borderPaint);

                // Contenu (visible seulement une fois révélé)
                if (i < filledCount && skins[i] != null) {
                    float circDist = radius * 0.60f;
                    float circR    = radius * 0.13f;
                    float bx = cx + circDist * (float) Math.cos(center);
                    float by = cy + circDist * (float) Math.sin(center);

                    // Preview du skin via BallRenderer
                    canvas.save();
                    android.graphics.Path clip = new android.graphics.Path();
                    clip.addCircle(bx, by, circR, android.graphics.Path.Direction.CW);
                    canvas.clipPath(clip);
                    try { BallRenderer.setColor(Color.parseColor(skins[i].colorHex)); }
                    catch (Exception ignored) { BallRenderer.setColor(Color.GRAY); }
                    BallRenderer.setAnimState(skins[i].id, 0f, 0f, 0f);
                    BallRenderer.draw(canvas, bx, by, circR, skins[i].id);
                    canvas.restore();

                    // Point de rareté (bord extérieur du camembert)
                    float dotDist = radius * 0.85f;
                    float dotX = cx + dotDist * (float) Math.cos(center);
                    float dotY = cy + dotDist * (float) Math.sin(center);
                    dotPaint.setColor(GachaSystem.getRarityColor(skins[i].rarity));
                    canvas.drawCircle(dotX, dotY, dpToPx(4f), dotPaint);
                }
            }

            // Cercle central (cache les pointes des tranches)
            centerPaint.setColor(Color.parseColor("#0D1B2A"));
            canvas.drawCircle(cx, cy, radius * 0.16f, centerPaint);
            centerPaint.setColor(Color.parseColor("#182838"));
            canvas.drawCircle(cx, cy, radius * 0.12f, centerPaint);

            // Aiguille rouge à 12h (triangle pointant vers le bas, fixe)
            float tipY   = cy - radius + dpToPx(8f);
            float baseY  = cy - radius - dpToPx(22f);
            float halfW  = dpToPx(9f);

            Path needle = new Path();
            needle.moveTo(cx, tipY);
            needle.lineTo(cx - halfW, baseY);
            needle.lineTo(cx + halfW, baseY);
            needle.close();
            canvas.drawPath(needle, needleFill);
            canvas.drawPath(needle, needleRim);

            // Petite boule en haut de l'aiguille
            canvas.drawCircle(cx, baseY, halfW * 0.7f, needleFill);
            canvas.drawCircle(cx, baseY, halfW * 0.7f, needleRim);
        }

        private float dpToPx(float dp) {
            return dp * getResources().getDisplayMetrics().density;
        }
    }

    // ──────────────────────────────────────────────────
    // POINT D'ENTRÉE — retourne un FrameLayout overlay
    // ──────────────────────────────────────────────────
    public static FrameLayout buildOverlay(Context ctx, GamePreferences prefs) {
        SharedPreferences raw      = prefs.getRaw();
        List<GachaSystem.SkinEntry> allSkins = CosmeticsPage.getAllGachaSkins();
        sPool      = loadOrBuildPool(raw, allSkins);
        sAnimating = false;

        // ── Overlay (fond semi-transparent → boutique visible et assombrie derrière) ──
        FrameLayout overlay = new FrameLayout(ctx);
        overlay.setBackgroundColor(Color.argb(185, 0, 0, 0));
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);

        // ── Conteneur central scrollable ──
        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.addView(scroll, scrollLp);

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(px(ctx, 16), px(ctx, 30), px(ctx, 16), px(ctx, 24));
        scroll.addView(container);

        // ── Devises ──
        LinearLayout currRow = new LinearLayout(ctx);
        currRow.setOrientation(LinearLayout.HORIZONTAL);
        currRow.setGravity(Gravity.CENTER);
        currRow.setPadding(0, 0, 0, px(ctx, 6));
        sGoldTv = new TextView(ctx);
        sGoldTv.setTextColor(Color.parseColor("#FFD700"));
        sGoldTv.setTextSize(14f);
        sDiamTv = new TextView(ctx);
        sDiamTv.setTextColor(Color.parseColor("#80DEEA"));
        sDiamTv.setTextSize(14f);
        sDiamTv.setPadding(px(ctx, 20), 0, 0, 0);
        currRow.addView(sGoldTv);
        currRow.addView(sDiamTv);
        container.addView(currRow);

        // ── Légende rarités ──
        LinearLayout legendRow = new LinearLayout(ctx);
        legendRow.setOrientation(LinearLayout.HORIZONTAL);
        legendRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        legLp.setMargins(0, 0, 0, px(ctx, 8));
        legendRow.setLayoutParams(legLp);
        addLegendBadge(ctx, legendRow, "Commun 72%",      GachaSystem.getRarityColor("common"));
        addLegendBadge(ctx, legendRow, "Rare 19.5%",      GachaSystem.getRarityColor("rare"));
        addLegendBadge(ctx, legendRow, "Légendaire 8.5%", GachaSystem.getRarityColor("legendary"));
        container.addView(legendRow);

        // ── Roue ──
        sWheelView = new WheelView(ctx);
        int wheelSize = px(ctx, 290);
        LinearLayout.LayoutParams wheelLp = new LinearLayout.LayoutParams(wheelSize, wheelSize);
        wheelLp.gravity = Gravity.CENTER_HORIZONTAL;
        sWheelView.setLayoutParams(wheelLp);
        container.addView(sWheelView);

        // ── Texte résultat ──
        sResultText = new TextView(ctx);
        sResultText.setText("");
        sResultText.setTextColor(Color.parseColor("#FFD700"));
        sResultText.setTextSize(13f);
        sResultText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resLp.setMargins(0, px(ctx, 10), 0, px(ctx, 4));
        sResultText.setLayoutParams(resLp);
        container.addView(sResultText);

        // ── Boutons Spin ──
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnRowLp.setMargins(0, px(ctx, 4), 0, 0);
        btnRow.setLayoutParams(btnRowLp);

        sGoldBtn = new Button(ctx);
        sGoldBtn.setText("🎰  Spin  ⬡ " + GachaSystem.COST_GOLD + " Or");
        sGoldBtn.setTextColor(Color.parseColor("#FFD700"));
        sGoldBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));

        sDiamBtn = new Button(ctx);
        sDiamBtn.setText("◆ " + GachaSystem.COST_DIAMONDS + " Diam");
        sDiamBtn.setTextColor(Color.parseColor("#80DEEA"));
        sDiamBtn.setBackgroundColor(Color.parseColor("#1A1A3A"));
        LinearLayout.LayoutParams dBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dBtnLp.setMargins(px(ctx, 14), 0, 0, 0);
        sDiamBtn.setLayoutParams(dBtnLp);

        btnRow.addView(sGoldBtn);
        btnRow.addView(sDiamBtn);
        container.addView(btnRow);

        // ── Bouton Fermer ──
        Button closeBtn = new Button(ctx);
        closeBtn.setText("✕  Fermer");
        closeBtn.setTextColor(Color.parseColor("#AAAAAA"));
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.setMargins(0, px(ctx, 10), 0, 0);
        closeLp.gravity = Gravity.CENTER_HORIZONTAL;
        closeBtn.setLayoutParams(closeLp);
        closeBtn.setOnClickListener(v -> overlay.setVisibility(View.GONE));
        container.addView(closeBtn);

        // ── Listeners Spin ──
        sGoldBtn.setOnClickListener(v -> {
            if (!sAnimating && prefs.getGold() >= GachaSystem.COST_GOLD) {
                prefs.spendGold(GachaSystem.COST_GOLD);
                updateCurrencyDisplay(prefs);
                doSpin(prefs, raw, allSkins);
            }
        });
        sDiamBtn.setOnClickListener(v -> {
            if (!sAnimating && prefs.getDiamonds() >= GachaSystem.COST_DIAMONDS) {
                prefs.spendDiamonds(GachaSystem.COST_DIAMONDS);
                updateCurrencyDisplay(prefs);
                doSpin(prefs, raw, allSkins);
            }
        });

        // ── Refresh runnable (appelé à l'ouverture de l'overlay) ──
        Runnable refreshOverlay = () -> {
            updateCurrencyDisplay(prefs);
            updateBtnStates(prefs);
        };
        overlay.setTag(R.id.tag_refresh, refreshOverlay);
        refreshOverlay.run();

        return overlay;
    }

    // ──────────────────────────────────────────────────
    // PHASE 1 : révélation des camemberts un par un
    // PHASE 2 : rotation jusqu'au gagnant
    // ──────────────────────────────────────────────────
    private static void doSpin(GamePreferences prefs, SharedPreferences raw,
                               List<GachaSystem.SkinEntry> allSkins) {
        sAnimating = true;
        updateBtnStates(prefs);
        if (sResultText != null) sResultText.setText("");

        int winner = GachaSystem.spin(sPool);

        // Réinitialiser la roue
        if (sWheelView != null) {
            sWheelView.filledCount   = 0;
            sWheelView.wheelRotation = 0f;
            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) sWheelView.skins[i] = null;
            sWheelView.invalidate();
        }

        Handler h = new Handler(Looper.getMainLooper());
        final int fillDelay = 180; // ms par tranche

        for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
            final int idx = i;
            h.postDelayed(() -> {
                if (sWheelView == null) return;
                sWheelView.skins[idx] = sPool[idx];
                sWheelView.filledCount = idx + 1;
                sWheelView.invalidate();
                // Une fois tous les camemberts remplis, lancer la rotation
                if (idx == GachaSystem.POOL_SIZE - 1) {
                    h.postDelayed(() -> startSpinAnimation(prefs, raw, allSkins, winner), 350);
                }
            }, (long) fillDelay * (i + 1));
        }
    }

    private static void startSpinAnimation(GamePreferences prefs, SharedPreferences raw,
                                           List<GachaSystem.SkinEntry> allSkins, int winner) {
        // Calcul de l'angle final pour aligner le centre du slice gagnant sous l'aiguille (−90°)
        // Centre du slice i sans rotation = −90 + i×36 + 18
        // On veut : −90 + winner×36 + 18 + wheelRotation ≡ −90  (mod 360)
        // → wheelRotation = −winner×36 − 18  (+ n×360 pour effet visuel)
        float base = ((-winner * 36f - 18f) % 360f + 360f) % 360f;
        float finalRotation = base + 5 * 360f; // 5 tours complets

        if (sWheelView == null) {
            onSpinComplete(prefs, raw, allSkins, winner);
            return;
        }

        ValueAnimator anim = ValueAnimator.ofFloat(0f, finalRotation);
        anim.setDuration(2800);
        // Ease-out cubic : départ rapide, freinage progressif
        anim.setInterpolator(t -> {
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        });
        anim.addUpdateListener(a -> {
            if (sWheelView != null) {
                sWheelView.wheelRotation = (float) a.getAnimatedValue();
                sWheelView.invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                if (sWheelView != null) {
                    sWheelView.wheelRotation = finalRotation % 360f;
                    sWheelView.invalidate();
                }
                onSpinComplete(prefs, raw, allSkins, winner);
            }
        });
        anim.start();
    }

    private static void onSpinComplete(GamePreferences prefs, SharedPreferences raw,
                                       List<GachaSystem.SkinEntry> allSkins, int winner) {
        GachaSystem.SkinEntry won = sPool[winner];
        prefs.addFragments(won.id, 1);

        int frags  = prefs.getFragments(won.id);
        int thresh = GachaSystem.getFragmentThreshold(won.rarity);

        String msg = "🧩 +1 fragment  " + won.name
                + "  [" + GachaSystem.getRarityLabel(won.rarity) + "]"
                + "  —  " + frags + " / " + thresh;
        if (sResultText != null) sResultText.setText(msg);

        updateCurrencyDisplay(prefs);

        // Régénère le pool pour le prochain spin
        sPool = GachaSystem.buildPool(allSkins);
        savePool(raw, sPool);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sAnimating = false;
            updateBtnStates(prefs);
        }, 1200);
    }

    // ──────────────────────────────────────────────────
    // PERSISTANCE DU POOL
    // ──────────────────────────────────────────────────
    private static GachaSystem.SkinEntry[] loadOrBuildPool(SharedPreferences raw,
                                                           List<GachaSystem.SkinEntry> allSkins) {
        String stored = raw.getString(PREF_POOL, "");
        if (!stored.isEmpty()) {
            String[] ids = stored.split(",");
            if (ids.length == GachaSystem.POOL_SIZE) {
                GachaSystem.SkinEntry[] pool = new GachaSystem.SkinEntry[GachaSystem.POOL_SIZE];
                boolean valid = true;
                for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                    GachaSystem.SkinEntry found = findById(allSkins, ids[i]);
                    if (found == null) { valid = false; break; }
                    pool[i] = found;
                }
                if (valid) return pool;
            }
        }
        GachaSystem.SkinEntry[] pool = GachaSystem.buildPool(allSkins);
        savePool(raw, pool);
        return pool;
    }

    private static void savePool(SharedPreferences raw, GachaSystem.SkinEntry[] pool) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pool.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(pool[i].id);
        }
        raw.edit().putString(PREF_POOL, sb.toString()).apply();
    }

    private static GachaSystem.SkinEntry findById(List<GachaSystem.SkinEntry> all, String id) {
        for (GachaSystem.SkinEntry e : all) if (e.id.equals(id)) return e;
        return null;
    }

    // ──────────────────────────────────────────────────
    // HELPERS UI
    // ──────────────────────────────────────────────────
    private static void updateCurrencyDisplay(GamePreferences prefs) {
        if (sGoldTv != null) sGoldTv.setText("⬡ " + prefs.getGold() + " Or");
        if (sDiamTv != null) sDiamTv.setText("◆ " + prefs.getDiamonds() + " Diam");
    }

    private static void updateBtnStates(GamePreferences prefs) {
        if (sGoldBtn != null)
            sGoldBtn.setEnabled(!sAnimating && prefs.getGold() >= GachaSystem.COST_GOLD);
        if (sDiamBtn != null)
            sDiamBtn.setEnabled(!sAnimating && prefs.getDiamonds() >= GachaSystem.COST_DIAMONDS);
    }

    private static void addLegendBadge(Context ctx, LinearLayout parent, String label, int color) {
        TextView tv = new TextView(ctx);
        tv.setText("⬤ " + label);
        tv.setTextColor(color);
        tv.setTextSize(11f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(px(ctx, 6), 0, px(ctx, 6), 0);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private static int px(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}