package com.example.bounceball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Iterator;
import androidx.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.GamePreferences;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.BlurMaskFilter;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final float GRAVITY = 0.6f;
    private float airResistance;
    private float trampElasticity;
    private float maxInk;
    private float currentInk;
    private float inkConsumptionRate;

    private Thread gameThread;
    private boolean isRunning;
    private SurfaceHolder surfaceHolder;
    private Paint paint;

    private int screenWidth;
    private int screenHeight;

    private float ballX;
    private float ballY;
    private float ballVelocityY;
    private float ballVelocityX;
    private float ballRotation     = 0f;
    private float ballAngularSpeed = 0f;
    private final float ballRadius = 48f;

    private boolean isGameStarted;
    private boolean isGameOver;
    private float totalHeightMeters;

    private float trampStartX, trampStartY, trampEndX, trampEndY;
    private boolean isDrawingTrampoline;
    private boolean hasTrampoline;
    private float lastTouchX, lastTouchY;

    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String currentBallSkin = "ball_basic";
    private Bitmap gaugeSprite;
    private float gaugeOffsetX = -500f;
    private float statsOffsetY = -250f;
    private boolean hudShouldBeVisible = false;
    private static final float HUD_ANIM_SPEED = 1800f;

    private GamePreferences prefs;
    private UpgradeStats upgrades;

    // ── Power-ups ──────────────────────────────────────
    private final ArrayList<float[]> inkBlobs = new ArrayList<>();
    private final ArrayList<float[]> warps    = new ArrayList<>();

    private static final float INK_BLOB_RADIUS = 32f * 0.67f;
    private static final float WARP_WIDTH      = 32f * 4f;
    private static final float WARP_HEIGHT     = 28f;
    private static final float INK_RECHARGE    = 80f;
    private static final float INK_SPAWN_CHANCE  = 0.004f;
    private static final float WARP_SPAWN_CHANCE = 0.001f;

    // ── Aimant ─────────────────────────────────────────
    private static final float MAGNET_RADIUS   = 220f;
    private static final float MAGNET_STRENGTH = 10f;

    // ── Machine a etats warp ───────────────────────────
    private static final int WARP_NONE   = 0;
    private static final int WARP_ABSORB = 1;
    private static final int WARP_SCROLL = 2;
    private static final int WARP_EJECT  = 3;

    private int   warpState        = WARP_NONE;
    private int   warpAbsorbTimer  = 0;
    private float warpScrollLeft   = 0f;
    private int   warpEjectTimer   = 0;
    private float exitPortalX      = 0f;
    private float exitPortalY      = 0f;
    private float warpBallScale    = 1f;

    private static final int   ABSORB_DUR  = 22;
    private static final int   EJECT_DUR   = 45;
    private static final float SCROLL_EASE = 0.09f;
    private static final float SCROLL_MIN  = 4f;

    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float currentRunGold = 0f;

    // ──────────────────────────────────────────────────

    public void setHudVisible(boolean visible) { hudShouldBeVisible = visible; }

    public interface GameStateListener {
        void onGameStarted();
        void onGameOver(float heightReached);
    }
    private GameStateListener gameStateListener;
    public void setGameStateListener(GameStateListener l) { this.gameStateListener = l; }

    public GameView(Context context, GamePreferences prefs, UpgradeStats upgrades) {
        super(context);
        this.prefs    = prefs;
        this.upgrades = upgrades;
        applyUpgrades();
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(false);
        loadBallSkin();
        ballShinePaint.setColor(Color.WHITE);
        ballShinePaint.setAlpha(120);
        gaugeSprite = BitmapFactory.decodeResource(getResources(), R.drawable.gauge);
        currentInk        = maxInk;
        isGameStarted     = false;
        isGameOver        = false;
        totalHeightMeters = 0f;
    }

    private void applyUpgrades() {
        airResistance      = 0.98f + upgrades.airResistance * 0.002f;
        trampElasticity    = 25f   + upgrades.elasticity    * 3f;
        maxInk             = 1000f + upgrades.inkReserve     * 150f;
        inkConsumptionRate = Math.max(0.1f, 0.4f - upgrades.inkEfficiency * 0.03f);
    }

    public void loadBallSkin() {
        String skinId = prefs.getRaw().getString("equipped_ball", "ball_basic");
        int color;
        currentBallSkin = skinId;
        switch (skinId) {
            // Classic
            case "ball_red":       color = Color.parseColor("#E82020"); break;
            case "ball_blue":      color = Color.parseColor("#2050E8"); break;
            case "ball_yellow":    color = Color.parseColor("#F5D800"); break;
            case "ball_green":     color = Color.parseColor("#20B020"); break;
            case "ball_orange":    color = Color.parseColor("#F07010"); break;
            case "ball_pink":      color = Color.parseColor("#F060A0"); break;
            case "ball_purple":    color = Color.parseColor("#8030D0"); break;
            case "ball_cyan":      color = Color.parseColor("#00C8D8"); break;
            case "ball_lime":      color = Color.parseColor("#80E000"); break;
            case "ball_brown":     color = Color.parseColor("#8B4513"); break;
            case "ball_beige":     color = Color.parseColor("#F5DEB3"); break;
            case "ball_white":     color = Color.parseColor("#F5F5F5"); break;
            case "ball_black":     color = Color.parseColor("#1A1A1A"); break;
            case "ball_lightgray": color = Color.parseColor("#C0C0C0"); break;
            case "ball_darkgray":  color = Color.parseColor("#505050"); break;
            // Metal
            case "ball_copper":    color = Color.parseColor("#BF6830"); break;
            case "ball_nickel":    color = Color.parseColor("#C8B87A"); break;
            case "ball_lead":      color = Color.parseColor("#707880"); break;
            case "ball_chrome":    color = Color.parseColor("#90CAF9"); break;
            case "ball_bronze":    color = Color.parseColor("#CD7F32"); break;
            case "ball_steel":     color = Color.parseColor("#6B7FA8"); break;
            case "ball_silver":    color = Color.parseColor("#B0BEC5"); break;
            case "ball_gold":      color = Color.parseColor("#FFD700"); break;
            case "ball_rosegold":  color = Color.parseColor("#E8A090"); break;
            case "ball_titanium":  color = Color.parseColor("#5B6B7C"); break;
            case "ball_platinum":  color = Color.parseColor("#D8DCE0"); break;
            case "ball_bismuth":   color = Color.parseColor("#C8A0C0"); break;
            case "ball_damascus":  color = Color.parseColor("#4A4A4A"); break;
            case "ball_meteorite": color = Color.parseColor("#7A7060"); break;
            // Space
            case "ball_void":         color = Color.parseColor("#212121"); break;
            case "ball_nebula":       color = Color.parseColor("#7B1FA2"); break;
            case "ball_comet":        color = Color.parseColor("#A0825A"); break;
            case "ball_mercury":      color = Color.parseColor("#9E9585"); break;
            case "ball_venus":        color = Color.parseColor("#E8D5A3"); break;
            case "ball_earth":        color = Color.parseColor("#1A6FA8"); break;
            case "ball_moon":         color = Color.parseColor("#B8B8B8"); break;
            case "ball_mars":         color = Color.parseColor("#C1440E"); break;
            case "ball_jupiter":      color = Color.parseColor("#C88B3A"); break;
            case "ball_saturn":       color = Color.parseColor("#C8A96E"); break;
            case "ball_uranus":       color = Color.parseColor("#7DE8E8"); break;
            case "ball_pluto":        color = Color.parseColor("#C4A882"); break;
            case "ball_red_dwarf":    color = Color.parseColor("#CC2200"); break;
            case "ball_yellow_dwarf": color = Color.parseColor("#FFD700"); break;
            case "ball_blue_giant":   color = Color.parseColor("#4488FF"); break;
            case "ball_black_hole":   color = Color.parseColor("#0A0010"); break;
            case "ball_pulsar":       color = Color.parseColor("#050520"); break;

            // Sport
            case "ball_soccer":   color = Color.parseColor("#F5F5F5"); break;
            case "ball_basket":   color = Color.parseColor("#E65100"); break;
            case "ball_tennis":   color = Color.parseColor("#CDDC39"); break;
            case "ball_bowling":  color = Color.parseColor("#C0392B"); break;
            case "ball_petanque": color = Color.parseColor("#A0A8B0"); break;
            case "ball_golf":     color = Color.parseColor("#F5F5F5"); break;
            case "ball_cateye": color = Color.parseColor("#00000000"); break;
            case "ball_beach":  color = Color.parseColor("#FFFFFF"); break;
            case "ball_volleyball": color = Color.parseColor("#F5E6C8"); break;
            case "ball_baseball": color = Color.parseColor("#F5EED8"); break;
            case "ball_8ball": color = Color.parseColor("#111111"); break;

            // Elemental
            case "ball_emerald":  color = Color.parseColor("#43A047"); break;
            case "ball_sapphire": color = Color.parseColor("#1E88E5"); break;
            case "ball_fire":     color = Color.parseColor("#FF5722"); break;
            case "ball_ice":      color = Color.parseColor("#80DEEA"); break;
            case "ball_thunder":  color = Color.parseColor("#FDD835"); break;
            case "ball_neptune":  color = Color.parseColor("#2A5FD4"); break;
            // Défaut
            default:              color = Color.parseColor("#E53935"); break;
        }
        ballPaint.setColor(color);
    }

    private void resetGame() {
        ballX = screenWidth  / 2f;
        ballY = screenHeight / 4f;
        ballVelocityY     = 0f;
        ballVelocityX     = 0f;
        currentInk        = maxInk;
        totalHeightMeters = 0f;
        isGameStarted     = false;
        isGameOver        = false;
        isDrawingTrampoline = false;
        hasTrampoline     = false;
        warpState         = WARP_NONE;
        warpBallScale     = 1f;
        ballRotation     = 0f;
        ballAngularSpeed = 0f;
        inkBlobs.clear();
        warps.clear();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        screenWidth  = getWidth();
        screenHeight = getHeight();
        resetGame();
        isRunning  = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int f, int w, int h) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        while (true) {
            try { gameThread.join(); break; }
            catch (InterruptedException e) { Log.e("GameView", "thread join", e); }
        }
    }

    @Override public void run() { while (isRunning) { update(); draw(); sleep(); } }

    // ══════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════
    private void update() {

        // HUD slide animation (toujours active)
        float hudDt    = 0.016f;
        float targetGX = hudShouldBeVisible ? 0f : -500f;
        float targetSY = hudShouldBeVisible ? 0f : -250f;
        if (gaugeOffsetX < targetGX)      gaugeOffsetX = Math.min(gaugeOffsetX + HUD_ANIM_SPEED * hudDt, targetGX);
        else if (gaugeOffsetX > targetGX) gaugeOffsetX = Math.max(gaugeOffsetX - HUD_ANIM_SPEED * hudDt, targetGX);
        if (statsOffsetY < targetSY)      statsOffsetY = Math.min(statsOffsetY + HUD_ANIM_SPEED * hudDt, targetSY);
        else if (statsOffsetY > targetSY) statsOffsetY = Math.max(statsOffsetY - HUD_ANIM_SPEED * hudDt, targetSY);

        if (!isGameStarted || isGameOver) return;

        // Machine a etats warp : suspend la physique normale
        if (warpState != WARP_NONE) {
            updateWarpAnimation();
            return;
        }

        // Physique
        ballVelocityY += GRAVITY;
        ballVelocityY *= airResistance;
        ballVelocityX *= airResistance;
        ballY += ballVelocityY;
        ballX += ballVelocityX;

        ballAngularSpeed = ballVelocityX / ballRadius * (180f / (float) Math.PI);
        ballRotation    += ballAngularSpeed;

        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            ballVelocityX    = -ballVelocityX * 0.8f;
            ballAngularSpeed = -ballAngularSpeed * 0.6f; // rebond inverse + amortissement
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballVelocityX    = -ballVelocityX * 0.8f;
            ballAngularSpeed = -ballAngularSpeed * 0.6f;
        }

        // Camera (Lerp)
        float idealCameraY = screenHeight * 0.4f;
        if (ballY < idealCameraY) {
            float shift = (idealCameraY - ballY) * 0.1f;
            ballY += shift;
            if (hasTrampoline) { trampStartY += shift; trampEndY += shift; }
            for (float[] b : inkBlobs) b[1] += shift;
            for (float[] w : warps)    w[1] += shift;
            totalHeightMeters += shift / 100f;
            currentRunGold += shift / 100f * upgrades.goldMultiplier;
        }

        // Rebond trampoline
        if (ballVelocityY > 0 && hasTrampoline) {
            float minX = Math.min(trampStartX, trampEndX);
            float maxX = Math.max(trampStartX, trampEndX);
            float minY = Math.min(trampStartY, trampEndY) - 30f;
            float maxY = Math.max(trampStartY, trampEndY) + 30f;
            if (ballX + ballRadius >= minX && ballX - ballRadius <= maxX
                    && ballY + ballRadius >= minY && ballY - ballRadius <= maxY) {
                float dx  = trampEndX - trampStartX;
                float dy  = trampEndY - trampStartY;
                float len = Math.max(50f, (float) Math.hypot(dx, dy));
                float nx  = -dy / len, ny = dx / len;
                if (ny > 0) { nx = -nx; ny = -ny; }
                float mult = Math.max(0.2f, Math.min(3f, screenWidth / len));
                float incomingSpeed = (float) Math.hypot(ballVelocityX, ballVelocityY);
                float bounceForce = (trampElasticity * mult) + (incomingSpeed * 0.5f);
                ballVelocityX = nx * bounceForce;
                ballVelocityY = ny * bounceForce;
                float tx = ny, ty = -nx;
                float tangentialSpeed = ballVelocityX * tx + ballVelocityY * ty;
// Spin proportionnel à la vitesse tangentielle et à l'angle d'inclinaison
                ballAngularSpeed = (tangentialSpeed / ballRadius) * (180f / (float) Math.PI) * 0.8f;
                hasTrampoline = false;
            }
        }

        spawnPowerUps();

        // Aimant + collecte billes
        Iterator<float[]> blobIt = inkBlobs.iterator();
        while (blobIt.hasNext()) {
            float[] blob = blobIt.next();
            float dx   = ballX - blob[0];
            float dy   = ballY - blob[1];
            float dist = (float) Math.hypot(dx, dy);
            if (dist < MAGNET_RADIUS && dist > 0.1f) {
                blob[0] += (dx / dist) * MAGNET_STRENGTH;
                blob[1] += (dy / dist) * MAGNET_STRENGTH;
                dist = (float) Math.hypot(ballX - blob[0], ballY - blob[1]);
            }
            if (dist < ballRadius + INK_BLOB_RADIUS) {
                currentInk = Math.min(currentInk + INK_RECHARGE, maxInk);
                blobIt.remove();
            }
        }

        // Collision warp
        Iterator<float[]> warpIt = warps.iterator();
        while (warpIt.hasNext()) {
            float[] w = warpIt.next();
            float wCx = w[0];
            if (ballX + ballRadius > wCx - WARP_WIDTH / 2f
                    && ballX - ballRadius < wCx + WARP_WIDTH / 2f
                    && ballY + ballRadius > w[1] - WARP_HEIGHT / 2f
                    && ballY - ballRadius < w[1] + WARP_HEIGHT / 2f) {
                startWarp(wCx);
                warpIt.remove();
                break;
            }
        }

        inkBlobs.removeIf(b -> b[1] > screenHeight + 100f);
        warps.removeIf(w -> w[1] > screenHeight + 100f);

        // Game Over
        if (ballY > screenHeight + ballRadius && !isGameOver) {
            isGameOver = true;
            float h = totalHeightMeters;
            if (gameStateListener != null) gameStateListener.onGameOver(h);
            resetGame();
        }
    }

    // ──────────────────────────────────────────────────
    // WARP STATE MACHINE
    // ──────────────────────────────────────────────────

    private void startWarp(float entryX) {
        exitPortalX     = entryX;
        exitPortalY     = screenHeight * 0.42f;
        warpState       = WARP_ABSORB;
        warpAbsorbTimer = ABSORB_DUR;
        warpBallScale   = 1f;
        float metersJump   = 80f + (float)(Math.random() * 40f);
        warpScrollLeft     = metersJump * 100f;
        totalHeightMeters += metersJump;
    }

    private void updateWarpAnimation() {
        switch (warpState) {

            case WARP_ABSORB:
                warpBallScale = (float) warpAbsorbTimer / ABSORB_DUR;
                warpAbsorbTimer--;
                if (warpAbsorbTimer <= 0) {
                    warpBallScale = 0f;
                    warpState = WARP_SCROLL;
                }
                break;

            case WARP_SCROLL:
                float step = Math.min(warpScrollLeft, Math.max(SCROLL_MIN, warpScrollLeft * SCROLL_EASE));
                if (hasTrampoline) { trampStartY += step; trampEndY += step; }
                for (float[] b : inkBlobs) b[1] += step;
                for (float[] w : warps)    w[1] += step;
                warpScrollLeft -= step;
                if (warpScrollLeft <= 0f) {
                    ballX = exitPortalX;
                    ballY = exitPortalY;
                    ballVelocityY  = -22f;
                    ballVelocityX  = 0f;
                    warpBallScale  = 1f;
                    warpEjectTimer = EJECT_DUR;
                    warpState      = WARP_EJECT;
                }
                break;

            case WARP_EJECT:
                ballVelocityY += GRAVITY;
                ballVelocityY *= airResistance;
                ballVelocityX *= airResistance;
                ballY += ballVelocityY;
                ballX += ballVelocityX;
                if (ballX - ballRadius < 0)             { ballX = ballRadius;              ballVelocityX *= -0.8f; }
                if (ballX + ballRadius > screenWidth)   { ballX = screenWidth - ballRadius; ballVelocityX *= -0.8f; }
                if (ballX - ballRadius < 0)           { ballX = ballRadius;              ballVelocityX *= -0.8f; ballAngularSpeed *= -0.6f; }
                if (ballX + ballRadius > screenWidth) { ballX = screenWidth - ballRadius; ballVelocityX *= -0.8f; ballAngularSpeed *= -0.6f; }
                warpEjectTimer--;
                if (warpEjectTimer <= 0) warpState = WARP_NONE;
                break;
        }
    }

    private void spawnPowerUps() {
        if (!isGameStarted) return;
        float margin = 60f;
        if (Math.random() < INK_SPAWN_CHANCE) {
            float x = margin + (float)(Math.random() * (screenWidth - 2 * margin));
            inkBlobs.add(new float[]{ x, -INK_BLOB_RADIUS });
        }
        if (Math.random() < WARP_SPAWN_CHANCE) {
            float halfW = WARP_WIDTH / 2f + margin;
            float x = halfW + (float)(Math.random() * (screenWidth - 2 * halfW));
            warps.add(new float[]{ x, -WARP_HEIGHT });
        }
    }

    // ══════════════════════════════════════════════════
    // DRAW
    // ══════════════════════════════════════════════════
    private void draw() {
        if (!surfaceHolder.getSurface().isValid()) return;
        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawColor(Color.WHITE);

        // Billes d'encre
        blobPaint.setStyle(Paint.Style.FILL);
        for (float[] blob : inkBlobs) {
            blobPaint.setColor(Color.BLACK);
            blobPaint.setAlpha(255);
            canvas.drawCircle(blob[0], blob[1], INK_BLOB_RADIUS, blobPaint);
            blobPaint.setColor(Color.WHITE);
            blobPaint.setAlpha(120);
            canvas.drawCircle(blob[0] - INK_BLOB_RADIUS * 0.25f,
                    blob[1] - INK_BLOB_RADIUS * 0.25f,
                    INK_BLOB_RADIUS * 0.3f, blobPaint);
        }

        // Warps en jeu
        warpPaint.setStyle(Paint.Style.FILL);
        for (float[] w : warps) drawWarpPortal(canvas, warpPaint, w[0], w[1], false);

        // Portail de sortie (SCROLL + EJECT)
        if (warpState == WARP_SCROLL || warpState == WARP_EJECT) {
            drawWarpPortal(canvas, warpPaint, exitPortalX, exitPortalY, true);
        }

        // Balle
        canvas.save();
        canvas.rotate(ballRotation, ballX, ballY);
        if (warpState == WARP_NONE || warpState == WARP_EJECT) {
            drawBall(canvas, ballX, ballY, ballRadius);
        } else if (warpState == WARP_ABSORB && warpBallScale > 0f) {
            drawBall(canvas, ballX, ballY, ballRadius * warpBallScale);
        }
        canvas.restore();
// Reflet statique hors rotation
        if (warpState != WARP_SCROLL) {
            float sr = (warpState == WARP_ABSORB) ? ballRadius * warpBallScale : ballRadius;
            if (!currentBallSkin.equals("ball_pulsar") && !currentBallSkin.equals("ball_blackhole")
                    && !currentBallSkin.equals("ball_red_dwarf") && !currentBallSkin.equals("ball_yellow_dwarf")
                    && !currentBallSkin.equals("ball_blue_giant")) {
                canvas.drawCircle(ballX - sr * 0.3f, ballY - sr * 0.3f, sr * 0.35f, ballShinePaint);
            }
        }
        //if (warpState == WARP_NONE || warpState == WARP_EJECT) {
        //    canvas.drawCircle(ballX, ballY, ballRadius, ballPaint);
        //    canvas.drawCircle(ballX - ballRadius * 0.3f, ballY - ballRadius * 0.3f, ballRadius * 0.35f, ballShinePaint);
        //} else if (warpState == WARP_ABSORB && warpBallScale > 0f) {
        //    float sr = ballRadius * warpBallScale;
        //    canvas.drawCircle(ballX, ballY, sr, ballPaint);
        //    canvas.drawCircle(ballX - sr * 0.3f, ballY - sr * 0.3f, sr * 0.35f, ballShinePaint);
        //}

        // Jauge d'encre
        canvas.save();
        canvas.translate(gaugeOffsetX, 0);
        float gaugeH      = screenHeight * 0.6f;
        float gaugeW      = gaugeH * (24f / 256f);
        float gaugeLeft   = 40f;
        float gaugeBottom = screenHeight * 0.95f;
        float gaugeTop    = gaugeBottom - gaugeH;
        float gaugeRight  = gaugeLeft + gaugeW;
        float inkWidth    = gaugeW * (16f / 32f);
        float inkLeft     = gaugeLeft + (gaugeW - inkWidth) / 2f;
        float inkRight    = inkLeft + inkWidth;
        float vMargin     = gaugeH * (6f / 256f);
        float inkMaxTop   = gaugeTop  + vMargin;
        float inkBottom2  = gaugeBottom - vMargin;
        float inkH        = (inkBottom2 - inkMaxTop) * (currentInk / maxInk);
        float inkTop      = inkBottom2 - inkH;
        canvas.save();
        canvas.clipRect(inkLeft, inkTop, inkRight, inkBottom2);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(inkLeft, inkMaxTop, inkRight, inkBottom2, inkWidth / 2f, inkWidth / 2f, paint);
        canvas.restore();
        canvas.drawBitmap(gaugeSprite, null, new RectF(gaugeLeft, gaugeTop, gaugeRight, gaugeBottom), paint);
        canvas.restore();

        // HUD stats
        canvas.save();
        canvas.translate(0, statsOffsetY);
        paint.setColor(Color.BLACK);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format(Locale.getDefault(), "%.1fm", totalHeightMeters), screenWidth / 2f, 100, paint);
        paint.setTextSize(36);
        paint.setColor(Color.parseColor("#888888"));
        canvas.drawText(String.format(Locale.getDefault(), "Record: %.1fm", prefs.getMaxHeight()), screenWidth / 2f, 150, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(40);
        canvas.drawText("Or: " + prefs.getGold() + " (+" + (int)currentRunGold + ")", screenWidth / 2f, 200, paint);
        canvas.restore();

        // Trampoline
        if (isDrawingTrampoline || hasTrampoline) {
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(15f);
            canvas.drawLine(trampStartX, trampStartY, trampEndX, trampEndY, paint);
            paint.setStrokeWidth(0f);
        }

        surfaceHolder.unlockCanvasAndPost(canvas);
    }
    private void drawBall(Canvas canvas, float cx, float cy, float r) {
        switch (currentBallSkin) {
            case "ball_basket":       drawBasketball(canvas, cx, cy, r);  break;
            case "ball_tennis":       drawTennisBall(canvas, cx, cy, r);  break;
            case "ball_bowling":      drawBowlingBall(canvas, cx, cy, r); break;
            case "ball_soccer":       drawSoccerBall(canvas, cx, cy, r);  break;
            case "ball_petanque":     drawPetanque(canvas, cx, cy, r);    break;
            case "ball_golf":         drawGolf(canvas, cx, cy, r);        break;
            case "ball_cateye":       drawCatEye(canvas, cx, cy, r);      break;
            case "ball_beach":        drawBeachBall(canvas, cx, cy, r);   break;
            case "ball_volleyball":   drawVolleyball(canvas, cx, cy, r);  break;
            case "ball_baseball":     drawBaseball(canvas, cx, cy, r);    break;
            case "ball_8ball":        draw8Ball(canvas, cx, cy, r);       break;
            case "ball_gold":
            case "ball_silver":
            case "ball_copper":
            case "ball_chrome":
            case "ball_lead":
            case "ball_bronze":
            case "ball_titanium":
            case "ball_steel":
            case "ball_nickel":
            case "ball_rosegold":
            case "ball_platinum":     drawMetalBall(canvas, cx, cy, r); break;
            case "ball_bismuth":      drawBismuth(canvas, cx, cy, r);   break;
            case "ball_damascus":     drawDamascus(canvas, cx, cy, r);  break;
            case "ball_meteorite":    drawMeteorite(canvas, cx, cy, r); break;
            case "ball_comet":        drawComet(canvas, cx, cy, r); break;
            case "ball_mercury":      drawMercury(canvas, cx, cy, r); break;
            case "ball_venus":        drawVenus(canvas, cx, cy, r);   break;
            case "ball_earth":        drawEarth(canvas, cx, cy, r);   break;
            case "ball_moon":         drawMoon(canvas, cx, cy, r); break;
            case "ball_mars":         drawMars(canvas, cx, cy, r);    break;
            case "ball_jupiter":      drawJupiter(canvas, cx, cy, r); break;
            case "ball_saturn":       drawSaturn(canvas, cx, cy, r);  break;
            case "ball_uranus":       drawUranus(canvas, cx, cy, r); break;
            case "ball_neptune":      drawNeptune(canvas, cx, cy, r); break;
            case "ball_pluto":        drawPluto(canvas, cx, cy, r); break;
            case "ball_red_dwarf":    drawStar(canvas, cx, cy, r, new int[]{0xFFFFEEDD, 0xFFCC2200, 0xFFAA1800, 0xFFFF4400}); break;
            case "ball_yellow_dwarf": drawStar(canvas, cx, cy, r, new int[]{0xFFFFFFEE, 0xFFFFD700, 0xFFE8A800, 0xFFFFAA00}); break;
            case "ball_blue_giant":   drawStar(canvas, cx, cy, r, new int[]{0xFFEEF4FF, 0xFF4488FF, 0xFF2255DD, 0xFF88CCFF}); break;
            case "ball_black_hole":   drawBlackHole(canvas, cx, cy, r); break;
            case "ball_pulsar":       drawPulsar(canvas, cx, cy, r); break;

            default:
                canvas.drawCircle(cx, cy, r, ballPaint);
                //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
                break;
        }
    }

    private void drawBasketball(Canvas canvas, float cx, float cy, float r) {
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ballPaint);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint seam = new Paint(Paint.ANTI_ALIAS_FLAG);
        seam.setColor(Color.parseColor("#3E1F00"));
        seam.setStyle(Paint.Style.STROKE);
        seam.setStrokeWidth(r * 0.07f);

        // Croix centrale (deux lignes droites qui se coupent pile au centre)
        canvas.drawLine(cx - r, cy, cx + r, cy, seam);
        canvas.drawLine(cx, cy - r, cx, cy + r, seam);

        // Lignes courbes (même logique que tennis, couleur noire)
        float gap = r * 0.80f;
        float cpX = r * 0.35f;
        float cpY = r * -0.10f;

        Path top = new Path();
        top.moveTo(cx - r * 0.95f, cy - gap);
        top.cubicTo(cx - cpX, cy + cpY,
                cx + cpX, cy + cpY,
                cx + r * 0.95f, cy - gap);
        canvas.drawPath(top, seam);

        Path bot = new Path();
        bot.moveTo(cx - r * 0.95f, cy + gap);
        bot.cubicTo(cx - cpX, cy - cpY,
                cx + cpX, cy - cpY,
                cx + r * 0.95f, cy + gap);
        canvas.drawPath(bot, seam);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawTennisBall(Canvas canvas, float cx, float cy, float r) {
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ballPaint);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.WHITE);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(r * 0.13f);

        // ── Écart entre les 2 lignes ──────────────────────────
        float gap = r * 0.82f; // décalage vertical de chaque courbe par rapport au centre
        // Augmenter = plus d'espace entre les deux lignes

        // ── Resserrement horizontal des points de contrôle ────
        float cpX = r * 0.45f; // distance X des points de contrôle depuis le centre
        // Diminuer = courbe plus bombée / Augmenter = courbe plus plate

        // ── Amplitude du bombage vers le centre ───────────────
        float cpY = r * -0.30f; // de combien les ctrl points dépassent l'équateur vers le centre
        // Augmenter = courbe bomb davantage vers le centre

        // Courbe nord : part de cy-gap, les ctrl points descendent vers cy+cpY → bombe vers centre
        Path top = new Path();
        top.moveTo(cx - r * 0.95f, cy - gap);          // point de départ : pôle gauche, décalé haut
        top.cubicTo(cx - cpX, cy + cpY,                // ctrl 1 : tire vers le bas (centre)
                cx + cpX, cy + cpY,                // ctrl 2 : tire vers le bas (centre)
                cx + r * 0.95f, cy - gap);          // point d'arrivée : pôle droit, même décalage
        canvas.drawPath(top, line);

        // Courbe sud : miroir exact, ctrl points montent vers cy-cpY → bombe vers centre
        Path bot = new Path();
        bot.moveTo(cx - r * 0.95f, cy + gap);          // point de départ : pôle gauche, décalé bas
        bot.cubicTo(cx - cpX, cy - cpY,                // ctrl 1 : tire vers le haut (centre)
                cx + cpX, cy - cpY,                // ctrl 2 : tire vers le haut (centre)
                cx + r * 0.95f, cy + gap);          // point d'arrivée : pôle droit, même décalage
        canvas.drawPath(bot, line);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawBowlingBall(Canvas canvas, float cx, float cy, float r) {
        // Base violet foncé
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ballPaint);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Taches blanchâtres de marbrure
        Paint marble = new Paint(Paint.ANTI_ALIAS_FLAG);
        marble.setStyle(Paint.Style.FILL);
        marble.setColor(Color.WHITE);

        marble.setAlpha(22);
        canvas.drawOval(new RectF(cx - r * 0.55f, cy - r * 0.75f,
                cx + r * 0.15f,  cy + r * 0.05f), marble);
        marble.setAlpha(14);
        canvas.drawOval(new RectF(cx - r * 0.05f, cy - r * 0.15f,
                cx + r * 0.65f,  cy + r * 0.55f), marble);
        marble.setAlpha(10);
        canvas.drawOval(new RectF(cx - r * 0.3f,  cy + r * 0.2f,
                cx + r * 0.4f,   cy + r * 0.75f), marble);

        canvas.restore();

        // 3 trous décalés vers le haut-droite (pas centrés, pour voir la rotation)
        Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        hole.setStyle(Paint.Style.FILL);
        hole.setColor(Color.parseColor("#4A0000"));
        float hr = r * 0.095f;
        float hcx = cx + r * 0.30f;
        float hcy = cy - r * 0.10f;
        canvas.drawCircle(hcx,              hcy,              hr, hole);
        canvas.drawCircle(hcx + r * 0.27f,  hcy + r * 0.14f,  hr, hole);
        canvas.drawCircle(hcx - r * 0.06f,  hcy + r * 0.28f,  hr, hole);

        // Ombre intérieure sur chaque trou pour l'effet de profondeur
        Paint holeShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        holeShadow.setStyle(Paint.Style.STROKE);
        holeShadow.setStrokeWidth(hr * 0.4f);
        holeShadow.setColor(Color.BLACK);
        holeShadow.setAlpha(80);
        canvas.drawCircle(hcx,              hcy,              hr * 0.6f, holeShadow);
        canvas.drawCircle(hcx + r * 0.27f,  hcy + r * 0.14f,  hr * 0.6f, holeShadow);
        canvas.drawCircle(hcx - r * 0.06f,  hcy + r * 0.28f,  hr * 0.6f, holeShadow);

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawSoccerBall(Canvas canvas, float cx, float cy, float r) {
        // ── Fond avec gradient radial (lumière en haut-gauche) ──
        float lightX = cx - r * 0.25f;
        float lightY = cy - r * 0.25f;
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                lightX, lightY, r * 1.2f,
                new int[]{ 0xFFFFFFFF, 0xFFE8E8E8, 0xFFCCCCCC },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // ── Pentagone central ──
        float pr = r * 0.28f;
        Paint pent = new Paint(Paint.ANTI_ALIAS_FLAG);
        pent.setStyle(Paint.Style.FILL);
        // Le pentagone central est légèrement plus clair (il est "devant" sur la sphère)
        pent.setColor(Color.parseColor("#222222"));
        canvas.drawPath(regularPentagon(cx, cy, pr, -(float)Math.PI / 2), pent);

        // ── 5 pentagones externes ──
        // Chacun rétrécit proportionnellement à son éloignement du point lumineux
        float ringDist = r * 0.74f;
        for (int i = 0; i < 5; i++) {
            float angle    = -(float)Math.PI / 2 + (float)(Math.PI * 2 * i / 5);
            float px       = cx + (float)Math.cos(angle) * ringDist;
            float py       = cy + (float)Math.sin(angle) * ringDist;

            // Distance normalisée au point lumineux → facteur d'échelle (0.82 à 1.0)
            float dxL = px - lightX;
            float dyL = py - lightY;
            float distToLight = (float)Math.sqrt(dxL * dxL + dyL * dyL);
            float scale = 1.0f - 0.18f * Math.min(1f, distToLight / (2f * r));

            float outerPr = r * 0.26f * scale;

            // Teinte plus sombre pour les pentagones côté ombre
            float brightness = 1f - 0.25f * Math.min(1f, distToLight / (2f * r));
            int grey = (int)(0x22 * brightness); // de #222 à #191919
            pent.setColor(Color.rgb(grey, grey, grey));

            float startAngle = angle + (float)Math.PI;
            canvas.drawPath(regularPentagon(px, py, outerPr, startAngle), pent);
        }

        // ── Ombre interne en bas-droite (donne le volume) ──
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.35f, cy + r * 0.35f, r * 0.9f,
                new int[]{ 0x00000000, 0x00000000, 0x22000000 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        canvas.restore();

        // ── Contour ──
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(Color.parseColor("#BBBBBB"));
        stroke.setStrokeWidth(r * 0.025f);
        canvas.drawCircle(cx, cy, r * 0.988f, stroke);

        // ── Reflet ──
        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawPetanque(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new LinearGradient(
                cx - r * 0.7f, cy - r * 0.7f,
                cx + r * 0.7f, cy + r * 0.7f,
                new int[]{ 0xFFDDE0E8, 0xFFA0A8B8, 0xFF606878, 0xFF282E38 },
                new float[]{ 0f, 0.38f, 0.68f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.35f, cy + r * 0.40f, r,
                new int[]{ 0x00000000, 0x55000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        // Traits : légèrement plus clairs que le corps, doux
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(Color.parseColor("#C8D0DC"));
        line.setStrokeWidth(r * 0.025f);
        line.setAlpha(130);

        // 3 traits verticaux espacés régulièrement
        float spacing = r * 0.28f;
        canvas.drawLine(cx - spacing, cy - r, cx - spacing, cy + r, line);
        canvas.drawLine(cx,           cy - r, cx,           cy + r, line);
        canvas.drawLine(cx + spacing, cy - r, cx + spacing, cy + r, line);

        // 3 traits horizontaux espacés régulièrement
        canvas.drawLine(cx - r, cy - spacing, cx + r, cy - spacing, line);
        canvas.drawLine(cx - r, cy,           cx + r, cy,           line);
        canvas.drawLine(cx - r, cy + spacing, cx + r, cy + spacing, line);

        // Cercle gravé près du bord
        line.setStrokeWidth(r * 0.022f);
        canvas.drawCircle(cx, cy, r * 0.82f, line);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawGolf(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.25f, r * 1.1f,
                new int[]{ 0xFFFFFFFF, 0xFFF0F0F0, 0xFFD0D0D0 },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint dimpleShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimpleShadow.setStyle(Paint.Style.FILL);

        Paint dimpleLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimpleLight.setStyle(Paint.Style.FILL);
        dimpleLight.setColor(Color.WHITE);

        float dimpleR  = r * 0.052f;
        float spacingX = dimpleR * 3.2f;
        float spacingY = dimpleR * 2.9f;

        int cols = (int)(2f * r / spacingX) + 2;
        int rows = (int)(2f * r / spacingY) + 2;

        for (int row = -rows; row <= rows; row++) {
            for (int col = -cols; col <= cols; col++) {
                float offsetX = (row % 2 == 0) ? 0f : spacingX * 0.5f;
                float dx = cx + col * spacingX + offsetX;
                float dy = cy + row * spacingY;

                // On laisse le clipPath couper naturellement — pas de filtre artificiel
                float dist = (float) Math.hypot(dx - cx, dy - cy);
                if (dist > r * 1.05f) continue;

                float vertFactor = (dy - (cy - r)) / (2f * r);
                float distFactor = dist / r;
                float depth = 0.08f + vertFactor * 0.35f + distFactor * 0.18f;
                depth = Math.min(depth, 0.75f);

                int shadowAlpha = (int)(depth * 140f);
                int lightAlpha  = (int)((1f - depth) * 90f);

                dimpleShadow.setColor(Color.parseColor("#999999"));
                dimpleShadow.setAlpha(shadowAlpha);
                canvas.drawCircle(dx + dimpleR * 0.20f,
                        dy + dimpleR * 0.20f,
                        dimpleR, dimpleShadow);

                dimpleShadow.setColor(Color.parseColor("#BBBBBB"));
                dimpleShadow.setAlpha((int)(shadowAlpha * 0.5f));
                canvas.drawCircle(dx, dy, dimpleR * 0.55f, dimpleShadow);

                dimpleLight.setAlpha(lightAlpha);
                canvas.drawCircle(dx - dimpleR * 0.18f,
                        dy - dimpleR * 0.18f,
                        dimpleR * 0.40f, dimpleLight);
            }
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawCatEye(Canvas canvas, float cx, float cy, float r) {
        Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
        glass.setStyle(Paint.Style.FILL);
        glass.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.25f, r * 1.1f,
                new int[]{ 0x6644DDBB, 0x4422BB99, 0x6600AA88 },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, glass);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        float topY = cy - r * 0.95f;
        float botY = cy + r * 0.95f;
        float y1   = cy - r * 0.30f;
        float y2   = cy + r * 0.30f;
        float bow  = r * 0.55f;
        float w    = r * 0.16f;

        // 4 bords, chacun avec un offset horizontal intégré dans ses contrôles.
        // Tous partagent (cx, topY) et (cx, botY) — pointes communes.
        // L'offset ox pousse le contrôle haut vers la gauche et bas vers la droite,
        // garantissant que l'ordre gauche→droite est conservé sans croisement.
        float[] ox = { -1.5f * w, -0.5f * w, 0.5f * w, 1.5f * w };

        int[] colors = {
                Color.parseColor("#E02020"),
                Color.parseColor("#F0D000"),
                Color.parseColor("#2040D0"),
        };

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 3; i++) {
            float ox0 = ox[i];
            float ox1 = ox[i + 1];
            fill.setColor(colors[i]);

            Path ribbon = new Path();
            ribbon.moveTo(cx, topY);
            // Bord gauche : descend, ox0 intégré dans les contrôles
            ribbon.cubicTo(cx + ox0 - bow, y1,
                    cx + ox0 + bow, y2,
                    cx, botY);
            // Bord droit : remonte (contrôles inversés), ox1 intégré
            ribbon.cubicTo(cx + ox1 + bow, y2,
                    cx + ox1 - bow, y1,
                    cx, topY);
            ribbon.close();
            canvas.drawPath(ribbon, fill);
        }

        canvas.restore();

        Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        shine.setStyle(Paint.Style.FILL);
        shine.setShader(new RadialGradient(
                cx - r * 0.30f, cy - r * 0.32f, r * 0.38f,
                new int[]{ 0x66FFFFFF, 0x00FFFFFF },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx - r * 0.30f, cy - r * 0.32f, r * 0.38f, shine);
    }

    private void drawBeachBall(Canvas canvas, float cx, float cy, float r) {
        // Pole excentré (haut-gauche, comme dans l'image)
        float px = cx - r * 0.10f;
        float py = cy - r * 0.20f;

        // Base blanche
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Angles des frontières (depuis le centre de la balle, en degrés)
        // Non uniformes pour simuler la perspective
        float[] bounds = { -108f, -48f, 18f, 78f, 150f, 222f };

        // Segments entre bounds[i] et bounds[i+1] :
        // blanc, bleu, blanc, jaune, blanc, rose
        int[] segColors = {
                Color.WHITE,
                Color.parseColor("#3399EE"),
                Color.WHITE,
                Color.parseColor("#FFD700"),
                Color.WHITE,
                Color.parseColor("#EE1899"),
        };

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);

        int n = bounds.length;
        for (int i = 0; i < n; i++) {
            if (segColors[i] == Color.WHITE) continue; // segments blancs déjà couverts par la base

            float a1 = bounds[i];
            float a2 = bounds[(i + 1) % n];
            float sweep = a2 - a1;
            if (sweep < 0) sweep += 360f;

            float x1 = cx + r * (float)Math.cos(Math.toRadians(a1));
            float y1 = cy + r * (float)Math.sin(Math.toRadians(a1));

            fill.setColor(segColors[i]);
            Path seg = new Path();
            seg.moveTo(px, py);
            seg.lineTo(x1, y1);
            seg.arcTo(new RectF(cx - r, cy - r, cx + r, cy + r), a1, sweep);
            seg.close();
            canvas.drawPath(seg, fill);
        }

        // Léger ombrage sphérique pour le relief
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.30f, cy + r * 0.35f, r * 1.0f,
                new int[]{ 0x00000000, 0x28000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        // Petit cercle blanc de jonction (pole)
        Paint pole = new Paint(Paint.ANTI_ALIAS_FLAG);
        pole.setStyle(Paint.Style.FILL);
        pole.setColor(Color.WHITE);
        canvas.drawCircle(px, py, r * 0.09f, pole);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawVolleyball(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFFFF0D8, 0xFFF5E6C8, 0xFFD4C090 },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(Color.parseColor("#2A1A08"));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(r * 0.030f);
        line.setAlpha(210);

        double[] angles = {
                Math.PI / 2.0,
                Math.PI / 2.0 + 2.0 * Math.PI / 3.0,
                Math.PI / 2.0 + 4.0 * Math.PI / 3.0
        };

        // 3 droites du centre vers le bord
        for (double a : angles) {
            canvas.drawLine(cx, cy,
                    cx + (float) Math.cos(a) * r * 0.97f,
                    cy + (float) Math.sin(a) * r * 0.97f, line);
        }

        // Pour chaque droite i :
        // - courbe A : part du bord près de la droite i+1, arrive à 1/3 de la droite i
        // - courbe B : part du bord près de la droite i+2, arrive à 2/3 de la droite i
        for (int i = 0; i < 3; i++) {
            double aBase = angles[i];
            double aNext = angles[(i + 1) % 3];

            float s1X = cx + (float) Math.cos(aNext) * r * 0.33f;
            float s1Y = cy + (float) Math.sin(aNext) * r * 0.33f;

            float s2X = cx + (float) Math.cos(aNext) * r * 0.66f;
            float s2Y = cy + (float) Math.sin(aNext) * r * 0.66f;

            double endAngle1 = aBase + 0.30;
            double endAngle2 = aBase + 0.61;

            float e1X = cx + (float) Math.cos(endAngle1) * r;
            float e1Y = cy + (float) Math.sin(endAngle1) * r;

            float e2X = cx + (float) Math.cos(endAngle2) * r;
            float e2Y = cy + (float) Math.sin(endAngle2) * r;

            double cpAngle1 = aBase + 1.2;
            float cp1X = cx + (float) Math.cos(cpAngle1) * r * 0.5f;
            float cp1Y = cy + (float) Math.sin(cpAngle1) * r * 0.5f;

            double cpAngle2 = aBase + 1.35;
            float cp2X = cx + (float) Math.cos(cpAngle2) * r * 0.85f;
            float cp2Y = cy + (float) Math.sin(cpAngle2) * r * 0.85f;

            Path curveA = new Path();
            curveA.moveTo(s1X, s1Y);
            curveA.quadTo(cp1X, cp1Y, e1X, e1Y);
            canvas.drawPath(curveA, line);

            Path curveB = new Path();
            curveB.moveTo(s2X, s2Y);
            curveB.quadTo(cp2X, cp2Y, e2X, e2Y);
            canvas.drawPath(curveB, line);
        }
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.3f, cy + r * 0.35f, r,
                new int[]{ 0x00000000, 0x30000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawBaseball(Canvas canvas, float cx, float cy, float r) {
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ballPaint);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.parseColor("#1A0A0A"));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(r * 0.07f);

        float gap = r * 0.62f; // plus proche du centre que le tennis
        float cpX = r * 0.45f;
        float cpY = r * -0.30f;

        Path top = new Path();
        top.moveTo(cx - r * 0.95f, cy - gap);
        top.cubicTo(cx - cpX, cy + cpY,
                cx + cpX, cy + cpY,
                cx + r * 0.95f, cy - gap);
        canvas.drawPath(top, line);

        Path bot = new Path();
        bot.moveTo(cx - r * 0.95f, cy + gap);
        bot.cubicTo(cx - cpX, cy - cpY,
                cx + cpX, cy - cpY,
                cx + r * 0.95f, cy + gap);
        canvas.drawPath(bot, line);

        // ── Points de suture le long des 2 courbes ───────────
        // On échantillonne la courbe en t=[0..1] et on place
        // un petit trait perpendiculaire à la tangente à chaque point
        Paint stitch = new Paint(Paint.ANTI_ALIAS_FLAG);
        stitch.setStyle(Paint.Style.STROKE);
        stitch.setStrokeCap(Paint.Cap.ROUND);
        stitch.setColor(Color.parseColor("#CC1111"));
        stitch.setStrokeWidth(r * 0.033f);

        int stitchCount = 9;
        float stitchLen = r * 0.10f;

        for (int s = 0; s < 2; s++) {
            float ySign = (s == 0) ? -1f : 1f; // top ou bottom

            for (int i = 1; i <= stitchCount; i++) {
                float t = i / (float)(stitchCount + 1);

                // Évaluation du point sur la courbe cubique de Bézier
                float mt  = 1f - t;
                float mt2 = mt * mt;
                float mt3 = mt2 * mt;
                float t2  = t * t;
                float t3  = t2 * t;

                float p0x = cx - r * 0.95f;
                float p0y = cy + ySign * gap;
                float p1x = cx - cpX;
                float p1y = cy + ySign * cpY * -1f; // ctrl 1
                float p2x = cx + cpX;
                float p2y = cy + ySign * cpY * -1f; // ctrl 2
                float p3x = cx + r * 0.95f;
                float p3y = cy + ySign * gap;

                float bx = mt3 * p0x + 3 * mt2 * t * p1x + 3 * mt * t2 * p2x + t3 * p3x;
                float by = mt3 * p0y + 3 * mt2 * t * p1y + 3 * mt * t2 * p2y + t3 * p3y;

                // Tangente (dérivée de la cubique)
                float dx = 3 * mt2 * (p1x - p0x) + 6 * mt * t * (p2x - p1x) + 3 * t2 * (p3x - p2x);
                float dy = 3 * mt2 * (p1y - p0y) + 6 * mt * t * (p2y - p1y) + 3 * t2 * (p3y - p2y);
                float len = (float) Math.hypot(dx, dy);
                if (len < 0.001f) continue;

                // Perpendiculaire à la tangente
                float nx = -dy / len;
                float ny =  dx / len;

                canvas.drawLine(
                        bx + nx * stitchLen, by + ny * stitchLen,
                        bx - nx * stitchLen, by - ny * stitchLen,
                        stitch);
            }
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void draw8Ball(Canvas canvas, float cx, float cy, float r) {
        // Base noire avec gradient sphérique
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFF444444, 0xFF111111, 0xFF000000 },
                new float[]{ 0f, 0.45f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Le centre du cercle blanc est excentré vers le haut-gauche
        // pour simuler que le 8 n'est pas face à nous
        float ocx = cx - r * 0.12f;
        float ocy = cy - r * 0.10f;

        // Cercle blanc aplati (ellipse) — la courbure de la balle
        // le déforme légèrement : plus large que haut
        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setStyle(Paint.Style.FILL);
        white.setColor(Color.WHITE);
        float ow = r * 0.48f; // demi-largeur
        float oh = r * 0.44f; // demi-hauteur (légèrement aplati)
        canvas.drawOval(new RectF(ocx - ow, ocy - oh, ocx + ow, ocy + oh), white);

        // ── Chiffre 8 ────────────────────────────────────────
        // Dessiné avec 2 ovales noirs superposés (lobe haut + lobe bas)
        // légèrement déformés pour coller à la perspective
        Paint eight = new Paint(Paint.ANTI_ALIAS_FLAG);
        eight.setStyle(Paint.Style.FILL);
        eight.setColor(Color.BLACK);

        // Fond noir du 8 (rectangle arrondi englobant)
        float ew  = r * 0.20f;
        float eh  = r * 0.34f;
        float ex  = ocx + r * 0.01f;
        float ey  = ocy + r * 0.01f;

        // Lobe supérieur : plus petit
        canvas.drawOval(new RectF(ex - ew * 0.80f, ey - eh * 0.95f,
                ex + ew * 0.80f, ey - eh * 0.05f), eight);

        // Lobe inférieur : légèrement plus large
        canvas.drawOval(new RectF(ex - ew * 0.95f, ey - eh * 0.10f,
                ex + ew * 0.95f, ey + eh * 0.90f), eight);

        // Trous blancs du 8 (intérieur des lobes)
        eight.setColor(Color.WHITE);

        // Trou haut
        canvas.drawOval(new RectF(ex - ew * 0.42f, ey - eh * 0.82f,
                ex + ew * 0.42f, ey - eh * 0.22f), eight);

        // Trou bas
        canvas.drawOval(new RectF(ex - ew * 0.52f, ey - eh * 0.00f,
                ex + ew * 0.52f, ey + eh * 0.72f), eight);

        // Ombre sphérique
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.35f, cy + r * 0.40f, r,
                new int[]{ 0x00000000, 0x55000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawMetalBall(Canvas canvas, float cx, float cy, float r) {
        int[] colors;
        switch (currentBallSkin) {
            case "ball_gold":
                colors = new int[]{
                        0xFFFFF5A0, 0xFFFFD700, 0xFFC8A000, 0xFF7A5800 };
                break;
            case "ball_silver":
                colors = new int[]{
                        0xFFFFFFFF, 0xFFC8D0D8, 0xFF8090A0, 0xFF404850 };
                break;
            case "ball_copper":
                colors = new int[]{
                        0xFFFFCBA0, 0xFFBF6830, 0xFF8B4010, 0xFF4A1800 };
                break;
            case "ball_chrome":
                colors = new int[]{
                        0xFFFFFFFF, 0xFFB8D0E8, 0xFF6090B8, 0xFF203848 };
                break;
            case "ball_lead":
                colors = new int[]{
                        0xFFB0B8C0, 0xFF707880, 0xFF404850, 0xFF202428 };
                break;
            case "ball_bronze":
                colors = new int[]{ 0xFFEDAA70, 0xFFCD7F32, 0xFF8B4A10, 0xFF4A2005 };
                break;
            case "ball_titanium":
                colors = new int[]{ 0xFFAAB8C8, 0xFF5B6B7C, 0xFF334455, 0xFF151E28 };
                break;
            case "ball_steel":
                colors = new int[]{ 0xFFCCD4E8, 0xFF6B7FA8, 0xFF384870, 0xFF182038 };
                break;
            case "ball_nickel":
                colors = new int[]{ 0xFFEEDDA0, 0xFFC8B87A, 0xFF8A7A40, 0xFF454020 };
                break;
            case "ball_rosegold":
                colors = new int[]{ 0xFFFFF0EC, 0xFFE8A090, 0xFFB86858, 0xFF703830 };
                break;
            case "ball_platinum":
            default:
                colors = new int[]{
                        0xFFF8F8F8, 0xFFD8DCE0, 0xFFA0A8B0, 0xFF606870 };
                break;

        }

        float[] stops = { 0f, 0.35f, 0.72f, 1f };

        // Gradient diagonal haut-gauche → bas-droite (source lumineuse fixe)
        Paint metalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        metalPaint.setStyle(Paint.Style.FILL);
        metalPaint.setShader(new LinearGradient(
                cx - r * 0.7f, cy - r * 0.7f,
                cx + r * 0.7f, cy + r * 0.7f,
                colors, stops,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, metalPaint);

        // Ombre interne bas-droite pour le volume
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.3f, cy + r * 0.3f, r * 0.85f,
                new int[]{ 0x00000000, 0x00000000, 0x30000000 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        // Reflet
        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawBismuth(Canvas canvas, float cx, float cy, float r) {
        // Base argentée
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new LinearGradient(
                cx - r * 0.7f, cy - r * 0.7f,
                cx + r * 0.7f, cy + r * 0.7f,
                new int[]{ 0xFFD8D0E0, 0xFFB0A8C0, 0xFF908898, 0xFF605870 },
                new float[]{ 0f, 0.35f, 0.65f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Couches irisées superposées — chacune à un angle différent
        // comme les facettes d'oxydation naturelle du bismuth
        int[][] iridLayers = {
                // {angle_start_x, angle_start_y, angle_end_x, angle_end_y, color1, color2, alpha}
        };

        // On dessine 6 zones rectangulaires colorées à angles variés
        // simulant les marches d'escalier du bismuth cristallisé
        float[][] zones = {
                // {x1, y1, x2, y2, rotation, color_hex_int, alpha}
        };

        Paint irid = new Paint(Paint.ANTI_ALIAS_FLAG);
        irid.setStyle(Paint.Style.FILL);

        // Zone rose-magenta (haut-gauche)
        irid.setShader(new LinearGradient(
                cx - r, cy - r,
                cx,     cy,
                new int[]{ 0x00FF80C0, 0x55FF40A0, 0x00FF80C0 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Zone bleue (haut-droite)
        irid.setShader(new LinearGradient(
                cx + r * 0.2f, cy - r,
                cx - r * 0.3f, cy + r * 0.5f,
                new int[]{ 0x004488FF, 0x6644AAFF, 0x002266CC },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Zone verte (centre-gauche)
        irid.setShader(new LinearGradient(
                cx - r, cy + r * 0.1f,
                cx + r * 0.4f, cy - r * 0.4f,
                new int[]{ 0x0040CC88, 0x4440EE88, 0x0020AA60 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Zone jaune-or (bas-droite)
        irid.setShader(new LinearGradient(
                cx + r * 0.1f, cy + r * 0.2f,
                cx - r * 0.5f, cy - r * 0.3f,
                new int[]{ 0x00FFD700, 0x55FFCC00, 0x00EE9900 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Zone violet-cyan (bas-gauche)
        irid.setShader(new LinearGradient(
                cx - r * 0.3f, cy + r * 0.5f,
                cx + r * 0.5f, cy - r * 0.2f,
                new int[]{ 0x0088FFEE, 0x4400CCCC, 0x00006688 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Zone orange (centre-haut)
        irid.setShader(new LinearGradient(
                cx,            cy - r * 0.5f,
                cx + r * 0.3f, cy + r * 0.6f,
                new int[]{ 0x00FF8800, 0x44FFAA00, 0x00CC5500 },
                new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, irid);

        // Lignes de cristallisation : fines lignes argentées simulant
        // les marches d'escalier caractéristiques du bismuth
        Paint crystal = new Paint(Paint.ANTI_ALIAS_FLAG);
        crystal.setStyle(Paint.Style.STROKE);
        crystal.setColor(Color.parseColor("#E0D8F0"));
        crystal.setStrokeWidth(r * 0.012f);
        crystal.setAlpha(80);

        // Grille de marches horizontales/verticales décalées
        float step = r * 0.28f;
        for (float offset = -r; offset < r; offset += step) {
            // Horizontales
            canvas.drawLine(cx - r, cy + offset, cx + r, cy + offset, crystal);
            // Verticales décalées (motif escalier)
            canvas.drawLine(cx + offset, cy - r, cx + offset, cy + r, crystal);
        }

        // Reflet
        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);

        canvas.restore();
    }

    private void drawDamascus(Canvas canvas, float cx, float cy, float r) {
        // Base acier sombre
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new LinearGradient(
                cx - r * 0.7f, cy - r * 0.7f,
                cx + r * 0.7f, cy + r * 0.7f,
                new int[]{ 0xFF707070, 0xFF484848, 0xFF282828, 0xFF181818 },
                new float[]{ 0f, 0.35f, 0.65f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Ondulations damas : lignes sinueuses très serrées alternant
        // clair/sombre — le motif Widmanstätten du damas
        Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        light.setStyle(Paint.Style.STROKE);
        light.setStrokeCap(Paint.Cap.ROUND);

        Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
        dark.setStyle(Paint.Style.STROKE);
        dark.setStrokeCap(Paint.Cap.ROUND);

        float spacing = r * 0.075f;
        int lineCount = (int)(2f * r / spacing) + 2;

        for (int i = 0; i < lineCount; i++) {
            float baseY = (cy - r) + i * spacing;

            // Amplitude et phase varient ligne par ligne pour l'aspect organique
            float amp1  = r * (0.05f + 0.08f * ((i * 7 % 11) / 11f));
            float amp2  = r * (0.04f + 0.07f * ((i * 5 % 9)  / 9f));
            float phase = r * ((i * 3 % 13) / 13f) * 0.4f;

            Path wave = new Path();
            wave.moveTo(cx - r, baseY + phase);
            wave.cubicTo(
                    cx - r * 0.5f, baseY - amp1 + phase,
                    cx,            baseY + amp2 + phase,
                    cx + r * 0.5f, baseY - amp1 * 0.7f + phase
            );
            wave.cubicTo(
                    cx + r * 0.7f, baseY + amp2 * 0.5f + phase,
                    cx + r * 0.9f, baseY - amp1 * 0.3f + phase,
                    cx + r,        baseY + phase * 0.5f
            );

            // Alternance clair / sombre
            if (i % 2 == 0) {
                light.setColor(Color.parseColor("#909090"));
                light.setStrokeWidth(r * 0.022f);
                light.setAlpha(160);
                canvas.drawPath(wave, light);
            } else {
                dark.setColor(Color.parseColor("#181818"));
                dark.setStrokeWidth(r * 0.022f);
                dark.setAlpha(200);
                canvas.drawPath(wave, dark);
            }
        }

        // Reflet acier
        Paint shadowOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowOverlay.setStyle(Paint.Style.FILL);
        shadowOverlay.setShader(new RadialGradient(
                cx + r * 0.4f, cy + r * 0.4f, r * 0.9f,
                new int[]{ 0x00000000, 0x40000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadowOverlay);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawMeteorite(Canvas canvas, float cx, float cy, float r) {
        // Base gris-beige ferreuse
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new LinearGradient(
                cx - r * 0.7f, cy - r * 0.7f,
                cx + r * 0.7f, cy + r * 0.7f,
                new int[]{ 0xFFB0A890, 0xFF807868, 0xFF504840, 0xFF302820 },
                new float[]{ 0f, 0.38f, 0.65f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Bandes de Widmanstätten : 3 familles à 0°, 60°, 120°
        Paint bandLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        bandLight.setStyle(Paint.Style.STROKE);
        bandLight.setColor(Color.parseColor("#C8B898"));
        bandLight.setStrokeWidth(r * 0.030f);

        Paint bandDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        bandDark.setStyle(Paint.Style.STROKE);
        bandDark.setColor(Color.parseColor("#282018"));
        bandDark.setStrokeWidth(r * 0.018f);

        float spacing = r * 0.18f;
        int   count   = (int)(3f * r / spacing) + 2;

        // Les 3 angles caractéristiques du motif Widmanstätten
        double[] angles = { 0.0, Math.PI / 3.0, 2.0 * Math.PI / 3.0 };

        for (double angle : angles) {
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            // Vecteur perpendiculaire pour l'espacement
            float px = -sin;
            float py =  cos;

            for (int i = -count; i <= count; i++) {
                float ox = px * i * spacing;
                float oy = py * i * spacing;

                // Ligne étendue bien au-delà de r pour couvrir toute la balle
                float x1 = cx + ox + cos * r * 2f;
                float y1 = cy + oy + sin * r * 2f;
                float x2 = cx + ox - cos * r * 2f;
                float y2 = cy + oy - sin * r * 2f;

                bandLight.setAlpha(i % 2 == 0 ? 130 : 70);
                canvas.drawLine(x1, y1, x2, y2, bandLight);
                bandDark.setAlpha(i % 2 == 0 ? 80 : 140);
                canvas.drawLine(x1, y1, x2, y2, bandDark);
            }
        }

        // Quelques inclusions de troïlite (taches sombres arrondies)
        Paint inclusion = new Paint(Paint.ANTI_ALIAS_FLAG);
        inclusion.setStyle(Paint.Style.FILL);
        float[][] spots = {
                { -0.30f, -0.35f, 0.07f },
                {  0.40f,  0.20f, 0.05f },
                { -0.10f,  0.45f, 0.06f },
                {  0.25f, -0.50f, 0.04f },
                { -0.52f,  0.15f, 0.05f },
        };
        for (float[] s : spots) {
            inclusion.setColor(Color.parseColor("#1A1208"));
            inclusion.setAlpha(160);
            canvas.drawCircle(cx + s[0] * r, cy + s[1] * r, s[2] * r, inclusion);
        }

        // Ombre sphérique
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setStyle(Paint.Style.FILL);
        shadow.setShader(new RadialGradient(
                cx + r * 0.4f, cy + r * 0.4f, r * 0.9f,
                new int[]{ 0x00000000, 0x44000000 },
                new float[]{ 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, shadow);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawComet(Canvas canvas, float cx, float cy, float r) {
        // Halo dessiné EN PREMIER (derrière le corps)
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setStyle(Paint.Style.FILL);
        halo.setShader(new RadialGradient(
                cx, cy, r * 1.6f,
                new int[]{ 0xAAFFFFCC, 0x88FFAA00, 0x44FF4400, 0x00FF0000 },
                new float[]{ 0f, 0.35f, 0.65f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r * 1.6f, halo);

        // Corps rocheux brun-gris
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFCCAA80, 0xFFA0825A, 0xFF604030 },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Cratères : moins nombreux, plus grands
        float[][] craters = {
                { -0.38f, -0.42f, 0.20f, 185, 95 },
                {  0.40f,  0.20f, 0.22f, 190, 90 },
                { -0.15f,  0.45f, 0.18f, 175, 85 },
                {  0.15f, -0.55f, 0.14f, 165, 80 },
                { -0.55f,  0.25f, 0.15f, 170, 82 },
                {  0.50f, -0.40f, 0.12f, 160, 78 },
        };

        Paint craterFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint craterRim  = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterRim.setStyle(Paint.Style.STROKE);

        for (float[] c : craters) {
            float kx = cx + c[0] * r;
            float ky = cy + c[1] * r;
            float kr = c[2] * r;

            craterFill.setStyle(Paint.Style.FILL);
            craterFill.setColor(Color.parseColor("#503820"));
            craterFill.setAlpha((int)c[3]);
            canvas.drawCircle(kx, ky, kr, craterFill);

            craterFill.setColor(Color.parseColor("#302010"));
            craterFill.setAlpha((int)(c[3] * 0.5f));
            canvas.drawOval(new RectF(
                    kx - kr * 0.6f, ky,
                    kx + kr * 0.6f, ky + kr * 0.8f), craterFill);

            craterRim.setColor(Color.parseColor("#C8A878"));
            craterRim.setAlpha((int)c[4]);
            craterRim.setStrokeWidth(kr * 0.22f);
            canvas.drawCircle(kx, ky, kr, craterRim);
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawMercury(Canvas canvas, float cx, float cy, float r) {
        // Base gris-brun
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ballPaint);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Cratères : cercles clairs avec bord légèrement sombre
        Paint craterFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterFill.setStyle(Paint.Style.FILL);

        Paint craterRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterRim.setStyle(Paint.Style.STROKE);
        craterRim.setColor(Color.parseColor("#6B6055"));

        // {offset_x, offset_y, rayon, alpha_fill}
        float[][] craters = {
                { -0.38f,  -0.40f, 0.13f, 1.0f },
                {  0.30f,  -0.50f, 0.10f, 0.9f },
                { -0.55f,   0.15f, 0.16f, 1.0f },
                {  0.45f,   0.25f, 0.12f, 0.9f },
                {  0.10f,   0.55f, 0.14f, 1.0f },
                { -0.15f,   0.10f, 0.08f, 0.8f },
                {  0.55f,  -0.20f, 0.09f, 0.85f},
                { -0.20f,  -0.65f, 0.07f, 0.75f},
                {  0.20f,   0.10f, 0.11f, 0.9f },
        };

        for (float[] c : craters) {
            float ccx = cx + c[0] * r;
            float ccy = cy + c[1] * r;
            float cr  = c[2] * r;
            int alpha = (int)(c[3] * 255);

            craterFill.setColor(Color.parseColor("#C4BAB0"));
            craterFill.setAlpha(alpha);
            canvas.drawCircle(ccx, ccy, cr, craterFill);

            craterRim.setStrokeWidth(cr * 0.18f);
            craterRim.setAlpha((int)(alpha * 0.6f));
            canvas.drawCircle(ccx, ccy, cr * 0.88f, craterRim);
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawVenus(Canvas canvas, float cx, float cy, float r) {
        // Base jaune-crème avec gradient radial (atmosphère lumineuse)
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFFFF8DC, 0xFFE8D5A3, 0xFFD4B870 },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

// {offset_y, amplitude, ctrl_dx1, ctrl_dx2, épaisseur, alpha, couleur_hex}
        Object[][] bands = {
                { -0.55f,  0.08f,  0.30f, -0.10f, 0.07f, 100, "#FFFAEE" },
                { -0.32f,  0.18f, -0.25f,  0.40f, 0.13f, 120, "#FFF3CC" },
                { -0.10f,  0.11f,  0.45f,  0.10f, 0.09f,  90, "#F5E090" },
                {  0.14f,  0.15f, -0.35f, -0.05f, 0.14f, 130, "#FFF8E0" },
                {  0.38f,  0.09f,  0.20f,  0.50f, 0.08f,  80, "#EDD878" },
                {  0.60f,  0.13f, -0.15f,  0.25f, 0.11f, 110, "#FFF0C0" },
        };

        Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloud.setStyle(Paint.Style.STROKE);

        for (Object[] b : bands) {
            float bandY   = cy + (float)b[0] * r;
            float amp     = (float)b[1] * r;
            float cdx1    = (float)b[2] * r;
            float cdx2    = (float)b[3] * r;
            cloud.setStrokeWidth((float)b[4] * r);
            cloud.setAlpha((int)b[5]);
            cloud.setColor(Color.parseColor((String)b[6]));

            Path band = new Path();
            band.moveTo(cx - r, bandY);
            band.cubicTo(cx - r * 0.3f + cdx1, bandY - amp,
                    cx + r * 0.3f + cdx2, bandY + amp,
                    cx + r,               bandY);
            canvas.drawPath(band, cloud);
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawEarth(Canvas canvas, float cx, float cy, float r) {
        Paint ocean = new Paint(Paint.ANTI_ALIAS_FLAG);
        ocean.setStyle(Paint.Style.FILL);
        ocean.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.25f, r * 1.1f,
                new int[]{ 0xFF4FC3F7, 0xFF1A6FA8, 0xFF0D3F6B },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, ocean);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint land = new Paint(Paint.ANTI_ALIAS_FLAG);
        land.setStyle(Paint.Style.FILL);

        // ── Afrique ────────────────────────────────────────
        // Reconnaissable : large rectangle en haut, bombé Brasil-like sur l'ouest,
        // golfe de Guinée (rentrant) puis cap au sud
        land.setColor(Color.parseColor("#6AAA55"));
        Path africa = new Path();
        africa.moveTo(cx + r * 0.08f,  cy - r * 0.32f); // Maroc (haut-gauche)
        africa.lineTo(cx + r * 0.38f,  cy - r * 0.32f); // Libye (haut-droite)
        africa.cubicTo(cx + r * 0.52f, cy - r * 0.18f,
                cx + r * 0.54f, cy + r * 0.08f,
                cx + r * 0.48f, cy + r * 0.25f); // Corne de l'Afrique / côte est
        africa.cubicTo(cx + r * 0.42f, cy + r * 0.50f,
                cx + r * 0.25f, cy + r * 0.72f,
                cx + r * 0.18f, cy + r * 0.76f); // descend vers le Cap
        africa.cubicTo(cx + r * 0.08f, cy + r * 0.74f,
                cx + r * 0.00f, cy + r * 0.65f,
                cx - r * 0.02f, cy + r * 0.52f); // cap sud
        // Golfe de Guinée : la côte ouest remonte en rentrant vers l'est
        africa.cubicTo(cx + r * 0.10f, cy + r * 0.28f,
                cx + r * 0.12f, cy + r * 0.12f,
                cx - r * 0.02f, cy + r * 0.05f); // golfe de Guinée (bosse vers l'est)
        africa.cubicTo(cx - r * 0.08f, cy - r * 0.08f,
                cx - r * 0.02f, cy - r * 0.22f,
                cx + r * 0.08f, cy - r * 0.32f); // remonte côte ouest vers Maroc
        africa.close();
        canvas.drawPath(africa, land);

        // ── Amérique du Sud ────────────────────────────────
        // Reconnaissable : étroite en haut, grande bosse à droite (Brésil),
        // côte gauche (Chili) longue et droite, pointe effilée au sud
        land.setColor(Color.parseColor("#3D7A35"));
        Path samerica = new Path();
        samerica.moveTo(cx - r * 0.45f, cy + r * 0.08f); // Colombie ouest
        samerica.cubicTo(cx - r * 0.28f, cy + r * 0.02f,
                cx - r * 0.12f, cy + r * 0.08f,
                cx - r * 0.05f, cy + r * 0.22f); // Venezuela / Guyane (haut étroit)
        // Grande bosse brésilienne vers la droite
        samerica.cubicTo(cx + r * 0.02f,  cy + r * 0.35f,
                cx + r * 0.02f,  cy + r * 0.50f,
                cx - r * 0.12f,  cy + r * 0.62f);
        samerica.cubicTo(cx + r * 0.02f,  cy + r * 0.35f,
                cx + r * 0.02f,  cy + r * 0.50f,
                cx - r * 0.12f,  cy + r * 0.62f); // Brésil bombé (bord droit)
        samerica.cubicTo(cx - r * 0.20f, cy + r * 0.74f,
                cx - r * 0.30f, cy + r * 0.82f,
                cx - r * 0.35f, cy + r * 0.80f); // pointe sud (Patagonie)
        // Côte ouest longue et relativement droite (Chili/Pérou)
        samerica.cubicTo(cx - r * 0.52f, cy + r * 0.68f,
                cx - r * 0.58f, cy + r * 0.42f,
                cx - r * 0.55f, cy + r * 0.18f); // Chili remonte
        samerica.cubicTo(cx - r * 0.52f, cy + r * 0.10f,
                cx - r * 0.48f, cy + r * 0.08f,
                cx - r * 0.45f, cy + r * 0.08f);
        samerica.close();
        canvas.drawPath(samerica, land);

        // ── Amérique du Nord ───────────────────────────────
        // Reconnaissable : large en haut (Canada), côte est descend,
        // golfe du Mexique (grande échancrure en bas), côte ouest droite
        land.setColor(Color.parseColor("#4A8C3F"));
        Path namerica = new Path();
        namerica.moveTo(cx - r * 0.78f, cy - r * 0.55f); // Alaska
        namerica.cubicTo(cx - r * 0.55f, cy - r * 0.75f,
                cx - r * 0.22f, cy - r * 0.78f,
                cx - r * 0.05f, cy - r * 0.62f); // Canada haut large
        namerica.cubicTo(cx - r * 0.00f, cy - r * 0.52f,
                cx - r * 0.05f, cy - r * 0.35f,
                cx - r * 0.12f, cy - r * 0.18f); // côte est descend
        namerica.cubicTo(cx - r * 0.15f, cy - r * 0.05f,
                cx - r * 0.18f, cy + r * 0.05f,
                cx - r * 0.22f, cy + r * 0.08f); // Floride (pointe)
        // Golfe du Mexique : grande échancrure vers le haut
        namerica.cubicTo(cx - r * 0.35f, cy + r * 0.12f,
                cx - r * 0.48f, cy + r * 0.08f,
                cx - r * 0.52f, cy + r * 0.00f); // fond du golfe (Mexique)
        namerica.cubicTo(cx - r * 0.55f, cy - r * 0.08f,
                cx - r * 0.50f, cy - r * 0.18f,
                cx - r * 0.48f, cy - r * 0.25f); // Mexique remonte (Yucatan)
        // Côte ouest (Californie / Canada-ouest) : assez droite
        namerica.cubicTo(cx - r * 0.62f, cy - r * 0.25f,
                cx - r * 0.80f, cy - r * 0.35f,
                cx - r * 0.85f, cy - r * 0.42f); // côte pacifique
        namerica.cubicTo(cx - r * 0.85f, cy - r * 0.48f,
                cx - r * 0.82f, cy - r * 0.52f,
                cx - r * 0.78f, cy - r * 0.55f);
        namerica.close();
        canvas.drawPath(namerica, land);

        // ── Europe + Eurasie ouest ──────────────────────────
        // Compact, collé à l'Afrique par le haut, visible sur la gauche de l'Asie
        land.setColor(Color.parseColor("#5A9A50"));
        Path europe = new Path();
        europe.moveTo(cx + r * 0.08f,  cy - r * 0.32f); // jonction avec Afrique (Maroc/Espagne)
        europe.cubicTo(cx + r * 0.05f, cy - r * 0.45f,
                cx + r * 0.08f, cy - r * 0.62f,
                cx + r * 0.18f, cy - r * 0.68f); // péninsule ibérique / France
        europe.cubicTo(cx + r * 0.30f, cy - r * 0.75f,
                cx + r * 0.45f, cy - r * 0.70f,
                cx + r * 0.50f, cy - r * 0.58f); // Scandinavie
        europe.cubicTo(cx + r * 0.50f, cy - r * 0.48f,
                cx + r * 0.42f, cy - r * 0.38f,
                cx + r * 0.38f, cy - r * 0.32f); // bord est de l'Europe
        europe.cubicTo(cx + r * 0.28f, cy - r * 0.28f,
                cx + r * 0.18f, cy - r * 0.30f,
                cx + r * 0.08f, cy - r * 0.32f); // Méditerranée, rejoint l'Afrique
        europe.close();
        canvas.drawPath(europe, land);

        // ── Asie ───────────────────────────────────────────
        land.setColor(Color.parseColor("#4E8C45"));
        Path asia = new Path();
        asia.moveTo(cx + r * 0.38f,  cy - r * 0.32f); // bord ouest (rejoint Europe)
        asia.cubicTo(cx + r * 0.50f, cy - r * 0.58f,
                cx + r * 0.70f, cy - r * 0.70f,
                cx + r * 0.92f, cy - r * 0.35f); // Sibérie / bord nord-est
        asia.cubicTo(cx + r * 0.98f, cy - r * 0.10f,
                cx + r * 0.92f, cy + r * 0.15f,
                cx + r * 0.80f, cy + r * 0.22f); // côte est Asie
        asia.cubicTo(cx + r * 0.70f, cy + r * 0.28f,
                cx + r * 0.58f, cy + r * 0.22f,
                cx + r * 0.52f, cy + r * 0.10f); // Asie du Sud-Est
        // Péninsule indienne (pointe vers le bas)
        asia.cubicTo(cx + r * 0.48f, cy - r * 0.02f,
                cx + r * 0.50f, cy + r * 0.12f,
                cx + r * 0.44f, cy + r * 0.22f); // Inde (pointe)
        asia.cubicTo(cx + r * 0.42f, cy + r * 0.10f,
                cx + r * 0.40f, cy - r * 0.05f,
                cx + r * 0.38f, cy - r * 0.15f); // remonte côte ouest Inde
        asia.cubicTo(cx + r * 0.35f, cy - r * 0.22f,
                cx + r * 0.36f, cy - r * 0.28f,
                cx + r * 0.38f, cy - r * 0.32f);
        asia.close();
        canvas.drawPath(asia, land);

        // ── Antarctique : fine bande en bas ────────────────
        land.setColor(Color.parseColor("#DFF0F8"));
        land.setAlpha(210);
        Path antarctica = new Path();
        antarctica.moveTo(cx - r, cy + r * 0.85f);
        antarctica.cubicTo(cx - r * 0.4f, cy + r * 0.78f,
                cx + r * 0.4f, cy + r * 0.80f,
                cx + r,        cy + r * 0.85f);
        antarctica.lineTo(cx + r, cy + r);
        antarctica.lineTo(cx - r, cy + r);
        antarctica.close();
        canvas.drawPath(antarctica, land);
        land.setAlpha(255);

        // ── Nuages ─────────────────────────────────────────
        Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloud.setStyle(Paint.Style.FILL);
        cloud.setColor(Color.WHITE);

        cloud.setAlpha(170);
        canvas.drawOval(new RectF(cx - r * 0.38f, cy - r * 0.52f,
                cx + r * 0.05f,  cy - r * 0.40f), cloud);
        cloud.setAlpha(110);
        canvas.drawOval(new RectF(cx - r * 0.30f, cy - r * 0.50f,
                cx + r * 0.12f,  cy - r * 0.44f), cloud);

        cloud.setAlpha(150);
        canvas.drawOval(new RectF(cx - r * 0.90f, cy + r * 0.20f,
                cx - r * 0.50f,  cy + r * 0.32f), cloud);
        cloud.setAlpha(90);
        canvas.drawOval(new RectF(cx - r * 0.95f, cy + r * 0.24f,
                cx - r * 0.55f,  cy + r * 0.30f), cloud);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawMoon(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFD8D8D8, 0xFFB8B8B8, 0xFF787878 },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Cratères : {offset_x, offset_y, rayon, alpha_ombre, alpha_rebord}
        float[][] craters = {
                { -0.42f,  -0.50f,  0.13f,  180,  90 },  // grand, haut-gauche
                {  0.35f,  -0.38f,  0.10f,  160,  80 },  // moyen, haut-droite
                { -0.58f,   0.10f,  0.09f,  170,  85 },  // moyen, gauche
                {  0.50f,   0.25f,  0.14f,  190,  95 },  // grand, droite
                { -0.15f,   0.52f,  0.11f,  165,  80 },  // moyen, bas-centre
                {  0.18f,  -0.62f,  0.07f,  150,  70 },  // petit, haut
                { -0.30f,  -0.18f,  0.08f,  155,  75 },  // petit, centre-gauche
                {  0.55f,  -0.58f,  0.06f,  140,  65 },  // petit, haut-droite
                { -0.62f,  -0.42f,  0.07f,  145,  70 },  // petit, haut-gauche
                {  0.28f,   0.58f,  0.08f,  150,  72 },  // petit, bas-droite
                { -0.20f,   0.22f,  0.06f,  140,  65 },  // minuscule, centre
                {  0.08f,  -0.30f,  0.05f,  135,  60 },  // minuscule, centre-haut
                { -0.50f,   0.52f,  0.09f,  160,  78 },  // moyen, bas-gauche
        };

        Paint craterFill  = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint craterRim   = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterRim.setStyle(Paint.Style.STROKE);

        for (float[] c : craters) {
            float kx  = cx + c[0] * r;
            float ky  = cy + c[1] * r;
            float kr  = c[2] * r;

            // Intérieur sombre (creux)
            craterFill.setStyle(Paint.Style.FILL);
            craterFill.setColor(Color.parseColor("#606060"));
            craterFill.setAlpha((int)c[3]);
            canvas.drawCircle(kx, ky, kr, craterFill);

            // Ombre interne (gradient simulé : arc sombre en bas-droite)
            craterFill.setColor(Color.parseColor("#404040"));
            craterFill.setAlpha((int)(c[3] * 0.5f));
            canvas.drawOval(new RectF(
                    kx - kr * 0.6f, ky,
                    kx + kr * 0.6f, ky + kr * 0.8f), craterFill);

            // Rebord clair (relief autour du cratère)
            craterRim.setColor(Color.parseColor("#DDDDDD"));
            craterRim.setAlpha((int)c[4]);
            craterRim.setStrokeWidth(kr * 0.22f);
            canvas.drawCircle(kx, ky, kr, craterRim);
        }

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawMars(Canvas canvas, float cx, float cy, float r) {
        // Base rouge-orangé avec gradient radial (lumière haut-gauche)
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.25f, cy - r * 0.25f, r * 1.15f,
                new int[]{ 0xFFE8714A, 0xFFC1440E, 0xFF8B2500 },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // ── Taches sombres (régions volcaniques / Syrtis Major) ──
        Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
        dark.setStyle(Paint.Style.FILL);
        dark.setColor(Color.parseColor("#8B2500"));

        // Syrtis Major : grande tache triangulaire sombre, hémisphère nord-est
        dark.setAlpha(130);
        Path syrtis = new Path();
        syrtis.moveTo(cx + r * 0.28f, cy - r * 0.28f);
        syrtis.cubicTo(cx + r * 0.48f, cy - r * 0.42f,
                cx + r * 0.58f, cy - r * 0.22f,
                cx + r * 0.42f, cy - r * 0.08f);
        syrtis.cubicTo(cx + r * 0.32f, cy - r * 0.02f,
                cx + r * 0.20f, cy - r * 0.10f,
                cx + r * 0.28f, cy - r * 0.28f);
        syrtis.close();
        canvas.drawPath(syrtis, dark);

        // Tache secondaire (Acidalia Planitia) : ovale plus diffus, nord-ouest
        dark.setAlpha(75);
        canvas.drawOval(new RectF(
                cx - r * 0.55f, cy - r * 0.48f,
                cx - r * 0.15f, cy - r * 0.22f), dark);

        // Petite tache sud (Hellas Basin : légèrement plus claire, creux d'impact)
        Paint hellas = new Paint(Paint.ANTI_ALIAS_FLAG);
        hellas.setStyle(Paint.Style.FILL);
        hellas.setColor(Color.parseColor("#D4623A"));
        hellas.setAlpha(120);
        canvas.drawOval(new RectF(
                cx + r * 0.18f, cy + r * 0.35f,
                cx + r * 0.55f, cy + r * 0.56f), hellas);

        // ── Valles Marineris : grand canyon horizontal ──────
        // Long trait sombre légèrement courbé traversant le centre
        Paint canyon = new Paint(Paint.ANTI_ALIAS_FLAG);
        canyon.setStyle(Paint.Style.STROKE);
        canyon.setColor(Color.parseColor("#5A1200"));
        canyon.setStrokeWidth(r * 0.055f);
        canyon.setAlpha(210);
        canyon.setStrokeCap(Paint.Cap.ROUND);

        // Segment ouest : part à gauche, légèrement montant
        Path vallesW = new Path();
        vallesW.moveTo(cx - r * 0.58f, cy + r * 0.18f);
        vallesW.cubicTo(cx - r * 0.38f, cy + r * 0.12f,
                cx - r * 0.25f, cy + r * 0.20f,
                cx - r * 0.12f, cy + r * 0.15f);
        canvas.drawPath(vallesW, canyon);

        // Segment central : légère ondulation, c'est le plus large
        canyon.setStrokeWidth(r * 0.065f);
        Path vallesC = new Path();
        vallesC.moveTo(cx - r * 0.12f, cy + r * 0.15f);
        vallesC.cubicTo(cx + r * 0.02f, cy + r * 0.22f,
                cx + r * 0.15f, cy + r * 0.10f,
                cx + r * 0.28f, cy + r * 0.17f);
        canvas.drawPath(vallesC, canyon);

        // Segment est : se rétrécit et remonte légèrement
        canyon.setStrokeWidth(r * 0.040f);
        canyon.setAlpha(160);
        Path vallesE = new Path();
        vallesE.moveTo(cx + r * 0.28f, cy + r * 0.17f);
        vallesE.cubicTo(cx + r * 0.38f, cy + r * 0.12f,
                cx + r * 0.50f, cy + r * 0.18f,
                cx + r * 0.60f, cy + r * 0.13f);
        canvas.drawPath(vallesE, canyon);

        // Ramification sud, depuis le segment central
        canyon.setStrokeWidth(r * 0.028f);
        canyon.setAlpha(130);
        Path branch = new Path();
        branch.moveTo(cx + r * 0.05f, cy + r * 0.18f);
        branch.cubicTo(cx + r * 0.08f, cy + r * 0.26f,
                cx + r * 0.18f, cy + r * 0.30f,
                cx + r * 0.25f, cy + r * 0.27f);
        canvas.drawPath(branch, canyon);

        // ── Calotte polaire nord ────────────────────────────
        Paint polar = new Paint(Paint.ANTI_ALIAS_FLAG);
        polar.setStyle(Paint.Style.FILL);
        polar.setColor(Color.parseColor("#F5EEE8"));
        polar.setAlpha(220);
        canvas.drawOval(new RectF(
                cx - r * 0.28f, cy - r * 0.92f,
                cx + r * 0.28f, cy - r * 0.62f), polar);

        // Bord légèrement teinté (glace carbonique = légèrement rosé)
        polar.setColor(Color.parseColor("#E8D5CC"));
        polar.setAlpha(100);
        canvas.drawOval(new RectF(
                cx - r * 0.22f, cy - r * 0.88f,
                cx + r * 0.22f, cy - r * 0.68f), polar);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawJupiter(Canvas canvas, float cx, float cy, float r) {
        // Base beige chaud
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFE8D5A3, 0xFFC88B3A, 0xFF8B5A1A },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        band.setStyle(Paint.Style.STROKE);

        // Bandes de haut en bas — {offset_y, épaisseur, ondulation_amp, ctrl_dx1, ctrl_dx2, alpha, couleur}
        // Proportions réelles : NEB et SEB très larges, zones claires plus fines
        Object[][] bands = {
                // Zone polaire nord : brun foncé compact
                { -0.78f, 0.10f, 0.03f,  0.15f, -0.10f, 160, "#7A4010" },
                // North North Temperate Belt : brun moyen
                { -0.60f, 0.08f, 0.04f, -0.20f,  0.15f, 140, "#A05A20" },
                // North Temperate Zone : crème clair, fine
                { -0.48f, 0.06f, 0.02f,  0.10f,  0.05f, 100, "#E8CFA0" },
                // North Equatorial Belt : LARGE, brun-orangé foncé, le plus marqué
                { -0.30f, 0.18f, 0.05f, -0.25f,  0.30f, 180, "#8B3A08" },
                // Equatorial Zone : LARGE, crème très clair
                { -0.08f, 0.16f, 0.03f,  0.20f, -0.15f, 130, "#F0D8A8" },
                // South Equatorial Belt : LARGE, brun-orangé, c'est ici la GRS
                {  0.12f, 0.17f, 0.06f, -0.30f,  0.20f, 175, "#9A4010" },
                // South Temperate Zone : crème, fine
                {  0.32f, 0.07f, 0.03f,  0.15f, -0.10f, 110, "#DEC898" },
                // South South Temperate Belt : brun
                {  0.44f, 0.09f, 0.04f, -0.18f,  0.22f, 145, "#A06028" },
                // Zone polaire sud : brun foncé
                {  0.62f, 0.12f, 0.03f,  0.10f, -0.08f, 155, "#7A4010" },
        };

        for (Object[] b : bands) {
            float bandY   = cy + (float)b[0] * r;
            float thick   = (float)b[1] * r;
            float amp     = (float)b[2] * r;
            float cdx1    = (float)b[3] * r;
            float cdx2    = (float)b[4] * r;
            band.setAlpha((int)b[5]);
            band.setColor(Color.parseColor((String)b[6]));
            band.setStrokeWidth(thick);

            Path p = new Path();
            p.moveTo(cx - r, bandY);
            p.cubicTo(cx - r * 0.3f + cdx1, bandY - amp,
                    cx + r * 0.3f + cdx2, bandY + amp,
                    cx + r,               bandY);
            canvas.drawPath(p, band);
        }

        // ── Grande Tache Rouge ──────────────────────────────
        // Ovale dans le South Equatorial Belt, légèrement à droite du centre
        Paint grs = new Paint(Paint.ANTI_ALIAS_FLAG);
        grs.setStyle(Paint.Style.FILL);
        grs.setColor(Color.parseColor("#B83A18"));
        grs.setAlpha(200);
        canvas.drawOval(new RectF(
                cx + r * 0.10f, cy + r * 0.14f,
                cx + r * 0.48f, cy + r * 0.28f), grs);

        // Contour légèrement plus sombre de la GRS
        grs.setStyle(Paint.Style.STROKE);
        grs.setStrokeWidth(r * 0.025f);
        grs.setColor(Color.parseColor("#7A2008"));
        grs.setAlpha(160);
        canvas.drawOval(new RectF(
                cx + r * 0.10f, cy + r * 0.14f,
                cx + r * 0.48f, cy + r * 0.28f), grs);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawSaturn(Canvas canvas, float cx, float cy, float r) {
        // Dimensions de l'anneau : plus large que la balle, incliné (ellipse aplatie)
        float ringRx  = r * 1.75f; // demi-largeur totale de l'anneau
        float ringRy  = r * 0.30f; // aplatissement (perspective)
        float ringOffY = r * 0.18f; // décalage vertical vers le bas (inclinaison)

        // ── Passe 1 : demi-anneau ARRIÈRE (au-dessus du centre) ──
        Paint ringBack = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringBack.setStyle(Paint.Style.STROKE);
        ringBack.setAntiAlias(true);

        // 3 anneaux concentriques, du plus externe au plus interne, demi-ellipse du haut
        String[] ringColors = { "#D4B483", "#C8A055", "#B08840" };
        float[][] rings = {
                { 1.75f, 0.30f, 0.14f, 130 },
                { 1.45f, 0.25f, 0.18f, 160 },
                { 1.18f, 0.20f, 0.10f,  90 },
        };

        for (int i = 0; i < rings.length; i++) {
            float rx = r * rings[i][0];
            float ry = r * rings[i][1];
            ringBack.setStrokeWidth(r * rings[i][2]);
            ringBack.setAlpha((int)rings[i][3]);
            ringBack.setColor(Color.parseColor(ringColors[i]));

            // Clipper uniquement la moitié supérieure pour le passage arrière
            canvas.save();
            canvas.clipRect(cx - rx - 10f, cy - ry * 3f, cx + rx + 10f, cy + ringOffY);
            canvas.drawOval(new RectF(cx - rx, cy - ry + ringOffY,
                    cx + rx, cy + ry + ringOffY), ringBack);
            canvas.restore();
        }

        // ── Passe 2 : la planète ──────────────────────────────
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFEDD9A3, 0xFFC8A96E, 0xFF9A7840 },
                        new float[]{ 0f, 0.55f, 1f },
                        Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Bandes atmosphériques — plus douces que Jupiter, teintes beige/or
        Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        band.setStyle(Paint.Style.STROKE);

        Object[][] bands = {
                { -0.70f, 0.09f, 0.03f,  0.12f, -0.08f, 120, "#A07838" },
                { -0.52f, 0.07f, 0.02f, -0.15f,  0.10f,  90, "#DEC880" },
                { -0.36f, 0.13f, 0.04f,  0.22f, -0.18f, 140, "#B08840" },
                { -0.18f, 0.10f, 0.03f, -0.10f,  0.20f, 100, "#E8D090" },
                {  0.00f, 0.14f, 0.05f,  0.18f, -0.12f, 150, "#B89050" },
                {  0.18f, 0.09f, 0.03f, -0.22f,  0.15f,  95, "#D8B870" },
                {  0.34f, 0.11f, 0.04f,  0.15f, -0.20f, 130, "#A87830" },
                {  0.52f, 0.08f, 0.02f, -0.08f,  0.12f, 100, "#C8A060" },
                {  0.68f, 0.10f, 0.03f,  0.10f, -0.08f, 115, "#987030" },
        };

        for (Object[] b : bands) {
            float bandY = cy + (float)b[0] * r;
            float thick = (float)b[1] * r;
            float amp   = (float)b[2] * r;
            float cdx1  = (float)b[3] * r;
            float cdx2  = (float)b[4] * r;
            band.setAlpha((int)b[5]);
            band.setColor(Color.parseColor((String)b[6]));
            band.setStrokeWidth(thick);

            Path p = new Path();
            p.moveTo(cx - r, bandY);
            p.cubicTo(cx - r * 0.3f + cdx1, bandY - amp,
                    cx + r * 0.3f + cdx2, bandY + amp,
                    cx + r, bandY);
            canvas.drawPath(p, band);
        }

        canvas.restore();

        // Reflet
        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);

        // ── Passe 3 : demi-anneau AVANT (en-dessous du centre) ──
        for (int i = 0; i < rings.length; i++) {
            float rx = r * rings[i][0];
            float ry = r * rings[i][1];
            ringBack.setStrokeWidth(r * rings[i][2]);
            ringBack.setAlpha((int)rings[i][3]);
            ringBack.setColor(Color.parseColor(ringColors[i]));

            canvas.save();
            canvas.clipRect(cx - rx - 10f, cy + ringOffY, cx + rx + 10f, cy + ry * 3f + ringOffY);
            canvas.drawOval(new RectF(cx - rx, cy - ry + ringOffY,
                    cx + rx, cy + ry + ringOffY), ringBack);
            canvas.restore();
        }
}

    private void drawUranus(Canvas canvas, float cx, float cy, float r) {
        // Anneau arrière (fin, sombre, incliné) — dessiné AVANT la planète
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Color.parseColor("#607070"));
        float ringRx = r * 1.55f;
        float ringRy = r * 0.22f;

        ring.setStrokeWidth(r * 0.04f);
        ring.setAlpha(110);
        canvas.save();
        canvas.clipRect(cx - ringRx, cy - ringRy * 3f, cx + ringRx, cy);
        canvas.drawOval(new RectF(cx - ringRx, cy - ringRy, cx + ringRx, cy + ringRy), ring);
        canvas.restore();

        ring.setStrokeWidth(r * 0.025f);
        ring.setAlpha(80);
        float ringRx2 = r * 1.38f;
        float ringRy2 = r * 0.19f;
        canvas.save();
        canvas.clipRect(cx - ringRx2, cy - ringRy2 * 3f, cx + ringRx2, cy);
        canvas.drawOval(new RectF(cx - ringRx2, cy - ringRy2, cx + ringRx2, cy + ringRy2), ring);
        canvas.restore();

        // Base : bleu-cyan pâle et froid, très proche des vraies images Voyager 2
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.25f, cy - r * 0.25f, r * 1.1f,
                new int[]{ 0xFFCCF0F0, 0xFF88D8D8, 0xFF3AACAC, 0xFF1A6868 },
                new float[]{ 0f, 0.38f, 0.72f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

// 3 zones de couleur seulement, chacune rendue en 3 passes superposées
// (large+transparent → étroit+opaque) pour simuler un dégradé doux
        Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        band.setStyle(Paint.Style.STROKE);

// {offset_y, cdx1, cdx2, amp, couleur_hex}
        Object[][] zones = {
                { -0.40f,  0.10f, -0.08f, 0.025f, "#4AACAC" },
                {  0.05f, -0.12f,  0.16f, 0.020f, "#6ABEBE" },
                {  0.48f,  0.08f, -0.10f, 0.025f, "#3A9E9E" },
        };

// Passes : {épaisseur_r, alpha}
        float[][] passes = {
                { 0.28f,  8 },
                { 0.16f, 14 },
                { 0.07f, 20 },
        };

        for (Object[] z : zones) {
            float bandY = cy + (float)z[0] * r;
            float cdx1  = (float)z[1] * r;
            float cdx2  = (float)z[2] * r;
            float amp   = (float)z[3] * r;
            String col  = (String)z[4];

            for (float[] pass : passes) {
                band.setMaskFilter(new BlurMaskFilter(r * 0.10f, BlurMaskFilter.Blur.NORMAL));
                band.setStrokeWidth(r * 0.20f);
                band.setAlpha(22);
                band.setColor(Color.parseColor(col));

                Path p = new Path();
                p.moveTo(cx - r, bandY);
                p.cubicTo(cx - r * 0.3f + cdx1, bandY - amp,
                        cx + r * 0.3f + cdx2, bandY + amp,
                        cx + r, bandY);
                canvas.drawPath(p, band);
                band.setMaskFilter(null);
            }
        }

// Calotte polaire : très discrète
        Paint polar = new Paint(Paint.ANTI_ALIAS_FLAG);
        polar.setStyle(Paint.Style.FILL);
        polar.setColor(Color.parseColor("#DAFAFF"));
        polar.setAlpha(35);
        canvas.drawOval(new RectF(
                cx - r * 0.30f, cy - r * 0.88f,
                cx + r * 0.30f, cy - r * 0.58f), polar);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawNeptune(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.25f, cy - r * 0.25f, r * 1.1f,
                new int[]{ 0xFF4A80E8, 0xFF2A5FD4, 0xFF1238A0, 0xFF081A60 },
                new float[]{ 0f, 0.38f, 0.72f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Unique bande légèrement plus claire, légèrement sous l'équateur
        // 3 passes superposées pour bords fondus
        Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        band.setStyle(Paint.Style.STROKE);

        float bandY = cy + r * 0.18f;
        float cdx1  =  r * 0.14f;
        float cdx2  = -r * 0.10f;
        float amp   =  r * 0.022f;

        float[][] passes = {
                { 0.32f,  7 },
                { 0.18f, 13 },
                { 0.07f, 19 },
        };

        for (float[] pass : passes) {
            band.setMaskFilter(new BlurMaskFilter(r * 0.12f, BlurMaskFilter.Blur.NORMAL));
            band.setStrokeWidth(r * 0.22f);
            band.setAlpha(28);
            band.setColor(Color.parseColor("#5A8FEE"));

            Path p = new Path();
            p.moveTo(cx - r, bandY);
            p.cubicTo(cx - r * 0.3f + cdx1, bandY - amp,
                    cx + r * 0.3f + cdx2, bandY + amp,
                    cx + r, bandY);
            canvas.drawPath(p, band);
            band.setMaskFilter(null);
        }

        // 2 minuscules taches blanches (tempêtes)
        Paint spot = new Paint(Paint.ANTI_ALIAS_FLAG);
        spot.setStyle(Paint.Style.FILL);
        spot.setColor(Color.WHITE);

        // Tache 1 : légèrement plus visible, hémisphère sud (la Grande Tache Sombre est sombre,
        // mais les cirrus blancs autour sont blancs)
        spot.setAlpha(140);
        canvas.drawOval(new RectF(
                cx - r * 0.22f, cy + r * 0.28f,
                cx - r * 0.08f, cy + r * 0.36f), spot);

        // Tache 2 : plus petite, nord
        spot.setAlpha(100);
        canvas.drawOval(new RectF(
                cx + r * 0.28f, cy - r * 0.32f,
                cx + r * 0.38f, cy - r * 0.26f), spot);

        // Tache 3 : minuscule, très discrète
        spot.setAlpha(75);
        canvas.drawOval(new RectF(
                cx - r * 0.45f, cy - r * 0.12f,
                cx - r * 0.36f, cy - r * 0.07f), spot);

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawPluto(Canvas canvas, float cx, float cy, float r) {
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx - r * 0.2f, cy - r * 0.2f, r * 1.1f,
                new int[]{ 0xFFD8BCA0, 0xFFC4A882, 0xFF9A7858 },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // Arc du bas : brun foncé
        Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.FILL);
        arc.setColor(Color.parseColor("#6B3A20"));
        Path arcBottom = new Path();
        arcBottom.moveTo(cx - r, cy + r * 0.52f);
        arcBottom.cubicTo(cx - r * 0.5f, cy + r * 0.68f,
                cx + r * 0.5f, cy + r * 0.65f,
                cx + r,        cy + r * 0.52f);
        arcBottom.lineTo(cx + r, cy + r);
        arcBottom.lineTo(cx - r, cy + r);
        arcBottom.close();
        canvas.drawPath(arcBottom, arc);

        // Arc intermédiaire : brun moyen, au-dessus du foncé
        arc.setColor(Color.parseColor("#A87850"));
        Path arcMid = new Path();
        arcMid.moveTo(cx - r, cy + r * 0.28f);
        arcMid.cubicTo(cx - r * 0.4f, cy + r * 0.42f,
                cx + r * 0.5f, cy + r * 0.38f,
                cx + r,        cy + r * 0.28f);
        arcMid.lineTo(cx + r, cy + r * 0.58f);
        arcMid.cubicTo(cx + r * 0.5f, cy + r * 0.65f,
                cx - r * 0.5f, cy + r * 0.68f,
                cx - r,        cy + r * 0.52f);
        arcMid.close();
        canvas.drawPath(arcMid, arc);

        // Coeur de Tombaugh : légèrement incliné vers la droite
        Paint heart = new Paint(Paint.ANTI_ALIAS_FLAG);
        heart.setStyle(Paint.Style.FILL);
        heart.setColor(Color.parseColor("#EDD8C0"));

        canvas.save();
        canvas.rotate(12f, cx + r * 0.22f, cy + r * 0.26f);

        float hcx = cx + r * 0.35f;
        float hcy = cy + r * 0.57f;
        float hs  = r * 0.80f;

        Path heartPath = new Path();
        heartPath.moveTo(hcx, hcy + hs * 0.35f); // pointe bas
        // Côté gauche
        heartPath.cubicTo(hcx - hs * 0.05f, hcy + hs * 0.10f,
                hcx - hs * 0.60f, hcy - hs * 0.10f,
                hcx - hs * 0.50f, hcy - hs * 0.45f);
        heartPath.cubicTo(hcx - hs * 0.40f, hcy - hs * 0.80f,
                hcx,              hcy - hs * 0.65f,
                hcx,              hcy - hs * 0.30f);
        // Côté droit (miroir)
        heartPath.cubicTo(hcx,              hcy - hs * 0.65f,
                hcx + hs * 0.40f, hcy - hs * 0.80f,
                hcx + hs * 0.50f, hcy - hs * 0.45f);
        heartPath.cubicTo(hcx + hs * 0.60f, hcy - hs * 0.10f,
                hcx + hs * 0.05f, hcy + hs * 0.10f,
                hcx,              hcy + hs * 0.35f);
        heartPath.close();
        canvas.drawPath(heartPath, heart);
        canvas.restore();

        canvas.restore();

        //canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawStar(Canvas canvas, float cx, float cy, float r, int[] p) {
        // p[0] = cœur clair, p[1] = corps, p[2] = bord sombre, p[3] = couleur halo

        // ── Assombrissement au limbe (centre brillant → bords sombres) ──
        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setStyle(Paint.Style.FILL);
        base.setShader(new RadialGradient(
                cx, cy, r,
                new int[]{ p[0], p[1], p[2] },
                new float[]{ 0f, 0.55f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, base);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // ── Granulation : minuscules cellules de convection ──
        Paint grain = new Paint(Paint.ANTI_ALIAS_FLAG);
        grain.setStyle(Paint.Style.FILL);
        long seed = 0x5A3F2C1B;
        for (int i = 0; i < 38; i++) {
            seed = seed * 0x5DEECE66DL + 0xBL;
            float gx = cx + ((seed >> 16 & 0xFFFF) / 65535f * 2f - 1f) * r * 0.88f;
            seed = seed * 0x5DEECE66DL + 0xBL;
            float gy = cy + ((seed >> 16 & 0xFFFF) / 65535f * 2f - 1f) * r * 0.88f;
            if ((gx - cx) * (gx - cx) + (gy - cy) * (gy - cy) > r * r * 0.80f) continue;
            seed = seed * 0x5DEECE66DL + 0xBL;
            float gs = r * 0.04f + ((seed >> 16 & 0xFFFF) / 65535f) * r * 0.05f;
            seed = seed * 0x5DEECE66DL + 0xBL;
            boolean bright = (seed & 1) == 0;
            grain.setColor(bright ? p[0] : p[2]);
            grain.setAlpha(18 + (int)((seed >> 8 & 0xFF) % 18));
            canvas.drawCircle(gx, gy, gs, grain);
        }

        // ── Halo de couronne : transparent au centre, lumineux au bord ──
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setStyle(Paint.Style.FILL);
        int haloTransp = (p[3] & 0x00FFFFFF); // même couleur, alpha 0
        halo.setShader(new RadialGradient(
                cx, cy, r,
                new int[]{ 0xFFFFFFFF, p[0], p[1], p[1], p[2] },
                new float[]{ 0f, 0.45f, 0.72f, 0.86f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, halo);

        canvas.restore();

        // Pas de ballShinePaint sur les étoiles : elles sont auto-lumineuses
    }

    private void drawBlackHole(Canvas canvas, float cx, float cy, float r) {
        // ── Disque d'accrétion ARRIÈRE (dépasse la balle, moitié haute) ──
        Paint disk = new Paint(Paint.ANTI_ALIAS_FLAG);
        disk.setStyle(Paint.Style.STROKE);

        float[][] diskRings = {
                // {rx, ry, épaisseur, alpha, couleur}
                { 1.70f, 0.22f, 0.10f, 255, 0xFFFFFFEE },
                { 1.55f, 0.20f, 0.13f, 255, 0xFFFFCC44 },
                { 1.38f, 0.18f, 0.16f, 255, 0xFFFF8800 },
                { 1.20f, 0.15f, 0.11f, 220, 0xFFCC4400 },
        };

        for (float[] d : diskRings) {
            disk.setStrokeWidth(d[2] * r);
            disk.setAlpha((int)d[3]);
            disk.setColor((int)d[4]);
            float drx = d[0] * r;
            float dry = d[1] * r;
            canvas.save();
            canvas.clipRect(cx - drx - 10f, cy - dry * 4f, cx + drx + 10f, cy);
            canvas.drawOval(new RectF(cx - drx, cy - dry, cx + drx, cy + dry), disk);
            canvas.restore();
        }

        // ── Corps : fond spatial violet-noir ──
        Paint space = new Paint(Paint.ANTI_ALIAS_FLAG);
        space.setStyle(Paint.Style.FILL);
        space.setShader(new RadialGradient(
                cx, cy, r,
                new int[]{ 0xFF1A0030, 0xFF0A0018, 0xFF000008 },
                new float[]{ 0f, 0.6f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r, space);

        canvas.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(clip);

        // ── Anneau de photons : couronne lumineuse nette ──
        Paint photonRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        photonRing.setStyle(Paint.Style.FILL);
        photonRing.setShader(new RadialGradient(
                cx, cy, r * 0.74f,
                new int[]{ 0x00FFFFFF, 0x00FFFFFF, 0xFFFFEE88, 0x00FFFFFF },
                new float[]{ 0f, 0.60f, 0.72f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, r * 0.74f, photonRing);

        // ── Horizon des événements : noir absolu ──
        Paint horizon = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizon.setStyle(Paint.Style.FILL);
        horizon.setColor(Color.BLACK);
        canvas.drawCircle(cx, cy, r * 0.50f, horizon);

        canvas.restore();

        // ── Disque d'accrétion AVANT (moitié basse) ──
        for (float[] d : diskRings) {
            disk.setStrokeWidth(d[2] * r);
            disk.setAlpha((int)(d[3] * 0.9f));
            disk.setColor((int)d[4]);
            float drx = d[0] * r;
            float dry = d[1] * r;
            canvas.save();
            canvas.clipRect(cx - drx - 10f, cy, cx + drx + 10f, cy + dry * 4f);
            canvas.drawOval(new RectF(cx - drx, cy - dry, cx + drx, cy + dry), disk);
            canvas.restore();
        }
    }

    private void drawPulsar(Canvas canvas, float cx, float cy, float r) {
        float innerR = r * 0.18f;
        float gapY   = r * 0.06f;

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(Color.parseColor("#88DDFF"));

        // Courbe intérieure gauche
        Path innerLeft = new Path();
        innerLeft.moveTo(cx, cy - gapY);
        innerLeft.cubicTo(cx - innerR * 0.8f, cy - innerR * 1.2f,
                cx - innerR * 0.8f, cy + innerR * 1.2f,
                cx, cy + gapY);

        // Courbe intérieure droite
        Path innerRight = new Path();
        innerRight.moveTo(cx, cy - gapY);
        innerRight.cubicTo(cx + innerR * 0.8f, cy - innerR * 1.2f,
                cx + innerR * 0.8f, cy + innerR * 1.2f,
                cx, cy + gapY);

        // Demi-cercle gauche (bord extérieur)
        Path outerLeft = new Path();
        outerLeft.moveTo(cx, cy - gapY);
        outerLeft.cubicTo(cx - r * 0.05f, cy - r,
                cx - r,         cy - r * 0.55f,
                cx - r,         cy);
        outerLeft.cubicTo(cx - r,         cy + r * 0.55f,
                cx - r * 0.05f, cy + r,
                cx,             cy + gapY);

        // Demi-cercle droit (bord extérieur)
        Path outerRight = new Path();
        outerRight.moveTo(cx, cy - gapY);
        outerRight.cubicTo(cx + r * 0.05f, cy - r,
                cx + r,         cy - r * 0.55f,
                cx + r,         cy);
        outerRight.cubicTo(cx + r,         cy + r * 0.55f,
                cx + r * 0.05f, cy + r,
                cx,             cy + gapY);

        Path[] edges = { innerLeft, innerRight, outerLeft, outerRight };
        for (Path p : edges) {
            stroke.setMaskFilter(new BlurMaskFilter(r * 0.07f, BlurMaskFilter.Blur.NORMAL));
            stroke.setStrokeWidth(r * 0.07f);
            stroke.setAlpha(120);
            canvas.drawPath(p, stroke);

            stroke.setMaskFilter(null);
            stroke.setStrokeWidth(r * 0.022f);
            stroke.setAlpha(245);
            canvas.drawPath(p, stroke);
        }

        // Anneau central
        stroke.setMaskFilter(new BlurMaskFilter(r * 0.06f, BlurMaskFilter.Blur.NORMAL));
        stroke.setStrokeWidth(r * 0.06f);
        stroke.setAlpha(140);
        canvas.drawCircle(cx, cy, innerR, stroke);

        stroke.setMaskFilter(null);
        stroke.setStrokeWidth(r * 0.022f);
        stroke.setAlpha(255);
        canvas.drawCircle(cx, cy, innerR, stroke);

        // Étoile à neutrons centrale
        Paint neutron = new Paint(Paint.ANTI_ALIAS_FLAG);
        neutron.setStyle(Paint.Style.FILL);
        neutron.setShader(new RadialGradient(cx, cy, innerR,
                new int[]{ 0xFFFFFFFF, 0xFF99EEFF, 0xFF2255BB },
                new float[]{ 0f, 0.45f, 1f },
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, innerR, neutron);
    }

    private void drawWarpPortal(Canvas canvas, Paint wp, float cx, float cy, boolean flipped) {
        canvas.save();
        if (flipped) canvas.rotate(180f, cx, cy);
        warpPaint.setColor(Color.parseColor("#7B1FA2")); warpPaint.setAlpha(200);
        canvas.drawOval(new RectF(cx - WARP_WIDTH/2f, cy - WARP_HEIGHT/2f,
                cx + WARP_WIDTH/2f, cy + WARP_HEIGHT/2f), wp);
        warpPaint.setColor(Color.parseColor("#CE93D8")); warpPaint.setAlpha(100);
        canvas.drawOval(new RectF(cx - WARP_WIDTH/2f - 8f, cy - WARP_HEIGHT/2f - 8f,
                cx + WARP_WIDTH/2f + 8f, cy + WARP_HEIGHT/2f + 8f), wp);
        warpPaint.setColor(Color.WHITE); warpPaint.setAlpha(180);
        canvas.drawOval(new RectF(cx - WARP_WIDTH/4f, cy - WARP_HEIGHT/4f,
                cx + WARP_WIDTH/4f, cy + WARP_HEIGHT/4f), wp);
        warpPaint.setAlpha(255);
        canvas.restore();
    }

    private void sleep() {
        try { Thread.sleep(16); }
        catch (InterruptedException e) { Log.e("GameView", "sleep", e); }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (warpState != WARP_NONE) return true;
        float touchX = event.getX();
        float touchY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
                if (currentInk > 0 && !hasTrampoline) {
                    if (!isGameStarted) {
                        isGameStarted      = true;
                        hudShouldBeVisible = true;
                        if (gameStateListener != null) gameStateListener.onGameStarted();
                    }
                    isDrawingTrampoline = true;
                    hasTrampoline = false;
                    trampStartX = trampEndX = touchX;
                    trampStartY = trampEndY = touchY;
                    lastTouchX  = touchX;
                    lastTouchY  = touchY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDrawingTrampoline && currentInk > 0) {
                    boolean oob = false;
                    if (touchX < 0)                { touchX = 0;            oob = true; }
                    else if (touchX > screenWidth) { touchX = screenWidth;  oob = true; }
                    trampEndX = touchX; trampEndY = touchY;
                    float dist = (float) Math.hypot(touchX - lastTouchX, touchY - lastTouchY);
                    currentInk = Math.max(0, currentInk - dist * inkConsumptionRate);
                    lastTouchX = touchX; lastTouchY = touchY;
                    if (oob) { isDrawingTrampoline = false; hasTrampoline = true; }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDrawingTrampoline) { isDrawingTrampoline = false; hasTrampoline = true; }
                break;
        }
        return true;
    }

    private Path soccerPentagon(float cx, float cy, float r, float startAngle) {
        Path path = new Path();
        for (int i = 0; i < 5; i++) {
            double a = startAngle + Math.PI * 2 * i / 5;
            float x = cx + (float)Math.cos(a) * r;
            float y = cy + (float)Math.sin(a) * r;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    private Path soccerPentagonDistorted(float cx, float cy, float r, float startAngle,
                                         float radDx, float radDy, float depth) {
        // radDx, radDy : vecteur du centre du ballon vers le centre du pentagone
        // depth        : composante Z (0 = bord, 1 = pôle face à l'écran)
        // L'axe radial est compressé par `depth`, l'axe tangentiel reste intact.
        float radLen = (float)Math.hypot(radDx, radDy);
        float rnx = radLen > 0.001f ? radDx / radLen : 0f;
        float rny = radLen > 0.001f ? radDy / radLen : 1f;

        Path path = new Path();
        for (int i = 0; i < 5; i++) {
            double a = startAngle + Math.PI * 2 * i / 5;
            float vx = (float)Math.cos(a) * r;
            float vy = (float)Math.sin(a) * r;

            // Décomposition radiale / tangentielle
            float radComp = vx * rnx + vy * rny;
            float tanVx   = vx - radComp * rnx;
            float tanVy   = vy - radComp * rny;

            // Compression radiale simulant la courbure de la sphère
            float fx = cx + tanVx + radComp * depth;
            float fy = cy + tanVy + radComp * depth;

            if (i == 0) path.moveTo(fx, fy);
            else path.lineTo(fx, fy);
        }
        path.close();
        return path;
    }

    private Path regularPentagon(float cx, float cy, float r, float startAngle) {
        Path path = new Path();
        for (int i = 0; i < 5; i++) {
            double a = startAngle + Math.PI * 2 * i / 5;
            float x = cx + (float)Math.cos(a) * r;
            float y = cy + (float)Math.sin(a) * r;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}