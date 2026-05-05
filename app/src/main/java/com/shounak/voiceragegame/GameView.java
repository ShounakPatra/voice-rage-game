package com.shounak.voiceragegame;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    public final SettingsManager sm = new SettingsManager();
    // FIX: Settings fields were removed during refactor but usages remain everywhere.
    // Re-declared here. SettingsManager is kept as the persistence/save layer only.
    private int selectedMode = -1;
    private float sensitivityPct = 50f;
    private float hudOpacityPct = 100f;
    private boolean shadowsEnabled = false;
    private int graphicsQualityIndex = 1;
    private int shadowPresetIndex = 1;
    private int shadowResolutionIndex = 2;
    private int shadowCascadesIndex = 1;
    private boolean softShadows = true;
    private int msaaIndex = 1;
    private float saturationPct = 50f;
    private float contrastPct = 50f;
    private float exposurePct = 50f;
    private boolean highSunIntensity = false;
    private float masterAudioBoostPct = 50f;
    private boolean dailyChallenge = false;
    private boolean hapticsEnabled = true;

    public final ParticleSystem ps = new ParticleSystem();
    public final BackgroundRenderer backgroundRenderer;
    public final CharacterRenderer characterRenderer;
    public final WorldRenderer worldRenderer;
    public final ObstacleRenderer obstacleRenderer;
    public final GameUI gameUI;

    private boolean inSettings = false;
    private int activeSettingsTab = 0;

    private boolean draggingSlider = false;
    private int activeSlider = 0;
    private float activeSliderLeft = 0f;
    private float activeSliderRight = 1f;
    private int activeDropdown = 0;
    private boolean draggingSettingsTabs = false;
    private float settingsTabsScrollX = 0f;
    private float settingsTabsDownX = 0f;
    private float settingsTabsLastX = 0f;
    private boolean settingsTabsMoved = false;

    private boolean calibrating = false;
    private long calibrationStartTime = 0;
    private int calibrationPeakAmplitude = 0;
    private long calibrationAmplitudeSum = 0;
    private int calibrationSampleCount = 0;
    private int calibrationNoiseFloor = 0;
    private int dailyBestScore = 0;
    private int finalScore = 0;
    private String failMessage = "Your scream had no plot armor";
    private long lastRageHapticTime = 0;
    private long lastHapticTimestamp = 0;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private android.media.AudioFocusRequest audioFocusRequest;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
    };
    private boolean hadAudioFocus = false;
    private boolean premiumRendering = true;
    private boolean hardwareCanvasEnabled = false;
    private long runEventBannerUntil = 0;
    private String runEventBannerTitle = "";
    private String runEventBannerSubtitle = "";
    private boolean skyRaidAnnounced = false;
    private static final float SHARE_BTN_SIZE = 80f;
    private static final float SHARE_BTN_MARGIN = 20f;
    private boolean isCapturingShare = false;

    // Back-press-to-exit state
    private boolean backPressedOnce = false;
    private long backPressTime = 0;
    private static final long BACK_PRESS_INTERVAL = 2000; // 2 seconds window

    public static final int SETTINGS_TAB_MODES = 0;
    public static final int SETTINGS_TAB_UI = 1;
    public static final int SETTINGS_TAB_SENSITIVITY = 2;
    public static final int SETTINGS_TAB_VOICE = SETTINGS_TAB_SENSITIVITY;
    public static final int SETTINGS_TAB_GRAPHICS = 3;
    public static final int SETTINGS_TAB_POST = 4;
    public static final int SETTINGS_TAB_LIGHTING = 5;
    public static final int SETTINGS_TAB_AUDIO = 6;
    public static final int SETTINGS_TAB_GAMEPLAY = 7;
    public static final int SETTINGS_TAB_STATS = 8;
    public static final int SETTINGS_TAB_COUNT = 9;
    public static final String[] SETTINGS_TABS = {
            "Modes", "UI", "Sensitivity", "Graphics", "Post", "Lighting", "Audio", "Game", "Stats"
    };
    public static final int SLIDER_NONE = 0;
    public static final int SLIDER_SENSITIVITY = 1;
    public static final int SLIDER_HUD_OPACITY = 2;
    public static final int SLIDER_SATURATION = 3;
    public static final int SLIDER_CONTRAST = 4;
    public static final int SLIDER_EXPOSURE = 5;
    public static final int SLIDER_AUDIO_BOOST = 6;
    public static final int DROPDOWN_NONE = 0;
    public static final int DROPDOWN_SHADOW_PRESET = 1;
    public static final int DROPDOWN_SHADOW_RESOLUTION = 2;
    public static final int DROPDOWN_SHADOW_CASCADES = 3;
    public static final int DROPDOWN_MSAA = 4;
    public static final int DROPDOWN_GRAPHICS_QUALITY = 5;
    public static final String[] GRAPHICS_QUALITY_LABELS = {"Low", "Balanced", "High", "Ultra"};
    public static final String[] SHADOW_PRESET_LABELS = {"Low", "Balanced", "High", "Ultra", "Custom"};
    public static final String[] SHADOW_RESOLUTION_LABELS = {"256", "512", "1024", "2048", "4096"};
    public static final String[] SHADOW_CASCADE_LABELS = {"1", "2", "3", "4"};
    public static final String[] MSAA_LABELS = {"Off", "2x", "4x"};

    private boolean showingFirstTimePrompt = false;
    private boolean showingTutorial = false;
    private int tutorialStep = 0;
    private static final int TUTORIAL_STEPS = 5;

    private GameThread gameThread;
    private final AudioEngine audioEngine;
    private Player player;
    private SoundPool soundPool;

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();
    private final Random obstacleRandom = new Random();
    private long lastSparkTime = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groundBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint settingsBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint firstPromptBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint auxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path scratchPath = new android.graphics.Path();
    private boolean renderCacheReady = false;

    private int screenWidth, screenHeight, groundY;

    private int score = 0;
    private int highScore = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private boolean gamePaused = false;
    private float shakeX = 0, shakeY = 0;

    private long lastObstacleTime = 0;
    private static final long BASE_OBSTACLE_INTERVAL = 4200;
    private static final long CALIBRATION_DURATION_MS = 3000;
    private static final long CALIBRATION_WARMUP_MS = 350;
    private static final int CELESTIAL_CYCLE_SCORE = 300;
    private static final float TWO_PI = (float)(Math.PI * 2.0);

    private static final String[] FAIL_MESSAGES = {
            "Your scream had no plot armor",
            "Mic skill issue",
            "Too loud. Still not enough.",
            "The cactus heard everything",
            "Voice crack detected",
            "Certified rage moment",
            "Bro got clapped by silence"
    };

    private int soundWow = -1;
    private int soundCatLaugh = -1;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        audioEngine = new AudioEngine();
        setKeepScreenOn(true);

        backgroundRenderer = new BackgroundRenderer();
        characterRenderer = new CharacterRenderer(this, sm);
        worldRenderer = new WorldRenderer(this, sm);
        obstacleRenderer = new ObstacleRenderer(this);
        gameUI = new GameUI(this, sm);
    }

    public void pause() {
        if (gameThread != null) {
            gameThread.setRunning(false);
            try {
                gameThread.join(500);
            } catch (InterruptedException ignored) {
            }
            gameThread = null;
        }
        audioEngine.stop();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            // FIX: Reset IDs so stale IDs aren't used with new SoundPool on next resume()
            soundWow = -1;
            soundCatLaugh = -1;
        }
        // Release audio focus → music player resumes automatically
        if (audioManager != null && hadAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
            hadAudioFocus = false;
        }
    }

    public void resume() {
        if (soundPool == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
            try {
                soundWow = soundPool.load(getContext(), R.raw.awkward_moment, 1);
            } catch (Exception ignored) {
            }
            try {
                soundCatLaugh = soundPool.load(getContext(), R.raw.cat_laugh_meme_1, 1);
            } catch (Exception ignored) {
            }
        }

        audioEngine.start();
        if (gameThread == null && getHolder().getSurface().isValid()) {
            gameThread = new GameThread(getHolder(), this);
            gameThread.setRunning(true);
            gameThread.start();
        }
    }

    /**
     * Called by the Activity when the system back button is pressed.
     * @return true if the Activity should finish, false if we handled it internally.
     */
    public boolean onBackPressed() {
        // If in settings, close settings instead of exiting
        if (inSettings) {
            inSettings = false;
            draggingSlider = false;
            saveSettings();
            vibrate(15);
            return false;
        }
        // If showing tutorial, close it
        if (showingTutorial) {
            showingTutorial = false;
            vibrate(15);
            return false;
        }
        // If showing first-time prompt, close it
        if (showingFirstTimePrompt) {
            showingFirstTimePrompt = false;
            markLaunched();
            vibrate(15);
            return false;
        }
        // If game is running, use back as a quick pause/resume toggle.
        if (gameStarted && !gameOver) {
            gamePaused = !gamePaused;
            vibrate(15);
            return false;
        }

        // Double-press back to exit
        long now = System.currentTimeMillis();
        if (backPressedOnce && (now - backPressTime) < BACK_PRESS_INTERVAL) {
            return true; // let Activity finish
        }
        backPressedOnce = true;
        backPressTime = now;
        vibrate(20);
        return false;
    }

    @Override
    public void surfaceCreated(@androidx.annotation.NonNull SurfaceHolder holder) {
        configureSurfaceSize(getWidth(), getHeight());

        android.content.SharedPreferences prefs = getContext()
                .getSharedPreferences("VocexRunPrefs", android.content.Context.MODE_PRIVATE);
        sm.load(getContext());
        syncLocalSettingsFromManager();
        highScore = prefs.getInt("highScore", 0);
        finalScore = prefs.getInt("lastScore", 0);
        dailyBestScore = prefs.getInt(getDailyBestKey(), 0);
        initializeVibrator();
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest == null) {
            audioFocusRequest = new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
        }

        if (!prefs.getBoolean("hasLaunched", false)) {
            showingFirstTimePrompt = true;
        }
        applySelectedModeBaseThresholds();
        applySlider();
        applyRenderingSettings();
        syncAudioCalibration();

        resume();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public void surfaceChanged(@androidx.annotation.NonNull SurfaceHolder holder, int format, int w, int h) {
        configureSurfaceSize(w, h);
    }

    boolean shouldUseHardwareCanvas() {
        return hardwareCanvasEnabled;
    }

    private void updateRenderingTier() {
        ActivityManager activityManager =
                (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        boolean lowPerformanceMode = activityManager != null && activityManager.isLowRamDevice();
        long pixels = Math.max(1L, (long) screenWidth * Math.max(1, screenHeight));
        boolean normalSurface = pixels >= 700_000L;
        boolean canUsePremium = !lowPerformanceMode && pixels >= 420_000L;

        switch (graphicsQualityIndex) {
            case 0:
                premiumRendering = false;
                hardwareCanvasEnabled = false;
                break;
            case 2:
            case 3:
                premiumRendering = canUsePremium;
                hardwareCanvasEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && canUsePremium;
                break;
            case 1:
            default:
                premiumRendering = !lowPerformanceMode && normalSurface;
                hardwareCanvasEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && !lowPerformanceMode && normalSurface;
                break;
        }
    }

    private void configureSurfaceSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean sizeChanged = width != screenWidth || height != screenHeight;
        if (!sizeChanged && player != null) {
            return; // No size change, keep current state (optimizes 180-degree rotation)
        }

        screenWidth = width;
        screenHeight = height;
        groundY = (int) (screenHeight * 0.82f);

        updateRenderingTier();

        if (player == null || sizeChanged) {
            player = new Player((int) (screenWidth * 0.15f), groundY);
            synchronized (obstacles) {
                obstacles.clear();
            }
            ps.clear();
            gameStarted = false;
            gameOver = false;
            gamePaused = false;
            calibrating = false;
            skyRaidAnnounced = false;
            runEventBannerUntil = 0;
        }

        rebuildRenderCache();
    }

    private void rebuildRenderCache() {
        if (screenWidth <= 0 || screenHeight <= 0 || groundY <= 0) {
            renderCacheReady = false;
            return;
        }

        groundBandPaint.setShader(new android.graphics.LinearGradient(
                0, groundY, 0, groundY + 14,
                Color.rgb(195, 168, 100), Color.rgb(160, 130, 72),
                android.graphics.Shader.TileMode.CLAMP));

        settingsBgPaint.setShader(new android.graphics.LinearGradient(
                0, 0, screenWidth, screenHeight,
                new int[]{Color.rgb(5, 7, 22), Color.rgb(18, 19, 48),
                        Color.rgb(14, 46, 55)},
                new float[]{0f, 0.58f, 1f},
                android.graphics.Shader.TileMode.CLAMP));

        firstPromptBgPaint.setShader(new android.graphics.LinearGradient(
                0, 0, screenWidth, screenHeight,
                new int[]{Color.rgb(5, 7, 22), Color.rgb(20, 18, 48),
                        Color.rgb(40, 28, 18)},
                new float[]{0f, 0.62f, 1f},
                android.graphics.Shader.TileMode.CLAMP));

        renderCacheReady = true;
    }

    public void update() {
        if (gameOver || player == null) return;

        int amp = audioEngine.getAmplitude();
        int jumpPulseAmp = audioEngine.consumeJumpPulse();
        boolean instantJumpPulse = jumpPulseAmp > 0;
        if (instantJumpPulse) {
            amp = Math.max(amp, jumpPulseAmp);
        }
        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();

        if (calibrating) {
            long elapsed = System.currentTimeMillis() - calibrationStartTime;
            int sample = Math.max(amp, audioEngine.getSmoothedAmplitude());
            if (elapsed > CALIBRATION_WARMUP_MS) {
                calibrationPeakAmplitude = Math.max(calibrationPeakAmplitude, sample);
                calibrationAmplitudeSum += sample;
                calibrationSampleCount++;
                if (elapsed < CALIBRATION_WARMUP_MS + 850) {
                    calibrationNoiseFloor = Math.max(calibrationNoiseFloor, sample);
                }
            } else {
                calibrationNoiseFloor = Math.max(calibrationNoiseFloor, sample);
            }
            if (elapsed >= CALIBRATION_DURATION_MS) {
                finishCalibration();
            }
            return;
        }

        if (!gameStarted || gamePaused) return;

        long now = System.currentTimeMillis();
        score++;
        int runScoreNow = score / 10;
        if (!skyRaidAnnounced && runScoreNow >= 300) {
            skyRaidAnnounced = true;
            showRunEventBanner("SKY RAID UNLOCKED", "Flying enemies can now attack in waves", 2600L);
        }
        boolean jumped = player.update(level, amp, selectedMode, instantJumpPulse);
        if (jumped) {
            vibrate(30);
        }
        if (player.justLanded) {
            ps.spawnLandingDust(player, groundY, player.lastLandImpact, premiumRendering);
        }
        if (level == AudioEngine.VoiceLevel.RAGE && now - lastSparkTime > 46L) {
            ps.spawnRageSparks(player, premiumRendering);
            lastSparkTime = now;
        }
        ps.update(premiumRendering);
        if (level == AudioEngine.VoiceLevel.RAGE
                && now - lastRageHapticTime > 500) {
            vibrate(40);
            lastRageHapticTime = now;
        }
        shakeX *= 0.85f;
        shakeY *= 0.85f;

        int milestone = (score / 10) / 100;
        float warmup = Math.min(score / 900f, 1f);
        float scrollSpeed = 6 + (warmup * 2f) + (milestone * 3.5f) + ((score % 2000) / 400f);

        long interval = (long) Math.max(1400, BASE_OBSTACLE_INTERVAL + ((1f - warmup) * 1400f) - (score / 1.5f));

        synchronized (obstacles) {
            if (now - lastObstacleTime > interval) {
                int type = obstacleRandom.nextInt(3);
                int runScore = score / 10;
                boolean flyingUnlocked = runScore >= 300;
                float heightScale = Math.min(0.82f + (warmup * 0.18f) + (score / 2000f), 2.2f);

                if (flyingUnlocked && shouldSpawnFlyingEnemyWave(runScore)) {
                    spawnFlyingEnemyWave(runScore, scrollSpeed);
                } else if (type == 0) {
                    int h = (int) (55 * heightScale);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 90, h));
                } else if (type == 1) {
                    int h = (int) (120 * heightScale);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 55, h));
                } else {
                    int h = (int) (70 * heightScale);
                    float gap = screenWidth * 0.4f + (scrollSpeed * 10);
                    obstacles.add(new Obstacle(screenWidth, groundY - h, 60, h));
                    obstacles.add(new Obstacle(screenWidth + gap, groundY - h, 60, h));
                }
                lastObstacleTime = now;
            }

            Iterator<Obstacle> it = obstacles.iterator();
            while (it.hasNext()) {
                Obstacle obs = it.next();
                float obstacleSpeed = obs.isFlyingEnemy() ? scrollSpeed * 1.18f : scrollSpeed;
                obs.x -= obstacleSpeed;
                if (obs.isFlyingEnemy()) {
                    obs.y = obs.baseY + (float) Math.sin(now * 0.008f + obs.phaseSeed) * 11f;
                }
                if (obs.x + obs.width < 0) {
                    it.remove();
                    continue;
                }

                if (!obs.passed && obs.x + obs.width < player.x) {
                    obs.passed = true;
                    playSound(soundWow);
                }

                if (RectF.intersects(player.getBounds(), obs.getBounds())) {
                    gameOver = true;
                    gamePaused = false;
                    int currentScore = score / 10;
                    finalScore = currentScore;
                    failMessage = FAIL_MESSAGES[random.nextInt(FAIL_MESSAGES.length)];

                    android.content.SharedPreferences prefs = getContext()
                            .getSharedPreferences("VocexRunPrefs", android.content.Context.MODE_PRIVATE);
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("lastScore", finalScore);

                    if (currentScore > highScore) {
                        highScore = currentScore;
                        editor.putInt("highScore", highScore);
                    }
                    if (dailyChallenge && currentScore > dailyBestScore) {
                        dailyBestScore = currentScore;
                        editor.putInt(getDailyBestKey(), dailyBestScore);
                    }
                    editor.apply();

                    vibrate(70);
                    playSound(soundCatLaugh);
                    return;
                }
            }
        }
    }

    private boolean shouldSpawnFlyingEnemyWave(int runScore) {
        int chance = Math.min(56, 30 + Math.max(0, runScore - 300) / 14);
        return obstacleRandom.nextInt(100) < chance;
    }

    private void spawnFlyingEnemyWave(int runScore, float scrollSpeed) {
        int w = premiumRendering ? 112 : 96;
        int h = premiumRendering ? 72 : 62;
        int count = 1;
        if (runScore >= 380 && obstacleRandom.nextInt(100) < 42) {
            count++;
        }
        if (runScore >= 680 && obstacleRandom.nextInt(100) < 28) {
            count++;
        }

        float gap = screenWidth * 0.26f + scrollSpeed * 11f + obstacleRandom.nextInt(70);
        for (int i = 0; i < count; i++) {
            float lane = groundY - 210f - obstacleRandom.nextInt(126);
            lane = Math.max(64f, Math.min(groundY - h - 54f, lane));
            float x = screenWidth + 20f + i * gap;
            obstacles.add(new Obstacle(x, lane, w, h,
                    Obstacle.TYPE_FLYING_ENEMY, obstacleRandom.nextFloat() * TWO_PI));
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null || player == null) {
            return;
        }
        syncSettingsManagerFromLocal();

        AudioEngine.VoiceLevel level = audioEngine.getVoiceLevel();
        long now2 = System.currentTimeMillis();
        float daylight = getDaylightAmount();

        drawScoreDrivenSky(canvas, daylight);


        canvas.save();
        canvas.translate(shakeX, shakeY);

        // FIX: Sky elements always draw — RAGE mode dims them, not hides them
        // Previously `if (level != RAGE)` caused stars/moon/clouds to vanish mid-jump
        {
            float rageDim = 1f;

            // ── STARS ─────────────────────────────────────────────────────
            float nightAlpha = (1f - daylight) * rageDim;
            float backPressDim = 1f;
            if (backPressedOnce) {
                long elapsed = now2 - backPressTime;
                if (elapsed < BACK_PRESS_INTERVAL) {
                    // Ease-in-out dim: quickly fade to 0.25, then slowly recover
                    float t = elapsed / (float) BACK_PRESS_INTERVAL;
                    backPressDim = t < 0.15f
                            ? 1f - (t / 0.15f) * 0.75f   // fade down
                            : 0.25f + (t - 0.15f) / 0.85f * 0.75f; // fade back up
                } else {
                    backPressedOnce = false; // expired
                }
            }

            // Layer 1: tiny distant stars, very slow parallax
            int farStarCount = premiumRendering ? 55 : 30;
            for (int i = 0; i < farStarCount; i++) {
                float sx = ((i * 239 + (int) (score * 0.15f)) % (screenWidth + 20)) - 10;
                float sy = (i * 97 + i * 31) % (int) (groundY * 0.65f);
                float twinkle = 0.45f + 0.55f * (float) (Math.sin(now2 * 0.003 + i * 1.7));
                twinkle *= backPressDim * nightAlpha;
                int alpha = (int) (twinkle * (i % 3 == 0 ? 200 : 130));
                if (alpha <= 2) continue;
                int r = i % 7 == 0 ? 240 : i % 5 == 0 ? 210 : 255;
                int g = i % 7 == 0 ? 240 : i % 5 == 0 ? 230 : 255;
                int b = i % 7 == 0 ? 180 : i % 5 == 0 ? 215 : 255;
                paint.setColor(Color.argb(alpha, r, g, b));
                float sz = i % 9 == 0 ? 2.8f : 1.4f;
                canvas.drawCircle(sx, sy, sz, paint);
                // Cross sparkle on bright stars
                if (i % 9 == 0 && twinkle > 0.8f) {
                    paint.setColor(Color.argb((int) (twinkle * 90), r, g, b));
                    paint.setStrokeWidth(1f);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(sx - 5, sy, sx + 5, sy, paint);
                    canvas.drawLine(sx, sy - 5, sx, sy + 5, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }

            // Layer 2: mid stars, medium parallax
            int midStarCount = premiumRendering ? 28 : 14;
            for (int i = 0; i < midStarCount; i++) {
                float sx = ((i * 311 + (int) (score * 0.28f) + 60) % (screenWidth + 20)) - 10;
                float sy = (i * 157 + 30) % (int) (groundY * 0.78f);
                float twinkle = 0.5f + 0.5f * (float) (Math.sin(now2 * 0.0045 + i * 2.3));
                twinkle *= backPressDim * nightAlpha;
                int alpha = (int) (twinkle * 170);
                if (alpha <= 2) continue;
                paint.setColor(Color.argb(alpha, 210, 225, 255));
                canvas.drawCircle(sx, sy, 2.2f, paint);
            }

            // ── MOON ──────────────────────────────────────────────────────
            drawCelestialCycle(canvas, daylight, now2); // FIX: sun/moon never dimmed by rage

            // ── CLOUDS ────────────────────────────────────────────────────
            int[][] cloudDefs = {
                    {(int) (score * 0.6f), 55},
                    {(int) (score * 0.6f) + 560, 115},
                    {(int) (score * 0.6f) + 1080, 80}
            };
            int cloudLimit = premiumRendering ? cloudDefs.length : 1;
            for (int ci = 0; ci < cloudLimit; ci++) {
                int[] cd = cloudDefs[ci];
                float cx2 = (cd[0] % (screenWidth + 380)) - 190;
                float cy2 = cd[1];
                drawRealisticCloud(canvas, cx2, cy2, rageDim);
            }

        } // end sky block (always draws, RAGE just dims)

        // ── MOUNTAINS ────────────────────────────────────────────────────
        drawUltraRealisticMountains(canvas);

        drawBackgroundDunes(canvas, level);

        // Desert sand base
        if (renderCacheReady) {
            canvas.drawRect(0, groundY, screenWidth, groundY + 14, groundBandPaint);
        } else {
            paint.setColor(Color.rgb(195, 168, 100));
            canvas.drawRect(0, groundY, screenWidth, groundY + 14, paint);
        }

        // Sub-ground fill
        paint.setColor(Color.rgb(110, 85, 45));
        canvas.drawRect(0, groundY + 14, screenWidth, screenHeight, paint);

        // Ground surface details: cracks
        paint.setColor(Color.argb(60, 80, 55, 20));
        paint.setStrokeWidth(1.5f);
        paint.setStyle(Paint.Style.STROKE);
        for (int c = 0; c < 8; c++) {
            float cx3 = ((c * 193 + score * 2) % screenWidth);
            canvas.drawLine(cx3, groundY + 3, cx3 + 18, groundY + 8, paint);
            canvas.drawLine(cx3 + 18, groundY + 8, cx3 + 28, groundY + 5, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        drawPremiumGround(canvas, level);

        // ── OBSTACLES (CACTI) ─────────────────────────────────────────────
        synchronized (obstacles) {
            for (Obstacle obs : obstacles) {
                float cx = obs.x, cy = obs.y, cw = obs.width, ch = obs.height;
                if (obs.isFlyingEnemy()) {
                    obstacleRenderer.drawFlyingEnemy(canvas, paint, auxPaint, obs, now2, premiumRendering);
                    continue;
                }

                // Ground shadow
                if (shouldDrawSceneShadows()) {
                    float blur = getSoftShadowExtra();
                    paint.setColor(Color.argb(shadowAlpha(55), 0, 0, 0));
                    canvas.drawOval(new RectF(cx - cw * 0.3f - blur, groundY - 6 - blur * 0.18f,
                            cx + cw * 1.3f + blur, groundY + 6 + blur * 0.18f), paint);
                }

                // Trunk base (rounded bottom)
                float tL = cx + cw * 0.28f, tR = cx + cw * 0.72f;

                // Shadow side (right)
                paint.setColor(Color.rgb(22, 90, 22));
                canvas.drawRoundRect(new RectF(tL, cy, tR, cy + ch), 10, 10, paint);

                // Lit side (left)
                paint.setColor(Color.rgb(42, 148, 42));
                canvas.drawRoundRect(new RectF(tL, cy, tL + (tR - tL) * 0.58f, cy + ch), 10, 10, paint);

                // Highlight strip
                paint.setColor(Color.argb(70, 160, 255, 120));
                canvas.drawRoundRect(new RectF(tL + 4, cy + ch * 0.05f,
                        tL + 9, cy + ch * 0.88f), 4, 4, paint);

                // Ribs (horizontal texture bands)
                paint.setColor(Color.argb(45, 0, 60, 0));
                int ribCount = Math.max(3, (int) (ch / 22));
                for (int r = 1; r < ribCount; r++) {
                    float ry = cy + ch * r / (float) ribCount;
                    canvas.drawLine(tL + 2, ry, tR - 2, ry, paint);
                }

                // Left arm
                if (ch > 60) {
                    float aY = cy + ch * 0.35f;
                    float aW = cw * 0.45f;
                    float aH = cw * 0.30f;
                    // Arm shadow side
                    paint.setColor(Color.rgb(22, 90, 22));
                    canvas.drawRoundRect(new RectF(cx, aY - ch * 0.18f, tL + 4, aY + aH), 7, 7, paint);
                    // Arm lit side
                    paint.setColor(Color.rgb(42, 148, 42));
                    canvas.drawRoundRect(new RectF(cx, aY - ch * 0.18f, tL + 4, aY + aH * 0.55f), 7, 7, paint);
                    // Arm vertical upward bit
                    paint.setColor(Color.rgb(38, 138, 38));
                    canvas.drawRoundRect(new RectF(cx + 2, aY - ch * 0.38f, cx + aW * 0.6f, aY + aH * 0.1f), 7, 7, paint);
                    // Arm highlight
                    paint.setColor(Color.argb(60, 160, 255, 120));
                    canvas.drawLine(cx + 4, aY - ch * 0.35f, cx + 4, aY + aH * 0.08f, paint);
                }

                // Right arm
                if (ch > 80) {
                    float aY = cy + ch * 0.50f;
                    float aH = cw * 0.30f;
                    paint.setColor(Color.rgb(22, 90, 22));
                    canvas.drawRoundRect(new RectF(tR - 4, aY - ch * 0.14f, cx + cw, aY + aH), 7, 7, paint);
                    paint.setColor(Color.rgb(42, 148, 42));
                    canvas.drawRoundRect(new RectF(tR - 4, aY - ch * 0.14f, cx + cw, aY + aH * 0.55f), 7, 7, paint);
                    paint.setColor(Color.rgb(38, 138, 38));
                    canvas.drawRoundRect(new RectF(tR - cw * 0.55f, aY - ch * 0.30f, cx + cw - 2, aY + aH * 0.1f), 7, 7, paint);
                }

                // Spines (small white lines radiating from edges)
                paint.setColor(Color.argb(180, 230, 240, 210));
                paint.setStrokeWidth(1.8f);
                paint.setStyle(Paint.Style.STROKE);
                int spineCount = Math.max(4, (int) (ch / 18));
                for (int s = 0; s < spineCount; s++) {
                    float sy2 = cy + ch * (s + 0.5f) / spineCount;
                    // Left spines
                    canvas.drawLine(tL + 2, sy2, tL - 9, sy2 - 5, paint);
                    canvas.drawLine(tL + 2, sy2, tL - 9, sy2 + 5, paint);
                    // Right spines
                    canvas.drawLine(tR - 2, sy2, tR + 9, sy2 - 5, paint);
                    canvas.drawLine(tR - 2, sy2, tR + 9, sy2 + 5, paint);
                }
                // Spines on arms
                if (ch > 60) {
                    float aY = cy + ch * 0.35f - ch * 0.07f;
                    canvas.drawLine(cx + cw * 0.15f, aY - 3, cx + cw * 0.04f, aY - 9, paint);
                    canvas.drawLine(cx + cw * 0.15f, aY + 3, cx + cw * 0.04f, aY + 9, paint);
                }
                paint.setStyle(Paint.Style.FILL);
                drawCactus3D(canvas, obs);
            }
        }

        // ── PLAYER ────────────────────────────────────────────────────────
        ps.draw(canvas, paint);
        drawMotionBlurTrail(canvas, level, now2);
        drawRageBloom(canvas, level, now2);

        float px = player.x, py = player.y, pw = player.width, ph = player.height;
        boolean onGround = player.isOnGround;
        long animT = System.currentTimeMillis();

        int overallsColor = Color.rgb(28, 72, 195);
        int overallsDark = Color.rgb(18, 50, 145);
        int skinColor = Color.rgb(255, 200, 140);
        int skinShadow = Color.rgb(220, 160, 100);
        int shirtColor = player.isRaging ? Color.rgb(255, 40, 40)
                : (level == AudioEngine.VoiceLevel.SHOUT ? Color.rgb(255, 100, 0)
                : Color.rgb(210, 35, 35));
        int shirtDark = player.isRaging ? Color.rgb(200, 20, 20)
                : (level == AudioEngine.VoiceLevel.SHOUT ? Color.rgb(200, 70, 0)
                : Color.rgb(160, 20, 20));
        int brownDark = Color.rgb(65, 30, 8);
        int brownLight = Color.rgb(100, 55, 15);

        // Drop shadow on ground
        if (shouldDrawSceneShadows()) {
            paint.setColor(Color.argb(shadowAlpha(50), 0, 0, 0));
            float blur = getSoftShadowExtra();
            canvas.drawOval(new RectF(px + pw * 0.05f - blur, groundY - 7 - blur * 0.18f,
                    px + pw * 0.95f + blur, groundY + 7 + blur * 0.18f), paint);
        }

        // ── Legs ────────────────────────────────────────────────────────
        // FIX: slower, more human gait — was 0.0105f which was too rapid
        float runRate = gameStarted ? 0.0062f + Math.min(player.speed, 18f) * 0.00045f : 0.0045f;
        float gaitPhase = (animT * runRate) % TWO_PI;
        float bodyBob = onGround
                ? Math.abs((float) Math.sin(gaitPhase)) * 3.6f
                : Math.max(-4f, Math.min(5f, player.velY * 0.10f));
        py += bodyBob;
        drawRunnerLeg(canvas, px, py, pw, ph, gaitPhase + (float) Math.PI, false, onGround,
                overallsDark, overallsColor, brownDark, brownLight);
        drawRunnerLeg(canvas, px, py, pw, ph, gaitPhase, true, onGround,
                overallsColor, overallsDark, brownDark, brownLight);

        // ── Torso ───────────────────────────────────────────────────────
        // Overalls bib
        paint.setColor(overallsColor);
        canvas.drawRoundRect(new RectF(px + pw * 0.16f, py + ph * 0.40f, px + pw * 0.84f, py + ph * 0.68f), 10, 10, paint);
        paint.setColor(overallsDark);
        canvas.drawRoundRect(new RectF(px + pw * 0.56f, py + ph * 0.40f, px + pw * 0.84f, py + ph * 0.68f), 10, 10, paint);

        // Shirt body
        paint.setColor(shirtColor);
        canvas.drawRoundRect(new RectF(px + pw * 0.08f, py + ph * 0.36f, px + pw * 0.92f, py + ph * 0.66f), 12, 12, paint);
        paint.setColor(shirtDark);
        canvas.drawRoundRect(new RectF(px + pw * 0.58f, py + ph * 0.36f, px + pw * 0.92f, py + ph * 0.66f), 12, 12, paint);
        // Shirt highlight
        paint.setColor(Color.argb(55, 255, 255, 255));
        canvas.drawRoundRect(new RectF(px + pw * 0.12f, py + ph * 0.38f, px + pw * 0.30f, py + ph * 0.55f), 6, 6, paint);

        // Shoulder straps
        paint.setColor(overallsColor);
        canvas.drawRect(px + pw * 0.23f, py + ph * 0.30f, px + pw * 0.38f, py + ph * 0.50f, paint);
        canvas.drawRect(px + pw * 0.60f, py + ph * 0.30f, px + pw * 0.75f, py + ph * 0.50f, paint);
        // Buckles (gold)
        paint.setColor(Color.rgb(255, 210, 40));
        canvas.drawRoundRect(new RectF(px + pw * 0.25f, py + ph * 0.31f, px + pw * 0.36f, py + ph * 0.40f), 3, 3, paint);
        canvas.drawRoundRect(new RectF(px + pw * 0.62f, py + ph * 0.31f, px + pw * 0.73f, py + ph * 0.40f), 3, 3, paint);

        // ── Arms ────────────────────────────────────────────────────────
        drawRunnerArm(canvas, px, py, pw, ph, gaitPhase, false, onGround,
                shirtDark, skinShadow, skinColor);
        drawRunnerArm(canvas, px, py, pw, ph, gaitPhase + (float) Math.PI, true, onGround,
                shirtColor, skinColor, skinShadow);
        // Head shadow        // Head shadow
        paint.setColor(skinShadow);
        canvas.drawRoundRect(new RectF(px + pw * 0.18f, py + ph * 0.10f, px + pw * 0.88f, py + ph * 0.40f), 18, 18, paint);
        // Head main
        paint.setColor(skinColor);
        canvas.drawRoundRect(new RectF(px + pw * 0.15f, py + ph * 0.07f, px + pw * 0.85f, py + ph * 0.38f), 18, 18, paint);
        // Head highlight
        paint.setColor(Color.argb(55, 255, 220, 180));
        canvas.drawRoundRect(new RectF(px + pw * 0.18f, py + ph * 0.09f, px + pw * 0.50f, py + ph * 0.22f), 14, 14, paint);

        // Hat (red cap)
        paint.setColor(shirtColor);
        canvas.drawRoundRect(new RectF(px + pw * 0.16f, py - ph * 0.05f, px + pw * 0.84f, py + ph * 0.15f), 10, 10, paint);
        // Hat brim
        canvas.drawRoundRect(new RectF(px + pw * 0.05f, py + ph * 0.10f, px + pw * 0.95f, py + ph * 0.20f), 5, 5, paint);
        // Hat shadow/dark top
        paint.setColor(shirtDark);
        canvas.drawRoundRect(new RectF(px + pw * 0.52f, py - ph * 0.05f, px + pw * 0.84f, py + ph * 0.15f), 10, 10, paint);
        // Hat badge
        paint.setColor(Color.WHITE);
        canvas.drawCircle(px + pw * 0.50f, py + ph * 0.05f, 12, paint);
        paint.setColor(shirtColor);
        paint.setTextSize(16);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("V", px + pw * 0.50f, py + ph * 0.10f, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // ── Face ────────────────────────────────────────────────────────
        // Eyebrows
        paint.setColor(Color.rgb(80, 45, 10));
        paint.setStrokeWidth(3.5f);
        paint.setStyle(Paint.Style.STROKE);
        float browY = py + ph * 0.19f + (player.isRaging ? -3 : 0);
        canvas.drawLine(px + pw * 0.22f, browY, px + pw * 0.42f, browY - (player.isRaging ? 5 : 1), paint);
        canvas.drawLine(px + pw * 0.58f, browY - (player.isRaging ? 5 : 1), px + pw * 0.78f, browY, paint);
        paint.setStyle(Paint.Style.FILL);

        // Eye whites
        paint.setColor(Color.WHITE);
        canvas.drawCircle(px + pw * 0.33f, py + ph * 0.23f, 8.5f, paint);
        canvas.drawCircle(px + pw * 0.67f, py + ph * 0.23f, 8.5f, paint);
        // Iris
        paint.setColor(Color.rgb(55, 35, 12));
        float ed = onGround ? 2.2f : -2.2f;
        canvas.drawCircle(px + pw * 0.35f, py + ph * 0.23f + ed, 5f, paint);
        canvas.drawCircle(px + pw * 0.69f, py + ph * 0.23f + ed, 5f, paint);
        // Eye shine
        paint.setColor(Color.argb(200, 255, 255, 255));
        canvas.drawCircle(px + pw * 0.36f, py + ph * 0.21f + ed, 2f, paint);
        canvas.drawCircle(px + pw * 0.70f, py + ph * 0.21f + ed, 2f, paint);

        // Mouth / expression
        paint.setStrokeWidth(2.8f);
        paint.setStyle(Paint.Style.STROKE);
        if (player.isRaging) {
            paint.setColor(Color.rgb(200, 40, 40));
            canvas.drawLine(px + pw * 0.34f, py + ph * 0.32f, px + pw * 0.66f, py + ph * 0.32f, paint);
        } else if (level == AudioEngine.VoiceLevel.SHOUT) {
            paint.setColor(Color.rgb(180, 80, 80));
            canvas.drawOval(new RectF(px + pw * 0.37f, py + ph * 0.27f,
                    px + pw * 0.63f, py + ph * 0.35f), paint);
        } else {
            paint.setColor(Color.rgb(180, 80, 80));
            canvas.drawArc(new RectF(px + pw * 0.36f, py + ph * 0.25f,
                    px + pw * 0.64f, py + ph * 0.36f), 10, 160, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // Cheeks
        paint.setColor(Color.argb(60, 255, 120, 100));
        canvas.drawCircle(px + pw * 0.24f, py + ph * 0.29f, 7f, paint);
        canvas.drawCircle(px + pw * 0.76f, py + ph * 0.29f, 7f, paint);

        // Rage flame particles
        if (player.isRaging) {
            for (int fi = 0; fi < 5; fi++) {
                float fProgress = ((now2 % 400) / 400f + fi * 0.2f) % 1.0f;
                float fX = px + pw * (0.2f + fi * 0.15f);
                float fY = py - fProgress * 35;
                int fAlpha = (int) ((1f - fProgress) * 200);
                paint.setColor(fi % 2 == 0
                        ? Color.argb(fAlpha, 255, 120, 0)
                        : Color.argb(fAlpha, 255, 60, 0));
                canvas.drawCircle(fX, fY, (1f - fProgress) * 10, paint);
            }
        }
        drawPlayerPremiumHighlights(canvas, level, now2);
        drawPostProcessingOverlay(canvas, level);

        float difficultyRight = gameStarted && !gameOver
                ? getPauseButtonRect().left - 18f
                : screenWidth - 40f;
        gameUI.drawHud(canvas, paint, score, highScore, dailyBestScore,
                dailyChallenge, screenWidth, difficultyRight);
        gameUI.drawVoiceMeter(canvas, paint, audioEngine.getSmoothedAmplitude(),
                level, screenWidth, screenHeight);

        if (!inSettings && gameStarted && !gameOver && !calibrating) {
            drawRunEventBanner(canvas, now2);
            if (gamePaused) {
                drawPauseMenu(canvas, now2);
            } else {
                drawPauseButton(canvas, now2);
            }
        }

        // ══════════════ SETTINGS MENU ══════════════
        drawRoofContactFlash(canvas, now2);

        if (inSettings && activeSettingsTab >= 0) {
            drawSettingsMenu(canvas);
            canvas.restore();
            return;
        }

        if (inSettings) {
            paint.setColor(Color.argb(248, 6, 6, 20));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

            float cx = screenWidth / 2f;

            // Back button
            paint.setColor(Color.rgb(255, 68, 0));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(20, 18, 102, 100), 18, 18, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(52);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("←", 61, 76, paint);

            // Title
            paint.setColor(Color.WHITE);
            paint.setTextSize(58);
            canvas.drawText("⚙  SETTINGS", cx, 82, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            canvas.drawRoundRect(new RectF(cx - 290, 96, cx + 290, 100), 2, 2, paint);

            // ── SECTION CARD HELPER (inline) ──────────────────────────────
            // We draw 4 group cards then their contents.

            // ── GROUP 1: GAME MODE ────────────────────────────────────────
            paint.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawRoundRect(new RectF(22, 112, screenWidth - 22, 365), 18, 18, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("  GAME MODE", 42, 140, paint);

            // 3 horizontal mode cards
            float mT = 150, mB = 358;
            float[] mL = {cx - 450, cx - 145, cx + 165};
            float[] mR = {cx - 165, cx + 145, cx + 450};
            String[] mEmoji = {"🤫", "🗣️", "💀"};
            String[] mName = {"SILENT", "NORMAL", "RAGE"};
            String[] mHint = {"Whisper", "Speak", "SCREAM"};
            int[] mClr = {0xFF64C8FF, 0xFF64FF78, 0xFFFF5050};

            for (int i = 0; i < 3; i++) {
                boolean sel = (selectedMode == i);
                int c = mClr[i];
                paint.setColor(sel ? c : Color.argb(38,
                        (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF));
                canvas.drawRoundRect(new RectF(mL[i], mT, mR[i], mB), 16, 16, paint);
                if (sel) {
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3.5f);
                    canvas.drawRoundRect(new RectF(mL[i], mT, mR[i], mB), 16, 16, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                float cardCx = (mL[i] + mR[i]) / 2f;
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(sel ? Color.BLACK : Color.WHITE);
                paint.setTextSize(56);
                canvas.drawText(mEmoji[i], cardCx, mT + 76, paint);
                paint.setTextSize(28);
                canvas.drawText(mName[i], cardCx, mT + 120, paint);
                paint.setColor(sel ? Color.argb(160, 0, 0, 0) : Color.argb(120, 255, 255, 255));
                paint.setTextSize(22);
                canvas.drawText(mHint[i], cardCx, mT + 152, paint);
            }

            // ── GROUP 2: SENSITIVITY ──────────────────────────────────────
            paint.setColor(Color.argb(55, 255, 255, 255));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(22, 375, screenWidth - 22, 510), 18, 18, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("  SENSITIVITY", 42, 402, paint);

            // DEFAULT button (top-right of card)
            boolean isDefault = Math.abs(sensitivityPct - 50f) < 0.5f;
            paint.setColor(isDefault
                    ? Color.argb(60, 255, 255, 255)
                    : Color.rgb(255, 68, 0));
            canvas.drawRoundRect(new RectF(screenWidth - 174, 382, screenWidth - 32, 414), 10, 10, paint);
            paint.setColor(isDefault ? Color.argb(120, 255, 255, 255) : Color.WHITE);
            paint.setTextSize(20);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("↺  DEFAULT", screenWidth - 103, 403, paint);

            float sL = cx - 300, sR = cx + 300, sY = 452;
            float kX = sL + (sensitivityPct / 100f) * (sR - sL);
            paint.setColor(Color.argb(80, 255, 255, 255));
            canvas.drawRoundRect(new RectF(sL, sY - 7, sR, sY + 7), 7, 7, paint);
            paint.setColor(Color.rgb(255, 100, 0));
            canvas.drawRoundRect(new RectF(sL, sY - 7, kX, sY + 7), 7, 7, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(kX, sY, 22, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            canvas.drawCircle(kX, sY, 14, paint);
            paint.setTextSize(22);
            paint.setColor(Color.argb(150, 255, 255, 255));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Hard", sL, sY + 44, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Easy", sR, sY + 44, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(26);
            canvas.drawText((int) sensitivityPct + "%", cx, sY + 44, paint);

            // ── GROUP 3: GAMEPLAY ─────────────────────────────────────────
            paint.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawRoundRect(new RectF(22, 520, screenWidth - 22, 630), 18, 18, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("  GAMEPLAY", 42, 548, paint);

            drawSettingsToggle(canvas, "DAILY CHALLENGE", dailyChallenge,
                    cx - 460, 556, cx - 16, 622);
            drawSettingsToggle(canvas, "HAPTICS", hapticsEnabled,
                    cx + 16, 556, cx + 460, 622);

            // ── GROUP 4: STATS ────────────────────────────────────────────
            paint.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawRoundRect(new RectF(22, 640, screenWidth - 22, 730), 18, 18, paint);
            paint.setColor(Color.rgb(255, 68, 0));
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("  STATS", 42, 668, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.rgb(255, 215, 0));
            paint.setTextSize(30);
            canvas.drawText("All-time Best:  " + highScore + "m", cx - 220, 713, paint);
            paint.setColor(Color.rgb(100, 220, 255));
            if (dailyChallenge) {
                canvas.drawText("Daily Best:  " + dailyBestScore + "m", cx + 220, 713, paint);
            } else {
                paint.setColor(Color.argb(100, 255, 255, 255));
                canvas.drawText("Enable Daily to track daily best", cx + 110, 713, paint);
            }

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
            return;
        }

        // ══════════════ FIRST TIME PROMPT ══════════════
        if (showingFirstTimePrompt) {
            drawFirstTimePrompt(canvas);
            canvas.restore();
            return;
        }

        // ══════════════ TUTORIAL ══════════════
        if (showingTutorial) {
            drawTutorialOverlay(canvas);
            canvas.restore();
            return;
        }

        if (calibrating) {
            drawCalibrationOverlay(canvas);
        } else if (!gameStarted) {
            drawHomeScreen(canvas, now2);
        }

        canvas.restore();

        // Game over
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
            if (dailyChallenge) {
                paint.setColor(Color.rgb(100, 220, 255));
                paint.setTextSize(34);
                canvas.drawText("Daily Best: " + dailyBestScore, screenWidth / 2f, screenHeight / 2f + 120, paint);
            }
            float rankY = screenHeight / 2f + (dailyChallenge ? 158 : 120);
            paint.setColor(Color.rgb(255, 176, 58));
            paint.setTextSize(36);
            canvas.drawText(getScoreRankLabel(score / 10), screenWidth / 2f, rankY, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(34);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(failMessage, screenWidth / 2f, screenHeight / 2f + (dailyChallenge ? 200 : 160), paint);

            // Share icon — top-right corner — hidden in shared screenshot
            if (!isCapturingShare) {
                float shareBtnLeft = screenWidth - SHARE_BTN_MARGIN - SHARE_BTN_SIZE;
                float shareBtnTop = SHARE_BTN_MARGIN;
                drawPremiumButtonSurface(canvas, new RectF(shareBtnLeft, shareBtnTop,
                                shareBtnLeft + SHARE_BTN_SIZE, shareBtnTop + SHARE_BTN_SIZE),
                        Color.rgb(255, 108, 34), Color.rgb(185, 42, 0), false, true, 0.72f);
                float cx2 = shareBtnLeft + SHARE_BTN_SIZE / 2f;
                float cy2 = shareBtnTop + SHARE_BTN_SIZE / 2f;
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(cx2, cy2 + 18, cx2, cy2 - 12, paint);
                canvas.drawLine(cx2 - 12, cy2 - 2, cx2, cy2 - 16, paint);
                canvas.drawLine(cx2 + 12, cy2 - 2, cx2, cy2 - 16, paint);
                canvas.drawLine(cx2 - 22, cy2 + 2, cx2 - 22, cy2 + 22, paint);
                canvas.drawLine(cx2 + 22, cy2 + 2, cx2 + 22, cy2 + 22, paint);
                canvas.drawLine(cx2 - 22, cy2 + 22, cx2 + 22, cy2 + 22, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(0f);
            }

            paint.setTextSize(32);
            paint.setColor(Color.rgb(180, 255, 180));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Tap anywhere to try again", screenWidth / 2f,
                    screenHeight / 2f + (dailyChallenge ? 242 : 204), paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        // ── BACK-PRESS TOAST OVERLAY ──────────────────────────────────────
        if (backPressedOnce) {
            long elapsed = System.currentTimeMillis() - backPressTime;
            if (elapsed < BACK_PRESS_INTERVAL) {
                // Animate: fade in quickly, hold, fade out
                float t = elapsed / (float) BACK_PRESS_INTERVAL;
                float toastAlpha;
                if (t < 0.1f) {
                    toastAlpha = t / 0.1f; // fade in
                } else if (t > 0.8f) {
                    toastAlpha = (1f - t) / 0.2f; // fade out
                } else {
                    toastAlpha = 1f; // hold
                }
                // Slide-up entrance
                float slideOffset = t < 0.12f ? (1f - t / 0.12f) * 40f : 0f;

                float toastCx = screenWidth / 2f;
                float toastCy = screenHeight - 120f + slideOffset;
                String toastText = "Press the back button again";
                paint.setTextSize(30);
                float textW = paint.measureText(toastText);
                float padH = 40f, padV = 22f;

                // Pill background with glassmorphism
                int bgAlpha = (int) (toastAlpha * 180);
                paint.setColor(Color.argb(bgAlpha, 15, 12, 35));
                RectF pill = new RectF(toastCx - textW / 2f - padH,
                        toastCy - padV, toastCx + textW / 2f + padH,
                        toastCy + padV + 6f);
                canvas.drawRoundRect(pill, 32, 32, paint);

                // Border glow
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(Color.argb((int) (toastAlpha * 120), 255, 100, 40));
                canvas.drawRoundRect(pill, 32, 32, paint);
                paint.setStyle(Paint.Style.FILL);

                // Text
                paint.setColor(Color.argb((int) (toastAlpha * 255), 255, 220, 180));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(toastText, toastCx, toastCy + 10f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            } else {
                backPressedOnce = false;
            }
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX(), ty = event.getY();
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            performClick();
        }

        // ── First-time prompt ─────────────────────────────────────────────
        if (showingFirstTimePrompt && action == MotionEvent.ACTION_DOWN) {
            float cx = screenWidth / 2f, cy = screenHeight / 2f;
            // Recompute button rects the same way drawFirstTimePrompt does
            float iconTop = cy - 330f;
            float titleY = iconTop + 100 + 20 + 58;
            float subtitleY = titleY + 50;
            float divY = subtitleY + 26;
            float btnTop = divY + 18;
            float btnBottom = btnTop + 112;
            if (tx > cx - 334 && tx < cx - 16 && ty > btnTop && ty < btnBottom) {
                // New player → show tutorial
                showingFirstTimePrompt = false;
                showingTutorial = true;
                tutorialStep = 0;
                markLaunched();
                vibrate(20);
            } else if (tx > cx + 16 && tx < cx + 334 && ty > btnTop && ty < btnBottom) {
                // Returning player → skip straight to game
                showingFirstTimePrompt = false;
                markLaunched();
                vibrate(20);
            }
            return true;
        }

        // ── Tutorial ──────────────────────────────────────────────────────
        if (showingTutorial && action == MotionEvent.ACTION_DOWN) {
            float cx = screenWidth / 2f;
            boolean skipHit = tx > screenWidth - 185 && tx < screenWidth - 15
                    && ty > 12 && ty < 68;
            boolean nextHit = tx > cx - 210 && tx < cx + 210
                    && ty > screenHeight - 108 && ty < screenHeight - 22;
            if (skipHit || (nextHit && tutorialStep == TUTORIAL_STEPS - 1)) {
                showingTutorial = false;
                vibrate(25);
            } else if (nextHit) {
                tutorialStep++;
                vibrate(15);
            }
            return true;
        }

        // ── Game over ─────────────────────────────────────────────────────
        if (action == MotionEvent.ACTION_DOWN && gameOver) {
            if (isShareButtonHit(tx, ty)) {
                shareScore();
                return true;
            }
            restart(); // always go back to home menu on game over tap
            return true;
        }

        if (calibrating) return true;

        // ── Settings gear open ────────────────────────────────────────────
        if (action == MotionEvent.ACTION_DOWN
                && !gameStarted && !inSettings
                && tx > screenWidth - 110 && ty < 110) {
            inSettings = true;
            activeSettingsTab = selectedMode < 0 ? SETTINGS_TAB_MODES : activeSettingsTab;
            return true;
        }

        // ── Settings interactions ─────────────────────────────────────────
        if (inSettings && activeSettingsTab >= 0) {
            return handleTabbedSettingsTouch(tx, ty, action);
        }

        if (inSettings) {
            float cx = screenWidth / 2f;

            if (action == MotionEvent.ACTION_DOWN) {
                // Back button
                if (tx > 20 && tx < 102 && ty > 18 && ty < 100) {
                    inSettings = false;
                    saveSettings();
                    return true;
                }

                // Mode cards (horizontal layout, y=150–358)
                if (ty > 150 && ty < 358) {
                    if (tx > cx - 450 && tx < cx - 165) {
                        selectedMode = 0;
                        applySelectedModeBaseThresholds();
                        applySlider();
                        saveSettings();
                        vibrate(15);
                    } else if (tx > cx - 145 && tx < cx + 145) {
                        selectedMode = 1;
                        applySelectedModeBaseThresholds();
                        applySlider();
                        saveSettings();
                        vibrate(15);
                    } else if (tx > cx + 165 && tx < cx + 450) {
                        selectedMode = 2;
                        applySelectedModeBaseThresholds();
                        applySlider();
                        saveSettings();
                        vibrate(15);
                    }
                }

                // Sensitivity slider touch start (y=412–492)
                if (ty > 412 && ty < 492 && tx > cx - 330 && tx < cx + 330) {
                    draggingSlider = true;
                    sensitivityPct = Math.max(0, Math.min(100,
                            (tx - (cx - 300)) / 600f * 100));
                    applySlider();
                }

                // DEFAULT button (top-right of sensitivity card, y=382–414)
                if (tx > screenWidth - 174 && tx < screenWidth - 32
                        && ty > 382 && ty < 414) {
                    sensitivityPct = 50f;
                    applySlider();
                    saveSettings();
                    vibrate(20);
                }

                // Gameplay toggles (y=556–622)
                if (ty > 556 && ty < 622) {
                    if (tx > cx - 460 && tx < cx - 16) {
                        dailyChallenge = !dailyChallenge;
                        if (dailyChallenge) {
                            dailyBestScore = getContext()
                                    .getSharedPreferences("VocexRunPrefs", Context.MODE_PRIVATE)
                                    .getInt(getDailyBestKey(), 0);
                        }
                        saveSettings();
                        vibrate(20);
                    } else if (tx > cx + 16 && tx < cx + 460) {
                        hapticsEnabled = !hapticsEnabled;
                        saveSettings();
                        vibrate(20);
                    }
                }
            }

            if (action == MotionEvent.ACTION_MOVE && draggingSlider) {
                float cx2 = screenWidth / 2f;
                sensitivityPct = Math.max(0, Math.min(100,
                        (tx - (cx2 - 300)) / 600f * 100));
                applySlider();
            }

            if (action == MotionEvent.ACTION_UP) {
                draggingSlider = false;
                saveSettings();
            }

            return true;
        }

        if (gameStarted && !gameOver && action == MotionEvent.ACTION_DOWN) {
            if (gamePaused) {
                if (getPauseHomeButtonRect().contains(tx, ty)) {
                    restart();
                    vibrate(22);
                    return true;
                }
                if (getPauseSettingsButtonRect().contains(tx, ty)) {
                    inSettings = true;
                    activeSettingsTab = selectedMode < 0 ? SETTINGS_TAB_MODES : activeSettingsTab;
                    vibrate(18);
                    return true;
                }
                if (getPauseResumeRect().contains(tx, ty)) {
                    gamePaused = false;
                    vibrate(22);
                    return true;
                }
                if (getPauseRestartRect().contains(tx, ty)) {
                    gamePaused = false;
                    if (selectedMode >= 0) {
                        beginCalibration();
                    } else {
                        restart();
                    }
                    vibrate(24);
                    return true;
                }
                return true;
            }

            if (getPauseButtonRect().contains(tx, ty)) {
                gamePaused = true;
                vibrate(18);
                return true;
            }
        }

        // ── Mode select (start screen) ────────────────────────────────────
        if (action == MotionEvent.ACTION_DOWN && !gameStarted) {
            int hitMode = getHomeModeHit(tx, ty);
            if (hitMode >= 0) {
                selectedMode = hitMode;
                applySelectedModeBaseThresholds();
                applySlider();
                saveSettings();
                vibrate(18);
                return true;
            }

            if (isHomeStartButtonHit(tx, ty)) {
                if (selectedMode >= 0) {
                    applySelectedModeBaseThresholds();
                    applySlider();
                    saveSettings();
                    beginCalibration();
                } else {
                    vibrate(12);
                }
                return true;
            }
        }

        return true;
    }

    private void drawHomeScreen(Canvas canvas, long now) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(232, 4, 5, 17));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        drawHomeSettingsButton(canvas);

        float cx = screenWidth / 2f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(255, 246, 228));
        paint.setTextSize(Math.min(68f, screenHeight * 0.085f));
        canvas.drawText("VOCEX RUN", cx, Math.max(92f, screenHeight * 0.14f), paint);

        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.argb(190, 220, 228, 242));
        paint.setTextSize(Math.min(25f, screenHeight * 0.035f));
        canvas.drawText(dailyChallenge ? "DAILY CHALLENGE ACTIVE" : "CHOOSE YOUR VOICE PROFILE",
                cx, Math.max(128f, screenHeight * 0.19f), paint);

        for (int i = 0; i < 3; i++) {
            drawHomeModeCard(canvas, i, now);
        }
        drawHomeStartButton(canvas, now);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHomeSettingsButton(Canvas canvas) {
        RectF gear = new RectF(screenWidth - 105, 15, screenWidth - 15, 105);
        drawPremiumButtonSurface(canvas, gear, Color.rgb(42, 48, 72),
                Color.rgb(10, 13, 26), false, true, 0.55f);
        float cx = gear.centerX();
        float cy = gear.centerY();
        paint.setStrokeWidth(4f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(255, 246, 228));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            float x1 = cx + (float) Math.cos(a) * 19f;
            float y1 = cy + (float) Math.sin(a) * 19f;
            float x2 = cx + (float) Math.cos(a) * 27f;
            float y2 = cy + (float) Math.sin(a) * 27f;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, 16f, paint);
        paint.setColor(Color.argb(150, 255, 180, 86));
        paint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, 25f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawHomeModeCard(Canvas canvas, int mode, long now) {
        RectF rect = getHomeModeRect(mode);
        boolean selected = selectedMode == mode;
        int color = getHomeModeColor(mode);
        int dark = darken(color, 90);
        float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.006 + mode);

        drawPremiumButtonSurface(canvas, rect,
                selected ? brighten(color, 18) : Color.rgb(28, 34, 54),
                selected ? dark : Color.rgb(8, 11, 24),
                selected, true, selected ? 0.8f + pulse * 0.25f : 0.22f);

        float railW = 10f;
        paint.setColor(selected ? Color.WHITE : color);
        canvas.drawRoundRect(new RectF(rect.left + 5f, rect.top + 9f,
                rect.left + railW + 7f, rect.bottom - 9f), 8f, 8f, paint);

        String[] names = {"SILENT", "NORMAL", "RAGE"};
        String[] hints = {"Whisper tuned", "Balanced voice", "High-threshold scream"};
        String[] badges = {"LOW", "MID", "MAX"};
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.min(38f, rect.height() * 0.37f));
        paint.setColor(selected ? Color.rgb(12, 14, 22) : Color.WHITE);
        canvas.drawText(names[mode], rect.left + 42f, rect.top + rect.height() * 0.45f, paint);

        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(Math.min(22f, rect.height() * 0.23f));
        paint.setColor(selected ? Color.argb(190, 20, 22, 28) : Color.argb(185, 225, 232, 245));
        canvas.drawText(hints[mode], rect.left + 42f, rect.top + rect.height() * 0.74f, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        float badgeW = 92f;
        RectF badge = new RectF(rect.right - badgeW - 28f, rect.centerY() - 24f,
                rect.right - 28f, rect.centerY() + 24f);
        paint.setColor(selected ? Color.argb(190, 255, 255, 255) : Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(badge, 8f, 8f, paint);
        paint.setColor(selected ? dark : color);
        paint.setTextSize(22f);
        canvas.drawText(badges[mode], badge.centerX(), badge.centerY() + 8f, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    private void drawHomeStartButton(Canvas canvas, long now) {
        RectF rect = getHomeStartButtonRect();
        boolean ready = selectedMode >= 0;
        float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.008);
        int startColor = ready ? Color.rgb(255, 176, 64) : Color.rgb(70, 76, 92);
        int endColor = ready ? Color.rgb(255, 76, 28) : Color.rgb(31, 35, 48);

        drawPremiumButtonSurface(canvas, rect, startColor, endColor,
                ready, ready, ready ? 0.85f + pulse * 0.4f : 0.08f);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.min(34f, rect.height() * 0.46f));
        paint.setColor(ready ? Color.WHITE : Color.argb(160, 225, 230, 240));
        canvas.drawText(ready ? "START" : "SELECT A MODE", rect.centerX(), rect.centerY() + 12f, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
    }

    private int getHomeModeHit(float x, float y) {
        for (int i = 0; i < 3; i++) {
            RectF rect = getHomeModeRect(i);
            if (rect.contains(x, y)) return i;
        }
        return -1;
    }

    private boolean isHomeStartButtonHit(float x, float y) {
        return getHomeStartButtonRect().contains(x, y);
    }

    private RectF getHomeModeRect(int mode) {
        float width = Math.min(720f, screenWidth * 0.66f);
        float height = getHomeModeHeight();
        float gap = getHomeModeGap();
        float left = screenWidth / 2f - width / 2f;
        float top = getHomeModesTop() + mode * (height + gap);
        return new RectF(left, top, left + width, top + height);
    }

    private RectF getHomeStartButtonRect() {
        RectF lastMode = getHomeModeRect(2);
        float width = Math.min(390f, screenWidth * 0.36f);
        float height = Math.max(62f, Math.min(78f, screenHeight * 0.105f));
        float top = Math.min(screenHeight - height - 26f, lastMode.bottom + Math.max(18f, screenHeight * 0.028f));
        float left = screenWidth / 2f - width / 2f;
        return new RectF(left, top, left + width, top + height);
    }

    private float getHomeModesTop() {
        float height = getHomeModeHeight();
        float gap = getHomeModeGap();
        float total = height * 3f + gap * 2f;
        float desired = screenHeight * 0.45f;
        float maxTop = screenHeight - total - Math.max(104f, screenHeight * 0.145f);
        return Math.max(screenHeight * 0.34f, Math.min(desired, maxTop));
    }

    private float getHomeModeHeight() {
        return Math.max(82f, Math.min(104f, screenHeight * 0.128f));
    }

    private float getHomeModeGap() {
        return Math.max(12f, Math.min(18f, screenHeight * 0.022f));
    }

    private int getHomeModeColor(int mode) {
        switch (mode) {
            case 0:
                return Color.rgb(86, 197, 255);
            case 1:
                return Color.rgb(96, 235, 126);
            case 2:
                return Color.rgb(255, 72, 70);
            default:
                return Color.WHITE;
        }
    }

    public int darken(int color, int amount) {
        return Color.rgb(Math.max(0, Color.red(color) - amount),
                Math.max(0, Color.green(color) - amount),
                Math.max(0, Color.blue(color) - amount));
    }

    private int brighten(int color, int amount) {
        return Color.rgb(Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount),
                Math.min(255, Color.blue(color) + amount));
    }

    private float getDaylightAmount() {
        int runScore = Math.max(0, score / 10);
        int cycleScore = CELESTIAL_CYCLE_SCORE * 2;
        int phaseScore = runScore % cycleScore;
        float raw = phaseScore <= CELESTIAL_CYCLE_SCORE
                ? phaseScore / (float) CELESTIAL_CYCLE_SCORE
                : 1f - ((phaseScore - CELESTIAL_CYCLE_SCORE)
                / (float) CELESTIAL_CYCLE_SCORE);
        return MathUtils.smoothStep(raw);
    }

    private void drawScoreDrivenSky(Canvas canvas, float daylight) {
        worldRenderer.drawScoreDrivenSky(canvas, paint, auxPaint,
                screenWidth, groundY, daylight, highSunIntensity);
    }

    private void drawCelestialCycle(Canvas canvas, float daylight, long now) {
        worldRenderer.drawCelestialCycle(canvas, paint, auxPaint, screenWidth,
                groundY, score, now, premiumRendering, highSunIntensity, CELESTIAL_CYCLE_SCORE);
    }

    public void drawPremiumButtonSurface(Canvas canvas, RectF rect, int startColor, int endColor,
                                         boolean selected, boolean enabled, float glow) {
        gameUI.drawPremiumButtonSurface(canvas, rect, startColor, endColor,
                selected, enabled, glow);
    }

    private void showRunEventBanner(String title, String subtitle, long durationMs) {
        runEventBannerTitle = title;
        runEventBannerSubtitle = subtitle;
        runEventBannerUntil = System.currentTimeMillis() + durationMs;
    }

    private void drawRunEventBanner(Canvas canvas, long now) {
        if (now > runEventBannerUntil || runEventBannerTitle.length() == 0) {
            return;
        }

        float remaining = (runEventBannerUntil - now) / 2600f;
        int alpha = Math.max(0, Math.min(220, (int) (220f * Math.min(1f, remaining * 3.5f))));
        float width = Math.min(screenWidth - 72f, 650f);
        float height = 76f;
        float left = screenWidth / 2f - width / 2f;
        float top = Math.max(118f, screenHeight * 0.17f);
        RectF rect = new RectF(left, top, left + width, top + height);

        auxPaint.setShader(new android.graphics.LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                Color.argb(alpha, 255, 118, 28),
                Color.argb(alpha, 48, 122, 255),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 22f, 22f, auxPaint);
        auxPaint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        paint.setColor(Color.argb(Math.min(255, alpha + 30), 255, 236, 180));
        canvas.drawRoundRect(rect, 22f, 22f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        paint.setColor(Color.argb(Math.min(255, alpha + 35), 255, 255, 255));
        paint.setTextSize(28f);
        canvas.drawText(runEventBannerTitle, rect.centerX(), rect.top + 32f, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.argb(Math.min(230, alpha + 10), 236, 244, 255));
        paint.setTextSize(20f);
        canvas.drawText(runEventBannerSubtitle, rect.centerX(), rect.top + 58f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private RectF getPauseButtonRect() {
        float size = Math.max(76f, Math.min(90f, screenWidth * 0.075f));
        float margin = 18f;
        return new RectF(screenWidth - margin - size, margin,
                screenWidth - margin, margin + size);
    }

    private RectF getPauseHomeButtonRect() {
        float size = Math.max(76f, Math.min(92f, screenWidth * 0.078f));
        return new RectF(24f, 24f, 24f + size, 24f + size);
    }

    private RectF getPauseSettingsButtonRect() {
        float size = Math.max(76f, Math.min(92f, screenWidth * 0.078f));
        return new RectF(screenWidth - 24f - size, 24f, screenWidth - 24f, 24f + size);
    }

    private RectF getPauseResumeRect() {
        float width = Math.min(460f, screenWidth * 0.54f);
        float height = Math.max(74f, Math.min(88f, screenHeight * 0.11f));
        float top = screenHeight / 2f - height - 14f;
        float left = screenWidth / 2f - width / 2f;
        return new RectF(left, top, left + width, top + height);
    }

    private RectF getPauseRestartRect() {
        RectF resume = getPauseResumeRect();
        float gap = Math.max(18f, screenHeight * 0.025f);
        return new RectF(resume.left, resume.bottom + gap,
                resume.right, resume.bottom + gap + resume.height());
    }

    private void drawPauseButton(Canvas canvas, long now) {
        RectF rect = getPauseButtonRect();
        float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.006);
        drawPremiumButtonSurface(canvas, rect, Color.rgb(255, 168, 58),
                Color.rgb(218, 62, 24), false, true, 0.5f + pulse * 0.35f);

        float barW = rect.width() * 0.15f;
        float barH = rect.height() * 0.44f;
        float gap = rect.width() * 0.14f;
        float cx = rect.centerX();
        float cy = rect.centerY();
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(cx - gap - barW, cy - barH / 2f,
                cx - gap, cy + barH / 2f), 5f, 5f, paint);
        canvas.drawRoundRect(new RectF(cx + gap, cy - barH / 2f,
                cx + gap + barW, cy + barH / 2f), 5f, 5f, paint);
    }

    private void drawPauseMenu(Canvas canvas, long now) {
        paint.setColor(Color.argb(196, 2, 4, 14));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        float panelW = Math.min(screenWidth - 72f, 620f);
        float panelH = Math.min(screenHeight - 152f, 430f);
        RectF panel = new RectF(cx - panelW / 2f, cy - panelH / 2f,
                cx + panelW / 2f, cy + panelH / 2f);

        auxPaint.setShader(new android.graphics.RadialGradient(
                cx, cy, panelW * 0.72f,
                new int[]{Color.argb(125, 255, 124, 38),
                        Color.argb(72, 52, 126, 255), Color.TRANSPARENT},
                new float[]{0f, 0.48f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, panelW * 0.72f, auxPaint);
        auxPaint.setShader(null);

        paint.setColor(Color.argb(130, 0, 0, 0));
        canvas.drawRoundRect(new RectF(panel.left + 8f, panel.top + 14f,
                panel.right + 8f, panel.bottom + 18f), 24f, 24f, paint);
        auxPaint.setShader(new android.graphics.LinearGradient(
                panel.left, panel.top, panel.right, panel.bottom,
                new int[]{Color.argb(238, 24, 29, 48),
                        Color.argb(238, 7, 10, 22),
                        Color.argb(238, 40, 20, 16)},
                new float[]{0f, 0.54f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(panel, 24f, 24f, auxPaint);
        auxPaint.setShader(null);

        paint.setColor(Color.argb(86, 255, 255, 255));
        canvas.drawRoundRect(new RectF(panel.left + 8f, panel.top + 8f,
                panel.right - 8f, panel.top + panel.height() * 0.34f), 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.8f);
        paint.setColor(Color.argb(170, 255, 174, 86));
        canvas.drawRoundRect(panel, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);

        RectF home = getPauseHomeButtonRect();
        RectF settings = getPauseSettingsButtonRect();
        drawPremiumButtonSurface(canvas, home, Color.rgb(54, 68, 96),
                Color.rgb(12, 16, 30), false, true, 0.45f);
        drawHomeIcon(canvas, home);
        drawPremiumButtonSurface(canvas, settings, Color.rgb(54, 68, 96),
                Color.rgb(12, 16, 30), false, true, 0.45f);
        drawGearGlyph(canvas, settings);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        paint.setColor(Color.rgb(255, 246, 228));
        paint.setTextSize(Math.min(58f, panel.height() * 0.15f));
        canvas.drawText("PAUSED", cx, panel.top + 86f, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.argb(190, 218, 228, 246));
        paint.setTextSize(26f);
        canvas.drawText("Score " + (score / 10) + "  |  Best " + highScore,
                cx, panel.top + 126f, paint);

        RectF resume = getPauseResumeRect();
        RectF restart = getPauseRestartRect();
        float pulse = 0.5f + 0.5f * (float) Math.sin(now * 0.007);
        drawPremiumButtonSurface(canvas, resume, Color.rgb(255, 184, 72),
                Color.rgb(230, 72, 24), true, true, 0.85f + pulse * 0.35f);
        drawPremiumButtonSurface(canvas, restart, Color.rgb(86, 197, 255),
                Color.rgb(22, 80, 166), false, true, 0.55f);

        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(34f, resume.height() * 0.43f));
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        canvas.drawText("RESUME", resume.centerX(), resume.centerY() + 12f, paint);
        canvas.drawText("RESTART", restart.centerX(), restart.centerY() + 12f, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHomeIcon(Canvas canvas, RectF rect) {
        float cx = rect.centerX();
        float cy = rect.centerY();
        float w = rect.width() * 0.42f;
        float h = rect.height() * 0.34f;
        paint.setColor(Color.rgb(255, 246, 228));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        scratchPath.reset();
        scratchPath.moveTo(cx - w * 0.62f, cy - h * 0.08f);
        scratchPath.lineTo(cx, cy - h * 0.68f);
        scratchPath.lineTo(cx + w * 0.62f, cy - h * 0.08f);
        canvas.drawPath(scratchPath, paint);
        canvas.drawLine(cx - w * 0.46f, cy - h * 0.06f, cx - w * 0.46f, cy + h * 0.62f, paint);
        canvas.drawLine(cx + w * 0.46f, cy - h * 0.06f, cx + w * 0.46f, cy + h * 0.62f, paint);
        canvas.drawLine(cx - w * 0.46f, cy + h * 0.62f, cx + w * 0.46f, cy + h * 0.62f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawGearGlyph(Canvas canvas, RectF rect) {
        float cx = rect.centerX();
        float cy = rect.centerY();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.rgb(255, 246, 228));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            canvas.drawLine(cx + (float) Math.cos(a) * rect.width() * 0.20f,
                    cy + (float) Math.sin(a) * rect.height() * 0.20f,
                    cx + (float) Math.cos(a) * rect.width() * 0.30f,
                    cy + (float) Math.sin(a) * rect.height() * 0.30f, paint);
        }
        paint.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, rect.width() * 0.17f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private boolean handleTabbedSettingsTouch(float tx, float ty, int action) {
        float cx = screenWidth / 2f;
        float tabsLeft = getSettingsTabsLeft();
        float tabsTop = getSettingsTabsTop();
        float tabsWidth = getSettingsTabsWidth();
        float tabWidth = getSettingsTabWidth();

        if (action == MotionEvent.ACTION_DOWN) {
            if (tx > 20 && tx < 102 && ty > 18 && ty < 100) {
                inSettings = false;
                draggingSlider = false;
                activeSlider = SLIDER_NONE;
                activeDropdown = DROPDOWN_NONE;
                saveSettings();
                return true;
            }

            if (ty >= tabsTop && ty <= tabsTop + 58
                    && tx >= tabsLeft && tx <= tabsLeft + tabsWidth) {
                draggingSettingsTabs = true;
                settingsTabsDownX = tx;
                settingsTabsLastX = tx;
                settingsTabsMoved = false;
                draggingSlider = false;
                activeSlider = SLIDER_NONE;
                activeDropdown = DROPDOWN_NONE;
                return true;
            }

            if (handleGraphicsDropdownOptionTouch(tx, ty)) {
                return true;
            }
            activeDropdown = DROPDOWN_NONE;

            // FIX: Per-tab reset button — works in every settings tab
            {
                float contentTop = getSettingsContentTop();
                if (getTabResetRect(contentTop).contains(tx, ty)) {
                    resetCurrentTab();
                    saveSettings();
                    vibrate(28);
                    return true;
                }
            }

            if (activeSettingsTab == SETTINGS_TAB_MODES) {
                float contentTop = getSettingsContentTop();
                if (getRestoreDefaultsRect(contentTop).contains(tx, ty)) {
                    restoreEverythingToDefault();
                    saveSettings();
                    vibrate(28);
                    return true;
                }
                float margin = Math.max(56f, screenWidth * 0.07f);
                float gap = 24f;
                float cardTop = contentTop + 82f;
                float cardBottom = Math.min(screenHeight - 92f, cardTop + 250f);
                float cardWidth = (screenWidth - margin * 2f - gap * 2f) / 3f;
                for (int i = 0; i < 3; i++) {
                    float left = margin + i * (cardWidth + gap);
                    float right = left + cardWidth;
                    if (tx >= left && tx <= right && ty >= cardTop && ty <= cardBottom) {
                        selectedMode = i;
                        applySelectedModeBaseThresholds();
                        applySlider();
                        saveSettings();
                        vibrate(18);
                        return true;
                    }
                }
            } else if (activeSettingsTab == SETTINGS_TAB_UI) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(68f, screenWidth * 0.08f);
                float right = screenWidth - left;
                float rowTop = contentTop + 96f;
                float rowHeight = Math.min(94f, Math.max(72f, screenHeight * 0.12f));
                if (beginSliderIfHit(SLIDER_HUD_OPACITY, tx, ty, left, rowTop, right, rowTop + rowHeight)) {
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_VOICE) {
                float contentTop = getSettingsContentTop();
                float sliderLeft = cx - Math.min(380f, screenWidth * 0.32f);
                float sliderRight = cx + Math.min(380f, screenWidth * 0.32f);
                float sliderY = contentTop + 232f;
                if (tx > screenWidth - 236f && tx < screenWidth - 64f
                        && ty > contentTop + 42f && ty < contentTop + 96f) {
                    sensitivityPct = 50f;
                    applySlider();
                    saveSettings();
                    vibrate(20);
                    return true;
                }
                if (ty > sliderY - 48f && ty < sliderY + 56f
                        && tx > sliderLeft - 24f && tx < sliderRight + 24f) {
                    beginSliderWithTrack(SLIDER_SENSITIVITY, tx, sliderLeft, sliderRight);
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_GRAPHICS) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(58f, screenWidth * 0.07f);
                float right = screenWidth - left;
                float rowTop = getGraphicsSettingsRowTop(contentTop);
                float gap = getGraphicsSettingsGap();
                float rowHeight = getGraphicsSettingsRowHeight(rowTop, gap);
                int row = getSettingsRowHit(tx, ty, left, rowTop, right, rowHeight, gap,
                        getGraphicsSettingsRowCount());
                if (row == 0) {
                    toggleDropdown(DROPDOWN_GRAPHICS_QUALITY);
                    return true;
                } else if (row == 1) {
                    shadowsEnabled = !shadowsEnabled;
                    saveSettings();
                    vibrate(18);
                    return true;
                } else if (row == 2) {
                    toggleDropdown(DROPDOWN_SHADOW_PRESET);
                    return true;
                } else if (row == 3) {
                    toggleDropdown(DROPDOWN_SHADOW_RESOLUTION);
                    return true;
                } else if (row == 4) {
                    toggleDropdown(DROPDOWN_SHADOW_CASCADES);
                    return true;
                } else if (row == 5) {
                    softShadows = !softShadows;
                    shadowPresetIndex = 4;
                    saveSettings();
                    vibrate(18);
                    return true;
                } else if (row == 6) {
                    toggleDropdown(DROPDOWN_MSAA);
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_POST) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(68f, screenWidth * 0.08f);
                float right = screenWidth - left;
                float rowTop = contentTop + 76f;
                float gap = 16f;
                float rowHeight = Math.max(68f, Math.min(86f,
                        (screenHeight - rowTop - 40f - gap * 2f) / 3f));
                if (beginSliderIfHit(SLIDER_SATURATION, tx, ty, left, rowTop, right, rowTop + rowHeight)
                        || beginSliderIfHit(SLIDER_CONTRAST, tx, ty, left, rowTop + rowHeight + gap,
                        right, rowTop + rowHeight * 2f + gap)
                        || beginSliderIfHit(SLIDER_EXPOSURE, tx, ty, left,
                        rowTop + rowHeight * 2f + gap * 2f, right,
                        rowTop + rowHeight * 3f + gap * 2f)) {
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_LIGHTING) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(68f, screenWidth * 0.08f);
                float right = screenWidth - left;
                float rowTop = contentTop + 96f;
                float rowHeight = Math.min(90f, Math.max(72f, screenHeight * 0.12f));
                if (tx >= left && tx <= right && ty >= rowTop && ty <= rowTop + rowHeight) {
                    highSunIntensity = !highSunIntensity;
                    saveSettings();
                    vibrate(18);
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_AUDIO) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(68f, screenWidth * 0.08f);
                float right = screenWidth - left;
                float rowTop = contentTop + 96f;
                float rowHeight = Math.min(94f, Math.max(72f, screenHeight * 0.12f));
                if (beginSliderIfHit(SLIDER_AUDIO_BOOST, tx, ty, left, rowTop, right, rowTop + rowHeight)) {
                    return true;
                }
            } else if (activeSettingsTab == SETTINGS_TAB_GAMEPLAY) {
                float contentTop = getSettingsContentTop();
                float left = Math.max(68f, screenWidth * 0.08f);
                float right = screenWidth - left;
                float rowTop = contentTop + 84f;
                float rowHeight = 84f;
                float gap = 24f;
                if (tx >= left && tx <= right && ty >= rowTop && ty <= rowTop + rowHeight) {
                    dailyChallenge = !dailyChallenge;
                    dailyBestScore = getContext()
                            .getSharedPreferences("VocexRunPrefs", Context.MODE_PRIVATE)
                            .getInt(getDailyBestKey(), 0);
                    saveSettings();
                    vibrate(20);
                    return true;
                }
                float hapticsTop = rowTop + rowHeight + gap;
                if (tx >= left && tx <= right && ty >= hapticsTop && ty <= hapticsTop + rowHeight) {
                    hapticsEnabled = !hapticsEnabled;
                    saveSettings();
                    vibrate(20);
                    return true;
                }
                float tutorialTop = hapticsTop + rowHeight + gap;
                if (tx >= left && tx <= right && ty >= tutorialTop && ty <= tutorialTop + rowHeight) {
                    inSettings = false;
                    showingTutorial = true;
                    tutorialStep = 0;
                    markLaunched();
                    saveSettings();
                    vibrate(24);
                    return true;
                }
            }
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (draggingSettingsTabs) {
                float dx = tx - settingsTabsLastX;
                if (Math.abs(tx - settingsTabsDownX) > 8f) {
                    settingsTabsMoved = true;
                }
                settingsTabsScrollX -= dx;
                settingsTabsLastX = tx;
                constrainSettingsTabsScroll();
                return true;
            }
            if (draggingSlider) {
                updateActiveSliderFromTouch(tx);
                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP) {
            if (draggingSettingsTabs) {
                if (!settingsTabsMoved && ty >= tabsTop && ty <= tabsTop + 58
                        && tx >= tabsLeft && tx <= tabsLeft + tabsWidth) {
                    activeSettingsTab = Math.max(0, Math.min(SETTINGS_TAB_COUNT - 1,
                            (int) ((tx - tabsLeft + settingsTabsScrollX) / tabWidth)));
                    activeDropdown = DROPDOWN_NONE;
                    vibrate(12);
                }
                draggingSettingsTabs = false;
                return true;
            }
            if (draggingSlider) {
                draggingSlider = false;
                activeSlider = SLIDER_NONE;
                saveSettings();
                vibrate(12);
                return true;
            }
            return true;
        }

        return true;
    }

    private boolean beginSliderIfHit(int slider, float tx, float ty,
                                     float left, float top, float right, float bottom) {
        if (tx < left || tx > right || ty < top || ty > bottom) {
            return false;
        }
        beginSliderWithTrack(slider, tx, getSettingsSliderTrackLeft(left, right),
                getSettingsSliderTrackRight(left, right));
        return true;
    }

    private void beginSliderWithTrack(int slider, float tx, float sliderLeft, float sliderRight) {
        activeSlider = slider;
        activeSliderLeft = sliderLeft;
        activeSliderRight = Math.max(sliderLeft + 1f, sliderRight);
        draggingSlider = true;
        updateActiveSliderFromTouch(tx);
    }

    private void updateActiveSliderFromTouch(float tx) {
        float pct = Math.max(0f, Math.min(100f,
                (tx - activeSliderLeft) / (activeSliderRight - activeSliderLeft) * 100f));
        switch (activeSlider) {
            case SLIDER_SENSITIVITY:
                sensitivityPct = pct;
                applySlider();
                break;
            case SLIDER_HUD_OPACITY:
                hudOpacityPct = Math.max(15f, pct);
                break;
            case SLIDER_SATURATION:
                saturationPct = pct;
                break;
            case SLIDER_CONTRAST:
                contrastPct = pct;
                break;
            case SLIDER_EXPOSURE:
                exposurePct = pct;
                break;
            case SLIDER_AUDIO_BOOST:
                masterAudioBoostPct = pct;
                break;
            default:
                break;
        }
    }

    private float getSettingsSliderTrackLeft(float left, float right) {
        return left + Math.min(280f, (right - left) * 0.38f);
    }

    private float getSettingsSliderTrackRight(float left, float right) {
        return right - 150f;
    }

    private int getSettingsRowHit(float tx, float ty, float left, float rowTop,
                                  float right, float rowHeight, float gap, int rowCount) {
        if (tx < left || tx > right) {
            return -1;
        }
        for (int i = 0; i < rowCount; i++) {
            float top = rowTop + i * (rowHeight + gap);
            if (ty >= top && ty <= top + rowHeight) {
                return i;
            }
        }
        return -1;
    }

    private void toggleDropdown(int dropdown) {
        activeDropdown = activeDropdown == dropdown ? DROPDOWN_NONE : dropdown;
        vibrate(12);
    }

    private boolean handleGraphicsDropdownOptionTouch(float tx, float ty) {
        if (activeDropdown == DROPDOWN_NONE || activeSettingsTab != SETTINGS_TAB_GRAPHICS) {
            return false;
        }
        float contentTop = getSettingsContentTop();
        float left = Math.max(58f, screenWidth * 0.07f);
        float right = screenWidth - left;
        float rowTop = getGraphicsSettingsRowTop(contentTop);
        float gap = getGraphicsSettingsGap();
        float rowHeight = getGraphicsSettingsRowHeight(rowTop, gap);
        String[] options;
        int rowIndex;
        switch (activeDropdown) {
            case DROPDOWN_GRAPHICS_QUALITY:
                options = GRAPHICS_QUALITY_LABELS;
                rowIndex = 0;
                break;
            case DROPDOWN_SHADOW_PRESET:
                options = SHADOW_PRESET_LABELS;
                rowIndex = 2;
                break;
            case DROPDOWN_SHADOW_RESOLUTION:
                options = SHADOW_RESOLUTION_LABELS;
                rowIndex = 3;
                break;
            case DROPDOWN_SHADOW_CASCADES:
                options = SHADOW_CASCADE_LABELS;
                rowIndex = 4;
                break;
            case DROPDOWN_MSAA:
                options = MSAA_LABELS;
                rowIndex = 6;
                break;
            default:
                return false;
        }
        float optionHeight = Math.max(42f, Math.min(52f, rowHeight * 0.86f));
        float top = rowTop + rowIndex * (rowHeight + gap) + rowHeight + 4f;
        float maxBottom = screenHeight - 24f;
        if (top + optionHeight * options.length > maxBottom) {
            top = rowTop + rowIndex * (rowHeight + gap) - optionHeight * options.length - 4f;
        }
        float dropdownLeft = right - 320f;
        float dropdownRight = right - 28f;
        if (tx < dropdownLeft || tx > dropdownRight || ty < top || ty > top + optionHeight * options.length) {
            return false;
        }
        int option = Math.max(0, Math.min(options.length - 1, (int) ((ty - top) / optionHeight)));
        applyDropdownSelection(activeDropdown, option);
        activeDropdown = DROPDOWN_NONE;
        saveSettings();
        vibrate(18);
        return true;
    }

    private void applyDropdownSelection(int dropdown, int option) {
        switch (dropdown) {
            case DROPDOWN_GRAPHICS_QUALITY:
                graphicsQualityIndex = option;
                applyGraphicsQualityDefaults(option);
                applyRenderingSettings();
                break;
            case DROPDOWN_SHADOW_PRESET:
                shadowPresetIndex = option;
                applyShadowPresetDefaults(option);
                applyRenderingSettings();
                break;
            case DROPDOWN_SHADOW_RESOLUTION:
                shadowResolutionIndex = option;
                shadowPresetIndex = 4;
                applyRenderingSettings();
                break;
            case DROPDOWN_SHADOW_CASCADES:
                shadowCascadesIndex = option;
                shadowPresetIndex = 4;
                applyRenderingSettings();
                break;
            case DROPDOWN_MSAA:
                msaaIndex = option;
                applyRenderingSettings();
                break;
            default:
                break;
        }
    }

    private void applyGraphicsQualityDefaults(int quality) {
        switch (quality) {
            case 0:
                shadowsEnabled = false;
                shadowPresetIndex = 0;
                shadowResolutionIndex = 0;
                shadowCascadesIndex = 0;
                softShadows = false;
                msaaIndex = 0;
                highSunIntensity = false;
                break;
            case 2:
                shadowsEnabled = true;
                shadowPresetIndex = 2;
                applyShadowPresetDefaults(2);
                msaaIndex = 1;
                highSunIntensity = false;
                break;
            case 3:
                shadowsEnabled = true;
                shadowPresetIndex = 3;
                applyShadowPresetDefaults(3);
                msaaIndex = 2;
                highSunIntensity = true;
                break;
            case 1:
            default:
                shadowsEnabled = false;
                shadowPresetIndex = 1;
                applyShadowPresetDefaults(1);
                msaaIndex = 1;
                highSunIntensity = false;
                break;
        }
    }

    private void applyShadowPresetDefaults(int preset) {
        switch (preset) {
            case 0:
                shadowResolutionIndex = 0;
                shadowCascadesIndex = 0;
                softShadows = false;
                break;
            case 1:
                shadowResolutionIndex = 2;
                shadowCascadesIndex = 1;
                softShadows = true;
                break;
            case 2:
                shadowResolutionIndex = 3;
                shadowCascadesIndex = 2;
                softShadows = true;
                break;
            case 3:
                shadowResolutionIndex = 4;
                shadowCascadesIndex = 3;
                softShadows = true;
                break;
            default:
                break;
        }
    }

    private void beginCalibration() {
        // Request audio focus → any music app will pause automatically
        if (audioManager != null) {
            int result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && audioFocusRequest != null) {
                result = audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                result = audioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.AUDIOFOCUS_GAIN
                );
            }
            hadAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        synchronized (obstacles) {
            obstacles.clear();
        }
        ps.clear();
        score = 0;
        gameOver = false;
        gamePaused = false;
        gameStarted = false;
        calibrating = true;
        skyRaidAnnounced = false;
        runEventBannerUntil = 0;
        calibrationPeakAmplitude = 0;
        calibrationAmplitudeSum = 0;
        calibrationSampleCount = 0;
        calibrationNoiseFloor = 0;
        calibrationStartTime = System.currentTimeMillis();
        lastObstacleTime = 0;
        lastRageHapticTime = 0;
        audioEngine.resetSmoothing();
        if (player != null) {
            player.reset(groundY);
        }
        vibrate(35);
    }

    private void finishCalibration() {
        applySelectedModeBaseThresholds();
        int baseAmp = Player.jumpAmpThreshold;
        int baseDelta = Player.jumpDeltaThreshold;
        int average = calibrationSampleCount > 0
                ? (int) (calibrationAmplitudeSum / calibrationSampleCount)
                : calibrationPeakAmplitude;
        int baseline = Math.max(calibrationNoiseFloor / 2, average / 4);
        int peak = Math.max(calibrationPeakAmplitude, average + 260);
        if (calibrationSampleCount < 8 || peak < 160) {
            peak = baseAmp;
            baseline = 0;
        }

        float modeScale = selectedMode == 0 ? 0.46f : selectedMode == 1 ? 0.54f : 0.64f;
        int noiseGuard = baseline + (selectedMode == 0 ? 160 : selectedMode == 1 ? 260 : 430);
        int calibratedAmp = clamp(Math.max(noiseGuard, (int) (peak * modeScale)),
                (int) (baseAmp * 0.35f), (int) (baseAmp * 1.45f));
        int voiceRange = Math.max(peak - baseline, calibratedAmp);
        int calibratedDelta = clamp(Math.max((int) (voiceRange * 0.22f), (int) (baseDelta * 0.42f)),
                (int) (baseDelta * 0.35f), (int) (baseDelta * 1.3f));

        Player.jumpAmpThreshold = calibratedAmp;
        Player.jumpDeltaThreshold = calibratedDelta;
        applySlider();
        syncAudioCalibration();
        calibrating = false;
        gamePaused = false;
        gameStarted = true;
        lastObstacleTime = System.currentTimeMillis();
        obstacleRandom.setSeed(dailyChallenge ? getDailySeed() : System.nanoTime());
        vibrate(60);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawSettingsMenu(Canvas canvas) {
        if (renderCacheReady) {
            canvas.drawRect(0, 0, screenWidth, screenHeight, settingsBgPaint);
        } else {
            canvas.drawColor(Color.rgb(5, 7, 22));
        }

        float cx = screenWidth / 2f;

        // FIX: Back button — glass style matching rest of settings
        drawPremiumButtonSurface(canvas, new RectF(20, 18, 102, 100),
                Color.rgb(255, 92, 28), Color.rgb(180, 40, 0), true, true, 0.8f);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(67f, 40f, 48f, 59f, paint);
        canvas.drawLine(48f, 59f, 67f, 78f, paint);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(130, 255, 238, 210));
        canvas.drawCircle(61f, 59f, 29f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setTextAlign(Paint.Align.CENTER);

        paint.setColor(Color.WHITE);
        paint.setTextSize(58);
        canvas.drawText("SETTINGS", cx, 82, paint);
        paint.setColor(Color.rgb(255, 68, 0));
        canvas.drawRoundRect(new RectF(cx - 260, 96, cx + 260, 100), 2, 2, paint);

        drawSettingsTabs(canvas);

        float contentTop = getSettingsContentTop();
        float contentBottom = screenHeight - 28f;
        paint.setColor(Color.argb(68, 255, 255, 255));
        canvas.drawRoundRect(new RectF(34, contentTop, screenWidth - 34, contentBottom), 24, 24, paint);
        paint.setColor(Color.argb(90, 0, 0, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(new RectF(34, contentTop, screenWidth - 34, contentBottom), 24, 24, paint);
        paint.setStyle(Paint.Style.FILL);

        switch (activeSettingsTab) {
            case SETTINGS_TAB_MODES:
                drawSettingsModesTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_UI:
                drawSettingsUiTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_VOICE:
                drawSettingsVoiceTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_GRAPHICS:
                drawSettingsGraphicsTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_POST:
                drawSettingsPostProcessingTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_LIGHTING:
                drawSettingsLightingTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_AUDIO:
                drawSettingsAudioTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_GAMEPLAY:
                drawSettingsGameplayTab(canvas, contentTop);
                break;
            case SETTINGS_TAB_STATS:
                drawSettingsStatsTab(canvas, contentTop);
                break;
            default:
                activeSettingsTab = SETTINGS_TAB_MODES;
                drawSettingsModesTab(canvas, contentTop);
                break;
        }

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsTabs(Canvas canvas) {
        float left = getSettingsTabsLeft();
        float top = getSettingsTabsTop();
        float width = getSettingsTabsWidth();
        float tabWidth = getSettingsTabWidth();
        constrainSettingsTabsScroll();

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(22);
        canvas.save();
        canvas.clipRect(left, top - 12f, left + width, top + 70f);
        for (int i = 0; i < SETTINGS_TAB_COUNT; i++) {
            boolean active = activeSettingsTab == i;
            float l = left - settingsTabsScrollX + i * tabWidth + 5f;
            float r = l + tabWidth - 10f;
            if (r < left - 8f || l > left + width + 8f) {
                continue;
            }
            RectF tabRect = new RectF(l, top, r, top + 58f);
            drawPremiumButtonSurface(canvas, tabRect,
                    active ? Color.rgb(255, 92, 28) : Color.rgb(38, 44, 66),
                    active ? Color.rgb(180, 40, 0) : Color.rgb(14, 16, 30),
                    active, true, active ? 0.75f : 0.18f);
            paint.setShader(null);
            paint.setColor(active ? Color.WHITE : Color.rgb(190, 200, 218));
            canvas.drawText(SETTINGS_TABS[i], (l + r) / 2f, top + 38f, paint);
        }
        canvas.restore();
        if (getSettingsTabsTotalWidth() > width + 1f) {
            paint.setTextSize(18);
            paint.setColor(Color.argb(135, 230, 235, 245));
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("slide", left + width, top - 6f, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsModesTab(Canvas canvas, float contentTop) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        canvas.drawText("Choose Game Mode", screenWidth / 2f, contentTop + 48f, paint);

        float margin = Math.max(56f, screenWidth * 0.07f);
        float gap = 24f;
        float cardTop = contentTop + 82f;
        float cardBottom = Math.min(screenHeight - 92f, cardTop + 250f);
        float cardWidth = (screenWidth - margin * 2f - gap * 2f) / 3f;
        String[] names = {"SILENT", "NORMAL", "RAGE"};
        String[] hints = {"Whisper jumps", "Speak naturally", "Scream high"};
        String[] details = {"Best for quiet rooms", "Balanced for most players", "Hardest voice threshold"};
        int[] colors = {
                Color.rgb(92, 202, 255),
                Color.rgb(96, 244, 130),
                Color.rgb(255, 82, 82)
        };

        for (int i = 0; i < 3; i++) {
            float left = margin + i * (cardWidth + gap);
            float right = left + cardWidth;
            drawModeTabCard(canvas, i, names[i], hints[i], details[i],
                    colors[i], left, cardTop, right, cardBottom);
        }
        RectF restoreRect = getRestoreDefaultsRect(contentTop);
        drawPremiumButtonSurface(canvas, restoreRect,
                Color.rgb(58, 66, 92), Color.rgb(16, 19, 34), false, true, 0.34f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 238, 220));
        paint.setTextSize(Math.min(24f, restoreRect.height() * 0.42f));
        canvas.drawText("RESTORE EVERYTHING TO DEFAULT",
                restoreRect.centerX(), restoreRect.centerY() + 9f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawModeTabCard(Canvas canvas, int mode, String name, String hint, String details,
                                 int color, float left, float top, float right, float bottom) {
        boolean selected = selectedMode == mode;
        int startColor = selected
                ? color
                : Color.argb(80, Color.red(color), Color.green(color), Color.blue(color));
        int endColor = selected
                ? darken(color, 80)
                : Color.argb(48, Math.max(0, Color.red(color) - 60),
                Math.max(0, Color.green(color) - 60),
                Math.max(0, Color.blue(color) - 60));
        // Glass premium surface — no new Paint() allocation per frame
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                startColor, endColor, selected, true, selected ? 0.9f : 0.22f);

        float center = (left + right) / 2f;
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(selected ? Color.rgb(10, 14, 24) : Color.WHITE);
        paint.setTextSize(40);
        canvas.drawText(name, center, top + 82f, paint);
        paint.setTextSize(27);
        paint.setColor(selected ? Color.argb(210, 0, 0, 0) : Color.rgb(220, 230, 245));
        canvas.drawText(hint, center, top + 126f, paint);
        paint.setTextSize(22);
        paint.setColor(selected ? Color.argb(150, 0, 0, 0) : Color.argb(150, 255, 255, 255));
        canvas.drawText(details, center, top + 166f, paint);

        if (selected) {
            paint.setColor(Color.rgb(10, 14, 24));
            paint.setTextSize(22);
            canvas.drawText("SELECTED", center, bottom - 30f, paint);
        }
    }

    private void drawSettingsVoiceTab(Canvas canvas, float contentTop) {
        float cx = screenWidth / 2f;
        // FIX: symmetric margins so content is centred
        float margin = Math.max(80f, screenWidth * 0.09f);
        float sliderLeft = cx - Math.min(320f, screenWidth * 0.28f);
        float sliderRight = cx + Math.min(320f, screenWidth * 0.28f);
        float sliderY = contentTop + 248f;
        float knobX = sliderLeft + (sliderRight - sliderLeft) * (sensitivityPct / 100f);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Sensitivity", cx, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        paint.setShader(null);
        paint.setColor(Color.argb(60, 255, 255, 255));
        canvas.drawRoundRect(new RectF(sliderLeft, sliderY - 13f, sliderRight, sliderY + 13f), 13, 13, paint);
        paint.setColor(Color.rgb(255, 148, 36));
        canvas.drawRoundRect(new RectF(sliderLeft, sliderY - 13f, knobX, sliderY + 13f), 13, 13, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(knobX, sliderY, 30f, paint);
        paint.setColor(Color.rgb(255, 92, 28));
        canvas.drawCircle(knobX, sliderY, 18f, paint);

        paint.setTextSize(26f);
        paint.setColor(Color.rgb(210, 220, 235));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Harder", sliderLeft, sliderY + 60f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Easier", sliderRight, sliderY + 60f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(42f);
        canvas.drawText((int) sensitivityPct + "%", cx, sliderY + 75f, paint);

        paint.setTextSize(27f);
        paint.setColor(Color.argb(180, 220, 230, 245));
        canvas.drawText("Quick voice calibration runs before each game.",
                cx, Math.min(screenHeight - 72f, contentTop + 390f), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsUiTab(Canvas canvas, float contentTop) {
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        float rowTop = contentTop + 110f;
        float rowHeight = Math.min(94f, Math.max(72f, screenHeight * 0.12f));

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("UI", screenWidth / 2f, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsSliderRow(canvas, "HUD Opacity", "Score, best and voice meter",
                hudOpacityPct, (int) hudOpacityPct + "%",
                left, rowTop, right, rowTop + rowHeight);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private float getGraphicsSettingsRowTop(float contentTop) {
        return contentTop + 86f;
    }

    private float getGraphicsSettingsGap() {
        return 7f;
    }

    private int getGraphicsSettingsRowCount() {
        return 7;
    }

    private float getGraphicsSettingsRowHeight(float rowTop, float gap) {
        int rowCount = getGraphicsSettingsRowCount();
        float available = Math.max(320f, screenHeight - rowTop - 32f);
        return Math.max(44f, Math.min(62f, (available - gap * (rowCount - 1)) / rowCount));
    }

    private void drawSettingsGraphicsTab(Canvas canvas, float contentTop) {
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        float rowTop = getGraphicsSettingsRowTop(contentTop);
        float gap = getGraphicsSettingsGap();
        float rowHeight = getGraphicsSettingsRowHeight(rowTop, gap);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Graphics", screenWidth / 2f, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsDropdownRow(canvas, "Graphics Quality", "Overall visuals and performance",
                GRAPHICS_QUALITY_LABELS[graphicsQualityIndex],
                activeDropdown == DROPDOWN_GRAPHICS_QUALITY,
                left, rowTop, right, rowTop + rowHeight);
        float shadowsTop = rowTop + (rowHeight + gap);
        drawSettingsSwitchRow(canvas, "Shadows", "Enable cast and terrain shadows", shadowsEnabled,
                left, shadowsTop, right, shadowsTop + rowHeight);
        float presetTop = shadowsTop + (rowHeight + gap);
        drawSettingsDropdownRow(canvas, "Shadow Preset", "Quality profile",
                SHADOW_PRESET_LABELS[shadowPresetIndex], activeDropdown == DROPDOWN_SHADOW_PRESET,
                left, presetTop, right, presetTop + rowHeight);
        float resolutionTop = presetTop + (rowHeight + gap);
        drawSettingsDropdownRow(canvas, "Shadow Resolution", "Texture budget",
                SHADOW_RESOLUTION_LABELS[shadowResolutionIndex], activeDropdown == DROPDOWN_SHADOW_RESOLUTION,
                left, resolutionTop, right, resolutionTop + rowHeight);
        float cascadesTop = resolutionTop + (rowHeight + gap);
        drawSettingsDropdownRow(canvas, "Shadow Cascades", "Layered depth steps",
                SHADOW_CASCADE_LABELS[shadowCascadesIndex], activeDropdown == DROPDOWN_SHADOW_CASCADES,
                left, cascadesTop, right, cascadesTop + rowHeight);
        float softTop = cascadesTop + (rowHeight + gap);
        drawSettingsSwitchRow(canvas, "Soft Shadows", "Feather shadow edges", softShadows,
                left, softTop, right, softTop + rowHeight);
        float msaaTop = softTop + (rowHeight + gap);
        drawSettingsDropdownRow(canvas, "MSAA", "Edge smoothing",
                MSAA_LABELS[msaaIndex], activeDropdown == DROPDOWN_MSAA,
                left, msaaTop, right, msaaTop + rowHeight);

        drawActiveSettingsDropdown(canvas, left, right, rowTop, rowHeight, gap);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsPostProcessingTab(Canvas canvas, float contentTop) {
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        float rowTop = contentTop + 96f;
        float gap = 16f;
        float rowHeight = Math.max(68f, Math.min(86f,
                (screenHeight - rowTop - 40f - gap * 2f) / 3f));

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Post Processing", screenWidth / 2f, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsSliderRow(canvas, "Saturation", "Color intensity",
                saturationPct, signedSettingLabel(saturationPct),
                left, rowTop, right, rowTop + rowHeight);
        drawSettingsSliderRow(canvas, "Contrast", "Light and dark separation",
                contrastPct, signedSettingLabel(contrastPct),
                left, rowTop + rowHeight + gap, right, rowTop + rowHeight * 2f + gap);
        drawSettingsSliderRow(canvas, "Exposure", "Scene brightness",
                exposurePct, signedSettingLabel(exposurePct),
                left, rowTop + rowHeight * 2f + gap * 2f,
                right, rowTop + rowHeight * 3f + gap * 2f);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsLightingTab(Canvas canvas, float contentTop) {
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        float rowTop = contentTop + 110f;
        float rowHeight = Math.min(90f, Math.max(72f, screenHeight * 0.12f));

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Lighting", screenWidth / 2f, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsSwitchRow(canvas, "High Sun Intensity",
                "Brighter daylight and warmer horizon glow", highSunIntensity,
                left, rowTop, right, rowTop + rowHeight);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsAudioTab(Canvas canvas, float contentTop) {
        float cx = screenWidth / 2f;
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        // FIX: more room between title and slider row to prevent overlap
        float rowTop = contentTop + 110f;
        float rowHeight = Math.min(110f, Math.max(90f, screenHeight * 0.14f));

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Audio", cx, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsSliderRow(canvas, "Master Audio Boost", "Game sound effect volume",
                masterAudioBoostPct, Math.round(getMasterAudioBoost() * 100f) + "%",
                left, rowTop, right, rowTop + rowHeight);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsGameplayTab(Canvas canvas, float contentTop) {
        float margin = Math.max(80f, screenWidth * 0.09f);
        float left = margin;
        float right = screenWidth - margin;
        float rowTop = contentTop + 100f;
        float rowHeight = 84f;
        float gap = 24f;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Gameplay", screenWidth / 2f, contentTop + 52f, paint);

        // FIX: per-tab reset button
        drawTabResetButton(canvas, contentTop);

        drawSettingsSwitchRow(canvas, "Daily Challenge",
                "Same obstacle seed for today's run", dailyChallenge,
                left, rowTop, right, rowTop + rowHeight);
        drawSettingsSwitchRow(canvas, "Haptics",
                "Jump and rage vibration feedback", hapticsEnabled,
                left, rowTop + rowHeight + gap, right, rowTop + rowHeight * 2f + gap);

        float tutorialTop = rowTop + rowHeight * 2f + gap * 2f;
        // Glass row container for How To Play
        drawPremiumButtonSurface(canvas, new RectF(left, tutorialTop, right, tutorialTop + rowHeight),
                Color.rgb(32, 38, 58), Color.rgb(12, 15, 28), false, true, 0.2f);
        // OPEN button inside row — glass orange
        RectF openRect = new RectF(right - 194f, tutorialTop + 16f,
                right - 26f, tutorialTop + rowHeight - 16f);
        drawPremiumButtonSurface(canvas, openRect,
                Color.rgb(255, 92, 28), Color.rgb(180, 40, 0), false, true, 0.65f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        canvas.drawText("How To Play", left + 28f, tutorialTop + 38f, paint);
        paint.setColor(Color.argb(165, 220, 230, 245));
        paint.setTextSize(22);
        canvas.drawText("Replay the in-game tutorial any time", left + 28f, tutorialTop + 66f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(23);
        canvas.drawText("OPEN", openRect.centerX(), openRect.centerY() + 9f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsSwitchRow(Canvas canvas, String title, String subtitle, boolean enabled,
                                       float left, float top, float right, float bottom) {
        // Glass row container
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                Color.rgb(32, 38, 58), Color.rgb(12, 15, 28), false, true, 0.2f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        float height = bottom - top;
        paint.setTextSize(Math.min(30f, height * 0.36f));
        canvas.drawText(title, left + 28f, top + height * 0.43f, paint);
        paint.setColor(Color.argb(165, 220, 230, 245));
        paint.setTextSize(Math.min(22f, height * 0.25f));
        canvas.drawText(subtitle, left + 28f, top + height * 0.74f, paint);

        float switchW = 118f;
        float switchH = Math.min(50f, height - 16f);
        float switchLeft = right - switchW - 28f;
        float switchTop = top + (bottom - top - switchH) / 2f;
        paint.setColor(enabled ? Color.rgb(96, 244, 130) : Color.rgb(60, 64, 82));
        canvas.drawRoundRect(new RectF(switchLeft, switchTop,
                switchLeft + switchW, switchTop + switchH), 25, 25, paint);
        // Switch highlight
        paint.setColor(Color.argb(enabled ? 80 : 40, 255, 255, 255));
        canvas.drawRoundRect(new RectF(switchLeft + 4, switchTop + 4,
                switchLeft + switchW - 4, switchTop + switchH / 2f), 20, 20, paint);
        paint.setColor(Color.WHITE);
        float knobX = enabled ? switchLeft + switchW - 26f : switchLeft + 26f;
        canvas.drawCircle(knobX, switchTop + switchH / 2f, 21f, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(20);
        paint.setColor(enabled ? Color.rgb(8, 16, 12) : Color.argb(180, 255, 255, 255));
        canvas.drawText(enabled ? "ON" : "OFF", switchLeft + switchW / 2f,
                switchTop + switchH / 2f + 7f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsSliderRow(Canvas canvas, String title, String subtitle,
                                       float valuePct, String valueLabel,
                                       float left, float top, float right, float bottom) {
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                Color.rgb(32, 38, 58), Color.rgb(12, 15, 28), false, true, 0.2f);
        float height = bottom - top;
        float sliderLeft = left + Math.min(320f, (right - left) * 0.42f);
        float sliderRight = right - 150f;
        float sliderY = top + height * 0.50f;
        float knobX = sliderLeft + (sliderRight - sliderLeft) * (Math.max(0f, Math.min(100f, valuePct)) / 100f);

        paint.setShader(null);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(29f, height * 0.32f));
        canvas.drawText(title, left + 28f, top + height * 0.38f, paint);
        paint.setColor(Color.argb(165, 220, 230, 245));
        paint.setTextSize(Math.min(21f, height * 0.22f));
        canvas.drawText(subtitle, left + 28f, top + height * 0.72f, paint);

        paint.setColor(Color.argb(70, 255, 255, 255));
        canvas.drawRoundRect(new RectF(sliderLeft, sliderY - 8f, sliderRight, sliderY + 8f), 8f, 8f, paint);
        paint.setColor(Color.rgb(255, 148, 36));
        canvas.drawRoundRect(new RectF(sliderLeft, sliderY - 8f, knobX, sliderY + 8f), 8f, 8f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(knobX, sliderY, Math.min(22f, height * 0.24f), paint);
        paint.setColor(Color.rgb(255, 92, 28));
        canvas.drawCircle(knobX, sliderY, Math.min(13f, height * 0.14f), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(25f, height * 0.28f));
        canvas.drawText(valueLabel, right - 78f, top + height * 0.50f + 9f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsDropdownRow(Canvas canvas, String title, String subtitle, String value,
                                         boolean open, float left, float top, float right, float bottom) {
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                open ? Color.rgb(62, 72, 104) : Color.rgb(32, 38, 58),
                open ? Color.rgb(20, 28, 52) : Color.rgb(12, 15, 28),
                open, true, open ? 0.45f : 0.2f);
        float height = bottom - top;
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(29f, height * 0.34f));
        canvas.drawText(title, left + 28f, top + height * 0.40f, paint);
        paint.setColor(Color.argb(165, 220, 230, 245));
        paint.setTextSize(Math.min(21f, height * 0.23f));
        canvas.drawText(subtitle, left + 28f, top + height * 0.70f, paint);

        RectF valueRect = new RectF(right - 220f, top + height * 0.18f,
                right - 28f, bottom - height * 0.18f);
        drawPremiumButtonSurface(canvas, valueRect,
                Color.rgb(255, 92, 28), Color.rgb(180, 40, 0), false, true, open ? 0.58f : 0.35f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.min(24f, height * 0.30f));
        canvas.drawText(value + (open ? " ^" : " v"), valueRect.centerX(), valueRect.centerY() + 8f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawActiveSettingsDropdown(Canvas canvas, float left, float right,
                                            float rowTop, float rowHeight, float gap) {
        if (activeDropdown == DROPDOWN_NONE || activeSettingsTab != SETTINGS_TAB_GRAPHICS) {
            return;
        }
        String[] options;
        int selected;
        int rowIndex;
        switch (activeDropdown) {
            case DROPDOWN_GRAPHICS_QUALITY:
                options = GRAPHICS_QUALITY_LABELS;
                selected = graphicsQualityIndex;
                rowIndex = 0;
                break;
            case DROPDOWN_SHADOW_PRESET:
                options = SHADOW_PRESET_LABELS;
                selected = shadowPresetIndex;
                rowIndex = 2;
                break;
            case DROPDOWN_SHADOW_RESOLUTION:
                options = SHADOW_RESOLUTION_LABELS;
                selected = shadowResolutionIndex;
                rowIndex = 3;
                break;
            case DROPDOWN_SHADOW_CASCADES:
                options = SHADOW_CASCADE_LABELS;
                selected = shadowCascadesIndex;
                rowIndex = 4;
                break;
            case DROPDOWN_MSAA:
                options = MSAA_LABELS;
                selected = msaaIndex;
                rowIndex = 6;
                break;
            default:
                return;
        }

        float optionHeight = Math.max(42f, Math.min(52f, rowHeight * 0.86f));
        float top = rowTop + rowIndex * (rowHeight + gap) + rowHeight + 4f;
        float dropdownLeft = right - 320f;
        float dropdownRight = right - 28f;
        float maxBottom = screenHeight - 24f;
        if (top + optionHeight * options.length > maxBottom) {
            top = rowTop + rowIndex * (rowHeight + gap) - optionHeight * options.length - 4f;
        }
        // Opaque backdrop panel behind all dropdown options
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(242, 10, 14, 28));
        canvas.drawRoundRect(new RectF(dropdownLeft - 5f, top - 5f,
                dropdownRight + 5f, top + optionHeight * options.length + 5f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(200, 255, 128, 42));
        canvas.drawRoundRect(new RectF(dropdownLeft - 5f, top - 5f,
                dropdownRight + 5f, top + optionHeight * options.length + 5f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < options.length; i++) {
            float optTop = top + i * optionHeight;
            RectF opt = new RectF(dropdownLeft, optTop, dropdownRight, optTop + optionHeight - 4f);
            boolean isSelected = selected == i;
            drawPremiumButtonSurface(canvas, opt,
                    isSelected ? Color.rgb(255, 128, 42) : Color.rgb(52, 62, 92),
                    isSelected ? Color.rgb(188, 48, 0) : Color.rgb(22, 28, 52),
                    isSelected, true, isSelected ? 0.68f : 0.28f);
            paint.setShader(null);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(22f);
            canvas.drawText(options[i], opt.centerX(), opt.centerY() + 8f, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsStatsTab(Canvas canvas, float contentTop) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40f); // FIX: larger
        canvas.drawText("Scores", screenWidth / 2f, contentTop + 52f, paint);

        float margin = Math.max(70f, screenWidth * 0.09f);
        float gap = 28f;
        float top = contentTop + 104f;
        float cardHeight = Math.min(150f, (screenHeight - top - 70f) / 2f);
        float cardWidth = (screenWidth - margin * 2f - gap) / 2f;
        drawStatCard(canvas, "All-Time High Score", highScore + "m",
                Color.rgb(255, 215, 80), margin, top, margin + cardWidth, top + cardHeight);
        drawStatCard(canvas, "Daily High Score", dailyBestScore + "m",
                Color.rgb(100, 220, 255), margin + cardWidth + gap, top,
                margin + cardWidth * 2f + gap, top + cardHeight);
        drawStatCard(canvas, "Last Score", finalScore + "m",
                Color.rgb(255, 132, 88), margin, top + cardHeight + gap,
                margin + cardWidth, top + cardHeight * 2f + gap);
        drawStatCard(canvas, "Daily Mode",
                dailyChallenge ? "ON" : "OFF",
                dailyChallenge ? Color.rgb(96, 244, 130) : Color.rgb(160, 166, 185),
                margin + cardWidth + gap, top + cardHeight + gap,
                margin + cardWidth * 2f + gap, top + cardHeight * 2f + gap);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStatCard(Canvas canvas, String label, String value, int color,
                              float left, float top, float right, float bottom) {
        int dimColor = Color.argb(180,
                Math.max(0, Color.red(color) - 60),
                Math.max(0, Color.green(color) - 60),
                Math.max(0, Color.blue(color) - 60));
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                Color.argb(110, Color.red(color), Color.green(color), Color.blue(color)),
                dimColor, false, true, 0.35f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.argb(220, 20, 20, 20)); // FIX: black instead of white
        paint.setTextSize(24);
        canvas.drawText(label, (left + right) / 2f, top + 44f, paint);
        paint.setColor(color); // coloured value — unchanged
        paint.setTextSize(48);
        canvas.drawText(value, (left + right) / 2f, bottom - 42f, paint);
    }

    private float getSettingsTabsTop() {
        return 114f;
    }

    private float getSettingsTabsLeft() {
        float totalWidth = getSettingsTabsTotalWidth();
        // Center the tabs within the screen width
        return (screenWidth - totalWidth) / 2f;
    }

    private float getSettingsTabsWidth() {
        // available space for the tab container
        return screenWidth - 48f;
    }

    private float getSettingsTabWidth() {
        // size for each tab, slightly wider for centering look
        float available = getSettingsTabsWidth();
        return Math.min(165f, available / SETTINGS_TAB_COUNT);
    }

    private float getSettingsTabsTotalWidth() {
        return getSettingsTabWidth() * SETTINGS_TAB_COUNT;
    }

    private void constrainSettingsTabsScroll() {
        float maxScroll = Math.max(0f, getSettingsTabsTotalWidth() - getSettingsTabsWidth());
        settingsTabsScrollX = Math.max(0f, Math.min(maxScroll, settingsTabsScrollX));
    }

    private float getSettingsContentTop() {
        return getSettingsTabsTop() + 76f;
    }

    private void drawCalibrationOverlay(Canvas canvas) {
        long elapsed = System.currentTimeMillis() - calibrationStartTime;
        float remaining = Math.max(0f, (CALIBRATION_DURATION_MS - elapsed) / 1000f);
        float progress = Math.min(1f, elapsed / (float) CALIBRATION_DURATION_MS);
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;

        paint.setColor(Color.argb(220, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setTextSize(58);
        canvas.drawText("VOICE CALIBRATION", cx, cy - 105, paint);
        paint.setTextSize(34);
        paint.setColor(Color.rgb(220, 240, 255));
        canvas.drawText("Speak once like you will play", cx, cy - 45, paint);
        paint.setTextSize(74);
        paint.setColor(Color.rgb(255, 220, 80));
        canvas.drawText(String.valueOf((int) Math.ceil(remaining)), cx, cy + 35, paint);

        float left = cx - 320;
        float right = cx + 320;
        float top = cy + 80;
        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawRoundRect(new RectF(left, top, right, top + 26), 13, 13, paint);
        paint.setColor(Color.rgb(255, 68, 0));
        canvas.drawRoundRect(new RectF(left, top, left + (right - left) * progress, top + 26), 13, 13, paint);
        paint.setTextSize(28);
        paint.setColor(Color.argb(190, 255, 255, 255));
        canvas.drawText("Peak: " + calibrationPeakAmplitude, cx, top + 75, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsToggle(Canvas canvas, String label, boolean enabled,
                                    float left, float top, float right, float bottom) {
        drawPremiumButtonSurface(canvas, new RectF(left, top, right, bottom),
                enabled ? Color.rgb(60, 180, 80) : Color.rgb(38, 44, 66),
                enabled ? Color.rgb(20, 110, 40) : Color.rgb(14, 16, 30),
                enabled, true, enabled ? 0.75f : 0.2f);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(30);
        paint.setColor(enabled ? Color.WHITE : Color.argb(180, 220, 228, 240));
        canvas.drawText(label + ": " + (enabled ? "ON" : "OFF"),
                (left + right) / 2f, top + 50, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private RectF getRestoreDefaultsRect(float contentTop) {
        float width = Math.min(420f, screenWidth * 0.36f);
        float height = 58f;
        float right = screenWidth - Math.max(58f, screenWidth * 0.07f);
        float bottom = Math.min(screenHeight - 40f, Math.max(contentTop + 360f, screenHeight - 52f));
        return new RectF(right - width, bottom - height, right, bottom);
    }

    /** Draws a small "Reset" pill in the top-right of each settings tab */
    private void drawTabResetButton(Canvas canvas, float contentTop) {
        float btnRight = screenWidth - Math.max(48f, screenWidth * 0.055f);
        float btnLeft = btnRight - Math.min(200f, screenWidth * 0.19f);
        float btnTop2 = contentTop + 36f;
        float btnBot = contentTop + 82f;
        RectF r = new RectF(btnLeft, btnTop2, btnRight, btnBot);
        drawPremiumButtonSurface(canvas, r,
                Color.rgb(62, 72, 108), Color.rgb(22, 26, 46), false, true, 0.28f);
        paint.setShader(null);
        paint.setColor(Color.argb(200, 255, 255, 255));
        paint.setTextSize(22f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("↺  Reset", r.centerX(), r.centerY() + 8f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** Returns the hit rect for the per-tab reset button */
    private RectF getTabResetRect(float contentTop) {
        float btnRight = screenWidth - Math.max(48f, screenWidth * 0.055f);
        float btnLeft = btnRight - Math.min(200f, screenWidth * 0.19f);
        return new RectF(btnLeft, contentTop + 36f, btnRight, contentTop + 82f);
    }

    private String signedSettingLabel(float pct) {
        int value = Math.round(pct - 50f);
        if (value == 0) {
            return "0";
        }
        return (value > 0 ? "+" : "") + value;
    }

    /** Resets only the settings in the currently active tab */
    private void resetCurrentTab() {
        switch (activeSettingsTab) {
            case SETTINGS_TAB_MODES:
                selectedMode = -1;
                applySelectedModeBaseThresholds();
                break;
            case SETTINGS_TAB_VOICE:
                sensitivityPct = 50f;
                applySlider();
                break;
            case SETTINGS_TAB_UI:
                hudOpacityPct = 100f;
                break;
            case SETTINGS_TAB_GRAPHICS:
                graphicsQualityIndex = 1;
                applyGraphicsQualityDefaults(graphicsQualityIndex);
                activeDropdown = DROPDOWN_NONE;
                applyRenderingSettings();
                break;
            case SETTINGS_TAB_POST:
                saturationPct = 50f;
                contrastPct = 50f;
                exposurePct = 50f;
                break;
            case SETTINGS_TAB_LIGHTING:
                highSunIntensity = false;
                break;
            case SETTINGS_TAB_AUDIO:
                masterAudioBoostPct = 50f;
                break;
            case SETTINGS_TAB_GAMEPLAY:
                dailyChallenge = false;
                hapticsEnabled = true;
                break;
        }
    }

    private void restoreEverythingToDefault() {
        selectedMode = -1;
        sensitivityPct = 50f;
        hudOpacityPct = 100f;
        graphicsQualityIndex = 1;
        applyGraphicsQualityDefaults(graphicsQualityIndex);
        saturationPct = 50f;
        contrastPct = 50f;
        exposurePct = 50f;
        highSunIntensity = false;
        masterAudioBoostPct = 50f;
        dailyChallenge = false;
        hapticsEnabled = true;
        activeDropdown = DROPDOWN_NONE;
        draggingSlider = false;
        activeSlider = SLIDER_NONE;
        applySelectedModeBaseThresholds();
        applySlider();
        applyRenderingSettings();
    }

    private void applySlider() {
        syncSettingsManagerFromLocal();
        sm.applySensitivity();
        syncAudioCalibration();
    }

    private void applyRenderingSettings() {
        syncSettingsManagerFromLocal();
        updateRenderingTier();
        boolean antiAlias = msaaIndex != 0 && graphicsQualityIndex != 0;
        paint.setAntiAlias(antiAlias);
        groundBandPaint.setAntiAlias(antiAlias);
        settingsBgPaint.setAntiAlias(antiAlias);
        firstPromptBgPaint.setAntiAlias(antiAlias);
        auxPaint.setAntiAlias(antiAlias);
        cloudPaint.setAntiAlias(antiAlias);
    }

    private void syncAudioCalibration() {
        syncSettingsManagerFromLocal();
        sm.configureAudio(audioEngine);
    }

    private void applySelectedModeBaseThresholds() {
        syncSettingsManagerFromLocal();
        sm.applySelectedModeBaseThresholds();
    }

    private void saveSettings() {
        syncSettingsManagerFromLocal();
        sm.save(getContext());
    }

    private void syncLocalSettingsFromManager() {
        selectedMode = sm.selectedMode;
        sensitivityPct = sm.sensitivityPct;
        hudOpacityPct = sm.hudOpacityPct;
        shadowsEnabled = sm.shadowsEnabled;
        graphicsQualityIndex = sm.graphicsQualityIndex;
        shadowPresetIndex = sm.shadowPresetIndex;
        shadowResolutionIndex = sm.shadowResolutionIndex;
        shadowCascadesIndex = sm.shadowCascadesIndex;
        softShadows = sm.softShadows;
        msaaIndex = sm.msaaIndex;
        saturationPct = sm.saturationPct;
        contrastPct = sm.contrastPct;
        exposurePct = sm.exposurePct;
        highSunIntensity = sm.highSunIntensity;
        masterAudioBoostPct = sm.masterAudioBoostPct;
        dailyChallenge = sm.dailyChallenge;
        hapticsEnabled = sm.hapticsEnabled;
    }

    private void syncSettingsManagerFromLocal() {
        sm.selectedMode = selectedMode;
        sm.sensitivityPct = sensitivityPct;
        sm.hudOpacityPct = hudOpacityPct;
        sm.shadowsEnabled = shadowsEnabled;
        sm.graphicsQualityIndex = graphicsQualityIndex;
        sm.shadowPresetIndex = shadowPresetIndex;
        sm.shadowResolutionIndex = shadowResolutionIndex;
        sm.shadowCascadesIndex = shadowCascadesIndex;
        sm.softShadows = softShadows;
        sm.msaaIndex = msaaIndex;
        sm.saturationPct = saturationPct;
        sm.contrastPct = contrastPct;
        sm.exposurePct = exposurePct;
        sm.highSunIntensity = highSunIntensity;
        sm.masterAudioBoostPct = masterAudioBoostPct;
        sm.dailyChallenge = dailyChallenge;
        sm.hapticsEnabled = hapticsEnabled;
    }

    private void restart() {
        synchronized (obstacles) {
            obstacles.clear();
        }
        ps.clear();
        score = 0;
        gameOver = false;
        gamePaused = false;
        gameStarted = false;
        calibrating = false;
        skyRaidAnnounced = false;
        runEventBannerUntil = 0;
        if (player != null) player.reset(groundY);
        lastObstacleTime = 0;
        if (soundPool != null) soundPool.autoPause();
    }

    private void playSound(int soundId) {
        if (soundPool != null && soundId != -1) {
            float volume = getMasterAudioBoost();
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        }
    }

    private float getMasterAudioBoost() {
        return 0.55f + (masterAudioBoostPct / 100f) * 1.45f;
    }

    private boolean isShareButtonHit(float x, float y) {
        float shareLeft = screenWidth - SHARE_BTN_MARGIN - SHARE_BTN_SIZE;
        float shareTop = SHARE_BTN_MARGIN;
        return x >= shareLeft && x <= shareLeft + SHARE_BTN_SIZE
                && y >= shareTop && y <= shareTop + SHARE_BTN_SIZE;
    }

    private void shareScore() {
        try {
            isCapturingShare = true; // FIX: hide share button during screenshot capture
            // FIX: Reduced from 1080x1080 (4.5MB) to 720x720 (2MB) — same visual quality, half the RAM
            final int W = 1080, H = 1350;
            Bitmap card = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(card);

            // ── Background gradient (top dark → bottom slightly lighter) ──
            android.graphics.LinearGradient grad = new android.graphics.LinearGradient(
                    0, 0, 0, H,
                    new int[]{Color.rgb(8, 8, 18), Color.rgb(18, 18, 38)},
                    null, android.graphics.Shader.TileMode.CLAMP);
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setShader(grad);
            c.drawRect(0, 0, W, H, bgPaint);
            bgPaint.setShader(null);

            // ── Accent top bar ──
            bgPaint.setColor(Color.rgb(255, 68, 0));
            c.drawRect(0, 0, W, 14, bgPaint);

            // ── "VOCEX RUN" title ──
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
            titlePaint.setTextSize(96f);
            titlePaint.setColor(Color.rgb(255, 68, 0));
            titlePaint.setLetterSpacing(0.15f);
            c.drawText("VOCEX RUN", W / 2f, 170, titlePaint);

            // ── Mode badge ──
            String modeLabel = selectedMode == 0 ? "SILENT MODE"
                    : selectedMode == 1 ? "NORMAL MODE"
                    : selectedMode == 2 ? "RAGE MODE"
                    : "VOCEX RUN";
            if (dailyChallenge) {
                modeLabel += " DAILY";
            }
            Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setTextAlign(Paint.Align.CENTER);
            badgePaint.setTextSize(38f);
            badgePaint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
            badgePaint.setColor(Color.WHITE);
            float badgeW = badgePaint.measureText(modeLabel) + 60;
            float badgeLeft = W / 2f - badgeW / 2f;
            Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeBg.setColor(Color.argb(160, 255, 68, 0));
            c.drawRoundRect(new RectF(badgeLeft, 192, badgeLeft + badgeW, 246), 20, 20, badgeBg);
            c.drawText(modeLabel, W / 2f, 234, badgePaint);

            // ── Divider ──
            Paint divPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            divPaint.setColor(Color.argb(60, 255, 255, 255));
            divPaint.setStrokeWidth(2f);
            c.drawLine(120, 280, W - 120, 280, divPaint);

            // ── Score label ──
            Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTextSize(46f);
            labelPaint.setColor(Color.argb(180, 255, 255, 255));
            labelPaint.setLetterSpacing(0.2f);
            c.drawText("SCORE", W / 2f, 370, labelPaint);

            // ── Big score number ──
            Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            scorePaint.setTextAlign(Paint.Align.CENTER);
            scorePaint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
            scorePaint.setTextSize(260f);
            scorePaint.setColor(Color.WHITE);
            c.drawText(String.valueOf(finalScore), W / 2f, 640, scorePaint);

            // ── "metres" unit ──
            Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            unitPaint.setTextAlign(Paint.Align.CENTER);
            unitPaint.setTextSize(52f);
            unitPaint.setColor(Color.rgb(255, 68, 0));
            c.drawText("metres", W / 2f, 710, unitPaint);

            // ── Best score line ──
            Paint bestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bestPaint.setTextAlign(Paint.Align.CENTER);
            bestPaint.setTextSize(40f);
            bestPaint.setColor(Color.rgb(255, 204, 0));
            String bestLabel = dailyChallenge
                    ? "Daily Best: " + dailyBestScore + "m"
                    : "Personal Best: " + highScore + "m";
            c.drawText(bestLabel, W / 2f, 790, bestPaint);

            // ── Fail message ──
            Paint failPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            failPaint.setTextAlign(Paint.Align.CENTER);
            failPaint.setTextSize(36f);
            failPaint.setColor(Color.argb(200, 200, 200, 200));
            failPaint.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC));
            c.drawText("\"" + failMessage + "\"", W / 2f, 870, failPaint);

            // ── CTA bottom ──
            Paint ctaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ctaPaint.setTextAlign(Paint.Align.CENTER);
            ctaPaint.setTextSize(34f);
            ctaPaint.setColor(Color.argb(150, 255, 255, 255));
            c.drawText("Can you beat me?", W / 2f, 960, ctaPaint);
            drawShareCardPromoScene(c, W, H);

            // ── Bottom accent bar ──
            bgPaint.setColor(Color.rgb(255, 68, 0));
            c.drawRect(0, H - 14, W, H, bgPaint);

            // ── Save & share ──
            File shareDir = new File(getContext().getCacheDir(), "share");
            if (!shareDir.exists() && !shareDir.mkdirs()) return;
            File imageFile = new File(shareDir, "vocex_run_score.png");
            FileOutputStream out = new FileOutputStream(imageFile);
            card.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri uri = FileProvider.getUriForFile(getContext(),
                    getContext().getPackageName() + ".fileprovider", imageFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(
                    getContext().getContentResolver(), "Vocex Run score", uri));
            intent.putExtra(Intent.EXTRA_TEXT,
                    "🔥 I just ran " + finalScore + "m in Vocex Run"
                            + (dailyChallenge ? " (Daily Challenge)" : "") + "!\n"
                            + "Think you can beat me? 😏\n"
                            + "Download now 👉 https://github.com/ShounakPatra/vocex-run");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(Intent.createChooser(intent, "Share Vocex Run score"));

        } catch (IOException | IllegalArgumentException e) {
            android.util.Log.e("VocexRun", "Error sharing score", e);
        } finally {
            isCapturingShare = false; // FIX: always reset, even on failure
        }
    }

    private String getScoreRankLabel(int scoreValue) {
        if (scoreValue >= 1000) {
            return "Legend Run";
        } else if (scoreValue >= 700) {
            return "Sky Raider";
        } else if (scoreValue >= 450) {
            return "Rage Pilot";
        } else if (scoreValue >= 300) {
            return "Enemy Wave Survivor";
        } else if (scoreValue >= 150) {
            return "Desert Dasher";
        }
        return "Fresh Runner";
    }

    private void drawShareCardPromoScene(Canvas canvas, int width, int height) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float sceneTop = height - 330f;
        float sceneBottom = height - 42f;
        RectF scene = new RectF(70f, sceneTop, width - 70f, sceneBottom);

        p.setShader(new android.graphics.LinearGradient(
                scene.left, scene.top, scene.right, scene.bottom,
                Color.argb(214, 28, 34, 68),
                Color.argb(224, 98, 42, 22),
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(scene, 34f, 34f, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.5f);
        p.setColor(Color.argb(165, 255, 188, 82));
        canvas.drawRoundRect(scene, 34f, 34f, p);
        p.setStyle(Paint.Style.FILL);

        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        p.setTextSize(34f);
        p.setColor(Color.WHITE);
        canvas.drawText(getScoreRankLabel(finalScore), width / 2f, scene.top + 54f, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);
        p.setTextSize(24f);
        p.setColor(Color.argb(190, 230, 238, 255));
        canvas.drawText("Sky raids unlock after 300m", width / 2f, scene.top + 88f, p);

        float ground = scene.bottom - 70f;
        p.setColor(Color.rgb(223, 174, 86));
        canvas.drawRect(scene.left + 28f, ground, scene.right - 28f, scene.bottom - 26f, p);
        p.setColor(Color.argb(92, 255, 236, 160));
        for (int i = 0; i < 8; i++) {
            float x = scene.left + 72f + i * 102f;
            canvas.drawLine(x, ground + 36f, x + 48f, ground + 22f, p);
        }

        drawShareMiniRunner(canvas, p, scene.left + 210f, ground - 138f, 124f, 160f);
        drawShareMiniEnemy(canvas, p, scene.left + 558f, ground - 166f, 132f, 82f);
        drawShareMiniCactus(canvas, p, scene.right - 168f, ground - 130f, 62f, 130f);
    }

    private void drawShareMiniRunner(Canvas canvas, Paint p, float x, float y, float w, float h) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(15f);
        p.setColor(Color.rgb(28, 72, 195));
        canvas.drawLine(x + w * 0.42f, y + h * 0.63f, x + w * 0.20f, y + h * 0.94f, p);
        canvas.drawLine(x + w * 0.58f, y + h * 0.63f, x + w * 0.90f, y + h * 0.82f, p);
        p.setColor(Color.rgb(220, 44, 34));
        canvas.drawLine(x + w * 0.35f, y + h * 0.44f, x + w * 0.12f, y + h * 0.62f, p);
        canvas.drawLine(x + w * 0.66f, y + h * 0.44f, x + w * 0.92f, y + h * 0.30f, p);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStyle(Paint.Style.FILL);

        p.setColor(Color.rgb(232, 48, 34));
        canvas.drawRoundRect(new RectF(x + w * 0.22f, y + h * 0.30f,
                x + w * 0.78f, y + h * 0.68f), 18f, 18f, p);
        p.setColor(Color.rgb(255, 198, 138));
        canvas.drawRoundRect(new RectF(x + w * 0.29f, y + h * 0.06f,
                x + w * 0.71f, y + h * 0.33f), 18f, 18f, p);
        p.setColor(Color.rgb(226, 42, 32));
        canvas.drawRoundRect(new RectF(x + w * 0.22f, y, x + w * 0.78f,
                y + h * 0.15f), 14f, 14f, p);
        p.setColor(Color.WHITE);
        canvas.drawCircle(x + w * 0.40f, y + h * 0.19f, 5f, p);
        canvas.drawCircle(x + w * 0.62f, y + h * 0.19f, 5f, p);
        p.setColor(Color.rgb(45, 28, 12));
        canvas.drawCircle(x + w * 0.43f, y + h * 0.19f, 2.7f, p);
        canvas.drawCircle(x + w * 0.65f, y + h * 0.19f, 2.7f, p);
    }

    private void drawShareMiniEnemy(Canvas canvas, Paint p, float x, float y, float w, float h) {
        android.graphics.Path path = new android.graphics.Path();
        p.setColor(Color.argb(80, 96, 206, 255));
        canvas.drawOval(new RectF(x - 56f, y + h * 0.20f,
                x + w * 0.74f, y + h * 0.86f), p);
        p.setColor(Color.rgb(36, 44, 56));
        path.moveTo(x + w * 0.45f, y + h * 0.48f);
        path.cubicTo(x - w * 0.10f, y - h * 0.22f, x - w * 0.50f, y + h * 0.08f,
                x - w * 0.24f, y + h * 0.72f);
        path.close();
        canvas.drawPath(path, p);
        path.reset();
        path.moveTo(x + w * 0.55f, y + h * 0.48f);
        path.cubicTo(x + w * 1.08f, y - h * 0.18f, x + w * 1.40f, y + h * 0.10f,
                x + w * 1.08f, y + h * 0.72f);
        path.close();
        canvas.drawPath(path, p);
        p.setColor(Color.rgb(214, 222, 226));
        canvas.drawOval(new RectF(x + w * 0.16f, y + h * 0.20f,
                x + w * 0.96f, y + h * 0.78f), p);
        p.setColor(Color.rgb(255, 58, 42));
        canvas.drawCircle(x + w * 0.82f, y + h * 0.40f, 7f, p);
        p.setColor(Color.rgb(226, 166, 54));
        path.reset();
        path.moveTo(x + w * 0.96f, y + h * 0.46f);
        path.lineTo(x + w * 1.18f, y + h * 0.54f);
        path.lineTo(x + w * 0.96f, y + h * 0.62f);
        path.close();
        canvas.drawPath(path, p);
    }

    private void drawShareMiniCactus(Canvas canvas, Paint p, float x, float y, float w, float h) {
        p.setColor(Color.rgb(28, 126, 54));
        canvas.drawRoundRect(new RectF(x + w * 0.28f, y, x + w * 0.72f,
                y + h), 18f, 18f, p);
        canvas.drawRoundRect(new RectF(x - w * 0.08f, y + h * 0.34f,
                x + w * 0.38f, y + h * 0.56f), 14f, 14f, p);
        canvas.drawRoundRect(new RectF(x - w * 0.08f, y + h * 0.14f,
                x + w * 0.24f, y + h * 0.48f), 14f, 14f, p);
        canvas.drawRoundRect(new RectF(x + w * 0.62f, y + h * 0.48f,
                x + w * 1.06f, y + h * 0.70f), 14f, 14f, p);
        canvas.drawRoundRect(new RectF(x + w * 0.86f, y + h * 0.28f,
                x + w * 1.08f, y + h * 0.62f), 14f, 14f, p);
        p.setColor(Color.argb(105, 182, 255, 150));
        canvas.drawRoundRect(new RectF(x + w * 0.38f, y + h * 0.05f,
                x + w * 0.47f, y + h * 0.88f), 6f, 6f, p);
    }

    private void drawPremiumGround(Canvas canvas, AudioEngine.VoiceLevel level) {
        worldRenderer.drawPremiumGround(canvas, paint, auxPaint, screenWidth,
                screenHeight, groundY, score, level, premiumRendering);
    }

    private void drawUltraRealisticMountains(Canvas canvas) {
        worldRenderer.drawUltraRealisticMountains(canvas, paint, auxPaint,
                screenWidth, groundY, score, premiumRendering);
    }



    private void drawBackgroundDunes(Canvas canvas, AudioEngine.VoiceLevel level) {
        worldRenderer.drawBackgroundDunes(canvas, paint, auxPaint, screenWidth,
                screenHeight, groundY, score, premiumRendering);
    }

    private void drawMotionBlurTrail(Canvas canvas, AudioEngine.VoiceLevel level, long now) {
        characterRenderer.drawMotionBlurTrail(canvas, paint, player, gameStarted,
                gamePaused, premiumRendering, level);
    }

    private void drawRageBloom(Canvas canvas, AudioEngine.VoiceLevel level, long now) {
        characterRenderer.drawRageBloom(canvas, auxPaint, player, level, now);
    }

    private void drawRoofContactFlash(Canvas canvas, long now) {
        if (!gameStarted || gamePaused || gameOver || player == null || !player.touchingRoof) {
            return;
        }
        float pulse = 0.74f + 0.26f * (float) Math.sin(now * 0.035f);
        paint.setColor(Color.argb((int) (118 * pulse), 255, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
    }

    private void drawPostProcessingOverlay(Canvas canvas, AudioEngine.VoiceLevel level) {
        backgroundRenderer.drawPostProcessingOverlay(canvas, paint, auxPaint,
                screenWidth, screenHeight, groundY, exposurePct, saturationPct, contrastPct);
    }

    public boolean shouldDrawShadows() {
        return sm.shadowsEnabled;
    }

    public boolean shouldDrawSceneShadows() {
        return shouldDrawShadows() && getDaylightAmount() > 0.18f;
    }

    public int shadowAlpha(int baseAlpha) {
        if (!shouldDrawShadows() || baseAlpha <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(255, (int) (baseAlpha * getShadowAlphaScale())));
    }

    public float getShadowAlphaScale() {
        switch (sm.shadowPresetIndex) {
            case 0:
                return 0.55f;
            case 2:
                return 0.98f;
            case 3:
                return 1.18f;
            case 4:
                return 0.55f + sm.shadowResolutionIndex * 0.10f + sm.shadowCascadesIndex * 0.08f;
            case 1:
            default:
                return 0.78f;
        }
    }

    public float getSoftShadowExtra() {
        if (!sm.softShadows || !sm.shadowsEnabled) {
            return 0f;
        }
        return 4f + sm.shadowCascadesIndex * 1.5f + sm.shadowResolutionIndex * 0.8f;
    }

    private void drawCactus3D(Canvas canvas, Obstacle obs) {
        obstacleRenderer.drawCactus3D(canvas, paint, auxPaint, obs, groundY, premiumRendering);
    }

    private void drawRunnerLeg(Canvas canvas, float px, float py, float pw, float ph,
                               float phase, boolean nearLeg, boolean onGround,
                               int mainColor, int shadowColor, int shoeColor, int shoeHighlight) {
        characterRenderer.drawRunnerLeg(canvas, paint, px, py, pw, ph, phase,
                nearLeg, onGround, groundY, mainColor, shadowColor, shoeColor, shoeHighlight);
    }

    private void drawRunnerArm(Canvas canvas, float px, float py, float pw, float ph,
                               float phase, boolean nearArm, boolean onGround,
                               int sleeveColor, int handColor, int handShadow) {
        characterRenderer.drawRunnerArm(canvas, paint, px, py, pw, ph, phase,
                nearArm, onGround, sleeveColor, handColor, handShadow);
    }

    public int withAlpha(int color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (safeAlpha << 24);
    }

    public int hudAlpha(int alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * (sm.hudOpacityPct / 100f))));
    }

    public int hudColor(int color) {
        return withAlpha(color, hudAlpha(Color.alpha(color)));
    }

    private void drawPlayerPremiumHighlights(Canvas canvas, AudioEngine.VoiceLevel level, long now) {
        characterRenderer.drawPlayerPremiumHighlights(canvas, paint, player,
                level, now, premiumRendering);
    }

    private void drawRealisticCloud(Canvas canvas, float cx, float cy, float dimAlpha) {
        worldRenderer.drawRealisticCloud(canvas, cloudPaint, cx, cy, dimAlpha, premiumRendering);
    }

    private void markLaunched() {
        getContext().getSharedPreferences("VocexRunPrefs", Context.MODE_PRIVATE)
                .edit().putBoolean("hasLaunched", true).apply();
    }



    private void drawFirstTimePrompt(Canvas canvas) {
        float cx = screenWidth / 2f, cy = screenHeight / 2f;

        if (renderCacheReady) {
            canvas.drawRect(0, 0, screenWidth, screenHeight, firstPromptBgPaint);
        } else {
            paint.setColor(Color.argb(252, 6, 6, 20));
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        }

        // FIX: icon smaller + higher so it no longer overlaps the title
        int sz = 100;
        float iconTop = cy - 330f;
        try {
            android.graphics.drawable.Drawable icon = getContext().getPackageManager()
                    .getApplicationIcon(getContext().getApplicationInfo());
            int ix = (int) (cx - sz / 2f), iy = (int) iconTop;
            icon.setBounds(ix, iy, ix + sz, iy + sz);
            icon.draw(canvas);
        } catch (Exception ignored) {
        }

        // Title 20px below icon bottom — zero overlap
        float titleY = iconTop + sz + 20 + 58;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 68, 0));
        paint.setTextSize(70);
        canvas.drawText("VOCEX RUN", cx, titleY, paint);

        float subtitleY = titleY + 50;
        paint.setColor(Color.argb(200, 220, 220, 240));
        paint.setTextSize(30);
        canvas.drawText("Are you a new player or an old player?", cx, subtitleY, paint);

        float divY = subtitleY + 26;
        paint.setColor(Color.argb(50, 255, 255, 255));
        canvas.drawRoundRect(new RectF(cx - 340, divY, cx + 340, divY + 2), 2, 2, paint);

        float btnTop = divY + 18;
        float btnBottom = btnTop + 112;
        RectF newPlayerRect = new RectF(cx - 334, btnTop, cx - 16, btnBottom);
        RectF oldPlayerRect = new RectF(cx + 16, btnTop, cx + 334, btnBottom);

        // FIX: glass premium surface for NEW PLAYER — matches home menu buttons
        drawPremiumButtonSurface(canvas, newPlayerRect,
                Color.rgb(255, 120, 28), Color.rgb(200, 48, 0), true, true, 0.9f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        canvas.drawText("NEW PLAYER", newPlayerRect.centerX(), btnTop + 54, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.argb(200, 255, 255, 255));
        paint.setTextSize(21);
        canvas.drawText("Show me the tutorial", newPlayerRect.centerX(), btnTop + 84, paint);

        // FIX: glass premium surface for OLD PLAYER — matches home menu buttons
        drawPremiumButtonSurface(canvas, oldPlayerRect,
                Color.rgb(54, 120, 180), Color.rgb(18, 52, 96), false, true, 0.55f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        canvas.drawText("OLD PLAYER", oldPlayerRect.centerX(), btnTop + 54, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(Color.argb(200, 255, 255, 255));
        paint.setTextSize(21);
        canvas.drawText("Jump straight in", oldPlayerRect.centerX(), btnTop + 84, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTutorialOverlay(Canvas canvas) {
        float cx = screenWidth / 2f, cy = screenHeight / 2f;

        paint.setColor(Color.argb(248, 5, 5, 18));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        // Step indicator dots
        for (int i = 0; i < TUTORIAL_STEPS; i++) {
            float dotX = cx - (TUTORIAL_STEPS - 1) * 22 + i * 44;
            paint.setColor(i == tutorialStep
                    ? Color.rgb(255, 68, 0) : Color.argb(70, 255, 255, 255));
            canvas.drawCircle(dotX, 38, i == tutorialStep ? 10 : 6, paint);
        }

        // SKIP button — glass style
        RectF skipRect = new RectF(screenWidth - 185, 12, screenWidth - 15, 68);
        drawPremiumButtonSurface(canvas, skipRect,
                Color.rgb(50, 55, 80), Color.rgb(18, 20, 38), false, true, 0.25f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SKIP  ✕", skipRect.centerX(), skipRect.centerY() + 9f, paint);

        // Step emoji
        String[] emojis = {"🎤", "📢", "🦘", "🌵", "🎮"};
        String[] titles = {
                "WELCOME TO VOCEX RUN!",
                "NOISE  =  SPEED",
                "SHOUT TO JUMP",
                "DODGE THE CACTI",
                "PICK YOUR MODE"
        };

        paint.setTextSize(110);
        canvas.drawText(emojis[tutorialStep], cx, cy - 105, paint);

        // Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText(titles[tutorialStep], cx, cy - 12, paint);

        // Orange divider
        paint.setColor(Color.rgb(255, 68, 0));
        canvas.drawRoundRect(new RectF(cx - 220, cy + 6, cx + 220, cy + 10), 4, 4, paint);

        // Description (3 lines per step)
        paint.setTextSize(30);
        String[][] descs = {
                {"You control everything with your VOICE.",
                        "No buttons. No tapping. Just noise.",
                        "The louder you are, the more you control."},
                {"Making any noise moves you forward.",
                        "Louder = faster.  Silent = you stop.",
                        "Whisper to walk. Yell to sprint."},
                {"A sudden loud burst = a jump.",
                        "Scream louder = jump higher.",
                        "Timing your yells is the whole game."},
                {"Cacti are your only enemies.",
                        "Touch one = instant death 💀",
                        "Jump over them by shouting at the right time."},
                null // step 4 uses glass mode buttons below
        };

        if (tutorialStep < 4) {
            String[] desc = descs[tutorialStep];
            for (int i = 0; i < desc.length; i++) {
                paint.setColor(i == 0
                        ? Color.rgb(220, 220, 240)
                        : Color.argb(180, 200, 200, 220));
                canvas.drawText(desc[i], cx, cy + 58 + i * 44, paint);
            }
        } else {
            // FIX: Step 4 "PICK YOUR MODE" — glass buttons instead of plain text
            String[] modeLabels = {"🤫  Silent", "🗣️  Normal", "💀  Rage"};
            String[] modeHints = {"for quiet rooms", "speak naturally", "full-send SCREAMING"};
            int[] modeStartColors = {
                    Color.rgb(54, 140, 200),
                    Color.rgb(54, 168, 90),
                    Color.rgb(210, 54, 54)
            };
            int[] modeEndColors = {
                    Color.rgb(18, 52, 96),
                    Color.rgb(18, 72, 32),
                    Color.rgb(96, 18, 18)
            };
            float btnW = Math.min(540f, screenWidth * 0.55f);
            float btnH = 72f;
            float btnGap = 18f;
            float btnX = cx - btnW / 2f;
            float btnStartY = cy + 32f;
            for (int i = 0; i < 3; i++) {
                float top2 = btnStartY + i * (btnH + btnGap);
                RectF modeRect = new RectF(btnX, top2, btnX + btnW, top2 + btnH);
                drawPremiumButtonSurface(canvas, modeRect,
                        modeStartColors[i], modeEndColors[i], false, true, 0.45f);
                paint.setShader(null);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setColor(Color.WHITE);
                paint.setTextSize(28);
                canvas.drawText(modeLabels[i], btnX + 28f, top2 + 32f, paint);
                paint.setColor(Color.argb(190, 220, 230, 245));
                paint.setTextSize(21);
                canvas.drawText(modeHints[i], btnX + 28f, top2 + 57f, paint);
                paint.setTextAlign(Paint.Align.CENTER);
            }
        }

        // NEXT / LET'S GO button — glass style
        boolean isLast = tutorialStep == TUTORIAL_STEPS - 1;
        RectF nextRect = new RectF(cx - 210, screenHeight - 108, cx + 210, screenHeight - 22);
        drawPremiumButtonSurface(canvas, nextRect,
                isLast ? Color.rgb(255, 176, 64) : Color.rgb(255, 92, 28),
                isLast ? Color.rgb(220, 80, 0) : Color.rgb(180, 40, 0),
                true, true, isLast ? 1.1f : 0.85f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(38);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD));
        canvas.drawText(isLast ? "LET'S GO! 🎮" : "NEXT  →", cx, screenHeight - 53, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void initializeVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                    (VibratorManager) getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager != null ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void vibrate(long millis) {
        if (!hapticsEnabled) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastHapticTimestamp < 24) return;
        lastHapticTimestamp = now;

        final long duration = Math.max(8L, Math.min(90L, millis));
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    int amplitude = vibrator.hasAmplitudeControl()
                            ? (duration >= 55L ? 220 : 170)
                            : VibrationEffect.DEFAULT_AMPLITUDE;
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
                } else {
                    vibrator.vibrate(duration);
                }
            }
        } catch (RuntimeException e) {
            android.util.Log.e("VocexRun", "Haptic feedback failed", e);
        }

        post(() -> performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK));
    }

    private String getDailyBestKey() {
        return "dailyBest_" + getDailyDateKey();
    }

    private long getDailySeed() {
        return getDailyDateKey().hashCode() * 31L + selectedMode;
    }

    private String getDailyDateKey() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private int getVoiceColor(AudioEngine.VoiceLevel level) {
        switch (level) {
            case SILENT:
                return Color.rgb(150, 150, 150);
            case WHISPER:
                return Color.rgb(150, 230, 255);
            case NORMAL:
                return Color.rgb(100, 255, 100);
            case SHOUT:
                return Color.rgb(255, 200, 0);
            case RAGE:
                return Color.rgb(255, 60, 60);
            default:
                return Color.WHITE;
        }
    }
}
