package com.company.mysapbtpsdkproject.ui

import android.app.Application
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.company.mysapbtpsdkproject.service.OfflineBaseWorker
import com.company.mysapbtpsdkproject.service.OfflineOpenWorker
import com.company.mysapbtpsdkproject.service.OfflineProgressListener
import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.flows.compose.db.UserSecureStoreDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

enum class SyncFailureType {
    NETWORK_ERROR, TRANSACTION_ISSUE, UNKNOWN
}

sealed class OpenResult() {
    data class OpenFail(val error: SyncFailureType, val message: String? = null) : OpenResult()
    data class OpenSuccess(val message: String? = null) : OpenResult()
}

data class SyncUIState(
    val progress: Float = 0F,
    val result: OpenResult? = null,
)

class OfflineOpenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SyncUIState())
    val uiState = _uiState.asStateFlow()

    val previousUser = FlowContextRegistry.flowContext.getPreviousUser()

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.OPEN

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            LOGGER.debug("Open progress current step {}, totalSteps {}", currentStep, totalSteps)
            _uiState.update { currentState ->
                currentState.copy(
                    progress = currentStep.toFloat() / totalSteps
                )
            }
        }

        override fun getStartPoint(): Int {
            return OfflineOpenWorker.startPointForOpen
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val isMultipleUserMode =
                UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserMode()
            val appConfig = FlowContextRegistry.flowContext.appConfig

            if (OfflineWorkerUtil.initializeOffline(application, appConfig, isMultipleUserMode)) {
                startOpenOffline()
            } else {
                _uiState.update { currentState ->
                    currentState.copy(
                        result = OpenResult.OpenSuccess()
                    )
                }
            }
        }
    }

    fun startOpenOffline() {
        OfflineWorkerUtil.addProgressListener(progressListener)
        _uiState.update { SyncUIState() } //reset ui state for relaunch
        val openRequestId = OfflineWorkerUtil.open(getApplication())
        viewModelScope.launch {
            LOGGER.info("Waiting for the open finish.")
            WorkManager.getInstance(getApplication())
                .getWorkInfoByIdLiveData(openRequestId).asFlow().collect(
                ) { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        OfflineWorkerUtil.removeProgressListener(progressListener)
                        when (workInfo.state) {
                            WorkInfo.State.FAILED -> {
                                when (workInfo.outputData.getInt(
                                    OfflineWorkerUtil.OUTPUT_ERROR_KEY,
                                    0
                                )) {
                                    -1 -> {
                                        _uiState.update { currentState ->
                                            currentState.copy(
                                                result = OpenResult.OpenFail(SyncFailureType.NETWORK_ERROR)
                                            )
                                        }
                                    }
                                    -10425 -> {
                                        _uiState.update { currentState ->
                                            currentState.copy(
                                                result = OpenResult.OpenFail(SyncFailureType.TRANSACTION_ISSUE)
                                            )
                                        }
                                    }
                                    else -> {
                                        _uiState.update { currentState ->
                                            currentState.copy(
                                                result = OpenResult.OpenFail(
                                                    SyncFailureType.UNKNOWN,
                                                    message = workInfo.outputData.getString(
                                                        OfflineWorkerUtil.OUTPUT_ERROR_DETAIL
                                                    ) ?: "Offline sync failed."
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _uiState.update { currentState ->
                                    LOGGER.debug("open ")
                                    currentState.copy(
                                        result = OpenResult.OpenSuccess()
                                    )
                                }
                            }
                            else -> {
                                LOGGER.warn("Not sure state : {}", workInfo.state)
                            }
                        }
                    }
                }
        }
    }

    fun cancelSync() {
        getApplication<Application>().getSystemService(NotificationManager::class.java)
            .cancel(OfflineBaseWorker.OFFLINE_NOTIFICATION_CHANNEL_INT_ID)
        //TODO: actually open worker cancellation does not take effect, need to terminate it gracefully
        WorkManager.getInstance(getApplication())
            .cancelUniqueWork(OfflineWorkerUtil.OFFLINE_OPEN_WORKER_UNIQUE_NAME)
        OfflineWorkerUtil.resetOfflineODataProvider()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OfflineOpenViewModel::class.java)
    }
}
