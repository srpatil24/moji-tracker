package com.dokushotracker.domain.usecase

import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaTypeCount
import com.dokushotracker.data.model.MediaTypeMoji
import com.dokushotracker.data.model.MonthlyCount
import com.dokushotracker.data.model.MonthlyMoji
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.domain.model.ReadingEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
) {
    fun getTotalMoji(): Flow<Long> = readingRepository.getTotalMojiCount()

    fun getTotalBooks(): Flow<Int> = readingRepository.getTotalEntryCount()

    fun getMediaTypeCounts(): Flow<List<MediaTypeCount>> = readingRepository.getCountByMediaType()

    fun getMediaTypeMoji(): Flow<List<MediaTypeMoji>> = readingRepository.getMojiByMediaType()

    fun getAverageMojiPerBook(): Flow<Double> = readingRepository.getAverageMojiPerEntry()

    fun getLatestEntry(): Flow<ReadingEntry?> = readingRepository.getLatestEntry()

    fun getReadingStreak(): Flow<Int> = readingRepository.getReadingStreak()

    fun getCumulativeMoji(): Flow<List<CumulativeMoji>> = readingRepository.getCumulativeMojiOverTime()

    fun getMonthlyCount(): Flow<List<MonthlyCount>> = readingRepository.getEntriesByMonth()

    fun getMonthlyMoji(): Flow<List<MonthlyMoji>> = readingRepository.getMojiByMonth()
}
