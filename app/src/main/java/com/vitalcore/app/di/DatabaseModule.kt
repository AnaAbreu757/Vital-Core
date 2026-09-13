package com.vitalcore.app.di

import android.content.Context
import androidx.room.Room
import com.vitalcore.app.data.database.VitalCoreDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): VitalCoreDatabase =
        Room.databaseBuilder(context, VitalCoreDatabase::class.java, VitalCoreDatabase.DATABASE_NAME)
            // No destructive fallback: schema changes ship a real Migration
            // (see VitalCoreDatabase.MIGRATION_1_2) so a missing migration
            // fails loudly during development instead of silently wiping data.
            .addMigrations(VitalCoreDatabase.MIGRATION_1_2)
            .build()
}
