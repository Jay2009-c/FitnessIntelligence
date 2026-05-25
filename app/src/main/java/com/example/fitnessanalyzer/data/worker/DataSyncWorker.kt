package com.example.fitnessanalyzer.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitnessanalyzer.data.local.FitnessDatabase
import com.example.fitnessanalyzer.data.local.UserPreferences
import com.example.fitnessanalyzer.data.repository.WorkoutRepositoryImpl
import com.example.fitnessanalyzer.domain.usecase.CalculateAdvancedMetricsUseCase

class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = FitnessDatabase.getDatabase(applicationContext)
            val userPreferences = UserPreferences(applicationContext)
            val calculateUseCase = CalculateAdvancedMetricsUseCase()
            val repository = WorkoutRepositoryImpl(
                applicationContext,
                database.workoutDao(),
                userPreferences,
                calculateUseCase
            )
            
            repository.runLocalHealthConnectSync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
