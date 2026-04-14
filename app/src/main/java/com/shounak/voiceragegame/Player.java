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

    private final float groundY;       // Y position of the floor
    private static final float GRAVITY = 2.0f;
    private static final float JUMP_FORCE = -35f;

    public Player(float startX, float groundY) {
        this.x = startX;
        this.groundY = groundY;
        this.y = groundY - height;
    }

    public void update(AudioEngine.VoiceLevel level, int amplitude) {

        boolean isShouting = (level == AudioEngine.VoiceLevel.SHOUT || level == AudioEngine.VoiceLevel.RAGE);

        if (isShouting) {
            // Normalize amplitude
            float normalized = Math.min(amplitude / 6000f, 1f);

            // Smooth upward acceleration
            velY += (-5f - normalized * 15f);

// cap upward speed (no rocket glitch 💀)
            if (velY < -28f) velY = -28f;

        } else {
            // Gravity
            velY += GRAVITY;
        }

        // Apply movement
        y += velY;

        // Ground check
        if (y >= groundY - height) {
            y = groundY - height;
            velY = 0;
            isOnGround = true;
        } else {
            isOnGround = false;
        }
        // Ceiling clamp — can't fly off screen
        if (y < 10) {
            y = 10;
            velY = 0;
        }

        // Voice → speed mapping
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

            // Normalize amplitude (0 → ~8000)
            float normalized = Math.min(amplitude / 6000f, 1f);

            // Base jump + extra boost
            float jumpForce = -25f - (normalized * 25f) - random.nextFloat() * 5;

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
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}