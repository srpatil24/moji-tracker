package com.dokushotracker.domain.model

data class GoalProgress(
    val goal: ReadingGoal,
    val currentValue: Long,
    val targetValue: Long,
    val progress: Float,
    val daysRemaining: Long?,
    val isEligibleForCelebration: Boolean,
)
