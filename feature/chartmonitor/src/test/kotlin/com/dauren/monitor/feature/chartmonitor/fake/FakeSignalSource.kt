package com.dauren.monitor.feature.chartmonitor.fake

import com.dauren.monitor.core.common.model.GeneratorInfo
import com.dauren.monitor.feature.chartmonitor.domain.model.SignalPoint
import com.dauren.monitor.feature.chartmonitor.domain.model.SourceStatus
import com.dauren.monitor.feature.chartmonitor.domain.source.SignalSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Minimal [SignalSource] whose points and lifecycle are controlled by the test.
 * Tests arrange: `fake.emit(SignalPoint(...))` or `fake.finish()`.
 */
class FakeSignalSource(
    override val info: GeneratorInfo,
    initialStatus: SourceStatus = SourceStatus.Active(deadlineMs = Long.MAX_VALUE),
) : SignalSource {

    private val channel = Channel<SignalPoint>(Channel.UNLIMITED)
    private val _status = MutableStateFlow(initialStatus)

    override val status: StateFlow<SourceStatus> = _status.asStateFlow()

    override fun points(): Flow<SignalPoint> = channel.receiveAsFlow()

    fun emit(point: SignalPoint) {
        check(channel.trySend(point).isSuccess) { "FakeSignalSource channel is full or closed" }
    }

    fun finish() {
        _status.value = SourceStatus.Finished
        channel.close()
    }
}
