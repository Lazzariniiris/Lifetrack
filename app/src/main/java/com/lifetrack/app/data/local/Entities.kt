package com.lifetrack.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits", indices = [Index(value = ["isActive"])])
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val targetType: String,
    val targetValue: Int,
    val color: Long,
    val isActive: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["loggedAt"])],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val loggedAt: Long,
    val value: Int,
)

@Entity(tableName = "water_entries", indices = [Index(value = ["loggedAt"])])
data class WaterEntryEntity(
    @PrimaryKey val id: String,
    val amountMl: Int,
    val loggedAt: Long,
)

@Entity(tableName = "sleep_entries", indices = [Index(value = ["bedtime"]), Index(value = ["wakeTime"])])
data class SleepEntryEntity(
    @PrimaryKey val id: String,
    val bedtime: Long,
    val wakeTime: Long,
    val quality: Int,
    val notes: String?,
)
