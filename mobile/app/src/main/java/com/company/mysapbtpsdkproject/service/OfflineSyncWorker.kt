package com.company.mysapbtpsdkproject.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.company.mysapbtpsdkproject.ui.odata.ODataActivity
import com.sap.cloud.mobile.kotlin.odata.offline.OfflineODataException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class OfflineSyncWorker(context: Context, params: WorkerParameters) :
    OfflineBaseWorker(context, params) {

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.SYNC

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            val requestID = System.currentTimeMillis().toInt()
            val intent = Intent(context, ODataActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
            return startPointForSync
        }
    }

    override suspend fun doWork(): Result = withContext(IO) {
        var errorMessage: String? = null
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification())
        }
        setForeground(foregroundInfo)
        OfflineWorkerUtil.offlineODataProvider?.let { provider ->
            startPointForSync = 0
            try {
                OfflineWorkerUtil.addProgressListener(progressListener)
                logger.info("Start uploading data...")
                provider.upload()
                logger.info("Start downloading data...")
                startPointForSync = progressListener.totalStepsForTwoProgresses / 2
                provider.download()
            } catch (exception: OfflineODataException) {
                errorMessage = exception.message ?: "Unknown offline sync error when syncing data."
                logger.error("Offline sync error $errorMessage")
            } finally {
                OfflineWorkerUtil.removeProgressListener(progressListener)
            }
        }

        errorMessage?.let {
            logger.error("Offline sync error: $it")
            return@withContext Result.failure(workDataOf(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL to it))
        }
        return@withContext Result.success()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OfflineSyncWorker::class.java)
        var startPointForSync = 0
            private set
    }
}
