package com.example.fitnessanalyzer.domain.model

enum class WorkoutType {
    WALKING,
    RUNNING,
    CYCLING,
    HIIT,
    STRENGTH,
    TREADMILL,
    OTHER
}

enum class SensorQuality {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
    UNKNOWN
}

data class HeartRateSample(
    val timestampMillis: Long,
    val bpm: Int,
    val confidence: Float = 1f
)

data class PaceSample(
    val timestampMillis: Long,

    /**
     * Minutes per kilometer.
     * Example:
     * 5.0 = 5:00/km
     */
    val paceMinPerKm: Float
)

data class StepSample(
    val timestampMillis: Long,
    val steps: Int
)

data class HrZoneDistribution(
    val zone1RecoverySec: Int = 0,
    val zone2AerobicSec: Int = 0,
    val zone3TempoSec: Int = 0,
    val zone4ThresholdSec: Int = 0,
    val zone5AnaerobicSec: Int = 0
)

data class PaceZoneDistribution(
    val recoverySec: Int = 0,
    val easySec: Int = 0,
    val moderateSec: Int = 0,
    val tempoSec: Int = 0,
    val thresholdSec: Int = 0,
    val sprintSec: Int = 0
)

data class WorkoutSession(

    /**
     * Unique session ID.
     */
    val sessionId: String,

    /**
     * Health Connect package name.
     * Example:
     * com.xiaomi.wearable
     */
    val sourcePackage: String? = null,

    /**
     * Session timing.
     */
    val startTimeMillis: Long,
    val endTimeMillis: Long,

    /**
     * Duration in seconds.
     */
    val durationSeconds: Long,

    /**
     * Workout category.
     */
    val workoutType: WorkoutType,

    /**
     * User physiological profile.
     */
    val age: Int? = null,
    val weightKg: Float? = null,
    val heightCm: Float? = null,
    val isMale: Boolean? = null,

    /**
     * Baseline HR values.
     */
    val restingHeartRate: Int? = null,

    /**
     * User max HR estimate.
     * Prefer estimated physiological max HR
     * instead of session peak HR.
     */
    val estimatedMaxHeartRate: Int? = null,

    /**
     * Session HR statistics.
     */
    val averageHeartRate: Float? = null,
    val peakHeartRate: Int? = null,

    /**
     * Calories from wearable/app.
     */
    val caloriesKcal: Float? = null,

    /**
     * Distance from GPS/IMU fusion.
     */
    val distanceMeters: Float? = null,

    /**
     * Activity data.
     */
    val totalSteps: Int? = null,

    /**
     * Physical Dynamics
     */
    val averagePaceMinPerKm: Float? = null,
    val bestPaceMinPerKm: Float? = null,
    val averageCadence: Float? = null,
    val averageStrideLengthMeters: Float? = null,
    val elevationGainedMeters: Float? = null,
    val averagePowerWatts: Float? = null,
    val maxPowerWatts: Float? = null,

    /**
     * Sensor quality estimation.
     */
    val sensorQuality: SensorQuality = SensorQuality.UNKNOWN,

    /**
     * Raw/smoothed samples.
     */
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    val paceSamples: List<PaceSample> = emptyList(),
    val stepSamples: List<StepSample> = emptyList(),

    /**
     * ---------- ADVANCED DERIVED METRICS ----------
     * These are cached calculated metrics.
     * They should usually be computed AFTER
     * session import/processing.
     */

    /**
     * VO2 max estimation variants.
     */
    val estimatedVo2Max: Float? = null,
    val cooperVo2Max: Float? = null,
    val uthVo2Max: Float? = null,
    val acsmVo2Max: Float? = null,
    val friendVo2Max: Float? = null,

    /**
     * Training load metrics variants.
     */
    val trimpScore: Float? = null,
    val edwardsTrimp: Float? = null,
    val luciaTrimp: Float? = null,
    val strainScore: Float? = null,

    /**
     * Recovery analytics.
     */
    val estimatedRecoveryHours: Int? = null,
    val heartRateRecovery: Int? = null, // 1-min drop
    val readinessScore: Float? = null,

    /**
     * Workout efficiency metrics.
     */
    val aerobicEfficiency: Float? = null,

    /**
     * HR drift / aerobic decoupling.
     * Percentage increase in HR relative
     * to maintained pace.
     */
    val heartRateDriftPercent: Float? = null,

    /**
     * Aerobic vs anaerobic load.
     */
    val aerobicLoad: Float? = null,
    val anaerobicLoad: Float? = null,

    /**
     * Training effect.
     * Example:
     * Recovery
     * Base
     * Tempo
     * Threshold
     * VO2
     */
    val trainingEffectLabel: String? = null,

    /**
     * Zone distributions.
     */
    val hrZoneDistribution: HrZoneDistribution? = null,
    val paceZoneDistribution: PaceZoneDistribution? = null,

    /**
     * Confidence score for overall session quality.
     */
    val confidenceScore: Float = 1f,

    /**
     * Flags.
     */
    val hasMissingHeartRateData: Boolean = false,
    val hasGpsDropouts: Boolean = false,
    val containsOutlierData: Boolean = false,

    /**
     * Optional notes/debugging.
     */
    val notes: String? = null
)
