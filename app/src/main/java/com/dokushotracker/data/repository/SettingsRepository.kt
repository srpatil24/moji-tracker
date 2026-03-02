package com.dokushotracker.data.repository

import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.domain.model.UiLanguage
import com.dokushotracker.data.model.MediaType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun settingsFlow(): Flow<AppSettings>
    suspend fun setDefaultMojiCount(value: Long)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setLanguage(language: UiLanguage)
    suspend fun setDefaultMediaType(mediaType: MediaType?)
}
