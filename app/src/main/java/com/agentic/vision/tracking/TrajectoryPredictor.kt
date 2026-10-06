package com.agentic.vision.tracking

import com.agentic.vision.fusion.ApproachState
import com.agentic.vision.fusion.RiskLevel
import kotlin.math.abs
import kotlin.math.hypot

data class CollisionRiskResult(
    val approachState: ApproachState,
    val speedMps: Float,
    val ttcSeconds: Float?,
    val cpaMeters: Float,
    val riskLevel: RiskLevel
)

/**
 * Computes Closest Point of Approach (CPA), Time-to-Collision (TTC), approach state,
 * and multi-stage risk levels against a 0.8m safety corridor.
 */
class TrajectoryPredictor {

    private val corridorHalfWidthM: Float = 0.4f // 0.8m total corridor width

    fun analyzeTrajectory(
        posX: Float,
        posZ: Float,
        vx: Float,
        vz: Float
    ): CollisionRiskResult {
        val speed = hypot(vx.toDouble(), vz.toDouble()).toFloat()

        // 1. Approach State Classification
        val approachState = when {
            speed < 0.3f -> ApproachState.STATIC
            vz < -0.3f -> ApproachState.APPROACHING
            vz > 0.3f -> ApproachState.RECEDING
            abs(vx) >= 0.4f -> ApproachState.CROSSING
            else -> ApproachState.STATIC
        }

        // 2. Trajectory prediction 0.1 to 3.0 seconds (for dynamic moving objects)
        var minCorridorDist = Float.MAX_VALUE
        var ttc: Float? = null

        if (approachState != ApproachState.STATIC) {
            val dtStep = 0.1f
            var t = 0.1f
            while (t <= 3.0f) {
                val futureX = posX + vx * t
                val futureZ = posZ + vz * t

                if (futureZ in 0.0f..10.0f) {
                    val lateralDist = abs(futureX)
                    if (lateralDist < corridorHalfWidthM && ttc == null) {
                        ttc = t
                    }
                    if (lateralDist < minCorridorDist) {
                        minCorridorDist = lateralDist
                    }
                }
                t += dtStep
            }
        }

        val cpa = if (minCorridorDist == Float.MAX_VALUE) abs(posX) else minCorridorDist

        // 3. Risk Level Determination
        val riskLevel = when {
            // CRITICAL: Imminent collision < 1.5s or static obstacle < 1.0m directly in corridor
            (ttc != null && ttc < 1.5f && posZ < 5.0f) || (approachState == ApproachState.STATIC && abs(posX) < corridorHalfWidthM && posZ < 1.0f) -> RiskLevel.CRITICAL

            // WARNING: Collision < 2.5s or static obstacle < 2.0m in corridor
            (ttc != null && ttc < 2.5f) || (approachState == ApproachState.STATIC && abs(posX) < corridorHalfWidthM && posZ < 2.0f) -> RiskLevel.WARNING

            // CAUTION: TTC < 4.0s or obstacle < 3.5m nearby
            (ttc != null && ttc < 4.0f) || (posZ < 3.5f && abs(posX) < 1.2f) -> RiskLevel.CAUTION

            else -> RiskLevel.SAFE
        }

        return CollisionRiskResult(
            approachState = approachState,
            speedMps = speed,
            ttcSeconds = ttc,
            cpaMeters = cpa,
            riskLevel = riskLevel
        )
    }
}
