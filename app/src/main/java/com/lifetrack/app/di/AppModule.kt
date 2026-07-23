package com.lifetrack.app.di

import android.content.Context
import androidx.room.Room
import com.lifetrack.app.data.local.HabitDao
import com.lifetrack.app.data.local.HabitLogDao
import com.lifetrack.app.data.local.LifeTrackDatabase
import com.lifetrack.app.data.local.SleepDao
import com.lifetrack.app.data.local.WaterDao
import com.lifetrack.app.data.repository.DataStorePreferencesRepository
import com.lifetrack.app.data.repository.LocalHabitRepository
import com.lifetrack.app.data.repository.LocalSleepRepository
import com.lifetrack.app.data.repository.LocalWaterRepository
import com.lifetrack.app.domain.repository.HabitRepository
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.domain.repository.SleepRepository
import com.lifetrack.app.domain.repository.WaterRepository
import com.lifetrack.app.notifications.ReminderScheduler
import com.lifetrack.app.notifications.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindHabitRepository(repository: LocalHabitRepository): HabitRepository
    @Binds abstract fun bindWaterRepository(repository: LocalWaterRepository): WaterRepository
    @Binds abstract fun bindSleepRepository(repository: LocalSleepRepository): SleepRepository
    @Binds abstract fun bindPreferencesRepository(repository: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun bindReminderScheduler(scheduler: WorkManagerReminderScheduler): ReminderScheduler
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeTrackDatabase =
        Room.databaseBuilder(context, LifeTrackDatabase::class.java, "lifetrack.db")
            .build()

    @Provides fun provideHabitDao(database: LifeTrackDatabase): HabitDao = database.habitDao()
    @Provides fun provideHabitLogDao(database: LifeTrackDatabase): HabitLogDao = database.habitLogDao()
    @Provides fun provideWaterDao(database: LifeTrackDatabase): WaterDao = database.waterDao()
    @Provides fun provideSleepDao(database: LifeTrackDatabase): SleepDao = database.sleepDao()
}
