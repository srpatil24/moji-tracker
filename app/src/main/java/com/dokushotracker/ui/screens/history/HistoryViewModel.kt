package com.dokushotracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import com.dokushotracker.domain.usecase.AddEntryUseCase
import com.dokushotracker.domain.usecase.DeleteEntryUseCase
import com.dokushotracker.domain.usecase.GetEntriesUseCase
import com.dokushotracker.domain.usecase.UpdateEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val entries: List<ReadingEntry> = emptyList(),
    val sortOption: SortOption = SortOption.DATE_DESC,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val editingEntry: ReadingEntry? = null,
    val deletingEntry: ReadingEntry? = null,
)

data class HistorySnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val undoEntry: ReadingEntry? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel @Inject constructor(
    private val getEntriesUseCase: GetEntriesUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val addEntryUseCase: AddEntryUseCase,
) : ViewModel() {
    private val sortOption = MutableStateFlow(SortOption.DATE_DESC)
    private val searchQuery = MutableStateFlow("")
    private val localUiState = MutableStateFlow(HistoryUiState())

    private val _snackbarEvents = MutableSharedFlow<HistorySnackbarEvent>()
    val snackbarEvents: SharedFlow<HistorySnackbarEvent> = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        sortOption,
        searchQuery,
        sortOption.flatMapLatest { sort -> getEntriesUseCase(sort) },
        localUiState,
    ) { currentSort, query, entries, local ->
        val filteredEntries = if (query.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                    (entry.notes?.contains(query, ignoreCase = true) == true)
            }
        }
        local.copy(
            entries = filteredEntries,
            sortOption = currentSort,
            searchQuery = query,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HistoryUiState(),
    )

    fun onSortChanged(newSort: SortOption) {
        sortOption.value = newSort
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onRequestDelete(entry: ReadingEntry) {
        localUiState.update { it.copy(deletingEntry = entry) }
    }

    fun dismissDeleteDialog() {
        localUiState.update { it.copy(deletingEntry = null) }
    }

    fun confirmDelete() {
        val entry = uiState.value.deletingEntry ?: return
        localUiState.update { it.copy(deletingEntry = null) }
        viewModelScope.launch {
            deleteEntryUseCase(entry)
            _snackbarEvents.emit(
                HistorySnackbarEvent(
                    message = "Entry deleted",
                    actionLabel = "Undo",
                    undoEntry = entry,
                ),
            )
        }
    }

    fun undoDelete(entry: ReadingEntry) {
        viewModelScope.launch {
            addEntryUseCase(entry.copy(id = 0L), allowDuplicate = true)
        }
    }

    fun onEdit(entry: ReadingEntry) {
        localUiState.update { it.copy(editingEntry = entry) }
    }

    fun dismissEditSheet() {
        localUiState.update { it.copy(editingEntry = null) }
    }

    fun saveEdit(entry: ReadingEntry) {
        viewModelScope.launch {
            updateEntryUseCase(entry)
            localUiState.update { it.copy(editingEntry = null) }
            _snackbarEvents.emit(HistorySnackbarEvent(message = "Entry updated"))
        }
    }
}
