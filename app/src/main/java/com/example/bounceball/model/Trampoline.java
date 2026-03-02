package com.example.bounceball.model;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.bounceball.upgrade.UpgradeStats;

public class Trampoline {
    public float x1, y1, x2, y2;
    public float springForce;
    private float thickness = 12f;

    public Trampoline(float x1, float y1, float x2, float y2, UpgradeStats upgrades) {
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        // Spring force based on elasticity upgrade
        this.springForce = 200f + upgrades.elasticity * 80f;
    }

    public float getLength() {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns the collision point [x, y] if ball collides, null otherwise.
     * Simple line-segment vs circle test.
     */
    public float[] checkCollision(Ball ball) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        if (len2 == 0) return null;

        // Project ball center onto segment
        float t = ((ball.x - x1) * dx + (ball.y - y1) * dy) / len2;
        t = Math.max(0f, Math.min(1f, t));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        float distX = ball.x - closestX;
        float distY = ball.y - closestY;
        float dist = (float) Math.sqrt(distX * distX + distY * distY);

        if (dist < ball.radius + thickness / 2f) {
            // Check ball is coming from above (vy > 0)
            if (ball.vy > 0 && ball.y < closestY + ball.radius) {
                return new float[]{closestX, closestY};
            }
        }
        return null;
    }

    public void draw(Canvas canvas, Paint paint) {
        // Shadow
        Paint shadowPaint = new Paint(paint);
        shadowPaint.setColor(Color.parseColor("#4400E676"));
        shadowPaint.setStrokeWidth(paint.getStrokeWidth() + 8f);
        canvas.drawLine(x1, y1, x2, y2, shadowPaint);

        // Main line
        canvas.drawLine(x1, y1, x2, y2, paint);

        // Endpoints
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#80FFFFFF"));
        dotPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x1, y1, 8f, dotPaint);
        canvas.drawCircle(x2, y2, 8f, dotPaint);
    }
}
