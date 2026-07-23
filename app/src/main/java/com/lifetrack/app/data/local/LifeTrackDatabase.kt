package com.lifetrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, WaterEntryEntity::class, SleepEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LifeTrackDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
}
