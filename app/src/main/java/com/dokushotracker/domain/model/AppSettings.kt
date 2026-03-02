package com.dokushotracker.domain.model

import com.dokushotracker.data.model.MediaType

data class AppSettings(
    val defaultMojiCount: Long = 100_000L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultMediaType: MediaType? = null,
    val language: UiLanguage = UiLanguage.ENGLISH,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class UiLanguage {
    ENGLISH,
    JAPANESE,
}
