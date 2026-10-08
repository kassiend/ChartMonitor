package com.dauren.monitor.feature.chartmonitor.data.di

import com.dauren.monitor.feature.chartmonitor.data.source.GeneratorSourceFactory
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSourceFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

// Multibinding: a new source type adds @Binds @IntoSet here; the repository picks it up.
@Module
@InstallIn(SingletonComponent::class)
internal interface SourceFactoriesModule {

    @Binds
    @IntoSet
    fun generatorSourceFactory(factory: GeneratorSourceFactory): SignalSourceFactory
}
