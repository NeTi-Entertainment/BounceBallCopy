package com.example.bounceball.colony;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import com.example.bounceball.R;

import java.util.Random;

public class ColonyView extends View {

    interface OnSlotTappedListener {
        void onSlotTapped(int slotIndex);
    }

    private ColonyBuildingSlot[] slots;
    private OnSlotTappedListener listener;

    private final Paint bgPaint         = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubFillPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubBorderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotFillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint levelPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyIconPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint upgradeRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maxRingPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float centerX, centerY;
    private float orbitRadius;
    private float slotRadius;
    private float hubRadius;

    private final float[] slotX = new float[ColonyManager.SLOT_COUNT];
    private final float[] slotY = new float[ColonyManager.SLOT_COUNT];

    private Bitmap bmpHub;
    private final Bitmap[] bmpBuildings = new Bitmap[ColonyManager.SLOT_COUNT];
    private final RectF drawRect = new RectF();
    private final Paint alphaPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint decoPaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint upgradeRingBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timerPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringRect         = new RectF();

    // ── Paramètres de placement pseudo-aléatoire des copies décoratives ─────
    // Cône angulaire autour du bâtiment parent dans lequel on tire les décos.
    // Offset min évite qu'une déco tombe sur son bâtiment parent.
    // Offset max < (sectorDeg / 2) pour éviter d'empiéter sur le slot voisin.
    private static final float DECO_MIN_OFFSET_DEG   = 18f;
    private static final float DECO_MAX_OFFSET_DEG   = 48f;
    private static final float DECO_MIN_RADIUS_FRAC  = 0.42f;
    private static final float DECO_MAX_RADIUS_FRAC  = 0.55f;
    // Distance minimale (en fraction de minDim) entre deux décos d'un même slot.
    private static final float DECO_MIN_SEPARATION   = 0.13f;
    // Seed de base. Changer cette valeur = regénérer entièrement tous les patterns.
    private static final long  DECO_SEED_BASE        = 0xB00BEEF5CA1AB1EL;
    // Nombre max de tirages avant d'accepter faute de mieux.
    private static final int   DECO_MAX_TRIES        = 40;

    private float[][] decoX;
    private float[][] decoY;

    public ColonyView(Context context, ColonyBuildingSlot[] slots) {
        super(context);
        this.slots = slots;

        bgPaint.setColor(Color.parseColor("#0A130A"));

        linePaint.setColor(Color.parseColor("#1A3A1A"));
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);

        hubFillPaint.setColor(Color.parseColor("#1B3A1B"));
        hubFillPaint.setStyle(Paint.Style.FILL);

        hubBorderPaint.setColor(Color.parseColor("#4CAF50"));
        hubBorderPaint.setStyle(Paint.Style.STROKE);
        hubBorderPaint.setStrokeWidth(4f);

        shadowPaint.setColor(Color.parseColor("#08100A"));
        shadowPaint.setStyle(Paint.Style.FILL);

        slotFillPaint.setStyle(Paint.Style.FILL);

        slotBorderPaint.setStyle(Paint.Style.STROKE);
        slotBorderPaint.setStrokeWidth(3f);

        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        levelPaint.setColor(Color.parseColor("#FFD700"));
        levelPaint.setTextAlign(Paint.Align.CENTER);

        emptyIconPaint.setColor(Color.parseColor("#2E5A2E"));
        emptyIconPaint.setTextAlign(Paint.Align.CENTER);

        upgradeRingPaint.setColor(Color.parseColor("#76FF03"));
        upgradeRingPaint.setStyle(Paint.Style.STROKE);
        upgradeRingPaint.setStrokeWidth(5f);

        maxRingPaint.setColor(Color.parseColor("#FFD700"));
        maxRingPaint.setStyle(Paint.Style.STROKE);
        maxRingPaint.setStrokeWidth(5f);

        bmpHub = BitmapFactory.decodeResource(context.getResources(), R.drawable.building_hub);

        alphaPaint.setAlpha(110);
        decoPaint.setAlpha(210);

        upgradeRingBgPaint.setColor(Color.parseColor("#2A4A2A"));
        upgradeRingBgPaint.setStyle(Paint.Style.STROKE);
        upgradeRingBgPaint.setStrokeWidth(6f);
        upgradeRingBgPaint.setAntiAlias(true);

        timerPaint.setColor(Color.WHITE);
        timerPaint.setTextAlign(Paint.Align.CENTER);
        timerPaint.setFakeBoldText(true);

        timerStrokePaint.setColor(Color.BLACK);
        timerStrokePaint.setTextAlign(Paint.Align.CENTER);
        timerStrokePaint.setFakeBoldText(true);
        timerStrokePaint.setStyle(Paint.Style.STROKE);
        timerStrokePaint.setStrokeWidth(5f);
        timerStrokePaint.setStrokeJoin(Paint.Join.ROUND);

        int[] resIds = {
                R.drawable.building_habitation,
                R.drawable.building_water,
                R.drawable.building_oxygen,
                R.drawable.building_farm,
        };
        for (int i = 0; i < resIds.length && i < ColonyManager.SLOT_COUNT; i++) {
            bmpBuildings[i] = BitmapFactory.decodeResource(context.getResources(), resIds[i]);
        }
    }

    public void setSlots(ColonyBuildingSlot[] slots) {
        this.slots = slots;
        invalidate();
    }

    public void setOnSlotTappedListener(OnSlotTappedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        float minDim = Math.min(w, h);
        hubRadius    = minDim * 0.21f;//scale hub
        slotRadius   = minDim * 0.115f;
        orbitRadius  = minDim * 0.32f;

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            double angle = Math.toRadians(-90 + i * (360.0 / ColonyManager.SLOT_COUNT));
            slotX[i] = centerX + (float)(orbitRadius * Math.cos(angle));
            slotY[i] = centerY + (float)(orbitRadius * Math.sin(angle));
        }

        computeDecoPositions(minDim);
    }

    private void computeDecoPositions(float minDim) {
        int slotCount = ColonyManager.SLOT_COUNT;
        decoX = new float[slotCount][3];
        decoY = new float[slotCount][3];

        float sectorDeg = 360f / slotCount;

        float[] chosenAngles = new float[3];
        float[] chosenRadii  = new float[3];

        for (int i = 0; i < slotCount; i++) {
            float mainAngleDeg = -90f + i * sectorDeg;

            // Un seed distinct par slot casse toute symétrie rotationnelle
            // entre slots : chaque bâtiment a son propre pattern unique.
            Random rng = new Random(DECO_SEED_BASE
                    ^ ((long)(i + 1) * 0x9E3779B97F4A7C15L));

            for (int d = 0; d < 3; d++) {
                float relAngle = 0f;
                float r = 0f;

                for (int tries = 0; tries < DECO_MAX_TRIES; tries++) {
                    float sign = rng.nextBoolean() ? 1f : -1f;
                    float mag  = DECO_MIN_OFFSET_DEG
                            + rng.nextFloat() * (DECO_MAX_OFFSET_DEG - DECO_MIN_OFFSET_DEG);
                    relAngle = sign * mag;
                    r = DECO_MIN_RADIUS_FRAC
                            + rng.nextFloat() * (DECO_MAX_RADIUS_FRAC - DECO_MIN_RADIUS_FRAC);

                    // Vérifie la séparation avec les décos déjà placées du même slot.
                    double a = Math.toRadians(mainAngleDeg + relAngle);
                    float xNorm = (float)(r * Math.cos(a));
                    float yNorm = (float)(r * Math.sin(a));

                    boolean ok = true;
                    for (int j = 0; j < d; j++) {
                        double aj = Math.toRadians(mainAngleDeg + chosenAngles[j]);
                        float xj = (float)(chosenRadii[j] * Math.cos(aj));
                        float yj = (float)(chosenRadii[j] * Math.sin(aj));
                        float dx = xNorm - xj;
                        float dy = yNorm - yj;
                        if (dx * dx + dy * dy < DECO_MIN_SEPARATION * DECO_MIN_SEPARATION) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) break;
                }

                chosenAngles[d] = relAngle;
                chosenRadii[d]  = r;

                double a = Math.toRadians(mainAngleDeg + relAngle);
                decoX[i][d] = centerX + (float)(r * minDim * Math.cos(a));
                decoY[i][d] = centerY + (float)(r * minDim * Math.sin(a));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            drawDecoSlots(canvas, i);
        }

        drawHub(canvas);

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            drawSlot(canvas, i);
        }
    }

    private int getDecoCount(ColonyBuildingSlot slot) {
        int lv = slot.getLevel();
        if (lv >= 9) return 3;
        if (lv >= 6) return 2;
        if (lv >= 3) return 1;
        return 0;
    }

    private void drawDecoSlots(Canvas canvas, int i) {
        if (decoX == null || i >= decoX.length) return;
        ColonyBuildingSlot slot = slots[i];
        if (!slot.isBuilt()) return;
        int count = getDecoCount(slot);
        if (count == 0) return;
        Bitmap bmp = (i < bmpBuildings.length) ? bmpBuildings[i] : null;
        float s = slotRadius * 1.05f;
        for (int d = 0; d < count; d++) {
            float x = decoX[i][d];
            float y = decoY[i][d];
            if (bmp != null) {
                drawRect.set(x - s, y - s, x + s, y + s);
                canvas.drawBitmap(bmp, null, drawRect, decoPaint);
            } else {
                emptyIconPaint.setTextSize(s * 0.6f);
                canvas.drawText(slot.type.icon, x, y + s * 0.22f, emptyIconPaint);
            }
        }
    }

    private void drawHub(Canvas canvas) {
        canvas.drawCircle(centerX, centerY + hubRadius * 0.08f, hubRadius * 1.06f, shadowPaint);
        if (bmpHub != null) {
            drawRect.set(centerX - hubRadius, centerY - hubRadius,
                    centerX + hubRadius, centerY + hubRadius);
            canvas.drawBitmap(bmpHub, null, drawRect, null);
        } else {
            canvas.drawCircle(centerX, centerY, hubRadius, hubFillPaint);
            canvas.drawCircle(centerX, centerY, hubRadius, hubBorderPaint);
            iconPaint.setTextSize(hubRadius * 0.72f);
            canvas.drawText("🌕", centerX, centerY + iconPaint.getTextSize() * 0.36f, iconPaint);
        }
    }

    private void drawSlot(Canvas canvas, int i) {
        ColonyBuildingSlot slot = slots[i];
        float x = slotX[i];
        float y = slotY[i];

        Bitmap bmp = (i < bmpBuildings.length) ? bmpBuildings[i] : null;

        if (slot.isBuilt()) {
            float s = slotRadius * 1.33f;
            if (bmp != null) {
                drawRect.set(x - s, y - s, x + s, y + s);
                canvas.drawBitmap(bmp, null, drawRect, null);
            } else {
                iconPaint.setTextSize(slotRadius * 0.68f);
                canvas.drawText(slot.type.icon, x, y + iconPaint.getTextSize() * 0.36f, iconPaint);
            }

            // ── Variables à ajuster ──────────────────────
            float ringR      = s * 0.72f;  // taille du cercle — plus petit que le sprite
            float ringOffsetY = -s * 0.10f; // décalage vertical du centre du cercle (négatif = vers le haut)
            float lvOffsetY   = s * 0.82f;  // position fixe du label Lv sous le sprite
            // ─────────────────────────────────────────────

            float ringCY = y + ringOffsetY;

            levelPaint.setTextSize(slotRadius * 0.30f);

            if (slot.isMaxLevel()) {
                canvas.drawCircle(x, ringCY, ringR, maxRingPaint);

            } else if (slot.isUpgrading()) {
                canvas.drawCircle(x, ringCY, ringR, upgradeRingBgPaint);

                long remaining = slot.getRemainingMillis();
                long total     = slot.getUpgradeDurationMillis();
                float progress = (total > 0) ? (1f - (float) remaining / total) : 1f;
                upgradeRingPaint.setStrokeWidth(6f);
                ringRect.set(x - ringR, ringCY - ringR, x + ringR, ringCY + ringR);
                canvas.drawArc(ringRect, -90f, progress * 360f, false, upgradeRingPaint);

                timerPaint.setTextSize(slotRadius * 0.34f);
                timerStrokePaint.setTextSize(slotRadius * 0.34f);

                // ── Variable à ajuster ───────────────────
                float timerOffsetY = s * 0.18f; // décalage vers le bas (0f = centré, positif = descend)
                // ─────────────────────────────────────────

                float timerY = ringCY + timerPaint.getTextSize() * 0.36f + timerOffsetY;
                String timerStr = formatTimer(remaining);
                canvas.drawText(timerStr, x, timerY, timerStrokePaint);
                canvas.drawText(timerStr, x, timerY, timerPaint);
            }

            // Label Lv toujours à la même position
            canvas.drawText("Lv" + slot.getLevel(), x, y + lvOffsetY, levelPaint);

        } else {
            float s = slotRadius * 1.08f;
            if (bmp != null) {
                drawRect.set(x - s, y - s, x + s, y + s);
                canvas.drawBitmap(bmp, null, drawRect, alphaPaint);
            } else {
                emptyIconPaint.setTextSize(slotRadius * 0.62f);
                canvas.drawText(slot.type.icon, x,
                        y + emptyIconPaint.getTextSize() * 0.36f, emptyIconPaint);
            }

            if (slot.isUpgrading()) {
                float ringR       = s * 0.72f;
                float ringOffsetY = -s * 0.10f;
                float ringCY      = y + ringOffsetY;

                canvas.drawCircle(x, ringCY, ringR, upgradeRingBgPaint);

                long remaining = slot.getRemainingMillis();
                long total     = slot.getUpgradeDurationMillis();
                float progress = (total > 0) ? (1f - (float) remaining / total) : 1f;
                upgradeRingPaint.setStrokeWidth(6f);
                ringRect.set(x - ringR, ringCY - ringR, x + ringR, ringCY + ringR);
                canvas.drawArc(ringRect, -90f, progress * 360f, false, upgradeRingPaint);

                timerPaint.setTextSize(slotRadius * 0.34f);
                timerStrokePaint.setTextSize(slotRadius * 0.34f);
                float timerOffsetY = s * 0.18f;
                float timerY = ringCY + timerPaint.getTextSize() * 0.36f + timerOffsetY;
                String timerStr = formatTimer(remaining);
                canvas.drawText(timerStr, x, timerY, timerStrokePaint);
                canvas.drawText(timerStr, x, timerY, timerPaint);
            }
        }
    }

    private String formatTimer(long millis) {
        long secs = millis / 1000L;
        if (secs < 60)   return secs + "s";
        long mins = secs / 60;
        long s2   = secs % 60;
        if (mins < 60)   return mins + "m" + s2 + "s";
        long hrs  = mins / 60;
        long m2   = mins % 60;
        return hrs + "h" + m2 + "m";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float tx = event.getX();
            float ty = event.getY();
            float tapRadius = slotRadius * 1.4f;
            for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
                float dx = tx - slotX[i];
                float dy = ty - slotY[i];
                if (dx * dx + dy * dy <= tapRadius * tapRadius) {
                    if (listener != null) listener.onSlotTapped(i);
                    return true;
                }
            }
        }
        return true;
    }
}