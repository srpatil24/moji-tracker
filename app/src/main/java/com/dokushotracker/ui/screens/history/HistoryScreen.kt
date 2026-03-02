package com.dokushotracker.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import com.dokushotracker.ui.components.ConfirmationDialog
import com.dokushotracker.ui.components.DokushoTopBar
import com.dokushotracker.ui.components.EmptyState
import com.dokushotracker.ui.theme.mediaTypeColor
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HistoryScreen(
    onOpenSettings: () -> Unit,
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
        topBar = {
            DokushoTopBar(
                title = "History",
                onOpenSettings = onOpenSettings,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text("Search") },
            )
            LazyRow(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SortOption.entries) { sort ->
                    AssistChip(
                        onClick = { viewModel.onSortChanged(sort) },
                        label = { Text(sort.displayName) },
                        leadingIcon = if (state.sortOption == sort) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            if (state.entries.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Book,
                    title = "No entries yet",
                    subtitle = "Start logging your reading from the Log tab.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun HistoryEntryCard(
    entry: ReadingEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${entry.mediaType.emoji} ${entry.mediaType.name}",
                    color = mediaTypeColor(entry.mediaType),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = DateUtils.formatDate(entry.dateFinished),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (entry.isSeries && entry.seriesNumber != null) {
                    "${entry.title} Vol. ${entry.seriesNumber}"
                } else {
                    entry.title
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "\uD83D\uDCDD ${NumberFormatUtils.formatLong(entry.mojiCount)} moji",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!entry.notes.isNullOrBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
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
                    label = { Text("Moji Count") },
                )
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
}
