package com.dokushotracker.data.repository

import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.model.MediaTypeCount
import com.dokushotracker.data.model.MediaTypeMoji
import com.dokushotracker.data.model.MonthlyCount
import com.dokushotracker.data.model.MonthlyMoji
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReadingRepository {
    suspend fun insertEntry(entry: ReadingEntry): Long
    suspend fun updateEntry(entry: ReadingEntry)
    suspend fun deleteEntry(entry: ReadingEntry)
    suspend fun clearAllEntries()

    fun getEntries(sortOption: SortOption): Flow<List<ReadingEntry>>
    fun getEntriesInDateRange(start: LocalDate, end: LocalDate): Flow<List<ReadingEntry>>

    fun getTotalMojiCount(): Flow<Long>
    fun getTotalEntryCount(): Flow<Int>
    fun getCountByMediaType(): Flow<List<MediaTypeCount>>
    fun getMojiByMonth(): Flow<List<MonthlyMoji>>
    fun getEntriesByMonth(): Flow<List<MonthlyCount>>
    fun getCumulativeMojiOverTime(): Flow<List<CumulativeMoji>>
    fun getMojiByMediaType(): Flow<List<MediaTypeMoji>>
    fun getLatestEntry(): Flow<ReadingEntry?>
    fun getAverageMojiPerEntry(): Flow<Double>
    fun getReadingStreak(): Flow<Int>

    fun getDistinctSeriesTitles(): Flow<List<String>>
    fun getMaxSeriesNumber(title: String, mediaType: MediaType): Flow<Int?>
    fun checkDuplicateExists(title: String, seriesNumber: Int?, mediaType: MediaType): Flow<Boolean>
}
