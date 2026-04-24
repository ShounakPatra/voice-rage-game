package com.shounak.voiceragegame;

import java.util.Random;
import android.graphics.RectF;

public class Player {

    Random random = new Random();

    public float x, y;
    public float velY = 0;
    public boolean isOnGround = true;
    public int width = 90, height = 90;
    public float speed = 0;
    public boolean isRaging = false;
    public static int jumpAmpThreshold = 5000;
    public static int jumpDeltaThreshold = 2000;
    private final float groundY;

    private static final float GRAVITY = 2.0f;

    // 🔥 spike detection
    private long lastJumpTime = 0;
    private int lastAmp = 0;

    public Player(float startX, float groundY) {
        this.x = startX;
        this.groundY = groundY;
        this.y = groundY - height;
    }

    public void update(AudioEngine.VoiceLevel level, int amplitude) {

        // =========================
        // 🔥 SMART JUMP (LOUD SPIKE ONLY)
        // =========================
        int delta = amplitude - lastAmp;

        if (amplitude > jumpAmpThreshold
                && delta > jumpDeltaThreshold
                && isOnGround
                && System.currentTimeMillis() - lastJumpTime > 500) {

            jump(amplitude);
            lastJumpTime = System.currentTimeMillis();
        }

        // =========================
        // 🧠 PHYSICS
        // =========================
        velY += GRAVITY;
        y += velY;

        // Ground collision
        if (y >= groundY - height) {
            y = groundY - height;
            velY = 0;
            isOnGround = true;
        } else {
            isOnGround = false;
        }

        // =========================
        // 🏃 SPEED CONTROL
        // =========================
        switch (level) {

            case SILENT:
                speed = 0;
                isRaging = false;
                break;

            case WHISPER:
                speed = 3;
                isRaging = false;
                break;

            case NORMAL:
                speed = 7;
                isRaging = false;
                break;

            case SHOUT:
                speed = 12;
                isRaging = false;
                break;

            case RAGE:
                speed = 18;
                isRaging = true;
                break;
        }
    }

    public void jump(int amplitude) {
        if (isOnGround) {

            float normalized = Math.min(amplitude / 6000f, 1f);

            float jumpForce = -32f - (normalized * 28f);

            velY = jumpForce;
            isOnGround = false;
        }
    }

    public void reset(float groundY) {
        y = groundY - height;
        velY = 0;
        isOnGround = true;
        speed = 0;
        isRaging = false;

        lastAmp = 0;
        lastJumpTime = 0;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}