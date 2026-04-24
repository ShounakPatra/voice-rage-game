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
    private int selectedMode = -1; // -1 = not selected yet
    private GameThread gameThread;
    private final AudioEngine audioEngine;
    private Player player;
    private SoundPool soundPool;

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int screenWidth, screenHeight, groundY;

    private int score     = 0;
    private int highScore = 0;
    private boolean gameOver    = false;
    private boolean gameStarted = false;
    private float shakeX = 0, shakeY = 0;


    private long lastObstacleTime = 0;
    private static final long BASE_OBSTACLE_INTERVAL = 4200;

    private int soundFahh     = -1;
    private int soundWow      = -1;
    private int soundCatLaugh = -1;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        audioEngine = new AudioEngine();
    }

    public void pause() {
        if (gameThread != null) {
            gameThread.setRunning(false);
            try {
                gameThread.join(500);
            } catch (InterruptedException ignored) {}
            gameThread = null;
        }
        audioEngine.stop();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void resume() {
        if (soundPool == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
            try { soundFahh     = soundPool.load(getContext(), R.raw.fahh,            1); } catch (Exception ignored) {}
            try { soundWow      = soundPool.load(getContext(), R.raw.anime_wow_sound,  1); } catch (Exception ignored) {}
            try { soundCatLaugh = soundPool.load(getContext(), R.raw.cat_laugh_meme_1, 1); } catch (Exception ignored) {}
        }

        audioEngine.start();
        if (gameThread == null && getHolder().getSurface().isValid()) {
            gameThread = new GameThread(getHolder(), this);
            gameThread.setRunning(true);
            gameThread.start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth  = getWidth();
        screenHeight = getHeight();
        groundY      = (int)(screenHeight * 0.82f);
        if (player == null) {
            player = new Player((int) (screenWidth * 0.15f), groundY);
        }

        android.content.SharedPreferences prefs = getContext()
                .getSharedPreferences("VoiceRagePrefs", android.content.Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);

        resume();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}

    public void update() {
        if (gameOver || player == null) return;

        int amp = audioEngine.getAmplitude();
        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();

        if (!gameStarted) {
            // Wait for sound to start once mode is selected
            if (selectedMode != -1 && level != AudioEngine.VoiceLevel.SILENT) {
                gameStarted = true;
            } else {
                return;
            }
        }

        score++;
        player.update(level, amp);
        shakeX *= 0.85f;
        shakeY *= 0.85f;

        int milestone = (score / 10) / 100;
        float scrollSpeed = 8 + (milestone * 3.5f) + ((score % 2000) / 400f);

        long now = System.currentTimeMillis();
        long interval = (long) Math.max(1200, BASE_OBSTACLE_INTERVAL - (score / 1.5f));

        synchronized (obstacles) {
            if (now - lastObstacleTime > interval) {
                int type = random.nextInt(3);
                float heightScale = Math.min(1f + (score / 2000f), 2.2f);

                if (type == 0) {
                    int h = (int) (55 * heightScale);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 90, h));
                } else if (type == 1) {
                    int h = (int) (120 * heightScale);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 55, h));
                } else {
                    int h = (int) (70 * heightScale);
                    float gap = 520 + (scrollSpeed * 15);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 60, h));
                    obstacles.add(new Obstacle(screenWidth + gap, groundY - h, 60, h));
                }
                lastObstacleTime = now;
            }

            Iterator<Obstacle> it = obstacles.iterator();
            while (it.hasNext()) {
                Obstacle obs = it.next();
                obs.x -= scrollSpeed;
                if (obs.x + obs.width < 0) {
                    it.remove();
                    continue;
                }

                // passed obstacle without hitting = wow
                if (!obs.passed && obs.x + obs.width < player.x) {
                    obs.passed = true;
                    playSound(soundWow);
                }

                // collision = cat laugh
                if (RectF.intersects(player.getBounds(), obs.getBounds())) {
                    gameOver = true;
                    int currentScore = score / 10;
                    if (currentScore > highScore) {
                        highScore = currentScore;
                        android.content.SharedPreferences prefs = getContext()
                                .getSharedPreferences("VoiceRagePrefs", android.content.Context.MODE_PRIVATE);
                        prefs.edit().putInt("highScore", highScore).apply();
                    }
                    playSound(soundCatLaugh);
                    return;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null || player == null) return;

        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();
        int amp = audioEngine.getAmplitude();

        if (level == AudioEngine.VoiceLevel.RAGE) {
            canvas.drawColor(random.nextInt(2) == 0
                    ? Color.rgb(200, 0, 0) : Color.rgb(255, 30, 30));
        } else {
            canvas.drawColor(Color.rgb(15, 15, 35));
        }

        canvas.save();
        canvas.translate(shakeX, shakeY);

        // Far stars
        paint.setColor(Color.argb(50, 255, 255, 255));
        for (int i = 0; i < 40; i++) {
            float sx = (i * 137 + score / 4f) % screenWidth;
            float sy = (i * 97) % (groundY * 0.4f);
            canvas.drawCircle(sx, sy, 1.5f, paint);
        }

        // Mid-stars
        paint.setColor(Color.argb(100, 200, 220, 255));
        for (int i = 0; i < 20; i++) {
            float sx = (i * 173 + score / 2f) % screenWidth;
            float sy = (i * 113) % (groundY * 0.7f);
            canvas.drawCircle(sx, sy, 3f, paint);
        }

        // Clouds
        paint.setColor(Color.argb(18, 255, 255, 255));
        for (int i = 0; i < 5; i++) {
            float sx = (i * 400 + score) % (screenWidth + 200) - 100;
            canvas.drawOval(new RectF(sx, 80 + i * 60, sx + 220, 150 + i * 60), paint);
        }

        // Mountains
        paint.setColor(Color.argb(60, 80, 80, 140));
        canvas.drawRect(0, groundY - 180, screenWidth / 3f, groundY, paint);
        paint.setColor(Color.argb(50, 60, 60, 120));
        canvas.drawRect(screenWidth / 4f, groundY - 140, screenWidth * 0.6f, groundY, paint);
        paint.setColor(Color.argb(40, 40, 40, 100));
        canvas.drawRect(screenWidth / 2f, groundY - 100, screenWidth, groundY, paint);

        // Ground
        paint.setColor(Color.rgb(60, 160, 60));
        canvas.drawRect(0, groundY, screenWidth, groundY + 12, paint);
        paint.setColor(Color.rgb(40, 100, 40));
        canvas.drawRect(0, groundY + 12, screenWidth, screenHeight, paint);

        // Obstacles
        // Cactus obstacles
        synchronized (obstacles) {
            for (Obstacle obs : obstacles) {
                float cx = obs.x, cy = obs.y, cw = obs.width, ch = obs.height;

                // Main trunk
                paint.setColor(Color.rgb(34, 139, 34));
                canvas.drawRoundRect(new RectF(cx + cw*0.3f, cy, cx + cw*0.7f, cy + ch), 8, 8, paint);

                // Left arm — appears only if cactus tall enough
                if (ch > 60) {
                    float armY = cy + ch * 0.35f;
                    // horizontal part
                    canvas.drawRoundRect(new RectF(cx, armY, cx + cw*0.32f, armY + cw*0.28f), 6,6,paint);
                    // vertical part going up
                    canvas.drawRoundRect(new RectF(cx, armY - ch*0.20f, cx + cw*0.32f, armY + cw*0.28f), 6,6,paint);
                }

                // Right arm — appears only if cactus tall enough
                if (ch > 80) {
                    float armY = cy + ch * 0.50f;
                    canvas.drawRoundRect(new RectF(cx + cw*0.68f, armY, cx + cw, armY + cw*0.28f), 6,6,paint);
                    canvas.drawRoundRect(new RectF(cx + cw*0.68f, armY - ch*0.15f, cx + cw, armY + cw*0.28f), 6,6,paint);
                }

                // Dark green shading on trunk right side
                paint.setColor(Color.rgb(20, 100, 20));
                canvas.drawRoundRect(new RectF(cx + cw*0.55f, cy, cx + cw*0.70f, cy + ch), 8,8,paint);

                // Spikes on trunk
                paint.setColor(Color.rgb(200, 220, 180));
                paint.setStrokeWidth(2.5f);
                paint.setStyle(Paint.Style.STROKE);
                for (int s = 0; s < 4; s++) {
                    float sy2 = cy + ch * (0.2f + s * 0.2f);
                    // left spikes
                    canvas.drawLine(cx + cw*0.30f, sy2, cx + cw*0.05f, sy2 - 10, paint);
                    canvas.drawLine(cx + cw*0.30f, sy2 + 8, cx + cw*0.05f, sy2 + 18, paint);
                    // right spikes
                    canvas.drawLine(cx + cw*0.70f, sy2, cx + cw*0.95f, sy2 - 10, paint);
                    canvas.drawLine(cx + cw*0.70f, sy2 + 8, cx + cw*0.95f, sy2 + 18, paint);
                }
                paint.setStyle(Paint.Style.FILL);

                // Base/ground bump
                paint.setColor(Color.rgb(34, 139, 34));
                canvas.drawRoundRect(new RectF(cx + cw*0.15f, cy + ch - 12, cx + cw*0.85f, cy + ch), 5,5,paint);
            }
        }

        // ── Bootleg Mario ──────────────────────────────────────────────────
        float px = player.x, py = player.y, pw = player.width, ph = player.height;

        int overallsColor = Color.rgb(30, 80, 200);
        int shirtColor = player.isRaging ? Color.rgb(255,50,50)
                : (level == AudioEngine.VoiceLevel.SHOUT ? Color.rgb(255,120,0) : Color.rgb(220,40,40));

        // Legs
        paint.setColor(overallsColor);
        if (player.isOnGround) {
            long t = System.currentTimeMillis(); boolean lf = (t/150)%2==0;
            canvas.drawRect(px+pw*0.15f, py+ph*(lf?.65f:.72f), px+pw*0.42f, py+ph, paint);
            canvas.drawRect(px+pw*0.50f, py+ph*(lf?.72f:.65f), px+pw*0.78f, py+ph, paint);
        } else {
            canvas.drawRect(px+pw*0.15f, py+ph*0.70f, px+pw*0.42f, py+ph*0.92f, paint);
            canvas.drawRect(px+pw*0.52f, py+ph*0.70f, px+pw*0.80f, py+ph*0.92f, paint);
        }

        // Shoes
        paint.setColor(Color.rgb(80,40,10));
        if (player.isOnGround) {
            long t = System.currentTimeMillis(); boolean lf = (t/150)%2==0;
            canvas.drawRoundRect(new RectF(px+pw*0.04f, py+ph*(lf?.85f:.90f), px+pw*0.46f, py+ph), 6,6,paint);
            canvas.drawRoundRect(new RectF(px+pw*0.50f, py+ph*(lf?.90f:.85f), px+pw*0.88f, py+ph), 6,6,paint);
        } else {
            canvas.drawRoundRect(new RectF(px+pw*0.08f, py+ph*0.82f, px+pw*0.44f, py+ph*0.96f), 6,6,paint);
            canvas.drawRoundRect(new RectF(px+pw*0.50f, py+ph*0.82f, px+pw*0.86f, py+ph*0.96f), 6,6,paint);
        }

        // Overalls bib
        paint.setColor(overallsColor);
        canvas.drawRoundRect(new RectF(px+pw*0.18f, py+ph*0.42f, px+pw*0.82f, py+ph*0.70f), 8,8,paint);

        // Shirt
        paint.setColor(shirtColor);
        canvas.drawRoundRect(new RectF(px+pw*0.10f, py+ph*0.38f, px+pw*0.90f, py+ph*0.68f), 10,10,paint);

        // Straps + buttons
        paint.setColor(overallsColor);
        canvas.drawRect(px+pw*0.25f, py+ph*0.32f, px+pw*0.40f, py+ph*0.50f, paint);
        canvas.drawRect(px+pw*0.60f, py+ph*0.32f, px+pw*0.75f, py+ph*0.50f, paint);
        paint.setColor(Color.rgb(255,220,50));
        canvas.drawCircle(px+pw*0.32f, py+ph*0.36f, 5, paint);
        canvas.drawCircle(px+pw*0.68f, py+ph*0.36f, 5, paint);

        // Arms
        paint.setColor(shirtColor);
        if (!player.isOnGround) {
            canvas.drawRoundRect(new RectF(px-pw*0.08f, py+ph*0.28f, px+pw*0.18f, py+ph*0.52f), 8,8,paint);
            canvas.drawRoundRect(new RectF(px+pw*0.82f, py+ph*0.28f, px+pw*1.08f, py+ph*0.52f), 8,8,paint);
        } else {
            canvas.drawRoundRect(new RectF(px-pw*0.08f, py+ph*0.40f, px+pw*0.18f, py+ph*0.62f), 8,8,paint);
            canvas.drawRoundRect(new RectF(px+pw*0.82f, py+ph*0.40f, px+pw*1.08f, py+ph*0.62f), 8,8,paint);
        }

        // Gloves
        paint.setColor(Color.WHITE);
        canvas.drawCircle(px-pw*0.02f, py+ph*(player.isOnGround?.64f:.26f), 10, paint);
        canvas.drawCircle(px+pw*1.02f, py+ph*(player.isOnGround?.64f:.26f), 10, paint);

        // Face
        paint.setColor(Color.rgb(255,200,140));
        canvas.drawRoundRect(new RectF(px+pw*0.15f, py+ph*0.08f, px+pw*0.85f, py+ph*0.40f), 18,18,paint);

        // Hat
        paint.setColor(shirtColor);
        canvas.drawRoundRect(new RectF(px+pw*0.05f, py+ph*0.12f, px+pw*0.95f, py+ph*0.22f), 6,6,paint);
        canvas.drawRoundRect(new RectF(px+pw*0.18f, py-ph*0.04f, px+pw*0.82f, py+ph*0.16f), 10,10,paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(px+pw*0.50f, py+ph*0.06f, 12, paint);
        paint.setColor(shirtColor);
        paint.setTextSize(18);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("V", px+pw*0.50f, py+ph*0.11f, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Mustache
        paint.setColor(Color.rgb(80,40,10));
        canvas.drawRoundRect(new RectF(px+pw*0.20f, py+ph*0.30f, px+pw*0.46f, py+ph*0.40f), 8,8,paint);
        canvas.drawRoundRect(new RectF(px+pw*0.54f, py+ph*0.30f, px+pw*0.80f, py+ph*0.40f), 8,8,paint);

        // Eyes
        paint.setColor(Color.WHITE);
        canvas.drawCircle(px+pw*0.33f, py+ph*0.22f, 9, paint);
        canvas.drawCircle(px+pw*0.67f, py+ph*0.22f, 9, paint);
        paint.setColor(Color.rgb(60,30,10));
        float ed = player.isOnGround ? 2f : -2f;
        canvas.drawCircle(px+pw*0.35f, py+ph*0.22f+ed, 5, paint);
        canvas.drawCircle(px+pw*0.69f, py+ph*0.22f+ed, 5, paint);

        // Nose
        paint.setColor(Color.rgb(220,130,100));
        canvas.drawOval(new RectF(px+pw*0.40f, py+ph*0.26f, px+pw*0.62f, py+ph*0.34f), paint);

        // Rage steam
        if (player.isRaging) {
            paint.setColor(Color.argb(180, 255, 100, 0));
            canvas.drawCircle(px+pw*0.3f, py-15, 8, paint);
            canvas.drawCircle(px+pw*0.5f, py-22, 11, paint);
            canvas.drawCircle(px+pw*0.7f, py-15, 8, paint);
        }

        // ── UI ──────────────────────────────────────────────────────────────
        paint.setColor(Color.WHITE);
        paint.setTextSize(55);
        canvas.drawText("SCORE: " + (score / 10), 40, 60, paint);

        paint.setColor(Color.rgb(255, 215, 0));
        paint.setTextSize(40);
        canvas.drawText("BEST: " + highScore, 40, 110, paint);

        int rs = score / 10;
        int difficultyLevel = rs / 100;

        String dl = difficultyLevel == 0 ? "EASY 😊" :
                difficultyLevel == 1 ? "MEDIUM 😬" :
                        difficultyLevel == 2 ? "HARD 😤" :
                                difficultyLevel == 3 ? "INSANE 😱" :
                                        "💀 WHY ARE YOU STILL ALIVE";
        paint.setTextSize(38);
        paint.setColor(Color.YELLOW);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(dl, screenWidth - 40, 60, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        float barW = Math.min(amp / 8000f, 1f) * screenWidth;
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRect(0, screenHeight-40, screenWidth, screenHeight-10, paint);
        paint.setColor(getVoiceColor(level));
        canvas.drawRect(0, screenHeight-40, barW, screenHeight-10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(28);
        canvas.drawText("VOICE METER", 20, screenHeight-16, paint);

        // Start screen
        // Mode select + start screen
        if (!gameStarted) {
            paint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

            // App icon
            try {
                android.graphics.drawable.Drawable icon = getContext().getPackageManager()
                        .getApplicationIcon(getContext().getApplicationInfo());
                int sz = 160, ix = (int)(screenWidth/2f - sz/2f), iy = 30;
                icon.setBounds(ix, iy, ix+sz, iy+sz);
                icon.draw(canvas);
            } catch (Exception ignored) {}

            paint.setTextAlign(Paint.Align.CENTER);

            if (selectedMode == -1) {
                // Mode selection
                paint.setColor(Color.WHITE);
                paint.setTextSize(55);
                canvas.drawText("SELECT MODE", screenWidth/2f, 240, paint);

                // Silent mode button
                paint.setColor(Color.rgb(100, 200, 255));
                canvas.drawRoundRect(new RectF(screenWidth/2f-320, 290, screenWidth/2f+320, 390), 20,20,paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(40);
                canvas.drawText("🤫  SILENT MODE", screenWidth/2f, 355, paint);
                paint.setColor(Color.argb(150,255,255,255));
                paint.setTextSize(26);
                canvas.drawText("Quiet room — whisper to jump", screenWidth/2f, 415, paint);

                // Normal mode button
                paint.setColor(Color.rgb(100, 255, 120));
                canvas.drawRoundRect(new RectF(screenWidth/2f-320, 450, screenWidth/2f+320, 550), 20,20,paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(40);
                canvas.drawText("🗣️  NORMAL MODE", screenWidth/2f, 515, paint);
                paint.setColor(Color.argb(150,255,255,255));
                paint.setTextSize(26);
                canvas.drawText("Normal room — speak to jump", screenWidth/2f, 575, paint);

                // Rage mode button
                paint.setColor(Color.rgb(255, 80, 80));
                canvas.drawRoundRect(new RectF(screenWidth/2f-320, 610, screenWidth/2f+320, 710), 20,20,paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(40);
                canvas.drawText("💀  RAGE MODE", screenWidth/2f, 675, paint);
                paint.setColor(Color.argb(150,255,255,255));
                paint.setTextSize(26);
                canvas.drawText("Crowded room — SCREAM to jump", screenWidth/2f, 735, paint);

            } else {
                // Mode selected, waiting for sound
                String modeName = selectedMode==0?"🤫 SILENT":selectedMode==1?"🗣️ NORMAL":"💀 RAGE";
                paint.setColor(Color.YELLOW);
                paint.setTextSize(45);
                canvas.drawText(modeName + " MODE", screenWidth/2f, screenHeight/2f - 40, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(38);
                canvas.drawText("Make a sound to START!", screenWidth/2f, screenHeight/2f + 30, paint);
            }

            paint.setTextAlign(Paint.Align.LEFT);
        }

        canvas.restore();

        // Game over
        if (gameOver) {
            paint.setColor(Color.argb(210, 0, 0, 0));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            paint.setColor(Color.rgb(255, 80, 80));
            paint.setTextSize(110);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GAME OVER", screenWidth/2f, screenHeight/2f-80, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(55);
            canvas.drawText("Score: " + (score/10), screenWidth/2f, screenHeight/2f+10, paint);
            paint.setColor(Color.rgb(255,215,0));
            canvas.drawText("Best: " + highScore, screenWidth/2f, screenHeight/2f+75, paint);
            paint.setTextSize(40);
            paint.setColor(Color.rgb(180,255,180));
            canvas.drawText("Tap anywhere to try again", screenWidth/2f, screenHeight/2f+140, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        if (gameOver) {
            restart();
            return true;
        }

        // Mode selection taps
        if (!gameStarted && selectedMode == -1) {
            float tx = event.getX(), ty = event.getY();
            float cx = screenWidth / 2f;

            if (tx > cx - 320 && tx < cx + 320) {
                if (ty > 290 && ty < 390) {
                    // Silent mode — whisper threshold
                    selectedMode = 0;
                    Player.jumpAmpThreshold = 800;
                    Player.jumpDeltaThreshold = 300;
                } else if (ty > 450 && ty < 550) {
                    // Normal mode
                    selectedMode = 1;
                    Player.jumpAmpThreshold = 2500;
                    Player.jumpDeltaThreshold = 800;
                } else if (ty > 610 && ty < 710) {
                    // Rage mode — loud threshold
                    selectedMode = 2;
                    Player.jumpAmpThreshold = 6000;
                    Player.jumpDeltaThreshold = 2500;
                }
            }
            return true;
        }

        return true;
    }

    private void restart() {
        synchronized (obstacles) {
            obstacles.clear();
        }
        score = 0;
        gameOver = false;
        gameStarted = false;
        if (player != null) player.reset(groundY);
        lastObstacleTime = 0;
        if (soundPool != null) soundPool.autoPause();
        selectedMode = -1;
    }

    private void playSound(int soundId) {
        if (soundPool != null && soundId != -1) soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    private int getVoiceColor(AudioEngine.VoiceLevel level) {
        switch (level) {
            case SILENT:  return Color.rgb(150,150,150);
            case WHISPER: return Color.rgb(150,230,255);
            case NORMAL:  return Color.rgb(100,255,100);
            case SHOUT:   return Color.rgb(255,200,0);
            case RAGE:    return Color.rgb(255,60,60);
            default:      return Color.WHITE;
        }
    }
}