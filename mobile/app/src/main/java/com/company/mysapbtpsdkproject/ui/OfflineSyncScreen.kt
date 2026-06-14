package com.company.mysapbtpsdkproject.ui

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.AlertDialogComponent
import com.sap.cloud.mobile.onboarding.compose.screens.OfflineNetworkIssueScreen
import com.sap.cloud.mobile.onboarding.compose.screens.OfflineSyncScreen
import com.sap.cloud.mobile.onboarding.compose.screens.OfflineTransactionIssueScreen

@Composable
fun OfflineInitScreen(
    viewModel: OfflineOpenViewModel = viewModel(),
    navigateToEntityList: () -> Unit,
    navigateToWelcome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var cancelDialog by remember { mutableStateOf(false) }

    val dismissCancelDialog = remember { { cancelDialog = false } }
    val showCancelDialog = remember { { cancelDialog = true } }
    val cancelSyncAndNavigateUp = remember {
        {
            viewModel.cancelSync()
            navigateToWelcome.invoke()
        }
    }

    OfflineSyncScreen(
        progress = {uiState.progress},
        onBackButtonClick = showCancelDialog
    )
    val status = uiState.result
    if (status is OpenResult.OpenFail) {
        when (status.error) {
            SyncFailureType.NETWORK_ERROR -> {
                OfflineNetworkIssueScreen(
                    onBackButtonClick = navigateToWelcome,
                    onMainButtonClick = viewModel::startOpenOffline
                )
            }
            SyncFailureType.TRANSACTION_ISSUE -> {
                viewModel.previousUser?.let {
                    OfflineTransactionIssueScreen(
                        userName = it.name,
                        email = it.email,
                        onBackButtonClick = navigateToWelcome,
                        onMainButtonClick = navigateToWelcome
                    )
                }
                    ?: throw IllegalStateException("Unexpected offline transaction issue without previous user")
            }
            else -> {
                val errorMessage = status.message
                AlertDialogComponent(
                    stringResource(id = R.string.offline_initial_open_error),
                    errorMessage!!,
                    navigateToWelcome
                )
            }
        }
    }

    if (cancelDialog) {
        AlertDialogComponent(
            title = stringResource(R.string.offline_navigation_dialog_title),
            text = stringResource(R.string.offline_navigation_dialog_message),
            onPositiveButtonClick = cancelSyncAndNavigateUp,
            positiveButtonText = stringResource(R.string.offline_navigation_dialog_positive_option),
            onNegativeButtonClick = dismissCancelDialog,
            negativeButtonText = stringResource(R.string.offline_navigation_dialog_negative_option),
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    if (status is OpenResult.OpenSuccess) {
        navigateToEntityList.invoke()
    }

}
