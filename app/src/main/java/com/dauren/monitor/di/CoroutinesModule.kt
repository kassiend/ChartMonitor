package com.dauren.monitor.di

import com.dauren.monitor.core.common.coroutines.ApplicationScope
import com.dauren.monitor.core.common.coroutines.ChartMonitorDispatchers
import com.dauren.monitor.core.common.coroutines.Dispatcher
import com.dauren.monitor.core.common.time.MonotonicClock
import com.dauren.monitor.core.common.time.SystemMonotonicClock
import com.dauren.monitor.core.common.time.SystemWallClock
import com.dauren.monitor.core.common.time.WallClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CoroutinesModule {

    @Provides
    @Dispatcher(ChartMonitorDispatchers.Default)
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Dispatcher(ChartMonitorDispatchers.IO)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(
        @Dispatcher(ChartMonitorDispatchers.Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher + CoroutineName("app-scope"))

    @Provides
    fun monotonicClock(): MonotonicClock = SystemMonotonicClock

    @Provides
    fun wallClock(): WallClock = SystemWallClock
}
