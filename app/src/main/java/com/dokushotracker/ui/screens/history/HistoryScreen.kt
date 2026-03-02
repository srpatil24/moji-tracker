@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dokushotracker.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import com.dokushotracker.ui.components.ConfirmationDialog
import com.dokushotracker.ui.components.EmptyState
import com.dokushotracker.ui.theme.mediaTypeColor
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collectLatest { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && event.undoEntry != null) {
                viewModel.undoDelete(event.undoEntry)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text("Search") },
            )

            SortButtonsRow(
                current = state.sortOption,
                onSortChanged = viewModel::onSortChanged,
            )

            if (state.entries.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Book,
                    title = "No entries yet",
                    subtitle = "Start logging your reading from the Log tab.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = state.entries,
                        key = { it.id },
                    ) { entry ->
                        HistoryEntryCard(
                            entry = entry,
                            onEdit = { viewModel.onEdit(entry) },
                            onDelete = { viewModel.onRequestDelete(entry) },
                        )
                    }
                    item { Spacer(Modifier.height(92.dp)) }
                }
            }
        }
    }

    state.deletingEntry?.let { entry ->
        ConfirmationDialog(
            title = "Delete entry?",
            message = "Delete ${entry.title}? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    state.editingEntry?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = viewModel::dismissEditSheet,
            onSave = viewModel::saveEdit,
        )
    }
}

@Composable
private fun SortButtonsRow(
    current: SortOption,
    onSortChanged: (SortOption) -> Unit,
) {
    val currentGroup = current.toSortGroup()
    val currentDescending = current.isDescending()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortGroup.entries.forEach { group ->
            val isActive = currentGroup == group
            val label = buildString {
                append(group.label)
                if (isActive) {
                    append(if (currentDescending) " ▼" else " ▲")
                }
            }

            val onClick = {
                val newSort = if (isActive) {
                    group.toSortOption(descending = !currentDescending)
                } else {
                    group.toSortOption(descending = true)
                }
                onSortChanged(newSort)
            }

            if (isActive) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClick,
                ) {
                    Text(label)
                }
            } else {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClick,
                ) {
                    Text(group.label)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryEntryCard(
    entry: ReadingEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(entry.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { menuExpanded = true },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.isSeries && entry.seriesNumber != null) {
                        "${entry.title} Vol. ${entry.seriesNumber}"
                    } else {
                        entry.title
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${entry.mediaType.displayName} • ${DateUtils.formatDate(entry.dateFinished)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = mediaTypeColor(entry.mediaType),
                )
            }
            Text(
                text = "${NumberFormatUtils.formatLong(entry.mojiCount)}文字",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    menuExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EditEntryDialog(
    entry: ReadingEntry,
    onDismiss: () -> Unit,
    onSave: (ReadingEntry) -> Unit,
) {
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var moji by remember(entry.id) { mutableStateOf(entry.mojiCount.toString()) }
    var notes by remember(entry.id) { mutableStateOf(entry.notes.orEmpty()) }
    var dateFinished by remember(entry.id) { mutableStateOf(entry.dateFinished) }
    var showDatePicker by remember(entry.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(200) },
                    label = { Text("Title") },
                )
                OutlinedTextField(
                    value = moji,
                    onValueChange = { moji = it.filter(Char::isDigit) },
                    label = { Text("文字数") },
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true },
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(DateUtils.formatDate(dateFinished), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    label = { Text("Notes") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedMoji = moji.toLongOrNull() ?: return@TextButton
                    onSave(
                        entry.copy(
                            title = title.trim(),
                            mojiCount = parsedMoji,
                            dateFinished = dateFinished,
                            notes = notes.trim().ifBlank { null },
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showDatePicker) {
        val initialMillis = remember(dateFinished) {
            dateFinished.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (!pickedDate.isAfter(LocalDate.now())) {
                                dateFinished = pickedDate
                            }
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class SortGroup(val label: String) {
    DATE("Date"),
    MOJI("文字"),
    TITLE("Title"),
}

private fun SortOption.toSortGroup(): SortGroup = when (this) {
    SortOption.DATE_DESC,
    SortOption.DATE_ASC,
    -> SortGroup.DATE

    SortOption.MOJI_DESC,
    SortOption.MOJI_ASC,
    -> SortGroup.MOJI

    SortOption.TITLE_ASC,
    SortOption.TITLE_DESC,
    -> SortGroup.TITLE
}

private fun SortOption.isDescending(): Boolean = when (this) {
    SortOption.DATE_DESC,
    SortOption.MOJI_DESC,
    SortOption.TITLE_DESC,
    -> true

    SortOption.DATE_ASC,
    SortOption.MOJI_ASC,
    SortOption.TITLE_ASC,
    -> false
}

private fun SortGroup.toSortOption(descending: Boolean): SortOption = when (this) {
    SortGroup.DATE -> if (descending) SortOption.DATE_DESC else SortOption.DATE_ASC
    SortGroup.MOJI -> if (descending) SortOption.MOJI_DESC else SortOption.MOJI_ASC
    SortGroup.TITLE -> if (descending) SortOption.TITLE_DESC else SortOption.TITLE_ASC
}
