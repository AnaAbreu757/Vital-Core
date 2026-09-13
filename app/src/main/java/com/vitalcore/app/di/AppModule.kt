package com.vitalcore.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Root Hilt module. Health Connect client, Room database, and repository
 * bindings are added here starting Milestone 2/3 — kept as a real, empty
 * module now so the Hilt component graph is valid and the app builds.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
