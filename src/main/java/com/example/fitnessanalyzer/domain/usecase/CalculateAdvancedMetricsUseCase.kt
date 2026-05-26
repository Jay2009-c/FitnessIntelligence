package com.example.fitnessanalyzer.domain.usecase

import com.example.fitnessanalyzer.domain.model.*
import kotlin.math.exp
import kotlin.math.pow

class CalculateAdvancedMetricsUseCase {

    fun execute(sessions: List<WorkoutSession>): List<WorkoutSession> {
        return sessions.map { session ->
            calculateMetricsForSession(session)
        }
    }

    private fun calculateMetricsForSession(session: WorkoutSession): WorkoutSession {
        // --- 1. Physiological Baselines (Medical Grade) ---
        val sessionMinHr = session.heartRateSamples.map { it.bpm }.minOrNull()?.toDouble() ?: 60.0
        val restHr = session.restingHeartRate?.toDouble()?.coerceIn(35.0, 85.0) ?: sessionMinHr.coerceIn(40.0, 75.0)
        
        val age = session.age ?: 30
        val ageMaxHr = 220.0 - age
        val sessionMaxHr = session.peakHeartRate?.toDouble() ?: ageMaxHr
        val maxHr = if (sessionMaxHr > 100) sessionMaxHr else ageMaxHr

        // --- 2. Volume & Mechanical Metrics ---
        val totalDurationSec = session.durationSeconds.coerceAtLeast(60)
        val totalDistance = session.distanceMeters ?: 0f
        val totalSteps = session.totalSteps ?: 0
        
        val activeSamples = session.heartRateSamples.filter { it.bpm > restHr + 10 }
        val movingSec = if (activeSamples.size > 5) (activeSamples.size * 5L).coerceAtMost(totalDurationSec) else totalDurationSec
        
        val speedMps = totalDistance / movingSec
        val speedMpm = speedMps * 60.0
        val cadence = if (movingSec > 0) totalSteps / (movingSec / 60.0) else 0.0
        val strideLength = if (totalSteps > 0) totalDistance / totalSteps else 0.0

        val isRunning = (session.workoutType == WorkoutType.RUNNING || session.workoutType == WorkoutType.TREADMILL)

        // --- 3. Scientific VO2 MAX Comparative Analysis ---
        val uthVo2 = (15.3 * (maxHr / restHr)).coerceIn(10.0, 95.0)
        val d12 = (totalDistance / (totalDurationSec / 720.0))
        val cooperVo2 = if (isRunning && totalDurationSec > 300) ((d12 - 504.9) / 44.73).coerceIn(10.0, 95.0) else null
        val acsmVo2 = if (isRunning) (3.5 + 0.2 * speedMpm).coerceIn(10.0, 95.0) else (3.5 + 0.1 * speedMpm).coerceIn(10.0, 95.0)
        
        val avgHr = session.averageHeartRate?.toDouble() ?: (restHr + 30.0)
        val hrrUsed = (avgHr - restHr).coerceAtLeast(5.0)
        val hrrTotal = (maxHr - restHr).coerceAtLeast(50.0)
        val fractionalEffort = (hrrUsed / hrrTotal).coerceIn(0.1, 0.98)
        val friendMacroVo2 = (speedMpm * 0.17) + 3.5
        val friendVo2 = ((friendMacroVo2 - 3.5) / fractionalEffort + 3.5).coerceIn(10.0, 95.0)

        val finalVo2Max = friendVo2

        // --- 4. Training Load (TRIMP) Models ---
        val intensityHrr = (avgHr - restHr) / hrrTotal.coerceAtLeast(1.0)
        val banisterTrimp = (totalDurationSec / 60.0) * intensityHrr * (if (session.isMale == false) 0.86 else 0.64) * exp((if (session.isMale == false) 1.67 else 1.92) * intensityHrr)
        
        val zones = calculateHrZones(session.heartRateSamples, maxHr, restHr, totalDurationSec)
        val edwardsTrimp = (zones.zone1RecoverySec / 60.0 * 1) + (zones.zone2AerobicSec / 60.0 * 2) + (zones.zone3TempoSec / 60.0 * 3) + (zones.zone4ThresholdSec / 60.0 * 4) + (zones.zone5AnaerobicSec / 60.0 * 5)
        val luciaTrimp = (zones.zone1RecoverySec / 60.0 * 1) + ((zones.zone2AerobicSec + zones.zone3TempoSec) / 60.0 * 2) + ((zones.zone4ThresholdSec + zones.zone5AnaerobicSec) / 60.0 * 3)

        // --- 5. Recovery Optimization ---
        val recoveryHours = (banisterTrimp / 10.0).pow(1.03).toInt().plus(intensityHrr * 6).toInt().coerceIn(2, 96)

        return session.copy(
            estimatedVo2Max = finalVo2Max.toFloat(),
            uthVo2Max = uthVo2.toFloat(),
            cooperVo2Max = cooperVo2?.toFloat(),
            acsmVo2Max = acsmVo2.toFloat(),
            friendVo2Max = friendVo2.toFloat(),
            trimpScore = banisterTrimp.toFloat(),
            edwardsTrimp = edwardsTrimp.toFloat(),
            luciaTrimp = luciaTrimp.toFloat(),
            strainScore = (banisterTrimp / 10.0).toFloat(),
            estimatedRecoveryHours = recoveryHours,
            heartRateRecovery = calculateHeartRateRecovery(session),
            readinessScore = calculateReadiness(banisterTrimp, restHr),
            hrZoneDistribution = zones,
            aerobicLoad = (zones.zone2AerobicSec + zones.zone3TempoSec) / 60f,
            anaerobicLoad = (zones.zone4ThresholdSec + zones.zone5AnaerobicSec * 1.5f) / 60f,
            trainingEffectLabel = determineTrainingEffect(banisterTrimp, totalDurationSec),
            averageCadence = cadence.toFloat(),
            averageStrideLengthMeters = strideLength.toFloat(),
            averagePaceMinPerKm = if (speedMps > 0) (1000.0 / speedMps / 60.0).toFloat() else 0f,
            heartRateDriftPercent = calculateAerobicDecoupling(session)?.toFloat()
        )
    }

    private fun calculateHeartRateRecovery(session: WorkoutSession): Int? {
        val samples = session.heartRateSamples
        if (samples.isEmpty()) return null
        val peakHr = samples.maxOf { it.bpm }
        val peakTimestamp = samples.first { it.bpm == peakHr }.timestampMillis
        val recoverySample = samples.filter { it.timestampMillis >= peakTimestamp + 60000 }.minByOrNull { it.timestampMillis }
        return recoverySample?.let { peakHr - it.bpm }
    }

    private fun calculateAerobicDecoupling(session: WorkoutSession): Double? {
        val samples = session.heartRateSamples
        if (samples.size < 10) return null 
        val midpoint = samples.size / 2
        val avgHr1 = samples.subList(0, midpoint).map { it.bpm }.average()
        val avgHr2 = samples.subList(midpoint, samples.size).map { it.bpm }.average()
        return if (avgHr1 > 0) ((avgHr2 - avgHr1) / avgHr1) * 100.0 else 0.0
    }

    private fun determineTrainingEffect(trimp: Double, durationSec: Long): String {
        val intensity = trimp / (durationSec / 60.0)
        return when {
            intensity < 0.4 -> "Active Recovery"
            intensity < 1.0 -> "Aerobic Base"
            intensity < 1.8 -> "Tempo / Quality"
            else -> "Threshold Effort"
        }
    }

    private fun calculateHrZones(samples: List<HeartRateSample>, maxHr: Double, restHr: Double, totalSec: Long): HrZoneDistribution {
        val hrr = maxHr - restHr
        if (hrr <= 10.0 || samples.isEmpty()) return HrZoneDistribution((totalSec * 0.1).toInt(), (totalSec * 0.4).toInt(), (totalSec * 0.3).toInt(), (totalSec * 0.15).toInt(), (totalSec * 0.05).toInt())
        var z1 = 0; var z2 = 0; var z3 = 0; var z4 = 0; var z5 = 0
        samples.sortedBy { it.timestampMillis }.zipWithNext { a, b ->
            val duration = (b.timestampMillis - a.timestampMillis) / 1000
            val intensity = (a.bpm - restHr) / hrr
            when {
                intensity < 0.6 -> z1 += duration.toInt()
                intensity < 0.7 -> z2 += duration.toInt()
                intensity < 0.8 -> z3 += duration.toInt()
                intensity < 0.9 -> z4 += duration.toInt()
                else -> z5 += duration.toInt()
            }
        }
        return HrZoneDistribution(z1, z2, z3, z4, z5)
    }

    private fun calculateReadiness(trimp: Double, restHr: Double): Float {
        val hrFactor = (60.0 / restHr).coerceIn(0.5, 1.5)
        val effortFactor = (1.0 - (trimp / 500.0)).coerceIn(0.0, 1.0)
        return (hrFactor * effortFactor * 100.0).toFloat().coerceIn(0f, 100f)
    }
}
