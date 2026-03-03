package com.dokushotracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokushotracker.BuildConfig
import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ReadingGoal
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.domain.usecase.ExportImportUseCase
import com.dokushotracker.domain.usecase.ObserveSettingsUseCase
import com.dokushotracker.domain.usecase.SetGoalUseCase
import com.dokushotracker.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val activeGoal: ReadingGoal? = null,
)

sealed interface SettingsEvent {
    data class Message(val value: String) : SettingsEvent
    data class ExportReady(val json: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    goalRepository: GoalRepository,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val setGoalUseCase: SetGoalUseCase,
    private val exportImportUseCase: ExportImportUseCase,
) : ViewModel() {
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeSettingsUseCase(),
        goalRepository.getActiveGoal(),
    ) { settings, goal ->
        SettingsUiState(
            appSettings = settings,
            activeGoal = goal,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    fun setDefaultMojiCount(value: Long) {
        viewModelScope.launch {
            updateSettingsUseCase.setDefaultMojiCount(value)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            updateSettingsUseCase.setThemeMode(mode)
        }
    }

    fun setAccentColor(accentColor: AccentColorOption) {
        viewModelScope.launch {
            updateSettingsUseCase.setAccentColor(accentColor)
        }
    }

    fun setPureBlackDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setPureBlackDarkMode(enabled)
        }
    }

    fun setDefaultMediaType(mediaType: MediaType?) {
        viewModelScope.launch {
            updateSettingsUseCase.setDefaultMediaType(mediaType)
        }
    }

    fun saveGoal(goalType: GoalType, targetValue: Long, startDate: LocalDate, endDate: LocalDate?) {
        viewModelScope.launch {
            setGoalUseCase.setGoal(
                ReadingGoal(
                    goalType = goalType,
                    targetValue = targetValue,
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true,
                ),
            ).onSuccess {
                _events.emit(SettingsEvent.Message("Goal saved"))
            }.onFailure {
                _events.emit(SettingsEvent.Message(it.message ?: "Failed to save goal"))
            }
        }
    }

    fun clearGoal() {
        viewModelScope.launch {
            setGoalUseCase.clearGoal()
            _events.emit(SettingsEvent.Message("Goal cleared"))
        }
    }

    fun exportData() {
        viewModelScope.launch {
            runCatching { exportImportUseCase.export(appVersion = BuildConfig.VERSION_NAME) }
                .onSuccess { json -> _events.emit(SettingsEvent.ExportReady(json)) }
                .onFailure { error -> _events.emit(SettingsEvent.Message(error.message ?: "Export failed")) }
        }
    }

    fun importData(rawJson: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            runCatching { exportImportUseCase.import(rawJson, replaceExisting) }
                .onSuccess { result ->
                    _events.emit(
                        SettingsEvent.Message(
                            "Imported ${result.importedCount} entries, skipped ${result.skippedCount} duplicates.",
                        ),
                    )
                }
                .onFailure { error ->
                    _events.emit(SettingsEvent.Message(error.message ?: "Import failed"))
                }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            exportImportUseCase.clearAllData()
            _events.emit(SettingsEvent.Message("All data cleared"))
        }
    }
}
