package com.example.bounceball.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import com.example.bounceball.upgrade.UpgradeStats;

public class Ball {
    public float x, y;
    public float vx, vy;
    public float radius = 28f;
    public float mass;
    public float airResistance;

    private static final float GRAVITY = 980f; // pixels/s^2

    private Paint glowPaint;
    private Paint corePaint;
    private int skinColor = Color.parseColor("#E0E0E0");
    private int glowColor = Color.parseColor("#80FFFFFF");

    public Ball(float x, float y, UpgradeStats upgrades) {
        this.x = x;
        this.y = y;
        this.vx = 0f;
        this.vy = 0f;
        // Mass: heavier ball builds more momentum but needs more force to go up
        this.mass = 1f + upgrades.weight * 0.2f;
        // Air resistance: 0 = no drag, higher = more drag
        this.airResistance = Math.max(0.01f, 0.15f - upgrades.airResistance * 0.015f);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(skinColor);
    }

    public void update(float dt) {
        // Gravity
        vy += GRAVITY * mass * dt;

        // Air drag (quadratic drag)
        float speed = (float) Math.sqrt(vx * vx + vy * vy);
        if (speed > 0) {
            float drag = airResistance * speed * speed * dt;
            float dragRatio = Math.min(drag / speed, speed * 0.9f) / speed;
            vx -= vx * dragRatio;
            vy -= vy * dragRatio;
        }

        x += vx * dt;
        y += vy * dt;
    }

    public void draw(Canvas canvas, Paint paint) {
        // Glow effect
        glowPaint.setColor(glowColor);
        glowPaint.setAlpha(80);
        canvas.drawCircle(x, y, radius * 1.5f, glowPaint);

        // Core
        corePaint.setColor(skinColor);
        canvas.drawCircle(x, y, radius, corePaint);

        // Shine
        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.WHITE);
        shinePaint.setAlpha(120);
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.35f, shinePaint);
    }

    public void setSkin(int color, int glow) {
        this.skinColor = color;
        this.glowColor = glow;
    }
}
