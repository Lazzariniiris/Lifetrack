package com.lifetrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Query("UPDATE habits SET isActive = 0 WHERE id = :id")
    suspend fun archive(id: String)
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLogEntity)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<WaterEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries ORDER BY bedtime DESC")
    fun observeAll(): Flow<List<SleepEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM pending_meal_analyses WHERE ownerUserId = :ownerUserId AND status = 'PENDING' ORDER BY createdAt LIMIT 1")
    suspend fun nextPending(ownerUserId: String): PendingMealAnalysisEntity?
    @Query("SELECT * FROM pending_meal_analyses WHERE ownerUserId = :ownerUserId AND status = 'READY' ORDER BY updatedAt DESC")
    fun observeReady(ownerUserId: String): Flow<List<PendingMealAnalysisEntity>>
    @Query("SELECT * FROM pending_meal_analyses WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun observeQueue(ownerUserId: String): Flow<List<PendingMealAnalysisEntity>>
    @Query("SELECT photoPath FROM pending_meal_analyses")
    suspend fun allPendingPhotoPaths(): List<String>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(value: PendingMealAnalysisEntity)
    @Query("UPDATE pending_meal_analyses SET cloudPhotoPath = :cloudPhotoPath, attemptCount = attemptCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAttempt(id: String, cloudPhotoPath: String?, updatedAt: Long)
    @Query("UPDATE pending_meal_analyses SET status = 'READY', resultJson = :result, lastError = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markReady(id: String, result: String, updatedAt: Long)
    @Query("UPDATE pending_meal_analyses SET status = 'FAILED', lastError = :message, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markFailed(id: String, message: String, updatedAt: Long)
    @Query("UPDATE pending_meal_analyses SET status = 'PENDING', lastError = NULL, attemptCount = 0, updatedAt = :updatedAt WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun retry(id: String, ownerUserId: String, updatedAt: Long)
    @Query("DELETE FROM pending_meal_analyses WHERE id = :id")
    suspend fun deletePending(id: String)
    @Query("SELECT * FROM meal_history WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun observeHistory(ownerUserId: String): Flow<List<MealHistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(value: MealHistoryEntity)
    @Query("DELETE FROM meal_history WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteHistory(id: String, ownerUserId: String)
}
