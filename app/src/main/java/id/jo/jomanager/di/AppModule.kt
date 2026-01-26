package id.jo.jomanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.jo.jomanager.data.repository.RootRepository
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.data.repository.TuningRepository
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import id.jo.jomanager.data.repository.ThermalRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideTuningRepository(@ApplicationContext context: Context): TuningRepository = TuningRepository(context)

    @Provides
    @Singleton
    fun provideThermalRepository(
        @ApplicationContext context: Context,
        rootRepository: RootRepository,
        @ThermalSettings thermalDataStore: DataStore<Preferences>
    ): ThermalRepository = ThermalRepository(context, rootRepository, thermalDataStore)

    @Provides
    @Singleton
    fun provideRootRepository(): RootRepository = RootRepository()

    @Provides
    @Singleton
    fun provideSystemRepository(@ApplicationContext context: Context, tuningRepository: TuningRepository, rootRepository: RootRepository): SystemRepository =
        SystemRepository(context, tuningRepository, rootRepository)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })
    }

    @Provides
    @Singleton
    @ThermalSettings
    fun provideThermalDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("thermal_settings") })
    }

    @Provides
    @Singleton
    fun provideBatteryHistoryDatabase(@ApplicationContext context: Context): id.jo.jomanager.data.database.BatteryHistoryDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            id.jo.jomanager.data.database.BatteryHistoryDatabase::class.java,
            "battery_history_db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun provideBatteryGraphDao(database: id.jo.jomanager.data.database.BatteryHistoryDatabase): id.jo.jomanager.data.database.BatteryGraphDao {
        return database.batteryGraphDao()
    }

    @Provides
    @Singleton
    fun provideBatteryGraphRepository(dao: id.jo.jomanager.data.database.BatteryGraphDao): id.jo.jomanager.data.repository.BatteryGraphRepository {
        return id.jo.jomanager.data.repository.BatteryGraphRepository(dao)
    }

    @Provides
    @Singleton
    fun provideAppProfileDao(database: id.jo.jomanager.data.database.BatteryHistoryDatabase): id.jo.jomanager.data.database.AppProfileDao {
        return database.appProfileDao()
    }

    @Provides
    @Singleton
    fun provideAppProfileRepository(dao: id.jo.jomanager.data.database.AppProfileDao): id.jo.jomanager.data.repository.AppProfileRepository {
        return id.jo.jomanager.data.repository.AppProfileRepository(dao)
    }
}