package com.dokushotracker.domain.model

import com.dokushotracker.data.model.MediaType
import java.time.Instant
import java.time.LocalDate

data class ReadingEntry(
    val id: Long = 0L,
    val title: String,
    val mediaType: MediaType,
    val isSeries: Boolean,
    val seriesNumber: Int?,
    val mojiCount: Long,
    val dateFinished: LocalDate,
    val dateAdded: Instant = Instant.now(),
    val notes: String?,
)
