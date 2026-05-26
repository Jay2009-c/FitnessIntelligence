package com.example.fitnessanalyzer.data.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.fitnessanalyzer.data.local.UserPreferences
import com.example.fitnessanalyzer.data.local.WorkoutDao
import com.example.fitnessanalyzer.data.local.WorkoutSessionEntity
import com.example.fitnessanalyzer.data.local.toDomain
import com.example.fitnessanalyzer.data.local.toEntity
import com.example.fitnessanalyzer.domain.model.*
import com.example.fitnessanalyzer.domain.repository.WorkoutRepository
import com.example.fitnessanalyzer.domain.usecase.CalculateAdvancedMetricsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.supervisorScope
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.pow

class WorkoutRepositoryImpl(
    private val context: Context,
    private val workoutDao: WorkoutDao,
    private val userPreferences: UserPreferences,
    private val calculateUseCase: CalculateAdvancedMetricsUseCase
) : WorkoutRepository {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    private val _summaryState = MutableStateFlow<WorkoutSummaryState>(WorkoutSummaryState.Idle)
    val summaryState: StateFlow<WorkoutSummaryState> = _summaryState.asStateFlow()

    override suspend fun syncWorkoutsFromCloud(token: String) {
        // Decommissioned
    }

    private suspend fun getScientificRestingHr(): Double {
        val now = Instant.now()
        val filter = TimeRangeFilter.between(now.minus(30, ChronoUnit.DAYS), now)
        
        return try {
            val response = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter))
            val sleepSamples = response.records.flatMap { it.samples }
                .filter { 
                    val hour = it.time.atZone(ZoneId.systemDefault()).hour
                    hour in 2..6 
                }
                .map { it.beatsPerMinute }
                .sorted()
            
            if (sleepSamples.isNotEmpty()) {
                val index = (sleepSamples.size * 0.01).toInt()
                sleepSamples[index].toDouble().coerceIn(38.0, 80.0)
            } else 60.0
        } catch (e: Exception) {
            60.0
        }
    }

    suspend fun runLocalHealthConnectSync() = supervisorScope {
        try {
            val exerciseRequest = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(90, ChronoUnit.DAYS))
            )
            val exerciseResponse = client.readRecords(exerciseRequest)
            if (exerciseResponse.records.isEmpty()) return@supervisorScope

            val restHr = getScientificRestingHr()

            val entities = exerciseResponse.records.map { workout ->
                async {
                    try {
                        val hrTask = async { client.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(workout.startTime, workout.endTime))) }
                        val agg = client.aggregate(androidx.health.connect.client.request.AggregateRequest(
                            metrics = setOf(
                                DistanceRecord.DISTANCE_TOTAL, 
                                HeartRateRecord.BPM_AVG, 
                                HeartRateRecord.BPM_MAX,
                                TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                                StepsRecord.COUNT_TOTAL,
                                ElevationGainedRecord.ELEVATION_GAINED_TOTAL,
                                PowerRecord.POWER_AVG
                            ),
                            timeRangeFilter = TimeRangeFilter.between(workout.startTime, workout.endTime)
                        ))

                        val hrRes = hrTask.await()
                        val hrSamples = hrRes.records.flatMap { it.samples }

                        val age = userPreferences.getUserProfile().age ?: 30
                        val maxHrCeiling = 220.0 - age
                        
                        val dist = agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                        val calories = agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
                        val steps = agg[StepsRecord.COUNT_TOTAL]
                        val elevation = agg[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters
                        val avgPower = agg[PowerRecord.POWER_AVG]?.inWatts
                        
                        val sessionAvgHr = agg[HeartRateRecord.BPM_AVG]?.toDouble() ?: 130.0
                        val sessionMaxHr = agg[HeartRateRecord.BPM_MAX]?.toDouble() ?: 175.0

                        WorkoutSessionEntity(
                            id = workout.metadata.id,
                            sourcePackage = workout.metadata.dataOrigin.packageName,
                            startTimeMillis = workout.startTime.toEpochMilli(),
                            endTimeMillis = workout.endTime.toEpochMilli(),
                            durationSeconds = Duration.between(workout.startTime, workout.endTime).seconds,
                            workoutType = mapExerciseType(workout.exerciseType),
                            age = age,
                            weightKg = userPreferences.getUserProfile().weightKg,
                            heightCm = userPreferences.getUserProfile().heightCm,
                            isMale = userPreferences.getUserProfile().isMale ?: true,
                            restingHeartRate = restHr.toInt(),
                            estimatedMaxHeartRate = maxHrCeiling.toInt(),
                            averageHeartRate = sessionAvgHr.toFloat(),
                            peakHeartRate = sessionMaxHr.toInt(),
                            caloriesKcal = calories?.toFloat(),
                            distanceMeters = dist.toFloat(),
                            totalSteps = steps?.toInt(),
                            averagePaceMinPerKm = if (dist > 10.0) (Duration.between(workout.startTime, workout.endTime).toMinutes().toDouble() / (dist / 1000.0)).toFloat() else null,
                            bestPaceMinPerKm = null,
                            averageCadence = null,
                            averageStrideLengthMeters = if (steps != null && steps > 0) (dist / steps).toFloat() else null,
                            elevationGainedMeters = elevation?.toFloat(),
                            averagePowerWatts = avgPower?.toFloat(),
                            maxPowerWatts = null,
                            sensorQuality = if (hrSamples.isEmpty()) SensorQuality.POOR else SensorQuality.EXCELLENT,
                            heartRateSamples = hrSamples.map { HeartRateSample(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) },
                            paceSamples = emptyList(),
                            stepSamples = emptyList(),
                            estimatedVo2Max = 0f,
                            uthVo2Max = null,
                            cooperVo2Max = null,
                            acsmVo2Max = null,
                            friendVo2Max = null,
                            trimpScore = 0f,
                            edwardsTrimp = null,
                            luciaTrimp = null,
                            strainScore = 0f,
                            estimatedRecoveryHours = 0,
                            heartRateRecovery = null,
                            readinessScore = 100f,
                            aerobicEfficiency = null,
                            heartRateDriftPercent = null,
                            aerobicLoad = null,
                            anaerobicLoad = null,
                            trainingEffectLabel = if (hrSamples.isEmpty()) "Summary" else "Series",
                            hrZoneDistribution = null,
                            paceZoneDistribution = null,
                            confidenceScore = 1.0f,
                            hasMissingHeartRateData = hrSamples.isEmpty(),
                            hasGpsDropouts = false,
                            containsOutlierData = false,
                            notes = "Imported"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            if (entities.isNotEmpty()) {
                val analyzed = calculateUseCase.execute(entities.map { it.toDomain() }).map { it.toEntity() }
                workoutDao.insertWorkouts(analyzed)
            }
        } catch (e: Exception) {
            Log.e("Sync", "Sync error", e)
        }
    }

    private fun mapExerciseType(type: Int): WorkoutType {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> WorkoutType.WALKING
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> WorkoutType.RUNNING
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> WorkoutType.CYCLING
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> WorkoutType.HIIT
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> WorkoutType.STRENGTH
            else -> WorkoutType.OTHER
        }
    }
}
