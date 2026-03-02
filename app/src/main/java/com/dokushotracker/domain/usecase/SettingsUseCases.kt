package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.SettingsRepository
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.domain.model.UiLanguage
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
    suspend fun setLanguage(language: UiLanguage) = settingsRepository.setLanguage(language)
    suspend fun setDefaultMediaType(mediaType: MediaType?) = settingsRepository.setDefaultMediaType(mediaType)
}
