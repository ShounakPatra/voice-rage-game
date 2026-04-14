package com.shounak.voiceragegame;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioEngine {

    // Constants for mic setup
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL, ENCODING);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private volatile int currentAmplitude = 0; // volatile = thread-safe read

    // Voice levels (the whole game is based on this)
    public enum VoiceLevel {
        SILENT,  // Dead silent
        WHISPER, // Very quiet
        NORMAL,  // Normal talking
        SHOUT,   // Yelling
        RAGE     // Full send 💀
    }

    public void start() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL, ENCODING, BUFFER_SIZE);
        isRecording = true;
        audioRecord.startRecording();

        // Background thread — constantly reading mic
        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    // Calculate RMS amplitude (basically: how loud are you rn)
                    long sum = 0;
                    for (int i = 0; i < read; i++) {
                        sum += (long) buffer[i] * buffer[i];
                    }
                    currentAmplitude = (int) Math.sqrt((double) sum / read);
                }
            }
        }).start();
    }

    public void stop() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    public int getAmplitude() {
        return currentAmplitude;
    }

    // Convert raw amplitude number into a readable voice level
    public VoiceLevel getVoiceLevel() {
        if (currentAmplitude < 150)       return VoiceLevel.SILENT;
        else if (currentAmplitude < 600)  return VoiceLevel.WHISPER;
        else if (currentAmplitude < 1800) return VoiceLevel.NORMAL;
        else if (currentAmplitude < 5000) return VoiceLevel.SHOUT;
        else                              return VoiceLevel.RAGE;
    }
}