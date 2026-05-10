package com.shounak.voiceragegame;

import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    private static final float MAX_FRAME_SCALE = 4.0f;

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean isRunning = false;

    public GameThread(SurfaceHolder holder, GameView view) {
        this.surfaceHolder = holder;
        this.gameView = view;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    @Override
    public void run() {
        long lastFrameTime = System.nanoTime() - FRAME_TIME_NS;
        while (isRunning) {
            long startTime = System.nanoTime();
            float frameScale = Math.max(0.25f, Math.min(MAX_FRAME_SCALE,
                    (startTime - lastFrameTime) / (float) FRAME_TIME_NS));
            lastFrameTime = startTime;

            Canvas canvas = null;
            boolean usedHardwareCanvas = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && gameView.shouldUseHardwareCanvas()) {
                    try {
                        canvas = surfaceHolder.lockHardwareCanvas();
                        usedHardwareCanvas = canvas != null;
                    } catch (Throwable ignored) {
                        gameView.onHardwareCanvasFailed();
                    }
                }
                if (canvas == null) {
                    canvas = surfaceHolder.lockCanvas();
                    usedHardwareCanvas = false;
                }
                if (canvas != null) {
                    try {
                        gameView.update(frameScale);
                        gameView.draw(canvas);
                    } catch (RuntimeException e) {
                        if (usedHardwareCanvas) {
                            gameView.onHardwareCanvasFailed();
                        }
                        Log.e("VocexRun", "Render loop recovered after frame failure", e);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {
                    }
                }
            }

            long sleepNs = FRAME_TIME_NS - (System.nanoTime() - startTime);
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
