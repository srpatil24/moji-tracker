package com.dokushotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dokushotracker.data.local.converter.Converters
import com.dokushotracker.data.local.dao.ReadingEntryDao
import com.dokushotracker.data.local.dao.ReadingGoalDao
import com.dokushotracker.data.local.entity.ReadingEntryEntity
import com.dokushotracker.data.local.entity.ReadingGoalEntity

@Database(
    entities = [ReadingEntryEntity::class, ReadingGoalEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DokushoDatabase : RoomDatabase() {
    abstract fun readingEntryDao(): ReadingEntryDao
    abstract fun readingGoalDao(): ReadingGoalDao
}
