package com.lifetrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, WaterEntryEntity::class, SleepEntryEntity::class, PendingMealAnalysisEntity::class, MealHistoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class LifeTrackDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
    abstract fun mealDao(): MealDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS pending_meal_analyses (id TEXT NOT NULL PRIMARY KEY, photoPath TEXT NOT NULL, status TEXT NOT NULL, resultJson TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_meal_analyses_status_createdAt ON pending_meal_analyses(status, createdAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS meal_history (id TEXT NOT NULL PRIMARY KEY, resultJson TEXT NOT NULL, calories REAL NOT NULL, proteinG REAL NOT NULL, carbsG REAL NOT NULL, fatG REAL NOT NULL, fiberG REAL NOT NULL, sugarsG REAL NOT NULL, sodiumMg REAL NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_history_createdAt ON meal_history(createdAt)")
    }
}
