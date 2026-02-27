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
import androidx.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import com.example.bounceball.upgrade.UpgradeStats;
import com.example.bounceball.utils.GamePreferences;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final float GRAVITY = 0.6f;
    private float airResistance;        // modifié par upgrade
    private float trampElasticity;      // modifié par upgrade
    private float maxInk;
    private float currentInk;
    private float inkConsumptionRate;   // modifié par upgrade

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

    private Bitmap ballSprite;
    private Bitmap gaugeSprite;
    private float gaugeOffsetX = -500f;
    private float statsOffsetY = -250f;
    private boolean hudShouldBeVisible = false;
    private static final float HUD_ANIM_SPEED = 1800f;

    private GamePreferences prefs;
    private UpgradeStats upgrades;

    public void setHudVisible(boolean visible) {
        hudShouldBeVisible = visible;
    }

    /**
     * Interface mise à jour : onGameOver reçoit la hauteur atteinte.
     * La gestion de l'or ET la sauvegarde sont faites dans GameActivity.
     */
    public interface GameStateListener {
        void onGameStarted();
        void onGameOver(float heightReached);
    }

    private GameStateListener gameStateListener;

    public void setGameStateListener(GameStateListener l) { this.gameStateListener = l; }

    public GameView(Context context, GamePreferences prefs, UpgradeStats upgrades) {
        super(context);
        this.prefs = prefs;
        this.upgrades = upgrades;

        // Applique les upgrades aux constantes de jeu
        applyUpgrades();

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(false);

        ballSprite = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        gaugeSprite = BitmapFactory.decodeResource(getResources(), R.drawable.gauge);

        currentInk = maxInk;
        isGameStarted = false;
        isGameOver = false;
        totalHeightMeters = 0f;
    }

    private void applyUpgrades() {
        // Air resistance: base 0.98, +0.002 par niveau (max 10 → 0.998)
        airResistance = 0.98f + upgrades.airResistance * 0.002f;

        // Trampoline elasticity: base 25, +3 par niveau
        trampElasticity = 25f + upgrades.elasticity * 3f;

        // Ink: base 1000, +150 par niveau d'inkReserve
        maxInk = 1000f + upgrades.inkReserve * 150f;

        // Ink consumption: base 0.4, -0.03 par niveau (min 0.1)
        inkConsumptionRate = Math.max(0.1f, 0.4f - upgrades.inkEfficiency * 0.03f);
    }

    private void resetGame() {
        ballX = screenWidth / 2f;
        ballY = screenHeight / 4f;
        ballVelocityY = 0f;
        ballVelocityX = 0f;
        currentInk = maxInk;
        totalHeightMeters = 0f;
        isGameStarted = false;
        isGameOver = false;
        isDrawingTrampoline = false;
        hasTrampoline = false;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        resetGame();
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        boolean retry = true;
        isRunning = false;
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e("GameView", "Erreur de thread", e);
            }
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            update();
            draw();
            sleep();
        }
    }

    private void update() {
        float hudDt = 0.016f;
        float targetGX = hudShouldBeVisible ? 0f : -500f;
        float targetSY = hudShouldBeVisible ? 0f : -250f;
        if (gaugeOffsetX < targetGX)      gaugeOffsetX = Math.min(gaugeOffsetX + HUD_ANIM_SPEED * hudDt, targetGX);
        else if (gaugeOffsetX > targetGX) gaugeOffsetX = Math.max(gaugeOffsetX - HUD_ANIM_SPEED * hudDt, targetGX);
        if (statsOffsetY < targetSY)      statsOffsetY = Math.min(statsOffsetY + HUD_ANIM_SPEED * hudDt, targetSY);
        else if (statsOffsetY > targetSY) statsOffsetY = Math.max(statsOffsetY - HUD_ANIM_SPEED * hudDt, targetSY);

        if (!isGameStarted || isGameOver) return;

        // Physique
        ballVelocityY += GRAVITY;
        ballVelocityY *= airResistance;
        ballVelocityX *= airResistance;

        ballY += ballVelocityY;
        ballX += ballVelocityX;

        // Murs latéraux
        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            ballVelocityX = -ballVelocityX * 0.8f;
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballVelocityX = -ballVelocityX * 0.8f;
        }

        // Caméra organique (Lerp)
        float idealCameraY = screenHeight * 0.4f;
        if (ballY < idealCameraY) {
            float diff = idealCameraY - ballY;
            float shift = diff * 0.1f;
            ballY += shift;
            if (hasTrampoline) {
                trampStartY += shift;
                trampEndY += shift;
            }
            totalHeightMeters += shift / 100f;
        }

        // Rebond trampoline
        if (ballVelocityY > 0 && hasTrampoline) {
            float minX = Math.min(trampStartX, trampEndX);
            float maxX = Math.max(trampStartX, trampEndX);
            float minY = Math.min(trampStartY, trampEndY) - 30f;
            float maxY = Math.max(trampStartY, trampEndY) + 30f;

            if (ballX + ballRadius >= minX && ballX - ballRadius <= maxX
                    && ballY + ballRadius >= minY && ballY - ballRadius <= maxY) {

                float dx = trampEndX - trampStartX;
                float dy = trampEndY - trampStartY;
                float lineLength = (float) Math.hypot(dx, dy);
                lineLength = Math.max(50.0f, lineLength);

                float nx = -dy / lineLength;
                float ny = dx / lineLength;

                if (ny > 0) { nx = -nx; ny = -ny; }

                float bounceMultiplier = screenWidth / lineLength;
                if (bounceMultiplier > 3.0f) bounceMultiplier = 3.0f;
                if (bounceMultiplier < 0.2f) bounceMultiplier = 0.2f;

                float force = trampElasticity * bounceMultiplier;

                ballVelocityX = nx * force;
                ballVelocityY = ny * force;

                hasTrampoline = false;
            }
        }

        // Game Over
        if (ballY > screenHeight + ballRadius) {
            if (!isGameOver) {
                isGameOver = true;
                float finalHeight = totalHeightMeters;
                if (gameStateListener != null) {
                    gameStateListener.onGameOver(finalHeight);
                }
                resetGame();
            }
        }
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.WHITE);

            RectF ballRect = new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius);
            canvas.drawBitmap(ballSprite, null, ballRect, paint);

            // Jauge d'encre
            canvas.save();
            canvas.translate(gaugeOffsetX, 0);

            float gaugeH = screenHeight * 0.6f;
            float gaugeW = gaugeH * (24f / 256f);
            float gaugeLeft = 40f;
            float gaugeBottom = screenHeight * 0.95f;
            float gaugeTop = gaugeBottom - gaugeH;
            float gaugeRight = gaugeLeft + gaugeW;

            float inkWidthRatio = 16f / 32f;
            float inkWidth = gaugeW * inkWidthRatio;
            float inkLeft = gaugeLeft + (gaugeW - inkWidth) / 2f;
            float inkRight = inkLeft + inkWidth;

            float verticalMarginRatio = 6f / 256f;
            float verticalMargin = gaugeH * verticalMarginRatio;
            float inkMaxTop = gaugeTop + verticalMargin;
            float inkBottom = gaugeBottom - verticalMargin;
            float inkMaxHeight = inkBottom - inkMaxTop;

            float currentInkHeight = inkMaxHeight * (currentInk / maxInk);
            float inkCurrentTop = inkBottom - currentInkHeight;

            canvas.save();
            canvas.clipRect(inkLeft, inkCurrentTop, inkRight, inkBottom);
            paint.setColor(Color.BLACK);
            float cornerRadius = inkWidth / 2f;
            canvas.drawRoundRect(inkLeft, inkMaxTop, inkRight, inkBottom, cornerRadius, cornerRadius, paint);
            canvas.restore();

            RectF destRect = new RectF(gaugeLeft, gaugeTop, gaugeRight, gaugeBottom);
            canvas.drawBitmap(gaugeSprite, null, destRect, paint);
            canvas.restore();

            // HUD stats
            canvas.save();
            canvas.translate(0, statsOffsetY);

            paint.setColor(Color.BLACK);
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.format(Locale.getDefault(), "%.1fm", totalHeightMeters), screenWidth / 2f, 100, paint);

            // Record
            float maxH = prefs.getMaxHeight();
            paint.setTextSize(36);
            paint.setColor(Color.parseColor("#888888"));
            canvas.drawText(String.format(Locale.getDefault(), "Record: %.1fm", maxH), screenWidth / 2f, 150, paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(40);
            canvas.drawText("Or: " + prefs.getGold(), screenWidth / 2f, 200, paint);

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
    }

    private void sleep() {
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            Log.e("GameView", "Erreur de thread", e);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                performClick();
                if (currentInk > 0) {
                    if (!isGameStarted) {
                        isGameStarted = true;
                        hudShouldBeVisible = true;
                        if (gameStateListener != null) gameStateListener.onGameStarted();
                    }
                    isDrawingTrampoline = true;
                    hasTrampoline = false;
                    trampStartX = touchX;
                    trampStartY = touchY;
                    trampEndX = touchX;
                    trampEndY = touchY;
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDrawingTrampoline && currentInk > 0) {
                    boolean outOfBounds = false;
                    if (touchX < 0) { touchX = 0; outOfBounds = true; }
                    else if (touchX > screenWidth) { touchX = screenWidth; outOfBounds = true; }

                    trampEndX = touchX;
                    trampEndY = touchY;

                    float distance = (float) Math.hypot(touchX - lastTouchX, touchY - lastTouchY);
                    currentInk -= distance * inkConsumptionRate;
                    if (currentInk < 0) currentInk = 0;

                    lastTouchX = touchX;
                    lastTouchY = touchY;

                    if (outOfBounds) {
                        isDrawingTrampoline = false;
                        hasTrampoline = true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDrawingTrampoline) {
                    isDrawingTrampoline = false;
                    hasTrampoline = true;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}