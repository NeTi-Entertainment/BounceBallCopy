package com.example.bounceball;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Rendu statique des previews de backgrounds, clippé en cercle.
 * Point d'entrée unique : BgPreviewRenderer.draw(canvas, cx, cy, r, skinId)
 *
 * Couleurs et formes extraites directement de BackgroundRenderer.
 * Les patterns animés sont présentés dans un état figé représentatif.
 */
public final class BgPreviewRenderer {

    private BgPreviewRenderer() {}

    private static final Paint P  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint P2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path  PT = new Path();
    private static final RectF RF = new RectF();

    // Palettes dégradés — identiques à GRAD_COLORS dans BackgroundRenderer
    private static final int[][] GRAD_COLORS = {
            { 0xFFDD0040, 0xFFFF8800, 0xFFEEDD00, 0xFF00BB44, 0xFF2266FF, 0xFF9900DD }, // rainbow
            { 0xFFEE4400, 0xFFFF6644, 0xFFDD2266, 0xFF772299, 0xFFFF7733, 0xFFCC1166 }, // sunset
            { 0xFF0033AA, 0xFF00AACC, 0xFF00BB88, 0xFF1155CC, 0xFF007788, 0xFF002255 }, // ocean
            { 0xFF226622, 0xFF557722, 0xFF887733, 0xFFAA7722, 0xFF114411, 0xFF886622 }, // forest
            { 0xFFCC1100, 0xFFFF4400, 0xFFFF8800, 0xFF331100, 0xFFEE2200, 0xFF552200 }, // lava
            { 0xFF00CC66, 0xFF00AAAA, 0xFF6633CC, 0xFF2288AA, 0xFF44DD88, 0xFF7744BB }, // aurora
            { 0xFFFF3388, 0xFFDD44CC, 0xFF8855EE, 0xFFFF66AA, 0xFFCC22FF, 0xFFFF88CC }, // candy
            { 0xFF220000, 0xFF880000, 0xFFCC3300, 0xFF110000, 0xFF661100, 0xFFFF5500 }, // volcano
            { 0xFF1A0033, 0xFF4400AA, 0xFF8800CC, 0xFFDD44AA, 0xFF2200BB, 0xFF110044 }, // galaxy
            { 0xFF33DD00, 0xFFAAEE00, 0xFF006633, 0xFF88FF00, 0xFF004422, 0xFFCCFF22 }, // toxic
    };

    // ──────────────────────────────────────────────────
    // POINT D'ENTRÉE
    // ──────────────────────────────────────────────────
    public static void draw(Canvas canvas, float cx, float cy, float r, String skinId) {
        if (skinId == null) { drawDefault(canvas, cx, cy, r); return; }

        // Clip en cercle
        int saved = canvas.save();
        PT.reset();
        PT.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.clipPath(PT);

        if      (skinId.equals("bg_default"))          drawDefault(canvas, cx, cy, r);
        else if (skinId.startsWith("bg_grad_"))        drawGrad(canvas, cx, cy, r, skinId);
        else if (skinId.startsWith("bg_minimal_"))     drawMinimal(canvas, cx, cy, r, skinId);
        else if (skinId.startsWith("bg_urban_"))       drawUrban(canvas, cx, cy, r, skinId);
        else                                            drawDefault(canvas, cx, cy, r);

        canvas.restoreToCount(saved);

        // Contour
        P.setStyle(Paint.Style.STROKE);
        P.setColor(Color.parseColor("#2A3A4A"));
        P.setAlpha(130);
        P.setStrokeWidth(r * 0.06f);
        canvas.drawCircle(cx, cy, r - r * 0.03f, P);
        P.setAlpha(255);
    }

    // ──────────────────────────────────────────────────
    // bg_default
    // ──────────────────────────────────────────────────
    private static void drawDefault(Canvas canvas, float cx, float cy, float r) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(Color.parseColor("#F5F5F0"));
        canvas.drawRect(cx - r, cy - r, cx + r, cy + r, P);
    }

    // ──────────────────────────────────────────────────
    // bg_grad_*  — dégradé en bandes avec les vraies couleurs
    // ──────────────────────────────────────────────────
    private static void drawGrad(Canvas canvas, float cx, float cy, float r, String id) {
        int[] pal = getPalette(id);
        int n = pal.length;
        float bandH = (r * 2f) / n;
        P.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            P.setColor(pal[i]);
            canvas.drawRect(cx - r, cy - r + i * bandH, cx + r, cy - r + (i + 1) * bandH, P);
        }
        // Sur-couche de blobs pour évoquer l'animation lava-lamp
        P.setAlpha(55);
        for (int i = 0; i < 3; i++) {
            float bx = cx + (i - 1) * r * 0.4f;
            float by = cy + (float) Math.sin(i * 1.8f) * r * 0.3f;
            P.setColor(pal[(i + 1) % n]);
            canvas.drawCircle(bx, by, r * 0.38f, P);
        }
        P.setAlpha(255);
    }

    // ──────────────────────────────────────────────────
    // bg_minimal_*
    // ──────────────────────────────────────────────────
    private static void drawMinimal(Canvas canvas, float cx, float cy, float r, String id) {
        // Fond
        P.setStyle(Paint.Style.FILL);
        P.setColor(getMinimalBg(id));
        canvas.drawRect(cx - r, cy - r, cx + r, cy + r, P);

        P2.setColor(getMinimalFg(id));
        P2.setAlpha(200);

        switch (id) {
            case "bg_minimal_triangles": {
                P2.setStyle(Paint.Style.FILL);
                float[] tx1 = {cx - r*0.55f, cx + r*0.05f, cx - r*0.25f};
                float[] ty1 = {cy - r*0.05f, cy - r*0.58f, cy + r*0.52f};
                drawTriPath(canvas, tx1, ty1);
                float[] tx2 = {cx + r*0.35f, cx + r*0.75f, cx + r*0.10f};
                float[] ty2 = {cy + r*0.10f, cy - r*0.30f, cy + r*0.60f};
                drawTriPath(canvas, tx2, ty2);
                break;
            }
            case "bg_minimal_circles": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                P2.setAlpha(160);
                canvas.drawCircle(cx - r*0.3f, cy,          r * 0.42f, P2);
                canvas.drawCircle(cx + r*0.35f, cy - r*0.3f, r * 0.30f, P2);
                P2.setAlpha(90);
                canvas.drawCircle(cx + r*0.05f, cy + r*0.5f,  r * 0.22f, P2);
                break;
            }
            case "bg_minimal_lines": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                P2.setAlpha(170);
                for (float ox = -r * 0.6f; ox <= r * 0.65f; ox += r * 0.30f)
                    canvas.drawLine(cx + ox, cy - r, cx + ox + r * 0.2f, cy + r, P2);
                break;
            }
            case "bg_minimal_dots": {
                P2.setStyle(Paint.Style.FILL);
                float dot = r * 0.06f;
                for (int row = -2; row <= 2; row++)
                    for (int col = -2; col <= 2; col++)
                        canvas.drawCircle(cx + col * r * 0.36f, cy + row * r * 0.36f, dot, P2);
                break;
            }
            case "bg_minimal_hexagons": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                P2.setAlpha(190);
                drawHex(canvas, cx - r*0.28f, cy - r*0.18f, r * 0.36f, P2);
                P2.setAlpha(110);
                drawHex(canvas, cx + r*0.38f, cy + r*0.32f, r * 0.26f, P2);
                break;
            }
        }
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────
    // bg_urban_*
    // ──────────────────────────────────────────────────
    private static void drawUrban(Canvas canvas, float cx, float cy, float r, String id) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(getUrbanBg(id));
        canvas.drawRect(cx - r, cy - r, cx + r, cy + r, P);

        switch (id) {
            case "bg_urban_rain": {
                // Gouttes néon diagonales + halos
                int[] nc = {0xFF00E5FF, 0xFFFF4081, 0xFFEA80FC, 0xFF40C4FF};
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeCap(Paint.Cap.ROUND);
                P2.setStrokeWidth(r * 0.05f);
                for (int i = 0; i < 8; i++) {
                    float ox = cx - r * 0.75f + i * r * 0.22f;
                    P2.setColor(nc[i % nc.length]);
                    P2.setAlpha(200);
                    float len = r * (0.25f + (i % 3) * 0.15f);
                    canvas.drawLine(ox, cy - r * 0.4f, ox - r * 0.06f, cy - r * 0.4f + len, P2);
                }
                // Halos en bas
                for (int i = 0; i < 3; i++) {
                    float hx = cx - r*0.5f + i * r*0.5f;
                    P2.setColor(nc[i % nc.length]);
                    P2.setAlpha(60);
                    P2.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(hx, cy + r * 0.7f, r * 0.22f, P2);
                }
                break;
            }
            case "bg_urban_circuit": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(0xFFCCDAC8);
                P2.setStrokeWidth(r * 0.06f);
                P2.setAlpha(200);
                // Traces horizontales + verticales
                float y0 = cy - r * 0.25f, y1 = cy + r * 0.25f;
                canvas.drawLine(cx - r, y0, cx + r, y0, P2);
                canvas.drawLine(cx - r, y1, cx + r, y1, P2);
                canvas.drawLine(cx - r*0.4f, y0, cx - r*0.4f, y1, P2);
                canvas.drawLine(cx + r*0.4f, y0, cx + r*0.4f, y1, P2);
                // Nœuds
                P2.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx - r*0.4f, y0, r*0.06f, P2);
                canvas.drawCircle(cx + r*0.4f, y0, r*0.06f, P2);
                canvas.drawCircle(cx - r*0.4f, y1, r*0.06f, P2);
                canvas.drawCircle(cx + r*0.4f, y1, r*0.06f, P2);
                // Pulse jaune
                P2.setAlpha(140);
                P2.setColor(0xFFFFEE00);
                canvas.drawCircle(cx, cy, r*0.09f, P2);
                break;
            }
            case "bg_urban_grid": {
                // Grille perspective + soleil vaporwave
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.05f);
                P2.setColor(0xFFFF44CC);
                P2.setAlpha(200);
                float hy = cy + r * 0.1f; // horizon
                // Lignes horizontales convergentes
                for (int i = 0; i < 4; i++) {
                    float yy = hy + (i + 1) * r * 0.22f;
                    canvas.drawLine(cx - r, yy, cx + r, yy, P2);
                }
                // Lignes verticales
                P2.setAlpha(130);
                for (int i = -2; i <= 2; i++) {
                    canvas.drawLine(cx + i * r * 0.38f, hy, cx - r + (i + 2) * r * 0.5f, cy + r, P2);
                }
                // Soleil
                P2.setStyle(Paint.Style.FILL);
                P2.setColor(0xFFFF44CC);
                P2.setAlpha(220);
                canvas.drawCircle(cx, hy - r * 0.1f, r * 0.26f, P2);
                P2.setColor(0xFF060010);
                P2.setAlpha(255);
                for (int b = 0; b < 3; b++) {
                    float by2 = hy - r * 0.1f + b * r * 0.075f - r*0.04f;
                    canvas.drawRect(cx - r*0.27f, by2, cx + r*0.27f, by2 + r*0.035f, P2);
                }
                break;
            }
            case "bg_urban_tunnel": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeCap(Paint.Cap.BUTT);
                int[] tc = {0xFFFFEE00, 0xFFFFCC00, 0xFFFFAA22};
                for (int i = 0; i < 4; i++) {
                    float sc = 0.85f - i * 0.20f;
                    if (sc <= 0f) break;
                    P2.setColor(tc[i % tc.length]);
                    P2.setStrokeWidth(r * 0.055f);
                    P2.setAlpha(200 - i * 40);
                    RF.set(cx - r*sc, cy - r*sc*0.75f, cx + r*sc, cy + r*sc*0.75f);
                    canvas.drawRect(RF, P2);
                }
                break;
            }
            case "bg_urban_equalizer": {
                P2.setStyle(Paint.Style.FILL);
                float bw = r * 0.13f;
                float baseY2 = cy + r * 0.75f;
                float[] heights = {0.55f, 0.90f, 0.40f, 0.75f, 0.60f, 0.85f};
                int[] ec = {0xFF00AAFF, 0xFF0088DD, 0xFF00CCFF, 0xFF4499FF, 0xFF0077CC, 0xFF55AAFF};
                for (int b = 0; b < 6; b++) {
                    float bx2 = cx - r * 0.65f + b * (bw + r * 0.05f);
                    float bh2 = r * heights[b] * 1.5f;
                    P2.setColor(ec[b]);
                    P2.setAlpha(220);
                    RF.set(bx2, baseY2 - bh2, bx2 + bw, baseY2);
                    canvas.drawRect(RF, P2);
                    // Halo
                    P2.setAlpha(60);
                    canvas.drawRect(bx2 - r*0.02f, baseY2 - bh2 - r*0.06f,
                            bx2 + bw + r*0.02f, baseY2 - bh2 + r*0.06f, P2);
                }
                break;
            }
        }
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────
    private static void drawTriPath(Canvas c, float[] xs, float[] ys) {
        PT.reset();
        PT.moveTo(xs[0], ys[0]);
        PT.lineTo(xs[1], ys[1]);
        PT.lineTo(xs[2], ys[2]);
        PT.close();
        c.drawPath(PT, P2);
    }

    private static void drawHex(Canvas c, float cx, float cy, float r, Paint paint) {
        PT.reset();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6.0 + i * Math.PI / 3.0;
            float x = cx + r * (float) Math.cos(a);
            float y = cy + r * (float) Math.sin(a);
            if (i == 0) PT.moveTo(x, y); else PT.lineTo(x, y);
        }
        PT.close();
        c.drawPath(PT, paint);
    }

    private static int[] getPalette(String id) {
        switch (id) {
            case "bg_grad_rainbow":  return GRAD_COLORS[0];
            case "bg_grad_sunset":   return GRAD_COLORS[1];
            case "bg_grad_ocean":    return GRAD_COLORS[2];
            case "bg_grad_forest":   return GRAD_COLORS[3];
            case "bg_grad_lava":     return GRAD_COLORS[4];
            case "bg_grad_aurora":   return GRAD_COLORS[5];
            case "bg_grad_candy":    return GRAD_COLORS[6];
            case "bg_grad_volcano":  return GRAD_COLORS[7];
            case "bg_grad_galaxy":   return GRAD_COLORS[8];
            case "bg_grad_toxic":    return GRAD_COLORS[9];
            default:                 return new int[]{0xFF222222, 0xFF444444};
        }
    }

    private static int getMinimalBg(String id) {
        switch (id) {
            case "bg_minimal_triangles": return 0xFF6AADAA;
            case "bg_minimal_circles":   return 0xFF4A5568;
            case "bg_minimal_lines":     return 0xFF2A3550;
            case "bg_minimal_dots":      return 0xFF3D1F2A;
            case "bg_minimal_hexagons":  return 0xFF3A2D1A;
            default:                     return 0xFF1A2A3A;
        }
    }

    private static int getMinimalFg(String id) {
        switch (id) {
            case "bg_minimal_triangles": return Color.WHITE;
            case "bg_minimal_circles":   return Color.WHITE;
            case "bg_minimal_lines":     return 0xFF6688AA;
            case "bg_minimal_dots":      return 0xFFCC6688;
            case "bg_minimal_hexagons":  return 0xFFAA8844;
            default:                     return Color.WHITE;
        }
    }

    private static int getUrbanBg(String id) {
        switch (id) {
            case "bg_urban_rain":       return 0xFF12203A;
            case "bg_urban_circuit":    return 0xFF1A4A1F;
            case "bg_urban_grid":       return 0xFF060010;
            case "bg_urban_tunnel":     return 0xFF020008;
            case "bg_urban_equalizer":  return 0xFF050505;
            default:                    return 0xFF0A0A1A;
        }
    }
}
