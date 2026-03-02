package com.dokushotracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dokushotracker.data.local.entity.ReadingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: ReadingGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: ReadingGoalEntity)

    @Query("SELECT * FROM reading_goals WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveGoal(): Flow<ReadingGoalEntity?>

    @Query("SELECT * FROM reading_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<ReadingGoalEntity>>

    @Query("UPDATE reading_goals SET isActive = 0")
    suspend fun deactivateAllGoals()

    @Query("DELETE FROM reading_goals")
    suspend fun clearAllGoals()
}
