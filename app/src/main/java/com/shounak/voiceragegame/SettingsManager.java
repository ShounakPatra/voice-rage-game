package com.shounak.voiceragegame;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    public int selectedMode = -1;
    public float sensitivityPct = 50f;
    public float hudOpacityPct = 100f;
    public boolean shadowsEnabled = false;
    public int graphicsQualityIndex = 1;
    public int shadowPresetIndex = 1;
    public int shadowResolutionIndex = 2;
    public int shadowCascadesIndex = 1;
    public boolean softShadows = true;
    public int msaaIndex = 1;
    public float saturationPct = 50f;
    public float contrastPct = 50f;
    public float exposurePct = 50f;
    public boolean highSunIntensity = false;
    public float masterAudioBoostPct = 50f;
    public boolean dailyChallenge = false;
    public boolean hapticsEnabled = true;

    private static final String PREFS_NAME = "VocexRunPrefs";

    public void load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sensitivityPct = clamp(prefs.getFloat("sensitivityPct", 50f), 0f, 100f);
        hudOpacityPct = clamp(prefs.getFloat("hudOpacityPct", 100f), 15f, 100f);
        shadowsEnabled = prefs.getBoolean("shadowsEnabled", false);
        graphicsQualityIndex = clamp(prefs.getInt("graphicsQualityIndex", 1), 0, 3);
        shadowPresetIndex = clamp(prefs.getInt("shadowPresetIndex", 1), 0, 4);
        shadowResolutionIndex = clamp(prefs.getInt("shadowResolutionIndex", 2), 0, 4);
        shadowCascadesIndex = clamp(prefs.getInt("shadowCascadesIndex", 1), 0, 3);
        softShadows = prefs.getBoolean("softShadows", true);
        msaaIndex = clamp(prefs.getInt("msaaIndex", 1), 0, 2);
        saturationPct = clamp(prefs.getFloat("saturationPct", 50f), 0f, 100f);
        contrastPct = clamp(prefs.getFloat("contrastPct", 50f), 0f, 100f);
        exposurePct = clamp(prefs.getFloat("exposurePct", 50f), 0f, 100f);
        highSunIntensity = prefs.getBoolean("highSunIntensity", false);
        masterAudioBoostPct = clamp(prefs.getFloat("masterAudioBoostPct", 50f), 0f, 100f);
        selectedMode = prefs.getInt("selectedMode", -1);
        dailyChallenge = prefs.getBoolean("dailyChallenge", false);
        hapticsEnabled = prefs.getBoolean("hapticsEnabled", true);
    }

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("sensitivityPct", sensitivityPct)
                .putFloat("hudOpacityPct", hudOpacityPct)
                .putBoolean("shadowsEnabled", shadowsEnabled)
                .putInt("graphicsQualityIndex", graphicsQualityIndex)
                .putInt("shadowPresetIndex", shadowPresetIndex)
                .putInt("shadowResolutionIndex", shadowResolutionIndex)
                .putInt("shadowCascadesIndex", shadowCascadesIndex)
                .putBoolean("softShadows", softShadows)
                .putInt("msaaIndex", msaaIndex)
                .putFloat("saturationPct", saturationPct)
                .putFloat("contrastPct", contrastPct)
                .putFloat("exposurePct", exposurePct)
                .putBoolean("highSunIntensity", highSunIntensity)
                .putFloat("masterAudioBoostPct", masterAudioBoostPct)
                .putInt("selectedMode", selectedMode)
                .putBoolean("dailyChallenge", dailyChallenge)
                .putBoolean("hapticsEnabled", hapticsEnabled)
                .apply();
    }

    public float getMasterAudioBoost() {
        return 0.55f + (masterAudioBoostPct / 100f) * 1.45f;
    }

    public void applySelectedModeBaseThresholds() {
        switch (selectedMode) {
            case 0:
                Player.jumpAmpThreshold = 800;
                Player.jumpDeltaThreshold = 300;
                break;
            case 1:
                Player.jumpAmpThreshold = 2500;
                Player.jumpDeltaThreshold = 800;
                break;
            case 2:
                Player.jumpAmpThreshold = 6000;
                Player.jumpDeltaThreshold = 2500;
                break;
            default:
                break;
        }
    }

    public void applySensitivity() {
        Player.sensitivityMultiplier = 2.0f - (sensitivityPct / 100f) * 1.7f;
    }

    public void configureAudio(AudioEngine audioEngine) {
        if (audioEngine == null || selectedMode < 0) {
            return;
        }
        int effectiveJumpThreshold = clamp(
                (int) (Player.jumpAmpThreshold * Player.sensitivityMultiplier), 120, 16000);
        audioEngine.configureVoiceProfile(effectiveJumpThreshold, selectedMode);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
