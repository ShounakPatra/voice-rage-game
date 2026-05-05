package com.shounak.voiceragegame;

import android.graphics.RectF;

public class Obstacle {

    public float x, y;
    public int width, height;
    public float baseY;
    public float phaseSeed;
    public int type;

    public boolean passed = false;
    public static final int TYPE_CACTUS = 0;
    public static final int TYPE_FLYING_ENEMY = 1;

    public Obstacle(float x, float y, int width, int height) {
        this(x, y, width, height, TYPE_CACTUS, 0f);
    }

    public Obstacle(float x, float y, int width, int height, int type, float phaseSeed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.baseY = y;
        this.phaseSeed = phaseSeed;
    }

    public boolean isFlyingEnemy() {
        return type == TYPE_FLYING_ENEMY;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}
