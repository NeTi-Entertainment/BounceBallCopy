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
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.PixelFormat;

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

    private float gravityMultiplier = 1.0f;
    private float bounceMultiplier = 1.0f;
    private float inkConsumptionMultiplier = 1.0f;
    private float magnetMultiplier = 1.0f;

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
    private final RectF gaugeRect = new RectF();
    private static final int COLOR_GREY_HUD = 0xFF888888;
    private float gaugeOffsetX = -500f;
    private float statsOffsetY = -250f;
    private boolean hudShouldBeVisible = false;
    private static final float HUD_ANIM_SPEED = 1800f;

    private GamePreferences prefs;
    private UpgradeStats upgrades;

    private BackgroundRenderer bgRenderer;

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

    public void loadBgSkin() {
        if (bgRenderer == null) return;
        String bgId = prefs.getRaw().getString("equipped_bg", "bg_default");
        bgRenderer.loadBgSkin(bgId);
    }

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
        //surfaceHolder.setFormat(PixelFormat.RGBA_8888);
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
            case "ball_elem_fire":  color = Color.parseColor("#FF4400"); break;
            case "ball_elem_water": color = Color.parseColor("#0088CC"); break;
            case "ball_elem_earth": color = Color.parseColor("#6B4226"); break;
            case "ball_elem_ice":   color = Color.parseColor("#A8D8EA"); break;
            case "ball_elem_darkness": color = Color.parseColor("#050505"); break;
            case "ball_elem_light": color = Color.parseColor("#FFFFEE"); break;
            case "ball_elem_air":   color = Color.parseColor("#E0F7FA"); break;
            case "ball_elem_lightning": color = Color.parseColor("#FFF176"); break;
            case "ball_elem_plasma": color = Color.parseColor("#E040FB"); break;
            case "ball_elem_lava": color = Color.parseColor("#FF3300"); break;
            // Défaut
            default:              color = Color.parseColor("#E53935"); break;
        }
        ballPaint.setColor(color);
        BallRenderer.setColor(color);
        applySkinModifiers();
    }

    private void applySkinModifiers() {
        gravityMultiplier = 1.0f;
        bounceMultiplier = 1.0f;
        inkConsumptionMultiplier = 1.0f;
        magnetMultiplier = 1.0f;

        String category = getCategoryForSkin(currentBallSkin);

        switch (category) {
            case "metal":
                gravityMultiplier = 1.35f;
                bounceMultiplier = 0.8f;
                break;
            case "sport":
                gravityMultiplier = 0.95f;
                bounceMultiplier = 1.25f;
                break;
            case "space":
                gravityMultiplier = 0.65f;
                break;
            case "elemental":
                inkConsumptionMultiplier = 0.7f;
                magnetMultiplier = 1.5f;
                break;
            case "classic":
            default:
                break;
        }
    }

    private String getCategoryForSkin(String skinId) {
        if (skinId == null) return "classic";
        if (skinId.startsWith("ball_elem_")) return "elemental";

        if (java.util.Arrays.asList("ball_lead", "ball_nickel", "ball_copper", "ball_chrome",
                        "ball_bronze", "ball_steel", "ball_silver", "ball_gold", "ball_rosegold",
                        "ball_titanium", "ball_platinum", "ball_bismuth", "ball_damascus", "ball_meteorite")
                .contains(skinId)) return "metal";

        if (java.util.Arrays.asList("ball_void", "ball_nebula", "ball_comet", "ball_mercury",
                        "ball_venus", "ball_earth", "ball_moon", "ball_mars", "ball_jupiter", "ball_saturn",
                        "ball_uranus", "ball_neptune", "ball_pluto", "ball_red_dwarf", "ball_yellow_dwarf",
                        "ball_blue_giant", "ball_black_hole", "ball_pulsar")
                .contains(skinId)) return "space";

        if (java.util.Arrays.asList("ball_soccer", "ball_basket", "ball_tennis", "ball_bowling",
                        "ball_petanque", "ball_golf", "ball_cateye", "ball_beach", "ball_volleyball",
                        "ball_baseball", "ball_8ball")
                .contains(skinId)) return "sport";

        return "classic";
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
        bgRenderer = new BackgroundRenderer(getContext(), screenWidth, screenHeight);
        loadBgSkin();
        resetGame();
        isRunning  = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int f, int w, int h) {
        screenWidth  = w;
        screenHeight = h;
        bgRenderer.onScreenSizeChanged(w, h);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        while (true) {
            try { gameThread.join(); break; }
            catch (InterruptedException e) { Log.e("GameView", "thread join", e); }
        }
    }

    @Override public void run() {
        while (isRunning) {
            long frameStart = System.currentTimeMillis();
            update();
            draw();
            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepMs = 16 - elapsed;
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { Log.e("GameView", "sleep", e); }
            }
        }
    }

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
        ballVelocityY += (GRAVITY * gravityMultiplier);
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
            if (bgRenderer != null) bgRenderer.updateParallax(shift);
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
                float bounceForce = (trampElasticity * bounceMultiplier * mult) + (incomingSpeed * 0.5f);
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
            if (dist < (MAGNET_RADIUS * magnetMultiplier) && dist > 0.1f) {
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
        //Canvas canvas = surfaceHolder.lockCanvas();
        Canvas canvas;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            canvas = surfaceHolder.lockHardwareCanvas();
        } else {
            canvas = surfaceHolder.lockCanvas();
        }
        if (canvas == null) return;
        if (bgRenderer != null) bgRenderer.draw(canvas);
        else canvas.drawColor(Color.WHITE);

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
        gaugeRect.set(gaugeLeft, gaugeTop, gaugeRight, gaugeBottom);
        canvas.drawBitmap(gaugeSprite, null, gaugeRect, paint);
        canvas.restore();

        // HUD stats
        canvas.save();
        canvas.translate(0, statsOffsetY);
        paint.setColor(Color.BLACK);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format(Locale.getDefault(), "%.1fm", totalHeightMeters), screenWidth / 2f, 100, paint);
        paint.setTextSize(36);
        paint.setColor(COLOR_GREY_HUD);
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
        BallRenderer.setAnimState(currentBallSkin, ballRotation, ballVelocityX, ballVelocityY);
        BallRenderer.draw(canvas, cx, cy, r, currentBallSkin);
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
                    currentInk = Math.max(0, currentInk - dist * (inkConsumptionRate * inkConsumptionMultiplier));
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