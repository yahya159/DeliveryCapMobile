package com.company.mysapbtpsdkproject.ui.odata.screens.deliveries

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.company.mysapbtpsdkproject.R
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import com.company.mysapbtpsdkproject.ui.odata.*
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import com.company.mysapbtpsdkproject.ui.odata.data.EntityMediaResource
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.*
import com.sap.cloud.mobile.fiori.compose.avatar.model.*
import com.sap.cloud.mobile.fiori.compose.common.FioriIcon
import com.sap.cloud.mobile.fiori.compose.common.FioriImage
import com.sap.cloud.mobile.fiori.compose.objectcell.model.*
import com.sap.cloud.mobile.fiori.compose.objectcell.ui.FioriObjectCell
import com.sap.cloud.mobile.fiori.compose.objectcell.ui.FioriObjectCellDefaults
import com.sap.cloud.mobile.fiori.compose.theme.fioriHorizonAttributes
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.onboarding.compose.screens.LoadingItem
import com.company.mysapbtpsdkproject.ui.AlertDialogComponent
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

//TODO: pull down screen to refresh
//https://github.com/aakarshrestha/compose-swipe-to-refresh
//https://google.github.io/accompanist/swiperefresh/
@OptIn(kotlinx.coroutines.FlowPreview::class)
val DeliveriesEntitiesScreen:
    @Composable
        (
        navigateToHome: () -> Unit,
        navigateUp: () -> Unit,
        viewModel: ODataViewModel<EntityValue>,
        isExpandScreen: Boolean,
    ) -> Unit =
{ navigateToHome, navigateUp, odataViewModel, isExpandedScreen ->
    val viewModel = odataViewModel as EntityViewModel
    val entities = viewModel.pagingDataState.value.collectAsLazyPagingItems()
    val uiState by viewModel.odataUIState.collectAsState()
    val context = LocalContext.current

    val listState: LazyListState = rememberLazyListState()

    val isInCreateOrUpdate = remember {
        derivedStateOf {
            isExpandedScreen &&
                uiState.entityOperationType in setOf(
                    EntityOperationType.CREATE,
                    EntityUpdateOperationType.UPDATE_FROM_LIST,
                    EntityUpdateOperationType.UPDATE_FROM_DETAIL
        ) }
    }

    // List screen handle navigation popBackStack, need to implement a debouncing mechanism
    // to prevent the second back press from being processed too quickly
    val backPressFlow = remember { MutableStateFlow(0L) }
    val isLeave = remember { mutableStateOf(false) }
    val navUp = remember {
        {
            isLeave.value = true
            backPressFlow.value = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            backPressFlow
                .debounce(200L) // Adjust debounce time as needed
                .collectLatest {
                    if(isLeave.value) {
                        navigateUp.invoke()
                    }
                }
        }
    }

    BackHandler {
        navUp.invoke()
    }

   // Delete confirmation dialog
    val showDelConfirmDlg = remember {
        mutableStateOf(false)
    }

    DeleteEntityWithConfirmation(viewModel, showDelConfirmDlg)

    // Leave confirmation callback function
    val showLeaveConfirmDlg = remember {
        mutableStateOf(false)
    }

    // Leave dialog confirmation callback
    var onLeaveConfirmed by remember {
        //by default navigateUp, but may be changed to onClickAction in expanded screen mode
        mutableStateOf(navigateUp)
    }

    // app bar navigate up button callback
    val onNavigateUp = remember {
        {
            if(isInCreateOrUpdate.value) {
                showLeaveConfirmDlg.value = isInCreateOrUpdate.value
                onLeaveConfirmed = navUp
            }
            else {
                navUp.invoke()
            }
        }
    }

    // list item click callback
    val onClickChange: (EntityValue) -> Unit = remember {
        { entity ->
            // in expanded screen mode, do nothing while click on current entity
            if (isInCreateOrUpdate.value) {
                if( entity != uiState.masterEntity ) {
                    showLeaveConfirmDlg.value = true
                    onLeaveConfirmed = { viewModel.onClickAction(entity) }
                }
            } else viewModel.onClickAction(entity)
        }
    }

        LeaveEditorWithConfirmation(showLeaveConfirmDlg) {
            viewModel.exitEditor()
            onLeaveConfirmed.invoke()
        }

    val actionItems =
        if (isExpandedScreen) listOf()
        else getSelectedItemActionsList(
            navigateToHome,
            viewModel,
            showDelConfirmDlg
        )

    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = screenTitle(getEntityScreenInfo(viewModel.entityType, viewModel.entitySet), ScreenType.List),
            navigateUp = onNavigateUp,
            actionItems = actionItems,
            floatingActionClick = viewModel.onFloatingAdd(),
            floatingActionIcon = Icons.Filled.Add
        ),
        modifier = Modifier,
        viewModel = viewModel
    ) {
        if (entities.loadState.refresh == LoadState.Loading) {
            LoadingItem()
        } else {
            LazyColumn(state = listState) {
                items(
                    count = entities.itemCount,
                ) { index ->
                    val entity = entities[index] ?: return@items
                    val selected = uiState.selectedItems.contains(entity)
                    val avatar = FioriAvatarConstruct(
                        hasBadge = false,
                        type = FioriAvatarType.SINGLE,
                        avatarList = listOf(
                            if (!selected) {
                                if (EntityMediaResource.hasMediaResources(entity.entityType)) {
                                        when (val media =
                                            loadMedia(entity, viewModel).value) {
                                            is ResultOf.Success -> FioriAvatarData(
                                                FioriImage(media.bitmap),
                                                shape = FioriAvatarShape.ROUNDEDCORNER
                                            )
                                            is ResultOf.Loading -> FioriAvatarData(
                                                FioriImage(
                                                    ResourcesCompat.getDrawable(
                                                        context.resources,
                                                        R.drawable.ic_downloading,
                                                        context.theme
                                                    )!!.toBitmap()
                                                ),
                                                shape = FioriAvatarShape.ROUNDEDCORNER
                                            )
                                            else -> FioriAvatarData(
                                                text = viewModel.getAvatarText(entity).uppercase(),
                                                textColor = MaterialTheme.fioriHorizonAttributes.SapFioriColorBaseText
                                            )
                                        }
                                } else FioriAvatarData(
                                    text = viewModel.getAvatarText(entity).uppercase(),
                                    textColor = MaterialTheme.fioriHorizonAttributes.SapFioriColorBaseText
                                )
                            } else FioriAvatarData(
                                FioriImage(resId = R.drawable.ic_sap_icon_done),
                                color = MaterialTheme.fioriHorizonAttributes.SapFioriColorHeaderCaption,
                                size = 40.dp,
                            )
                        ),
                        size = 40.dp,
                        shape = FioriAvatarShape.CIRCLE,
//                      backgroundColor = MaterialTheme.fioriHorizonAttributes.SapFioriColorS6
                    )
                    val stateIcon = getEntityStateIcon(entity)
                    val objectCellData = FioriObjectCellData.Builder().apply {
                        setHeadline(viewModel.getEntityTitle(entity))
                        setIconStack(listOf(
                            IconStackElement(
                                FioriIcon(
                                    resId = stateIcon.icon,
                                    contentDescription = stringResource(id = stateIcon.desc),
                                    tint = Color.Unspecified
                                )
                            ),
                            IconStackElement(
                                FioriIcon(
                                    resId = com.sap.cloud.mobile.fiori.compose.R.drawable.avatar_badge,
                                    contentDescription = stringResource(id = stateIcon.desc),
                                    tint = MaterialTheme.fioriHorizonAttributes.SapFioriColorSectionDivider
                                )
                            )
                        ))
                        setSubheadline("Subtitle goes here")
                        setFootnote("caption display")
                        setAvatar(avatar)
                    }.build()
                    objectCellData.setDisplayReadIndicator(false)

                    FioriObjectCell(
                        cellData = objectCellData,
                        colors = FioriObjectCellDefaults.colors(),
                        textStyles = FioriObjectCellDefaults.textStyles(),
                        styles = FioriObjectCellDefaults.styles(iconStackSize = 10.dp),
                        onClick = { onClickChange(entity) },
                        onLongPress = { viewModel.onSelectAction(entity) }
                    )
                }
            }
        }
    }
}
