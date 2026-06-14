package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.company.mysapbtpsdkproject.ui.odata.ScreenType
import com.company.mysapbtpsdkproject.ui.odata.getEntityScreenInfo
import com.company.mysapbtpsdkproject.ui.odata.screenTitle
import com.company.mysapbtpsdkproject.ui.odata.screens.DeleteEntityWithConfirmation
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationScreenSettings
import com.company.mysapbtpsdkproject.ui.odata.screens.getSelectedItemActionsList
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityOperationType
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityUpdateOperationType
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityViewModel
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.IEntityOperationType
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.ODataViewModel
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property

val DeliveriesEntitiesExpandScreen:
        @Composable (
            navigateToHome: () -> Unit,
            navigateUp: () -> Unit,
            onNavigateProperty: (EntityValue, Property, IEntityOperationType) -> Unit,
            viewModel: ODataViewModel<EntityValue>,
        ) -> Unit =
    { navigateToHome, navigateUp, onNavigateProperty, odataViewModel ->
        val viewModel = odataViewModel as EntityViewModel
        val uiState by viewModel.odataUIState.collectAsState()
        Row(modifier = Modifier) {
            Box(modifier = Modifier.weight(1f)) {
                DeliveriesEntitiesScreen(
                    navigateToHome,
                    navigateUp,
                    viewModel,
                    true
                )
            }
            Box(modifier = Modifier.weight(2f)) {
                when (uiState.entityOperationType) {
                    EntityOperationType.DETAIL -> {
                        DeliveriesEntityDetailScreen(onNavigateProperty, null, viewModel, true)
                    }

                    EntityOperationType.CREATE, EntityUpdateOperationType.UPDATE_FROM_LIST, EntityUpdateOperationType.UPDATE_FROM_DETAIL -> {
                        DeliveriesEntityEditScreen(onNavigateProperty, null, viewModel, true)
                    }

                    else -> {
                        DeliveriesBlankScreen(viewModel)
                    }
                }
            }
        }
    }

val DeliveriesBlankScreen:
        @Composable (
            viewModel: EntityViewModel,
        ) -> Unit =
    { viewModel ->
    val deleteState = remember {
        mutableStateOf(false)
    }
    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = screenTitle(
                getEntityScreenInfo(viewModel.entityType, viewModel.entitySet),
                ScreenType.Details
            ),
            actionItems = getSelectedItemActionsList(
                viewModel,
                deleteState
            ),
            navigateUp = null,
        ),
        modifier = Modifier,
        viewModel = viewModel
    ) {
        Box(modifier = Modifier)
        DeleteEntityWithConfirmation(viewModel, deleteState)
    }
}
