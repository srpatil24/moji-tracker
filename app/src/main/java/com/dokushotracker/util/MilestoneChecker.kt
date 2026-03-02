package com.dokushotracker.util

import com.dokushotracker.domain.model.Milestone
import com.dokushotracker.domain.model.MilestoneType

object MilestoneChecker {
    private val mojiMilestones = listOf(
        Milestone(100_000, "100K Moji!", "\uD83C\uDF31", MilestoneType.MOJI),
        Milestone(250_000, "250K Moji!", "\uD83C\uDF3F", MilestoneType.MOJI),
        Milestone(500_000, "500K Moji!", "\uD83C\uDF33", MilestoneType.MOJI),
        Milestone(1_000_000, "1 Million Moji!", "\u2B50", MilestoneType.MOJI),
        Milestone(2_000_000, "2 Million Moji!", "\uD83C\uDF1F", MilestoneType.MOJI),
        Milestone(5_000_000, "5 Million Moji!", "\uD83D\uDCAB", MilestoneType.MOJI),
        Milestone(10_000_000, "10 Million Moji!", "\uD83C\uDFC6", MilestoneType.MOJI),
    )

    private val bookMilestones = listOf(
        Milestone(1, "First Book!", "\uD83D\uDCD5", MilestoneType.BOOKS),
        Milestone(5, "5 Books Read", "\uD83D\uDCDA", MilestoneType.BOOKS),
        Milestone(10, "10 Books Read", "\uD83D\uDCD6", MilestoneType.BOOKS),
        Milestone(25, "25 Books Read", "\uD83C\uDFAF", MilestoneType.BOOKS),
        Milestone(50, "50 Books Read", "\uD83D\uDD25", MilestoneType.BOOKS),
        Milestone(100, "Century Reader", "\uD83D\uDCAF", MilestoneType.BOOKS),
    )

    fun achievedMojiMilestones(totalMoji: Long): List<Milestone> = mojiMilestones.filter { totalMoji >= it.threshold }

    fun achievedBookMilestones(totalBooks: Int): List<Milestone> = bookMilestones.filter { totalBooks >= it.threshold }

    fun nextMojiMilestone(totalMoji: Long): Milestone? = mojiMilestones.firstOrNull { totalMoji < it.threshold }

    fun nextBookMilestone(totalBooks: Int): Milestone? = bookMilestones.firstOrNull { totalBooks < it.threshold }
}
