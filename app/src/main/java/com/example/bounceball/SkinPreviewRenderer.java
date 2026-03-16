package com.example.bounceball;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Utilitaire statique de rendu des thumbnails de skins.
 * Toutes les méthodes sont thread-safe (UI thread uniquement) et sans allocation.
 *
 * Point d'entrée unique :
 *   SkinPreviewRenderer.draw(canvas, cx, cy, radius, skinId, colorHex)
 *
 * Routing par préfixe d'id :
 *   bg_*       → drawBg   (fond clippé en cercle)
 *   fx_*       → drawFx   (effet de traînée)
 *   ball_elem_ → drawElementalBall
 *   + détection metal / space / sport / classic
 *
 * Les objets Paint, Path, RectF statiques sont réutilisés à chaque appel.
 * Aucune allocation dans le chemin de dessin → adapté à onDraw().
 */
public final class SkinPreviewRenderer {

    private SkinPreviewRenderer() {}

    // ── Paints statiques réutilisés (UI thread = single-threaded → safe) ──
    private static final Paint P1   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint P2   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path  PATH = new Path();
    private static final RectF RECT = new RectF();

    // ──────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE
    // ──────────────────────────────────────────────────────────────────────
    public static void draw(Canvas c, float cx, float cy, float r,
                            String skinId, String colorHex) {
        int base = parseColor(colorHex, 0xFF888888);
        if (skinId == null) { drawClassicBall(c, cx, cy, r, base); return; }

        if (skinId.startsWith("bg_"))        { drawBg(c, cx, cy, r, skinId);           return; }
        if (skinId.startsWith("fx_"))        { drawFx(c, cx, cy, r, skinId);           return; }
        if (skinId.startsWith("ball_elem_")) { drawElementalBall(c, cx, cy, r, skinId, base); return; }

        switch (getBallCategory(skinId)) {
            case "metal":  drawMetalBall(c, cx, cy, r, skinId, base); break;
            case "space":  drawSpaceBall(c, cx, cy, r, skinId, base); break;
            case "sport":  drawSportBall(c, cx, cy, r, skinId, base); break;
            default:       drawClassicBall(c, cx, cy, r, base);       break;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // BALLES — CLASSIC
    // ──────────────────────────────────────────────────────────────────────
    private static void drawClassicBall(Canvas c, float cx, float cy, float r, int color) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(color);
        c.drawCircle(cx, cy, r, P1);
        P1.setColor(Color.WHITE);
        P1.setAlpha(90);
        c.drawCircle(cx - r * 0.27f, cy - r * 0.3f, r * 0.27f, P1);
        P1.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // BALLES — METAL
    // ──────────────────────────────────────────────────────────────────────
    private static void drawMetalBall(Canvas c, float cx, float cy, float r,
                                      String id, int color) {
        // Base
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(color);
        c.drawCircle(cx, cy, r, P1);

        // Zone claire haut-droite (simulation reflet métallique)
        P1.setColor(Color.WHITE);
        P1.setAlpha(55);
        c.drawCircle(cx + r * 0.14f, cy - r * 0.17f, r * 0.70f, P1);
        P1.setAlpha(255);

        // Arc spéculaire brillant
        P2.setStyle(Paint.Style.STROKE);
        P2.setColor(Color.WHITE);
        P2.setAlpha(180);
        P2.setStrokeWidth(r * 0.11f);
        P2.setStrokeCap(Paint.Cap.ROUND);
        RECT.set(cx - r * 0.58f, cy - r * 0.58f, cx + r * 0.58f, cy + r * 0.58f);
        c.drawArc(RECT, 210f, 65f, false, P2);
        P2.setAlpha(255);

        // Damas : stries sombres
        if ("ball_damascus".equals(id)) {
            P2.setStyle(Paint.Style.STROKE);
            P2.setColor(Color.parseColor("#111111"));
            P2.setAlpha(90);
            P2.setStrokeWidth(r * 0.09f);
            RECT.set(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f);
            c.drawArc(RECT, 0f,   200f, false, P2);
            RECT.set(cx - r * 0.32f, cy - r * 0.32f, cx + r * 0.32f, cy + r * 0.32f);
            c.drawArc(RECT, 40f,  190f, false, P2);
            P2.setAlpha(255);
        }
        // Bismuth : teintes irisées
        if ("ball_bismuth".equals(id)) {
            int[] iridescent = {0x55FF88CC, 0x5588FFCC, 0x55CCFF88};
            for (int i = 0; i < 3; i++) {
                P1.setColor(iridescent[i]);
                c.drawCircle(cx + (i - 1) * r * 0.25f, cy + (i - 1) * r * 0.15f, r * 0.35f, P1);
            }
        }
        // Contour fin
        P2.setStyle(Paint.Style.STROKE);
        P2.setColor(Color.parseColor("#1A2A3A"));
        P2.setAlpha(80);
        P2.setStrokeWidth(r * 0.06f);
        c.drawCircle(cx, cy, r - r * 0.03f, P2);
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // BALLES — SPACE
    // ──────────────────────────────────────────────────────────────────────
    private static void drawSpaceBall(Canvas c, float cx, float cy, float r,
                                      String id, int color) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(color);
        c.drawCircle(cx, cy, r, P1);

        boolean isPlanet = isPlanetId(id);

        if (isPlanet) {
            // Bandes atmosphériques simulées
            int bandColor = lighten(color, 55);
            P1.setColor(bandColor);
            P1.setAlpha(80);
            c.drawCircle(cx - r * 0.1f, cy - r * 0.15f, r * 0.75f, P1);
            P1.setAlpha(255);

            // Anneau pour Saturn
            if ("ball_saturn".equals(id)) {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#C8A96E"));
                P2.setAlpha(200);
                P2.setStrokeWidth(r * 0.18f);
                RECT.set(cx - r * 0.95f, cy - r * 0.28f, cx + r * 0.95f, cy + r * 0.28f);
                c.drawOval(RECT, P2);
                P2.setAlpha(255);
            }
        } else {
            // Lueur centrale (étoiles, nébuleuses, trous noirs)
            int glowColor = "ball_black_hole".equals(id)
                    ? Color.parseColor("#330066")
                    : lighten(color, 75);
            P1.setColor(glowColor);
            P1.setAlpha(145);
            c.drawCircle(cx, cy, r * 0.42f, P1);
            P1.setAlpha(255);

            // Étoiles de fond
            P1.setColor(Color.WHITE);
            P1.setAlpha(210);
            c.drawCircle(cx + r * 0.50f, cy - r * 0.40f, r * 0.05f, P1);
            c.drawCircle(cx - r * 0.52f, cy + r * 0.30f, r * 0.04f, P1);
            c.drawCircle(cx + r * 0.12f, cy + r * 0.54f, r * 0.04f, P1);
            P1.setAlpha(255);

            // Pulsar : rayons
            if ("ball_pulsar".equals(id)) {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#88CCFF"));
                P2.setAlpha(200);
                P2.setStrokeWidth(r * 0.08f);
                float len = r * 0.85f;
                c.drawLine(cx - len, cy, cx + len, cy, P2);
                c.drawLine(cx, cy - len, cx, cy + len, P2);
                P2.setAlpha(255);
            }
        }

        // Specular
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(Color.WHITE);
        P1.setAlpha(55);
        c.drawCircle(cx - r * 0.25f, cy - r * 0.28f, r * 0.20f, P1);
        P1.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // BALLES — ELEMENTAL
    // ──────────────────────────────────────────────────────────────────────
    private static void drawElementalBall(Canvas c, float cx, float cy, float r,
                                          String id, int color) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(color);
        c.drawCircle(cx, cy, r, P1);

        switch (id) {
            case "ball_elem_fire": {
                // Cœur jaune-orange
                P1.setColor(Color.parseColor("#FFCC00"));
                P1.setAlpha(170);
                c.drawCircle(cx, cy + r * 0.1f, r * 0.42f, P1);
                P1.setColor(Color.WHITE);
                P1.setAlpha(100);
                c.drawCircle(cx, cy + r * 0.15f, r * 0.2f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_elem_water": {
                P1.setColor(Color.parseColor("#AADDFF"));
                P1.setAlpha(140);
                c.drawCircle(cx, cy, r * 0.62f, P1);
                P1.setAlpha(255);
                // Vague
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.WHITE);
                P2.setAlpha(140);
                P2.setStrokeWidth(r * 0.10f);
                P2.setStrokeCap(Paint.Cap.ROUND);
                RECT.set(cx - r * 0.45f, cy - r * 0.45f, cx + r * 0.45f, cy + r * 0.45f);
                c.drawArc(RECT, 0f, 180f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_elem_lightning": {
                // Éclair en zigzag
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.WHITE);
                P2.setAlpha(230);
                P2.setStrokeWidth(r * 0.13f);
                P2.setStrokeJoin(Paint.Join.ROUND);
                P2.setStrokeCap(Paint.Cap.ROUND);
                PATH.reset();
                PATH.moveTo(cx + r * 0.08f, cy - r * 0.52f);
                PATH.lineTo(cx - r * 0.12f, cy - r * 0.02f);
                PATH.lineTo(cx + r * 0.14f, cy + r * 0.02f);
                PATH.lineTo(cx - r * 0.06f, cy + r * 0.52f);
                c.drawPath(PATH, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_elem_ice": {
                // Flocon (6 branches)
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.WHITE);
                P2.setAlpha(210);
                P2.setStrokeWidth(r * 0.09f);
                P2.setStrokeCap(Paint.Cap.ROUND);
                float len = r * 0.48f;
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3;
                    c.drawLine(cx, cy,
                            cx + len * (float) Math.cos(angle),
                            cy + len * (float) Math.sin(angle), P2);
                }
                P1.setStyle(Paint.Style.FILL);
                P1.setColor(Color.WHITE);
                P1.setAlpha(180);
                c.drawCircle(cx, cy, r * 0.12f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_elem_earth": {
                // Taches vertes
                P1.setColor(Color.parseColor("#3A8C1A"));
                P1.setAlpha(200);
                c.drawCircle(cx - r * 0.15f, cy - r * 0.1f, r * 0.3f, P1);
                c.drawCircle(cx + r * 0.25f, cy + r * 0.2f, r * 0.22f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_elem_darkness": {
                // Anneau sombre
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#220022"));
                P2.setAlpha(255);
                P2.setStrokeWidth(r * 0.18f);
                c.drawCircle(cx, cy, r * 0.52f, P2);
                break;
            }
            case "ball_elem_light": {
                P1.setColor(Color.parseColor("#FFFFCC"));
                P1.setAlpha(200);
                c.drawCircle(cx, cy, r * 0.65f, P1);
                P1.setColor(Color.WHITE);
                P1.setAlpha(240);
                c.drawCircle(cx, cy, r * 0.30f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_elem_plasma": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#FF88FF"));
                P2.setAlpha(200);
                P2.setStrokeWidth(r * 0.11f);
                RECT.set(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f);
                c.drawArc(RECT, 30f,  200f, false, P2);
                RECT.set(cx - r * 0.32f, cy - r * 0.32f, cx + r * 0.32f, cy + r * 0.32f);
                c.drawArc(RECT, 210f, 200f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_elem_lava": {
                P1.setColor(Color.parseColor("#1A0000"));
                P1.setAlpha(180);
                c.drawCircle(cx + r * 0.1f, cy + r * 0.2f, r * 0.35f, P1);
                c.drawCircle(cx - r * 0.3f, cy - r * 0.15f, r * 0.22f, P1);
                P1.setColor(Color.parseColor("#FF8800"));
                P1.setAlpha(130);
                c.drawCircle(cx, cy + r * 0.25f, r * 0.15f, P1);
                P1.setAlpha(255);
                break;
            }
            default: {
                // Air + générique : lueur intérieure
                P1.setColor(lighten(color, 80));
                P1.setAlpha(130);
                c.drawCircle(cx, cy, r * 0.52f, P1);
                P1.setAlpha(255);
                break;
            }
        }
        // Specular commun
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(Color.WHITE);
        P1.setAlpha(70);
        c.drawCircle(cx - r * 0.25f, cy - r * 0.28f, r * 0.22f, P1);
        P1.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // BALLES — SPORT
    // ──────────────────────────────────────────────────────────────────────
    private static void drawSportBall(Canvas c, float cx, float cy, float r,
                                      String id, int color) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(color);
        c.drawCircle(cx, cy, r, P1);

        switch (id) {
            case "ball_soccer": {
                P1.setColor(Color.parseColor("#111111"));
                P1.setAlpha(210);
                c.drawCircle(cx,           cy,           r * 0.22f, P1);
                c.drawCircle(cx + r*0.55f, cy - r*0.35f, r * 0.14f, P1);
                c.drawCircle(cx - r*0.55f, cy - r*0.28f, r * 0.13f, P1);
                c.drawCircle(cx + r*0.20f, cy + r*0.58f, r * 0.13f, P1);
                c.drawCircle(cx - r*0.35f, cy + r*0.53f, r * 0.13f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_basket": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#5C2800"));
                P2.setStrokeWidth(r * 0.10f);
                P2.setAlpha(210);
                c.drawLine(cx - r, cy, cx + r, cy, P2);
                RECT.set(cx - r*0.7f, cy - r*0.7f, cx + r*0.7f, cy + r*0.7f);
                c.drawArc(RECT, 180f, 180f, false, P2);
                c.drawArc(RECT,   0f, 180f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_tennis": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.WHITE);
                P2.setStrokeWidth(r * 0.14f);
                P2.setAlpha(230);
                P2.setStrokeCap(Paint.Cap.ROUND);
                RECT.set(cx - r*0.58f, cy - r*0.58f, cx + r*0.58f, cy + r*0.58f);
                c.drawArc(RECT,  30f, 118f, false, P2);
                c.drawArc(RECT, 210f, 118f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_bowling": {
                P1.setColor(Color.parseColor("#1A0066"));
                P1.setAlpha(230);
                c.drawCircle(cx,          cy - r*0.3f, r * 0.13f, P1);
                c.drawCircle(cx - r*0.28f, cy + r*0.1f, r * 0.12f, P1);
                c.drawCircle(cx + r*0.28f, cy + r*0.1f, r * 0.12f, P1);
                P1.setAlpha(255);
                break;
            }
            case "ball_8ball": {
                P1.setColor(Color.WHITE);
                c.drawCircle(cx, cy, r * 0.42f, P1);
                P1.setColor(Color.parseColor("#111111"));
                c.drawCircle(cx, cy - r*0.14f, r * 0.13f, P1);
                c.drawCircle(cx, cy + r*0.14f, r * 0.14f, P1);
                break;
            }
            case "ball_cateye": {
                P1.setStyle(Paint.Style.FILL);
                P1.setColor(Color.parseColor("#111111"));
                RECT.set(cx - r * 0.13f, cy - r * 0.55f, cx + r * 0.13f, cy + r * 0.55f);
                c.drawOval(RECT, P1);
                break;
            }
            case "ball_baseball": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#CC3322"));
                P2.setStrokeWidth(r * 0.09f);
                P2.setAlpha(200);
                RECT.set(cx - r*0.55f, cy - r*0.55f, cx + r*0.55f, cy + r*0.55f);
                c.drawArc(RECT,  20f, 140f, false, P2);
                c.drawArc(RECT, 200f, 140f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_volleyball": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#AAAAAA"));
                P2.setStrokeWidth(r * 0.08f);
                P2.setAlpha(190);
                c.drawLine(cx - r, cy, cx + r, cy, P2);
                RECT.set(cx - r*0.65f, cy - r*0.65f, cx + r*0.65f, cy + r*0.65f);
                c.drawArc(RECT, 270f, 120f, false, P2);
                c.drawArc(RECT,  30f, 120f, false, P2);
                P2.setAlpha(255);
                break;
            }
            case "ball_beach": {
                int[] stripes = {0xFFFF4444, 0xFF4488FF, 0xFF44CC44};
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.20f);
                for (int s = 0; s < 3; s++) {
                    P2.setColor(stripes[s]);
                    P2.setAlpha(190);
                    RECT.set(cx - r*0.6f, cy - r*0.6f, cx + r*0.6f, cy + r*0.6f);
                    c.drawArc(RECT, 60f + s * 60f, 50f, false, P2);
                }
                P2.setAlpha(255);
                break;
            }
            case "ball_petanque": {
                // Boule de pétanque : stries parallèles horizontales
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(lighten(color, -30));
                P2.setAlpha(160);
                P2.setStrokeWidth(r * 0.08f);
                for (int row = -2; row <= 2; row++) {
                    float y0 = cy + row * r * 0.32f;
                    float halfW = (float) Math.sqrt(Math.max(0, r*r - (y0-cy)*(y0-cy))) * 0.85f;
                    if (halfW > r * 0.1f) c.drawLine(cx - halfW, y0, cx + halfW, y0, P2);
                }
                P2.setAlpha(255);
                break;
            }
            default: {
                // golf, generics
                P1.setColor(Color.WHITE);
                P1.setAlpha(85);
                c.drawCircle(cx - r*0.25f, cy - r*0.30f, r * 0.26f, P1);
                P1.setAlpha(255);
                break;
            }
        }
        // Liseré extérieur fin
        P2.setStyle(Paint.Style.STROKE);
        P2.setColor(Color.WHITE);
        P2.setAlpha(35);
        P2.setStrokeWidth(r * 0.07f);
        c.drawCircle(cx, cy, r - r * 0.035f, P2);
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // BACKGROUNDS (clippé en cercle)
    // ──────────────────────────────────────────────────────────────────────
    private static void drawBg(Canvas c, float cx, float cy, float r, String id) {
        // Sauvegarder l'état, clipper au cercle, dessiner, restaurer
        int saved = c.save();
        PATH.reset();
        PATH.addCircle(cx, cy, r, Path.Direction.CW);
        c.clipPath(PATH);

        if (id.startsWith("bg_grad_")) {
            drawBgGradient(c, cx, cy, r, id);
        } else if (id.startsWith("bg_minimal_")) {
            drawBgMinimal(c, cx, cy, r, id);
        } else if (id.startsWith("bg_urban_")) {
            drawBgUrban(c, cx, cy, r, id);
        } else {
            // bg_default
            P1.setStyle(Paint.Style.FILL);
            P1.setColor(Color.parseColor("#F5F5F0"));
            c.drawRect(cx - r, cy - r, cx + r, cy + r, P1);
        }

        c.restoreToCount(saved);

        // Contour léger
        P2.setStyle(Paint.Style.STROKE);
        P2.setColor(Color.parseColor("#2A3A4A"));
        P2.setAlpha(100);
        P2.setStrokeWidth(r * 0.06f);
        c.drawCircle(cx, cy, r - r * 0.03f, P2);
        P2.setAlpha(255);
    }

    private static void drawBgGradient(Canvas c, float cx, float cy, float r, String id) {
        int[] cols = getBgGradientColors(id);
        // 14 bandes horizontales interpolées = aspect dégradé sans Shader
        float top = cy - r;
        float bandH = (r * 2f) / 14f;
        P1.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 14; i++) {
            P1.setColor(interpolateColor(cols[0], cols[1], i / 13f));
            c.drawRect(cx - r, top + i * bandH, cx + r, top + (i + 1) * bandH, P1);
        }
    }

    private static void drawBgMinimal(Canvas c, float cx, float cy, float r, String id) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(getBgBaseColor(id));
        c.drawRect(cx - r, cy - r, cx + r, cy + r, P1);

        P2.setColor(getBgPatternColor(id));
        P2.setAlpha(185);
        float s = r * 0.30f;

        switch (id) {
            case "bg_minimal_circles": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                c.drawCircle(cx - r*0.40f, cy,          s,        P2);
                c.drawCircle(cx + r*0.42f, cy - r*0.32f, s * 0.7f, P2);
                c.drawCircle(cx + r*0.05f, cy + r*0.52f, s * 0.5f, P2);
                break;
            }
            case "bg_minimal_lines": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                for (float ox = -r * 0.8f; ox < r; ox += r * 0.38f) {
                    c.drawLine(cx + ox, cy - r, cx + ox + r * 0.35f, cy + r, P2);
                }
                break;
            }
            case "bg_minimal_dots": {
                P2.setStyle(Paint.Style.FILL);
                float dot = r * 0.07f;
                for (int row = -2; row <= 2; row++)
                    for (int col = -2; col <= 2; col++)
                        c.drawCircle(cx + col * r * 0.38f, cy + row * r * 0.38f, dot, P2);
                break;
            }
            case "bg_minimal_hexagons": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                drawHexagon(c, cx - r*0.3f, cy - r*0.22f, s * 0.88f, P2);
                drawHexagon(c, cx + r*0.4f, cy + r*0.32f, s * 0.65f, P2);
                break;
            }
            default: { // triangles
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(r * 0.07f);
                drawTriangle(c, cx - r*0.30f, cy - r*0.22f, s,       P2);
                drawTriangle(c, cx + r*0.32f, cy + r*0.32f, s * 0.7f, P2);
                break;
            }
        }
        P2.setAlpha(255);
    }

    private static void drawBgUrban(Canvas c, float cx, float cy, float r, String id) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(getBgBaseColor(id));
        c.drawRect(cx - r, cy - r, cx + r, cy + r, P1);

        P2.setColor(getBgPatternColor(id));
        P2.setAlpha(205);
        float lw = r * 0.08f;

        switch (id) {
            case "bg_urban_rain": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(lw * 0.7f);
                for (float ox = -r * 0.75f; ox <= r * 0.75f; ox += r * 0.32f)
                    c.drawLine(cx + ox, cy - r, cx + ox - r * 0.1f, cy + r, P2);
                break;
            }
            case "bg_urban_circuit": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(lw);
                c.drawLine(cx - r, cy - r*0.28f, cx + r, cy - r*0.28f, P2);
                c.drawLine(cx - r, cy + r*0.28f, cx + r, cy + r*0.28f, P2);
                c.drawLine(cx - r*0.4f, cy - r*0.28f, cx - r*0.4f, cy + r*0.28f, P2);
                c.drawLine(cx + r*0.4f, cy - r*0.28f, cx + r*0.4f, cy + r*0.28f, P2);
                P1.setStyle(Paint.Style.FILL);
                P1.setColor(getBgPatternColor(id));
                c.drawCircle(cx - r*0.4f, cy, lw * 1.6f, P1);
                c.drawCircle(cx + r*0.4f, cy, lw * 1.6f, P1);
                break;
            }
            case "bg_urban_grid": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(lw * 0.7f);
                for (float d = -r * 0.65f; d <= r * 0.7f; d += r * 0.33f) {
                    c.drawLine(cx - r, cy + d, cx + r, cy + d, P2);
                    c.drawLine(cx + d, cy - r, cx + d, cy + r, P2);
                }
                break;
            }
            case "bg_urban_tunnel": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setStrokeWidth(lw * 0.8f);
                for (float sc = 0.88f; sc > 0.12f; sc -= 0.26f) {
                    RECT.set(cx - r*sc, cy - r*sc*0.7f, cx + r*sc, cy + r*sc*0.7f);
                    c.drawRect(RECT, P2);
                }
                break;
            }
            case "bg_urban_equalizer": {
                P2.setStyle(Paint.Style.FILL);
                float bw = r * 0.22f;
                float[] heights = {0.50f, 0.82f, 0.38f, 0.68f};
                float baseY = cy + r * 0.70f;
                for (int b = 0; b < 4; b++) {
                    float bx = cx - r * 0.55f + b * (bw + r * 0.08f);
                    float bh = r * heights[b] * 1.45f;
                    RECT.set(bx, baseY - bh, bx + bw, baseY);
                    c.drawRect(RECT, P2);
                }
                break;
            }
        }
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // EFFETS
    // ──────────────────────────────────────────────────────────────────────
    private static void drawFx(Canvas c, float cx, float cy, float r, String id) {
        P1.setStyle(Paint.Style.FILL);
        P1.setColor(Color.parseColor("#1A1A2A"));
        c.drawCircle(cx, cy, r, P1);

        switch (id) {
            case "fx_fire": {
                int[] fC = {0xFFFF8800, 0xFFFF4400, 0xFFFFCC00};
                for (int i = 0; i < 3; i++) {
                    P2.setStyle(Paint.Style.STROKE);
                    P2.setColor(fC[i]);
                    P2.setAlpha(210 - i * 40);
                    P2.setStrokeWidth(r * (0.17f - i * 0.03f));
                    P2.setStrokeCap(Paint.Cap.ROUND);
                    float sc = 0.55f - i * 0.1f;
                    RECT.set(cx - r*sc, cy - r*sc, cx + r*sc, cy + r*sc);
                    c.drawArc(RECT, 120f + i * 12f, 115f - i * 18f, false, P2);
                }
                P2.setAlpha(255);
                break;
            }
            case "fx_ice": {
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#80DEEA"));
                P2.setAlpha(220);
                P2.setStrokeWidth(r * 0.10f);
                P2.setStrokeCap(Paint.Cap.ROUND);
                float len = r * 0.46f;
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3;
                    c.drawLine(cx, cy,
                            cx + len * (float) Math.cos(angle),
                            cy + len * (float) Math.sin(angle), P2);
                }
                P1.setColor(Color.WHITE);
                P1.setAlpha(200);
                c.drawCircle(cx, cy, r * 0.11f, P1);
                P1.setAlpha(255);
                P2.setAlpha(255);
                break;
            }
            case "fx_rainbow": {
                String[] rc = {"#FF0000","#FF8800","#FFEE00","#00CC00","#0066FF","#9900CC"};
                for (int i = 0; i < rc.length; i++) {
                    P2.setStyle(Paint.Style.STROKE);
                    P2.setColor(Color.parseColor(rc[i]));
                    P2.setAlpha(225);
                    P2.setStrokeWidth(r * 0.13f);
                    float sc = 0.88f - i * 0.11f;
                    RECT.set(cx - r*sc, cy - r*sc, cx + r*sc, cy + r*sc);
                    c.drawArc(RECT, 180f, 180f, false, P2);
                }
                P2.setAlpha(255);
                break;
            }
            case "fx_stars": {
                P1.setColor(Color.parseColor("#FFD700"));
                P1.setAlpha(240);
                drawStar(c, cx,          cy - r*0.28f, r*0.16f);
                P1.setAlpha(190);
                drawStar(c, cx + r*0.45f, cy + r*0.32f, r*0.10f);
                drawStar(c, cx - r*0.42f, cy + r*0.28f, r*0.09f);
                P1.setAlpha(255);
                break;
            }
            default: { // fx_none
                P2.setStyle(Paint.Style.STROKE);
                P2.setColor(Color.parseColor("#555566"));
                P2.setAlpha(160);
                P2.setStrokeWidth(r * 0.09f);
                c.drawCircle(cx, cy, r * 0.48f, P2);
                P2.setAlpha(255);
                break;
            }
        }
        // Liseré
        P2.setStyle(Paint.Style.STROKE);
        P2.setColor(Color.parseColor("#3A4A5A"));
        P2.setAlpha(120);
        P2.setStrokeWidth(r * 0.07f);
        c.drawCircle(cx, cy, r - r * 0.035f, P2);
        P2.setAlpha(255);
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS GÉOMÉTRIQUES
    // ──────────────────────────────────────────────────────────────────────
    private static void drawHexagon(Canvas c, float cx, float cy, float r, Paint paint) {
        PATH.reset();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 6.0 + i * Math.PI / 3.0;
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) PATH.moveTo(x, y); else PATH.lineTo(x, y);
        }
        PATH.close();
        c.drawPath(PATH, paint);
    }

    private static void drawTriangle(Canvas c, float cx, float cy, float r, Paint paint) {
        PATH.reset();
        PATH.moveTo(cx, cy - r);
        PATH.lineTo(cx + r * 0.866f, cy + r * 0.5f);
        PATH.lineTo(cx - r * 0.866f, cy + r * 0.5f);
        PATH.close();
        c.drawPath(PATH, paint);
    }

    private static void drawStar(Canvas c, float cx, float cy, float r) {
        PATH.reset();
        for (int i = 0; i < 5; i++) {
            double outerAngle = -Math.PI / 2 + i * 2 * Math.PI / 5;
            double innerAngle = outerAngle + Math.PI / 5;
            float ox = cx + r * (float) Math.cos(outerAngle);
            float oy = cy + r * (float) Math.sin(outerAngle);
            float ix = cx + r * 0.42f * (float) Math.cos(innerAngle);
            float iy = cy + r * 0.42f * (float) Math.sin(innerAngle);
            if (i == 0) PATH.moveTo(ox, oy); else PATH.lineTo(ox, oy);
            PATH.lineTo(ix, iy);
        }
        PATH.close();
        c.drawPath(PATH, P1);
    }

    // ──────────────────────────────────────────────────────────────────────
    // HELPERS DONNÉES
    // ──────────────────────────────────────────────────────────────────────
    private static String getBallCategory(String id) {
        switch (id) {
            case "ball_lead": case "ball_nickel": case "ball_copper":
            case "ball_chrome": case "ball_bronze": case "ball_steel":
            case "ball_silver": case "ball_gold": case "ball_rosegold":
            case "ball_titanium": case "ball_platinum": case "ball_bismuth":
            case "ball_damascus": case "ball_meteorite":
                return "metal";
            case "ball_void": case "ball_nebula": case "ball_comet":
            case "ball_mercury": case "ball_venus": case "ball_earth":
            case "ball_moon": case "ball_mars": case "ball_jupiter":
            case "ball_saturn": case "ball_uranus": case "ball_neptune":
            case "ball_pluto": case "ball_red_dwarf": case "ball_yellow_dwarf":
            case "ball_blue_giant": case "ball_black_hole": case "ball_pulsar":
                return "space";
            case "ball_soccer": case "ball_basket": case "ball_tennis":
            case "ball_bowling": case "ball_petanque": case "ball_golf":
            case "ball_cateye": case "ball_beach": case "ball_volleyball":
            case "ball_baseball": case "ball_8ball":
                return "sport";
            default:
                return "classic";
        }
    }

    private static boolean isPlanetId(String id) {
        switch (id) {
            case "ball_mercury": case "ball_venus": case "ball_earth":
            case "ball_moon": case "ball_mars": case "ball_jupiter":
            case "ball_saturn": case "ball_uranus": case "ball_neptune":
            case "ball_pluto":
                return true;
            default:
                return false;
        }
    }

    private static int[] getBgGradientColors(String id) {
        switch (id) {
            case "bg_grad_rainbow":  return new int[]{0xFFDD0040, 0xFF0050DD};
            case "bg_grad_sunset":   return new int[]{0xFFEE4400, 0xFFFFAA00};
            case "bg_grad_ocean":    return new int[]{0xFF0033AA, 0xFF00AADD};
            case "bg_grad_forest":   return new int[]{0xFF226622, 0xFF88CC44};
            case "bg_grad_aurora":   return new int[]{0xFF003322, 0xFF00CC66};
            case "bg_grad_candy":    return new int[]{0xFFFF3388, 0xFFFFBBDD};
            case "bg_grad_volcano":  return new int[]{0xFF880000, 0xFFFF4400};
            case "bg_grad_galaxy":   return new int[]{0xFF000022, 0xFF4400AA};
            case "bg_grad_toxic":    return new int[]{0xFF004400, 0xFF33DD00};
            default:                 return new int[]{0xFF222222, 0xFF444444};
        }
    }

    private static int getBgBaseColor(String id) {
        switch (id) {
            case "bg_minimal_triangles": return 0xFF1E3836;
            case "bg_minimal_circles":   return 0xFF1A1E28;
            case "bg_minimal_lines":     return 0xFF0E1520;
            case "bg_minimal_dots":      return 0xFF180C12;
            case "bg_minimal_hexagons":  return 0xFF1A1408;
            case "bg_urban_rain":        return 0xFF05050F;
            case "bg_urban_circuit":     return 0xFF020D05;
            case "bg_urban_grid":        return 0xFF060010;
            case "bg_urban_tunnel":      return 0xFF020008;
            case "bg_urban_equalizer":   return 0xFF050505;
            default:                     return 0xFF1A2A3A;
        }
    }

    private static int getBgPatternColor(String id) {
        switch (id) {
            case "bg_minimal_triangles": return 0xFF4AADA8;
            case "bg_minimal_circles":   return 0xFF4A5568;
            case "bg_minimal_lines":     return 0xFF2A3550;
            case "bg_minimal_dots":      return 0xFF9D4F6E;
            case "bg_minimal_hexagons":  return 0xFF7A6035;
            case "bg_urban_rain":        return 0xFF2244AA;
            case "bg_urban_circuit":     return 0xFF00AA44;
            case "bg_urban_grid":        return 0xFF6600CC;
            case "bg_urban_tunnel":      return 0xFF8800AA;
            case "bg_urban_equalizer":   return 0xFF00AAFF;
            default:                     return 0xFF4A8ADA;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // UTILITAIRES COULEUR
    // ──────────────────────────────────────────────────────────────────────
    private static int interpolateColor(int c1, int c2, float t) {
        return Color.argb(
                (int) (Color.alpha(c1) + t * (Color.alpha(c2) - Color.alpha(c1))),
                (int) (Color.red(c1)   + t * (Color.red(c2)   - Color.red(c1))),
                (int) (Color.green(c1) + t * (Color.green(c2) - Color.green(c1))),
                (int) (Color.blue(c1)  + t * (Color.blue(c2)  - Color.blue(c1))));
    }

    private static int lighten(int color, int amount) {
        return Color.argb(
                Color.alpha(color),
                Math.min(255, Math.max(0, Color.red(color)   + amount)),
                Math.min(255, Math.max(0, Color.green(color) + amount)),
                Math.min(255, Math.max(0, Color.blue(color)  + amount)));
    }

    private static int parseColor(String hex, int fallback) {
        try { return Color.parseColor(hex); }
        catch (Exception e) { return fallback; }
    }
}
