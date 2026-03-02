package com.example.bounceball.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Particle {
    float x, y, vx, vy;
    float life = 1f;
    float maxLife = 1f;
    int color;
    float size;
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public float getY() {
        return this.y;
    }

    public Particle(float x, float y, int color) {
        this.x = x; this.y = y;
        this.color = color;
        this.size = 4f + (float)(Math.random() * 8f);
        this.maxLife = 0.4f + (float)(Math.random() * 0.4f);
        this.life = maxLife;
        double angle = Math.random() * Math.PI * 2;
        float speed = 100f + (float)(Math.random() * 300f);
        this.vx = (float)(Math.cos(angle) * speed);
        this.vy = (float)(Math.sin(angle) * speed) - 200f;
    }

    public void update(float dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 400f * dt;
        life -= dt;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Canvas canvas) {
        float alpha = life / maxLife;
        paint.setColor(color);
        paint.setAlpha((int)(alpha * 220));
        canvas.drawCircle(x, y, size * alpha, paint);
    }
}
