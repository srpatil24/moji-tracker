@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dokushotracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.domain.model.UiLanguage
import com.dokushotracker.ui.components.ConfirmationDialog
import com.dokushotracker.util.NumberFormatUtils
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    viewModel: SettingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportOptions by remember { mutableStateOf(false) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var clearDialogStep by remember { mutableStateOf(0) }
    var clearConfirmText by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(json)
                }
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (!content.isNullOrBlank()) {
            pendingImportJson = content
            showImportOptions = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.value)
                is SettingsEvent.ExportReady -> {
                    pendingExportJson = event.json
                    exportLauncher.launch("dokusho_export_${System.currentTimeMillis()}.json")
                }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoalSection(
                currentGoalText = state.activeGoal?.let {
                    "Read ${NumberFormatUtils.formatLong(it.targetValue)} ${it.goalType.name.lowercase()}"
                } ?: "No active goal",
                onSetGoal = { showGoalDialog = true },
                onClearGoal = viewModel::clearGoal,
            )

            DefaultsSection(
                settings = state.appSettings,
                onDefaultMojiChanged = viewModel::setDefaultMojiCount,
                onDefaultMediaTypeChanged = viewModel::setDefaultMediaType,
            )

            AppearanceSection(
                appSettings = appSettings,
                onThemeChanged = viewModel::setThemeMode,
                onLanguageChanged = viewModel::setLanguage,
            )

            DataManagementSection(
                onExport = viewModel::exportData,
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onClearAll = { clearDialogStep = 1 },
            )
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            onDismiss = { showGoalDialog = false },
            onSave = { goalType, target, startDate, endDate ->
                viewModel.saveGoal(goalType, target, startDate, endDate)
                showGoalDialog = false
            },
        )
    }

    if (showImportOptions && pendingImportJson != null) {
        ConfirmationDialog(
            title = "Import Data",
            message = "Replace existing data? Cancel to merge entries.",
            confirmLabel = "Replace",
            dismissLabel = "Merge",
            onConfirm = {
                viewModel.importData(pendingImportJson.orEmpty(), replaceExisting = true)
                showImportOptions = false
                pendingImportJson = null
            },
            onDismiss = {
                viewModel.importData(pendingImportJson.orEmpty(), replaceExisting = false)
                showImportOptions = false
                pendingImportJson = null
            },
        )
    }

    if (clearDialogStep == 1) {
        ConfirmationDialog(
            title = "Are you sure?",
            message = "This will delete all entries and goals.",
            confirmLabel = "Continue",
            onConfirm = { clearDialogStep = 2 },
            onDismiss = { clearDialogStep = 0 },
        )
    }

    if (clearDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { clearDialogStep = 0 },
            title = { Text("Type DELETE to confirm") },
            text = {
                OutlinedTextField(
                    value = clearConfirmText,
                    onValueChange = { clearConfirmText = it },
                    label = { Text("Confirmation text") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (clearConfirmText == "DELETE") {
                            viewModel.clearAllData()
                            clearConfirmText = ""
                            clearDialogStep = 0
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clearConfirmText = ""
                        clearDialogStep = 0
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun GoalSection(
    currentGoalText: String,
    onSetGoal: () -> Unit,
    onClearGoal: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Reading Goal", style = MaterialTheme.typography.titleMedium)
            Text(currentGoalText, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onSetGoal) { Text("Set / Change Goal") }
            TextButton(onClick = onClearGoal) { Text("Clear Goal") }
        }
    }
}

@Composable
private fun DefaultsSection(
    settings: AppSettings,
    onDefaultMojiChanged: (Long) -> Unit,
    onDefaultMediaTypeChanged: (MediaType?) -> Unit,
) {
    var defaultMojiText by remember(settings.defaultMojiCount) { mutableStateOf(settings.defaultMojiCount.toString()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Defaults", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = defaultMojiText,
                onValueChange = { defaultMojiText = it.filter(Char::isDigit) },
                label = { Text("Default 文字数") },
            )
            Button(
                onClick = { defaultMojiText.toLongOrNull()?.let(onDefaultMojiChanged) },
            ) {
                Text("Save Default")
            }
            Text("Default Media Type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.defaultMediaType == null,
                    onClick = { onDefaultMediaTypeChanged(null) },
                    label = { Text("None") },
                )
                MediaType.entries.forEach { mediaType ->
                    FilterChip(
                        selected = settings.defaultMediaType == mediaType,
                        onClick = { onDefaultMediaTypeChanged(mediaType) },
                        label = { Text(mediaType.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    appSettings: AppSettings,
    onThemeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (UiLanguage) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Text("Theme")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appSettings.themeMode == mode,
                        onClick = { onThemeChanged(mode) },
                        label = { Text(mode.name) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Language")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UiLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = appSettings.language == lang,
                        onClick = { onLanguageChanged(lang) },
                        label = { Text(lang.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DataManagementSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export Data")
            }
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Import Data")
            }
            TextButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                Text("Clear All Data")
            }
        }
    }
}

@Composable
private fun GoalDialog(
    onDismiss: () -> Unit,
    onSave: (GoalType, Long, LocalDate, LocalDate?) -> Unit,
) {
    var goalType by remember { mutableStateOf(GoalType.MOJI) }
    var target by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDateText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalType.entries.forEach { type ->
                        FilterChip(
                            selected = goalType == type,
                            onClick = { goalType = type },
                            label = { Text(type.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit) },
                    label = { Text("Target value") },
                )
                OutlinedTextField(
                    value = startDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start date") },
                )
                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    label = { Text("End date (YYYY-MM-DD, optional)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedTarget = target.toLongOrNull() ?: return@TextButton
                    val parsedEndDate = endDateText.trim().takeIf { it.isNotBlank() }?.let {
                        runCatching { LocalDate.parse(it) }.getOrNull()
                    }
                    onSave(goalType, parsedTarget, startDate, parsedEndDate)
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
