package com.agentic.vision.speech

enum class SpeechPriority(val value: Int) {
    CRITICAL(1),
    WARNING(2),
    GUIDANCE(3),
    INFO(4)
}

data class SpeechUtterance(
    val priority: SpeechPriority,
    val text: String,
    val textHindi: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Priority Queue & Anti-Spam Speech Dispatcher.
 * Prevents repetitive audio spam while guaranteeing immediate CRITICAL safety interrupts.
 */
class SpeechPrioritiser {

    private val cooldowns = mutableMapOf<String, Long>()
    private val minCooldownMs: Long = 2500L // 2.5 second cooldown per message key

    fun shouldQueueUtterance(text: String, priority: SpeechPriority, currentTimeMs: Long): Boolean {
        if (priority == SpeechPriority.CRITICAL) {
            cooldowns[text] = currentTimeMs
            return true
        }

        val lastTime = cooldowns[text]
        if (lastTime == null || (currentTimeMs - lastTime > minCooldownMs)) {
            cooldowns[text] = currentTimeMs
            return true
        }

        return false
    }

    fun clearCooldowns() {
        cooldowns.clear()
    }
}
