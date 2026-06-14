package com.company.mysapbtpsdkproject.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.company.mysapbtpsdkproject.ui.MainBusinessActivity
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.kotlin.odata.offline.OfflineODataException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Represents the worker to open the offline database.
 */
class OfflineOpenWorker(context: Context, params: WorkerParameters) :
    OfflineBaseWorker(context, params) {

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.OPEN

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            val requestID = System.currentTimeMillis().toInt()
            val intent = Intent(context, MainBusinessActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                requestID,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationManager.notify(
                OFFLINE_NOTIFICATION_CHANNEL_INT_ID,
                createNotification(totalSteps, currentStep, pendingIntent)
            )
        }

        override fun getStartPoint(): Int {
            return startPointForOpen
        }
    }

    override suspend fun doWork(): Result = withContext(IO) {
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification())
        }
        setForeground(foregroundInfo)
        OfflineWorkerUtil.addProgressListener(progressListener)

        var result: Result
        try {
            result = suspendOpen()
            if (result is Result.Success) {
                val currentUser = FlowContextRegistry.flowContext.getCurrentUser()!!.id
                val dbUser = OfflineEncryptedSharedPreference.getInstance(context).getOfflineDBUser()

                if(currentUser != dbUser) {
                    //enforce download for user switch in case any pending change uploaded download from backend
                    result = suspendDownload()
                    OfflineEncryptedSharedPreference.getInstance(context).setOfflineDBUser(currentUser)
                }
            }
        } finally {
            OfflineWorkerUtil.removeProgressListener(progressListener)
        }

        OfflineWorkerUtil.removeProgressListener(progressListener)
        result
    }

    private suspend fun suspendOpen(): Result {
        try {
            OfflineWorkerUtil.offlineODataProvider?.open()
            return Result.success()
        } catch (exception: OfflineODataException) {
            val errorMessage =
                exception.message ?: "Unknown offline sync error when init opening data."
            logger.error(errorMessage)
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val errorKey = if (capabilities == null) {
                -1
            } else {
                exception.errorCode
            }
            return Result.failure(
                workDataOf(
                    OfflineWorkerUtil.OUTPUT_ERROR_KEY to errorKey,
                    OfflineWorkerUtil.OUTPUT_ERROR_DETAIL to errorMessage
                )
            )
        }
    }

    private suspend fun suspendDownload(): Result {
        try {
            OfflineWorkerUtil.offlineODataProvider?.download()
            logger.debug("Offline provider download succeeded.")
            return Result.success()
        }
        catch (exception: OfflineODataException) {
            val errorMessage =
                exception.message ?: "Unknown offline sync error when downloading data."
            logger.error(errorMessage)
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val result = if (capabilities == null) {
                -1
            } else {
                exception.errorCode
            }
            return Result.failure(
                workDataOf(
                    OfflineWorkerUtil.OUTPUT_ERROR_KEY to result,
                    OfflineWorkerUtil.OUTPUT_ERROR_DETAIL to errorMessage
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OfflineOpenWorker::class.java)
        var startPointForOpen = 0
            private set
    }
}

