package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSeriesSuggestionsUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    fun getSeriesTitles(): Flow<List<String>> = readingRepository.getDistinctSeriesTitles()

    fun getSuggestedNextNumber(title: String, mediaType: MediaType): Flow<Int?> {
        return readingRepository.getMaxSeriesNumber(title, mediaType)
    }
}
