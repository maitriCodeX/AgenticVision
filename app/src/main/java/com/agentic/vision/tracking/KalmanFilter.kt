package com.agentic.vision.tracking

import kotlin.math.max

/**
 * 2D Constant-Velocity Kalman Filter for tracking spatial object positions on ground plane [x, z, vx, vz].
 * Uses full F * P * F^T covariance propagation and quadratic measurement noise scaling: R(z) ~ z^2.
 */
class KalmanFilter(
    initialX: Float,
    initialZ: Float,
    initialTimestampMs: Long
) {
    // State vector [x, z, vx, vz]
    var state = floatArrayOf(initialX, initialZ, 0.0f, 0.0f)
        private set

    // State covariance matrix (4x4)
    private var P = arrayOf(
        floatArrayOf(0.5f, 0.0f, 0.0f, 0.0f),
        floatArrayOf(0.0f, 0.5f, 0.0f, 0.0f),
        floatArrayOf(0.0f, 0.0f, 5.0f, 0.0f),
        floatArrayOf(0.0f, 0.0f, 0.0f, 5.0f)
    )

    private var lastTimestampMs: Long = initialTimestampMs

    /**
     * Predict state ahead by dt seconds using constant velocity model:
     * x_new = x + vx * dt
     * z_new = z + vz * dt
     */
    fun predict(currentTimestampMs: Long) {
        val dt = max(0.01f, (currentTimestampMs - lastTimestampMs) / 1000.0f)
        lastTimestampMs = currentTimestampMs

        // 1. State prediction
        state[0] += state[2] * dt
        state[1] += state[3] * dt

        // 2. Covariance matrix propagation: P_new = F * P * F^T + Q
        val qPos = 0.02f * dt
        val qVel = 0.20f * dt

        // X channel covariance
        P[0][0] += dt * (P[2][0] + P[0][2]) + dt * dt * P[2][2] + qPos
        P[0][2] += dt * P[2][2]
        P[2][0] += dt * P[2][2]
        P[2][2] += qVel

        // Z channel covariance
        P[1][1] += dt * (P[3][1] + P[1][3]) + dt * dt * P[3][3] + qPos
        P[1][3] += dt * P[3][3]
        P[3][1] += dt * P[3][3]
        P[3][3] += qVel
    }

    /**
     * Correct state using new 2D measurement [measX, measZ].
     * Measurement noise variance R scales quadratically with distance Z: R = r0 * (1 + (Z/3)^2).
     */
    fun update(measX: Float, measZ: Float) {
        val distance = max(0.5f, measZ)
        val rVar = 0.05f * (1.0f + (distance / 3.0f) * (distance / 3.0f))

        // Innovation y = z_meas - H*x
        val yX = measX - state[0]
        val yZ = measZ - state[1]

        // Innovation covariance S = H*P*H^T + R
        val sX = max(1e-5f, P[0][0] + rVar)
        val sZ = max(1e-5f, P[1][1] + rVar)

        // Kalman gains
        val k00 = P[0][0] / sX
        val k20 = P[2][0] / sX

        val k11 = P[1][1] / sZ
        val k31 = P[3][1] / sZ

        // State update
        state[0] += k00 * yX
        state[2] += k20 * yX

        state[1] += k11 * yZ
        state[3] += k31 * yZ

        // Covariance update: P = (I - K*H)*P
        val p00Old = P[0][0]
        val p02Old = P[0][2]
        P[0][0] = (1.0f - k00) * p00Old
        P[0][2] = (1.0f - k00) * p02Old
        P[2][0] -= k20 * p00Old
        P[2][2] -= k20 * p02Old

        val p11Old = P[1][1]
        val p13Old = P[1][3]
        P[1][1] = (1.0f - k11) * p11Old
        P[1][3] = (1.0f - k11) * p13Old
        P[3][1] -= k31 * p11Old
        P[3][3] -= k31 * p13Old
    }

    val posX: Float get() = state[0]
    val posZ: Float get() = state[1]
    val velVx: Float get() = state[2]
    val velVz: Float get() = state[3]
}
