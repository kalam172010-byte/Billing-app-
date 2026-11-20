package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundFeedback {
    fun playScanBeep() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        } catch (_: Exception) {
            // Audio device unavailable on headless emulator
        }
    }

    fun playCheckoutSuccess() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        } catch (_: Exception) {
            // Audio device unavailable
        }
    }
}
