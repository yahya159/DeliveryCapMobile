package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.odata.*
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.*
import com.sap.cloud.mobile.fiori.compose.keyvaluecell.model.FioriKeyValueCellContent
import com.sap.cloud.mobile.fiori.compose.keyvaluecell.ui.FioriKeyValueCell
import com.sap.cloud.mobile.fiori.compose.objectheader.model.*
import com.sap.cloud.mobile.fiori.compose.objectheader.ui.FioriObjectHeader
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreenSettings
import com.company.mysapbtpsdkproject.ui.odata.screens.defaultObjectHeaderData
import com.sap.cloud.mobile.fiori.compose.theme.fioriHorizonAttributes
import com.sap.cloud.android.odata.entitycontainer.Deliveries

val DeliveriesEntityDetailScreen: @Composable (
    onNavigateProperty: (EntityValue, Property, IEntityOperationType) -> Unit,
    navigateUp: (() -> Unit)?,
    viewModel: ODataViewModel<EntityValue>,
    isExpandedScreen: Boolean
) -> Unit = { _, navigateUp, odataViewModel, isExpandedScreen ->
    val viewModel = odataViewModel as EntityViewModel
    val uiState by viewModel.odataUIState.collectAsState()
    val imageByteArray by viewModel.loadMasterEntityMedia().collectAsState(initial = null)

    val deleteConfirm = remember {
        mutableStateOf(false)
    }

    DeleteEntityWithConfirmation(viewModel, deleteConfirm)

    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = screenTitle(getEntityScreenInfo(viewModel.entityType, viewModel.entitySet), ScreenType.Details),
            actionItems = uiState.masterEntity?.let { listOf(
                ActionItem(
                    nameRes = R.string.menu_update,
                    iconRes = R.drawable.ic_sap_icon_edit,
                    overflowMode = OverflowMode.IF_NECESSARY,
                    doAction = viewModel::onEditAction
                ),
                ActionItem(
                    nameRes = R.string.menu_delete,
                    iconRes = R.drawable.ic_sap_icon_delete,
                    overflowMode = OverflowMode.IF_NECESSARY,
                    doAction = { deleteConfirm.value = true }
                ),
            )} ?: emptyList(),
            navigateUp = if (isExpandedScreen) null else navigateUp,
        ),
        modifier = Modifier,
        viewModel = viewModel
    ) {
        Column {
            val entity = uiState.masterEntity
            if (entity != null) {
                FioriObjectHeader(
                    modifier = Modifier.fillMaxWidth(),
                    primaryPage = defaultObjectHeaderData(
                        title = viewModel.getEntityTitle(entity),
                        imageByteArray = imageByteArray,
                        imageChars = viewModel.getAvatarText(entity)
                    ),
                    statusLayout = FioriObjectHeaderStatusLayout.Inline
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.id.name,
                                value = entity.getOptionalValue(Deliveries.id)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.createdAt.name,
                                value = entity.getOptionalValue(Deliveries.createdAt)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.createdBy.name,
                                value = entity.getOptionalValue(Deliveries.createdBy)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.modifiedAt.name,
                                value = entity.getOptionalValue(Deliveries.modifiedAt)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.modifiedBy.name,
                                value = entity.getOptionalValue(Deliveries.modifiedBy)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.orderNo.name,
                                value = entity.getOptionalValue(Deliveries.orderNo)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.customerName.name,
                                value = entity.getOptionalValue(Deliveries.customerName)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.customerPhone.name,
                                value = entity.getOptionalValue(Deliveries.customerPhone)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.deliveryAddress.name,
                                value = entity.getOptionalValue(Deliveries.deliveryAddress)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.status.name,
                                value = entity.getOptionalValue(Deliveries.status)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.driverEmail.name,
                                value = entity.getOptionalValue(Deliveries.driverEmail)?.toString() ?: ""
                            )
                        )

                    FioriKeyValueCell(
                        content = FioriKeyValueCellContent(
                                key = Deliveries.note.name,
                                value = entity.getOptionalValue(Deliveries.note)?.toString() ?: ""
                            )
                        )

                }
            }
        }
    }
}

