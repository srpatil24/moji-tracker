package com.dokushotracker.data.repository

import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.data.model.MediaType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun settingsFlow(): Flow<AppSettings>
    suspend fun setDefaultMojiCount(value: Long)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setAccentColor(accentColor: AccentColorOption)
    suspend fun setPureBlackDarkMode(enabled: Boolean)
    suspend fun setLastCelebratedGoalCreatedAtMillis(epochMillis: Long?)
    suspend fun setDefaultMediaType(mediaType: MediaType?)
}
