package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class CharacterRenderer {
    private final GameView gv;
    private final SettingsManager sm;

    public CharacterRenderer(GameView gv, SettingsManager sm) {
        this.gv = gv;
        this.sm = sm;
    }

    public void drawRunnerLeg(Canvas canvas, Paint paint, float px, float py, float pw, float ph,
                              float phase, boolean nearLeg, boolean onGround, float groundY,
                              int mainColor, int shadowColor, int shoeColor, int shoeHighlight) {
        float stride = (float) Math.sin(phase);
        float recover = (float) Math.cos(phase);
        boolean airborne = !onGround;
        float hipX = px + pw * (nearLeg ? 0.37f : 0.62f);
        float hipY = py + ph * 0.61f;
        float thighLen = ph * 0.26f;
        float shinLen = ph * 0.27f;

        float thighAngle = airborne
                ? (nearLeg ? 0.55f : -0.32f)
                : stride * 0.48f - 0.06f;
        float shinAngle = airborne
                ? (nearLeg ? 0.82f : 0.18f)
                : -stride * 0.62f + Math.max(0f, -recover) * 0.22f;

        float kneeX = hipX + (float) Math.sin(thighAngle) * thighLen;
        float kneeY = hipY + (float) Math.cos(thighAngle) * thighLen;
        float ankleX = kneeX + (float) Math.sin(shinAngle) * shinLen;
        float ankleY = kneeY + (float) Math.cos(shinAngle) * shinLen;
        if (!airborne && ankleY > groundY - 3f) ankleY = groundY - 3f;

        int alpha = nearLeg ? 255 : 185;

        drawLegSegment(canvas, paint, hipX, hipY, kneeX, kneeY, ph * 0.155f,
                gv.withAlpha(shadowColor, alpha), gv.withAlpha(mainColor, alpha));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(gv.withAlpha(mainColor, (int)(alpha * 0.9f)));
        canvas.drawCircle(kneeX, kneeY, ph * 0.048f, paint);

        drawLegSegment(canvas, paint, kneeX, kneeY, ankleX, ankleY, ph * 0.135f,
                gv.withAlpha(shadowColor, alpha), gv.withAlpha(mainColor, alpha));

        float footAngle = stride * 0.18f + (airborne ? -0.18f : 0.04f);
        float footLen = pw * 0.36f;
        float footH = ph * 0.065f;
        canvas.save();
        canvas.rotate((float) Math.toDegrees(footAngle), ankleX, ankleY);

        paint.setColor(gv.withAlpha(shoeColor, alpha));
        canvas.drawRoundRect(new RectF(
                ankleX - pw * 0.14f, ankleY - footH * 0.5f,
                ankleX + pw * 0.05f, ankleY + footH), 5f, 5f, paint);
        paint.setColor(gv.withAlpha(shoeColor, alpha));
        canvas.drawRoundRect(new RectF(
                ankleX - pw * 0.08f, ankleY + footH * 0.4f,
                ankleX + footLen * 0.72f, ankleY + footH), 4f, 4f, paint);
        paint.setColor(gv.withAlpha(shoeHighlight, alpha));
        canvas.drawRoundRect(new RectF(
                ankleX + footLen * 0.44f, ankleY - footH * 0.3f,
                ankleX + footLen * 0.88f, ankleY + footH * 0.85f), 6f, 6f, paint);

        canvas.restore();
    }

    public void drawRunnerArm(Canvas canvas, Paint paint, float px, float py, float pw, float ph,
                              float phase, boolean nearArm, boolean onGround,
                              int sleeveColor, int handColor, int handShadow) {
        float stride = (float) Math.sin(phase);
        boolean airborne = !onGround;
        float shoulderX = px + pw * (nearArm ? 0.74f : 0.26f);
        float shoulderY = py + ph * 0.40f;
        float upperLen = ph * 0.23f;
        float foreLen = ph * 0.20f;
        float side = nearArm ? 1f : -1f;
        float swing = airborne ? side * 0.26f : -stride * side * 0.52f;
        float upperAngle = swing + side * 0.12f;
        float foreAngle = swing * -0.55f + side * 0.28f;

        float elbowX = shoulderX + (float) Math.sin(upperAngle) * upperLen;
        float elbowY = shoulderY + (float) Math.cos(upperAngle) * upperLen;
        float handX = elbowX + (float) Math.sin(foreAngle) * foreLen;
        float handY = elbowY + (float) Math.cos(foreAngle) * foreLen;

        int alpha = nearArm ? 255 : 190;

        drawLegSegment(canvas, paint, shoulderX, shoulderY, elbowX, elbowY, ph * 0.115f,
                gv.withAlpha(gv.darken(sleeveColor, 45), alpha), gv.withAlpha(sleeveColor, alpha));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(gv.withAlpha(gv.darken(sleeveColor, 30), alpha));
        canvas.drawCircle(elbowX, elbowY, ph * 0.042f, paint);

        drawLegSegment(canvas, paint, elbowX, elbowY, handX, handY, ph * 0.092f,
                gv.withAlpha(gv.darken(sleeveColor, 58), alpha), gv.withAlpha(sleeveColor, alpha));

        paint.setStyle(Paint.Style.FILL);
        float handW = pw * 0.18f;
        float handH = pw * 0.15f;
        paint.setColor(gv.withAlpha(handColor, alpha));
        canvas.drawRoundRect(new RectF(
                handX - handW * 0.5f, handY - handH * 0.4f,
                handX + handW * 0.5f, handY + handH * 0.6f), 5f, 5f, paint);
        paint.setColor(gv.withAlpha(handShadow, (int)(alpha * 0.7f)));
        canvas.drawRoundRect(new RectF(
                handX + side * handW * 0.3f, handY - handH * 0.55f,
                handX + side * handW * 0.55f, handY + handH * 0.05f), 4f, 4f, paint);
        paint.setColor(gv.withAlpha(handShadow, (int)(alpha * 0.35f)));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(handX - handW * 0.3f, handY, handX + handW * 0.3f, handY, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLegSegment(Canvas canvas, Paint paint, float x1, float y1, float x2, float y2,
                                float width, int shadowColor, int mainColor) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (gv.shouldDrawSceneShadows()) {
            paint.setStrokeWidth(width);
            paint.setColor(shadowColor);
            canvas.drawLine(x1 + width * 0.12f, y1 + width * 0.08f,
                    x2 + width * 0.12f, y2 + width * 0.08f, paint);
        }
        paint.setStrokeWidth(width * 0.78f);
        paint.setColor(mainColor);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    // FIX: darken() removed — was a duplicate. Using gv.darken() which is now public.

    public void drawPlayerPremiumHighlights(Canvas canvas, Paint paint, Player player, AudioEngine.VoiceLevel level, long now, boolean premiumRendering) {
        float px = player.x;
        float py = player.y;
        float pw = player.width;
        float ph = player.height;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(95, 255, 238, 180));
        canvas.drawArc(new RectF(px + pw * 0.08f, py - ph * 0.08f,
                px + pw * 0.92f, py + ph * 0.44f), 210, 105, false, paint);
        paint.setColor(Color.argb(75, 0, 0, 0));
        canvas.drawArc(new RectF(px + pw * 0.05f, py + ph * 0.18f,
                px + pw * 0.98f, py + ph * 1.02f), 20, 82, false, paint);

        float intensity = level == AudioEngine.VoiceLevel.RAGE ? 1f
                : level == AudioEngine.VoiceLevel.SHOUT ? 0.7f
                : level == AudioEngine.VoiceLevel.NORMAL ? 0.42f
                : 0.18f;
        if (intensity > 0.25f) {
            paint.setStrokeWidth(2.5f);
            int waveCount = premiumRendering ? 4 : 2;
            for (int i = 0; i < waveCount; i++) {
                float wave = ((now % 650) / 650f + i * 0.22f) % 1f;
                float x1 = px - 34f - wave * 72f;
                float y = py + ph * (0.32f + i * 0.13f);
                paint.setColor(Color.argb((int) ((1f - wave) * 90f * intensity), 255, 145, 56));
                canvas.drawLine(x1, y, x1 + 42f, y - 6f, paint);
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    public void drawRageBloom(Canvas canvas, Paint paint, Player player, AudioEngine.VoiceLevel level, long now) {
        if (level != AudioEngine.VoiceLevel.RAGE && level != AudioEngine.VoiceLevel.SHOUT) {
            return;
        }
        float intensity = level == AudioEngine.VoiceLevel.RAGE ? 1f : 0.42f;
        float pulse = 0.72f + 0.28f * (float) Math.sin(now * 0.018f);
        float cx = player.x + player.width * 0.5f;
        float cy = player.y + player.height * 0.44f;
        float radius = player.width * (2.0f + intensity * 1.6f) * pulse;

        paint.setShader(new android.graphics.RadialGradient(cx, cy, radius,
                new int[]{Color.argb((int) (intensity * 115 * pulse), 255, 200, 100),
                        Color.argb((int) (intensity * 48 * pulse), 255, 100, 40),
                        Color.TRANSPARENT},
                new float[]{0f, 0.45f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
    }

    public void drawMotionBlurTrail(Canvas canvas, Paint paint, Player player, boolean gameStarted, boolean gamePaused, boolean premiumRendering, AudioEngine.VoiceLevel level) {
        if (!gameStarted || gamePaused || player.speed < 4f) {
            return;
        }
        float intensity = Math.min(1f, player.speed / 18f) * (level == AudioEngine.VoiceLevel.RAGE ? 1f : 0.46f);
        int copies = premiumRendering ? 3 : 2;
        for (int i = copies; i >= 1; i--) {
            float t = i / (float) copies;
            float offset = 10f + player.speed * 1.6f * t;
            int alpha = (int) (30f * intensity * (1f - t * 0.15f));
            paint.setColor(Color.argb(alpha, 255, 190, 90));
            canvas.drawRoundRect(new RectF(player.x - offset, player.y + player.height * 0.18f,
                    player.x + player.width * 0.80f - offset,
                    player.y + player.height * 0.82f), 18f, 18f, paint);
        }
    }

    public void drawRoofContactFlash(Canvas canvas, Paint paint, int screenWidth, long now, long lastRoofContactTime) {
        long elapsed = now - lastRoofContactTime;
        if (elapsed < 280L) {
            float t = elapsed / 280f;
            int alpha = (int) ((1f - t) * 115);
            paint.setColor(Color.argb(alpha, 255, 255, 255));
            canvas.drawRect(0, 0, screenWidth, 16f, paint);
        }
    }
}
