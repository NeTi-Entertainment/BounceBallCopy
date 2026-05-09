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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public class BackgroundRenderer {

    private static final int BG_COLOR_TRIANGLES = 0xFF6AADAA;
    private static final int BG_COLOR_CIRCLES   = 0xFF4A5568;
    private static final int BG_COLOR_LINES     = 0xFF2A3550;
    private static final int BG_COLOR_DOTS      = 0xFF3D1F2A;
    private static final int BG_COLOR_HEXAGONS  = 0xFF3A2D1A;
    private static final int BG_COLOR_RAIN      = 0xFF12203A;
    private static final int BG_COLOR_CIRCUIT   = 0xFF1A4A1F;
    private static final int BG_COLOR_GRID      = 0xFF060010;
    private static final int BG_COLOR_TUNNEL    = 0xFF020008;
    private static final int BG_COLOR_EQUALIZER = 0xFF050505;

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
    private float[][] bgCircuitSegs = null;
    private float[][] bgCircuitPads = null;
    private final Paint circuitTracePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circuitNodeFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circuitNodeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int CIRCUIT_TRACE = 0xFFCCDAC8;
    private float[][] bgCircuitPulses  = null;
    private float[][] bgCircuitTraces = null;
    private long      bgCircuitLastSpawn = 0L;
    private final Paint circuitPulse0 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix circuitPulseMatrix = new Matrix();
    private final RadialGradient circuitPulseGradient = new RadialGradient(
            0f, 0f, 1f,
            new int[]{ 0xFFFFFFEE, 0xCCFFEE00, 0x66FFAA00, 0x22FF6600, 0x00FF0000 },
            new float[]{ 0f, 0.15f, 0.45f, 0.75f, 1f },
            Shader.TileMode.CLAMP);
    private static final int  CIRCUIT_MAX_PULSES = 7;
    private static final long CIRCUIT_SPAWN_MS   = 450L;
    private static final float CIRCUIT_PULSE_SPD = 0.011f;

    // ── bg_urban_grid ──
    private final Paint  gridFinePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  gridMidPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  gridWidePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint  gridSunPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path   gridSunClipPath = new Path();
    private final Path   gridWavePath   = new Path();
    private final Matrix gridSunMatrix   = new Matrix();
    private final RadialGradient gridSunGradient = new RadialGradient(
            0f, 0f, 1f,
            new int[]{ 0xFFFFEEFF, 0xFFFF44CC, 0xFFCC0066, 0x00660033 },
            new float[]{ 0f, 0.35f, 0.78f, 1f },
            Shader.TileMode.CLAMP);

    // ── bg_urban_tunnel ──
    private static final int   TUNNEL_SEGMENT_COUNT = 8;
    private static final float TUNNEL_SPEED = 0.065f;
    private float bgTunnelTime = 0f;
    private final Paint tunnelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tunnelVigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient tunnelVigGradient = null;
    private final Matrix tunnelVigMatrix = new Matrix();
    private int tunnelVigW = 0, tunnelVigH = 0;
    private static final int[] TUNNEL_COLORS = {
            0xFFFFEE00, 0xFFFFCC00, 0xFFFFAA22,
    };
    private static final float TUNNEL_ORBIT_RADIUS = 0.04f;
    private static final float TUNNEL_ORBIT_SPEED  = 0.35f;

    // ── bg_urban_equalizer ──
    private float[] bgEqBarHeights  = null;
    private float[] bgEqBarTargets  = null;
    private float[] bgEqBarSpeeds   = null;
    private float[] bgEqBarPhases   = null;
    private static final int EQ_BAR_COUNT = 28;
    private float bgEqTime = 0f;
    private final Paint eqBarPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eqGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RadialGradient eqGlowGrad = new RadialGradient(
            0f, 0f, 1f,
            new int[]{ 0xFFFFFFFF, 0xAAFFFFFF, 0x44FFFFFF, 0x11FFFFFF, 0x00FFFFFF },
            new float[]{ 0f, 0.15f, 0.40f, 0.70f, 1f },
            Shader.TileMode.CLAMP);
    private final Matrix eqGlowMatrix = new Matrix();
    private final RectF  eqGlowRect  = new RectF();
    private PorterDuffColorFilter[] eqCachedFilters = null;
    private int[] eqCachedFilterKeys = null;

    // ── bg_grad_* ── (lava-lamp style: per-pixel noise → palette LUT)
    private static final int GRAD_DOWNSCALE = 6;
    private static final int GRAD_LUT_SIZE = 256;
    private int              bgGradVariant = -1;
    private android.graphics.Bitmap  gradOffscreen      = null;
    private final Paint              gradBitmapPaint     = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF              gradDestRect        = new RectF();
    private int[]   gradPixelBuffer = null;
    private int[]   gradBlurBuffer  = null;
    private int[]   gradLUT         = null;
    private float[] gradTblX1, gradTblX2, gradTblX3, gradTblX4;
    private float[] gradTblY1, gradTblY2, gradTblY3, gradTblY4;
    private int     gradTblW, gradTblH;
    private float[] gradWavePhases = null;

    // ── bg_default : sol, nuages, étoiles ──────────────────
    // Nuages : { x, y, largeur, hauteur, vitesse, alpha, modèle }
    private ArrayList<float[]> bgClouds     = null;
    private long               lastCloudSpawnMs = 0L;

    // Étoiles : { x, y, timerActuel, timerMax }
    private ArrayList<float[]> bgStars      = null;
    private long               lastStarSpawnMs  = 0L;

    private final Paint defaultBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudLayerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[][] GRAD_COLORS = {
            // 0: Rainbow
            { 0xFFDD0040, 0xFFFF8800, 0xFFEEDD00, 0xFF00BB44, 0xFF2266FF, 0xFF9900DD },
            // 1: Sunset
            { 0xFFEE4400, 0xFFFF6644, 0xFFDD2266, 0xFF772299, 0xFFFF7733, 0xFFCC1166 },
            // 2: Ocean
            { 0xFF0033AA, 0xFF00AACC, 0xFF00BB88, 0xFF1155CC, 0xFF007788, 0xFF002255 },
            // 3: Forest
            { 0xFF226622, 0xFF557722, 0xFF887733, 0xFFAA7722, 0xFF114411, 0xFF886622 },
            // 4: Lava
            { 0xFFCC1100, 0xFFFF4400, 0xFFFF8800, 0xFF331100, 0xFFEE2200, 0xFF552200 },
            // 5: Aurora
            { 0xFF00CC66, 0xFF00AAAA, 0xFF6633CC, 0xFF2288AA, 0xFF44DD88, 0xFF7744BB },
            // 6: Candy
            { 0xFFFF3388, 0xFFDD44CC, 0xFF8855EE, 0xFFFF66AA, 0xFFCC22FF, 0xFFFF88CC },
            // 7: Volcano
            { 0xFF220000, 0xFF880000, 0xFFCC3300, 0xFF110000, 0xFF661100, 0xFFFF5500 },
            // 8: Galaxy
            { 0xFF1A0033, 0xFF4400AA, 0xFF8800CC, 0xFFDD44AA, 0xFF2200BB, 0xFF110044 },
            // 9: Toxic
            { 0xFF33DD00, 0xFFAAEE00, 0xFF006633, 0xFF88FF00, 0xFF004422, 0xFFCCFF22 },
    };

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
        circuitTracePaint.setStyle(Paint.Style.STROKE);
        circuitTracePaint.setStrokeCap(Paint.Cap.ROUND);
        circuitTracePaint.setColor(CIRCUIT_TRACE);
        circuitNodeFgPaint.setStyle(Paint.Style.FILL);
        circuitNodeFgPaint.setColor(CIRCUIT_TRACE);
        circuitNodeBgPaint.setStyle(Paint.Style.FILL);
        circuitNodeBgPaint.setColor(BG_COLOR_CIRCUIT);
        circuitPulse0.setStyle(Paint.Style.FILL);
        circuitPulse0.setShader(circuitPulseGradient);
        gridFinePaint.setStyle(Paint.Style.STROKE);
        gridMidPaint.setStyle(Paint.Style.STROKE);
        gridWidePaint.setStyle(Paint.Style.STROKE);
        gridSunPaint.setStyle(Paint.Style.FILL);
        tunnelPaint.setStyle(Paint.Style.STROKE);
        eqBarPaint.setStyle(Paint.Style.FILL);
        eqGlowPaint.setStyle(Paint.Style.FILL);
        eqGlowPaint.setShader(eqGlowGrad);
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
        bgCircuitSegs = null;
        bgCircuitPads = null;
        bgCircuitPulses = null;
        bgCircuitLastSpawn = 0L;
        bgCircuitTraces = null;
        bgNeonGlowData   = null;
        bgNeonGlowPaints  = null;
        bgNeonGlowShaders = null;
        bgTunnelTime  = 0f;
        tunnelVigGradient = null;
        tunnelVigW = tunnelVigH = 0;
        bgEqBarHeights = null;
        bgEqBarTargets = null;
        bgEqBarSpeeds  = null;
        bgEqBarPhases  = null;
        bgEqTime = 0f;
        bgGradVariant = -1;
        gradWavePhases = null;
        gradPixelBuffer = null;
        gradBlurBuffer = null;
        gradLUT = null;
        gradTblX1 = gradTblX2 = gradTblX3 = gradTblX4 = null;
        gradTblY1 = gradTblY2 = gradTblY3 = gradTblY4 = null;
        gradTblW = gradTblH = 0;
        if (gradOffscreen != null) {
            gradOffscreen.recycle();
            gradOffscreen = null;
        }
        bgClouds         = null;
        lastCloudSpawnMs = 0L;
        bgStars          = null;
        lastStarSpawnMs  = 0L;
    }

    public void updateParallax(float cameraShift) {
        bgParallaxY += cameraShift * 1.8f;
    }

    public void draw(Canvas canvas) {
        draw(canvas, 0f);
    }

//    public void draw(Canvas canvas) {
//        String pending = pendingSkinId;
//        if (pending != null) {
//            pendingSkinId = null;
//            currentBgSkin = pending;
//            bgParallaxY   = 0f;
//            resetAllState();
//        }

    public void draw(Canvas canvas, float currentHeight) {
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
            case "bg_urban_tunnel":    drawBgUrbanTunnel(canvas);    break;
            case "bg_urban_equalizer": drawBgUrbanEqualizer(canvas); break;
            case "bg_grad_rainbow": drawBgGrad(canvas, 0); break;
            case "bg_grad_sunset":  drawBgGrad(canvas, 1); break;
            case "bg_grad_ocean":   drawBgGrad(canvas, 2); break;
            case "bg_grad_forest":  drawBgGrad(canvas, 3); break;
            case "bg_grad_lava":    drawBgGrad(canvas, 4); break;
            case "bg_grad_aurora":  drawBgGrad(canvas, 5); break;
            case "bg_grad_candy":   drawBgGrad(canvas, 6); break;
            case "bg_grad_volcano": drawBgGrad(canvas, 7); break;
            case "bg_grad_galaxy":  drawBgGrad(canvas, 8); break;
            case "bg_grad_toxic":   drawBgGrad(canvas, 9); break;
            default:
                drawDefaultBackground(canvas, currentHeight);
                break;
        }
    }

    private void drawBgGrad(Canvas canvas, int variant) {
        int[] palette = GRAD_COLORS[variant];
        int palLen = palette.length;

        if (bgGradVariant != variant) {
            bgGradVariant = variant;
            gradWavePhases = new float[]{ 0f, 0.7f, 1.4f, 2.1f, 2.8f, 3.5f, 4.2f, 4.9f, 0f };

            gradLUT = new int[GRAD_LUT_SIZE];
            for (int i = 0; i < GRAD_LUT_SIZE; i++) {
                float t = (float) i / GRAD_LUT_SIZE * palLen;
                int ci = (int) t;
                float f = t - ci;
                f = f * f * (3f - 2f * f);
                int c1 = palette[ci % palLen];
                int c2 = palette[(ci + 1) % palLen];
                int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
                int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
                gradLUT[i] = 0xFF000000
                        | ((r1 + (int)(f * (r2 - r1))) << 16)
                        | ((g1 + (int)(f * (g2 - g1))) << 8)
                        |  (b1 + (int)(f * (b2 - b1)));
            }
        }

        int smallW = Math.max(1, screenWidth  / GRAD_DOWNSCALE);
        int smallH = Math.max(1, screenHeight / GRAD_DOWNSCALE);
        if (gradOffscreen == null || gradOffscreen.getWidth() != smallW || gradOffscreen.getHeight() != smallH) {
            if (gradOffscreen != null) gradOffscreen.recycle();
            gradOffscreen = android.graphics.Bitmap.createBitmap(smallW, smallH, android.graphics.Bitmap.Config.ARGB_8888);
        }
        int total = smallW * smallH;
        if (gradPixelBuffer == null || gradPixelBuffer.length != total) {
            gradPixelBuffer = new int[total];
            gradBlurBuffer  = new int[total];
        }
        if (gradTblW != smallW || gradTblH != smallH) {
            gradTblW = smallW;
            gradTblH = smallH;
            gradTblX1 = new float[smallW]; gradTblX2 = new float[smallW];
            gradTblX3 = new float[smallW]; gradTblX4 = new float[smallW];
            gradTblY1 = new float[smallH]; gradTblY2 = new float[smallH];
            gradTblY3 = new float[smallH]; gradTblY4 = new float[smallH];
        }

        gradWavePhases[0] += 0.007f;
        gradWavePhases[1] += 0.009f;
        gradWavePhases[2] += 0.005f;
        gradWavePhases[3] += 0.008f;
        gradWavePhases[4] += 0.006f;
        gradWavePhases[5] += 0.010f;
        gradWavePhases[6] += 0.004f;
        gradWavePhases[7] += 0.011f;
        gradWavePhases[8] += 0.0012f;

        float invW = 1f / smallW;
        float invH = 1f / smallH;
        for (int x = 0; x < smallW; x++) {
            float nx = x * invW;
            gradTblX1[x] = (float) Math.sin(nx * 1.3 + gradWavePhases[0]);
            gradTblX2[x] = (float) Math.sin(nx * 2.1 + gradWavePhases[2]);
            gradTblX3[x] = (float) Math.cos(nx * 0.7 + gradWavePhases[4]);
            gradTblX4[x] = (float) Math.sin(nx * 3.4 + gradWavePhases[6]);
        }
        for (int y = 0; y < smallH; y++) {
            float ny = y * invH;
            gradTblY1[y] = (float) Math.cos(ny * 1.0 + gradWavePhases[1]);
            gradTblY2[y] = (float) Math.cos(ny * 1.7 + gradWavePhases[3]);
            gradTblY3[y] = (float) Math.sin(ny * 0.6 + gradWavePhases[5]);
            gradTblY4[y] = (float) Math.cos(ny * 2.9 + gradWavePhases[7]);
        }

        float colorShift = gradWavePhases[8];
        int lutMask = GRAD_LUT_SIZE - 1;

        int idx = 0;
        for (int y = 0; y < smallH; y++) {
            float tY1 = gradTblY1[y], tY2 = gradTblY2[y];
            float tY3 = gradTblY3[y], tY4 = gradTblY4[y];
            for (int x = 0; x < smallW; x++) {
                float n = gradTblX1[x] * tY1
                        + 0.7f * gradTblX2[x] * tY2
                        + 0.5f * gradTblX3[x] * tY3
                        + 0.3f * gradTblX4[x] * tY4;
                n = n * 0.2f + 0.5f;
                n = (n + colorShift) % 1f;
                if (n < 0f) n += 1f;
                int li = (int)(n * GRAD_LUT_SIZE) & lutMask;
                gradPixelBuffer[idx++] = gradLUT[li];
            }
        }

        for (int y = 1; y < smallH - 1; y++) {
            int row = y * smallW;
            for (int x = 1; x < smallW - 1; x++) {
                int i = row + x;
                int c  = gradPixelBuffer[i];
                int cL = gradPixelBuffer[i - 1];
                int cR = gradPixelBuffer[i + 1];
                int cU = gradPixelBuffer[i - smallW];
                int cD = gradPixelBuffer[i + smallW];
                int r = (((c >> 16) & 0xFF) * 4 + ((cL >> 16) & 0xFF) + ((cR >> 16) & 0xFF) + ((cU >> 16) & 0xFF) + ((cD >> 16) & 0xFF)) >> 3;
                int g = (((c >>  8) & 0xFF) * 4 + ((cL >>  8) & 0xFF) + ((cR >>  8) & 0xFF) + ((cU >>  8) & 0xFF) + ((cD >>  8) & 0xFF)) >> 3;
                int b = (( c        & 0xFF) * 4 + ( cL        & 0xFF) + ( cR        & 0xFF) + ( cU        & 0xFF) + ( cD        & 0xFF)) >> 3;
                gradBlurBuffer[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        for (int x = 0; x < smallW; x++) {
            gradBlurBuffer[x] = gradPixelBuffer[x];
            gradBlurBuffer[(smallH - 1) * smallW + x] = gradPixelBuffer[(smallH - 1) * smallW + x];
        }
        for (int y = 0; y < smallH; y++) {
            gradBlurBuffer[y * smallW] = gradPixelBuffer[y * smallW];
            gradBlurBuffer[y * smallW + smallW - 1] = gradPixelBuffer[y * smallW + smallW - 1];
        }

        gradOffscreen.setPixels(gradBlurBuffer, 0, smallW, 0, 0, smallW, smallH);
        gradDestRect.set(0, 0, screenWidth, screenHeight);
        canvas.drawBitmap(gradOffscreen, null, gradDestRect, gradBitmapPaint);
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
        if (bgCircuitSegs == null) initCircuit();

        // Init pulses au premier appel
        if (bgCircuitPulses == null) {
            bgCircuitPulses = new float[CIRCUIT_MAX_PULSES][3];
            for (float[] p : bgCircuitPulses) p[0] = -1f;
        }

        float cell = screenWidth / 20f;
        float tw   = cell * 0.45f;
        float nr   = tw * 1.7f;
        float hr   = tw * 0.6f;
        circuitTracePaint.setStrokeWidth(tw);

        // ── Traits et pads ──
        for (float[] s : bgCircuitSegs)
            canvas.drawLine(s[0], s[1], s[2], s[3], circuitTracePaint);
        for (float[] p : bgCircuitPads) {
            canvas.drawCircle(p[0], p[1], nr, circuitNodeFgPaint);
            canvas.drawCircle(p[0], p[1], hr, circuitNodeBgPaint);
        }

        // ── Spawn ──
        long now = System.currentTimeMillis();
        if (now - bgCircuitLastSpawn > CIRCUIT_SPAWN_MS) {
            bgCircuitLastSpawn = now;
            for (float[] pulse : bgCircuitPulses) {
                if (pulse[0] >= 0f) continue;
                for (int attempt = 0; attempt < 30; attempt++) {
                    int ti = (int)(Math.random() * bgCircuitTraces.length);
                    boolean busy = false;
                    for (float[] other : bgCircuitPulses)
                        if ((int)other[0] == ti) { busy = true; break; }
                    if (!busy) {
                        pulse[0] = ti;
                        pulse[1] = 0f;
                        pulse[2] = Math.random() < 0.5f ? 1f : -1f;
                        break;
                    }
                }
                break;
            }
        }

        // ── Mise à jour + dessin des pulses ──
        for (float[] pulse : bgCircuitPulses) {
            if (pulse[0] < 0f) continue;
            pulse[1] += CIRCUIT_PULSE_SPD;
            if (pulse[1] >= 1f) { pulse[0] = -1f; continue; }

            float[] trace = bgCircuitTraces[(int) pulse[0]];
            int nPts = trace.length / 2;
            float prog = (pulse[2] > 0f) ? pulse[1] : (1f - pulse[1]);

            // Calcul longueurs des segments du tracé
            float totalLen = 0f;
            for (int k = 0; k < nPts - 1; k++) {
                float dx = trace[(k+1)*2] - trace[k*2];
                float dy = trace[(k+1)*2+1] - trace[k*2+1];
                totalLen += (float) Math.hypot(dx, dy);
            }
            float target = prog * totalLen;
            float px = trace[0], py = trace[1];
            float acc = 0f;
            for (int k = 0; k < nPts - 1; k++) {
                float dx = trace[(k+1)*2] - trace[k*2];
                float dy = trace[(k+1)*2+1] - trace[k*2+1];
                float segLen = (float) Math.hypot(dx, dy);
                if (acc + segLen >= target) {
                    float t = (target - acc) / segLen;
                    px = trace[k*2]   + dx * t;
                    py = trace[k*2+1] + dy * t;
                    break;
                }
                acc += segLen;
            }

            // Fondu court : 5% entrée, 5% sortie
            float fade = Math.min(pulse[1] / 0.05f, Math.min(1f, (1f - pulse[1]) / 0.05f));
            float glowR = tw * 4.5f;
            circuitPulseMatrix.setScale(glowR, glowR);
            circuitPulseMatrix.postTranslate(px, py);
            circuitPulseGradient.setLocalMatrix(circuitPulseMatrix);
            circuitPulse0.setAlpha((int)(255 * fade));
            canvas.drawCircle(px, py, glowR, circuitPulse0);
        }
    }

    private void initCircuit() {
        float cell = screenWidth / 20f;
        int cols = (int)(screenWidth  / cell) + 10;
        int rows = (int)(screenHeight / cell) + 10;
        int ox = 5, oy = 5;

        int[][] grid = new int[cols][rows];
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows; r++)
                grid[c][r] = -1;

        ArrayList<float[]> segList = new ArrayList<>();
        ArrayList<float[]> padList = new ArrayList<>();
        int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        int traceId = 0;

        for (int attempt = 0; attempt < 8000 && traceId < 120; attempt++) {
            int gx = (int)(Math.random() * cols);
            int gy = (int)(Math.random() * rows);
            if (!ccFree(grid, gx, gy, cols, rows, traceId)) continue;

            int dirIdx = (int)(Math.random() * DIRS.length);
            int[] d = DIRS[dirIdx];
            int maxLen = 4 + (int)(Math.random() * 4);
            int len = 0;
            for (int k = 1; k <= maxLen; k++) {
                int nx = gx + d[0]*k, ny = gy + d[1]*k;
                if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) break;
                if (!ccFree(grid, nx, ny, cols, rows, traceId)) break;
                len = k;
            }
            if (len < 2) continue;

            grid[gx][gy] = traceId;
            for (int k = 1; k <= len; k++)
                grid[gx + d[0]*k][gy + d[1]*k] = traceId;

            int ex = gx + d[0]*len, ey = gy + d[1]*len;
            float x1 = (gx - ox) * cell, y1 = (gy - oy) * cell;
            float x2 = (ex - ox) * cell, y2 = (ey - oy) * cell;
            segList.add(new float[]{x1, y1, x2, y2});
            padList.add(new float[]{x1, y1});

            boolean extended = false;
            if (Math.random() < 0.7f) {
                // Virages valides par direction : uniquement 90° ou 135°, jamais 45°
                // Index DIRS : 0={1,0} 1={-1,0} 2={0,1} 3={0,-1}
                //              4={1,1} 5={-1,-1} 6={1,-1} 7={-1,1}
                int[][][] VALID_TURNS = {
                        {{0,1},{0,-1},{1,1},{1,-1}},    // 0: →  90°: ↓↑,  135°: ↘↗
                        {{0,1},{0,-1},{-1,1},{-1,-1}},  // 1: ←  90°: ↓↑,  135°: ↙↖
                        {{1,0},{-1,0},{1,1},{-1,1}},    // 2: ↓  90°: →←,  135°: ↘↙
                        {{1,0},{-1,0},{1,-1},{-1,-1}},  // 3: ↑  90°: →←,  135°: ↗↖
                        {{1,-1},{-1,1},{1,0},{0,1}},    // 4: ↘  90°: ↗↙,  135°: →↓
                        {{1,-1},{-1,1},{-1,0},{0,-1}},  // 5: ↖  90°: ↗↙,  135°: ←↑
                        {{1,1},{-1,-1},{1,0},{0,-1}},   // 6: ↗  90°: ↘↖,  135°: →↑
                        {{1,1},{-1,-1},{-1,0},{0,1}},   // 7: ↙  90°: ↘↖,  135°: ←↓
                };
                int[][] turns = VALID_TURNS[dirIdx];
                int[] perp = turns[(int)(Math.random() * turns.length)];
                int maxLen2 = 3 + (int)(Math.random() * 4);
                int len2 = 0;
                for (int k = 1; k <= maxLen2; k++) {
                    int nx = ex + perp[0]*k, ny = ey + perp[1]*k;
                    if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) break;
                    if (!ccFree(grid, nx, ny, cols, rows, traceId)) break;
                    len2 = k;
                }
                if (len2 >= 2) {
                    for (int k = 1; k <= len2; k++)
                        grid[ex + perp[0]*k][ey + perp[1]*k] = traceId;
                    float x3 = (ex + perp[0]*len2 - ox) * cell;
                    float y3 = (ey + perp[1]*len2 - oy) * cell;
                    segList.add(new float[]{x2, y2, x3, y3});
                    padList.add(new float[]{x3, y3});
                    extended = true;
                }
            }
            if (!extended) padList.add(new float[]{x2, y2});
            traceId++;
        }

        bgCircuitSegs = segList.toArray(new float[0][]);
        bgCircuitPads = padList.toArray(new float[0][]);

        // Reconstituer les traces (polylines) depuis segList
        // Chaque traceId produit 1 ou 2 segments consécutifs dans segList
        ArrayList<float[]> traceList = new ArrayList<>();
        int si = 0;
        while (si < bgCircuitSegs.length) {
            float[] s1 = bgCircuitSegs[si];
            if (si + 1 < bgCircuitSegs.length) {
                float[] s2 = bgCircuitSegs[si + 1];
                // Vérifier si s2 commence là où s1 se termine (coude)
                if (Math.abs(s2[0] - s1[2]) < 1f && Math.abs(s2[1] - s1[3]) < 1f) {
                    traceList.add(new float[]{s1[0], s1[1], s1[2], s1[3], s2[2], s2[3]});
                    si += 2;
                    continue;
                }
            }
            traceList.add(new float[]{s1[0], s1[1], s1[2], s1[3]});
            si++;
        }
        bgCircuitTraces = traceList.toArray(new float[0][]);
    }

    private boolean ccFree(int[][] grid, int x, int y, int cols, int rows, int id) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return false;
        if (grid[x][y] >= 0 && grid[x][y] != id) return false;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x+dx, ny = y+dy;
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows) {
                    int v = grid[nx][ny];
                    if (v >= 0 && v != id) return false;
                }
            }
        return true;
    }

    private void drawBgUrbanGrid(Canvas canvas) {
        canvas.drawColor(BG_COLOR_GRID);

        long  t       = System.currentTimeMillis();
        float gridOff = ((float)(t % 8334L) * 0.00012f + bgParallaxY * 0.0003f) % 1.0f;

        float horizonY = screenHeight * 0.42f;
        float fpX      = screenWidth  * 0.50f;
        // Point de fuite au-dessus de l'horizon — lignes convergent mais ne se croisent pas à l'écran
        float fpY      = horizonY - screenHeight * 0.12f;

        // Onde de surbrillance : remonte du bas vers l'horizon en boucle
        float waveProgress = (float)(t % 2858L) * 0.00035f;
        float waveY        = Math.max(horizonY, screenHeight - waveProgress * (screenHeight - horizonY));
        float waveH        = screenHeight * 0.12f;

        // ── Soleil trois-quarts vaporwave ──
        float sunR = screenWidth * 0.22f;
        canvas.save();
        gridSunClipPath.reset();
        gridSunClipPath.addCircle(fpX, horizonY, sunR, Path.Direction.CW);
        canvas.clipPath(gridSunClipPath);
        canvas.clipRect(0, horizonY - sunR, screenWidth, horizonY);

        gridSunMatrix.setScale(sunR, sunR);
        gridSunMatrix.postTranslate(fpX, horizonY);
        gridSunGradient.setLocalMatrix(gridSunMatrix);
        gridSunPaint.setShader(gridSunGradient);
        gridSunPaint.setAlpha(255);
        canvas.drawCircle(fpX, horizonY, sunR, gridSunPaint);

        // Bandes animées : montent de horizonY vers mi-hauteur, s'affinent et disparaissent
        gridSunPaint.setShader(null);
        gridSunPaint.setColor(BG_COLOR_GRID);
        float sunMid    = horizonY - sunR * 0.5f;
        float sunRange  = sunR * 0.5f;
        float bandPhase = (float)(t % 18182L) * 0.000055f;
        int   numBands  = 5;
        for (int b = 0; b < numBands; b++) {
            float phase = (bandPhase + (float) b / numBands) % 1.0f;
            float by    = horizonY - phase * sunRange;
            float bh    = sunR * 0.07f * (1f - phase * 0.85f);
            int   ba    = (int)(240f * (1f - phase * phase));
            if (bh < 1f || by < sunMid) continue;
            gridSunPaint.setAlpha(ba);
            canvas.drawRect(fpX - sunR, by - bh * 0.5f, fpX + sunR, by + bh * 0.5f, gridSunPaint);
        }
        canvas.restore();

        // ── Lignes horizontales ──
        int hLines = 12;
        for (int i = 0; i < hLines; i++) {
            float frac = (i + gridOff) / (float) hLines;
            float t2   = frac * frac * frac;
            float y    = horizonY + t2 * (screenHeight - horizonY + screenHeight * 0.08f);
            if (y > screenHeight * 1.05f || y < horizonY) continue;

            float dist  = Math.abs(y - waveY);
            float wave  = dist < waveH ? (1f - dist / waveH) * (1f - dist / waveH) : 0f;

            int aBase = (int) Math.min(220f, 70f + t2 * 150f);
            int a     = (int) Math.min(255f, aBase + wave * 160f);

            gridWidePaint.setStrokeWidth(screenWidth * (0.022f + wave * 0.010f));
            gridWidePaint.setColor(Color.argb(a / 5, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, gridWidePaint);

            gridMidPaint.setStrokeWidth(screenWidth * (0.010f + wave * 0.006f));
            gridMidPaint.setColor(Color.argb(a / 2, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, gridMidPaint);

            gridFinePaint.setStrokeWidth(screenWidth * (0.004f + wave * 0.003f));
            gridFinePaint.setColor(Color.argb(a, 80, 40, 220));
            canvas.drawLine(0, y, screenWidth, y, gridFinePaint);
        }

        // ── Lignes de fuite ──
        int vLines = 12;
        for (int i = 0; i <= vLines; i++) {
            float startX = -screenWidth * 2.0f + i * (screenWidth * 6.0f / vLines);
            float startY = screenHeight * 1.05f;

            float tInter = (horizonY - startY) / (fpY - startY);
            float endX   = startX + tInter * (fpX - startX);

            int aBase = (int) Math.max(40f, Math.min(200f,
                    70f + (1f - Math.abs(startX - screenWidth * 0.5f) / (screenWidth * 3.0f)) * 130f));

            gridWidePaint.setStrokeWidth(screenWidth * 0.022f);
            gridWidePaint.setColor(Color.argb(aBase / 5, 80, 40, 220));
            canvas.drawLine(startX, startY, endX, horizonY, gridWidePaint);
            gridMidPaint.setStrokeWidth(screenWidth * 0.010f);
            gridMidPaint.setColor(Color.argb(aBase / 2, 80, 40, 220));
            canvas.drawLine(startX, startY, endX, horizonY, gridMidPaint);
            gridFinePaint.setStrokeWidth(screenWidth * 0.004f);
            gridFinePaint.setColor(Color.argb(aBase, 80, 40, 220));
            canvas.drawLine(startX, startY, endX, horizonY, gridFinePaint);

            // Onde : épaisseur progressive (fuselée) — 7 sous-segments
            if (waveY > horizonY && waveY < startY) {
                float lineT    = (waveY - startY) / (horizonY - startY);
                float waveIntX = startX + lineT * (endX - startX);
                float dx = endX - startX, dy = horizonY - startY;
                float len  = (float) Math.hypot(dx, dy);
                float ux = dx / len, uy = dy / len;
                // Perpendiculaire à la ligne
                float px = -uy, py = ux;
                float half     = screenWidth * 0.018f;  // demi-longueur
                float maxWidth = screenWidth * 0.006f;  // largeur max au centre

                // Pointe haute, côté droit, pointe basse, côté gauche
                float tipTopX  = waveIntX - ux * half, tipTopY  = waveY - uy * half;
                float tipBotX  = waveIntX + ux * half, tipBotY  = waveY + uy * half;
                float midRX    = waveIntX + px * maxWidth, midRY = waveY + py * maxWidth;
                float midLX    = waveIntX - px * maxWidth, midLY = waveY - py * maxWidth;

                canvas.save();
                canvas.clipRect(0, horizonY, screenWidth, screenHeight * 1.1f);

                // Passe glow large
                gridWavePath.reset();
                float gw = maxWidth * 2.2f;
                float gTipTopX = waveIntX - ux * half, gTipTopY = waveY - uy * half;
                float gTipBotX = waveIntX + ux * half, gTipBotY = waveY + uy * half;
                float gMidRX = waveIntX + px * gw,  gMidRY = waveY + py * gw;
                float gMidLX = waveIntX - px * gw,  gMidLY = waveY - py * gw;
                gridWavePath.moveTo(gTipTopX, gTipTopY);
                gridWavePath.quadTo(gMidRX, gMidRY, gTipBotX, gTipBotY);
                gridWavePath.quadTo(gMidLX, gMidLY, gTipTopX, gTipTopY);
                gridWavePath.close();
                gridWidePaint.setStyle(Paint.Style.FILL);
                gridWidePaint.setColor(Color.argb(aBase / 4, 80, 40, 220));
                canvas.drawPath(gridWavePath, gridWidePaint);
                gridWidePaint.setStyle(Paint.Style.STROKE);

                // Passe mid
                gridWavePath.reset();
                gridWavePath.moveTo(tipTopX, tipTopY);
                gridWavePath.quadTo(midRX, midRY, tipBotX, tipBotY);
                gridWavePath.quadTo(midLX, midLY, tipTopX, tipTopY);
                gridWavePath.close();
                gridMidPaint.setStyle(Paint.Style.FILL);
                gridMidPaint.setColor(Color.argb(Math.min(255, aBase * 2), 80, 40, 220));
                canvas.drawPath(gridWavePath, gridMidPaint);
                gridMidPaint.setStyle(Paint.Style.STROKE);

                // Passe core fine (contour seulement pour l'éclat)
                gridFinePaint.setStyle(Paint.Style.STROKE);
                gridFinePaint.setStrokeWidth(screenWidth * 0.004f);
                gridFinePaint.setColor(Color.argb(255, 80, 40, 220));
                canvas.drawPath(gridWavePath, gridFinePaint);

                canvas.restore();
            }
        }

        // ── Ligne d'horizon ──
        gridWidePaint.setStrokeWidth(screenWidth * 0.030f);
        gridWidePaint.setColor(Color.argb(50, 80, 40, 220));
        canvas.drawLine(0, horizonY, screenWidth, horizonY, gridWidePaint);

        gridMidPaint.setStrokeWidth(screenWidth * 0.014f);
        gridMidPaint.setColor(Color.argb(120, 80, 40, 220));
        canvas.drawLine(0, horizonY, screenWidth, horizonY, gridMidPaint);

        gridFinePaint.setStrokeWidth(screenWidth * 0.006f);
        gridFinePaint.setColor(Color.argb(220, 80, 40, 220));
        canvas.drawLine(0, horizonY, screenWidth, horizonY, gridFinePaint);
    }

    // ══════════════════════════════════════════════════
    // NÉON — Tunnel
    // Vignette RadialGradient + parois légères
    // + néons circulaires aux joints
    // ~45 drawCircle/frame au lieu de ~170
    // ══════════════════════════════════════════════════

    private void drawBgUrbanTunnel(Canvas canvas) {
        canvas.drawColor(BG_COLOR_TUNNEL);
        float dt = 0.016f;
        bgTunnelTime += dt;

        float baseCx = screenWidth * 0.5f;
        float baseCy = screenHeight * 0.45f;
        float orbitR = Math.min(screenWidth, screenHeight) * TUNNEL_ORBIT_RADIUS;
        float maxRadius = Math.max(screenWidth, screenHeight) * 0.95f;
        float diagHalf = (float) Math.hypot(screenWidth, screenHeight) * 0.55f;

        // ── Vignette radiale (1 seul drawCircle via RadialGradient) ──
        if (tunnelVigGradient == null || tunnelVigW != screenWidth || tunnelVigH != screenHeight) {
            tunnelVigW = screenWidth;
            tunnelVigH = screenHeight;
            tunnelVigGradient = new RadialGradient(
                    0f, 0f, 1f,
                    new int[]{  0x00161820, 0x00161820, 0x28161820, 0x60161820, 0xAA161820 },
                    new float[]{ 0f,          0.30f,      0.55f,      0.78f,      1f },
                    Shader.TileMode.CLAMP);
            tunnelVigPaint.setStyle(Paint.Style.FILL);
            tunnelVigPaint.setShader(tunnelVigGradient);
        }

        float vigCx = baseCx + (float) Math.cos(bgTunnelTime * TUNNEL_ORBIT_SPEED) * orbitR * 0.15f;
        float vigCy = baseCy + (float) Math.sin(bgTunnelTime * TUNNEL_ORBIT_SPEED * 0.7f) * orbitR * 0.1f;
        tunnelVigMatrix.setScale(diagHalf, diagHalf);
        tunnelVigMatrix.postTranslate(vigCx, vigCy);
        tunnelVigGradient.setLocalMatrix(tunnelVigMatrix);
        canvas.drawCircle(vigCx, vigCy, diagHalf, tunnelVigPaint);

        // ── Calcul des positions de segment ──
        float segDepth = 1.0f / TUNNEL_SEGMENT_COUNT;
        float scroll = (bgTunnelTime * TUNNEL_SPEED) % segDepth;

        int totalBounds = TUNNEL_SEGMENT_COUNT + 2;
        float[] radii  = new float[totalBounds];
        float[] cxArr  = new float[totalBounds];
        float[] cyArr  = new float[totalBounds];
        float[] alphas = new float[totalBounds];

        for (int s = 0; s < totalBounds; s++) {
            float progress = s * segDepth + scroll;
            float birthTime = bgTunnelTime - progress / Math.max(TUNNEL_SPEED, 0.001f);
            float angle = birthTime * TUNNEL_ORBIT_SPEED;
            float parallax = Math.min(1f, progress * 1.5f);
            cxArr[s]  = baseCx + (float) Math.cos(angle) * orbitR * parallax;
            cyArr[s]  = baseCy + (float) Math.sin(angle * 0.7f) * orbitR * 0.7f * parallax;

            float eased = progress * progress;
            radii[s] = eased * maxRadius;

            float fadeIn  = Math.min(1f, progress * 3f);
            float fadeOut = progress < 0.55f ? 1f : Math.max(0f, 1f - (progress - 0.55f) * 2f);
            alphas[s] = fadeIn * fadeOut;
        }

        tunnelPaint.setStyle(Paint.Style.STROKE);

        // ── Parois (2 passes par segment au lieu de 13) ──
        for (int s = totalBounds - 2; s >= 0; s--) {
            float a = (alphas[s] + alphas[s + 1]) * 0.5f;
            if (a <= 0.01f) continue;

            float rInner = radii[s];
            float rOuter = radii[s + 1];
            float wallThick = rOuter - rInner;
            if (wallThick < 4f) continue;

            float wCx = (cxArr[s] + cxArr[s + 1]) * 0.5f;
            float wCy = (cyArr[s] + cyArr[s + 1]) * 0.5f;
            float wR  = (rInner + rOuter) * 0.5f;

            // Passe large (bords diffus grâce au stroke épais + alpha faible)
            tunnelPaint.setStrokeWidth(wallThick * 0.95f);
            tunnelPaint.setColor(Color.argb((int)(a * 18), 35, 38, 50));
            canvas.drawCircle(wCx, wCy, wR, tunnelPaint);

            // Passe étroite au centre (cœur plus visible)
            tunnelPaint.setStrokeWidth(wallThick * 0.45f);
            tunnelPaint.setColor(Color.argb((int)(a * 12), 45, 48, 60));
            canvas.drawCircle(wCx, wCy, wR, tunnelPaint);
        }

        // ── Néons aux joints (3 passes par segment) ──
        for (int s = 0; s < totalBounds; s++) {
            float a = alphas[s];
            if (a <= 0.01f || radii[s] < 2f) continue;

            float cx = cxArr[s];
            float cy = cyArr[s];
            float r  = radii[s];

            int colorIdx  = s % TUNNEL_COLORS.length;
            int baseColor = TUNNEL_COLORS[colorIdx];
            int cr = (baseColor >> 16) & 0xFF;
            int cg = (baseColor >> 8) & 0xFF;
            int cb = baseColor & 0xFF;

            // Halo large (illumine les parois adjacentes)
            tunnelPaint.setStrokeWidth(screenWidth * 0.045f);
            tunnelPaint.setColor(Color.argb((int)(a * 16), cr, cg, cb));
            canvas.drawCircle(cx, cy, r, tunnelPaint);

            // Mid glow
            tunnelPaint.setStrokeWidth(screenWidth * 0.012f);
            tunnelPaint.setColor(Color.argb((int)(a * 70), cr, cg, cb));
            canvas.drawCircle(cx, cy, r, tunnelPaint);

            // Core
            tunnelPaint.setStrokeWidth(screenWidth * 0.003f);
            tunnelPaint.setColor(Color.argb((int)(a * 200), cr, cg, cb));
            canvas.drawCircle(cx, cy, r, tunnelPaint);
        }

        // ── Point de fuite (2 cercles au lieu de 3) ──
        float pulse = 0.08f + 0.03f * (float) Math.sin(bgTunnelTime * 0.5);
        float glowR = Math.min(screenWidth, screenHeight) * 0.06f;
        tunnelPaint.setStyle(Paint.Style.FILL);
        tunnelPaint.setColor(Color.argb((int)(pulse * 55), 255, 238, 180));
        canvas.drawCircle(vigCx, vigCy, glowR * 2f, tunnelPaint);
        tunnelPaint.setColor(Color.argb((int)(pulse * 160), 255, 248, 220));
        canvas.drawCircle(vigCx, vigCy, glowR * 0.5f, tunnelPaint);
        tunnelPaint.setStyle(Paint.Style.STROKE);
    }

    // ══════════════════════════════════════════════════
    // NÉON — Equalizer
    // 1 seul ovale RadialGradient par barre = pas de seam
    // ColorFilters cachés = pas d'allocation par frame
    // ══════════════════════════════════════════════════

    private void drawBgUrbanEqualizer(Canvas canvas) {
        canvas.drawColor(BG_COLOR_EQUALIZER);
        float dt = 0.016f;
        bgEqTime += dt;

        float barTotalW = screenWidth / (float) EQ_BAR_COUNT;
        float barW = barTotalW * 0.72f;
        float gap = barTotalW * 0.28f;
        float maxBarH = screenHeight * 0.75f;
        float baseY = screenHeight * 0.95f;

        if (bgEqBarHeights == null) {
            bgEqBarHeights = new float[EQ_BAR_COUNT];
            bgEqBarTargets = new float[EQ_BAR_COUNT];
            bgEqBarSpeeds  = new float[EQ_BAR_COUNT];
            bgEqBarPhases  = new float[EQ_BAR_COUNT];
            eqCachedFilters    = new PorterDuffColorFilter[EQ_BAR_COUNT];
            eqCachedFilterKeys = new int[EQ_BAR_COUNT];
            for (int i = 0; i < EQ_BAR_COUNT; i++) {
                bgEqBarPhases[i] = (float)(Math.random() * Math.PI * 2);
                bgEqBarSpeeds[i] = 0.8f + (float)(Math.random() * 1.2f);
            }
        }

        for (int i = 0; i < EQ_BAR_COUNT; i++) {
            float center = (float) i / EQ_BAR_COUNT;
            float dist = Math.abs(center - 0.5f) * 2f;
            float envelope = 1f - dist * dist * 0.6f;

            float wave1 = (float) Math.sin(bgEqTime * bgEqBarSpeeds[i] * 2.0 + bgEqBarPhases[i]);
            float wave2 = (float) Math.sin(bgEqTime * 1.3 + i * 0.4);
            float wave3 = (float) Math.sin(bgEqTime * 0.7 + i * 0.15);
            float combined = (wave1 * 0.5f + wave2 * 0.3f + wave3 * 0.2f) * 0.5f + 0.5f;

            bgEqBarTargets[i] = combined * envelope * maxBarH;
            bgEqBarTargets[i] = Math.max(maxBarH * 0.03f, bgEqBarTargets[i]);

            float diff = bgEqBarTargets[i] - bgEqBarHeights[i];
            float speed = diff > 0 ? 8f : 3f;
            bgEqBarHeights[i] += diff * dt * speed;
        }

        // ── Passe glow (shader identique pour toutes les barres) ──
        eqGlowPaint.setShader(eqGlowGrad);
        for (int i = 0; i < EQ_BAR_COUNT; i++) {
            float x = gap * 0.5f + i * barTotalW;
            float h = bgEqBarHeights[i];
            float topY = baseY - h;
            float barCx = x + barW * 0.5f;
            float barCy = topY + h * 0.35f;

            float frac = (float) i / (EQ_BAR_COUNT - 1);
            float hNorm = h / maxBarH;
            float hNorm2 = hNorm * hNorm;

            int cr, cg, cb;
            if (frac < 0.5f) {
                float t = frac * 2f;
                cr = (int)(t * 0xFF);
                cg = (int)(0xFF + t * (0xDD - 0xFF));
                cb = (int)(0x88 + t * (0x00 - 0x88));
            } else {
                float t = (frac - 0.5f) * 2f;
                cr = 0xFF;
                cg = (int)(0xDD + t * (0x22 - 0xDD));
                cb = (int)(t * 0x44);
            }

            int litR = Math.min(255, cr + (int)((255 - cr) * hNorm2 * 0.6f));
            int litG = Math.min(255, cg + (int)((255 - cg) * hNorm2 * 0.6f));
            int litB = Math.min(255, cb + (int)((255 - cb) * hNorm2 * 0.6f));

            int glowAlpha = (int)(35 + hNorm2 * 185);

            // ColorFilter caché : quantize sur 8 niveaux par canal
            int key = ((glowAlpha >> 5) << 24)
                    | ((litR >> 5) << 16)
                    | ((litG >> 5) << 8)
                    |  (litB >> 5);
            if (eqCachedFilterKeys[i] != key) {
                eqCachedFilterKeys[i] = key;
                eqCachedFilters[i] = new PorterDuffColorFilter(
                        Color.argb(glowAlpha, litR, litG, litB),
                        PorterDuff.Mode.SRC_IN);
            }

            float ovalRadX = barW * (2f + hNorm * 3.5f);
            float ovalRadY = h * (0.55f + hNorm * 0.25f) + barW;

            eqGlowMatrix.setScale(ovalRadX, ovalRadY);
            eqGlowMatrix.postTranslate(barCx, barCy);
            eqGlowGrad.setLocalMatrix(eqGlowMatrix);
            eqGlowPaint.setColorFilter(eqCachedFilters[i]);
            eqGlowRect.set(barCx - ovalRadX, barCy - ovalRadY,
                    barCx + ovalRadX, barCy + ovalRadY);
            canvas.drawOval(eqGlowRect, eqGlowPaint);
        }
        eqGlowPaint.setColorFilter(null);

        // ── Passe barres (pas de shader, pas de filter) ──
        eqGlowPaint.setShader(null);
        for (int i = 0; i < EQ_BAR_COUNT; i++) {
            float x = gap * 0.5f + i * barTotalW;
            float h = bgEqBarHeights[i];
            float topY = baseY - h;

            float frac = (float) i / (EQ_BAR_COUNT - 1);
            float hNorm = h / maxBarH;
            float hNorm2 = hNorm * hNorm;

            int cr, cg, cb;
            if (frac < 0.5f) {
                float t = frac * 2f;
                cr = (int)(t * 0xFF);
                cg = (int)(0xFF + t * (0xDD - 0xFF));
                cb = (int)(0x88 + t * (0x00 - 0x88));
            } else {
                float t = (frac - 0.5f) * 2f;
                cr = 0xFF;
                cg = (int)(0xDD + t * (0x22 - 0xDD));
                cb = (int)(t * 0x44);
            }

            int litR = Math.min(255, cr + (int)((255 - cr) * hNorm2 * 0.6f));
            int litG = Math.min(255, cg + (int)((255 - cg) * hNorm2 * 0.6f));
            int litB = Math.min(255, cb + (int)((255 - cb) * hNorm2 * 0.6f));

            int barAlpha = (int)(120 + hNorm * 135);
            eqBarPaint.setColor(Color.argb(barAlpha, litR, litG, litB));
            canvas.drawRect(x, topY, x + barW, baseY, eqBarPaint);

            float capH = screenWidth * (0.003f + hNorm2 * 0.005f);
            int capR = Math.min(255, litR + (int)((255 - litR) * 0.5f));
            int capG = Math.min(255, litG + (int)((255 - litG) * 0.5f));
            int capB = Math.min(255, litB + (int)((255 - litB) * 0.5f));
            eqBarPaint.setColor(Color.argb((int)(180 + hNorm * 75), capR, capG, capB));
            canvas.drawRect(x, topY, x + barW, topY + capH, eqBarPaint);
        }
    }

    private void drawDefaultBackground(Canvas canvas, float currentHeight) {
        int skyBlue    = Color.parseColor("#87CEEB");
        int darkBlue   = Color.parseColor("#00008B");
        int spaceBlack = Color.parseColor("#000000");

        int finalColor;
        if (currentHeight <= 1000f) {
            finalColor = interpolateColor(skyBlue, darkBlue, currentHeight / 1000f);
        } else if (currentHeight <= 3000f) {
            finalColor = interpolateColor(darkBlue, spaceBlack, (currentHeight - 1000f) / 2000f);
        } else {
            finalColor = spaceBlack;
        }
        canvas.drawColor(finalColor);

        drawDefaultGround(canvas, currentHeight);
        drawDefaultClouds(canvas, currentHeight);
        drawDefaultStars(canvas, currentHeight);
    }

    private void drawDefaultGround(Canvas canvas, float currentHeight) {
        if (currentHeight > 40f) return;
        float groundH = screenHeight * 0.06f;
        float alpha255 = Math.max(0f, 1f - currentHeight / 40f) * 255f;
        defaultBgPaint.setStyle(Paint.Style.FILL);
        defaultBgPaint.setColor(Color.argb((int) alpha255,
                34, 139, 34));
        canvas.drawRect(0, screenHeight - groundH, screenWidth, screenHeight, defaultBgPaint);
    }

    private void drawDefaultClouds(Canvas canvas, float currentHeight) {
        if (currentHeight < 100f || currentHeight > 900f) return;

        if (bgClouds == null) bgClouds = new ArrayList<>();

        long now = System.currentTimeMillis();

        if (now - lastCloudSpawnMs > 3200L && bgClouds.size() < 6) {
            lastCloudSpawnMs = now;

            float w = screenWidth * (0.24f + (float) Math.random() * 0.22f);
            float h = w * (0.30f + (float) Math.random() * 0.08f);

            float y = screenHeight * (0.14f + (float) Math.random() * 0.58f);

            float speed = 0.35f + (float) Math.random() * 0.45f;
            if ((float) Math.random() < 0.5f) speed = -speed;

            float startX = speed > 0 ? -w * 1.2f : screenWidth + w * 0.2f;

            // Beaucoup moins transparent qu’avant.
            // L’alpha est appliqué une seule fois au nuage entier.
            float alpha = 215f + (float) Math.random() * 35f;

            int shape = (int) (Math.random() * 3f);

            bgClouds.add(new float[]{ startX, y, w, h, speed, alpha, shape });
        }

        java.util.Iterator<float[]> it = bgClouds.iterator();

        while (it.hasNext()) {
            float[] c = it.next();

            c[0] += c[4];

            if ((c[4] > 0 && c[0] > screenWidth + c[2] * 1.3f)
                    || (c[4] < 0 && c[0] < -c[2] * 1.3f)) {
                it.remove();
                continue;
            }

            drawCloud(canvas, c[0], c[1], c[2], c[3], (int) c[5], (int) c[6]);
        }
    }

    private void drawCloud(Canvas canvas, float x, float y, float w, float h,
                           int alpha, int shape) {
        cloudLayerPaint.setAlpha(alpha);

        float left   = x - w * 0.08f;
        float top    = y - h * 0.85f;
        float right  = x + w * 1.08f;
        float bottom = y + h * 0.75f;

        int save = canvas.saveLayer(left, top, right, bottom, cloudLayerPaint);

        // Ombre douce interne, opaque dans le layer.
        // Elle ne s'accumulera pas avec l'alpha global du nuage.
        cloudPaint.setStyle(Paint.Style.FILL);
        cloudPaint.setColor(Color.rgb(225, 235, 245));

        canvas.drawOval(
                x + w * 0.08f,
                y - h * 0.04f,
                x + w * 0.92f,
                y + h * 0.58f,
                cloudPaint
        );

        // Corps principal du nuage.
        cloudPaint.setColor(Color.WHITE);

        if (shape == 0) {
            canvas.drawOval(x + w * 0.02f, y - h * 0.02f, x + w * 0.34f, y + h * 0.48f, cloudPaint);
            canvas.drawOval(x + w * 0.20f, y - h * 0.45f, x + w * 0.58f, y + h * 0.42f, cloudPaint);
            canvas.drawOval(x + w * 0.48f, y - h * 0.35f, x + w * 0.86f, y + h * 0.46f, cloudPaint);
            canvas.drawOval(x + w * 0.68f, y - h * 0.02f, x + w * 1.00f, y + h * 0.48f, cloudPaint);

            canvas.drawRoundRect(
                    x + w * 0.08f,
                    y + h * 0.08f,
                    x + w * 0.92f,
                    y + h * 0.58f,
                    h * 0.28f,
                    h * 0.28f,
                    cloudPaint
            );
        } else if (shape == 1) {
            canvas.drawOval(x + w * 0.00f, y + h * 0.05f, x + w * 0.28f, y + h * 0.45f, cloudPaint);
            canvas.drawOval(x + w * 0.18f, y - h * 0.30f, x + w * 0.46f, y + h * 0.40f, cloudPaint);
            canvas.drawOval(x + w * 0.36f, y - h * 0.55f, x + w * 0.70f, y + h * 0.38f, cloudPaint);
            canvas.drawOval(x + w * 0.62f, y - h * 0.22f, x + w * 0.94f, y + h * 0.44f, cloudPaint);
            canvas.drawOval(x + w * 0.78f, y + h * 0.08f, x + w * 1.06f, y + h * 0.45f, cloudPaint);

            canvas.drawRoundRect(
                    x + w * 0.07f,
                    y + h * 0.07f,
                    x + w * 0.98f,
                    y + h * 0.57f,
                    h * 0.30f,
                    h * 0.30f,
                    cloudPaint
            );
        } else {
            canvas.drawOval(x + w * 0.04f, y + h * 0.04f, x + w * 0.30f, y + h * 0.42f, cloudPaint);
            canvas.drawOval(x + w * 0.20f, y - h * 0.28f, x + w * 0.50f, y + h * 0.38f, cloudPaint);
            canvas.drawOval(x + w * 0.42f, y - h * 0.42f, x + w * 0.76f, y + h * 0.40f, cloudPaint);
            canvas.drawOval(x + w * 0.68f, y - h * 0.08f, x + w * 1.02f, y + h * 0.46f, cloudPaint);

            canvas.drawRoundRect(
                    x + w * 0.08f,
                    y + h * 0.10f,
                    x + w * 0.96f,
                    y + h * 0.56f,
                    h * 0.32f,
                    h * 0.32f,
                    cloudPaint
            );
        }

        canvas.restoreToCount(save);
    }

    private void drawDefaultStars(Canvas canvas, float currentHeight) {
        if (currentHeight < 2500f) return;

        if (bgStars == null) bgStars = new ArrayList<>();

        float density = Math.min(1f, (currentHeight - 2500f) / 3500f);

        long now = System.currentTimeMillis();

        long spawnInterval = (long) (700f - density * 520f);
        int maxStars = (int) (6 + density * 18f);

        if (now - lastStarSpawnMs > spawnInterval && bgStars.size() < maxStars) {
            lastStarSpawnMs = now;

            float sx = (float) Math.random() * screenWidth;
            float sy = (float) Math.random() * screenHeight;

            int maxTimer = 45 + (int) (Math.random() * 55); // durée plus douce

            float size = 0.8f + (float) Math.random() * 1.8f;
            float sparkle = (float) Math.random() < 0.22f ? 1f : 0f;

            bgStars.add(new float[]{ sx, sy, 0f, maxTimer, size, sparkle });
        }

        java.util.Iterator<float[]> it = bgStars.iterator();

        while (it.hasNext()) {
            float[] s = it.next();

            s[2]++;

            if (s[2] >= s[3]) {
                it.remove();
                continue;
            }

            float progress = s[2] / s[3];

            // Sinus : apparition/disparition plus naturelle qu’un triangle linéaire.
            float alphaF = (float) Math.sin(progress * Math.PI);
            int alpha = (int) (alphaF * 220f);

            float radius = s[4] * (0.65f + alphaF * 0.45f);

            starPaint.setStyle(Paint.Style.FILL);
            starPaint.setColor(Color.argb(alpha, 255, 250, 220));
            canvas.drawCircle(s[0], s[1], radius, starPaint);

            // Quelques étoiles seulement ont une petite croix.
            if (s[5] > 0.5f) {
                starPaint.setStyle(Paint.Style.STROKE);
                starPaint.setStrokeWidth(1f);
                starPaint.setStrokeCap(Paint.Cap.ROUND);
                starPaint.setColor(Color.argb((int) (alpha * 0.75f), 255, 250, 220));

                float len = radius * 3.2f;

                canvas.drawLine(s[0] - len, s[1], s[0] + len, s[1], starPaint);
                canvas.drawLine(s[0], s[1] - len, s[0], s[1] + len, starPaint);
            }
        }
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        float invRatio = 1f - ratio;
        float r = (Color.red(color1) * invRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * invRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * invRatio) + (Color.blue(color2) * ratio);
        return Color.rgb((int)r, (int)g, (int)b);
    }
}