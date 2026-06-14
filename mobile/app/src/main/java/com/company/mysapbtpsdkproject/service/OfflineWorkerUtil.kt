package com.company.mysapbtpsdkproject.service

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.company.mysapbtpsdkproject.app.SAPWizardApplication
import com.company.mysapbtpsdkproject.repository.RepositoryFactory
import com.sap.cloud.android.odata.entitycontainer.EntityContainer
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.flows.compose.db.UserSecureStoreDelegate
import com.sap.cloud.mobile.foundation.common.ClientProvider
import com.sap.cloud.mobile.foundation.common.SettingsProvider
import com.sap.cloud.mobile.foundation.model.AppConfig
import com.sap.cloud.mobile.kotlin.odata.core.AndroidSystem
import com.sap.cloud.mobile.kotlin.odata.core.LoggerFactory
import com.sap.cloud.mobile.kotlin.odata.offline.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.net.URL
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

object OfflineWorkerUtil {
    private val offlineMutex = Object()

    @Volatile
    var offlineODataProvider: OfflineODataProvider? = null
        private set(value) {
            synchronized(offlineMutex) {
                field = value
            }
        }

    /** OData service for interacting with local OData Provider */
    lateinit var entityContainer: EntityContainer
        private set
    private val logger = LoggerFactory.getLogger(OfflineWorkerUtil::class.java.toString())
    private val progressListeners =
        CopyOnWriteArraySet<OfflineProgressListener>() //To avoid concurrent mod issue

    const val OFFLINE_OPEN_WORKER_UNIQUE_NAME = "offline_open_worker"
    const val OFFLINE_SYNC_WORKER_UNIQUE_NAME = "offline_sync_worker"

    const val OUTPUT_ERROR_KEY = "output.error"
    const val OUTPUT_ERROR_DETAIL = "output.error.detail"

    /** Name of the offline data file on the application file space */
    private const val OFFLINE_DATASTORE = "OfflineDataStore"
    const val OFFLINE_DATASTORE_ENCRYPTION_KEY =
        "Offline_DataStore_EncryptionKey"

    /** Header name for application version */
    private const val APP_VERSION_HEADER = "X-APP-VERSION"

	/** Connection ID of Mobile Application*/
	private const val CONNECTION_ID_ENTITYCONTAINER = "MobileApp"

    fun addProgressListener(listener: OfflineProgressListener) =
        progressListeners.add(listener)

    fun removeProgressListener(listener: OfflineProgressListener) =
        progressListeners.remove(listener)

    fun resetOfflineODataProvider() {
        offlineODataProvider?.close()
        offlineODataProvider = null
        RepositoryFactory.reset()
    }

    private val delegate = object : OfflineODataProviderDelegate {
        override fun updateOpenProgress(
            provider: OfflineODataProvider,
            progress: OfflineODataProviderOperationProgress
        ) = notifyListeners(provider, progress)

        override fun updateDownloadProgress(
            provider: OfflineODataProvider,
            progress: OfflineODataProviderDownloadProgress
        ) = notifyListeners(provider, progress)

        override fun updateUploadProgress(
            provider: OfflineODataProvider,
            progress: OfflineODataProviderOperationProgress
        ) = notifyListeners(provider, progress)

        override fun updateFailedRequest(provider: OfflineODataProvider, request: OfflineODataFailedRequest) =
            Unit

        override fun updateSendStoreProgress(
            provider: OfflineODataProvider,
            progress: OfflineODataProviderOperationProgress
        ) = notifyListeners(provider, progress)

        private fun notifyListeners(
            provider: OfflineODataProvider,
            progress: OfflineODataProviderOperationProgress
        ) {
            logger.debug("Progress " + progress.currentStepNumber + " out of " + progress.totalNumberOfSteps)
            MainScope().launch {
                progressListeners.forEach {
                    it.onOfflineProgress(progress)
                }
            }
        }
    }

    /*
     * Create OfflineODataProvider
     * This is a blocking call, no data will be transferred until open, download, upload
     * @return if initialization needed
     */
    suspend fun initializeOffline(
        context: Context,
        appConfig: AppConfig,
        runtimeMultipleUserMode: Boolean
    ): Boolean {
        //reset offline odata provider if necessary. (e.g. switch user)
        resetOfflineODataProvider()

        if (FlowContextRegistry.flowContext.getCurrentUser() == null)
            error("Current user not ready yet.")
        AndroidSystem.setContext(context as SAPWizardApplication)
        val serviceUrl = appConfig.serviceUrl
        try {
            val url = URL(serviceUrl + CONNECTION_ID_ENTITYCONTAINER)
            val offlineODataParameters = OfflineODataParameters().apply {
                isEnableRepeatableRequests = true
                storeName = OFFLINE_DATASTORE

                currentUser = FlowContextRegistry.flowContext.getCurrentUser()!!.id
                isForceUploadOnUserSwitch = runtimeMultipleUserMode
                val encryptionKey = if (runtimeMultipleUserMode) {
                    UserSecureStoreDelegate.getInstance().getOfflineEncryptionKey()
                } else { //If is single user mode, create and save a key into user secure store for accessing offline DB
                    if (UserSecureStoreDelegate.getInstance()
                            .getData<String>(OFFLINE_DATASTORE_ENCRYPTION_KEY) == null
                    ) {
                        val bytes = ByteArray(32)
                        val random = SecureRandom()
                        random.nextBytes(bytes)
                        val key = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        UserSecureStoreDelegate.getInstance()
                            .saveData(OFFLINE_DATASTORE_ENCRYPTION_KEY, key)
                        Arrays.fill(bytes, 0.toByte())
                        key
                    } else {
                        UserSecureStoreDelegate.getInstance()
                            .getData<String>(OFFLINE_DATASTORE_ENCRYPTION_KEY)
                    }
                }
                storeEncryptionKey = encryptionKey
            }.also {
                // Set the default application version
                val customHeaders = it.getCustomHeaders().toMutableMap()
                SettingsProvider.get().applicationVersion?.also {appVersion ->
                    customHeaders[APP_VERSION_HEADER] = appVersion
                }
                it.setCustomHeaders(customHeaders)
            }
            logger.debug("start init offline odata provider")
            synchronized(offlineMutex) {
                buildOfflineODataProvider(
                    url,
                    offlineODataParameters
                ).also {
                    offlineODataProvider = it
                    entityContainer = EntityContainer(it.getSyncProvider())
                }
            }
            return true
        } catch (e: Exception) {
            logger.error("Exception encountered setting up offline store: " + e.message)
            throw e
        }
    }

    private fun buildOfflineODataProvider(
        url: URL,
        offlineODataParameters: OfflineODataParameters
    ): OfflineODataProvider {
        return OfflineODataProvider(
            url,
            offlineODataParameters,
            ClientProvider.get(),
            delegate
        ).apply {
            val deliveriesQuery = OfflineODataDefiningQuery("Deliveries", "Deliveries", false)
            addDefiningQuery(deliveriesQuery)
        }
    }

    /*
     * Close and remove offline data store
     */
    fun resetOffline(context: Context) {
        try {
            AndroidSystem.setContext(context)
            resetOfflineODataProvider()
            OfflineODataProvider.clear(OFFLINE_DATASTORE)
            OfflineEncryptedSharedPreference.getInstance(context).clear()
        } catch (e: OfflineODataException) {
            logger.error("Unable to reset Offline Data Store. Encountered exception: " + e.message)
        }
        progressListeners.clear()
    }

    fun open(context: Context): UUID {
        logger.debug("start open offline store.")
        if (FlowContextRegistry.flowContext.getCurrentUser() == null) {
            error("Current user not ready yet.")
        }

        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val openRequest = OneTimeWorkRequestBuilder<OfflineOpenWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OFFLINE_OPEN_WORKER_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            openRequest
        )

        return openRequest.id
    }

    fun sync(context: Context): UUID {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OFFLINE_SYNC_WORKER_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )

        return syncRequest.id
    }
}


abstract class OfflineProgressListener() {
    enum class WorkerType{
        OPEN, SYNC
    }

    private var previousStep = 0
    val totalStepsForTwoProgresses = 40

    fun onOfflineProgress(
        progress: OfflineODataProviderOperationProgress
    ) {
        if (progress.currentStepNumber > previousStep) {
            previousStep = progress.currentStepNumber
            if (workerType == WorkerType.OPEN) {
                updateProgress(progress.currentStepNumber, progress.totalNumberOfSteps)
            } else {
                /*
                 * The half of totalStepsForTwoProgresses is for first progress, the other half is for second progress.
                 * To make two progresses as one progress, the current step number needs to be calculated.
                 * For example, totalStepsForTwoProgresses is 40, then first progress will proceed from step 0 to step 20, and the second one will proceed from step 20 to step 40.
                 * So getStartPoint will be 0 for the first progress and 20 for the second progress.
                 * If first progress completes by 20% (i.e. getCurrentStepNumber / getTotalNumberOfSteps = 20%), the overall progress will be 4/40.
                 * If second progress completes by 20%, the overall progress will be 24/40.
                 */
                val currentStepNumber = totalStepsForTwoProgresses / 2 * progress.currentStepNumber / progress.totalNumberOfSteps + getStartPoint()
                updateProgress(currentStepNumber, totalStepsForTwoProgresses)
            }
        }
        if (progress.currentStepNumber == progress.totalNumberOfSteps) {
            previousStep = 0
        }
    }

    abstract fun updateProgress(currentStep: Int, totalSteps: Int)
    abstract fun getStartPoint(): Int
    abstract val workerType: WorkerType
}
