package com.dokushotracker.domain.usecase

import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.domain.model.ReadingGoal
import java.time.LocalDate
import javax.inject.Inject

class SetGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend fun setGoal(goal: ReadingGoal): Result<Long> {
        if (goal.targetValue <= 0L) {
            return Result.failure(IllegalArgumentException("Goal target must be positive"))
        }
        if (goal.endDate != null && goal.endDate.isBefore(goal.startDate)) {
            return Result.failure(IllegalArgumentException("Goal end date cannot be before start date"))
        }
        if (goal.startDate.isAfter(LocalDate.now().plusYears(20))) {
            return Result.failure(IllegalArgumentException("Invalid goal start date"))
        }
        return runCatching {
            goalRepository.deactivateAllGoals()
            goalRepository.insertGoal(goal.copy(isActive = true))
        }
    }

    suspend fun clearGoal() {
        goalRepository.deactivateAllGoals()
    }
}
