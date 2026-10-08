package com.dauren.monitor.core.testing.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** Binds `Dispatchers.Main` to the test scheduler so `viewModelScope` runs in virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTestMain(scheduler: TestCoroutineScheduler, block: suspend () -> T): T {
    Dispatchers.setMain(StandardTestDispatcher(scheduler))
    try {
        return block()
    } finally {
        Dispatchers.resetMain()
    }
}

/** App-scope `CoroutineScope` on the test scheduler — for repository tests with long-lived collectors. */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun withAppScope(scheduler: TestCoroutineScheduler, block: suspend (CoroutineScope) -> Unit) {
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler))
    try {
        block(scope)
    } finally {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
