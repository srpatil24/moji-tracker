package com.dokushotracker.data.repository

import com.dokushotracker.data.local.dao.ReadingEntryDao
import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.model.MediaTypeCount
import com.dokushotracker.data.model.MediaTypeMoji
import com.dokushotracker.data.model.MonthlyCount
import com.dokushotracker.data.model.MonthlyMoji
import com.dokushotracker.domain.model.ReadingEntry
import com.dokushotracker.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class ReadingRepositoryImpl @Inject constructor(
    private val readingEntryDao: ReadingEntryDao,
) : ReadingRepository {

    override suspend fun insertEntry(entry: ReadingEntry): Long {
        return readingEntryDao.insertEntry(entry.toEntity())
    }

    override suspend fun updateEntry(entry: ReadingEntry) {
        readingEntryDao.updateEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: ReadingEntry) {
        readingEntryDao.deleteEntry(entry.toEntity())
    }

    override suspend fun clearAllEntries() {
        readingEntryDao.clearAllEntries()
    }

    override fun getEntries(sortOption: SortOption): Flow<List<ReadingEntry>> {
        val flow = when (sortOption) {
            SortOption.DATE_DESC -> readingEntryDao.getAllEntriesSortedByDateDesc()
            SortOption.DATE_ASC -> readingEntryDao.getAllEntriesSortedByDateAsc()
            SortOption.MOJI_DESC -> readingEntryDao.getAllEntriesSortedByMojiDesc()
            SortOption.MOJI_ASC -> readingEntryDao.getAllEntriesSortedByMojiAsc()
            SortOption.TITLE_ASC -> readingEntryDao.getAllEntriesSortedByTitleAsc()
            SortOption.TITLE_DESC -> readingEntryDao.getAllEntriesSortedByTitleDesc()
        }
        return flow.map { entries -> entries.map { it.toDomain() } }
    }

    override fun getEntriesInDateRange(start: LocalDate, end: LocalDate): Flow<List<ReadingEntry>> {
        return readingEntryDao.getEntriesInDateRange(start, end).map { entries ->
            entries.map { it.toDomain() }
        }
    }

    override fun getTotalMojiCount(): Flow<Long> = readingEntryDao.getTotalMojiCount()

    override fun getTotalEntryCount(): Flow<Int> = readingEntryDao.getTotalEntryCount()

    override fun getCountByMediaType(): Flow<List<MediaTypeCount>> = readingEntryDao.getCountByMediaType()

    override fun getMojiByMonth(): Flow<List<MonthlyMoji>> = readingEntryDao.getMojiByMonth()

    override fun getEntriesByMonth(): Flow<List<MonthlyCount>> = readingEntryDao.getEntriesByMonth()

    override fun getCumulativeMojiOverTime(): Flow<List<CumulativeMoji>> {
        return readingEntryDao.getCumulativeMojiOverTime().map { dailyMoji ->
            var running = 0L
            dailyMoji.map { item ->
                running += item.cumulativeMoji
                item.copy(cumulativeMoji = running)
            }
        }
    }

    override fun getMojiByMediaType(): Flow<List<MediaTypeMoji>> = readingEntryDao.getMojiByMediaType()

    override fun getLatestEntry(): Flow<ReadingEntry?> = readingEntryDao.getLatestEntry().map { it?.toDomain() }

    override fun getAverageMojiPerEntry(): Flow<Double> = readingEntryDao.getAverageMojiPerEntry()

    override fun getReadingStreak(): Flow<Int> {
        return readingEntryDao.getActiveMonthsDesc().map { months ->
            if (months.isEmpty()) {
                0
            } else {
                calculateMonthlyStreak(months)
            }
        }
    }

    override fun getDistinctSeriesTitles(): Flow<List<String>> = readingEntryDao.getDistinctSeriesTitles()

    override fun getMaxSeriesNumber(title: String, mediaType: MediaType): Flow<Int?> {
        return readingEntryDao.getMaxSeriesNumber(title, mediaType)
    }

    override fun checkDuplicateExists(title: String, seriesNumber: Int?, mediaType: MediaType): Flow<Boolean> {
        return readingEntryDao.checkDuplicateExists(title, seriesNumber, mediaType)
    }

    private fun calculateMonthlyStreak(sortedYearMonthsDesc: List<String>): Int {
        var streak = 0
        var previous: YearMonth? = null
        for (monthString in sortedYearMonthsDesc) {
            val current = runCatching { YearMonth.parse(monthString) }.getOrNull() ?: continue
            if (previous == null) {
                streak = 1
                previous = current
                continue
            }
            if (current == previous.minusMonths(1)) {
                streak += 1
                previous = current
            } else {
                break
            }
        }
        return streak
    }
}
