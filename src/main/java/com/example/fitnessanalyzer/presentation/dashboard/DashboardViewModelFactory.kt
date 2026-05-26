package com.example.fitnessanalyzer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitnessanalyzer.data.local.UserPreferences
import com.example.fitnessanalyzer.data.local.WorkoutDao
import com.example.fitnessanalyzer.data.repository.WorkoutRepositoryImpl
import com.example.fitnessanalyzer.domain.usecase.CalculateAdvancedMetricsUseCase

class DashboardViewModelFactory(
    private val repository: WorkoutRepositoryImpl,
    private val workoutDao: WorkoutDao,
    private val calculateUseCase: CalculateAdvancedMetricsUseCase,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(repository, workoutDao, calculateUseCase, userPreferences) as T
    }
}
