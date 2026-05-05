package com.shounak.voiceragegame;

public class Particle {
    float x;
    float y;
    float vx;
    float vy;
    int life;
    final int maxLife;
    float size;
    final int color;
    final boolean spark;

    Particle(float x, float y, float vx, float vy, int life, float size, int color, boolean spark) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.size = size;
        this.color = color;
        this.spark = spark;
    }
}
