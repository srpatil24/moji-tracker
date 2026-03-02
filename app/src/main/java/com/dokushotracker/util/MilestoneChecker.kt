package com.dokushotracker.util

import com.dokushotracker.domain.model.Milestone
import com.dokushotracker.domain.model.MilestoneType
import kotlin.math.max
import kotlin.math.roundToLong

object MilestoneChecker {
    private val mojiMilestones = buildMojiMilestones(maxTarget = 100_000_000L)
    private val bookMilestones = buildBookMilestones(maxTarget = 1_000L)

    fun achievedMojiMilestones(totalMoji: Long): List<Milestone> = mojiMilestones.filter { totalMoji >= it.threshold }

    fun achievedBookMilestones(totalBooks: Int): List<Milestone> = bookMilestones.filter { totalBooks >= it.threshold }

    fun nextMojiMilestone(totalMoji: Long): Milestone? = mojiMilestones.firstOrNull { totalMoji < it.threshold }

    fun nextBookMilestone(totalBooks: Int): Milestone? = bookMilestones.firstOrNull { totalBooks < it.threshold }

    private fun buildMojiMilestones(maxTarget: Long): List<Milestone> {
        val milestones = mutableListOf<Milestone>()
        val emojis = listOf("\uD83C\uDF31", "\uD83C\uDF3F", "\uD83C\uDF33", "\u2B50", "\uD83C\uDF1F", "\uD83D\uDCAB", "\uD83C\uDFC6")
        var target = 100_000L
        var index = 0

        while (target <= maxTarget) {
            milestones += Milestone(
                threshold = target,
                label = "${formatMojiTarget(target)} milestone",
                emoji = emojis[minOf(index, emojis.lastIndex)],
                type = MilestoneType.MOJI,
            )
            val rawNext = (target * 1.35 + 35_000).roundToLong()
            val roundedNext = roundMojiTarget(rawNext)
            target = max(target + minMojiGap(target), roundedNext)
            index += 1
        }
        return milestones
    }

    private fun buildBookMilestones(maxTarget: Long): List<Milestone> {
        val milestones = mutableListOf<Milestone>()
        val emojis = listOf("\uD83D\uDCD5", "\uD83D\uDCDA", "\uD83D\uDCD6", "\uD83C\uDFAF", "\uD83D\uDD25", "\uD83D\uDCAF", "\uD83C\uDFC6")
        var target = 1L
        var index = 0

        while (target <= maxTarget) {
            milestones += Milestone(
                threshold = target,
                label = if (target == 1L) "First Book!" else "${NumberFormatUtils.formatLong(target)} books",
                emoji = emojis[minOf(index, emojis.lastIndex)],
                type = MilestoneType.BOOKS,
            )
            val rawNext = (target * 1.32 + 1).roundToLong()
            val roundedNext = roundBookTarget(rawNext)
            target = max(target + 1, roundedNext)
            index += 1
        }
        return milestones
    }

    private fun roundMojiTarget(value: Long): Long {
        val step = when {
            value < 1_000_000L -> 25_000L
            value < 5_000_000L -> 100_000L
            value < 20_000_000L -> 250_000L
            else -> 500_000L
        }
        return ((value + step - 1) / step) * step
    }

    private fun minMojiGap(current: Long): Long = when {
        current < 500_000L -> 50_000L
        current < 2_000_000L -> 100_000L
        current < 10_000_000L -> 250_000L
        else -> 500_000L
    }

    private fun roundBookTarget(value: Long): Long {
        val step = when {
            value < 20L -> 1L
            value < 100L -> 5L
            value < 500L -> 10L
            else -> 25L
        }
        return ((value + step - 1) / step) * step
    }

    private fun formatMojiTarget(value: Long): String {
        return when {
            value >= 1_000_000L -> {
                val millions = value / 1_000_000.0
                val rounded = ((millions * 10).roundToLong() / 10.0)
                "${rounded}M文字"
            }
            else -> "${value / 1_000}K文字"
        }
    }
}
