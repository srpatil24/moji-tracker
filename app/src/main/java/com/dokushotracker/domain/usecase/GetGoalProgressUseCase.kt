package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.GoalProgress
import com.dokushotracker.domain.model.SortOption
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
            readingRepository.getEntries(SortOption.DATE_ASC),
        ) { activeGoal, entries ->
            val goal = activeGoal ?: return@combine null
            val baselineEntries = entries.filter { it.dateAdded.isBefore(goal.createdAt) }

            val currentValue = when (goal.goalType) {
                GoalType.MOJI -> entries.sumOf { it.mojiCount }
                GoalType.BOOKS -> entries.size.toLong()
            }
            val baselineValue = when (goal.goalType) {
                GoalType.MOJI -> baselineEntries.sumOf { it.mojiCount }
                GoalType.BOOKS -> baselineEntries.size.toLong()
            }
            val progress = if (goal.targetValue == 0L) 0f else {
                (currentValue.toFloat() / goal.targetValue.toFloat()).coerceIn(0f, 1f)
            }
            val isEligibleForCelebration = baselineValue < goal.targetValue && currentValue >= goal.targetValue
            val daysRemaining = goal.endDate?.let { endDate ->
                ChronoUnit.DAYS.between(LocalDate.now(), endDate).coerceAtLeast(0)
            }
            GoalProgress(
                goal = goal,
                currentValue = currentValue,
                targetValue = goal.targetValue,
                progress = progress,
                daysRemaining = daysRemaining,
                isEligibleForCelebration = isEligibleForCelebration,
            )
        }
    }
}
