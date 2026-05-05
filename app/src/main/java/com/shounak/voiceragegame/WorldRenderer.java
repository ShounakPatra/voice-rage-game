package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class WorldRenderer {
    private final GameView gv;
    private final SettingsManager sm;
    private final Path scratchPath = new Path();
    private final Path scratchPath2 = new Path();
    private final Path scratchPath3 = new Path();

    public WorldRenderer(GameView gv, SettingsManager sm) {
        this.gv = gv;
        this.sm = sm;
    }

    public void drawScoreDrivenSky(Canvas canvas, Paint paint, Paint auxPaint, int screenWidth, int groundY, float daylight, boolean highSunIntensity) {
        if (screenWidth <= 0 || groundY <= 0) {
            canvas.drawColor(Color.rgb(10, 12, 48));
            return;
        }
        if (highSunIntensity) {
            daylight = clamp01(daylight * 1.18f + 0.04f);
        }

        int top = mixColor(Color.rgb(4, 6, 22), Color.rgb(74, 184, 255), daylight);
        int mid = mixColor(Color.rgb(10, 12, 48), Color.rgb(92, 198, 255), daylight);
        int horizon = mixColor(Color.rgb(38, 28, 95), Color.rgb(156, 222, 255), daylight);
        auxPaint.setShader(new android.graphics.LinearGradient(
                0, 0, 0, groundY,
                new int[]{top, mid, horizon},
                new float[]{0f, 0.52f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, screenWidth, groundY, auxPaint);
        auxPaint.setShader(null);

        int glowR = (int) lerp(118, 255, daylight);
        int glowG = (int) lerp(72, 218, daylight);
        int glowB = (int) lerp(180, 150, daylight);
        auxPaint.setShader(new android.graphics.LinearGradient(
                0, groundY - 150f, 0, groundY,
                Color.argb(0, glowR, glowG, glowB),
                Color.argb((int) lerp(58, 134, daylight), glowR, glowG, glowB),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, groundY - 150f, screenWidth, groundY, auxPaint);
        auxPaint.setShader(null);
    }

    private float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int mixColor(int from, int to, float t) {
        float clamped = clamp01(t);
        return Color.rgb(
                (int) lerp(Color.red(from), Color.red(to), clamped),
                (int) lerp(Color.green(from), Color.green(to), clamped),
                (int) lerp(Color.blue(from), Color.blue(to), clamped));
    }

    public void drawRealisticCloud(Canvas canvas, Paint cloudPaint, float cx, float cy, float dimAlpha, boolean premiumRendering) {
        int shadowA = (int)(gv.shadowAlpha(28) * dimAlpha);
        int bodyA = (int)(38 * dimAlpha);
        int hiA = (int)(22 * dimAlpha);

        if (gv.shouldDrawShadows() && shadowA > 2) {
            cloudPaint.setColor(Color.argb(shadowA, 140, 150, 180));
            canvas.drawOval(new RectF(cx - 10, cy + 22, cx + 200, cy + 70), cloudPaint);
        }

        int[][] ovals = {
                {20, 10, 160, 55},
                {0, 18, 100, 56},
                {80, 5, 185, 52},
                {50, 0, 155, 48},
                {35, 15, 130, 52},
                {95, 12, 195, 52},
        };
        for (int[] o : ovals) {
            cloudPaint.setColor(Color.argb(bodyA, 200, 210, 240));
            canvas.drawOval(new RectF(cx + o[0], cy + o[1], cx + o[2], cy + o[3]), cloudPaint);
        }
        cloudPaint.setColor(Color.argb(hiA, 255, 255, 255));
        canvas.drawOval(new RectF(cx + 55, cy + 2, cx + 148, cy + 32), cloudPaint);
        canvas.drawOval(new RectF(cx + 5, cy + 20, cx + 90, cy + 46), cloudPaint);
    }

    public void drawCelestialCycle(Canvas canvas, Paint paint, Paint auxPaint,
                                   int screenWidth, int groundY, int score,
                                   long now, boolean premiumRendering,
                                   boolean highSunIntensity, int celestialCycleScore) {
        float phase = getScoreCyclePhase(score, celestialCycleScore);
        float segment = phase < 0.5f ? phase * 2f : (phase - 0.5f) * 2f;
        float ease = MathUtils.smoothStep(segment);
        float daylight = getDaylightAmount(score, celestialCycleScore);

        float sunX = screenWidth * (phase < 0.5f
                ? lerp(0.14f, 0.52f, ease)
                : lerp(0.52f, 0.88f, ease));
        float sunY = phase < 0.5f
                ? lerp(groundY + 130f, groundY * 0.20f, ease)
                : lerp(groundY * 0.20f, groundY + 130f, ease);
        float moonX = screenWidth * (phase < 0.5f
                ? lerp(0.82f, 0.14f, ease)
                : lerp(0.14f, 0.82f, ease));
        float moonY = phase < 0.5f
                ? lerp(groundY * 0.22f, groundY + 142f, ease)
                : lerp(groundY + 142f, groundY * 0.22f, ease);

        float sunAlpha = clamp01(daylight);
        float moonAlpha = clamp01(1f - daylight);
        float sunR = Math.max(46f, Math.min(72f, screenWidth * 0.055f));
        float moonR = Math.max(42f, Math.min(58f, screenWidth * 0.044f));
        if (highSunIntensity) {
            sunAlpha = clamp01(sunAlpha * 1.28f);
            sunR *= 1.12f;
        }

        if (sunAlpha > 0.015f) {
            auxPaint.setShader(new android.graphics.RadialGradient(
                    sunX, sunY, sunR * 5.6f,
                    new int[]{
                            Color.argb((int) (150 * sunAlpha), 255, 232, 140),
                            Color.argb((int) (58 * sunAlpha), 255, 164, 56),
                            Color.TRANSPARENT
                    },
                    new float[]{0f, 0.45f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(sunX, sunY, sunR * 5.6f, auxPaint);
            auxPaint.setShader(null);

            if (premiumRendering) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(3.8f);
                for (int i = 0; i < 12; i++) {
                    float angle = (float) (i * Math.PI / 6.0 + Math.sin(now * 0.0011) * 0.06f);
                    float inner = sunR * 1.22f;
                    float outer = sunR * (1.85f + (i % 2) * 0.35f);
                    paint.setColor(Color.argb((int) (88 * sunAlpha), 255, 222, 116));
                    canvas.drawLine(sunX + (float) Math.cos(angle) * inner,
                            sunY + (float) Math.sin(angle) * inner,
                            sunX + (float) Math.cos(angle) * outer,
                            sunY + (float) Math.sin(angle) * outer, paint);
                }
                paint.setStrokeCap(Paint.Cap.BUTT);
                paint.setStyle(Paint.Style.FILL);
            }

            auxPaint.setShader(new android.graphics.RadialGradient(
                    sunX - sunR * 0.28f, sunY - sunR * 0.32f, sunR * 1.35f,
                    new int[]{
                            Color.argb((int) (255 * sunAlpha), 255, 252, 204),
                            Color.argb((int) (255 * sunAlpha), 255, 191, 64),
                            Color.argb((int) (255 * sunAlpha), 232, 92, 22)
                    },
                    new float[]{0f, 0.58f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(sunX, sunY, sunR, auxPaint);
            auxPaint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.2f);
            paint.setColor(Color.argb((int) (140 * sunAlpha), 255, 255, 218));
            canvas.drawCircle(sunX, sunY, sunR - 2f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        if (moonAlpha > 0.015f) {
            auxPaint.setShader(new android.graphics.RadialGradient(
                    moonX, moonY, moonR * 3.6f,
                    new int[]{
                            Color.argb((int) (92 * moonAlpha), 255, 248, 200),
                            Color.argb((int) (34 * moonAlpha), 168, 196, 255),
                            Color.TRANSPARENT
                    },
                    new float[]{0f, 0.58f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(moonX, moonY, moonR * 3.6f, auxPaint);
            auxPaint.setShader(null);

            auxPaint.setShader(new android.graphics.RadialGradient(
                    moonX - moonR * 0.25f, moonY - moonR * 0.28f, moonR * 1.1f,
                    new int[]{
                            Color.argb((int) (255 * moonAlpha), 255, 252, 230),
                            Color.argb((int) (245 * moonAlpha), 236, 232, 199),
                            Color.argb((int) (230 * moonAlpha), 188, 194, 210)
                    },
                    new float[]{0f, 0.62f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawCircle(moonX, moonY, moonR, auxPaint);
            auxPaint.setShader(null);

            paint.setColor(Color.argb((int) (56 * moonAlpha), 20, 15, 60));
            canvas.drawCircle(moonX + moonR * 0.22f, moonY + moonR * 0.08f, moonR * 0.95f, paint);

            if (premiumRendering) {
                int[][] craters = {{-18, -14, 9}, {12, 8, 6}, {-6, 18, 5}, {20, -20, 4}, {-22, 10, 4}};
                for (int[] cr : craters) {
                    float crX = moonX + cr[0] * (moonR / 52f);
                    float crY = moonY + cr[1] * (moonR / 52f);
                    float crR = cr[2] * (moonR / 52f);
                    paint.setColor(Color.argb((int) (40 * moonAlpha), 120, 112, 95));
                    canvas.drawCircle(crX, crY, crR, paint);
                    paint.setColor(Color.argb((int) (24 * moonAlpha), 255, 255, 220));
                    canvas.drawCircle(crX - crR * 0.3f, crY - crR * 0.3f, crR * 0.55f, paint);
                }
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(Color.argb((int) (96 * moonAlpha), 255, 252, 220));
            canvas.drawCircle(moonX, moonY, moonR - 1.5f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    public void drawUltraRealisticMountains(Canvas canvas, Paint paint, Paint auxPaint,
                                            int screenWidth, int groundY, int score,
                                            boolean premiumRendering) {
        float baseY = groundY + 10f;
        drawMountainRange(canvas, paint, auxPaint, screenWidth * 0.74f, baseY,
                groundY * 0.31f, score * 0.08f,
                Color.rgb(33, 43, 77), Color.rgb(17, 22, 48),
                Color.argb(120, 182, 196, 220), 0.18f, premiumRendering);
        drawMountainRange(canvas, paint, auxPaint, screenWidth * 0.58f, baseY + 4f,
                groundY * 0.40f, score * 0.14f,
                Color.rgb(55, 57, 88), Color.rgb(22, 23, 52),
                Color.argb(150, 232, 236, 242), 0.32f, premiumRendering);
        if (premiumRendering) {
            drawMountainRange(canvas, paint, auxPaint, screenWidth * 0.46f, baseY + 8f,
                    groundY * 0.47f, score * 0.22f,
                    Color.rgb(79, 67, 82), Color.rgb(28, 24, 45),
                    Color.argb(180, 246, 240, 224), 0.48f, true);
        }

        auxPaint.setShader(new android.graphics.LinearGradient(
                0, groundY - 96f, 0, groundY + 14f,
                Color.argb(0, 255, 230, 170),
                Color.argb(92, 229, 178, 95),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, groundY - 96f, screenWidth, groundY + 14f, auxPaint);
        auxPaint.setShader(null);
    }

    public void drawBackgroundDunes(Canvas canvas, Paint paint, Paint auxPaint,
                                    int screenWidth, int screenHeight, int groundY,
                                    int score, boolean premiumRendering) {
        float scroll = score * 0.55f;
        float baseY = groundY + 8f;
        int layers = premiumRendering ? 3 : 2;
        for (int layer = 0; layer < layers; layer++) {
            float depth = (layer + 1f) / layers;
            float height = 28f + depth * 42f;
            float spacing = screenWidth * (0.62f - depth * 0.08f);
            float offset = (scroll * (0.22f + depth * 0.32f)) % spacing;
            for (float x = -spacing - offset; x < screenWidth + spacing; x += spacing) {
                scratchPath.reset();
                float crestY = groundY - 42f - depth * 20f
                        + (float) Math.sin((x + scroll) * 0.006f + layer) * 5f;
                scratchPath.moveTo(x - spacing * 0.2f, baseY);
                scratchPath.cubicTo(x + spacing * 0.12f, crestY - height * 0.38f,
                        x + spacing * 0.60f, crestY - height * 0.52f,
                        x + spacing * 1.18f, baseY - height * 0.05f);
                scratchPath.lineTo(x + spacing * 1.18f, baseY);
                scratchPath.close();
                int alpha = (int) (70 + depth * 64);
                auxPaint.setShader(new android.graphics.LinearGradient(
                        x, crestY - height, x + spacing, baseY,
                        Color.argb(alpha, 232, 194, 112),
                        Color.argb(alpha, 150, 98, 52),
                        android.graphics.Shader.TileMode.CLAMP));
                canvas.drawPath(scratchPath, auxPaint);
                auxPaint.setShader(null);
                if (gv.shouldDrawShadows()) {
                    paint.setColor(Color.argb(gv.shadowAlpha((int) (26 + depth * 28)), 80, 46, 28));
                    canvas.drawLine(x + spacing * 0.48f, crestY - height * 0.42f,
                            x + spacing * 1.04f, baseY - height * 0.06f, paint);
                }
            }
        }
    }

    public void drawPremiumGround(Canvas canvas, Paint paint, Paint auxPaint,
                                  int screenWidth, int screenHeight, int groundY,
                                  int score, AudioEngine.VoiceLevel level,
                                  boolean premiumRendering) {
        float topY = groundY - 2f;
        float bottomY = screenHeight;
        float scroll = score * 3.4f;

        auxPaint.setShader(new android.graphics.LinearGradient(
                0, topY, 0, bottomY,
                new int[]{
                        Color.rgb(224, 186, 103),
                        Color.rgb(188, 139, 67),
                        Color.rgb(126, 85, 44)
                },
                new float[]{0f, 0.46f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0, topY, screenWidth, bottomY, auxPaint);
        auxPaint.setShader(null);

        drawDuneLayer(canvas, paint, auxPaint, screenWidth, screenHeight,
                topY + 18f, 30f, screenWidth * 0.72f, scroll * 0.28f,
                Color.rgb(222, 185, 104), Color.rgb(169, 116, 58), 78);
        drawDuneLayer(canvas, paint, auxPaint, screenWidth, screenHeight,
                topY + 42f, 46f, screenWidth * 0.56f, scroll * 0.55f,
                Color.rgb(232, 195, 112), Color.rgb(151, 95, 47), 105);
        drawDuneLayer(canvas, paint, auxPaint, screenWidth, screenHeight,
                topY + 72f, 70f, screenWidth * 0.43f, scroll * 0.92f,
                Color.rgb(245, 208, 122), Color.rgb(118, 74, 38), 132);

        drawSandRipples(canvas, paint, screenWidth, topY, bottomY, scroll, level, premiumRendering);
        drawMovingSandDetail(canvas, paint, screenWidth, topY, bottomY, scroll, premiumRendering);
    }

    private float getScoreCyclePhase(int score, int celestialCycleScore) {
        int runScore = Math.max(0, score / 10);
        int cycleScore = celestialCycleScore * 2;
        return (runScore % cycleScore) / (float) cycleScore;
    }

    private float getDaylightAmount(int score, int celestialCycleScore) {
        int runScore = Math.max(0, score / 10);
        int cycleScore = celestialCycleScore * 2;
        int phaseScore = runScore % cycleScore;
        float raw = phaseScore <= celestialCycleScore
                ? phaseScore / (float) celestialCycleScore
                : 1f - ((phaseScore - celestialCycleScore) / (float) celestialCycleScore);
        return MathUtils.smoothStep(raw);
    }

    private void drawMountainRange(Canvas canvas, Paint paint, Paint auxPaint,
                                   float width, float baseY, float height, float scroll,
                                   int lightColor, int shadowColor, int snowColor,
                                   float detailAlpha, boolean premiumRendering) {
        float spacing = Math.max(220f, width);
        float offset = scroll % spacing;
        int count = premiumRendering ? 5 : 4;
        for (int i = -2; i < count; i++) {
            float left = i * spacing - offset;
            float seed = i * 0.63f + width * 0.001f;
            float peakBias = 0.38f + 0.2f * (float) Math.sin(seed * 2.1f);
            float peakX = left + spacing * peakBias;
            float peakY = baseY - height * (0.74f + 0.22f * (float) Math.cos(seed * 1.7f));
            drawMountainMass(canvas, paint, auxPaint, left, spacing, baseY, peakX, peakY,
                    lightColor, shadowColor, snowColor, detailAlpha, seed, premiumRendering);
        }
    }

    private void drawMountainMass(Canvas canvas, Paint paint, Paint auxPaint,
                                  float left, float width, float baseY, float peakX,
                                  float peakY, int lightColor, int shadowColor,
                                  int snowColor, float detailAlpha, float seed,
                                  boolean premiumRendering) {
        float right = left + width;
        float shoulderL = left + width * (0.12f + 0.05f * (float) Math.sin(seed));
        float shoulderR = left + width * (0.78f + 0.08f * (float) Math.cos(seed * 1.3f));
        float ridgeL = left + width * (0.24f + 0.06f * (float) Math.cos(seed * 1.9f));
        float ridgeR = left + width * (0.61f + 0.08f * (float) Math.sin(seed * 1.4f));

        Path body = scratchPath;
        body.reset();
        body.moveTo(left - width * 0.08f, baseY);
        body.lineTo(shoulderL, baseY - width * 0.10f);
        body.lineTo(ridgeL, baseY - width * 0.23f);
        body.lineTo(peakX, peakY);
        body.lineTo(ridgeR, baseY - width * 0.29f);
        body.lineTo(shoulderR, baseY - width * 0.12f);
        body.lineTo(right + width * 0.08f, baseY);
        body.close();

        auxPaint.setShader(new android.graphics.LinearGradient(
                left, peakY, right, baseY, lightColor, shadowColor,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawPath(body, auxPaint);
        auxPaint.setShader(null);

        if (gv.shouldDrawShadows()) {
            Path shadow = scratchPath2;
            shadow.reset();
            shadow.moveTo(peakX, peakY);
            shadow.lineTo(ridgeR, baseY - width * 0.29f);
            shadow.lineTo(shoulderR, baseY - width * 0.12f);
            shadow.lineTo(right + width * 0.08f, baseY);
            shadow.lineTo(peakX + width * 0.08f, baseY);
            shadow.close();
            paint.setColor(Color.argb(gv.shadowAlpha((int) (150 * detailAlpha)), 0, 0, 0));
            canvas.drawPath(shadow, paint);
        }

        Path light = scratchPath3;
        light.reset();
        light.moveTo(peakX, peakY);
        light.lineTo(ridgeL, baseY - width * 0.23f);
        light.lineTo(shoulderL, baseY - width * 0.10f);
        light.lineTo(peakX - width * 0.07f, baseY);
        light.close();
        paint.setColor(Color.argb((int) (90 * detailAlpha), 255, 226, 180));
        canvas.drawPath(light, paint);

        float capH = (baseY - peakY) * 0.23f;
        Path cap = scratchPath3;
        cap.reset();
        cap.moveTo(peakX - capH * 0.92f, peakY + capH * 1.38f);
        cap.lineTo(peakX - capH * 0.30f, peakY + capH * 0.68f);
        cap.lineTo(peakX, peakY);
        cap.lineTo(peakX + capH * 0.34f, peakY + capH * 0.74f);
        cap.lineTo(peakX + capH * 0.98f, peakY + capH * 1.48f);
        cap.lineTo(peakX + capH * 0.18f, peakY + capH * 1.10f);
        cap.close();
        paint.setColor(Color.argb((int) (80 * detailAlpha), 145, 124, 104));
        canvas.drawPath(cap, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(1.2f + detailAlpha * 3f);
        paint.setColor(Color.argb((int) (135 * detailAlpha), 255, 236, 210));
        canvas.drawLine(peakX, peakY + capH * 0.26f, ridgeL, baseY - width * 0.20f, paint);
        int shadowAlpha = gv.shadowAlpha((int) (170 * detailAlpha));
        if (shadowAlpha > 0) {
            paint.setColor(Color.argb(shadowAlpha, 0, 0, 0));
            canvas.drawLine(peakX + width * 0.035f, peakY + capH * 0.38f,
                    ridgeR, baseY - width * 0.24f, paint);
        }
        paint.setStrokeWidth(1.1f);
        for (int i = 0; i < (premiumRendering ? 5 : 3); i++) {
            float t = (i + 1) / 6f;
            float x1 = peakX + (ridgeR - peakX) * t;
            float y1 = peakY + (baseY - width * 0.24f - peakY) * t;
            int alpha = gv.shadowAlpha((int) (75 * detailAlpha));
            if (alpha > 0) {
                paint.setColor(Color.argb(alpha, 0, 0, 0));
                canvas.drawLine(x1, y1, x1 + width * 0.10f, y1 + width * 0.035f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawDuneLayer(Canvas canvas, Paint paint, Paint auxPaint,
                               int screenWidth, int screenHeight, float baseY,
                               float amplitude, float spacing, float scroll,
                               int lightColor, int shadowColor, int alpha) {
        float safeSpacing = Math.max(220f, spacing);
        float offset = scroll % safeSpacing;
        for (float x = -safeSpacing * 1.6f - offset; x < screenWidth + safeSpacing * 1.8f; x += safeSpacing) {
            float crestY = baseY + (float) Math.sin((x + scroll) * 0.008f) * amplitude * 0.12f;

            Path dune = scratchPath;
            dune.reset();
            dune.moveTo(x - safeSpacing * 0.25f, screenHeight);
            dune.lineTo(x - safeSpacing * 0.25f, crestY + amplitude * 0.56f);
            dune.cubicTo(x + safeSpacing * 0.08f, crestY - amplitude * 0.52f,
                    x + safeSpacing * 0.56f, crestY - amplitude * 0.72f,
                    x + safeSpacing * 1.18f, crestY + amplitude * 0.34f);
            dune.lineTo(x + safeSpacing * 1.18f, screenHeight);
            dune.close();

            auxPaint.setShader(new android.graphics.LinearGradient(
                    x, crestY - amplitude, x + safeSpacing, screenHeight,
                    Color.argb(alpha, Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor)),
                    Color.argb(alpha, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor)),
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawPath(dune, auxPaint);
            auxPaint.setShader(null);

            if (gv.shouldDrawShadows()) {
                Path leeSide = scratchPath2;
                leeSide.reset();
                leeSide.moveTo(x + safeSpacing * 0.55f, crestY - amplitude * 0.66f);
                leeSide.cubicTo(x + safeSpacing * 0.77f, crestY - amplitude * 0.22f,
                        x + safeSpacing * 0.96f, crestY + amplitude * 0.12f,
                        x + safeSpacing * 1.18f, crestY + amplitude * 0.34f);
                leeSide.lineTo(x + safeSpacing * 1.18f, screenHeight);
                leeSide.lineTo(x + safeSpacing * 0.62f, screenHeight);
                leeSide.close();
                paint.setColor(Color.argb(gv.shadowAlpha(alpha / 3), 42, 30, 20));
                canvas.drawPath(leeSide, paint);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(2.2f);
            paint.setColor(Color.argb(alpha, 255, 232, 176));
            canvas.drawLine(x + safeSpacing * 0.08f, crestY - amplitude * 0.08f,
                    x + safeSpacing * 0.58f, crestY - amplitude * 0.68f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }
    }

    private void drawSandRipples(Canvas canvas, Paint paint, int screenWidth,
                                 float topY, float bottomY, float scroll,
                                 AudioEngine.VoiceLevel level, boolean premiumRendering) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int rows = premiumRendering ? 26 : 14;
        float height = bottomY - topY;
        for (int i = 0; i < rows; i++) {
            float t = (i + 1f) / rows;
            float y = topY + height * t;
            float segment = 82f + t * 90f;
            float offset = (scroll * (0.34f + t * 0.9f) + i * 43f) % segment;
            paint.setStrokeWidth(0.8f + t * 2.2f);
            int alpha = (int) ((level == AudioEngine.VoiceLevel.RAGE ? 78 : 44) + t * 70);
            paint.setColor(Color.argb(alpha, 255, 232, 174));
            for (float x = -segment - offset; x < screenWidth + segment; x += segment) {
                float wave = (float) Math.sin((x + scroll * 0.2f) * 0.02f + i) * 3f * t;
                canvas.drawLine(x, y + wave, x + segment * 0.48f, y - wave * 0.35f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawMovingSandDetail(Canvas canvas, Paint paint, int screenWidth,
                                      float topY, float bottomY, float scroll,
                                      boolean premiumRendering) {
        int detailCount = premiumRendering ? 28 : 12;
        float span = screenWidth + 120f;
        for (int i = 0; i < detailCount; i++) {
            float depth = (i % 9) / 8f;
            float y = topY + 16f + (bottomY - topY - 22f) * ((i * 37) % 100) / 100f;
            float x = ((i * 137f - scroll * (0.62f + depth * 0.9f)) % span);
            if (x < -80f) x += span;
            float size = 1.5f + depth * 5.5f;
            paint.setColor(Color.argb((int) (48 + depth * 70), 97, 66, 36));
            canvas.drawOval(new RectF(x - size * 1.6f, y - size * 0.55f,
                    x + size * 1.6f, y + size * 0.55f), paint);
        }
    }
}
