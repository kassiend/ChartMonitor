package com.dauren.monitor.core.ui.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseEffectViewModel<UIState : BaseUIState, UIEffect : BaseUIEffect> : BaseViewModel<UIState>() {

    private val _uiEffects = Channel<UIEffect>(Channel.BUFFERED)
    val uiEffects: Flow<UIEffect> = _uiEffects.receiveAsFlow()

    protected fun sendEffect(effect: UIEffect) {
        _uiEffects.trySend(effect)
    }
}
