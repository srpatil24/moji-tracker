package com.dokushotracker.util

import java.text.NumberFormat
import java.util.Locale

object NumberFormatUtils {
    fun formatLong(value: Long): String = NumberFormat.getNumberInstance(Locale.getDefault()).format(value)

    fun formatInt(value: Int): String = NumberFormat.getNumberInstance(Locale.getDefault()).format(value)

    fun compact(value: Long): String {
        return when {
            value >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000f)
            value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
            value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000f)
            else -> value.toString()
        }
    }
}
