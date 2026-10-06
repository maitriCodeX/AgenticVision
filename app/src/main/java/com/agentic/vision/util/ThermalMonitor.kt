package com.agentic.vision.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

enum class ThermalState {
    NOMINAL,
    MODERATE,
    SEVERE,
    CRITICAL
}

/**
 * Monitors hardware thermal status and adjusts perception frame rate to mitigate thermal throttling.
 */
class ThermalMonitor(
    private val context: Context? = null
) {
    var currentState: ThermalState = ThermalState.NOMINAL
        private set

    fun checkThermalStatus(): ThermalState {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ThermalState.NOMINAL
        }

        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val status = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE

            currentState = when (status) {
                PowerManager.THERMAL_STATUS_LIGHT, PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL, PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                else -> ThermalState.NOMINAL
            }
        } catch (e: Exception) {
            currentState = ThermalState.NOMINAL
        }

        return currentState
    }

    fun getRecommendedFrameIntervalMs(): Long {
        return when (currentState) {
            ThermalState.NOMINAL -> 66L  // ~15 FPS
            ThermalState.MODERATE -> 100L // ~10 FPS
            ThermalState.SEVERE -> 200L  // ~5 FPS
            ThermalState.CRITICAL -> 500L // ~2 FPS
        }
    }
}
