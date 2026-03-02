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
        switch (skinId) {
            case "ball_emerald":  color = Color.parseColor("#43A047"); break;
            case "ball_sapphire": color = Color.parseColor("#1E88E5"); break;
            case "ball_gold":     color = Color.parseColor("#FFD700"); break;
            case "ball_void":     color = Color.parseColor("#212121"); break;
            case "ball_rose":     color = Color.parseColor("#E91E63"); break;
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
                ballVelocityX = nx * trampElasticity * mult;
                ballVelocityY = ny * trampElasticity * mult;
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
            canvas.drawCircle(ballX, ballY, ballRadius, ballPaint);
            canvas.drawCircle(ballX - ballRadius * 0.3f, ballY - ballRadius * 0.3f, ballRadius * 0.35f, ballShinePaint);
        } else if (warpState == WARP_ABSORB && warpBallScale > 0f) {
            float sr = ballRadius * warpBallScale;
            canvas.drawCircle(ballX, ballY, sr, ballPaint);
            canvas.drawCircle(ballX - sr * 0.3f, ballY - sr * 0.3f, sr * 0.35f, ballShinePaint);
        }

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
                if (currentInk > 0) {
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

    @Override public boolean performClick() { super.performClick(); return true; }
}