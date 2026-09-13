package com.vitalcore.app.di

import com.vitalcore.app.ai.AiProvider
import com.vitalcore.app.ai.RemoteAiProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [AiProvider] to [RemoteAiProvider], which itself checks
 * AiSettingsRepository at call time and returns a clear "not configured"
 * error if the user hasn't set up a provider yet — so there's no need for a
 * runtime NoOp/Remote switch here; RemoteAiProvider degrades gracefully on
 * its own.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiProvider(impl: RemoteAiProvider): AiProvider
}
