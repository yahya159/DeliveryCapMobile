package com.company.mysapbtpsdkproject.app

import android.app.Application
import com.sap.cloud.mobile.foundation.app.security.ClipboardProtectionService
import com.company.mysapbtpsdkproject.data.SharedPreferenceRepository
import com.company.mysapbtpsdkproject.repository.RepositoryFactory
import com.sap.cloud.mobile.foundation.app.security.LockAndWipeService
import android.content.Intent
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.WelcomeActivity.Companion.EXTRA_WAINING_KEY
import com.company.mysapbtpsdkproject.ui.launchWelcomeActivity
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.flows.compose.flows.FlowType
import com.sap.cloud.mobile.flows.compose.flows.FlowUtil
import com.sap.cloud.mobile.flows.compose.flows.FlowUtil.populateCustomBundle
import com.sap.cloud.mobile.flows.compose.flows.FlowUtil.skipConfirmForDeleteRegistrationFlow
import com.sap.cloud.mobile.foundation.app.security.ServerBlockType
import com.sap.cloud.mobile.foundation.mobileservices.MobileService
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer
import com.company.mysapbtpsdkproject.BuildConfig
import com.sap.cloud.mobile.permission.request.tracker.PermissionRequestTracker
import com.sap.cloud.mobile.foundation.logging.LoggingService
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import com.sap.cloud.mobile.foundation.theme.ThemeDownloadService

import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.sap.cloud.mobile.foundation.settings.SharedDeviceService

class SAPWizardApplication: Application() {


    override fun onCreate() {
        super.onCreate()
        initServices()
        if (BuildConfig.FLAVOR == "tencentAppStoreforChinaMarket") {
            PermissionRequestTracker.setPermissionRequestTrackingEnabled(this,true)
        } else {
            PermissionRequestTracker.setPermissionRequestTrackingEnabled(this,false)
        }
    }

    /**
     * Clears all user-specific data and configuration from the application, essentially resetting it to its initial
     * state.
     *
     * If client code wants to handle the reset logic of a service, here is an example:
     *
     *   SDKInitializer.resetServices { service ->
     *       return@resetServices if( service is PushService ) {
     *           PushService.unregisterPushSync(object: CallbackListener {
     *               override fun onSuccess() {
     *               }
     *
     *               override fun onError(p0: Throwable) {
     *               }
     *           })
     *           true
     *       } else {
     *           false
     *       }
     *   }
     */
    suspend fun resetApplication() {
        SharedPreferenceRepository(this).resetSharedPreference()
        RepositoryFactory.reset()
        SDKInitializer.resetServices()
        OfflineWorkerUtil.resetOffline(this)

    }

    private fun initServices() {
        val services = mutableListOf<MobileService>()
        services.add(ThemeDownloadService(this))
        services.add(ClipboardProtectionService(true))
        services.add(LockAndWipeService {
            when (it) {
                ServerBlockType.TRAFFIC_REG_WIPED -> {
                    FlowUtil.startFlow(
                        context = this,
                        flowContext = FlowContextRegistry.flowContext.copy(
                            flowType = FlowType.DeleteRegistration
                        ),
                        updateIntent = { intent ->
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.populateCustomBundle {
                                skipConfirmForDeleteRegistrationFlow(true)
                            }
                        }
                    ) { _, _ ->
                        OfflineWorkerUtil.resetOffline(this@SAPWizardApplication)
                        launchWelcomeActivity(this@SAPWizardApplication) { intent ->
                            intent.putExtra(
                                EXTRA_WAINING_KEY,
                                getString(R.string.warning_wiped_msg)
                            )
                        }
                    }
                }

                ServerBlockType.TRAFFIC_REG_LOCKED -> {
                    launchWelcomeActivity(this@SAPWizardApplication) { intent ->
                        intent.putExtra(
                            EXTRA_WAINING_KEY,
                            getString(com.sap.cloud.mobile.flows.compose.R.string.reg_locked_by_policy)
                        )
                    }
                }

                else -> {
                    launchWelcomeActivity(this@SAPWizardApplication) { intent ->
                        intent.putExtra(
                            EXTRA_WAINING_KEY,
                            getString(com.sap.cloud.mobile.flows.compose.R.string.reg_locked_by_admin)
                        )
                    }
                }
            }
        })
        services.add(LoggingService(autoUpload = false).apply {
            policy = LogPolicy(logLevel = "WARN", entryExpiry = 0, maxFileNumber = 4)
            logToConsole = true
        })
        services.add(SharedDeviceService(OFFLINE_APP_ENCRYPTION_CONSTANT))

        SDKInitializer.start(this, * services.toTypedArray())
    }


    companion object {
        private const val OFFLINE_APP_ENCRYPTION_CONSTANT = "34dab53fc060450280faeed44a36571b"
    }
}
