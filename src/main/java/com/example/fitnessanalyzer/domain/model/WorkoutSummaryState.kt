package com.example.fitnessanalyzer.domain.model

sealed class WorkoutSummaryState {
    object Idle : WorkoutSummaryState()
    object Loading : WorkoutSummaryState()
    data class Success(
        val vo2Max: Float,
        val trimp: Float,
        val isFallback: Boolean,
        val recoveryHours: Int
    ) : WorkoutSummaryState()
    data class Error(val message: String) : WorkoutSummaryState()
}
