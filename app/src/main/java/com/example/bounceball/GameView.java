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
import com.example.bounceball.colony.ColonyManager;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.BlurMaskFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import com.example.bounceball.utils.SoundManager;
import com.example.bounceball.utils.Strings;

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
    private static final float TRAMPOLINE_STROKE = 15f;
    private static final float TRAMPOLINE_OUTLINE_STROKE = 19f;

    private float gravityMultiplier = 1.0f;
    private float bounceMultiplier = 1.0f;
    private float inkConsumptionMultiplier = 1.0f;
    private float magnetMultiplier = 1.0f;

    private boolean isGameStarted;
    private boolean isGameOver;
    private float totalHeightMeters;
    private long runStartTimeMs = 0L;
    private long baseRunDurationMs = 0L;

    private float trampStartX, trampStartY, trampEndX, trampEndY;
    private boolean isDrawingTrampoline;
    private boolean hasTrampoline;
    private float lastTouchX, lastTouchY;

    private com.example.bounceball.utils.SoundManager soundManager;

    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trampolinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trampolineOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String currentBallSkin = "ball_basic";

    private static final int COLOR_GREY_HUD = 0xFF888888;
    private float gaugeOffsetY = 200f;
    private float statsOffsetY = -350f;
    private boolean hudShouldBeVisible = false;
    private static final float HUD_ANIM_SPEED = 1800f;

    private GamePreferences prefs;
    private UpgradeStats upgrades;

    private BackgroundRenderer bgRenderer;

    private final ArrayList<float[]> inkBlobs  = new ArrayList<>();
    private final ArrayList<float[]> warps     = new ArrayList<>();
    private final ArrayList<float[]> rareMetals = new ArrayList<>();
    private final ArrayList<float[]> alienDrops = new ArrayList<>();

    private static final float INK_BLOB_RADIUS  = 32f * 0.67f;
    private static final float WARP_WIDTH       = 32f * 4.45f;
    private static final float WARP_HEIGHT      = 34f;
    private static final float INK_RECHARGE     = 80f;
    private static final float INK_SPAWN_CHANCE  = 0.004f;
    private static final float WARP_SPAWN_CHANCE = 0.001f;

    private static final float METAL_RADIUS      = 22f;
    private static final float ALIEN_RADIUS      = 28f;
    private static final float METAL_SPAWN_CHANCE = 0.00035f;
    private static final float ALIEN_SPAWN_CHANCE = 0.00012f;

    private static final float MAGNET_RADIUS   = 220f;
    private static final float MAGNET_STRENGTH = 10f;

    private static final int WARP_NONE   = 0;
    private static final int WARP_ABSORB = 1;
    private static final int WARP_SCROLL = 2;
    private static final int WARP_EJECT  = 3;

    private int   warpState        = WARP_NONE;

    private int warpInStreamId = -1;
    private int warpOutStreamId = -1;
    private long warpStartTime = 0;
    private long warpOutStartTime = 0;
    private long warpDuration = 0;
    private int   warpAbsorbTimer  = 0;
    private float warpScrollLeft   = 0f;
    private int   warpEjectTimer   = 0;
    private int   warpEjectGrowTimer = 0;
    private float entryPortalX     = 0f;
    private float entryPortalY     = 0f;
    private float warpEntryStartX  = 0f;
    private float warpEntryStartY  = 0f;
    private boolean warpEntryFromAbove = true;
    private float exitPortalX      = 0f;
    private float exitPortalY      = 0f;
    private float warpBallScale    = 1f;

    private static final int   ABSORB_DUR  = 22;
    private static final int   EJECT_DUR   = 45;
    private static final float WARP_MIN_BALL_SCALE = 0.25f;
    private static final float SCROLL_EASE = 0.055f;
    private static final float SCROLL_MIN  = 4f;

    private final Paint blobPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warpPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint metalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alienPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float currentRunGold = 0f;

    private long colonyFullNotifTime = 0L;
    private static final long COLONY_NOTIF_DURATION_MS = 2500L;
    private final Paint notifPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void setHudVisible(boolean visible) { hudShouldBeVisible = visible; }

    public void loadBgSkin() {
        if (bgRenderer == null) return;
        String bgId = prefs.getRaw().getString("equipped_bg", "bg_default");
        bgRenderer.loadBgSkin(bgId);
    }

    public interface GameStateListener {
        void onGameStarted();
        void onGameOver(float heightReached, long durationMillis);
    }
    private GameStateListener gameStateListener;
    public void setGameStateListener(GameStateListener l) { this.gameStateListener = l; }

    public GameView(Context context, GamePreferences prefs, UpgradeStats upgrades) {
        super(context);
        this.prefs    = prefs;
        this.upgrades = upgrades;
        applyUpgrades();
        soundManager = new com.example.bounceball.utils.SoundManager(context, prefs);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(false);
        setupTrampolinePaints();
        loadBallSkin();
        ballShinePaint.setColor(Color.WHITE);
        ballShinePaint.setAlpha(120);
        currentInk        = maxInk;
        isGameStarted     = false;
        isGameOver        = false;
        totalHeightMeters = 0f;
    }

    private void setupTrampolinePaints() {
        trampolineOutlinePaint.setStyle(Paint.Style.STROKE);
        trampolineOutlinePaint.setStrokeWidth(TRAMPOLINE_OUTLINE_STROKE);
        trampolineOutlinePaint.setStrokeCap(Paint.Cap.ROUND);
        trampolineOutlinePaint.setStrokeJoin(Paint.Join.ROUND);
        trampolineOutlinePaint.setColor(Color.WHITE);

        trampolinePaint.setStyle(Paint.Style.STROKE);
        trampolinePaint.setStrokeWidth(TRAMPOLINE_STROKE);
        trampolinePaint.setStrokeCap(Paint.Cap.ROUND);
        trampolinePaint.setStrokeJoin(Paint.Join.ROUND);
        trampolinePaint.setColor(Color.BLUE);
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
            case "ball_basic": color = Color.parseColor("#F5EDD0"); break;
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
        runStartTimeMs = 0L;
        baseRunDurationMs = 0L;
        isGameStarted     = false;
        isGameOver        = false;
        isDrawingTrampoline = false;
        hasTrampoline     = false;
        warpState         = WARP_NONE;
        warpBallScale     = 1f;
        warpEjectGrowTimer = 0;
        entryPortalX     = 0f;
        entryPortalY     = 0f;
        warpEntryStartX  = 0f;
        warpEntryStartY  = 0f;
        warpEntryFromAbove = true;
        ballRotation     = 0f;
        ballAngularSpeed = 0f;
        currentRunGold = 0f;
        inkBlobs.clear();
        warps.clear();
        rareMetals.clear();
        alienDrops.clear();
    }

    public void prepareContinueFrom(float heightMeters, long accumulatedDurationMillis) {
        resetGame();
        totalHeightMeters = Math.max(0f, heightMeters);
        baseRunDurationMs = Math.max(0L, accumulatedDurationMillis);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        screenWidth  = getWidth();
        screenHeight = getHeight();
        bgRenderer = new BackgroundRenderer(getContext(), screenWidth, screenHeight);
        if (soundManager == null) soundManager = new SoundManager(getContext(), prefs);
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
        if (soundManager != null) {
            soundManager.release();
            soundManager = null;
        }
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

    private void update() {

        float hudDt    = 0.016f;
        float targetGY = hudShouldBeVisible ? 0f : 200f;
        float targetSY = hudShouldBeVisible ? 0f : -350f;

        if (gaugeOffsetY < targetGY)      gaugeOffsetY = Math.min(gaugeOffsetY + HUD_ANIM_SPEED * hudDt, targetGY);
        else if (gaugeOffsetY > targetGY) gaugeOffsetY = Math.max(gaugeOffsetY - HUD_ANIM_SPEED * hudDt, targetGY);

        if (statsOffsetY < targetSY)      statsOffsetY = Math.min(statsOffsetY + HUD_ANIM_SPEED * hudDt, targetSY);
        else if (statsOffsetY > targetSY) statsOffsetY = Math.max(statsOffsetY - HUD_ANIM_SPEED * hudDt, targetSY);

        if (!isGameStarted || isGameOver) return;

        if (warpState != WARP_NONE) {
            updateWarpAnimation();
            return;
        }

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
            ballAngularSpeed = -ballAngularSpeed * 0.6f;
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballVelocityX    = -ballVelocityX * 0.8f;
            ballAngularSpeed = -ballAngularSpeed * 0.6f;
        }

        float idealCameraY = screenHeight * 0.4f;
        if (ballY < idealCameraY) {
            float shift = (idealCameraY - ballY) * 0.1f;
            ballY += shift;
            if (hasTrampoline) { trampStartY += shift; trampEndY += shift; }
            for (float[] b : inkBlobs)    b[1] += shift;
            for (float[] w : warps)       w[1] += shift;
            for (float[] m : rareMetals)  m[1] += shift;
            for (float[] a : alienDrops)  a[1] += shift;
            totalHeightMeters += shift / 100f;
            currentRunGold += shift / 100f * upgrades.goldMultiplier;
            if (bgRenderer != null) bgRenderer.updateParallax(shift);
        }

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
                ballAngularSpeed = (tangentialSpeed / ballRadius) * (180f / (float) Math.PI) * 0.8f;

                if (currentBallSkin.startsWith("ball_elem_")) {
                    soundManager.playElementalSound(currentBallSkin);
                } else {
                    soundManager.playSound(com.example.bounceball.utils.SoundManager.SOUND_BOUNCE);
                }

                hasTrampoline = false;
            }
        }

        spawnPowerUps();

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
                soundManager.playSound(com.example.bounceball.utils.SoundManager.SOUND_INK);
                blobIt.remove();
            }
        }

        Iterator<float[]> warpIt = warps.iterator();
        while (warpIt.hasNext()) {
            float[] w = warpIt.next();
            float wCx = w[0];
            if (ballX + ballRadius > wCx - WARP_WIDTH / 2f
                    && ballX - ballRadius < wCx + WARP_WIDTH / 2f
                    && ballY + ballRadius > w[1] - WARP_HEIGHT / 2f
                    && ballY - ballRadius < w[1] + WARP_HEIGHT / 2f) {
                startWarp(wCx, w[1]);
                warpIt.remove();
                break;
            }
        }

        inkBlobs.removeIf(b -> b[1] > screenHeight + 100f);
        warps.removeIf(w -> w[1] > screenHeight + 100f);

        if (prefs.hasHatched()) {
            Iterator<float[]> metalIt = rareMetals.iterator();
            while (metalIt.hasNext()) {
                float[] m  = metalIt.next();
                float dx   = ballX - m[0];
                float dy   = ballY - m[1];
                float dist = (float) Math.hypot(dx, dy);
                if (dist < (MAGNET_RADIUS * magnetMultiplier) && dist > 0.1f) {
                    m[0] += (dx / dist) * MAGNET_STRENGTH;
                    m[1] += (dy / dist) * MAGNET_STRENGTH;
                    dist = (float) Math.hypot(ballX - m[0], ballY - m[1]);
                }
                if (dist < ballRadius + METAL_RADIUS) {
                    prefs.addRareMetal(1);
                    metalIt.remove();
                }
            }

            Iterator<float[]> alienIt = alienDrops.iterator();
            while (alienIt.hasNext()) {
                float[] a  = alienIt.next();
                float dx   = ballX - a[0];
                float dy   = ballY - a[1];
                float dist = (float) Math.hypot(dx, dy);
                boolean canCollect = ColonyManager.canCollectAlien(
                        ColonyManager.loadSlots(prefs), prefs.getAlienCount());
                if (canCollect && dist < (MAGNET_RADIUS * magnetMultiplier) && dist > 0.1f) {
                    a[0] += (dx / dist) * MAGNET_STRENGTH;
                    a[1] += (dy / dist) * MAGNET_STRENGTH;
                    dist = (float) Math.hypot(ballX - a[0], ballY - a[1]);
                }
                if (canCollect && dist < ballRadius + ALIEN_RADIUS) {
                    prefs.addAlien(1);
                    alienIt.remove();
                } else if (!canCollect && dist < MAGNET_RADIUS * 1.5f) {
                    colonyFullNotifTime = System.currentTimeMillis();
                }
            }

            rareMetals.removeIf(m -> m[1] > screenHeight + 100f);
            alienDrops.removeIf(a -> a[1] > screenHeight + 100f);
        }

        if (ballY > screenHeight + ballRadius && !isGameOver) {
            soundManager.stopElementalSound();

            if (warpInStreamId != -1) {
                soundManager.stopSound(warpInStreamId);
                warpInStreamId = -1;
            }
            if (warpOutStreamId != -1) {
                soundManager.stopSound(warpOutStreamId);
                warpOutStreamId = -1;
            }

            soundManager.playSound(com.example.bounceball.utils.SoundManager.SOUND_FALL);

            isGameOver = true;
            float h = totalHeightMeters;
            long duration = baseRunDurationMs;
            if (runStartTimeMs > 0L) duration += System.currentTimeMillis() - runStartTimeMs;
            if (gameStateListener != null) gameStateListener.onGameOver(h, duration);
            resetGame();
        }

        if (warpOutStreamId != -1) {
            long elapsed = System.currentTimeMillis() - warpOutStartTime;
            if (elapsed < warpDuration) {
                float volume = 1.0f - ((float) elapsed / warpDuration);
                soundManager.setVolume(warpOutStreamId, Math.max(0f, volume));
            } else {
                soundManager.stopSound(warpOutStreamId);
                warpOutStreamId = -1;
            }
        }
    }

    private void startWarp(float entryX, float entryY) {
        warpStartTime = System.currentTimeMillis();
        warpInStreamId = soundManager.playSound(com.example.bounceball.utils.SoundManager.SOUND_WARP_IN);
        entryPortalX    = entryX;
        entryPortalY    = entryY;
        warpEntryStartX = ballX;
        warpEntryStartY = ballY;
        warpEntryFromAbove = ballY <= entryY;
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
                float absorbProgress = 1f - ((float) warpAbsorbTimer / ABSORB_DUR);
                warpBallScale = 1f - absorbProgress * (1f - WARP_MIN_BALL_SCALE);
                ballX = warpEntryStartX + (entryPortalX - warpEntryStartX) * absorbProgress;
                float entryTargetY = entryPortalY + (warpEntryFromAbove ? ballRadius * 0.42f : -ballRadius * 0.42f);
                ballY = warpEntryStartY + (entryTargetY - warpEntryStartY) * absorbProgress;
                warpAbsorbTimer--;
                if (warpAbsorbTimer <= 0) {
                    warpBallScale = WARP_MIN_BALL_SCALE;
                    warpState = WARP_SCROLL;
                }
                break;

            case WARP_SCROLL:
                float step = Math.min(warpScrollLeft, Math.max(SCROLL_MIN, warpScrollLeft * SCROLL_EASE));
                if (hasTrampoline) { trampStartY += step; trampEndY += step; }
                for (float[] b : inkBlobs)   b[1] += step;
                for (float[] w : warps)      w[1] += step;
                for (float[] m : rareMetals) m[1] += step;
                for (float[] a : alienDrops) a[1] += step;
                if (bgRenderer != null) bgRenderer.updateParallax(step);
                warpScrollLeft -= step;
                if (warpScrollLeft <= 0f) {
                    warpDuration = System.currentTimeMillis() - warpStartTime;
                    soundManager.stopSound(warpInStreamId);
                    warpOutStreamId = soundManager.playSound(com.example.bounceball.utils.SoundManager.SOUND_WARP_OUT);
                    warpOutStartTime = System.currentTimeMillis();

                    ballX = exitPortalX;
                    ballY = exitPortalY + ballRadius * 0.42f;
                    ballVelocityY  = -22f;
                    ballVelocityX  = 0f;
                    warpBallScale  = WARP_MIN_BALL_SCALE;
                    warpEjectGrowTimer = ABSORB_DUR;
                    warpEjectTimer = EJECT_DUR;
                    warpState      = WARP_EJECT;
                }
                break;

            case WARP_EJECT:
                if (warpEjectGrowTimer > 0) {
                    float growProgress = 1f - ((float) warpEjectGrowTimer / ABSORB_DUR);
                    warpBallScale = WARP_MIN_BALL_SCALE + growProgress * (1f - WARP_MIN_BALL_SCALE);
                    warpEjectGrowTimer--;
                } else {
                    warpBallScale = 1f;
                }
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
                if (warpEjectTimer <= 0) {
                    warpBallScale = 1f;
                    warpState = WARP_NONE;
                }
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
        if (prefs.hasHatched()) {
            if (Math.random() < METAL_SPAWN_CHANCE) {
                float x = margin + (float)(Math.random() * (screenWidth - 2 * margin));
                rareMetals.add(new float[]{ x, -METAL_RADIUS });
            }
            if (Math.random() < ALIEN_SPAWN_CHANCE) {
                float x = margin + (float)(Math.random() * (screenWidth - 2 * margin));
                alienDrops.add(new float[]{ x, -ALIEN_RADIUS });
            }
        }
    }

    private void draw() {
        if (!surfaceHolder.getSurface().isValid()) return;
        Canvas canvas;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            canvas = surfaceHolder.lockHardwareCanvas();
        } else {
            canvas = surfaceHolder.lockCanvas();
        }
        if (canvas == null) return;
        if (bgRenderer != null) bgRenderer.draw(canvas, totalHeightMeters);
        else canvas.drawColor(Color.WHITE);

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

        if (prefs.hasHatched()) {
            metalPaint.setStyle(Paint.Style.FILL);
            for (float[] m : rareMetals) {
                metalPaint.setColor(Color.parseColor("#80DEEA"));
                metalPaint.setAlpha(255);
                canvas.drawCircle(m[0], m[1], METAL_RADIUS, metalPaint);
                metalPaint.setColor(Color.parseColor("#26C6DA"));
                canvas.drawCircle(m[0], m[1], METAL_RADIUS, metalPaint);
                metalPaint.setColor(Color.WHITE);
                metalPaint.setAlpha(180);
                canvas.drawCircle(m[0] - METAL_RADIUS * 0.28f,
                        m[1] - METAL_RADIUS * 0.28f,
                        METAL_RADIUS * 0.32f, metalPaint);
                metalPaint.setColor(Color.parseColor("#B2EBF2"));
                metalPaint.setAlpha(200);
                metalPaint.setStyle(Paint.Style.STROKE);
                metalPaint.setStrokeWidth(3f);
                canvas.drawCircle(m[0], m[1], METAL_RADIUS + 4f, metalPaint);
                metalPaint.setStyle(Paint.Style.FILL);
            }

            alienPaint.setStyle(Paint.Style.FILL);
            for (float[] a : alienDrops) {
                alienPaint.setColor(Color.parseColor("#1B5E20"));
                alienPaint.setAlpha(255);
                canvas.drawCircle(a[0], a[1], ALIEN_RADIUS, alienPaint);
                alienPaint.setColor(Color.parseColor("#4CAF50"));
                canvas.drawCircle(a[0], a[1], ALIEN_RADIUS * 0.78f, alienPaint);
                alienPaint.setColor(Color.WHITE);
                alienPaint.setAlpha(200);
                canvas.drawCircle(a[0] - ALIEN_RADIUS * 0.25f,
                        a[1] - ALIEN_RADIUS * 0.25f,
                        ALIEN_RADIUS * 0.28f, alienPaint);
                alienPaint.setColor(Color.parseColor("#A5D6A7"));
                alienPaint.setAlpha(220);
                alienPaint.setStyle(Paint.Style.STROKE);
                alienPaint.setStrokeWidth(3.5f);
                canvas.drawCircle(a[0], a[1], ALIEN_RADIUS + 5f, alienPaint);
                alienPaint.setStyle(Paint.Style.FILL);
                alienPaint.setAlpha(255);
            }
        }

        warpPaint.setStyle(Paint.Style.FILL);
        for (float[] w : warps) drawWarpPortal(canvas, warpPaint, w[0], w[1], false);

        if (warpState == WARP_ABSORB) {
            drawWarpPortal(canvas, warpPaint, entryPortalX, entryPortalY, false);
        }
        if (warpState == WARP_EJECT || (warpState == WARP_SCROLL && warpScrollLeft < screenHeight * 1.15f)) {
            drawWarpPortal(canvas, warpPaint, exitPortalX, exitPortalY, true);
        }

        if (warpState == WARP_NONE || warpState == WARP_EJECT) {
            if (warpState == WARP_EJECT && warpBallScale < 0.999f) {
                drawClippedBall(canvas, exitPortalY, true, ballRadius * warpBallScale);
            } else {
                drawBallWithShine(canvas, ballRadius);
            }
        } else if (warpState == WARP_ABSORB && warpBallScale > 0f) {
            drawClippedBall(canvas, entryPortalY, warpEntryFromAbove, ballRadius * warpBallScale);
        }

        canvas.save();
        canvas.translate(0, gaugeOffsetY);
        float barHeight = 30f;
        float barWidth = screenWidth * 0.75f;
        float barLeft = (screenWidth - barWidth) / 2f;
        float barBottom = screenHeight - 40f;
        float barTop = barBottom - barHeight;

        paint.setColor(Color.parseColor("#44000000"));
        canvas.drawRoundRect(barLeft, barTop, barLeft + barWidth, barBottom, barHeight/2f, barHeight/2f, paint);

        float inkRatio = currentInk / maxInk;
        if (inkRatio > 0) {
            float inkWidth = barWidth * inkRatio;
            paint.setColor(Color.parseColor("#1DE9B6"));
            canvas.drawRoundRect(barLeft, barTop, barLeft + inkWidth, barBottom, barHeight/2f, barHeight/2f, paint);
        }
        canvas.restore();

        canvas.save();
        canvas.translate(0, statsOffsetY);
        paint.setColor(Color.BLACK);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(Strings.fmt("hud.height_fmt", totalHeightMeters), screenWidth / 2f, 180, paint);
        paint.setTextSize(36);
        paint.setColor(COLOR_GREY_HUD);
        canvas.drawText(Strings.fmt("hud.record_fmt", prefs.getMaxHeight()), screenWidth / 2f, 230, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(40);
        int displayedGold = prefs.getGold() + (int) currentRunGold;
        canvas.drawText(Strings.fmt("common.currency_gold_fmt", displayedGold), screenWidth / 2f, 280, paint);
        canvas.restore();

        if (prefs.hasHatched() && colonyFullNotifTime > 0) {
            long elapsed = System.currentTimeMillis() - colonyFullNotifTime;
            if (elapsed < COLONY_NOTIF_DURATION_MS) {
                float progress = (float) elapsed / COLONY_NOTIF_DURATION_MS;
                int alpha = (int)(255 * (1f - Math.max(0f, (progress - 0.7f) / 0.3f)));
                String msg = Strings.get("hud.colony_full");
                notifPaint.setTextSize(32f);
                notifPaint.setTextAlign(Paint.Align.CENTER);
                float textW = notifPaint.measureText(msg);
                float boxW  = textW + 40f;
                float boxH  = 56f;
                float boxL  = (screenWidth - boxW) / 2f;
                float boxT  = screenHeight * 0.72f;
                notifPaint.setStyle(Paint.Style.FILL);
                notifPaint.setColor(Color.argb(alpha * 180 / 255, 10, 30, 10));
                canvas.drawRoundRect(boxL, boxT, boxL + boxW, boxT + boxH, 18f, 18f, notifPaint);
                notifPaint.setColor(Color.argb(alpha * 80 / 255, 76, 175, 80));
                notifPaint.setStyle(Paint.Style.STROKE);
                notifPaint.setStrokeWidth(2f);
                canvas.drawRoundRect(boxL, boxT, boxL + boxW, boxT + boxH, 18f, 18f, notifPaint);
                notifPaint.setStyle(Paint.Style.FILL);
                notifPaint.setColor(Color.argb(alpha, 165, 214, 167));
                canvas.drawText(msg, screenWidth / 2f, boxT + boxH * 0.65f, notifPaint);
            } else {
                colonyFullNotifTime = 0L;
            }
        }

        if (isDrawingTrampoline || hasTrampoline) {
            drawTrampoline(canvas);
        }

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    private void drawTrampoline(Canvas canvas) {
        canvas.drawLine(trampStartX, trampStartY, trampEndX, trampEndY, trampolineOutlinePaint);
        canvas.drawLine(trampStartX, trampStartY, trampEndX, trampEndY, trampolinePaint);
    }

    private void drawBallWithShine(Canvas canvas, float r) {
        canvas.save();
        canvas.rotate(ballRotation, ballX, ballY);
        drawBall(canvas, ballX, ballY, r);
        canvas.restore();
        drawBallShine(canvas, r);
    }

    private void drawClippedBall(Canvas canvas, float clipY, boolean showAboveLine, float r) {
        canvas.save();
        if (showAboveLine) {
            canvas.clipRect(0f, 0f, (float) screenWidth, clipY);
        } else {
            canvas.clipRect(0f, clipY, (float) screenWidth, (float) screenHeight);
        }
        drawBallWithShine(canvas, r);
        canvas.restore();
    }

    private void drawBallShine(Canvas canvas, float r) {
        if (!currentBallSkin.equals("ball_pulsar") && !currentBallSkin.equals("ball_blackhole")
                && !currentBallSkin.equals("ball_red_dwarf") && !currentBallSkin.equals("ball_yellow_dwarf")
                && !currentBallSkin.equals("ball_blue_giant")) {
            canvas.drawCircle(ballX - r * 0.3f, ballY - r * 0.3f, r * 0.35f, ballShinePaint);
        }
    }

    private void drawBall(Canvas canvas, float cx, float cy, float r) {
        BallRenderer.setAnimState(currentBallSkin, ballRotation, ballVelocityX, ballVelocityY);
        BallRenderer.draw(canvas, cx, cy, r, currentBallSkin);
    }

    private void drawWarpPortal(Canvas canvas, Paint wp, float cx, float cy, boolean flipped) {
        float t = (android.os.SystemClock.uptimeMillis() % 60000L) / 1000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 6.0f + cx * 0.02f);
        float inhale = 0.5f + 0.5f * (float) Math.sin(t * 8.4f + cy * 0.015f);
        float w = WARP_WIDTH * (1f + pulse * 0.045f);
        float h = WARP_HEIGHT * (1f - pulse * 0.065f);
        float outerW = w + 16f + inhale * 10f;
        float outerH = h + 12f + inhale * 6f;

        canvas.save();
        if (flipped) canvas.rotate(180f, cx, cy);

        wp.setStyle(Paint.Style.FILL);
        wp.setColor(Color.parseColor("#CE93D8"));
        wp.setAlpha(55 + (int)(pulse * 45f));
        canvas.drawOval(new RectF(cx - outerW/2f, cy - outerH/2f,
                cx + outerW/2f, cy + outerH/2f), wp);

        wp.setColor(Color.parseColor("#7B1FA2"));
        wp.setAlpha(225);
        canvas.drawOval(new RectF(cx - w/2f, cy - h/2f,
                cx + w/2f, cy + h/2f), wp);

        wp.setColor(Color.parseColor("#24002E"));
        wp.setAlpha(190);
        canvas.drawOval(new RectF(cx - w * 0.34f, cy - h * 0.34f,
                cx + w * 0.34f, cy + h * 0.34f), wp);

        wp.setStyle(Paint.Style.STROKE);
        wp.setStrokeCap(Paint.Cap.ROUND);
        wp.setStrokeWidth(3.2f);
        wp.setColor(Color.WHITE);
        wp.setAlpha(115 + (int)(pulse * 70f));
        RectF ring = new RectF(cx - w * 0.42f, cy - h * 0.42f,
                cx + w * 0.42f, cy + h * 0.42f);
        canvas.drawArc(ring, t * 160f % 360f, 95f, false, wp);

        wp.setStrokeWidth(2.3f);
        wp.setColor(Color.parseColor("#E1BEE7"));
        wp.setAlpha(150);
        RectF innerRing = new RectF(cx - w * 0.28f, cy - h * 0.28f,
                cx + w * 0.28f, cy + h * 0.28f);
        canvas.drawArc(innerRing, 210f - (t * 210f % 360f), 110f, false, wp);

        wp.setStyle(Paint.Style.FILL);
        wp.setStrokeWidth(0f);
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
                if (currentInk > 0) {
                    if (!isGameStarted) {
                        isGameStarted      = true;
                        hudShouldBeVisible = true;
                        runStartTimeMs = System.currentTimeMillis();
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
