package com.dokushotracker.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.usecase.AddEntryResult
import com.dokushotracker.domain.usecase.AddEntryUseCase
import com.dokushotracker.domain.usecase.CheckDuplicateUseCase
import com.dokushotracker.domain.usecase.DeleteEntryUseCase
import com.dokushotracker.domain.usecase.GetSeriesSuggestionsUseCase
import com.dokushotracker.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class LogUiState(
    val mediaType: MediaType? = null,
    val isSeries: Boolean = false,
    val title: String = "",
    val seriesTitles: List<String> = emptyList(),
    val seriesNumber: String = "",
    val suggestedSeriesNumber: Int? = null,
    val mojiCount: String = "100000",
    val defaultMojiCount: Long = 100_000L,
    val dateFinished: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val showDuplicateWarning: Boolean = false,
)

data class LogSnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val undoEntry: ReadingEntry? = null,
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val addEntryUseCase: AddEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val checkDuplicateUseCase: CheckDuplicateUseCase,
    private val getSeriesSuggestionsUseCase: GetSeriesSuggestionsUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<LogSnackbarEvent>()
    val snackbarEvents: SharedFlow<LogSnackbarEvent> = _snackbarEvents.asSharedFlow()

    private var pendingDuplicateEntry: ReadingEntry? = null
    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        mediaType = state.mediaType ?: settings.defaultMediaType,
                        defaultMojiCount = settings.defaultMojiCount,
                        mojiCount = if (state.mojiCount == state.defaultMojiCount.toString()) {
                            settings.defaultMojiCount.toString()
                        } else {
                            state.mojiCount
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            getSeriesSuggestionsUseCase.getSeriesTitles().collect { titles ->
                _uiState.update { it.copy(seriesTitles = titles) }
            }
        }
    }

    fun onMediaTypeChanged(mediaType: MediaType) {
        _uiState.update { it.copy(mediaType = mediaType) }
        refreshSuggestedSeriesNumber()
    }

    fun onSeriesToggled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isSeries = enabled,
                seriesNumber = if (enabled) it.seriesNumber else "",
            )
        }
        refreshSuggestedSeriesNumber()
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value.take(200)) }
        refreshSuggestedSeriesNumber()
    }

    fun onSeriesNumberChanged(value: String) {
        _uiState.update { it.copy(seriesNumber = value.filter(Char::isDigit)) }
    }

    fun onMojiCountChanged(value: String) {
        _uiState.update { it.copy(mojiCount = value.filter(Char::isDigit)) }
    }

    fun onDateFinishedChanged(date: LocalDate) {
        _uiState.update { it.copy(dateFinished = date) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value.take(500)) }
    }

    fun dismissDuplicateWarning() {
        pendingDuplicateEntry = null
        _uiState.update { it.copy(showDuplicateWarning = false) }
    }

    fun confirmDuplicateSubmission() {
        val entry = pendingDuplicateEntry ?: return
        pendingDuplicateEntry = null
        _uiState.update { it.copy(showDuplicateWarning = false) }
        submitEntry(entry, allowDuplicate = true)
    }

    fun submit() {
        val state = uiState.value
        val errors = mutableMapOf<String, String>()
        val mediaType = state.mediaType
        if (mediaType == null) {
            errors["mediaType"] = "Select a media type"
        }
        val title = state.title.trim()
        if (title.isEmpty()) {
            errors["title"] = "Title is required"
        }
        val mojiCount = state.mojiCount.toLongOrNull()
        if (mojiCount == null || mojiCount <= 0L) {
            errors["mojiCount"] = "Enter a valid moji count"
        }
        val seriesNumber = if (state.isSeries) state.seriesNumber.toIntOrNull() else null
        if (state.isSeries && (seriesNumber == null || seriesNumber <= 0)) {
            errors["seriesNumber"] = "Enter a valid volume number"
        }
        if (state.dateFinished.isAfter(LocalDate.now())) {
            errors["dateFinished"] = "Date cannot be in the future"
        }
        if (errors.isNotEmpty() || mediaType == null || mojiCount == null) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        val entry = ReadingEntry(
            title = title,
            mediaType = mediaType,
            isSeries = state.isSeries,
            seriesNumber = seriesNumber,
            mojiCount = mojiCount,
            dateFinished = state.dateFinished,
            dateAdded = Instant.now(),
            notes = state.notes.trim().ifEmpty { null },
        )

        viewModelScope.launch {
            val isDuplicate = checkDuplicateUseCase(entry.title, entry.seriesNumber, entry.mediaType).first()
            if (isDuplicate) {
                pendingDuplicateEntry = entry
                _uiState.update { it.copy(showDuplicateWarning = true) }
            } else {
                submitEntry(entry, allowDuplicate = false)
            }
        }
    }

    fun undoInsert(entry: ReadingEntry) {
        viewModelScope.launch {
            deleteEntryUseCase(entry)
        }
    }

    private fun submitEntry(entry: ReadingEntry, allowDuplicate: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, validationErrors = emptyMap()) }
            when (val result = addEntryUseCase(entry, allowDuplicate)) {
                is AddEntryResult.Success -> {
                    val inserted = entry.copy(id = result.id)
                    _snackbarEvents.emit(
                        LogSnackbarEvent(
                            message = "Entry logged successfully",
                            actionLabel = "Undo",
                            undoEntry = inserted,
                        ),
                    )
                    resetForm()
                }
                AddEntryResult.Duplicate -> {
                    _snackbarEvents.emit(LogSnackbarEvent(message = "Duplicate entry already exists"))
                }
                is AddEntryResult.ValidationError -> {
                    _uiState.update { it.copy(validationErrors = mapOf("form" to result.message)) }
                }
                is AddEntryResult.Error -> {
                    _snackbarEvents.emit(LogSnackbarEvent(message = result.message))
                }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun resetForm() {
        val state = uiState.value
        _uiState.update {
            it.copy(
                isSeries = false,
                title = "",
                seriesNumber = "",
                suggestedSeriesNumber = null,
                mojiCount = state.defaultMojiCount.toString(),
                dateFinished = LocalDate.now(),
                notes = "",
                validationErrors = emptyMap(),
            )
        }
    }

    private fun refreshSuggestedSeriesNumber() {
        val state = uiState.value
        suggestionJob?.cancel()
        if (!state.isSeries || state.title.trim().isEmpty() || state.mediaType == null) {
            _uiState.update { it.copy(suggestedSeriesNumber = null) }
            return
        }
        suggestionJob = viewModelScope.launch {
            getSeriesSuggestionsUseCase.getSuggestedNextNumber(state.title.trim(), state.mediaType).collect { max ->
                _uiState.update { current ->
                    current.copy(suggestedSeriesNumber = max?.plus(1))
                }
            }
        }
    }
}
