package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class GameUI {
    public final GameView gv;
    public final SettingsManager sm;
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameUI(GameView gv, SettingsManager sm) {
        this.gv = gv;
        this.sm = sm;
    }

    public void drawButton(Canvas canvas, RectF rect,
                           int startColor, int endColor,
                           boolean selected, boolean enabled, float glowStrength) {
        drawPremiumButtonSurface(canvas, rect,
                startColor, endColor, selected, enabled, glowStrength);
    }

    public void drawPremiumButtonSurface(Canvas canvas, RectF rect, int startColor, int endColor,
                                         boolean selected, boolean enabled, float glowStrength) {
        float radius = 20f;
        int glowColor = selected ? startColor : brighten(startColor, 35);
        activePaint.setShader(null);
        activePaint.setStyle(Paint.Style.FILL);

        if (enabled && glowStrength > 0f) {
            for (int i = 4; i >= 1; i--) {
                float grow = i * (5.5f + glowStrength * 3f);
                activePaint.setColor(Color.argb((int) ((24f + glowStrength * 34f) / i),
                        Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)));
                canvas.drawRoundRect(new RectF(rect.left - grow, rect.top - grow,
                        rect.right + grow, rect.bottom + grow), radius, radius, activePaint);
            }
        }

        if (gv.shouldDrawShadows()) {
            activePaint.setColor(Color.argb(gv.shadowAlpha(enabled ? 115 : 70), 0, 0, 0));
            float blur = gv.getSoftShadowExtra() * 0.28f;
            canvas.drawRoundRect(new RectF(rect.left + 5f, rect.top + 8f + blur,
                    rect.right + 5f, rect.bottom + 10f + blur), radius, radius, activePaint);
        }

        activePaint.setShader(new android.graphics.LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                gv.withAlpha(startColor, enabled ? 250 : 155),
                gv.withAlpha(endColor, enabled ? 250 : 145),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, activePaint);
        activePaint.setShader(null);

        activePaint.setColor(Color.argb(enabled ? 90 : 35, 255, 255, 255));
        canvas.drawRoundRect(new RectF(rect.left + 6f, rect.top + 6f,
                rect.right - 6f, rect.top + rect.height() * 0.43f), 16f, 16f, activePaint);

        activePaint.setStyle(Paint.Style.STROKE);
        activePaint.setStrokeWidth(selected ? 4.2f : 2.2f);
        activePaint.setColor(selected ? Color.WHITE : Color.argb(enabled ? 150 : 75, 255, 255, 255));
        canvas.drawRoundRect(rect, radius, radius, activePaint);
        activePaint.setStrokeWidth(1.4f);
        activePaint.setColor(Color.argb(enabled ? 120 : 54, 255, 188, 92));
        canvas.drawRoundRect(new RectF(rect.left + 3f, rect.top + 3f,
                rect.right - 3f, rect.bottom - 3f), radius - 3f, radius - 3f, activePaint);
        activePaint.setStyle(Paint.Style.FILL);
    }

    public void drawVoiceMeter(Canvas canvas, Paint paint, int smoothedAmplitude,
                               AudioEngine.VoiceLevel level,
                               int screenWidth, int screenHeight) {
        float barW = Math.min(smoothedAmplitude / 8000f, 1f) * screenWidth;
        paint.setColor(Color.argb(gv.hudAlpha(80), 255, 255, 255));
        canvas.drawRect(0, screenHeight - 40, screenWidth, screenHeight - 10, paint);
        paint.setColor(gv.hudColor(getVoiceColor(level)));
        canvas.drawRect(0, screenHeight - 40, barW, screenHeight - 10, paint);
        paint.setColor(gv.hudColor(Color.WHITE));
        paint.setTextSize(28);
        canvas.drawText("VOICE METER", 20, screenHeight - 16, paint);
    }

    public void drawHud(Canvas canvas, Paint paint, int score, int highScore,
                        int dailyBestScore, boolean dailyChallenge,
                        int screenWidth, float difficultyRight) {
        paint.setColor(gv.withAlpha(Color.rgb(255, 128, 44), gv.hudAlpha(84)));
        paint.setTextSize(58);
        canvas.drawText("SCORE: " + (score / 10), 38, 62, paint);
        paint.setColor(gv.hudColor(Color.WHITE));
        paint.setTextSize(55);
        canvas.drawText("SCORE: " + (score / 10), 40, 60, paint);

        paint.setColor(gv.hudColor(Color.rgb(255, 215, 0)));
        paint.setTextSize(40);
        canvas.drawText("BEST: " + highScore, 40, 110, paint);
        if (dailyChallenge) {
            paint.setColor(gv.hudColor(Color.rgb(100, 220, 255)));
            paint.setTextSize(32);
            canvas.drawText("DAILY BEST: " + dailyBestScore, 40, 150, paint);
        }

        int runScore = score / 10;
        int difficultyLevel = runScore / 100;
        String difficultyLabel = difficultyLevel == 0 ? "EASY"
                : difficultyLevel == 1 ? "MEDIUM"
                : difficultyLevel == 2 ? "HARD"
                : difficultyLevel == 3 ? "INSANE"
                : "LEGEND RUN";
        paint.setTextSize(38);
        paint.setColor(gv.hudColor(Color.YELLOW));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(difficultyLabel, difficultyRight, 60, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private int getVoiceColor(AudioEngine.VoiceLevel level) {
        switch (level) {
            case SILENT:
                return Color.rgb(120, 120, 120);
            case WHISPER:
                return Color.rgb(80, 180, 255);
            case NORMAL:
                return Color.rgb(80, 255, 120);
            case SHOUT:
                return Color.rgb(255, 180, 50);
            case RAGE:
            default:
                return Color.rgb(255, 50, 50);
        }
    }

    private int brighten(int color, int amount) {
        return Color.rgb(Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount),
                Math.min(255, Color.blue(color) + amount));
    }
}
