@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dokushotracker.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LogScreen(
    viewModel: LogViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSeriesSuggestions by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collectLatest { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && event.undoEntry != null) {
                viewModel.undoInsert(event.undoEntry)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(if (state.isSubmitting) "Saving..." else "Log Entry") },
                icon = { androidx.compose.material3.Icon(Icons.Filled.Done, contentDescription = null) },
                onClick = viewModel::submit,
                expanded = true,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaType.entries.forEach { mediaType ->
                    FilterChip(
                        selected = state.mediaType == mediaType,
                        onClick = { viewModel.onMediaTypeChanged(mediaType) },
                        label = { Text(mediaType.displayName) },
                    )
                }
            }
            state.validationErrors["mediaType"]?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.isSeries,
                    onClick = { viewModel.onSeriesToggled(false) },
                    label = { Text("Standalone") },
                )
                FilterChip(
                    selected = state.isSeries,
                    onClick = { viewModel.onSeriesToggled(true) },
                    label = { Text("Series") },
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.title,
                onValueChange = {
                    viewModel.onTitleChanged(it)
                    showSeriesSuggestions = true
                },
                label = { Text(if (state.isSeries) "Series Title" else "Title") },
                isError = state.validationErrors.containsKey("title"),
                supportingText = {
                    state.validationErrors["title"]?.let { Text(it) }
                },
            )

            if (state.isSeries && showSeriesSuggestions) {
                val filtered = state.seriesTitles.filter {
                    it.contains(state.title, ignoreCase = true) && state.title.isNotBlank()
                }.take(4)
                filtered.forEach { seriesTitle ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.onTitleChanged(seriesTitle)
                            showSeriesSuggestions = false
                        },
                    ) {
                        Text(seriesTitle)
                    }
                }
            }

            if (state.isSeries) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.seriesNumber,
                    onValueChange = viewModel::onSeriesNumberChanged,
                    label = { Text("Volume Number") },
                    placeholder = { state.suggestedSeriesNumber?.let { Text(it.toString()) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.validationErrors.containsKey("seriesNumber"),
                    supportingText = {
                        if (state.validationErrors.containsKey("seriesNumber")) {
                            Text(state.validationErrors["seriesNumber"].orEmpty())
                        } else if (state.suggestedSeriesNumber != null && state.seriesNumber.isBlank()) {
                            Text("Suggested: ${state.suggestedSeriesNumber}")
                        }
                    },
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.mojiCount,
                onValueChange = viewModel::onMojiCountChanged,
                label = { Text("文字数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.validationErrors.containsKey("mojiCount"),
                supportingText = {
                    if (state.validationErrors.containsKey("mojiCount")) {
                        Text(state.validationErrors["mojiCount"].orEmpty())
                    } else {
                        val raw = state.mojiCount.toLongOrNull() ?: 0L
                        Text("${NumberFormatUtils.formatLong(raw)}文字")
                    }
                },
            )

            Text(text = "Date Finished", style = MaterialTheme.typography.labelLarge)
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true },
            ) {
                androidx.compose.material3.Icon(Icons.Filled.DateRange, contentDescription = null)
                Text(DateUtils.formatDate(state.dateFinished), modifier = Modifier.padding(start = 8.dp))
            }
            state.validationErrors["dateFinished"]?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 3,
                supportingText = { Text("${state.notes.length}/500") },
            )

            state.validationErrors["form"]?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(state.dateFinished) {
            state.dateFinished.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis
                        if (selected != null) {
                            val selectedDate = Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate()
                            if (!selectedDate.isAfter(LocalDate.now())) {
                                viewModel.onDateFinishedChanged(selectedDate)
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

    if (state.showDuplicateWarning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateWarning,
            title = { Text("Possible duplicate") },
            text = { Text("An entry with the same title, volume, and media type already exists. Add anyway?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDuplicateSubmission) {
                    Text("Add Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicateWarning) {
                    Text("Cancel")
                }
            },
        )
    }
}
