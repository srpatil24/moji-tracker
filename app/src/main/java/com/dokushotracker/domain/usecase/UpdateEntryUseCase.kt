package com.dokushotracker.domain.usecase

import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.ReadingEntry
import java.time.LocalDate
import javax.inject.Inject

class UpdateEntryUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    suspend operator fun invoke(entry: ReadingEntry): Result<Unit> {
        val validationMessage = validate(entry)
        if (validationMessage != null) {
            return Result.failure(IllegalArgumentException(validationMessage))
        }
        return runCatching { readingRepository.updateEntry(entry.copy(title = entry.title.trim())) }
    }

    private fun validate(entry: ReadingEntry): String? {
        if (entry.title.trim().isEmpty()) return "Title is required"
        if (entry.title.length > 200) return "Title must be at most 200 characters"
        if (entry.isSeries && (entry.seriesNumber == null || entry.seriesNumber <= 0)) {
            return "Series number must be positive"
        }
        if (entry.mojiCount <= 0L) return "文字数 must be greater than 0"
        if (entry.dateFinished.isAfter(LocalDate.now())) return "Date cannot be in the future"
        return null
    }
}
