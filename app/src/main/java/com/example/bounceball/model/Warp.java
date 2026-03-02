package com.example.bounceball.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Warp {
    public float x, y;
    private float life = 8f;
    private float maxLife = 8f;
    private float pulse = 0f;

    public Warp(float x, float y) {
        this.x = x; this.y = y;
    }

    public void update(float dt) {
        life -= dt;
        pulse += dt * 3f;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Canvas canvas, Paint paint) {
        float alpha = Math.min(1f, life / 2f) * (0.6f + 0.4f * (float)Math.sin(pulse));
        paint.setAlpha((int)(alpha * 255));
        float r = 30f + 8f * (float)Math.sin(pulse);
        canvas.drawCircle(x, y, r, paint);
        paint.setAlpha((int)(alpha * 120));
        canvas.drawCircle(x, y, r * 1.5f, paint);
        paint.setAlpha(255);

        // Swirl lines
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setAlpha((int)(alpha * 180));
        linePaint.setStrokeWidth(2f);
        for (int i = 0; i < 4; i++) {
            double angle = pulse + i * Math.PI / 2;
            float lx = (float)Math.cos(angle) * r * 0.8f;
            float ly = (float)Math.sin(angle) * r * 0.8f;
            canvas.drawLine(x, y, x + lx, y + ly, linePaint);
        }
    }
}
