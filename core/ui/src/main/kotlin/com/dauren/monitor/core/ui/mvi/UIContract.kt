package com.dauren.monitor.core.ui.mvi

interface BaseUIState
object NoUIState : BaseUIState

interface BaseUIEvent
interface BaseUIEffect

data object BackUIEvent : BaseUIEvent
data object OnLeaveUIEvent : BaseUIEvent
data class OnError<T : Throwable>(val th: T) : BaseUIEvent
