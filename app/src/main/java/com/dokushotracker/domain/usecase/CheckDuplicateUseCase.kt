package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckDuplicateUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    operator fun invoke(title: String, seriesNumber: Int?, mediaType: MediaType): Flow<Boolean> {
        return readingRepository.checkDuplicateExists(title.trim(), seriesNumber, mediaType)
    }
}
