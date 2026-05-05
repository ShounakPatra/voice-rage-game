package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class ObstacleRenderer {
    private final GameView gv;
    private final Path wingPath = new Path();

    public ObstacleRenderer(GameView gv) {
        this.gv = gv;
    }

    public void drawCactus3D(Canvas canvas, Paint paint, Paint auxPaint, Obstacle obs, float groundY, boolean premiumRendering) {
        if (!premiumRendering) {
            return;
        }
        float cx = obs.x;
        float cy = obs.y;
        float cw = obs.width;
        float ch = obs.height;
        float trunkLeft = cx + cw * 0.23f;
        float trunkRight = cx + cw * 0.77f;

        paint.setStyle(Paint.Style.FILL);
        if (gv.shouldDrawSceneShadows()) {
            float blur = gv.getSoftShadowExtra();
            paint.setColor(Color.argb(gv.shadowAlpha(78), 0, 0, 0));
            canvas.drawOval(new RectF(cx - cw * 0.18f - blur, groundY - 9f - blur * 0.18f,
                    cx + cw * 1.18f + blur, groundY + 11f + blur * 0.18f), paint);
        }

        auxPaint.setShader(new android.graphics.LinearGradient(
                trunkLeft, cy, trunkRight, cy + ch,
                new int[]{
                        Color.rgb(92, 202, 78),
                        Color.rgb(35, 132, 50),
                        Color.rgb(15, 76, 37)
                },
                new float[]{0f, 0.52f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(trunkLeft, cy, trunkRight, cy + ch), 16, 16, auxPaint);
        auxPaint.setShader(null);

        paint.setColor(Color.argb(72, 255, 255, 190));
        canvas.drawRoundRect(new RectF(trunkLeft + cw * 0.08f, cy + ch * 0.06f,
                trunkLeft + cw * 0.19f, cy + ch * 0.86f), 8, 8, paint);
        paint.setColor(Color.argb(80, 0, 38, 20));
        canvas.drawRoundRect(new RectF(trunkRight - cw * 0.17f, cy + ch * 0.04f,
                trunkRight - cw * 0.03f, cy + ch), 8, 8, paint);

        int ribCount = Math.max(4, (int) (ch / 24f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        for (int i = 1; i < ribCount; i++) {
            float rx = trunkLeft + (trunkRight - trunkLeft) * i / ribCount;
            paint.setColor(i % 2 == 0
                    ? Color.argb(52, 190, 255, 150)
                    : Color.argb(52, 0, 60, 28));
            canvas.drawLine(rx, cy + 10f, rx, cy + ch - 8f, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        if (ch > 62f) {
            float armY = cy + ch * 0.35f;
            drawCactusArm3D(canvas, paint, auxPaint, cx + cw * 0.02f, armY, trunkLeft + cw * 0.08f,
                    armY + cw * 0.34f, true);
        }
        if (ch > 84f) {
            float armY = cy + ch * 0.52f;
            drawCactusArm3D(canvas, paint, auxPaint, trunkRight - cw * 0.08f, armY,
                    cx + cw * 0.98f, armY + cw * 0.34f, false);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        paint.setColor(Color.argb(190, 235, 245, 215));
        int spineCount = Math.max(5, (int) (ch / 18f));
        for (int i = 0; i < spineCount; i++) {
            float sy = cy + ch * (i + 0.45f) / spineCount;
            canvas.drawLine(trunkLeft + 2f, sy, trunkLeft - 7f, sy - 4f, paint);
            canvas.drawLine(trunkRight - 2f, sy, trunkRight + 8f, sy - 4f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    public void drawFlyingEnemy(Canvas canvas, Paint paint, Paint auxPaint,
                                Obstacle obs, long now, boolean premiumRendering) {
        float x = obs.x;
        float y = obs.y;
        float w = obs.width;
        float h = obs.height;
        float cx = x + w * 0.52f;
        float cy = y + h * 0.50f;
        float wingBeat = (float) Math.sin(now * 0.018f + obs.phaseSeed);
        float wingLift = wingBeat * h * 0.18f;

        paint.setStyle(Paint.Style.FILL);
        if (premiumRendering) {
            for (int i = 3; i >= 1; i--) {
                float trail = i * (12f + w * 0.035f);
                paint.setColor(Color.argb(16 + i * 8, 95, 190, 255));
                canvas.drawOval(new RectF(x - trail, y + h * 0.18f,
                        x + w * 0.82f - trail, y + h * 0.84f), paint);
            }
        }

        auxPaint.setShader(new android.graphics.RadialGradient(
                cx - w * 0.16f, cy - h * 0.18f, w * 0.62f,
                new int[]{
                        Color.rgb(236, 242, 246),
                        Color.rgb(92, 108, 122),
                        Color.rgb(26, 32, 42)
                },
                new float[]{0f, 0.52f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawOval(new RectF(x + w * 0.18f, y + h * 0.20f,
                x + w * 0.88f, y + h * 0.78f), auxPaint);
        auxPaint.setShader(null);

        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawOval(new RectF(x + w * 0.28f, y + h * 0.26f,
                x + w * 0.58f, y + h * 0.46f), paint);
        paint.setColor(Color.argb(115, 0, 0, 0));
        canvas.drawOval(new RectF(x + w * 0.54f, y + h * 0.42f,
                x + w * 0.88f, y + h * 0.80f), paint);

        drawWing(canvas, paint, auxPaint, x + w * 0.36f, cy, -1f, w, h, wingLift, premiumRendering);
        drawWing(canvas, paint, auxPaint, x + w * 0.54f, cy, 1f, w, h, -wingLift * 0.8f, premiumRendering);

        float headLeft = x + w * 0.68f;
        auxPaint.setShader(new android.graphics.RadialGradient(
                headLeft + w * 0.12f, y + h * 0.33f, w * 0.24f,
                Color.rgb(218, 226, 232),
                Color.rgb(35, 41, 48),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawOval(new RectF(headLeft, y + h * 0.19f,
                x + w * 1.03f, y + h * 0.58f), auxPaint);
        auxPaint.setShader(null);

        paint.setColor(Color.rgb(255, 54, 38));
        canvas.drawCircle(x + w * 0.86f, y + h * 0.35f, Math.max(4f, w * 0.045f), paint);
        paint.setColor(Color.argb(170, 255, 180, 120));
        canvas.drawCircle(x + w * 0.875f, y + h * 0.335f, Math.max(1.8f, w * 0.017f), paint);

        wingPath.reset();
        wingPath.moveTo(x + w * 0.96f, y + h * 0.41f);
        wingPath.lineTo(x + w * 1.15f, y + h * 0.50f);
        wingPath.lineTo(x + w * 0.95f, y + h * 0.56f);
        wingPath.close();
        paint.setColor(Color.rgb(226, 172, 64));
        canvas.drawPath(wingPath, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(155, 180, 228, 255));
        canvas.drawArc(new RectF(x + w * 0.08f, y + h * 0.06f,
                x + w * 0.98f, y + h * 0.94f), 202, 74, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawWing(Canvas canvas, Paint paint, Paint auxPaint, float rootX, float rootY,
                          float side, float bodyW, float bodyH, float lift,
                          boolean premiumRendering) {
        wingPath.reset();
        wingPath.moveTo(rootX, rootY);
        wingPath.cubicTo(rootX - side * bodyW * 0.24f, rootY - bodyH * 0.42f + lift,
                rootX - side * bodyW * 0.70f, rootY - bodyH * 0.38f + lift,
                rootX - side * bodyW * 0.82f, rootY + bodyH * 0.04f + lift);
        wingPath.cubicTo(rootX - side * bodyW * 0.56f, rootY + bodyH * 0.25f + lift * 0.3f,
                rootX - side * bodyW * 0.24f, rootY + bodyH * 0.22f,
                rootX, rootY);
        wingPath.close();

        auxPaint.setShader(new android.graphics.LinearGradient(
                rootX, rootY - bodyH * 0.4f, rootX - side * bodyW * 0.82f, rootY + bodyH * 0.22f,
                new int[]{
                        Color.argb(235, 52, 62, 72),
                        Color.argb(230, 112, 126, 138),
                        Color.argb(235, 20, 24, 32)
                },
                new float[]{0f, 0.42f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawPath(wingPath, auxPaint);
        auxPaint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(premiumRendering ? 2.4f : 1.8f);
        for (int i = 1; i <= 4; i++) {
            float t = i / 5f;
            paint.setColor(Color.argb(72, 224, 234, 244));
            canvas.drawLine(rootX - side * bodyW * 0.08f, rootY - bodyH * 0.02f,
                    rootX - side * bodyW * (0.20f + t * 0.48f),
                    rootY - bodyH * (0.26f - t * 0.18f) + lift * (0.9f - t), paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCactusArm3D(Canvas canvas, Paint paint, Paint auxPaint, float left, float top, float right, float bottom,
                                 boolean leftArm) {
        auxPaint.setShader(new android.graphics.LinearGradient(
                left, top, right, bottom,
                Color.rgb(82, 188, 74),
                Color.rgb(18, 94, 42),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12, 12, auxPaint);
        float upLeft = leftArm ? left + (right - left) * 0.08f : right - (right - left) * 0.36f;
        float upRight = leftArm ? left + (right - left) * 0.38f : right - (right - left) * 0.06f;
        canvas.drawRoundRect(new RectF(upLeft, top - (bottom - top) * 1.45f,
                upRight, top + (bottom - top) * 0.18f), 12, 12, auxPaint);
        auxPaint.setShader(null);
        paint.setColor(Color.argb(72, 255, 255, 190));
        float hl = leftArm ? upLeft + 3f : upLeft + 5f;
        canvas.drawRoundRect(new RectF(hl, top - (bottom - top) * 1.28f,
                hl + 5f, top + (bottom - top) * 0.05f), 4, 4, paint);
    }
}
