package com.shounak.voiceragegame;

import android.graphics.RectF;

public class Obstacle {
    public float x, y, width, height;

    public Obstacle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }
}