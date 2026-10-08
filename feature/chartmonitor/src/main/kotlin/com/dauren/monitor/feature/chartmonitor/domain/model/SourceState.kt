package com.dauren.monitor.feature.chartmonitor.domain.model

import com.dauren.monitor.core.common.model.GeneratorInfo

data class SourceState(
    val info: GeneratorInfo,
    val status: SourceStatus,
    val latest: SignalPoint?,
)
