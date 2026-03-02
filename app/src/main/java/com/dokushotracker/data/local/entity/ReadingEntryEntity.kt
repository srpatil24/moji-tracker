package com.dokushotracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dokushotracker.data.model.MediaType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "reading_entries",
    indices = [
        Index(value = ["title", "seriesNumber", "mediaType"], unique = true),
    ],
)
data class ReadingEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val mediaType: MediaType,
    val isSeries: Boolean = false,
    val seriesNumber: Int?,
    val mojiCount: Long,
    val dateFinished: LocalDate,
    val dateAdded: Instant = Instant.now(),
    val notes: String?,
)
