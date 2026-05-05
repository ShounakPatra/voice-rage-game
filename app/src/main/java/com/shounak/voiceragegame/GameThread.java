package com.shounak.voiceragegame;
import android.graphics.Canvas;
import android.os.Build;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;

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
        while (isRunning) {
            long startTime = System.nanoTime();

            Canvas canvas = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && gameView.shouldUseHardwareCanvas()) {
                    try {
                        canvas = surfaceHolder.lockHardwareCanvas();
                    } catch (Throwable ignored) {
                        // fallback to software canvas
                    }
                }
                if (canvas == null) {
                    canvas = surfaceHolder.lockCanvas();
                }
                if (canvas != null) {
                    // FIX: Removed synchronized(surfaceHolder) — lockCanvas() already holds the
                    // SurfaceHolder's internal lock. Double-locking it causes deadlocks on some devices.
                    gameView.update();
                    gameView.draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // Frame rate limiter
            long sleepNs = FRAME_TIME_NS - (System.nanoTime() - startTime);
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
