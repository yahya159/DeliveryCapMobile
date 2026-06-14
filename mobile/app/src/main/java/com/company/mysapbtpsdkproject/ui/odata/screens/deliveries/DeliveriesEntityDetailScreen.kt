package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.odata.*
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.*
import com.sap.cloud.mobile.fiori.compose.keyvaluecell.model.FioriKeyValueCellContent
import com.sap.cloud.mobile.fiori.compose.keyvaluecell.ui.FioriKeyValueCell
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreenSettings
import com.sap.cloud.android.odata.entitycontainer.Deliveries

val DeliveriesEntityDetailScreen: @Composable (
    onNavigateProperty: (EntityValue, Property, IEntityOperationType) -> Unit,
    navigateUp: (() -> Unit)?,
    viewModel: ODataViewModel<EntityValue>,
    isExpandedScreen: Boolean
) -> Unit = { _, navigateUp, odataViewModel, isExpandedScreen ->
    val viewModel = odataViewModel as EntityViewModel
    val uiState by viewModel.odataUIState.collectAsState()

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
        val entity = uiState.masterEntity
        if (entity != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Clean header: customer name + status, replacing the default object header.
                Text(
                    text = entity.getOptionalValue(Deliveries.customerName)?.toString() ?: "",
                    style = MaterialTheme.typography.headlineSmall
                )
                val status = entity.getOptionalValue(Deliveries.status)?.toString()
                if (!status.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                detailCell(Deliveries.orderNo.name, entity.getOptionalValue(Deliveries.orderNo)?.toString())
                detailCell(Deliveries.customerPhone.name, entity.getOptionalValue(Deliveries.customerPhone)?.toString())
                detailCell(Deliveries.deliveryAddress.name, entity.getOptionalValue(Deliveries.deliveryAddress)?.toString())
                detailCell(Deliveries.driverEmail.name, entity.getOptionalValue(Deliveries.driverEmail)?.toString())
                detailCell(Deliveries.note.name, entity.getOptionalValue(Deliveries.note)?.toString())
                detailCell(Deliveries.id.name, entity.getOptionalValue(Deliveries.id)?.toString())

                // Read-only location preview + open-in-external-app actions.
                LocationSection(
                    latitude = entity.getOptionalValue(Deliveries.latitude)?.toString()?.toDoubleOrNull(),
                    longitude = entity.getOptionalValue(Deliveries.longitude)?.toString()?.toDoubleOrNull(),
                    editable = false
                )
            }
        }
    }
}

@Composable
private fun detailCell(key: String, value: String?) {
    FioriKeyValueCell(
        content = FioriKeyValueCellContent(
            key = key,
            value = value ?: ""
        )
    )
}
