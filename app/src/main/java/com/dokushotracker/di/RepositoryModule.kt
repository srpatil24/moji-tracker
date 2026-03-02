package com.dokushotracker.di

import com.dokushotracker.data.repository.GoalRepository
import com.dokushotracker.data.repository.GoalRepositoryImpl
import com.dokushotracker.data.repository.ReadingRepository
import com.dokushotracker.data.repository.ReadingRepositoryImpl
import com.dokushotracker.data.repository.SettingsRepository
import com.dokushotracker.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReadingRepository(impl: ReadingRepositoryImpl): ReadingRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
