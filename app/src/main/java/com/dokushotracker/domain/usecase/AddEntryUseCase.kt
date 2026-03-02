package com.dokushotracker.domain.usecase

import android.database.sqlite.SQLiteConstraintException
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.ReadingEntry
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

sealed interface AddEntryResult {
    data class Success(val id: Long) : AddEntryResult
    data object Duplicate : AddEntryResult
    data class ValidationError(val message: String) : AddEntryResult
    data class Error(val message: String) : AddEntryResult
}

class AddEntryUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    suspend operator fun invoke(entry: ReadingEntry, allowDuplicate: Boolean = false): AddEntryResult {
        val validationMessage = validate(entry)
        if (validationMessage != null) {
            return AddEntryResult.ValidationError(validationMessage)
        }

        val isDuplicate = readingRepository
            .checkDuplicateExists(entry.title.trim(), entry.seriesNumber, entry.mediaType)
            .first()
        if (isDuplicate && !allowDuplicate) {
            return AddEntryResult.Duplicate
        }

        return try {
            val id = readingRepository.insertEntry(entry.copy(title = entry.title.trim()))
            AddEntryResult.Success(id)
        } catch (_: SQLiteConstraintException) {
            AddEntryResult.Duplicate
        } catch (exception: Exception) {
            AddEntryResult.Error(exception.message ?: "Failed to save entry")
        }
    }

    private fun validate(entry: ReadingEntry): String? {
        if (entry.title.trim().isEmpty()) return "Title is required"
        if (entry.title.length > 200) return "Title must be at most 200 characters"
        if (entry.isSeries && (entry.seriesNumber == null || entry.seriesNumber <= 0)) {
            return "Series number must be a positive integer"
        }
        if (entry.mojiCount <= 0L) return "文字数 must be greater than 0"
        if (entry.dateFinished.isAfter(LocalDate.now())) return "Date cannot be in the future"
        if ((entry.notes?.length ?: 0) > 500) return "Notes must be 500 characters or less"
        return null
    }
}
