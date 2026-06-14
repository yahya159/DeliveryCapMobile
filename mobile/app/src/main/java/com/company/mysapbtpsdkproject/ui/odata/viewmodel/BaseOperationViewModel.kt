package com.company.mysapbtpsdkproject.ui.odata.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationResult
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class BaseOperationViewModel(application: Application) : AndroidViewModel(application) {
    protected val _operationUiState =
        MutableStateFlow(OperationUIState())
    val operationUiState = _operationUiState.asStateFlow()

    fun resetOperationState() {
        _operationUiState.update { OperationUIState() }
    }

    fun operationFinished(result: OperationResult) {
        _operationUiState.update { OperationUIState(result = result) }
    }

    fun operationStart() {
        _operationUiState.update { it.copy(inProgress = true) }
    }
}
