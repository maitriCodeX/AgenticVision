package com.agentic.vision.speech

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.PriorityQueue

/**
 * Android TextToSpeech Manager with priority queueing, English/Hindi language switching,
 * and vibration haptic alerts for CRITICAL safety events.
 */
class TtsManager(
    private val context: Context? = null
) : TextToSpeech.OnInitListener {

    var isHindiMode: Boolean = false
        set(value) {
            field = value
            setLanguageLocale()
        }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val prioritiser = SpeechPrioritiser()
    private val speechQueue = PriorityQueue<SpeechUtterance>(compareBy { it.priority.value })

    init {
        if (context != null) {
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguageLocale()
        }
    }

    private fun setLanguageLocale() {
        if (!isInitialized || tts == null) return
        val targetLocale = if (isHindiMode) Locale("hi", "IN") else Locale.US
        tts?.language = targetLocale
    }

    fun speak(utterance: SpeechUtterance) {
        val currentTime = System.currentTimeMillis()
        if (!prioritiser.shouldQueueUtterance(utterance.text, utterance.priority, currentTime)) {
            return
        }

        if (utterance.priority == SpeechPriority.CRITICAL) {
            // Flush lower priority items and trigger haptics
            speechQueue.clear()
            triggerVibrationHaptic()
            speakImmediate(utterance, queueMode = TextToSpeech.QUEUE_FLUSH)
        } else {
            speakImmediate(utterance, queueMode = TextToSpeech.QUEUE_ADD)
        }
    }

    private fun speakImmediate(utterance: SpeechUtterance, queueMode: Int) {
        val textToSay = if (isHindiMode && !utterance.textHindi.isNull_or_blank()) {
            utterance.textHindi
        } else {
            utterance.text
        }

        if (isInitialized && tts != null) {
            tts?.speak(textToSay, queueMode, null, "UTTERANCE_${System.currentTimeMillis()}")
        }
    }

    private fun triggerVibrationHaptic() {
        if (context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        v.vibrate(300)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
