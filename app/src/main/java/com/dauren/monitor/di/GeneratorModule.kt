package com.dauren.monitor.di

import androidx.compose.ui.graphics.toArgb
import com.dauren.monitor.core.ui.color.SeriesPalette
import com.dauren.monitor.feature.chartmonitor.data.source.GeneratorConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
internal object GeneratorModule {

    @Provides
    fun random(): Random = Random.Default

    @Provides
    fun generatorConfig(): GeneratorConfig {
        val count = GeneratorConfig.DEFAULT_COUNT
        return GeneratorConfig(
            count = count,
            colorsArgb = SeriesPalette.generate(count).map { it.toArgb() },
        )
    }
}
