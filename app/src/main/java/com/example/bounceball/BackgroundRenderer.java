package com.example.bounceball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Matrix;
import java.util.ArrayList;

public class BackgroundRenderer {

    private static final int BG_COLOR_TRIANGLES = 0xFF6AADAA;
    private static final int BG_COLOR_CIRCLES   = 0xFF4A5568;
    private static final int BG_COLOR_LINES     = 0xFF2A3550;
    private static final int BG_COLOR_DOTS      = 0xFF3D1F2A;
    private static final int BG_COLOR_HEXAGONS  = 0xFF3A2D1A;
    private static final int BG_COLOR_RAIN      = 0xFF12203A;
    private static final int BG_COLOR_CIRCUIT   = 0xFF020D05;
    private static final int BG_COLOR_GRID      = 0xFF060010;

    private final Context context;
    private int screenWidth;
    private int screenHeight;

    private String currentBgSkin = "bg_default";
    private float  bgParallaxY   = 0f;
    private volatile String pendingSkinId = null;

    // ── bg_minimal_triangles ──
    private float[][] bgTriangles = null;

    // ── bg_minimal_circles ──
    // Chaque cercle : {cx, cy_base, maxR, growSpeed, currentR, alpha}
    private ArrayList<float[]> bgCircleList = null;
    private long lastCircleSpawn = 0;

    // ── bg_minimal_lines ──
    // Groupes de lignes : chaque groupe = {offsetX, speed, alpha, nbLines, spacing}
    private ArrayList<float[]> bgLineGroups = null;
    private long lastLineSpawn = 0;

    // ── bg_minimal_dots ──
    // Ondulations : {cx, cy_base, radius, maxRadius, alpha}
    private ArrayList<float[]> bgRipples = null;
    private long lastRippleSpawn = 0;

    // ── bg_minimal_hexagons ──
    // Vague qui traverse la grille : {progressX} de 0 à screenWidth
    private float bgHexWaveX    = 0f;
    private float bgHexWaveDir  = 1f;
    private float[] bgHexPhases = null;

    // ── bg_urban_rain ──
    private float[][] bgRainDrops     = null;
    private float[]   bgNeonGlowData  = null;
    private Paint[]          bgNeonGlowPaints  = null;
    private RadialGradient[] bgNeonGlowShaders = null;
    private final Matrix bgGlowMatrix = new Matrix();
    private final Paint  bgRainPWide  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  bgRainPMid   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  bgRainPCore  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[]  bgNeonColors = {
            0xFF00E5FF, 0xFF00B8D4, 0xFF40C4FF, 0xFF0091EA,
            0xFFFF4081, 0xFFFF80AB, 0xFFEA80FC, 0xFFCE93D8,
    };

    // ── bg_urban_circuit ──
    private float[][] bgCircuitNodes = null;
    private float[][] bgCircuitPulses = null;

// ── bg_urban_grid ──
// entièrement calculé, pas d'état persistant

    public BackgroundRenderer(Context context, int screenWidth, int screenHeight) {
        this.context      = context;
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        bgRainPWide.setStyle(Paint.Style.STROKE);
        bgRainPWide.setStrokeCap(Paint.Cap.BUTT);
        bgRainPMid.setStyle(Paint.Style.STROKE);
        bgRainPMid.setStrokeCap(Paint.Cap.BUTT);
        bgRainPCore.setStyle(Paint.Style.STROKE);
        bgRainPCore.setStrokeCap(Paint.Cap.BUTT);
    }

    public void onScreenSizeChanged(int w, int h) {
        screenWidth  = w;
        screenHeight = h;
        resetAllState();
    }

    public void loadBgSkin(String skinId) {
        pendingSkinId = skinId;
    }

    private void resetAllState() {
        bgTriangles   = null;
        bgCircleList  = null;
        bgLineGroups  = null;
        bgRipples     = null;
        bgHexPhases   = null;
        bgHexWaveX    = 0f;
        bgHexWaveDir  = 1f;
        bgRainDrops    = null;
        bgCircuitNodes = null;
        bgCircuitPulses = null;
        bgNeonGlowData   = null;
        bgNeonGlowPaints  = null;
        bgNeonGlowShaders = null;
    }

    public void updateParallax(float cameraShift) {
        bgParallaxY += cameraShift * 1.8f;
    }

    public void draw(Canvas canvas) {
        String pending = pendingSkinId;
        if (pending != null) {
            pendingSkinId = null;
            currentBgSkin = pending;
            bgParallaxY   = 0f;
            resetAllState();
        }
        switch (currentBgSkin) {
            case "bg_minimal_triangles": drawBgMinimalTriangles(canvas); break;
            case "bg_minimal_circles":   drawBgMinimalCircles(canvas);   break;
            case "bg_minimal_lines":     drawBgMinimalLines(canvas);     break;
            case "bg_minimal_dots":      drawBgMinimalDots(canvas);      break;
            case "bg_minimal_hexagons":  drawBgMinimalHexagons(canvas);  break;
            case "bg_urban_rain":    drawBgUrbanRain(canvas);    break;
            case "bg_urban_circuit": drawBgUrbanCircuit(canvas); break;
            case "bg_urban_grid":    drawBgUrbanGrid(canvas);    break;
            default:
                canvas.drawColor(Color.WHITE);
                break;
        }
    }

    // ══════════════════════════════════════════════════
    // MINIMALISTE — Triangles
    // Triangles équilatéraux blancs qui traversent
    // l'écran horizontalement en tournant sur eux-mêmes
    // ══════════════════════════════════════════════════

    private void drawBgMinimalTriangles(Canvas canvas) {
        canvas.drawColor(BG_COLOR_TRIANGLES);
        if (bgTriangles == null) initTriangles();

        Paint triPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        triPaint.setStyle(Paint.Style.FILL);
        triPaint.setColor(Color.WHITE);

        for (float[] tri : bgTriangles) {
            tri[0] += tri[5];
            tri[4] += tri[3];

            float drawY = (tri[1] + bgParallaxY) % (screenHeight + tri[2] * 2);
            if (drawY < -tri[2]) drawY += screenHeight + tri[2] * 2;

            float dir = tri[6];
            if (dir > 0 && tri[0] >  screenWidth  + tri[2]) tri[0] = -tri[2];
            if (dir < 0 && tri[0] < -tri[2])                tri[0] =  screenWidth + tri[2];

            float sz     = tri[2];
            float rotRad = (float) Math.toRadians(tri[4]);
            float cosA   = (float) Math.cos(rotRad);
            float sinA   = (float) Math.sin(rotRad);

            float h = sz * 0.866f;
            float[] vx = {  0f,        -sz * 0.5f,  sz * 0.5f };
            float[] vy = { -h * 0.667f, h * 0.333f, h * 0.333f };

            Path triPath = new Path();
            for (int v = 0; v < 3; v++) {
                float rx = vx[v] * cosA - vy[v] * sinA + tri[0];
                float ry = vx[v] * sinA + vy[v] * cosA + drawY;
                if (v == 0) triPath.moveTo(rx, ry);
                else        triPath.lineTo(rx, ry);
            }
            triPath.close();

            float ratio = sz / (screenWidth * 0.10f);
            triPaint.setAlpha((int) Math.min(120 + ratio * 80f, 220));
            canvas.drawPath(triPath, triPaint);
        }
    }

    private void initTriangles() {
        int count = 7;
        bgTriangles = new float[count][7];
        for (int i = 0; i < count; i++) {
            float dir  = (Math.random() > 0.5) ? 1f : -1f;
            float size = (float)(screenWidth * 0.04f + Math.random() * screenWidth * 0.06f);
            bgTriangles[i][0] = dir > 0 ? -size : screenWidth + size;
            bgTriangles[i][1] = (float)(Math.random() * screenHeight);
            bgTriangles[i][2] = size;
            bgTriangles[i][3] = (float)(0.3f + Math.random() * 1.2f) * dir;
            bgTriangles[i][4] = (float)(Math.random() * 360f);
            bgTriangles[i][5] = (float)(1.5f + Math.random() * 3.0f) * dir;
            bgTriangles[i][6] = dir;
        }
    }

    // ══════════════════════════════════════════════════
    // MINIMALISTE — Cercles
    // Fond gris ardoise. Des anneaux blancs "pop"
    // aléatoirement, grandissent et s'évaporent en
    // fondu tout en continuant de s'agrandir.
    // Apparaissent seuls ou par 2-3 simultanément.
    // ══════════════════════════════════════════════════

    private void drawBgMinimalCircles(Canvas canvas) {
        canvas.drawColor(BG_COLOR_CIRCLES);

        if (bgCircleList == null) bgCircleList = new ArrayList<>();

        long now = System.currentTimeMillis();

        // Spawn : toutes les 1.2–2.5s, 1 à 3 cercles d'un coup
        if (now - lastCircleSpawn > 1200 + (long)(Math.random() * 1300)) {
            int burst = 1 + (int)(Math.random() * 3);
            for (int b = 0; b < burst; b++) {
                float cx   = (float)(screenWidth  * 0.08f + Math.random() * screenWidth  * 0.84f);
                float cy   = (float)(screenHeight * 0.08f + Math.random() * screenHeight * 0.84f);
                float maxR = (float)(screenWidth  * 0.12f + Math.random() * screenWidth  * 0.28f);
                float spd  = (float)(0.35f + Math.random() * 0.45f); // px/ms
                // {cx, cy_base, maxR, growSpeed, currentR, alpha(0-255)}
                bgCircleList.add(new float[]{ cx, cy, maxR, spd, 0f, 255f });
            }
            lastCircleSpawn = now;
        }

        Paint cPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cPaint.setStyle(Paint.Style.STROKE);
        cPaint.setColor(Color.WHITE);
        cPaint.setStrokeWidth(screenWidth * 0.007f);

        ArrayList<float[]> toRemove = new ArrayList<>();
        for (float[] c : bgCircleList) {
            // Croissance
            c[4] += c[3];

            // Fondu : commence à disparaître à partir de 60% du rayon max
            float lifeRatio = c[4] / c[2];
            float alpha;
            if (lifeRatio < 0.15f) {
                // Apparition rapide
                alpha = lifeRatio / 0.15f * 220f;
            } else if (lifeRatio < 0.6f) {
                alpha = 220f;
            } else {
                // Disparition progressive
                alpha = (1f - (lifeRatio - 0.6f) / 0.4f) * 220f;
            }
            c[5] = alpha;

            if (lifeRatio >= 1.0f) {
                toRemove.add(c);
                continue;
            }

            float drawY = (c[1] + bgParallaxY) % (screenHeight + c[2] * 2);
            if (drawY < -c[2]) drawY += screenHeight + c[2] * 2;

            cPaint.setAlpha(Math.max(0, (int) c[5]));
            canvas.drawCircle(c[0], drawY, c[4], cPaint);
        }
        bgCircleList.removeAll(toRemove);
    }

    // ══════════════════════════════════════════════════
    // MINIMALISTE — Lignes diagonales
    // Fond bleu nuit. Des groupes de 2-4 lignes fines
    // parallèles traversent l'écran en diagonale par
    // vagues, avec un intervalle calme entre chaque groupe.
    // ══════════════════════════════════════════════════

    private void drawBgMinimalLines(Canvas canvas) {
        canvas.drawColor(BG_COLOR_LINES);

        if (bgLineGroups == null) bgLineGroups = new ArrayList<>();

        long now = System.currentTimeMillis();

        if (now - lastLineSpawn > 1800 + (long)(Math.random() * 1700)) {
            int   nbLines  = 2 + (int)(Math.random() * 3);
            float spacing  = screenWidth * (0.03f + (float)(Math.random() * 0.04f));
            float speed    = screenWidth * (0.0015f + (float)(Math.random() * 0.0015f));
            float lifetime = 3500f + (float)(Math.random() * 2000f); // ms avant disparition
            // {offsetX, speed, alpha, nbLines, spacing, spawnTime, lifetime}
            bgLineGroups.add(new float[]{ -screenWidth * 0.5f, speed, 0f, nbLines, spacing, 0f, lifetime, bgParallaxY });
            lastLineSpawn = now;
        }

        float angleRad = (float) Math.toRadians(35f);
        float cosA     = (float) Math.cos(angleRad);
        float sinA     = (float) Math.sin(angleRad);
        float diag     = (float) Math.hypot(screenWidth, screenHeight);

        Paint lPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lPaint.setStyle(Paint.Style.STROKE);
        lPaint.setColor(Color.WHITE);
        lPaint.setStrokeWidth(screenWidth * 0.005f);

        ArrayList<float[]> toRemove = new ArrayList<>();
        for (float[] g : bgLineGroups) {
            g[0] += g[1];

            g[5] += 16f; // ~16ms par frame à 60fps
            float elapsed  = g[5];
            float lifetime = g[6];

            // Fondu basé sur le timer : apparition 0→300ms, plateau, disparition sur 600ms
            float alpha;
            if (elapsed < 300f) {
                alpha = (elapsed / 300f) * 180f;
            } else if (elapsed < lifetime - 600f) {
                alpha = 180f;
            } else if (elapsed < lifetime) {
                alpha = (1f - (elapsed - (lifetime - 600f)) / 600f) * 180f;
            } else {
                toRemove.add(g);
                continue;
            }

            int   nb    = (int) g[3];
            float space = g[4];

            for (int li = 0; li < nb; li++) {
                float d  = g[0] + li * space;
                float px = -sinA * d;
                // Parallaxe léger vers le bas
                float py = cosA * d + (bgParallaxY - g[7]) * 0.08f;
                float lx = cosA * diag;
                float ly = sinA * diag;

                lPaint.setAlpha(Math.max(0, (int) alpha));
                canvas.drawLine(
                        screenWidth / 2f + px - lx, screenHeight / 2f + py - ly,
                        screenWidth / 2f + px + lx, screenHeight / 2f + py + ly,
                        lPaint);
            }
        }
        bgLineGroups.removeAll(toRemove);
    }

    // ══════════════════════════════════════════════════
    // MINIMALISTE — Points / Ondulations
    // Fond bordeaux sombre. Grille fixe de points blancs
    // discrets. Périodiquement une ondulation part d'un
    // point aléatoire : un anneau blanc qui s'agrandit
    // et s'estompe, comme une goutte dans l'eau.
    // ══════════════════════════════════════════════════

    private void drawBgMinimalDots(Canvas canvas) {
        canvas.drawColor(BG_COLOR_DOTS);

        float spacing = screenWidth * 0.12f;
        float dotR    = screenWidth * 0.010f;
        int   cols    = (int)(screenWidth  / spacing) + 2;
        int   rows    = (int)(screenHeight / spacing) + 4;
        float offsetY = (bgParallaxY * 0.35f) % spacing;

        // La vague monte : waveY va de screenHeight → -spacing en boucle
        // On l'incrémente chaque frame, lentement
        if (bgRipples == null) {
            // On réutilise bgRipples comme conteneur d'un seul float : la position Y de la vague
            bgRipples = new ArrayList<>();
            bgRipples.add(new float[]{ screenHeight }); // waveY initial = bas de l'écran
        }
        float[] waveHolder = bgRipples.get(0);
        waveHolder[0] -= screenHeight * 0.0018f; // vitesse de montée
        if (waveHolder[0] < -spacing * 3)
            waveHolder[0] = screenHeight + spacing * 2; // repart du bas

        float waveY     = waveHolder[0];
        float waveWidth = screenHeight * 0.25f; // hauteur de la zone illuminée

        Paint dPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dPaint.setStyle(Paint.Style.FILL);
        dPaint.setColor(Color.WHITE);

        for (int col = 0; col < cols; col++) {
            for (int row = -1; row < rows; row++) {
                float qOffset = (col % 2 == 0) ? 0f : spacing * 0.5f;
                float x = col * spacing;
                float y = row * spacing + qOffset + offsetY;

                // Distance de ce point à la vague
                float distToWave = Math.abs(y - waveY);
                float waveAlpha;
                if (distToWave < waveWidth) {
                    float t = 1f - (distToWave / waveWidth);
                    waveAlpha = t * t * 185f; // profil quadratique, doux
                } else {
                    waveAlpha = 0f;
                }

                int finalAlpha = (int) Math.min(255f, 50f + waveAlpha);
                dPaint.setAlpha(finalAlpha);
                canvas.drawCircle(x, y, dotR, dPaint);
            }
        }
    }

    // ══════════════════════════════════════════════════
    // MINIMALISTE — Hexagones
    // Fond ocre sombre. Grille d'hexagones en contour
    // blanc. Une vague de luminosité traverse lentement
    // la grille de gauche à droite puis repart de l'autre
    // côté (va-et-vient). Les hexas hors de la vague
    // restent à faible opacité.
    // ══════════════════════════════════════════════════

    private void drawBgMinimalHexagons(Canvas canvas) {
        canvas.drawColor(BG_COLOR_HEXAGONS);

        float hexR    = screenWidth * 0.09f;
        float hexH    = hexR * (float) Math.sqrt(3);
        float colStep = hexR * 1.5f;
        float rowStep = hexH;
        int   cols    = (int)(screenWidth  / colStep) + 3;
        int   rows    = (int)(screenHeight / rowStep) + 4;

        int totalHex = cols * rows;
        if (bgHexPhases == null || bgHexPhases.length != totalHex) {
            bgHexPhases = new float[totalHex];
            for (int i = 0; i < totalHex; i++)
                bgHexPhases[i] = (float)(Math.random() * 0.3f); // micro-décalage aléatoire léger
        }

        // Vague qui avance en va-et-vient
        float waveSpeed = screenWidth * 0.003f;
        bgHexWaveX += waveSpeed * bgHexWaveDir;
        if (bgHexWaveX > screenWidth  * 1.2f) bgHexWaveDir = -1f;
        if (bgHexWaveX < -screenWidth * 0.2f) bgHexWaveDir =  1f;

        float waveWidth = screenWidth * 0.35f; // largeur de la zone lumineuse

        float offsetY = bgParallaxY % rowStep;

        Paint hPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hPaint.setStyle(Paint.Style.STROKE);
        hPaint.setColor(Color.WHITE);
        hPaint.setStrokeWidth(screenWidth * 0.007f);

        int idx = 0;
        for (int col = -1; col < cols; col++) {
            float hcx = col * colStep;

            // Distance de ce col à la vague
            float distToWave = Math.abs(hcx - bgHexWaveX);
            float waveAlpha;
            if (distToWave < waveWidth) {
                // Profil gaussien : max au centre, décroît sur les bords
                float t = 1f - (distToWave / waveWidth);
                waveAlpha = t * t * 180f;
            } else {
                waveAlpha = 0f;
            }

            for (int row = -1; row < rows; row++) {
                float hcy = row * rowStep + ((col % 2 == 0) ? 0 : hexH * 0.5f) + offsetY;

                // Opacité de base faible + contribution de la vague + micro-variation
                float baseAlpha = 35f + bgHexPhases[idx % bgHexPhases.length] * 30f;
                int finalAlpha  = (int) Math.min(255f, baseAlpha + waveAlpha);
                hPaint.setAlpha(finalAlpha);

                Path hex = new Path();
                for (int v = 0; v < 6; v++) {
                    double a  = Math.toRadians(60 * v + 30);
                    float  vx = hcx + (float) Math.cos(a) * hexR * 0.88f;
                    float  vy = hcy + (float) Math.sin(a) * hexR * 0.88f;
                    if (v == 0) hex.moveTo(vx, vy);
                    else        hex.lineTo(vx, vy);
                }
                hex.close();
                canvas.drawPath(hex, hPaint);
                idx++;
            }
        }
    }

    private void drawBgUrbanRain(Canvas canvas) {
        canvas.drawColor(BG_COLOR_RAIN);

        if (bgRainDrops == null) initRain();

        // ── Glows diffus ──
        final Paint[]          localPaints  = bgNeonGlowPaints;
        final RadialGradient[] localShaders = bgNeonGlowShaders;
        final float[]          localData    = bgNeonGlowData;
        if (localPaints != null && localShaders != null && localData != null) {
            for (int i = 0; i < 6; i++) {
                localData[i * 6 + 3] += localData[i * 6 + 4];
                float phase   = localData[i * 6 + 3];
                float flicker = (float)(Math.sin(phase * Math.PI * 2) * 0.15f
                        + Math.sin(phase * Math.PI * 13) * 0.05f);
                float alphaFactor = Math.max(0.1f, Math.min(1.0f, 1f + flicker));

                float cx  = localData[i * 6]     * screenWidth;
                float cy  = localData[i * 6 + 1] * screenHeight + bgParallaxY * 0.03f;
                float rad = localData[i * 6 + 2] * screenWidth;

                bgGlowMatrix.setScale(rad, rad);
                bgGlowMatrix.postTranslate(cx, cy);
                localShaders[i].setLocalMatrix(bgGlowMatrix);
                localPaints[i].setAlpha((int)(255 * alphaFactor));
                canvas.drawCircle(cx, cy, rad, localPaints[i]);
            }
        }

        // ── Gouttes ──
        bgRainPWide.setStrokeWidth(screenWidth * 0.032f);
        bgRainPMid.setStrokeWidth(screenWidth * 0.018f);
        bgRainPCore.setStrokeWidth(screenWidth * 0.005f);

        for (float[] d : bgRainDrops) {
            d[1] += d[3];
            float drawY = d[1] + bgParallaxY * 0.05f;
            if (drawY > screenHeight + d[2])
                d[1] = -d[2] - (float)(Math.random() * screenHeight * 0.5f);

            int color = bgNeonColors[(int) d[4]];
            int r = (color >> 16) & 0xFF;
            int g = (color >>  8) & 0xFF;
            int b =  color        & 0xFF;
            int a = Math.min(255, (int) d[5]);

            float tx = d[0], ty = drawY, bx = d[0], by = drawY + d[2];

            bgRainPWide.setColor(Color.argb(a / 9, r, g, b));
            canvas.drawLine(tx, ty, bx, by, bgRainPWide);

            bgRainPMid.setColor(Color.argb(a / 4, r, g, b));
            canvas.drawLine(tx, ty, bx, by, bgRainPMid);

            bgRainPCore.setColor(Color.argb(a, 255, 255, 255));
            canvas.drawLine(tx, ty, bx, by, bgRainPCore);
        }
    }

    private void initRain() {
        int count = 42;
        bgRainDrops = new float[count][6];
        int[] colorPool = {0, 0, 0, 1, 1, 2, 2, 3, 4, 4, 5, 6, 7};
        for (int i = 0; i < count; i++) {
            float speed  = (float)(2.5f + Math.random() * 7.0f);
            float length = (float)(screenHeight * 0.04f + Math.random() * screenHeight * 0.10f);
            float alpha  = Math.min(255f, 130f + (speed / 9.5f) * 110f);
            bgRainDrops[i][0] = (float)(Math.random() * screenWidth);
            bgRainDrops[i][1] = (float)(Math.random() * screenHeight);
            bgRainDrops[i][2] = length;
            bgRainDrops[i][3] = speed;
            bgRainDrops[i][4] = colorPool[(int)(Math.random() * colorPool.length)];
            bgRainDrops[i][5] = alpha;
        }
        bgNeonGlowData = new float[] {
                0.15f, 0.20f, 0.55f, 0.00f, 0.0007f, 0f,
                0.80f, 0.10f, 0.45f, 0.30f, 0.0011f, 1f,
                0.05f, 0.65f, 0.60f, 0.60f, 0.0009f, 1f,
                0.90f, 0.55f, 0.50f, 0.15f, 0.0013f, 0f,
                0.45f, 0.85f, 0.40f, 0.75f, 0.0008f, 0f,
                0.70f, 0.40f, 0.35f, 0.45f, 0.0012f, 1f,
        };
        bgNeonGlowShaders = new RadialGradient[6];
        bgNeonGlowPaints  = new Paint[6];
        for (int i = 0; i < 6; i++) {
            boolean isCyan = bgNeonGlowData[i * 6 + 5] == 0f;
            int r = isCyan ? 0   : 240;
            int g = isCyan ? 200 : 50;
            int b = isCyan ? 240 : 120;
            bgNeonGlowShaders[i] = new RadialGradient(0f, 0f, 1f,
                    new int[]{ Color.argb(70, r, g, b), Color.argb(0, r, g, b) },
                    new float[]{ 0f, 1f },
                    Shader.TileMode.CLAMP);
            bgNeonGlowPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgNeonGlowPaints[i].setStyle(Paint.Style.FILL);
            bgNeonGlowPaints[i].setShader(bgNeonGlowShaders[i]);
        }
    }

    private void drawBgUrbanCircuit(Canvas canvas) {
        canvas.drawColor(BG_COLOR_CIRCUIT);

        if (bgCircuitNodes == null) initCircuit();

        float parallaxOff = bgParallaxY * 0.12f;
        int n = bgCircuitNodes.length;

        // Passes glow simulé : large+transparent → moyen → fin+opaque
        Paint seg1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        seg1.setStyle(Paint.Style.STROKE);
        seg1.setStrokeWidth(screenWidth * 0.018f);
        seg1.setColor(Color.argb(18, 0, 255, 100));

        Paint seg2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        seg2.setStyle(Paint.Style.STROKE);
        seg2.setStrokeWidth(screenWidth * 0.008f);
        seg2.setColor(Color.argb(35, 0, 255, 100));

        Paint seg3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        seg3.setStyle(Paint.Style.STROKE);
        seg3.setStrokeWidth(screenWidth * 0.003f);
        seg3.setColor(Color.argb(90, 0, 255, 100));

        Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < n; i++) {
            float x1 = bgCircuitNodes[i][0];
            float y1 = bgCircuitNodes[i][1] + parallaxOff;
            for (int j = i + 1; j < n; j++) {
                float x2 = bgCircuitNodes[j][0];
                float y2 = bgCircuitNodes[j][1] + parallaxOff;
                float dist = (float) Math.hypot(x2 - x1, y2 - y1);
                if (dist < screenWidth * 0.32f
                        && (Math.abs(x2 - x1) < screenWidth * 0.05f
                        || Math.abs(y2 - y1) < screenWidth * 0.05f)) {
                    float mx = x2, my = y1;
                    canvas.drawLine(x1, y1, mx, my, seg1);
                    canvas.drawLine(mx, my, x2, y2, seg1);
                    canvas.drawLine(x1, y1, mx, my, seg2);
                    canvas.drawLine(mx, my, x2, y2, seg2);
                    canvas.drawLine(x1, y1, mx, my, seg3);
                    canvas.drawLine(mx, my, x2, y2, seg3);
                }
            }
            // Nœud : 3 cercles concentriques
            nodePaint.setColor(Color.argb(20,  0, 255, 100));
            canvas.drawCircle(x1, y1, screenWidth * 0.028f, nodePaint);
            nodePaint.setColor(Color.argb(50,  0, 255, 100));
            canvas.drawCircle(x1, y1, screenWidth * 0.016f, nodePaint);
            nodePaint.setColor(Color.argb(160, 0, 255, 100));
            canvas.drawCircle(x1, y1, screenWidth * 0.007f, nodePaint);
        }

        // Pulses
        Paint pulse1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulse1.setStyle(Paint.Style.FILL);
        Paint pulse2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulse2.setStyle(Paint.Style.FILL);
        Paint pulse3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulse3.setStyle(Paint.Style.FILL);

        for (float[] p : bgCircuitPulses) {
            p[2] += p[3];
            if (p[2] > 1f) p[2] = 0f;

            int from = (int) p[0], to = (int) p[1];
            if (from >= n || to >= n) continue;

            float x1 = bgCircuitNodes[from][0];
            float y1 = bgCircuitNodes[from][1] + parallaxOff;
            float x2 = bgCircuitNodes[to][0];
            float y2 = bgCircuitNodes[to][1] + parallaxOff;

            float mx = x2, my = y1, t = p[2];
            float seg1L = Math.abs(mx - x1), seg2L = Math.abs(y2 - my);
            float total = seg1L + seg2L;
            if (total < 1f) continue;

            float px, py;
            if (t < seg1L / total) {
                px = x1 + (mx - x1) * (t * total / seg1L);
                py = y1;
            } else {
                float t2 = (t * total - seg1L) / seg2L;
                px = mx;
                py = my + (y2 - my) * t2;
            }

            pulse1.setColor(Color.argb(25,  180, 255, 210));
            canvas.drawCircle(px, py, screenWidth * 0.038f, pulse1);
            pulse2.setColor(Color.argb(70,  180, 255, 210));
            canvas.drawCircle(px, py, screenWidth * 0.022f, pulse2);
            pulse3.setColor(Color.argb(220, 220, 255, 235));
            canvas.drawCircle(px, py, screenWidth * 0.009f, pulse3);
        }
    }

    private void initCircuit() {
        int cols = 5, rows = 9;
        float cellW = screenWidth  / (float) cols;
        float cellH = screenHeight / (float) rows;
        int count = cols * rows;
        bgCircuitNodes = new float[count][2];
        int idx = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                bgCircuitNodes[idx][0] = col * cellW + cellW * 0.2f + (float)(Math.random() * cellW * 0.6f);
                bgCircuitNodes[idx][1] = row * cellH + cellH * 0.2f + (float)(Math.random() * cellH * 0.6f);
                idx++;
            }
        }
        int pulseCount = 18;
        bgCircuitPulses = new float[pulseCount][4];
        for (int i = 0; i < pulseCount; i++) {
            bgCircuitPulses[i][0] = (float)(int)(Math.random() * count);
            bgCircuitPulses[i][1] = (float)(int)(Math.random() * count);
            bgCircuitPulses[i][2] = (float)(Math.random());
            bgCircuitPulses[i][3] = (float)(0.002f + Math.random() * 0.004f);
        }
    }

    private void drawBgUrbanGrid(Canvas canvas) {
        canvas.drawColor(BG_COLOR_GRID);

        long  t       = System.currentTimeMillis();
        float gridOff = (t * 0.00012f + bgParallaxY * 0.0003f) % 1.0f;

        float vpX     = screenWidth  * 0.50f;
        float vpY     = screenHeight * 0.42f;

        Paint fine = new Paint(Paint.ANTI_ALIAS_FLAG);
        fine.setStyle(Paint.Style.STROKE);

        Paint mid = new Paint(Paint.ANTI_ALIAS_FLAG);
        mid.setStyle(Paint.Style.STROKE);

        Paint wide = new Paint(Paint.ANTI_ALIAS_FLAG);
        wide.setStyle(Paint.Style.STROKE);

        // ── Lignes horizontales ──
        int hLines = 10;
        for (int i = 0; i < hLines; i++) {
            float frac = (i + gridOff) / (float) hLines;
            float t2   = frac * frac * frac;
            float y    = vpY + t2 * (screenHeight - vpY + screenHeight * 0.3f);
            if (y > screenHeight * 1.1f || y < vpY) continue;

            int a = (int) Math.min(220f, 40f + t2 * 180f);

            wide.setStrokeWidth(screenWidth * 0.018f);
            wide.setColor(Color.argb(a / 8, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, wide);

            mid.setStrokeWidth(screenWidth * 0.008f);
            mid.setColor(Color.argb(a / 4, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, mid);

            fine.setStrokeWidth(screenWidth * 0.003f);
            fine.setColor(Color.argb(a, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, fine);
        }

        // ── Lignes de fuite ──
        int vLines = 12;
        for (int i = 0; i <= vLines; i++) {
            float startX = -screenWidth * 0.3f + i * (screenWidth * 1.6f / vLines);
            float startY = screenHeight * 1.05f;

            int a = (int) Math.max(30f, Math.min(180f,
                    60f + (1f - Math.abs(startX - screenWidth / 2f) / (screenWidth * 0.8f)) * 120f));

            wide.setStrokeWidth(screenWidth * 0.018f);
            wide.setColor(Color.argb(a / 8, 80, 40, 220));
            canvas.drawLine(startX, startY, vpX, vpY, wide);

            mid.setStrokeWidth(screenWidth * 0.008f);
            mid.setColor(Color.argb(a / 4, 80, 40, 220));
            canvas.drawLine(startX, startY, vpX, vpY, mid);

            fine.setStrokeWidth(screenWidth * 0.003f);
            fine.setColor(Color.argb(a, 80, 40, 220));
            canvas.drawLine(startX, startY, vpX, vpY, fine);
        }

        // ── Halo central (point de fuite) ──
        Paint vpGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        vpGlow.setStyle(Paint.Style.FILL);
        vpGlow.setShader(new RadialGradient(vpX, vpY, screenWidth * 0.25f,
                new int[]{ 0x55AAAAFF, 0x224444FF, 0x00000000 },
                new float[]{ 0f, 0.4f, 1f },
                Shader.TileMode.CLAMP));
        canvas.drawCircle(vpX, vpY, screenWidth * 0.25f, vpGlow);
    }
}