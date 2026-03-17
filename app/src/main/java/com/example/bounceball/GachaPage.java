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
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation;
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
 * Visuel : roue ronde 10 camemberts, liseré jaune, aiguille rouge à 12h fixe.
 * Animation en 2 phases après clic sur Spin :
 *   1. Chaque balle atterrit en spirale dans son camembert (600 ms/balle, décalage 250 ms)
 *   2. Rotation de la roue avec décélération (ease-out cubic, ~2.8 s)
 * Résultat : +1 fragment du skin stoppé sous l'aiguille.
 */
public class GachaPage {

    private static final String PREF_POOL = "gacha_pool";

    private static GachaSystem.SkinEntry[] sPool;
    private static boolean sAnimating = false;
    private static final java.util.Random SPIN_RNG = new java.util.Random();
    private static WheelView sWheelView;
    private static TextView  sResultText;
    private static TextView  sGoldTv;
    private static TextView  sDiamTv;
    private static Button    sGoldBtn;
    private static Button    sDiamBtn;
    private static TextView  sSpinLabel;

    // ──────────────────────────────────────────────────
    // VUE PERSONNALISÉE : ROUE
    // ──────────────────────────────────────────────────
    static class WheelView extends View {

        float   wheelRotation  = 0f;
        float   lockedRotation = 0f;
        boolean spinning       = false;
        int     filledCount    = 0;
        final GachaSystem.SkinEntry[] skins        = new GachaSystem.SkinEntry[GachaSystem.POOL_SIZE];
        final float[]                 ballProgress = new float[GachaSystem.POOL_SIZE]; // 0=vide, 0..1=en vol, 1=posé

        private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleRim   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path  clipPath    = new Path();

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
            float cx     = getWidth()  / 2f;
            float cy     = getHeight() / 2f;
            float radius = Math.min(cx, cy) * 0.86f;
            android.util.Log.d("WHEEL", "onDraw wheelRot=" + wheelRotation + " spinning=" + spinning);

            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

            // Appliquer la rotation de la roue via canvas.rotate()
            // Tout le contenu tournant est dessiné à l'intérieur du save/restore
            canvas.save();
            canvas.rotate(wheelRotation, cx, cy);

            // ── 1. Camemberts (fond + bordure) ──
            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                float start = -90f + i * 36f;

                fillPaint.setStyle(Paint.Style.FILL);
                if (skins[i] != null) {
                    fillPaint.setColor(GachaSystem.getRarityBgColor(skins[i].rarity));
                } else {
                    fillPaint.setColor(Color.parseColor("#F0EBE0"));
                }
                canvas.drawArc(oval, start, 36f, true, fillPaint);
                canvas.drawArc(oval, start, 36f, true, borderPaint);
            }

            // ── 2. Balles posées dans leur slot ──
            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                if (ballProgress[i] < 1f || skins[i] == null) continue;

                float start  = -90f + i * 36f;
                float center = (float) Math.toRadians(start + 18f);
                float circDist = radius * 0.58f;
                float circR    = radius * 0.16f;
                float bx = cx + circDist * (float) Math.cos(center);
                float by = cy + circDist * (float) Math.sin(center);

                canvas.save();
                clipPath.reset();
                clipPath.addCircle(bx, by, circR * 1.85f, Path.Direction.CW);
                canvas.clipPath(clipPath);
                try { BallRenderer.setColor(Color.parseColor(skins[i].colorHex)); }
                catch (Exception ignored) { BallRenderer.setColor(Color.GRAY); }
                BallRenderer.setAnimState(skins[i].id, 0f, 0f, 0f);
                BallRenderer.draw(canvas, bx, by, circR, skins[i].id);
                canvas.restore();

                float dotDist = radius * 0.85f;
                float dotX = cx + dotDist * (float) Math.cos(center);
                float dotY = cy + dotDist * (float) Math.sin(center);
                dotPaint.setColor(GachaSystem.getRarityColor(skins[i].rarity));
                canvas.drawCircle(dotX, dotY, dpToPx(4f), dotPaint);
            }

            // ── 3. Balles en vol (spirale d'atterrissage) ──
            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                float p = ballProgress[i];
                if (p <= 0f || p >= 1f || skins[i] == null) continue;

                float start   = -90f + i * 36f;
                float center  = (float) Math.toRadians(start + 18f);
                float targetX = cx + radius * 0.58f * (float) Math.cos(center);
                float targetY = cy + radius * 0.58f * (float) Math.sin(center);
                float targetR = radius * 0.16f;

                float frac        = 1f - p;
                float spiralAngle = center + frac * (float) (1.2f * Math.PI);
                float spiralR     = frac * radius * 2.2f;
                float bx          = targetX + (float) Math.cos(spiralAngle) * spiralR;
                float by          = targetY + (float) Math.sin(spiralAngle) * spiralR;
                float currentR    = targetR * (1f + frac * 3.5f);

                canvas.save();
                clipPath.reset();
                clipPath.addCircle(bx, by, currentR * 1.85f, Path.Direction.CW);
                canvas.clipPath(clipPath);
                try { BallRenderer.setColor(Color.parseColor(skins[i].colorHex)); }
                catch (Exception ignored) { BallRenderer.setColor(Color.GRAY); }
                BallRenderer.setAnimState(skins[i].id, 0f, 0f, 0f);
                BallRenderer.draw(canvas, bx, by, currentR, skins[i].id);
                canvas.restore();
            }

            // ── 4. Cercle central (tourne avec la roue) ──
            centerPaint.setColor(Color.parseColor("#0D1B2A"));
            canvas.drawCircle(cx, cy, radius * 0.16f, centerPaint);
            centerPaint.setColor(Color.parseColor("#182838"));
            canvas.drawCircle(cx, cy, radius * 0.12f, centerPaint);

            // Fin de la zone tournante
            canvas.restore();

            // ── 5. Liseré jaune (FIXE, hors rotation) ──
            Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(dpToPx(14f));
            outlinePaint.setColor(Color.parseColor("#333333"));
            canvas.drawCircle(cx, cy, radius, outlinePaint);

            Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rimPaint.setStyle(Paint.Style.STROKE);
            rimPaint.setStrokeWidth(dpToPx(10f));
            rimPaint.setColor(Color.parseColor("#FFD700"));
            canvas.drawCircle(cx, cy, radius, rimPaint);

            // ── 6. Aiguille rouge à 12h (FIXE, hors rotation) ──
            float tipY  = cy - radius + dpToPx(8f);
            float baseY = cy - radius - dpToPx(22f);
            float halfW = dpToPx(9f);

            Path needle = new Path();
            needle.moveTo(cx, tipY);
            needle.lineTo(cx - halfW, baseY);
            needle.lineTo(cx + halfW, baseY);
            needle.close();
            canvas.drawPath(needle, needleFill);
            canvas.drawPath(needle, needleRim);
            canvas.drawCircle(cx, baseY, halfW * 0.7f, needleFill);
            canvas.drawCircle(cx, baseY, halfW * 0.7f, needleRim);

            // ── 7. Continuer le dessin uniquement si une animation est en cours ──
            boolean needsRedraw = spinning;
            for (float p : ballProgress) {
                if (p > 0f && p < 1f) { needsRedraw = true; break; }
            }
            if (needsRedraw) postInvalidateOnAnimation();
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

        // ── Overlay ──
        FrameLayout overlay = new FrameLayout(ctx);
        overlay.setBackgroundColor(Color.argb(185, 0, 0, 0));
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);

        // ── Colonne verticale pondérée : contenu au 2/3 ──
        LinearLayout outerCol = new LinearLayout(ctx);
        outerCol.setOrientation(LinearLayout.VERTICAL);
        outerCol.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams outerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlay.addView(outerCol, outerLp);
        overlay.setClipChildren(false);
        outerCol.setClipChildren(false);
        outerCol.setClipToPadding(false);

        View spacerTop = new View(ctx);
        outerCol.addView(spacerTop, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 2f));

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(px(ctx, 16), 0, px(ctx, 16), 0);
        outerCol.addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setClipChildren(false);
        container.setClipToPadding(false);

        View spacerBot = new View(ctx);
        outerCol.addView(spacerBot, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ── Devises ──
        LinearLayout currRow = new LinearLayout(ctx);
        currRow.setOrientation(LinearLayout.HORIZONTAL);
        currRow.setGravity(Gravity.CENTER);
        currRow.setPadding(0, 0, 0, px(ctx, 4));
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

        // ── "Spin the wheel" — au-dessus de la roue, grand et clignotant ──
        sSpinLabel = new TextView(ctx);
        sSpinLabel.setText("Spin the wheel");
        sSpinLabel.setTextColor(Color.parseColor("#FFD700"));
        sSpinLabel.setTextSize(26f);
        sSpinLabel.setGravity(Gravity.CENTER);
        sSpinLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spinLp.setMargins(0, px(ctx, 6), 0, px(ctx, 8));
        sSpinLabel.setLayoutParams(spinLp);
        startSpinLabelPulse();
        container.addView(sSpinLabel);

        // ── Roue ──
        sWheelView = new WheelView(ctx);
        int wheelSize = px(ctx, 340);
        LinearLayout.LayoutParams wheelLp = new LinearLayout.LayoutParams(wheelSize, wheelSize);
        wheelLp.gravity = Gravity.CENTER_HORIZONTAL;
        sWheelView.setLayoutParams(wheelLp);
        container.addView(sWheelView);
        sWheelView.setClipToOutline(false);

        // ── Texte résultat ──
        sResultText = new TextView(ctx);
        sResultText.setText("");
        sResultText.setTextColor(Color.parseColor("#FFD700"));
        sResultText.setTextSize(13f);
        sResultText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resLp.setMargins(0, px(ctx, 8), 0, px(ctx, 2));
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
        sGoldBtn.setText("⬡ " + GachaSystem.COST_GOLD + " Or");
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

        // ── Refresh runnable ──
        Runnable refreshOverlay = () -> {
            updateCurrencyDisplay(prefs);
            updateBtnStates(prefs);
        };
        overlay.setTag(R.id.tag_refresh, refreshOverlay);
        refreshOverlay.run();

        return overlay;
    }

    // ──────────────────────────────────────────────────
    // ANIMATION DU LABEL "SPIN THE WHEEL"
    // ──────────────────────────────────────────────────
    private static void startSpinLabelPulse() {
        if (sSpinLabel == null) return;
        AlphaAnimation pulse = new AlphaAnimation(0.35f, 1f);
        pulse.setDuration(1100);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        sSpinLabel.startAnimation(pulse);
    }

    private static void stopSpinLabelPulse() {
        if (sSpinLabel == null) return;
        sSpinLabel.clearAnimation();
        sSpinLabel.setAlpha(0.5f);
    }

    // ──────────────────────────────────────────────────
    // PHASE 1 : atterrissage en spirale (un par un)
    // PHASE 2 : rotation jusqu'au gagnant
    // ──────────────────────────────────────────────────
    private static void doSpin(GamePreferences prefs, SharedPreferences raw,
                               List<GachaSystem.SkinEntry> allSkins) {
        sAnimating = true;
        stopSpinLabelPulse();
        updateBtnStates(prefs);
        if (sResultText != null) sResultText.setText("");

        int winner = GachaSystem.spin(sPool);

        // Réinitialiser la roue
        if (sWheelView != null) {
            sWheelView.filledCount    = 0;
            sWheelView.spinning       = false;
            sWheelView.wheelRotation  = 0f;
            sWheelView.lockedRotation = 0f;
            for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
                sWheelView.skins[i]        = null;
                sWheelView.ballProgress[i] = 0f;
            }
            sWheelView.invalidate();
        }

        Handler h = new Handler(Looper.getMainLooper());
        final int staggerMs = 250; // délai entre chaque balle

        for (int i = 0; i < GachaSystem.POOL_SIZE; i++) {
            final int idx = i;
            h.postDelayed(() -> {
                if (sWheelView == null) return;

                // Enregistrer le skin dès le début de l'animation (pour couleur de fond immédiate)
                sWheelView.skins[idx] = sPool[idx];

                ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                anim.setDuration(580);
                // Ease-out smooth : accélération initiale, freinage doux en fin
                anim.setInterpolator(t -> {
                    // smoothstep amélioré
                    float s = t * t * (3f - 2f * t);
                    return s;
                });
                anim.addUpdateListener(a -> {
                    if (sWheelView != null) {
                        sWheelView.ballProgress[idx] = (float) a.getAnimatedValue();
                    }
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) {
                        if (sWheelView != null) {
                            sWheelView.ballProgress[idx] = 1f;
                            sWheelView.filledCount       = idx + 1;
                            sWheelView.invalidate();
                        }
                        if (idx == GachaSystem.POOL_SIZE - 1) {
                            h.postDelayed(() -> startSpinAnimation(prefs, raw, allSkins, winner), 350);
                        }
                    }
                });
                anim.start();
                // Déclencher le premier frame immédiatement
                sWheelView.invalidate();

            }, (long) staggerMs * i);
        }
    }

    private static void startSpinAnimation(GamePreferences prefs, SharedPreferences raw,
                                           List<GachaSystem.SkinEntry> allSkins, int winner) {
        if (sWheelView == null) { onSpinComplete(prefs, raw, allSkins, winner); return; }

        String  rarity    = sPool[winner].rarity;
        boolean isCommon  = "common".equals(rarity);
        boolean isRare    = "rare".equals(rarity);
        boolean isLeg     = "legendary".equals(rarity);

        // Near-miss : 28 % des tirages communs
        // L'aiguille s'arrêtera juste après avoir traversé le bord du slice précédent
        // (pour winner 3-5, ce bord est celui d'un slice rare — suspens maximal)
        boolean nearMiss = isCommon && (SPIN_RNG.nextFloat() < 0.28f);

        // Position d'atterrissage dans le slice (offset en ° par rapport au centre)
        // offset négatif = proche du bord entrant ("de quelques millimètres")
        // offset positif = proche du bord sortant
        float sliceOffset;
        if (nearMiss) {
            sliceOffset = -14f - SPIN_RNG.nextFloat() * 3f;   // −14° à −17° : bord entrant
        } else if (isLeg) {
            sliceOffset = (SPIN_RNG.nextFloat() - 0.5f) * 10f; // ±5° : légèrement centré
        } else if (isRare) {
            sliceOffset = (SPIN_RNG.nextFloat() - 0.5f) * 22f; // ±11° : bien réparti
        } else {
            sliceOffset = (SPIN_RNG.nextFloat() - 0.5f) * 30f; // ±15° : presque tout le slice
        }

        // Angle de rotation final : aiguille à -90° pointe le centre du slice + offset
        float base          = ((-winner * 36f - 18f - sliceOffset) % 360f + 360f) % 360f;
        int   rotations     = isLeg ? 6 + SPIN_RNG.nextInt(2)
                : isRare ? 5 + SPIN_RNG.nextInt(2)
                : nearMiss ? 4 + SPIN_RNG.nextInt(2)
                : 3 + SPIN_RNG.nextInt(3);
        float startRotation = sWheelView.wheelRotation % 360f;
        float finalRotation = startRotation + base + rotations * 360f;

        // Durée
        long duration = isLeg    ? 7000
                : nearMiss ? 5500 + SPIN_RNG.nextInt(700)
                : isRare   ? 4800 + SPIN_RNG.nextInt(400)
                : 3200 + SPIN_RNG.nextInt(600);

        final float decelFactor = isLeg ? 2.0f : isRare ? 1.8f : nearMiss ? 1.5f : 1.4f;

        ValueAnimator anim = ValueAnimator.ofFloat(startRotation, finalRotation);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator(decelFactor));
        final float frozenRotation = finalRotation;
        if (sWheelView != null) sWheelView.spinning = true;
        anim.addUpdateListener(a -> {
            if (sWheelView != null && sWheelView.spinning) {
                sWheelView.wheelRotation = (float) a.getAnimatedValue();
                sWheelView.invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                if (sWheelView != null) {
                    sWheelView.wheelRotation  = frozenRotation % 360f;
                    sWheelView.lockedRotation = frozenRotation % 360f;
                    sWheelView.spinning       = false;
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

        sPool = GachaSystem.buildPool(allSkins);
        savePool(raw, sPool);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sAnimating = false;
            updateBtnStates(prefs);
            startSpinLabelPulse();
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