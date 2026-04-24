package com.shounak.voiceragegame;

import android.graphics.RectF;

public class Obstacle {

    public float x, y;
    public int width, height;

    public boolean passed = false;

    public Obstacle(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}