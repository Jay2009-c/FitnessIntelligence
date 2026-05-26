package com.example.fitnessanalyzer.presentation.dashboard

import com.example.fitnessanalyzer.domain.model.WorkoutSession

sealed interface DashboardState {
    object Loading : DashboardState
    data class Success(val sessions: List<WorkoutSession>) : DashboardState
    data class Error(val reason: String) : DashboardState
}
