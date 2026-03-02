package com.dokushotracker.domain.usecase

import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.util.DataExporter
import com.dokushotracker.util.ImportResult
import javax.inject.Inject

class ExportImportUseCase @Inject constructor(
    private val dataExporter: DataExporter,
    private val readingRepository: ReadingRepository,
    private val goalRepository: GoalRepository,
) {
    suspend fun export(appVersion: String): String = dataExporter.exportJson(appVersion)

    suspend fun import(rawJson: String, replaceExisting: Boolean): ImportResult {
        return dataExporter.importJson(rawJson, replaceExisting)
    }

    suspend fun clearAllData() {
        readingRepository.clearAllEntries()
        goalRepository.clearAllGoals()
    }
}
