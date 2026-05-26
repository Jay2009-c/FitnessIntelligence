package com.example.fitnessanalyzer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutSessionEntity>)

    @Query("SELECT * FROM local_workouts ORDER BY startTimeMillis DESC")
    fun readLocalDatabaseStream(): Flow<List<WorkoutSessionEntity>>
}
