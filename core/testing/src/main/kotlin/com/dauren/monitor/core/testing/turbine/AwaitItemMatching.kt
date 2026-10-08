package com.dauren.monitor.core.testing.turbine

import app.cash.turbine.ReceiveTurbine

/**
 * Drops items until one matching [predicate] is received. Useful when a flow emits intermediate
 * updates and the test only cares about the terminal condition.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
