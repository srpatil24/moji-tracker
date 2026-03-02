package com.dokushotracker.data.repository

import com.dokushotracker.domain.model.ReadingGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    suspend fun insertGoal(goal: ReadingGoal): Long
    suspend fun updateGoal(goal: ReadingGoal)
    suspend fun deactivateAllGoals()
    suspend fun clearAllGoals()
    fun getAllGoals(): Flow<List<ReadingGoal>>
    fun getActiveGoal(): Flow<ReadingGoal?>
}
