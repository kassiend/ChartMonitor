package com.dauren.monitor.core.common.coroutines

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val value: ChartMonitorDispatchers)

enum class ChartMonitorDispatchers { Default, IO }

/**
 * App-scope work started from Application.onCreate; implementations are registered via Hilt `@IntoSet`.
 */
fun interface AppInitializer {
    fun init()
}
