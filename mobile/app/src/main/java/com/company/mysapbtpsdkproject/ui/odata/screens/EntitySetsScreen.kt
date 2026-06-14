package com.company.mysapbtpsdkproject.ui.odata.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.company.mysapbtpsdkproject.ui.odata.ActionItem
import com.company.mysapbtpsdkproject.ui.AlertDialogComponent
import com.company.mysapbtpsdkproject.ui.launchWelcomeActivity
import com.company.mysapbtpsdkproject.ui.odata.EntityScreenInfo
import com.company.mysapbtpsdkproject.ui.odata.OverflowMode
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntitySetViewModel
import com.sap.cloud.mobile.fiori.compose.avatar.model.FioriAvatarConstruct
import com.sap.cloud.mobile.fiori.compose.avatar.model.FioriAvatarData
import com.sap.cloud.mobile.fiori.compose.avatar.model.FioriAvatarType
import com.sap.cloud.mobile.fiori.compose.common.FioriImage
import com.sap.cloud.mobile.fiori.compose.objectcell.model.FioriObjectCellData
import com.sap.cloud.mobile.fiori.compose.objectcell.ui.FioriObjectCell
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.flows.compose.flows.FlowType
import com.sap.cloud.mobile.flows.compose.flows.FlowUtil
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType

@Composable
fun EntitySetScreen(
    list: List<EntityScreenInfo>,
    onRowClick: (EntityType) -> Unit,
    modifier: Modifier = Modifier,
    navigateToSettings: () -> Unit
) {
    val viewModel: EntitySetViewModel = viewModel()
    val isMultipleUserMode = viewModel.isMultipleUserMode.collectAsState()

    var startLogout by remember { mutableStateOf(false) }
    var startDeleteRegistration by remember { mutableStateOf(false) }

    if (startLogout) {
        startLogout = false
        StartLogoutFlow()
    }

    if (startDeleteRegistration) {
        DeleteRegistration { startDeleteRegistration = false }
    }

    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = stringResource(id = R.string.application_name),
            actionItems = listOf(
                ActionItem(
                    nameRes = R.string.menu_item_settings,
                    overflowMode = OverflowMode.ALWAYS_OVERFLOW,
                    doAction = navigateToSettings
                ),
                ActionItem(
                    nameRes = R.string.synchronize_action,
                    overflowMode = OverflowMode.ALWAYS_OVERFLOW,
                    doAction = viewModel::startSync
                ),
                ActionItem(
                    nameRes = R.string.logout,
                    overflowMode = OverflowMode.ALWAYS_OVERFLOW,
                    doAction = { startLogout = true },
                ),
                ActionItem(
                    nameRes = R.string.delete_registration,
                    overflowMode = if (isMultipleUserMode.value) OverflowMode.ALWAYS_OVERFLOW else OverflowMode.NOT_SHOWN,
                    doAction = { startDeleteRegistration = true },
                ),
            )
        ),
        modifier = modifier,
        viewModel = viewModel
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(list) { item ->
                val objectCellData = FioriObjectCellData.Builder().apply {
                    setHeadline(stringResource(id = item.setTitleId))
                    setAvatar(FioriAvatarConstruct(
                        hasBadge = false,
                        type = FioriAvatarType.SINGLE,
                        avatarList = listOf(
                            FioriAvatarData(
                                image = FioriImage(resId = item.iconId),
                                size = 40.dp,
                            )
                        )
                    ))
                }.build()
                objectCellData.setDisplayReadIndicator(false)

                FioriObjectCell(
                    cellData = objectCellData,
                    onClick = {
                        onRowClick(
                            item.entityType
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DeleteRegistration(dismissDialog: () -> Unit) {
    val context = LocalContext.current

    AlertDialogComponent(
        title = context.getString(
            com.sap.cloud.mobile.onboarding.compose.R.string.dialog_warning_title
        ),
        text = context.getString(R.string.delete_registration_warning),
        onPositiveButtonClick = {
            dismissDialog()
            startDeleteRegFlow(context)
            { resultCode ->
                if (resultCode == Activity.RESULT_OK) {
                    OfflineWorkerUtil.resetOffline(context)
                }
            }

        },
        positiveButtonText = context.getString(R.string.yes),
        onNegativeButtonClick = dismissDialog,
    )
}

private fun startDeleteRegFlow(context: Context, finishCallback: (Int) -> Unit = {}) {
    FlowUtil.startFlow(
        context = context,
        flowContext = FlowContextRegistry.flowContext.copy(
            flowType = FlowType.DeleteRegistration, flow = null
        )
    ) { resultCode, _ ->
        finishCallback(resultCode)
        if (resultCode == Activity.RESULT_OK) {
            launchWelcomeActivity(context)
        }
    }
}

@Composable
fun StartLogoutFlow() {
    val context = LocalContext.current
    FlowUtil.startFlow(
        context = context,
        flowContext = FlowContextRegistry.flowContext.copy(
            flowType = FlowType.Logout, flow = null
        )
    ) { resultCode, _ ->
        if (resultCode == Activity.RESULT_OK) {
            launchWelcomeActivity(context)
        }
    }
}

@Preview
@Composable
fun EntitySetScreenPreview() {
    val entityTypeNames = EntityScreenInfo.entries
    EntitySetScreen(entityTypeNames, { println("click $it row") }) {}
}
