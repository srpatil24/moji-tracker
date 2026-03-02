package com.dokushotracker.domain.usecase

import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetEntriesUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    operator fun invoke(sortOption: SortOption): Flow<List<ReadingEntry>> {
        return readingRepository.getEntries(sortOption)
    }

    fun inRange(start: LocalDate, end: LocalDate): Flow<List<ReadingEntry>> {
        return readingRepository.getEntriesInDateRange(start, end)
    }
}
