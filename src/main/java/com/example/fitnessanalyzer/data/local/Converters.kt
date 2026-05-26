package com.example.fitnessanalyzer.data.local

import androidx.room.TypeConverter
import com.example.fitnessanalyzer.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromHeartRateSamples(value: List<HeartRateSample>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toHeartRateSamples(value: String?): List<HeartRateSample>? {
        val listType = object : TypeToken<List<HeartRateSample>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromPaceSamples(value: List<PaceSample>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPaceSamples(value: String?): List<PaceSample>? {
        val listType = object : TypeToken<List<PaceSample>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStepSamples(value: List<StepSample>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStepSamples(value: String?): List<StepSample>? {
        val listType = object : TypeToken<List<StepSample>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromHrZoneDistribution(value: HrZoneDistribution?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toHrZoneDistribution(value: String?): HrZoneDistribution? {
        return gson.fromJson(value, HrZoneDistribution::class.java)
    }

    @TypeConverter
    fun fromPaceZoneDistribution(value: PaceZoneDistribution?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPaceZoneDistribution(value: String?): PaceZoneDistribution? {
        return gson.fromJson(value, PaceZoneDistribution::class.java)
    }

    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType {
        return WorkoutType.valueOf(value)
    }

    @TypeConverter
    fun fromSensorQuality(value: SensorQuality): String {
        return value.name
    }

    @TypeConverter
    fun toSensorQuality(value: String): SensorQuality {
        return SensorQuality.valueOf(value)
    }
}
