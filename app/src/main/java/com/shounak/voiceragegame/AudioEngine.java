package com.shounak.voiceragegame;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioEngine {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL     = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING    = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL, ENCODING);

    private AudioRecord audioRecord;
    private boolean isRecording           = false;
    private volatile int currentAmplitude = 0;

    public enum VoiceLevel {
        SILENT, WHISPER, NORMAL, SHOUT, RAGE
    }

    public synchronized void start() {
        if (isRecording) return;
        try {
            if (audioRecord == null) {
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, CHANNEL, ENCODING, BUFFER_SIZE);
            }
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return;
            }
            isRecording = true;
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE];
            while (isRecording) {
                AudioRecord record = audioRecord;
                if (record == null || record.getState() != AudioRecord.STATE_INITIALIZED) break;
                int read = record.read(buffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    long sum = 0;
                    for (int i = 0; i < read; i++) {
                        sum += (long) buffer[i] * buffer[i];
                    }
                    currentAmplitude = (int) Math.sqrt((double) sum / read);
                }
            }
        }, "AudioEngine-Thread").start();
    }

    public synchronized void stop() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
    }

    public int getAmplitude() {
        return currentAmplitude;
    }

    public VoiceLevel getVoiceLevel() {
        if      (currentAmplitude < 200)  return VoiceLevel.SILENT;
        else if (currentAmplitude < 900)  return VoiceLevel.WHISPER;
        else if (currentAmplitude < 3000) return VoiceLevel.NORMAL;
        else if (currentAmplitude < 7000) return VoiceLevel.SHOUT;
        else                              return VoiceLevel.RAGE;
    }
}