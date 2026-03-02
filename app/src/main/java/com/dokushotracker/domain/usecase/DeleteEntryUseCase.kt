package com.dokushotracker.domain.usecase

import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.ReadingEntry
import javax.inject.Inject

class DeleteEntryUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    suspend operator fun invoke(entry: ReadingEntry) {
        readingRepository.deleteEntry(entry)
    }
}
