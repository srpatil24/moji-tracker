package com.dokushotracker.domain.model

import com.dokushotracker.data.model.MediaType

data class AppSettings(
    val defaultMojiCount: Long = 100_000L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultMediaType: MediaType? = null,
    val accentColor: AccentColorOption = AccentColorOption.SAGE,
    val pureBlackDarkMode: Boolean = false,
    val lastCelebratedGoalCreatedAtMillis: Long? = null,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentColorOption(
    val displayName: String,
    val seedColor: Long,
) {
    SAGE("Sage", 0xFF4A6741),
    INDIGO("Indigo", 0xFF3F6BA5),
    EMERALD("Emerald", 0xFF2E7D6F),
    AMBER("Amber", 0xFFA67C2E),
    ROSE("Rose", 0xFFB94A78),
}
