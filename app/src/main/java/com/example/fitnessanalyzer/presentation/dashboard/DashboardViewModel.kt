package com.example.fitnessanalyzer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessanalyzer.data.local.UserPreferences
import com.example.fitnessanalyzer.data.local.WorkoutDao
import com.example.fitnessanalyzer.data.local.toDomain
import com.example.fitnessanalyzer.data.repository.WorkoutRepositoryImpl
import com.example.fitnessanalyzer.domain.model.UserProfile
import com.example.fitnessanalyzer.domain.usecase.CalculateAdvancedMetricsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: WorkoutRepositoryImpl,
    workoutDao: WorkoutDao,
    private val calculateUseCase: CalculateAdvancedMetricsUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val viewState: StateFlow<DashboardState> = workoutDao.readLocalDatabaseStream()
        .map { records ->
            val domainSessions = records.map { it.toDomain() }
            val analyzedList = calculateUseCase.execute(domainSessions)
            DashboardState.Success(analyzedList) as DashboardState
        }
        .catch { err -> 
            err.printStackTrace()
            emit(DashboardState.Error(err.message ?: "Critical SQLite stream capture error")) 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState.Loading)

    fun triggerLocalSyncPipeline() {
        viewModelScope.launch {
            try {
                repository.runLocalHealthConnectSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        userPreferences.saveUserProfile(profile)
        triggerLocalSyncPipeline()
    }

    fun getUserProfile(): UserProfile = userPreferences.getUserProfile()
}
