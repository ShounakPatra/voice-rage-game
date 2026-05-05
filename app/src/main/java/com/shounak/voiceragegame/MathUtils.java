package com.shounak.voiceragegame;

public class MathUtils {
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    public static float smoothStep(float t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }
}
