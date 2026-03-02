package com.dokushotracker.data.repository

import com.dokushotracker.data.local.entity.ReadingEntryEntity
import com.dokushotracker.data.local.entity.ReadingGoalEntity
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.ReadingGoal

fun ReadingEntryEntity.toDomain(): ReadingEntry = ReadingEntry(
    id = id,
    title = title,
    mediaType = mediaType,
    isSeries = isSeries,
    seriesNumber = seriesNumber,
    mojiCount = mojiCount,
    dateFinished = dateFinished,
    dateAdded = dateAdded,
    notes = notes,
)

fun ReadingEntry.toEntity(): ReadingEntryEntity = ReadingEntryEntity(
    id = id,
    title = title,
    mediaType = mediaType,
    isSeries = isSeries,
    seriesNumber = seriesNumber,
    mojiCount = mojiCount,
    dateFinished = dateFinished,
    dateAdded = dateAdded,
    notes = notes,
)

fun ReadingGoalEntity.toDomain(): ReadingGoal = ReadingGoal(
    id = id,
    goalType = goalType,
    targetValue = targetValue,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
    createdAt = createdAt,
)

fun ReadingGoal.toEntity(): ReadingGoalEntity = ReadingGoalEntity(
    id = id,
    goalType = goalType,
    targetValue = targetValue,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
    createdAt = createdAt,
)
