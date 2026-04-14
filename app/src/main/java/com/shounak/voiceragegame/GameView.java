package com.shounak.voiceragegame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Core stuff ──────────────────────────────────────────────────────────
    private GameThread gameThread;
    private final AudioEngine audioEngine;
    private Player player;
    private SoundPool soundPool;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Screen / layout ─────────────────────────────────────────────────────
    private int screenWidth, screenHeight, groundY;

    // ── Game state ──────────────────────────────────────────────────────────
    private int score = 0;
    private int highScore = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false; // waiting for first voice input
    private int shakeX = 0, shakeY = 0;
    private float bgHue = 220f; // background color hue

    // ── Obstacle spawning ────────────────────────────────────────────────────
    private long lastObstacleTime = 0;
    private static final long BASE_OBSTACLE_INTERVAL = 4200; // wider gaps — breathe bestie

    // ── Sound IDs ────────────────────────────────────────────────────────────
    private int soundOof = -1;
    private int soundBruh = -1;
    private int soundVineBoom = -1;
    private int soundMLG = -1;
    private int soundWhisper = -1;
    private long lastWhisperSoundTime = 0;
    private boolean rageSoundPlayed = false;
    private long lastRageSoundTime = 0;

    // ── Constructor ──────────────────────────────────────────────────────────
    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        audioEngine = new AudioEngine();
        setupSounds();
    }

    // ── SoundPool Setup ──────────────────────────────────────────────────────
    private void setupSounds() {

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        // Load sounds from res/raw/ — see Phase 5 for how to add these
        try { soundOof     = soundPool.load(getContext(), R.raw.oof,       1); } catch (Exception ignored) {}
        try { soundBruh    = soundPool.load(getContext(), R.raw.bruh,      1); } catch (Exception ignored) {}
        try { soundVineBoom= soundPool.load(getContext(), R.raw.vine_boom, 1); } catch (Exception ignored) {}
        try { soundMLG     = soundPool.load(getContext(), R.raw.mlg_horn,  1); } catch (Exception ignored) {}
        try { soundWhisper = soundPool.load(getContext(), R.raw.whisper,   1); } catch (Exception ignored) {}
    }

    // ── Surface Callbacks ────────────────────────────────────────────────────
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        groundY = (int)(screenHeight * 0.82f);
        player = new Player((int)(screenWidth * 0.15f), groundY);
        // Load saved high score from storage
        android.content.SharedPreferences prefs = getContext()
                .getSharedPreferences("VoiceRagePrefs", android.content.Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
        audioEngine.start();

        gameThread = new GameThread(getHolder(), this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        audioEngine.stop();
        if (gameThread != null) {
            gameThread.setRunning(false);
            try { gameThread.join(); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}

    // ── GAME UPDATE ──────────────────────────────────────────────────────────
    public void update() {
        if (gameOver) return;
        int amp = audioEngine.getAmplitude();
        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();

        // Start only when player makes sound
        if (!gameStarted) {
            if (level != AudioEngine.VoiceLevel.SILENT) gameStarted = true;
            else return;
        }

        score++;

        // Update player based on voice
        player.update(level, amp);

        // Scroll speed increases with score
        float scrollSpeed = 8 + (score / 200f);

        // Spawn obstacles
        long now = System.currentTimeMillis();
        long interval = (long) Math.max(1200, BASE_OBSTACLE_INTERVAL - (score / 1.5f));
        if (now - lastObstacleTime > interval) {
            // Occasionally spawn tall or wide obstacles for variety
            int type = random.nextInt(3);

// 👇 ADD THIS EXACTLY HERE
            if (type == 2 && player.speed > 15 && random.nextBoolean()) {
                type = random.nextInt(2);
            }

// Scale obstacle size with score
            float heightScale = 1f + (score / 2000f);
            heightScale = Math.min(heightScale, 2.2f);

            if (type == 0) {
                int h = (int)(55 * heightScale);
                obstacles.add(new Obstacle(screenWidth, groundY - h, 90, h));
            } else if (type == 1) {
                int h = (int)(120 * heightScale);
                obstacles.add(new Obstacle(screenWidth, groundY - h, 55, h));
            } else {
                int h = (int)(70 * heightScale);
                float minGap = 300 + (scrollSpeed * 10);
                obstacles.add(new Obstacle(screenWidth, groundY - h, 60, h));
                obstacles.add(new Obstacle(screenWidth + minGap, groundY - h, 60, h));
            }
            lastObstacleTime = now;
        }

        // Move + check obstacles
        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle obs = it.next();
            obs.x -= scrollSpeed;
            if (obs.x + obs.width < 0) { it.remove(); continue; }

            // Collision detection — RIP
            if (RectF.intersects(player.getBounds(), obs.getBounds())) {
                gameOver = true;
                // Save high score if beaten
                int currentScore = score / 10;
                if (currentScore > highScore) {
                    highScore = currentScore;
                    android.content.SharedPreferences prefs = getContext()
                            .getSharedPreferences("VoiceRagePrefs", android.content.Context.MODE_PRIVATE);
                    prefs.edit().putInt("highScore", highScore).apply();
                }
                playSound(soundVineBoom);
                playSound(soundOof);
                return;

            }
        }

        // Rage effects
        if (level == AudioEngine.VoiceLevel.RAGE) {
            shakeX = random.nextInt(18) - 9;
            shakeY = random.nextInt(18) - 9;
            bgHue = 0f;

            if (now - lastRageSoundTime > 2000) {
                playSound(soundBruh);
                playSound(soundMLG);
                lastRageSoundTime = now;
            }
        } else {
            shakeX = 0;
            shakeY = 0;
            bgHue = 220f;
        }

// 👇 PUT WHISPER HERE (OUTSIDE)
        if (level == AudioEngine.VoiceLevel.WHISPER) {
            if (now - lastWhisperSoundTime > 3000) {
                playSound(soundWhisper);
                lastWhisperSoundTime = now;
            }
        }
    }

    // ── GAME DRAW ────────────────────────────────────────────────────────────
    public void draw(Canvas canvas) {
        if (canvas == null) return;

        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();
        int amp = audioEngine.getAmplitude();

        // ── Background ─────────────────────────────────────────────────────
        if (level == AudioEngine.VoiceLevel.RAGE) {
            // Alternating red flicker = CHAOS
            canvas.drawColor(random.nextInt(2) == 0
                    ? Color.rgb(200, 0, 0)
                    : Color.rgb(255, 30, 30));
        } else {
            canvas.drawColor(Color.rgb(15, 15, 35));
        }

        // Shake transform
        canvas.save();
        canvas.translate(shakeX, shakeY);

        // ── Stars (background detail) ──────────────────────────────────────
        paint.setColor(Color.argb(80, 255, 255, 255));
        paint.setStrokeWidth(2);
        for (int i = 0; i < 30; i++) {
            float sx = (i * 137 + score) % screenWidth;
            float sy = (i * 97) % (groundY - 50);
            canvas.drawCircle(sx, sy, 2, paint);
        }

        // ── Ground ────────────────────────────────────────────────────────
        // Base ground
        paint.setColor(Color.rgb(60, 160, 60));
        canvas.drawRect(0, groundY, screenWidth, groundY + 12, paint);
        // Darker lower ground
        paint.setColor(Color.rgb(40, 100, 40));
        canvas.drawRect(0, groundY + 12, screenWidth, screenHeight, paint);

        // ── Obstacles ────────────────────────────────────────────────────
        for (Obstacle obs : obstacles) {
            // Gradient-ish look: lighter top
            paint.setColor(Color.rgb(255, 90, 20));
            canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, paint);
            paint.setColor(Color.rgb(200, 50, 0));
            canvas.drawRect(obs.x, obs.y + obs.height - 10,
                    obs.x + obs.width, obs.y + obs.height, paint);
        }

        // ── Player ───────────────────────────────────────────────────────
        // Body color
        if (player.isRaging) {
            paint.setColor(Color.rgb(255, 50, 50));
        } else if (level == AudioEngine.VoiceLevel.SHOUT) {
            paint.setColor(Color.rgb(255, 200, 50));
        } else {
            paint.setColor(Color.rgb(50, 200, 255));
        }
        canvas.drawRoundRect(
                new RectF(player.x, player.y, player.x + player.width, player.y + player.height),
                16, 16, paint);

        // Player eyes
        paint.setColor(Color.WHITE);
        canvas.drawCircle(player.x + 22, player.y + 30, 12, paint);
        canvas.drawCircle(player.x + 67, player.y + 30, 12, paint);

        // Pupils (look towards motion)
        paint.setColor(Color.BLACK);
        float pupilOffset = player.isOnGround ? 4 : -2;
        canvas.drawCircle(player.x + 26, player.y + 30 + pupilOffset, 6, paint);
        canvas.drawCircle(player.x + 71, player.y + 30 + pupilOffset, 6, paint);

        // Mouth — changes with voice level
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        if (level == AudioEngine.VoiceLevel.RAGE) {
            // Angry zigzag mouth
            float mx = player.x + 15;
            float my = player.y + 65;
            canvas.drawLine(mx, my, mx+15, my-10, paint);
            canvas.drawLine(mx+15, my-10, mx+30, my+5, paint);
            canvas.drawLine(mx+30, my+5, mx+45, my-8, paint);
            canvas.drawLine(mx+45, my-8, mx+60, my, paint);
        } else if (level == AudioEngine.VoiceLevel.SHOUT) {
            // O mouth (screaming)
            paint.setStyle(Paint.Style.FILL);
            canvas.drawOval(new RectF(player.x+30, player.y+55, player.x+60, player.y+80), paint);
        } else if (level == AudioEngine.VoiceLevel.WHISPER) {
            // Tiny smile
            canvas.drawArc(new RectF(player.x+25, player.y+55, player.x+65, player.y+80),
                    0, 180, false, paint);
        } else {
            // Flat line
            canvas.drawLine(player.x + 25, player.y + 68, player.x + 65, player.y + 68, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // ── UI Overlay ────────────────────────────────────────────────────

        // Score
        paint.setColor(Color.WHITE);
        paint.setTextSize(55);
        canvas.drawText("SCORE: " + (score / 10), 40, 60, paint);
        paint.setColor(Color.rgb(255, 215, 0)); // gold colour
        paint.setTextSize(40);
        canvas.drawText("BEST: " + highScore, 40, 110, paint);

// Difficulty label
        String diffLabel;
        int realScore = score / 10;

        if      (realScore < 20)  diffLabel = "EASY 😊";
        else if (realScore < 50)  diffLabel = "MEDIUM 😬";
        else if (realScore < 100) diffLabel = "HARD 😤";
        else if (realScore < 200) diffLabel = "INSANE 😱";
        else                      diffLabel = "💀 WHY ARE YOU STILL ALIVE";

        paint.setTextSize(38);
        paint.setColor(Color.YELLOW);
        canvas.drawText(diffLabel, screenWidth - 500, 60, paint);
        // ── Start Screen ─────────────────────────────────────────────────
        if (!gameStarted) {
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(80);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("VOICE RAGE GAME", screenWidth / 2f, screenHeight / 2f - 80, paint);
            paint.setTextSize(45);
            canvas.drawText("Make a sound to START", screenWidth / 2f, screenHeight / 2f, paint);
            paint.setTextSize(35);
            canvas.drawText("Whisper=Slow  |  Shout=Jump  |  RAGE=Chaos", screenWidth / 2f, screenHeight / 2f + 60, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        canvas.restore(); // End shake transform

        // ── Game Over Screen (outside shake for readability) ──────────────
        if (gameOver) {
            paint.setColor(Color.argb(210, 0, 0, 0));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

            paint.setColor(Color.rgb(255, 80, 80));
            paint.setTextSize(110);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f - 80, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(55);
            canvas.drawText("Score: " + (score / 10), screenWidth / 2f, screenHeight / 2f + 10, paint);
            paint.setColor(Color.rgb(255, 215, 0));
            canvas.drawText("Best: " + highScore, screenWidth / 2f, screenHeight / 2f + 75, paint);

            paint.setTextSize(40);
            paint.setColor(Color.rgb(180, 255, 180));
            canvas.drawText("Tap anywhere to try again", screenWidth / 2f, screenHeight / 2f + 140, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    // ── Touch = Restart ──────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameOver) {
            restart();
        }
        return true;
    }

    private void restart() {
        obstacles.clear();
        score = 0;
        gameOver = false;
        gameStarted = false;
        player.reset(groundY);
        lastObstacleTime = 0;
    }

    // ── Helper methods ───────────────────────────────────────────────────────
    private void playSound(int soundId) {
        if (soundPool != null && soundId != -1) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    private String getVoiceLabel(AudioEngine.VoiceLevel level) {
        switch (level) {
            case SILENT:  return "...silent... 💤";
            case WHISPER: return "psst psst 🤫";
            case NORMAL:  return "normal talk 🗣️";
            case SHOUT:   return "YELLING 😱";
            case RAGE:    return "💀 FULL RAGE MODE 💀";
            default:      return "";
        }
    }

    private int getVoiceColor(AudioEngine.VoiceLevel level) {
        switch (level) {
            case SILENT:  return Color.rgb(150, 150, 150);
            case WHISPER: return Color.rgb(150, 230, 255);
            case NORMAL:  return Color.rgb(100, 255, 100);
            case SHOUT:   return Color.rgb(255, 200, 0);
            case RAGE:    return Color.rgb(255, 60, 60);
            default:      return Color.WHITE;
        }
    }
}