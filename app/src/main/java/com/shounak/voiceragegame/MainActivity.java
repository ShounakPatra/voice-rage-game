package com.shounak.voiceragegame;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.GameManager;
import android.app.GameState;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int MIC_PERMISSION_CODE = 101;
    private static boolean openingLoadingShownForProcess = false;
    private GameView gameView;
    private boolean showOpeningLoadingOnStart = true;
    private boolean openingLoadingConsumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showOpeningLoadingOnStart = shouldShowOpeningLoading(savedInstanceState);

        // Keep screen on while playing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Handle back press using the modern dispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gameView != null && !gameView.onBackPressed()) {
                    // GameView handled it internally
                    return;
                }
                // Default behavior: disable callback and trigger back again
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // Ask for microphone permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_CODE);
        } else {
            startGame(); // permission already granted, let's gooo
        }
    }

    @SuppressLint("NewApi")
    private void startGame() {
        boolean showOpeningLoading = showOpeningLoadingOnStart
                && !openingLoadingConsumed
                && !openingLoadingShownForProcess;
        openingLoadingConsumed = true;
        if (showOpeningLoading) {
            openingLoadingShownForProcess = true;
        }
        gameView = new GameView(this, showOpeningLoading);
        setContentView(gameView);
        hideSystemUI();
        notifyGameState(GameState.MODE_CONTENT, false);
    }

    private boolean shouldShowOpeningLoading(Bundle savedInstanceState) {
        Intent intent = getIntent();
        boolean fromRecents = intent != null
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
        return savedInstanceState == null && !fromRecents && !openingLoadingShownForProcess;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showOpeningLoadingOnStart = false;
        hideSystemUI();
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("WrongConstant")
    private void notifyGameState(int mode, boolean isLoading) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Using "game" literal to avoid "Must be one of..." lint error on older SDKs
            GameManager gameManager = (GameManager) getSystemService("game");
            if (gameManager != null) {
                gameManager.setGameState(new GameState(isLoading, mode));
            }
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI(); // re-hides if user swipes to reveal
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGame();
            } else {
                Toast.makeText(this,
                        "Mic permission denied. Game needs your voice bro 💀",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.cancelOpeningLoadingScreen();
            gameView.pause();
        }
        notifyGameState(GameState.MODE_NONE, false);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (gameView != null) {
            gameView.resume();
        }
        notifyGameState(GameState.MODE_CONTENT, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            openingLoadingShownForProcess = false;
        }
    }
}
