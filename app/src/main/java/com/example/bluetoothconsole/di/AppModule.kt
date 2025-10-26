package com.example.bluetoothconsole.di

import android.content.Context
import androidx.room.Room
import com.example.bluetoothconsole.data.DashboardRepository
import com.example.bluetoothconsole.data.db.AppDatabase
import com.example.bluetoothconsole.data.db.DashboardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bluetooth_console.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDashboardDao(database: AppDatabase): DashboardDao = database.dashboardDao()

    @Provides
    @Singleton
    fun provideDashboardRepository(dao: DashboardDao): DashboardRepository =
        DashboardRepository(dao)
}
