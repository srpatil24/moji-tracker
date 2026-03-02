package com.dokushotracker.domain.model

data class Milestone(
    val threshold: Long,
    val label: String,
    val emoji: String,
    val type: MilestoneType,
)

enum class MilestoneType {
    MOJI,
    BOOKS,
}
