package com.example.data

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class FeedbackType {
    Success,
    Warning,
    Error
}

class FeedbackHelper(private val context: Context) {

    fun play(type: FeedbackType) {
        vibrate(type)
        playTone(type)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(type: FeedbackType) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    FeedbackType.Success -> VibrationEffect.createWaveform(
                        longArrayOf(0, 60, 70, 90),
                        -1
                    )
                    FeedbackType.Warning -> VibrationEffect.createOneShot(110, VibrationEffect.DEFAULT_AMPLITUDE)
                    FeedbackType.Error -> VibrationEffect.createWaveform(
                        longArrayOf(0, 120, 60, 120),
                        -1
                    )
                }
                vibrator.vibrate(effect)
            } else {
                val ms = when (type) {
                    FeedbackType.Success -> 120L
                    FeedbackType.Warning -> 100L
                    FeedbackType.Error -> 220L
                }
                vibrator.vibrate(ms)
            }
        }
    }

    private fun playTone(type: FeedbackType) {
        runCatching {
            val tone = when (type) {
                FeedbackType.Success -> ToneGenerator.TONE_PROP_ACK
                FeedbackType.Warning -> ToneGenerator.TONE_PROP_BEEP
                FeedbackType.Error -> ToneGenerator.TONE_PROP_NACK
            }
            val durationMs = when (type) {
                FeedbackType.Success -> 140
                FeedbackType.Warning -> 120
                FeedbackType.Error -> 200
            }
            val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            generator.startTone(tone, durationMs)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { generator.release() }
            }, (durationMs + 100).toLong())
        }
    }
}
