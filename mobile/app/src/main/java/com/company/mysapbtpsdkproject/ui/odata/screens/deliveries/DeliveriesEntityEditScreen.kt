package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.sap.cloud.mobile.fiori.compose.text.model.FioriTextFieldContent
import com.sap.cloud.mobile.fiori.compose.text.ui.FioriSimpleTextField
import com.sap.cloud.mobile.fiori.compose.theme.fioriHorizonAttributes
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreenSettings
import com.sap.cloud.android.odata.entitycontainer.Deliveries

val DeliveriesEntityEditScreen: @Composable (
    onNavigateProperty: (EntityValue, Property, IEntityOperationType) -> Unit,
    navigateUp: (() -> Unit)?,
    viewModel: ODataViewModel<EntityValue>,
    isExpandedScreen: Boolean
) -> Unit = { onNavigateProperty, navigateUp, odataViewModel, isExpandedScreen ->
    val viewModel = odataViewModel as EntityViewModel
    val odataUIState by viewModel.odataUIState.collectAsState()
    val masterEntity = odataUIState.masterEntity!!
    val fieldStates = odataUIState.editorFiledStates
    val isCreation = odataUIState.entityOperationType == EntityOperationType.CREATE
    val isNavigateUp = remember {
        mutableStateOf(false)
    }

    if (isNavigateUp.value) {
        LeaveEditorWithConfirmation(isNavigateUp, navigateUp!!)
    }

    BackHandler(!isExpandedScreen) {
        isNavigateUp.value = true
    }

    val actions = listOf(
        ActionItem(
            nameRes = R.string.save,
            iconRes = R.drawable.ic_sap_icon_done,
            overflowMode = OverflowMode.IF_NECESSARY,
            enabled = fieldStates.none { it.isError },
            doAction = {
                viewModel.onSaveAction(
                    masterEntity,
                    fieldStates.map { Pair(it.property, it.value) })

            }),
    )

    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = screenTitle(
                getEntityScreenInfo(viewModel.entityType, viewModel.entitySet),
                if (isCreation) ScreenType.Create else ScreenType.Update
            ),
            navigateUp = if (isExpandedScreen) null else ({ isNavigateUp.value = true }),
            actionItems = actions
        ),
        modifier = Modifier,
        viewModel = viewModel
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp)
        ) {
            // add the non-computed properties to the list
            // key properties are only shown in creation mode
            if (isCreation) {
            item {
                val iDIndex = fieldStates.indexOfFirst { it.property.name == "ID" }
                if (iDIndex >= 0) {
                    val iDState = fieldStates[iDIndex]
                    FioriSimpleTextField(
                        value = iDState.value,
                        onValueChange = { viewModel.updateFieldState(iDIndex, it) },
                        content = FioriTextFieldContent(
                            label = iDState.property.name,
                            required = !iDState.property.isNullable,
                            errorMessage = iDState.errorMessage
                        ),
                        isError = iDState.isError,
                    )
                }
            }
            }
            item {
                val orderNoIndex = fieldStates.indexOfFirst { it.property.name == "orderNo" }
                if (orderNoIndex >= 0) {
                    val orderNoState = fieldStates[orderNoIndex]
                    FioriSimpleTextField(
                        value = orderNoState.value,
                        onValueChange = { viewModel.updateFieldState(orderNoIndex, it) },
                        content = FioriTextFieldContent(
                            label = orderNoState.property.name,
                            required = !orderNoState.property.isNullable,
                            errorMessage = orderNoState.errorMessage
                        ),
                        isError = orderNoState.isError,
                    )
                }
            }
            item {
                val customerNameIndex = fieldStates.indexOfFirst { it.property.name == "customerName" }
                if (customerNameIndex >= 0) {
                    val customerNameState = fieldStates[customerNameIndex]
                    FioriSimpleTextField(
                        value = customerNameState.value,
                        onValueChange = { viewModel.updateFieldState(customerNameIndex, it) },
                        content = FioriTextFieldContent(
                            label = customerNameState.property.name,
                            required = !customerNameState.property.isNullable,
                            errorMessage = customerNameState.errorMessage
                        ),
                        isError = customerNameState.isError,
                    )
                }
            }
            item {
                val customerPhoneIndex = fieldStates.indexOfFirst { it.property.name == "customerPhone" }
                if (customerPhoneIndex >= 0) {
                    val customerPhoneState = fieldStates[customerPhoneIndex]
                    FioriSimpleTextField(
                        value = customerPhoneState.value,
                        onValueChange = { viewModel.updateFieldState(customerPhoneIndex, it) },
                        content = FioriTextFieldContent(
                            label = customerPhoneState.property.name,
                            required = !customerPhoneState.property.isNullable,
                            errorMessage = customerPhoneState.errorMessage
                        ),
                        isError = customerPhoneState.isError,
                    )
                }
            }
            item {
                val deliveryAddressIndex = fieldStates.indexOfFirst { it.property.name == "deliveryAddress" }
                if (deliveryAddressIndex >= 0) {
                    val deliveryAddressState = fieldStates[deliveryAddressIndex]
                    FioriSimpleTextField(
                        value = deliveryAddressState.value,
                        onValueChange = { viewModel.updateFieldState(deliveryAddressIndex, it) },
                        content = FioriTextFieldContent(
                            label = deliveryAddressState.property.name,
                            required = !deliveryAddressState.property.isNullable,
                            errorMessage = deliveryAddressState.errorMessage
                        ),
                        isError = deliveryAddressState.isError,
                    )
                }
            }
            item {
                val statusIndex = fieldStates.indexOfFirst { it.property.name == "status" }
                if (statusIndex >= 0) {
                    val statusState = fieldStates[statusIndex]
                    FioriSimpleTextField(
                        value = statusState.value,
                        onValueChange = { viewModel.updateFieldState(statusIndex, it) },
                        content = FioriTextFieldContent(
                            label = statusState.property.name,
                            required = !statusState.property.isNullable,
                            errorMessage = statusState.errorMessage
                        ),
                        isError = statusState.isError,
                    )
                }
            }
            item {
                val driverEmailIndex = fieldStates.indexOfFirst { it.property.name == "driverEmail" }
                if (driverEmailIndex >= 0) {
                    val driverEmailState = fieldStates[driverEmailIndex]
                    FioriSimpleTextField(
                        value = driverEmailState.value,
                        onValueChange = { viewModel.updateFieldState(driverEmailIndex, it) },
                        content = FioriTextFieldContent(
                            label = driverEmailState.property.name,
                            required = !driverEmailState.property.isNullable,
                            errorMessage = driverEmailState.errorMessage
                        ),
                        isError = driverEmailState.isError,
                    )
                }
            }
            item {
                val noteIndex = fieldStates.indexOfFirst { it.property.name == "note" }
                if (noteIndex >= 0) {
                    val noteState = fieldStates[noteIndex]
                    FioriSimpleTextField(
                        value = noteState.value,
                        onValueChange = { viewModel.updateFieldState(noteIndex, it) },
                        content = FioriTextFieldContent(
                            label = noteState.property.name,
                            required = !noteState.property.isNullable,
                            errorMessage = noteState.errorMessage
                        ),
                        isError = noteState.isError,
                    )
                }
            }

        }
    }
}
