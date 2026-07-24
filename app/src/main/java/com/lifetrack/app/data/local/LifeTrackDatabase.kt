package com.lifetrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, WaterEntryEntity::class, SleepEntryEntity::class, PendingMealAnalysisEntity::class, MealHistoryEntity::class],
    version = 3,
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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE pending_meal_analyses_new (id TEXT NOT NULL PRIMARY KEY, ownerUserId TEXT NOT NULL, photoPath TEXT NOT NULL, cloudPhotoPath TEXT, status TEXT NOT NULL, resultJson TEXT, lastError TEXT, attemptCount INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("INSERT INTO pending_meal_analyses_new (id, ownerUserId, photoPath, cloudPhotoPath, status, resultJson, lastError, attemptCount, createdAt, updatedAt) SELECT id, 'legacy-local', photoPath, NULL, status, resultJson, NULL, 0, createdAt, updatedAt FROM pending_meal_analyses")
        db.execSQL("DROP TABLE pending_meal_analyses")
        db.execSQL("ALTER TABLE pending_meal_analyses_new RENAME TO pending_meal_analyses")
        db.execSQL("CREATE INDEX index_pending_meal_analyses_ownerUserId_status_createdAt ON pending_meal_analyses(ownerUserId, status, createdAt)")

        db.execSQL("CREATE TABLE meal_history_new (id TEXT NOT NULL PRIMARY KEY, ownerUserId TEXT NOT NULL, resultJson TEXT NOT NULL, calories REAL NOT NULL, proteinG REAL NOT NULL, carbsG REAL NOT NULL, fatG REAL NOT NULL, fiberG REAL NOT NULL, sugarsG REAL NOT NULL, sodiumMg REAL NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("INSERT INTO meal_history_new (id, ownerUserId, resultJson, calories, proteinG, carbsG, fatG, fiberG, sugarsG, sodiumMg, createdAt, updatedAt) SELECT id, 'legacy-local', resultJson, calories, proteinG, carbsG, fatG, fiberG, sugarsG, sodiumMg, createdAt, createdAt FROM meal_history")
        db.execSQL("DROP TABLE meal_history")
        db.execSQL("ALTER TABLE meal_history_new RENAME TO meal_history")
        db.execSQL("CREATE INDEX index_meal_history_ownerUserId_createdAt ON meal_history(ownerUserId, createdAt)")
    }
}
