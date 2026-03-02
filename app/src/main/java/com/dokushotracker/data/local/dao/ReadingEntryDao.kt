package com.dokushotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dokushotracker.data.local.entity.ReadingEntryEntity
import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.data.model.MediaTypeCount
import com.dokushotracker.data.model.MediaTypeMoji
import com.dokushotracker.data.model.MonthlyCount
import com.dokushotracker.data.model.MonthlyMoji
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ReadingEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: ReadingEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: ReadingEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: ReadingEntryEntity)

    @Query("DELETE FROM reading_entries")
    suspend fun clearAllEntries()

    @Query("SELECT * FROM reading_entries")
    fun getAllEntries(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY dateFinished DESC, dateAdded DESC")
    fun getAllEntriesSortedByDateDesc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY dateFinished ASC, dateAdded ASC")
    fun getAllEntriesSortedByDateAsc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY mojiCount DESC, dateFinished DESC")
    fun getAllEntriesSortedByMojiDesc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY mojiCount ASC, dateFinished DESC")
    fun getAllEntriesSortedByMojiAsc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY title COLLATE NOCASE ASC")
    fun getAllEntriesSortedByTitleAsc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT * FROM reading_entries ORDER BY title COLLATE NOCASE DESC")
    fun getAllEntriesSortedByTitleDesc(): Flow<List<ReadingEntryEntity>>

    @Query("SELECT COALESCE(SUM(mojiCount), 0) FROM reading_entries")
    fun getTotalMojiCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM reading_entries")
    fun getTotalEntryCount(): Flow<Int>

    @Query("SELECT mediaType, COUNT(*) as count FROM reading_entries GROUP BY mediaType")
    fun getCountByMediaType(): Flow<List<MediaTypeCount>>

    @Query("SELECT DISTINCT title FROM reading_entries WHERE isSeries = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getDistinctSeriesTitles(): Flow<List<String>>

    @Query("SELECT MAX(seriesNumber) FROM reading_entries WHERE title = :title AND mediaType = :mediaType")
    fun getMaxSeriesNumber(title: String, mediaType: MediaType): Flow<Int?>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM reading_entries
            WHERE title = :title
            AND mediaType = :mediaType
            AND (
                (seriesNumber IS NULL AND :seriesNumber IS NULL)
                OR seriesNumber = :seriesNumber
            )
        )
        """,
    )
    fun checkDuplicateExists(title: String, seriesNumber: Int?, mediaType: MediaType): Flow<Boolean>

    @Query(
        """
        SELECT
            strftime('%Y-%m', date('1970-01-01', '+' || dateFinished || ' days')) as yearMonth,
            SUM(mojiCount) as totalMoji
        FROM reading_entries
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """,
    )
    fun getMojiByMonth(): Flow<List<MonthlyMoji>>

    @Query(
        """
        SELECT
            strftime('%Y-%m', date('1970-01-01', '+' || dateFinished || ' days')) as yearMonth,
            COUNT(*) as count
        FROM reading_entries
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
        """,
    )
    fun getEntriesByMonth(): Flow<List<MonthlyCount>>

    @Query(
        """
        SELECT dateFinished as date, SUM(mojiCount) as cumulativeMoji
        FROM reading_entries
        GROUP BY dateFinished
        ORDER BY dateFinished ASC
        """,
    )
    fun getCumulativeMojiOverTime(): Flow<List<CumulativeMoji>>

    @Query("SELECT * FROM reading_entries WHERE dateFinished BETWEEN :start AND :end ORDER BY dateFinished ASC")
    fun getEntriesInDateRange(start: LocalDate, end: LocalDate): Flow<List<ReadingEntryEntity>>

    @Query("SELECT mediaType, SUM(mojiCount) as totalMoji FROM reading_entries GROUP BY mediaType")
    fun getMojiByMediaType(): Flow<List<MediaTypeMoji>>

    @Query("SELECT * FROM reading_entries ORDER BY dateFinished DESC, dateAdded DESC LIMIT 1")
    fun getLatestEntry(): Flow<ReadingEntryEntity?>

    @Query("SELECT COALESCE(AVG(mojiCount), 0.0) FROM reading_entries")
    fun getAverageMojiPerEntry(): Flow<Double>

    @Query(
        """
        SELECT DISTINCT
            strftime('%Y-%m', date('1970-01-01', '+' || dateFinished || ' days')) AS month
        FROM reading_entries
        ORDER BY month DESC
        """,
    )
    fun getActiveMonthsDesc(): Flow<List<String>>
}
