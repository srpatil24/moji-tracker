package com.dokushotracker.ui.screens.log

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.ui.components.DokushoTopBar
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LogScreen(
    onOpenSettings: () -> Unit,
    viewModel: LogViewModel,
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
                viewModel.undoInsert(event.undoEntry)
            }
        }
    }

    Scaffold(
        topBar = {
            DokushoTopBar(
                title = "Log Reading",
                onOpenSettings = onOpenSettings,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        var showSeriesSuggestions by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Media Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaType.entries.forEach { mediaType ->
                    FilterChip(
                        selected = state.mediaType == mediaType,
                        onClick = { viewModel.onMediaTypeChanged(mediaType) },
                        label = { Text("${mediaType.emoji} ${mediaType.name}") },
                    )
                }
            }
            state.validationErrors["mediaType"]?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Part of a series", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.isSeries,
                    onCheckedChange = viewModel::onSeriesToggled,
                )
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { base ->
                        if (state.isSeries) {
                            base.clickable { showSeriesSuggestions = true }
                        } else {
                            base
                        }
                    },
                value = state.title,
                onValueChange = {
                    viewModel.onTitleChanged(it)
                    showSeriesSuggestions = true
                },
                label = { Text(if (state.isSeries) "Series Title" else "Title") },
                isError = state.validationErrors.containsKey("title"),
                supportingText = {
                    if (state.validationErrors.containsKey("title")) {
                        Text(state.validationErrors["title"].orEmpty())
                    }
                },
            )

            if (state.isSeries && showSeriesSuggestions) {
                val filtered = state.seriesTitles.filter {
                    it.contains(state.title, ignoreCase = true) && state.title.isNotBlank()
                }.take(6)
                if (filtered.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filtered.forEach { seriesTitle ->
                            TextButton(
                                onClick = {
                                    viewModel.onTitleChanged(seriesTitle)
                                    showSeriesSuggestions = false
                                },
                            ) {
                                Text(seriesTitle)
                            }
                        }
                    }
                }
            }

            if (state.isSeries) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.seriesNumber,
                    onValueChange = viewModel::onSeriesNumberChanged,
                    label = { Text("Volume Number") },
                    placeholder = {
                        state.suggestedSeriesNumber?.let { Text(it.toString()) }
                    },
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
                label = { Text("Moji Count") },
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

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                viewModel.onDateFinishedChanged(LocalDate.of(year, month + 1, dayOfMonth))
                            },
                            state.dateFinished.year,
                            state.dateFinished.monthValue - 1,
                            state.dateFinished.dayOfMonth,
                        ).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    },
                value = DateUtils.formatDate(state.dateFinished),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date Finished") },
                isError = state.validationErrors.containsKey("dateFinished"),
                supportingText = {
                    state.validationErrors["dateFinished"]?.let { Text(it) }
                },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text("${state.notes.length}/500") },
            )

            state.validationErrors["form"]?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSubmitting,
                onClick = viewModel::submit,
            ) {
                Text(if (state.isSubmitting) "Saving..." else "Log Entry")
            }

            Spacer(modifier = Modifier.height(8.dp))
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
