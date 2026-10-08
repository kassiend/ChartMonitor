package com.dauren.monitor.feature.chartmonitor.data.di

import com.dauren.monitor.core.common.coroutines.AppInitializer
import com.dauren.monitor.feature.chartmonitor.data.repository.DefaultSignalRepository
import com.dauren.monitor.feature.chartmonitor.data.repository.RepositoryConfig
import com.dauren.monitor.feature.chartmonitor.domain.repository.SignalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {

    @Binds
    fun signalRepository(impl: DefaultSignalRepository): SignalRepository

    @Binds
    @IntoSet
    fun repositoryInitializer(impl: DefaultSignalRepository): AppInitializer

    companion object {
        @Provides
        fun repositoryConfig(): RepositoryConfig = RepositoryConfig()
    }
}
