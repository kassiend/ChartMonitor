package com.dauren.monitor.di

import com.dauren.monitor.core.common.coroutines.AppInitializer
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Declares `Set<AppInitializer>` as a multibinding so the graph compiles even when no `AppInitializer`
 * implementation is bound yet. Stage 08's `DefaultSignalRepository` will contribute `@Binds @IntoSet`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface InitializersModule {
    @Multibinds
    fun initializers(): Set<AppInitializer>
}
