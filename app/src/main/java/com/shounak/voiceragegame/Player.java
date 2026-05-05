package com.shounak.voiceragegame;

import android.os.SystemClock;
import android.graphics.RectF;

public class Player {

    public float x, y;
    public float velY = 0;
    public boolean isOnGround = true;
    public int width = 90, height = 90;
    public float speed = 0;
    public boolean isRaging = false;
    public boolean justLanded = false;
    public boolean touchingRoof = false;
    public float lastLandImpact = 0f;

    private final float groundY;
    private static final float GRAVITY = 1.65f;
    private static final float MAX_FALL_SPEED = 30f;
    private static final float ROOF_Y = 10f;
    private static final long AIR_HANG_TIME_MS = 115;
    private static final float APEX_HANG_VELOCITY = 6.0f;
    private static final float AIR_HANG_GRAVITY_SCALE = 0.52f;

    // ── Mode-based thresholds (set by settings) ──────────────────────
    public static int jumpAmpThreshold = 2500;
    public static int jumpDeltaThreshold = 800;

    // ── Sensitivity multiplier (0.5 = easier, 2.0 = harder) ─────────
    public static float sensitivityMultiplier = 1.0f;

    // ── Jump tracking ─────────────────────────────────────────────────
    private int lastAmp = 0;
    private long lastJumpTime = 0;
    private static final long JUMP_COOLDOWN_MS = 155;

    public Player(float startX, float groundY) {
        this.x = startX;
        this.groundY = groundY;
        this.y = groundY - height;
    }

    public boolean update(AudioEngine.VoiceLevel level, int amplitude, int selectedMode) {
        return update(level, amplitude, selectedMode, false);
    }

    public boolean update(AudioEngine.VoiceLevel level, int amplitude, int selectedMode, boolean jumpRequested) {
        long now = SystemClock.uptimeMillis();
        boolean wasOnGround = isOnGround;
        justLanded = false;
        touchingRoof = false;

        // ── INSTANT JUMP DETECTION ────────────────────────────────────
        int delta = amplitude - lastAmp;
        int effectiveThreshold = (int) (jumpAmpThreshold * sensitivityMultiplier);
        int effectiveDelta = (int) (jumpDeltaThreshold * sensitivityMultiplier);

        boolean voiceOnset = amplitude > effectiveThreshold
                && delta > effectiveDelta;
        boolean jumped = ((jumpRequested && amplitude > effectiveThreshold * 0.72f) || voiceOnset)
                && isOnGround
                && now - lastJumpTime > JUMP_COOLDOWN_MS;

        if (jumped) {
            jump(amplitude, selectedMode == 2);
            lastJumpTime = now;
        }

        lastAmp = amplitude;

        // ── PHYSICS ───────────────────────────────────────────────────
        float gravity = GRAVITY;
        long airborneMs = now - lastJumpTime;
        if (!isOnGround && (airborneMs < AIR_HANG_TIME_MS || Math.abs(velY) < APEX_HANG_VELOCITY)) {
            gravity *= AIR_HANG_GRAVITY_SCALE;
        }
        velY += gravity;
        if (velY > MAX_FALL_SPEED) {
            velY = MAX_FALL_SPEED;
        }
        y += velY;

        // Ground clamp
        if (y >= groundY - height) {
            float impact = Math.abs(velY);
            y = groundY - height;
            velY = 0;
            isOnGround = true;
            if (!wasOnGround) {
                justLanded = true;
                lastLandImpact = Math.min(1f, impact / MAX_FALL_SPEED);
            }
        } else {
            isOnGround = false;
        }

        // ── ROOF BOUNCE (instead of going offscreen) ──────────────────
        if (y < ROOF_Y) {
            y = ROOF_Y;
            touchingRoof = true;
            velY = Math.abs(velY) * 0.42f; // bounce back down
        }

        // ── SPEED CONTROL ─────────────────────────────────────────────
        float targetSpeed;
        switch (level) {
            case SILENT:
                targetSpeed = 0;
                isRaging = false;
                break;
            case WHISPER:
                targetSpeed = 3;
                isRaging = false;
                break;
            case NORMAL:
                targetSpeed = 7;
                isRaging = false;
                break;
            case SHOUT:
                targetSpeed = 12;
                isRaging = false;
                break;
            case RAGE:
                targetSpeed = 18;
                isRaging = true;
                break;
            default:
                targetSpeed = 0;
                isRaging = false;
                break;
        }
        speed += (targetSpeed - speed) * 0.22f;

        return jumped;
    }

    public void jump(int amplitude, boolean rageMode) {
        if (isOnGround) {
            float normalized = Math.min(amplitude / 6000f, 1f);
            if (rageMode) {
                velY = -21f - (normalized * 10f);
            } else {
                velY = -23f - (normalized * 14f);
            }
            isOnGround = false;
        }
    }

    public void reset(float groundY) {
        y = groundY - height;
        velY = 0;
        isOnGround = true;
        justLanded = false;
        touchingRoof = false;
        lastLandImpact = 0f;
        speed = 0;
        isRaging = false;
        lastAmp = 0;
        lastJumpTime = 0;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}
