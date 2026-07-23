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
import com.lifetrack.app.data.remote.SupabaseClientProvider
import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.AuthApi
import com.lifetrack.app.data.repository.SupabaseAuthRepository
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.data.remote.MealApi
import com.lifetrack.app.data.repository.RemoteMealRepository
import com.lifetrack.app.domain.repository.MealRepository
import com.lifetrack.app.data.repository.LocalMealQueueRepository
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.data.local.MealDao
import com.lifetrack.app.data.local.MIGRATION_1_2
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindHabitRepository(repository: LocalHabitRepository): HabitRepository
    @Binds abstract fun bindWaterRepository(repository: LocalWaterRepository): WaterRepository
    @Binds abstract fun bindSleepRepository(repository: LocalSleepRepository): SleepRepository
    @Binds abstract fun bindPreferencesRepository(repository: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun bindReminderScheduler(scheduler: WorkManagerReminderScheduler): ReminderScheduler
    @Binds abstract fun bindAuthRepository(repository: SupabaseAuthRepository): AuthRepository
    @Binds abstract fun bindMealRepository(repository: RemoteMealRepository): MealRepository
    @Binds abstract fun bindMealQueueRepository(repository: LocalMealQueueRepository): MealQueueRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun provideSupabaseClientProvider(): SupabaseClientProvider = SupabaseClientProvider()
    @Provides @Singleton fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    @Provides @Singleton fun provideAuthApi(json: Json): AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.SUPABASE_URL.takeIf { it.startsWith("https://") }?.plus("/") ?: "https://localhost/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(AuthApi::class.java)
    @Provides @Singleton fun provideMealApi(json: Json): MealApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.takeIf { it.startsWith("https://") } ?: "https://localhost/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(MealApi::class.java)
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeTrackDatabase =
        Room.databaseBuilder(context, LifeTrackDatabase::class.java, "lifetrack.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideHabitDao(database: LifeTrackDatabase): HabitDao = database.habitDao()
    @Provides fun provideHabitLogDao(database: LifeTrackDatabase): HabitLogDao = database.habitLogDao()
    @Provides fun provideWaterDao(database: LifeTrackDatabase): WaterDao = database.waterDao()
    @Provides fun provideSleepDao(database: LifeTrackDatabase): SleepDao = database.sleepDao()
    @Provides fun provideMealDao(database: LifeTrackDatabase): MealDao = database.mealDao()
}
