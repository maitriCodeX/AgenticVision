package com.agentic.vision.navigation

import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.perception.DepthMap
import kotlin.math.abs
import kotlin.math.min

/**
 * Free-space & walkable path clearance analyzer splitting the spatial environment into 5 horizontal sectors.
 */
class FreeSpaceAnalyzer {

    fun analyze(
        depthMap: DepthMap,
        sceneObjects: List<SceneObject>
    ): PathAdvice {
        val sectors = Sector.values()
        val sectorClearances = mutableMapOf<Sector, Float>()

        // 1. Compute ground-level clearance per sector from lower-middle depth map region
        val w = depthMap.width
        val h = depthMap.height
        val colPerSector = w / 5

        for (i in sectors.indices) {
            val sec = sectors[i]
            val colStart = i * colPerSector
            val colEnd = if (i == 4) w - 1 else (i + 1) * colPerSector - 1

            val depths = mutableListOf<Float>()
            for (y in (h / 2) until h) {
                for (x in colStart..colEnd) {
                    val d = depthMap.getMetricDepthAt(x, y)
                    if (d in 0.2f..10.0f) {
                        depths.add(d)
                    }
                }
            }

            var minSectorDepth = if (depths.isEmpty()) 5.0f else (depths.minOrNull() ?: 5.0f)

            // Account for 3D detected scene objects in this sector
            for (obj in sceneObjects) {
                val secForObj = getSectorForBearing(obj.bearingDeg)
                if (secForObj == sec && obj.distanceM < minSectorDepth) {
                    minSectorDepth = obj.distanceM
                }
            }

            sectorClearances[sec] = minSectorDepth
        }

        // 2. Detect basic hazards
        val centerClearance = sectorClearanceM(Sector.CENTER, sectorClearances)
        val leftClearance = sectorClearanceM(Sector.LEFT, sectorClearances)
        val rightClearance = sectorClearanceM(Sector.RIGHT, sectorClearances)
        val farLeftClearance = sectorClearanceM(Sector.FAR_LEFT, sectorClearances)
        val farRightClearance = sectorClearanceM(Sector.FAR_RIGHT, sectorClearances)

        var hazard: String? = null
        if (centerClearance < 1.0f) {
            hazard = "Obstacle blocking path ahead"
        } else if (abs(leftClearance - rightClearance) > 2.5f) {
            hazard = "Narrowing passage ahead"
        }

        // 3. Optimal heading recommendation scoring
        // Score = Sector Clearance - turn penalty (0.02 * |angle|)
        val candidateHeadings = listOf(
            0.0f to centerClearance,
            -20.0f to (leftClearance * 0.7f + farLeftClearance * 0.3f),
            20.0f to (rightClearance * 0.7f + farRightClearance * 0.3f),
            -40.0f to farLeftClearance,
            40.0f to farRightClearance
        )

        // Find candidate with maximum clearance score
        val bestHeading = candidateHeadings.maxByOrNull { (angle, clearance) ->
            clearance - 0.02f * abs(angle)
        }?.first ?: 0.0f

        val isClearAhead = centerClearance >= 1.8f

        return PathAdvice(
            recommendedHeadingDeg = bestHeading,
            sectorClearanceM = sectorClearances,
            hazardWarning = hazard,
            isClearAhead = isClearAhead
        )
    }

    private fun getSectorForBearing(bearingDeg: Float): Sector {
        return when {
            bearingDeg < -25.0f -> Sector.FAR_LEFT
            bearingDeg < -8.0f -> Sector.LEFT
            bearingDeg <= 8.0f -> Sector.CENTER
            bearingDeg <= 25.0f -> Sector.RIGHT
            else -> Sector.FAR_RIGHT
        }
    }

    private fun sectorClearanceM(sector: Sector, map: Map<Sector, Float>): Float {
        return map[sector] ?: 3.0f
    }
}
