package com.shounak.voiceragegame;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final long TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS; // ~16ms

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean isRunning = false;

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
            long startTime = System.currentTimeMillis();

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    gameView.update(); // logic
                    gameView.draw(canvas); // graphics
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // Frame rate limiter
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = FRAME_TIME - elapsed;
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
            }
        }
    }
}