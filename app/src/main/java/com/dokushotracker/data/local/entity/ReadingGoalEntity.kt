package com.dokushotracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dokushotracker.data.model.GoalType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "reading_goals")
data class ReadingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val goalType: GoalType,
    val targetValue: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
)
