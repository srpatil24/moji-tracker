package com.dokushotracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    fun formatDate(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    }

    fun relativeDateText(date: LocalDate): String {
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(date, today)
        return when {
            days < 0 -> formatDate(date)
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days <= 7L -> "$days days ago"
            else -> formatDate(date)
        }
    }
}
