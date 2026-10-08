package com.dauren.monitor.core.ui.mvi

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel<UIState : BaseUIState> :
    ViewModel(),
    CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private val _uiState by lazy { MutableStateFlow(getStartUIState()) }

    protected val value: UIState get() = _uiState.value

    val uiState: StateFlow<UIState>
        get() = _uiState.asStateFlow()

    abstract fun getStartUIState(): UIState

    @CallSuper
    open fun onUIEvent(uiEvent: BaseUIEvent) {
        when (uiEvent) {
            is OnLeaveUIEvent -> reduce(uiEvent)
            is BackUIEvent -> reduce(uiEvent)
            is OnError<*> -> reduce(uiEvent)
            else -> Unit
        }
    }

    protected fun onUIState(newUIState: UIState) {
        _uiState.value = newUIState
    }

    protected fun updateUIState(updatedState: (state: UIState) -> UIState) {
        _uiState.update(updatedState)
    }

    protected suspend fun updateUIStateSuspend(updatedState: suspend (state: UIState) -> UIState) {
        _uiState.update { updatedState(it) }
    }

    protected fun launchWithoutCatch(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launchWithCatch(context = context, catch = {}, block = block)

    protected fun launchWithCatch(
        context: CoroutineContext = EmptyCoroutineContext,
        catch: suspend (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launchWithCatch(context = context, catch = catch, block = block)

    protected open fun reduce(uiEvent: OnLeaveUIEvent) = Unit

    protected open fun reduce(uiEvent: BackUIEvent) = Unit

    protected open fun reduce(uiEvent: OnError<*>) = Unit
}
