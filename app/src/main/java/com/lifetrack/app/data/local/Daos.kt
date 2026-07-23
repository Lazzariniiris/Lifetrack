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
