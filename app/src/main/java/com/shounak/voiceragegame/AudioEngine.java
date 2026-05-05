package com.shounak.voiceragegame;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

public class AudioEngine {

    private static final String TAG = "AudioEngine";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = getSafeBufferSize();
    private static final int READ_BUFFER_SIZE = getLowLatencyReadSize();
    private static final long JUMP_PULSE_LATCH_MS = 95L;
    private static final long JUMP_PULSE_DEBOUNCE_MS = 125L;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private volatile int currentAmplitude = 0;
    private volatile int instantJumpThreshold = 1600;
    private volatile int instantJumpDeltaThreshold = 280;
    private volatile long jumpPulseUntilMs = 0L;
    private volatile int jumpPulseAmplitude = 0;
    private int previousRawAmplitude = 0;
    private long lastJumpPulseAtMs = 0L;

    // Smoothed amplitude for display only (voice meter bar)
    private volatile int smoothedAmplitude = 0;
    private volatile int silentThreshold = 180;
    private volatile int whisperThreshold = 650;
    private volatile int normalThreshold = 1800;
    private volatile int shoutThreshold = 4300;
    private volatile int rageThreshold = 7600;

    public enum VoiceLevel {
        SILENT, WHISPER, NORMAL, SHOUT, RAGE
    }

    @SuppressLint("MissingPermission")
    public synchronized void start() {
        if (isRecording || (recordingThread != null && recordingThread.isAlive())) return;

        AudioRecord record = null;
        try {
            record = createRecorder(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            if (record == null) {
                record = createRecorder(MediaRecorder.AudioSource.MIC);
            }
            if (record == null) return;

            audioRecord = record;
            isRecording = true;
            record.startRecording();
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Failed to start recording", e);
            isRecording = false;
            if (record != null) {
                record.release();
            }
            audioRecord = null;
            return;
        }

        AudioRecord activeRecord = record;
        recordingThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            short[] buffer = new short[READ_BUFFER_SIZE];
            int consecutiveErrors = 0;
            final int MAX_CONSECUTIVE_ERRORS = 8; // FIX: retry on glitch instead of dying

            while (isRecording) {
                try {
                    int read = activeRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        consecutiveErrors = 0; // FIX: reset on success
                        long sum = 0;
                        int i = 0;
                        do {
                            sum += (long) buffer[i] * buffer[i];
                            i++;
                        } while (i < read);
                        int amplitude = (int) Math.sqrt((double) sum / read);
                        currentAmplitude = amplitude;
                        detectJumpPulse(amplitude);
                        synchronized (this) {
                            float blend = amplitude > smoothedAmplitude ? 0.46f : 0.18f;
                            smoothedAmplitude = (int) (smoothedAmplitude * (1f - blend) + amplitude * blend);
                        }
                    } else if (read == AudioRecord.ERROR_INVALID_OPERATION
                            || read == AudioRecord.ERROR_BAD_VALUE) {
                        // FIX: transient glitch — back off and retry instead of dying immediately
                        consecutiveErrors++;
                        Log.w(TAG, "AudioRecord transient error, attempt " + consecutiveErrors);
                        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                            Log.e(TAG, "Too many consecutive errors, stopping audio thread");
                            break;
                        }
                        try { Thread.sleep(50); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else if (read < 0) {
                        Log.e(TAG, "Unrecoverable AudioRecord error: " + read);
                        break;
                    }
                } catch (Exception e) {
                    consecutiveErrors++;
                    Log.e(TAG, "Exception in recording thread (" + consecutiveErrors + ")", e);
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) break;
                    try { Thread.sleep(50); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "AudioEngine-Thread");
        recordingThread.start();
    }

    @SuppressLint("MissingPermission")
    private AudioRecord createRecorder(int source) {
        try {
            AudioRecord record = new AudioRecord(
                    source, SAMPLE_RATE, CHANNEL, ENCODING, BUFFER_SIZE);
            if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                return record;
            }
            record.release();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Audio source unavailable: " + source, e);
        }
        return null;
    }

    public synchronized void stop() {
        isRecording = false;

        AudioRecord record = audioRecord;
        Thread thread = recordingThread;
        audioRecord = null;
        recordingThread = null;

        if (record != null) {
            try {
                switch (record.getRecordingState()) {
                    case AudioRecord.RECORDSTATE_RECORDING:
                        record.stop();
                        break;
                    default:
                        // already stopped or uninitialized
                        break;
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            } finally {
                record.release();
            }
        }

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt(); // FIX: signal thread to wake from sleep() immediately
            try {
                thread.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        currentAmplitude = 0;
        smoothedAmplitude = 0;
        previousRawAmplitude = 0;
        jumpPulseUntilMs = 0L;
        jumpPulseAmplitude = 0;
        lastJumpPulseAtMs = 0L;
    }

    // Raw amplitude — used for jump detection (no smoothing = low latency)
    public int getAmplitude() {
        return currentAmplitude;
    }

    // Smoothed amplitude — used only for voice meter bar display
    public int getSmoothedAmplitude() {
        return smoothedAmplitude;
    }

    public int consumeJumpPulse() {
        long now = SystemClock.uptimeMillis();
        if (now > jumpPulseUntilMs) {
            return 0;
        }
        int amplitude = jumpPulseAmplitude;
        jumpPulseUntilMs = 0L;
        jumpPulseAmplitude = 0;
        return amplitude;
    }

    public synchronized void configureVoiceProfile(int effectiveJumpThreshold, int selectedMode) {
        int jump = clamp(effectiveJumpThreshold, 260, 14000);
        float profileScale = selectedMode == 0 ? 0.74f : selectedMode == 1 ? 0.9f : 1.08f;
        int tuned = clamp((int) (jump * profileScale), 220, 15000);
        instantJumpThreshold = clamp((int) (jump * 0.92f), 180, 15000);
        instantJumpDeltaThreshold = clamp((int) (jump * (selectedMode == 2 ? 0.24f : 0.18f)), 80, 3600);

        silentThreshold = Math.max(95, tuned / 7);
        whisperThreshold = Math.max(silentThreshold + 95, tuned / 3);
        normalThreshold = Math.max(whisperThreshold + 170, (int) (tuned * 0.68f));
        shoutThreshold = Math.max(normalThreshold + 320, (int) (tuned * 1.05f));
        rageThreshold = Math.max(shoutThreshold + 650, (int) (tuned * 1.65f));
    }

    public synchronized void resetSmoothing() {
        currentAmplitude = 0;
        smoothedAmplitude = 0;
        previousRawAmplitude = 0;
        jumpPulseUntilMs = 0L;
        jumpPulseAmplitude = 0;
        lastJumpPulseAtMs = 0L;
    }

    public VoiceLevel getVoiceLevel() {
        int amplitude = currentAmplitude;
        if (amplitude < silentThreshold) return VoiceLevel.SILENT;
        else if (amplitude < whisperThreshold) return VoiceLevel.WHISPER;
        else if (amplitude < normalThreshold) return VoiceLevel.NORMAL;
        else if (amplitude < shoutThreshold) return VoiceLevel.SHOUT;  // FIX: was checking rageThreshold, shoutThreshold was dead code
        else return VoiceLevel.RAGE;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void detectJumpPulse(int amplitude) {
        int previous = previousRawAmplitude;
        previousRawAmplitude = amplitude;
        int delta = amplitude - previous;
        long now = SystemClock.uptimeMillis();
        boolean cleanOnset = amplitude >= instantJumpThreshold
                && delta >= instantJumpDeltaThreshold;
        boolean hardPeak = amplitude >= instantJumpThreshold * 1.32f
                && delta >= instantJumpDeltaThreshold * 0.38f;
        if ((cleanOnset || hardPeak) && now - lastJumpPulseAtMs >= JUMP_PULSE_DEBOUNCE_MS) {
            jumpPulseAmplitude = amplitude;
            jumpPulseUntilMs = now + JUMP_PULSE_LATCH_MS;
            lastJumpPulseAtMs = now;
        }
    }

    private static int getSafeBufferSize() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return SAMPLE_RATE / 10;
        }
        return minBufferSize;
    }

    private static int getLowLatencyReadSize() {
        int min = getSafeBufferSize();
        return clamp(min / 8, 128, 384);
    }
}
