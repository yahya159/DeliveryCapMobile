package com.company.mysapbtpsdkproject.ui.odata.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.company.mysapbtpsdkproject.R
import androidx.lifecycle.asFlow
import com.company.mysapbtpsdkproject.service.OfflineProgressListener
import com.company.mysapbtpsdkproject.service.OfflineSyncWorker
import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationResult
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sap.cloud.mobile.flows.compose.db.UserSecureStoreDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class EntitySetViewModel(application: Application) : BaseOperationViewModel(application) {

    private val _isMultipleUserMode = MutableStateFlow(false)
    val isMultipleUserMode = _isMultipleUserMode.asStateFlow()

    init {
        viewModelScope.launch {
            _isMultipleUserMode.update {
                UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserMode()
            }
        }
    }

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.SYNC
        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            _operationUiState.update { currentState ->
                currentState.copy(
                    progress = currentStep.toFloat() / totalSteps
                )
            }
        }

        override fun getStartPoint(): Int {
            return OfflineSyncWorker.startPointForSync
        }
    }

    fun startSync() {
        val requestId = OfflineWorkerUtil.sync(getApplication())
        OfflineWorkerUtil.addProgressListener(progressListener)

        viewModelScope.launch {
            operationStart()
            WorkManager.getInstance(getApplication())
                .getWorkInfoByIdLiveData(requestId).asFlow()
                .collect() { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        OfflineWorkerUtil.removeProgressListener(progressListener)

                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                operationFinished(
                                    result = OperationResult.OperationSuccess("Synchronization Success")
                                )
                            }
                            WorkInfo.State.FAILED -> {
                                operationFinished(
                                    result = OperationResult.OperationFail(
                                        workInfo.outputData.getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL)
                                            ?: getApplication<Application>().getString(R.string.synchronize_failure_detail)
                                    )
                                )
                            }
                            else -> {
                                resetOperationState()
                            }
                        }
                    }
                }
        }
    }
}
