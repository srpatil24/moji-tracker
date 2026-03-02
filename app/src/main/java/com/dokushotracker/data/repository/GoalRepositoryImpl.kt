package com.dokushotracker.data.repository

import com.dokushotracker.data.local.dao.ReadingGoalDao
import com.dokushotracker.domain.model.ReadingGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val readingGoalDao: ReadingGoalDao,
) : GoalRepository {
    override suspend fun insertGoal(goal: ReadingGoal): Long {
        return readingGoalDao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: ReadingGoal) {
        readingGoalDao.updateGoal(goal.toEntity())
    }

    override suspend fun deactivateAllGoals() {
        readingGoalDao.deactivateAllGoals()
    }

    override suspend fun clearAllGoals() {
        readingGoalDao.clearAllGoals()
    }

    override fun getAllGoals(): Flow<List<ReadingGoal>> {
        return readingGoalDao.getAllGoals().map { goals -> goals.map { it.toDomain() } }
    }

    override fun getActiveGoal(): Flow<ReadingGoal?> {
        return readingGoalDao.getActiveGoal().map { it?.toDomain() }
    }
}
