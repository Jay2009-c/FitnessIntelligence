package com.example.fitnessanalyzer.domain.repository

interface WorkoutRepository {
    suspend fun syncWorkoutsFromCloud(token: String)
}
