package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.SettingsRepository
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settingsFlow()
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun setDefaultMojiCount(value: Long) = settingsRepository.setDefaultMojiCount(value)
    suspend fun setThemeMode(themeMode: ThemeMode) = settingsRepository.setThemeMode(themeMode)
    suspend fun setAccentColor(accentColor: AccentColorOption) = settingsRepository.setAccentColor(accentColor)
    suspend fun setPureBlackDarkMode(enabled: Boolean) = settingsRepository.setPureBlackDarkMode(enabled)
    suspend fun setLastCelebratedGoalCreatedAtMillis(epochMillis: Long?) = settingsRepository.setLastCelebratedGoalCreatedAtMillis(epochMillis)
    suspend fun setDefaultMediaType(mediaType: MediaType?) = settingsRepository.setDefaultMediaType(mediaType)
}
