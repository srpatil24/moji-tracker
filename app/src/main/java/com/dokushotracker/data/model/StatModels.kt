package com.dokushotracker.data.model

import java.time.LocalDate

data class MediaTypeCount(
    val mediaType: MediaType,
    val count: Int,
)

data class MonthlyMoji(
    val yearMonth: String,
    val totalMoji: Long,
)

data class MonthlyCount(
    val yearMonth: String,
    val count: Int,
)

data class CumulativeMoji(
    val date: LocalDate,
    val cumulativeMoji: Long,
)

data class MediaTypeMoji(
    val mediaType: MediaType,
    val totalMoji: Long,
)
