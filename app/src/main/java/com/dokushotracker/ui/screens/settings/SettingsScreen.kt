@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.dokushotracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ReadingGoal
import com.dokushotracker.domain.model.ThemeMode
import com.dokushotracker.ui.components.ConfirmationDialog
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pureBlackMode = isPureBlackActive(state.appSettings)
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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoalSection(
                activeGoal = state.activeGoal,
                pureBlackMode = pureBlackMode,
                onSetGoal = { showGoalDialog = true },
                onClearGoal = viewModel::clearGoal,
            )

            DefaultsSection(
                settings = state.appSettings,
                pureBlackMode = pureBlackMode,
                onDefaultMojiChanged = viewModel::setDefaultMojiCount,
                onDefaultMediaTypeChanged = viewModel::setDefaultMediaType,
            )

            AppearanceSection(
                appSettings = state.appSettings,
                pureBlackMode = pureBlackMode,
                onThemeChanged = viewModel::setThemeMode,
                onAccentColorChanged = viewModel::setAccentColor,
                onPureBlackDarkModeChanged = viewModel::setPureBlackDarkMode,
            )

            DataManagementSection(
                pureBlackMode = pureBlackMode,
                onExport = viewModel::exportData,
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onClearAll = { clearDialogStep = 1 },
            )
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            initialGoal = state.activeGoal,
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
private fun isPureBlackActive(settings: AppSettings): Boolean {
    val systemDark = isSystemInDarkTheme()
    val darkModeActive = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    return darkModeActive && settings.pureBlackDarkMode
}

@Composable
private fun settingsCardColors(
    pureBlackMode: Boolean,
    normalContainerColor: Color,
): CardColors {
    return if (pureBlackMode) {
        CardDefaults.cardColors(containerColor = Color.Black)
    } else {
        CardDefaults.cardColors(containerColor = normalContainerColor)
    }
}

@Composable
private fun settingsCardBorder(pureBlackMode: Boolean): BorderStroke? {
    return if (pureBlackMode) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }
}

@Composable
private fun GoalSection(
    activeGoal: ReadingGoal?,
    pureBlackMode: Boolean,
    onSetGoal: () -> Unit,
    onClearGoal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = settingsCardColors(
            pureBlackMode = pureBlackMode,
            normalContainerColor = if (activeGoal != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = settingsCardBorder(pureBlackMode),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Reading Goal", style = MaterialTheme.typography.titleMedium)

            if (activeGoal == null) {
                Text("No active goal. Set one to keep your progress focused.")
                FilledTonalButton(onClick = onSetGoal) {
                    Text("Set Goal")
                }
            } else {
                val unit = if (activeGoal.goalType == GoalType.MOJI) "文字" else "books"
                Text(
                    text = "Target: ${NumberFormatUtils.formatLong(activeGoal.targetValue)} $unit",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Start: ${DateUtils.formatDate(activeGoal.startDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                activeGoal.endDate?.let { endDate ->
                    Text(
                        text = "Deadline: ${DateUtils.formatDate(endDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onSetGoal) {
                        Text("Change Goal")
                    }
                    OutlinedButton(onClick = onClearGoal) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultsSection(
    settings: AppSettings,
    pureBlackMode: Boolean,
    onDefaultMojiChanged: (Long) -> Unit,
    onDefaultMediaTypeChanged: (MediaType?) -> Unit,
) {
    var defaultMojiText by remember(settings.defaultMojiCount) { mutableStateOf(settings.defaultMojiCount.toString()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = settingsCardColors(pureBlackMode = pureBlackMode, normalContainerColor = MaterialTheme.colorScheme.surface),
        border = settingsCardBorder(pureBlackMode),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Defaults", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = defaultMojiText,
                onValueChange = { defaultMojiText = it.filter(Char::isDigit) },
                label = { Text("Default 文字数") },
            )
            FilledTonalButton(
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
                        label = { Text(mediaType.displayName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    appSettings: AppSettings,
    pureBlackMode: Boolean,
    onThemeChanged: (ThemeMode) -> Unit,
    onAccentColorChanged: (AccentColorOption) -> Unit,
    onPureBlackDarkModeChanged: (Boolean) -> Unit,
) {
    val darkModeActive = when (appSettings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = settingsCardColors(
            pureBlackMode = pureBlackMode,
            normalContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        border = settingsCardBorder(pureBlackMode),
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
            Text("Accent Color")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentColorOption.entries.forEach { accent ->
                    AccentColorSwatch(
                        accent = accent,
                        selected = appSettings.accentColor == accent,
                        size = 34.dp,
                        onClick = { onAccentColorChanged(accent) },
                    )
                }
            }
            if (darkModeActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Pure Black Dark Mode", modifier = Modifier.weight(1f))
                    Switch(
                        checked = appSettings.pureBlackDarkMode,
                        onCheckedChange = onPureBlackDarkModeChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentColorSwatch(
    accent: AccentColorOption,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(size)
                .clip(MaterialTheme.shapes.small)
                .background(Color(accent.seedColor))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small,
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            if (selected) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
        Text(accent.displayName, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DataManagementSection(
    pureBlackMode: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = settingsCardColors(
            pureBlackMode = pureBlackMode,
            normalContainerColor = MaterialTheme.colorScheme.surface,
        ),
        border = settingsCardBorder(pureBlackMode),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            DataActionItem(
                title = "Export Backup",
                subtitle = "Save entries, goals, and settings as a JSON file.",
                icon = Icons.Filled.ArrowUpward,
                pureBlackMode = pureBlackMode,
                onClick = onExport,
            )
            DataActionItem(
                title = "Import Backup",
                subtitle = "Bring data from a previously exported file.",
                icon = Icons.Filled.ArrowDownward,
                pureBlackMode = pureBlackMode,
                onClick = onImport,
            )
            DataActionItem(
                title = "Clear All Data",
                subtitle = "Permanently delete entries and goals from this device.",
                icon = Icons.Filled.DeleteForever,
                pureBlackMode = pureBlackMode,
                onClick = onClearAll,
                destructive = true,
            )
        }
    }
}

@Composable
private fun DataActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    pureBlackMode: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val containerColor = if (pureBlackMode) {
        Color.Black
    } else if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    }
    val contentColor = if (pureBlackMode && destructive) {
        MaterialTheme.colorScheme.error
    } else if (destructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        pureBlackMode && destructive -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        pureBlackMode -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun GoalDialog(
    initialGoal: ReadingGoal?,
    onDismiss: () -> Unit,
    onSave: (GoalType, Long, LocalDate, LocalDate?) -> Unit,
) {
    var goalType by remember(initialGoal) { mutableStateOf(initialGoal?.goalType ?: GoalType.MOJI) }
    var target by remember(initialGoal) { mutableStateOf(initialGoal?.targetValue?.toString().orEmpty()) }
    var startDate by remember(initialGoal) { mutableStateOf(initialGoal?.startDate ?: LocalDate.now()) }
    var endDate by remember(initialGoal) { mutableStateOf(initialGoal?.endDate) }

    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

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
                            label = { Text(type.displayName) },
                        )
                    }
                }
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit) },
                    label = { Text("Target value") },
                )

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pickingStartDate = true },
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text("Start: ${DateUtils.formatDate(startDate)}", modifier = Modifier.padding(start = 8.dp))
                }

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pickingEndDate = true },
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.DateRange, contentDescription = null)
                    Text(
                        text = "End: ${endDate?.let(DateUtils::formatDate) ?: "None"}",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                if (endDate != null) {
                    TextButton(onClick = { endDate = null }) {
                        Text("Clear end date")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedTarget = target.toLongOrNull() ?: return@TextButton
                    onSave(goalType, parsedTarget, startDate, endDate)
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

    if (pickingStartDate) {
        GoalDatePickerDialog(
            initialDate = startDate,
            onDismiss = { pickingStartDate = false },
            onDateSelected = {
                startDate = it
                if (endDate != null && endDate!!.isBefore(startDate)) {
                    endDate = startDate
                }
                pickingStartDate = false
            },
        )
    }

    if (pickingEndDate) {
        GoalDatePickerDialog(
            initialDate = endDate ?: startDate,
            onDismiss = { pickingEndDate = false },
            onDateSelected = {
                endDate = it
                pickingEndDate = false
            },
        )
    }
}

@Composable
private fun GoalDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(selected)
                    }
                },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
