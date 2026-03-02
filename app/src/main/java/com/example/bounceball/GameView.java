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
import android.graphics.Shader;

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
    private final float ballRadius = 32f;

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
            case "ball_rose":     color = Color.parseColor("#E91E63"); break;
            case "ball_cream":    color = Color.parseColor("#FFF8E1"); break;
            case "ball_navy":     color = Color.parseColor("#1A237E"); break;
            // Metal
            case "ball_gold":     color = Color.parseColor("#FFD700"); break;
            case "ball_silver":   color = Color.parseColor("#B0BEC5"); break;
            case "ball_copper":   color = Color.parseColor("#BF6830"); break;
            case "ball_chrome":   color = Color.parseColor("#90CAF9"); break;
            // Space
            case "ball_void":     color = Color.parseColor("#212121"); break;
            case "ball_nebula":   color = Color.parseColor("#7B1FA2"); break;
            case "ball_comet":    color = Color.parseColor("#4FC3F7"); break;
            case "ball_moon":     color = Color.parseColor("#ECEFF1"); break;
            // Sport
            case "ball_soccer":   color = Color.parseColor("#F5F5F5"); break;
            case "ball_basket":   color = Color.parseColor("#E65100"); break;
            case "ball_tennis":   color = Color.parseColor("#CDDC39"); break;
            case "ball_bowling":  color = Color.parseColor("#311B92"); break;
            // Elemental
            case "ball_emerald":  color = Color.parseColor("#43A047"); break;
            case "ball_sapphire": color = Color.parseColor("#1E88E5"); break;
            case "ball_fire":     color = Color.parseColor("#FF5722"); break;
            case "ball_ice":      color = Color.parseColor("#80DEEA"); break;
            case "ball_thunder":  color = Color.parseColor("#FDD835"); break;
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

        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            ballVelocityX = -ballVelocityX * 0.8f;
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballVelocityX = -ballVelocityX * 0.8f;
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
        if (warpState == WARP_NONE || warpState == WARP_EJECT) {
            drawBall(canvas, ballX, ballY, ballRadius);
        } else if (warpState == WARP_ABSORB && warpBallScale > 0f) {
            drawBall(canvas, ballX, ballY, ballRadius * warpBallScale);
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
            case "ball_basket":  drawBasketball(canvas, cx, cy, r);  break;
            case "ball_tennis":  drawTennisBall(canvas, cx, cy, r);  break;
            case "ball_bowling": drawBowlingBall(canvas, cx, cy, r); break;
            case "ball_soccer":  drawSoccerBall(canvas, cx, cy, r);  break;
            default:
                canvas.drawCircle(cx, cy, r, ballPaint);
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
                break;
        }
    }

    private void drawBasketball(Canvas canvas, float cx, float cy, float r) {
        // Base orange
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

        // Ligne horizontale centrale
        canvas.drawLine(cx - r, cy, cx + r, cy, seam);

        // Ligne verticale centrale (légèrement courbée à gauche)
        Path left = new Path();
        left.moveTo(cx, cy - r);
        left.cubicTo(cx - r * 0.55f, cy - r * 0.4f,
                cx - r * 0.55f, cy + r * 0.4f,
                cx, cy + r);
        canvas.drawPath(left, seam);

        // Ligne courbée symétrique à droite
        Path right = new Path();
        right.moveTo(cx, cy - r);
        right.cubicTo(cx + r * 0.55f, cy - r * 0.4f,
                cx + r * 0.55f, cy + r * 0.4f,
                cx, cy + r);
        canvas.drawPath(right, seam);

        canvas.restore();

        // Reflet
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
    }

    private void drawTennisBall(Canvas canvas, float cx, float cy, float r) {
        // Base jaune-vert
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

        // Courbe haute : part du bas-gauche, monte au centre, redescend en haut-droite
        Path top = new Path();
        top.moveTo(cx - r * 0.85f, cy + r * 0.35f);
        top.cubicTo(cx - r * 0.2f,  cy - r * 0.75f,
                cx + r * 0.2f,  cy - r * 0.75f,
                cx + r * 0.85f, cy + r * 0.35f);
        canvas.drawPath(top, line);

        // Courbe basse : miroir vertical de la première
        Path bot = new Path();
        bot.moveTo(cx - r * 0.85f, cy - r * 0.35f);
        bot.cubicTo(cx - r * 0.2f,  cy + r * 0.75f,
                cx + r * 0.2f,  cy + r * 0.75f,
                cx + r * 0.85f, cy - r * 0.35f);
        canvas.drawPath(bot, line);

        canvas.restore();

        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
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
        hole.setColor(Color.parseColor("#1A0A60"));
        float hr = r * 0.095f;
        float hcx = cx + r * 0.18f;
        float hcy = cy - r * 0.22f;
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

        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
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
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, ballShinePaint);
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