package com.vine.zaiko_package.di

import android.content.Context
import androidx.room.Room
import com.vine.database.ZaikoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideZaikoDatabase(
        @ApplicationContext context: Context,
    ): ZaikoDatabase {
        return Room.databaseBuilder(
            context,
            ZaikoDatabase::class.java,
            "zaiko.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}