package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.GoalProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetGoalProgressUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val readingRepository: ReadingRepository,
) {
    operator fun invoke(): Flow<GoalProgress?> {
        return combine(
            goalRepository.getActiveGoal(),
            readingRepository.getTotalMojiCount(),
            readingRepository.getTotalEntryCount(),
        ) { activeGoal, totalMoji, totalBooks ->
            val goal = activeGoal ?: return@combine null
            val currentValue = when (goal.goalType) {
                GoalType.MOJI -> totalMoji
                GoalType.BOOKS -> totalBooks.toLong()
            }
            val progress = if (goal.targetValue == 0L) 0f else {
                (currentValue.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)
            }
            val daysRemaining = goal.endDate?.let { endDate ->
                ChronoUnit.DAYS.between(LocalDate.now(), endDate).coerceAtLeast(0)
            }
            GoalProgress(
                goal = goal,
                currentValue = currentValue,
                targetValue = goal.targetValue,
                progress = progress,
                daysRemaining = daysRemaining,
            )
        }
    }
}
