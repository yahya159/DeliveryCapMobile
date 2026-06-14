package com.company.mysapbtpsdkproject.app

import android.content.Intent
import com.sap.cloud.mobile.flows.compose.core.FlowContext
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry.flowContext
import com.sap.cloud.mobile.flows.compose.ext.FlowStateListener
import com.sap.cloud.mobile.flows.compose.flows.FlowTypeName
import com.sap.cloud.mobile.foundation.model.AppConfig
import com.company.mysapbtpsdkproject.ui.launchWelcomeActivity
import com.company.mysapbtpsdkproject.ui.launchMainBusinessActivity
import android.widget.Toast
import ch.qos.logback.classic.Level
import com.sap.cloud.mobile.foundation.settings.policies.ClientPolicies
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import org.slf4j.LoggerFactory
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.data.SharedPreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.*
import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.sap.cloud.mobile.foundation.app.security.LockWipeActionType
import com.sap.cloud.mobile.flows.compose.db.UserSecureStoreDelegate
import com.company.mysapbtpsdkproject.service.OfflineEncryptedSharedPreference


class WizardFlowStateListener(private val application: SAPWizardApplication) :
    FlowStateListener() {

    override suspend fun onAppConfigRetrieved(appConfig: AppConfig) {
        logger.debug("onAppConfigRetrieved: {}", appConfig)
    }

    override suspend fun onApplicationReset() {
        this.application.resetApplication()
    }

    override suspend fun onOfflineKeyReady(flowName: String, key: String?) {
        if(flowName == FlowTypeName.FORGET_PASSCODE.name &&
            !UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserMode()) {
            // In single-user mode, the FORGET_PASSCODE flow will trigger a reset of the offline key.
            OfflineWorkerUtil.resetOffline(application)
        }
    }

    override suspend fun onApplicationLocked() {
        super.onApplicationLocked()
    }

    override suspend fun onFlowFinishedWithData(flowName: String?, data: Intent?) {
        when (flowName) {
            FlowTypeName.RESET.name, FlowTypeName.LOGOUT.name -> {
                launchWelcomeActivity(application)
            }
            FlowTypeName.DELETE_REGISTRATION.name -> {
                val isMultipleUserMode =
                    UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserMode()
                val currentUser = flowContext.getCurrentUser()!!.id
                if (!isMultipleUserMode || OfflineEncryptedSharedPreference.getInstance(application)
                        .getOfflineDBUser() == currentUser
                ) {
                    OfflineWorkerUtil.resetOffline(application)
                }
                launchWelcomeActivity(application)
            }
            FlowTypeName.FORGET_PASSCODE.name -> {
                launchMainBusinessActivity(application)
            }
            else -> {
                if (flowContext.isUserSwitch()) {
                    logger.debug("User switch onFlowFinished.")
                    launchMainBusinessActivity(application)
                }
            }
        }
    }

    override suspend fun onClientPolicyRetrieved(policies: ClientPolicies) {
        policies.logPolicy?.also { logSettings ->
            val preferenceRepository = SharedPreferenceRepository(application)
            val currentSettings =
                preferenceRepository.userPreferencesFlow.first().logSetting

            if (currentSettings.logLevel != logSettings.logLevel) {
                preferenceRepository.updateLogLevel(LogPolicy.getLogLevel(logSettings))

                val logString = when (LogPolicy.getLogLevel(logSettings)) {
                    Level.ALL -> application.getString(R.string.log_level_path)
                    Level.INFO -> application.getString(R.string.log_level_info)
                    Level.WARN -> application.getString(R.string.log_level_warning)
                    Level.ERROR -> application.getString(R.string.log_level_error)
                    Level.OFF -> application.getString(R.string.log_level_none)
                    else -> application.getString(R.string.log_level_debug)
                }

                logger.info(
                    String.format(
                        application.getString(R.string.log_level_changed),
                        logString
                    )
                )

                MainScope().launch {
                    Toast.makeText(
                        application,
                        String.format(
                            application.getString(R.string.log_level_changed),
                            logString
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WizardFlowStateListener::class.java)
    }
}

fun FlowContext.isUserSwitch(): Boolean {
    return getPreviousUser()?.let {
        getCurrentUser()!!.id != it.id
    } ?: false
}
