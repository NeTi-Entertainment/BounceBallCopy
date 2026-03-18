package com.example.bounceball.colony;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

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
        hubRadius    = minDim * 0.13f;
        slotRadius   = minDim * 0.115f;
        orbitRadius  = minDim * 0.36f;

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            double angle = Math.toRadians(-90 + i * (360.0 / ColonyManager.SLOT_COUNT));
            slotX[i] = centerX + (float)(orbitRadius * Math.cos(angle));
            slotY[i] = centerY + (float)(orbitRadius * Math.sin(angle));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            canvas.drawLine(centerX, centerY, slotX[i], slotY[i], linePaint);
        }

        drawHub(canvas);

        for (int i = 0; i < ColonyManager.SLOT_COUNT; i++) {
            drawSlot(canvas, i);
        }
    }

    private void drawHub(Canvas canvas) {
        canvas.drawCircle(centerX, centerY + hubRadius * 0.08f, hubRadius * 1.06f, shadowPaint);
        canvas.drawCircle(centerX, centerY, hubRadius, hubFillPaint);
        canvas.drawCircle(centerX, centerY, hubRadius, hubBorderPaint);
        iconPaint.setTextSize(hubRadius * 0.72f);
        canvas.drawText("🌕", centerX, centerY + iconPaint.getTextSize() * 0.36f, iconPaint);
    }

    private void drawSlot(Canvas canvas, int i) {
        ColonyBuildingSlot slot = slots[i];
        float x = slotX[i];
        float y = slotY[i];

        canvas.drawCircle(x, y + slotRadius * 0.08f, slotRadius * 1.06f, shadowPaint);

        if (slot.isBuilt()) {
            slotFillPaint.setColor(Color.parseColor("#1A3A1A"));
        } else {
            slotFillPaint.setColor(Color.parseColor("#0F1F0F"));
        }
        canvas.drawCircle(x, y, slotRadius, slotFillPaint);

        if (slot.isBuilt()) {
            if (slot.isMaxLevel()) {
                canvas.drawCircle(x, y, slotRadius, maxRingPaint);
            } else if (slot.isUpgrading()) {
                canvas.drawCircle(x, y, slotRadius, upgradeRingPaint);
            } else {
                slotBorderPaint.setColor(Color.parseColor("#2A4A2A"));
                canvas.drawCircle(x, y, slotRadius, slotBorderPaint);
            }
            iconPaint.setTextSize(slotRadius * 0.68f);
            canvas.drawText(slot.type.icon, x, y + iconPaint.getTextSize() * 0.36f, iconPaint);
            levelPaint.setTextSize(slotRadius * 0.30f);
            canvas.drawText("Lv" + slot.getLevel(), x, y + slotRadius * 0.84f, levelPaint);
        } else {
            slotBorderPaint.setColor(Color.parseColor("#1A3A1A"));
            canvas.drawCircle(x, y, slotRadius, slotBorderPaint);
            emptyIconPaint.setTextSize(slotRadius * 0.62f);
            canvas.drawText(slot.type.icon, x, y + emptyIconPaint.getTextSize() * 0.36f, emptyIconPaint);
        }
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