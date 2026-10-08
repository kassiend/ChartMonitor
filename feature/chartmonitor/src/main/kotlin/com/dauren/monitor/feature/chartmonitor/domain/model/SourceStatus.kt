package com.dauren.monitor.feature.chartmonitor.domain.model

sealed interface SourceStatus {
    // deadlineMs is on the MonotonicClock.
    data class Active(val deadlineMs: Long) : SourceStatus
    data object Finished : SourceStatus
}
