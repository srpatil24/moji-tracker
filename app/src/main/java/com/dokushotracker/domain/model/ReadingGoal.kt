package com.dokushotracker.domain.model

import com.dokushotracker.data.model.GoalType
import java.time.Instant
import java.time.LocalDate

data class ReadingGoal(
    val id: Long = 0L,
    val goalType: GoalType,
    val targetValue: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
)
