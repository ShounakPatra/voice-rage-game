package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public void update(boolean premiumRendering) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life--;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.x += p.vx;
            p.y += p.vy;
            p.vx *= p.spark ? 0.96f : 0.92f;
            p.vy += p.spark ? 0.04f : 0.12f;
            p.size *= p.spark ? 0.97f : 0.985f;
        }
        int maxParticles = premiumRendering ? 120 : 64;
        while (particles.size() > maxParticles) {
            particles.remove(0);
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Particle p : particles) {
            float t = Math.max(0f, p.life / (float) p.maxLife);
            int alpha = (int) (t * (p.spark ? 235f : 155f));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(p.color, alpha));
            if (p.spark) {
                paint.setStrokeWidth(Math.max(2f, p.size * 0.45f));
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(p.x, p.y, p.x - p.vx * 2.2f, p.y - p.vy * 2.2f, paint);
                paint.setStrokeCap(Paint.Cap.BUTT);
            } else {
                canvas.drawCircle(p.x, p.y, p.size * (0.5f + t * 0.5f), paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void clear() {
        particles.clear();
    }

    public void spawnLandingDust(Player player, int groundY, float impact, boolean premiumRendering) {
        int count = premiumRendering ? 12 : 7;
        float baseX = player.x + player.width * 0.48f;
        float baseY = groundY - 4f;
        for (int i = 0; i < count; i++) {
            float dir = (i - count / 2f) / count;
            float speed = 1.2f + random.nextFloat() * 2.6f + impact * 2.2f;
            particles.add(new Particle(baseX + random.nextFloat() * 28f - 14f, baseY,
                    dir * speed * 2.2f - player.speed * 0.28f,
                    -0.8f - random.nextFloat() * (1.8f + impact * 1.6f),
                    28 + random.nextInt(18),
                    4f + random.nextFloat() * 7f,
                    Color.rgb(231, 194, 123), false));
        }
    }

    public void spawnRageSparks(Player player, boolean premiumRendering) {
        int count = premiumRendering ? 4 : 2;
        float twoPi = (float) (Math.PI * 2.0);
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * twoPi;
            float speed = 2.2f + random.nextFloat() * 4.5f;
            float sx = player.x + player.width * (0.18f + random.nextFloat() * 0.72f);
            float sy = player.y + player.height * (0.12f + random.nextFloat() * 0.58f);
            particles.add(new Particle(sx, sy,
                    (float) Math.cos(angle) * speed - player.speed * 0.16f,
                    (float) Math.sin(angle) * speed - 1.2f,
                    18 + random.nextInt(16),
                    3f + random.nextFloat() * 4f,
                    random.nextBoolean() ? Color.rgb(255, 190, 50) : Color.rgb(255, 72, 38),
                    true));
        }
    }
}
