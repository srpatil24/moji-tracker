package com.dokushotracker.di

import android.content.Context
import androidx.room.Room
import com.dokushotracker.data.local.DokushoDatabase
import com.dokushotracker.data.local.dao.ReadingEntryDao
import com.dokushotracker.data.local.dao.ReadingGoalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DokushoDatabase {
        return Room.databaseBuilder(
            context,
            DokushoDatabase::class.java,
            "dokusho_database",
        ).build()
    }

    @Provides
    fun provideReadingEntryDao(database: DokushoDatabase): ReadingEntryDao = database.readingEntryDao()

    @Provides
    fun provideReadingGoalDao(database: DokushoDatabase): ReadingGoalDao = database.readingGoalDao()
}
