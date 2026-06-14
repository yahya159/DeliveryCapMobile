package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.odata.*
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.*
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
    val palette = rememberDeliveryPalette()

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
            fun value(p: Property): String = entity.getOptionalValue(p)?.toString() ?: ""
            val name = value(Deliveries.customerName)
            val status = entity.getOptionalValue(Deliveries.status)?.toString()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.screenBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = name.ifEmpty { "Delivery" },
                    color = palette.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                StatusTrackerCard(status, palette)

                InfoCard("Customer", palette) {
                    InfoRow("Name", name, palette, Icons.Filled.Person)
                    InfoRow("Phone", value(Deliveries.customerPhone), palette, Icons.Filled.Phone)
                }

                InfoCard("Delivery", palette) {
                    InfoRow("Address", value(Deliveries.deliveryAddress), palette, Icons.Filled.LocationOn)
                    InfoRow("Order no.", value(Deliveries.orderNo), palette, Icons.Filled.ShoppingCart)
                }

                InfoCard("Assignment", palette) {
                    InfoRow("Driver", value(Deliveries.driverEmail), palette, Icons.Filled.Email)
                }

                if (value(Deliveries.note).isNotBlank()) {
                    InfoCard("Note", palette) {
                        InfoRow("Instructions", value(Deliveries.note), palette, Icons.Filled.Info)
                    }
                }

                // Read-only map preview + open-in-external-app actions.
                LocationSection(
                    latitude = entity.getOptionalValue(Deliveries.latitude)?.toString()?.toDoubleOrNull(),
                    longitude = entity.getOptionalValue(Deliveries.longitude)?.toString()?.toDoubleOrNull(),
                    editable = false
                )

                InfoCard("Reference", palette) {
                    InfoRow("ID", value(Deliveries.id), palette)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
