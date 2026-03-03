package com.dokushotracker.util

import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.data.repository.SettingsRepository
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.ReadingGoal
import com.dokushotracker.domain.model.SortOption
import com.dokushotracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ExportPayload(
    val version: Int = 1,
    val exportedAt: String,
    val appVersion: String,
    val entries: List<ExportEntry>,
    val goals: List<ExportGoal>,
    val settings: ExportSettings,
)

@Serializable
data class ExportEntry(
    val title: String,
    val mediaType: String,
    val isSeries: Boolean,
    val seriesNumber: Int? = null,
    val mojiCount: Long,
    val dateFinished: String,
    val dateAdded: String,
    val notes: String? = null,
)

@Serializable
data class ExportGoal(
    val goalType: String,
    val targetValue: Long,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean,
    val createdAt: String,
)

@Serializable
data class ExportSettings(
    val defaultMojiCount: Long,
    val theme: String,
    val defaultMediaType: String? = null,
    val accentColor: String = AccentColorOption.SAGE.name,
    val pureBlackDarkMode: Boolean = false,
    val lastCelebratedGoalCreatedAtMillis: Long? = null,
    val language: String? = null,
)

@Serializable
data class ImportResult(
    @SerialName("imported")
    val importedCount: Int,
    @SerialName("skipped")
    val skippedCount: Int,
)

@Singleton
class DataExporter @Inject constructor(
    private val readingRepository: ReadingRepository,
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(appVersion: String): String {
        val entries = readingRepository.getEntries(SortOption.DATE_ASC).first()
        val goals = goalRepository.getAllGoals().first()
        val settings = settingsRepository.settingsFlow().first()

        val payload = ExportPayload(
            exportedAt = Instant.now().toString(),
            appVersion = appVersion,
            entries = entries.map { it.toExportEntry() },
            goals = goals.map { it.toExportGoal() },
            settings = settings.toExportSettings(),
        )
        return json.encodeToString(ExportPayload.serializer(), payload)
    }

    suspend fun importJson(rawJson: String, replaceExisting: Boolean): ImportResult {
        val payload = json.decodeFromString(ExportPayload.serializer(), rawJson)
        if (replaceExisting) {
            readingRepository.clearAllEntries()
            goalRepository.clearAllGoals()
        }

        var importedCount = 0
        var skippedCount = 0
        payload.entries.forEach { entry ->
            val domainEntry = entry.toDomainEntry()
            if (domainEntry == null) {
                skippedCount += 1
            } else {
                runCatching { readingRepository.insertEntry(domainEntry) }
                    .onSuccess { importedCount += 1 }
                    .onFailure { skippedCount += 1 }
            }
        }

        val parsedGoals = payload.goals.mapNotNull { it.toDomainGoal() }
        if (parsedGoals.any { it.isActive }) {
            goalRepository.deactivateAllGoals()
        }
        parsedGoals.forEach { goal ->
            runCatching { goalRepository.insertGoal(goal) }
        }

        payload.settings.toAppSettings()?.let { importedSettings ->
            settingsRepository.setDefaultMojiCount(importedSettings.defaultMojiCount)
            settingsRepository.setThemeMode(importedSettings.themeMode)
            settingsRepository.setDefaultMediaType(importedSettings.defaultMediaType)
            settingsRepository.setAccentColor(importedSettings.accentColor)
            settingsRepository.setPureBlackDarkMode(importedSettings.pureBlackDarkMode)
            settingsRepository.setLastCelebratedGoalCreatedAtMillis(importedSettings.lastCelebratedGoalCreatedAtMillis)
        }

        return ImportResult(importedCount = importedCount, skippedCount = skippedCount)
    }

    private fun ReadingEntry.toExportEntry(): ExportEntry = ExportEntry(
        title = title,
        mediaType = mediaType.name,
        isSeries = isSeries,
        seriesNumber = seriesNumber,
        mojiCount = mojiCount,
        dateFinished = dateFinished.toString(),
        dateAdded = dateAdded.toString(),
        notes = notes,
    )

    private fun ExportEntry.toDomainEntry(): ReadingEntry? {
        val parsedMediaType = runCatching { MediaType.valueOf(this.mediaType) }.getOrNull() ?: return null
        val parsedDateFinished = runCatching { LocalDate.parse(this.dateFinished) }.getOrNull() ?: return null
        val parsedDateAdded = runCatching { Instant.parse(this.dateAdded) }.getOrElse { Instant.now() }
        return ReadingEntry(
            title = title,
            mediaType = parsedMediaType,
            isSeries = isSeries,
            seriesNumber = seriesNumber,
            mojiCount = mojiCount,
            dateFinished = parsedDateFinished,
            dateAdded = parsedDateAdded,
            notes = notes,
        )
    }

    private fun ReadingGoal.toExportGoal(): ExportGoal = ExportGoal(
        goalType = goalType.name,
        targetValue = targetValue,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        isActive = isActive,
        createdAt = createdAt.toString(),
    )

    private fun ExportGoal.toDomainGoal(): ReadingGoal? {
        val parsedGoalType = runCatching { GoalType.valueOf(this.goalType) }.getOrNull() ?: return null
        val parsedStartDate = runCatching { LocalDate.parse(this.startDate) }.getOrNull() ?: return null
        val parsedEndDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val parsedCreatedAt = runCatching { Instant.parse(this.createdAt) }.getOrElse { Instant.now() }
        return ReadingGoal(
            goalType = parsedGoalType,
            targetValue = targetValue,
            startDate = parsedStartDate,
            endDate = parsedEndDate,
            isActive = isActive,
            createdAt = parsedCreatedAt,
        )
    }

    private fun AppSettings.toExportSettings(): ExportSettings = ExportSettings(
        defaultMojiCount = defaultMojiCount,
        theme = themeMode.name,
        defaultMediaType = defaultMediaType?.name,
        accentColor = accentColor.name,
        pureBlackDarkMode = pureBlackDarkMode,
        lastCelebratedGoalCreatedAtMillis = lastCelebratedGoalCreatedAtMillis,
    )

    private fun ExportSettings.toAppSettings(): AppSettings? {
        return AppSettings(
            defaultMojiCount = defaultMojiCount,
            themeMode = runCatching { ThemeMode.valueOf(theme) }.getOrDefault(ThemeMode.SYSTEM),
            defaultMediaType = defaultMediaType?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
            accentColor = runCatching { AccentColorOption.valueOf(accentColor) }.getOrDefault(AccentColorOption.SAGE),
            pureBlackDarkMode = pureBlackDarkMode,
            lastCelebratedGoalCreatedAtMillis = lastCelebratedGoalCreatedAtMillis,
        )
    }
}
