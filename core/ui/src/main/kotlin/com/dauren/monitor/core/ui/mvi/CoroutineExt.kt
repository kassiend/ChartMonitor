package com.dauren.monitor.core.ui.mvi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal fun CoroutineScope.launchWithCatch(
    context: CoroutineContext = EmptyCoroutineContext,
    catch: suspend (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context) {
    try {
        block()
    } catch (ce: CancellationException) {
        throw ce
    } catch (th: Throwable) {
        catch(th)
    }
}
