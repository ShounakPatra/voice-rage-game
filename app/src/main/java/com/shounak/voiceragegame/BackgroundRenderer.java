package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class BackgroundRenderer {

    public BackgroundRenderer() {}

    public static int mixColor(int from, int to, float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return Color.rgb(
                (int) (Color.red(from)   + (Color.red(to)   - Color.red(from))   * c),
                (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * c),
                (int) (Color.blue(from)  + (Color.blue(to)  - Color.blue(from))  * c));
    }

    public void drawPostProcessingOverlay(Canvas canvas, Paint paint, Paint auxPaint,
                                          int screenWidth, int screenHeight, int groundY,
                                          float exposurePct, float saturationPct,
                                          float contrastPct) {
        float exposure = (exposurePct - 50f) / 50f;
        if (exposure > 0.02f) {
            paint.setColor(Color.argb((int) (exposure * 42f), 255, 238, 210));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        } else if (exposure < -0.02f) {
            paint.setColor(Color.argb((int) (-exposure * 58f), 0, 0, 0));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        }

        float saturation = (saturationPct - 50f) / 50f;
        if (saturation < -0.02f) {
            paint.setColor(Color.argb((int) (-saturation * 46f), 138, 142, 146));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        } else if (saturation > 0.02f) {
            paint.setColor(Color.argb((int) (saturation * 24f), 255, 122, 36));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        }

        float contrast = (contrastPct - 50f) / 50f;
        if (contrast > 0.02f) {
            auxPaint.setShader(new android.graphics.RadialGradient(
                    screenWidth / 2f, groundY * 0.46f, screenWidth * 0.82f,
                    Color.TRANSPARENT,
                    Color.argb((int) (contrast * 76f), 0, 0, 0),
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, screenWidth, screenHeight, auxPaint);
            auxPaint.setShader(null);
        } else if (contrast < -0.02f) {
            paint.setColor(Color.argb((int) (-contrast * 34f), 210, 210, 210));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        }
    }
}
