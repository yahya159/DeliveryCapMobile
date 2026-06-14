package com.company.mysapbtpsdkproject.ui.odata

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.company.mysapbtpsdkproject.R
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntityTypes
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata
import com.company.mysapbtpsdkproject.ui.odata.screens.deliveries.DeliveriesEntitiesScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.deliveries.DeliveriesEntityEditScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.deliveries.DeliveriesEntityDetailScreen
import com.company.mysapbtpsdkproject.ui.odata.screens.deliveries.DeliveriesEntitiesExpandScreen
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.IEntityOperationType
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.ODataViewModel
import com.sap.cloud.mobile.kotlin.odata.ComplexType
import com.sap.cloud.mobile.kotlin.odata.ComplexValue
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StructureBase

interface IScreenInfo<T : StructureBase> {
    val setTitleId: Int
    val itemTitleId: Int
    val entityExpandScreen: @Composable (
        navigateToHome: () -> Unit,
        navigateUp: () -> Unit,
        onNavigateProperty: (StructureBase, Property, IEntityOperationType) -> Unit,
        viewModel: ODataViewModel<T>,
    ) -> Unit
    val entityListScreen: @Composable (
        navigateToHome: () -> Unit,
        navigateUp: () -> Unit,
        viewModel: ODataViewModel<T>,
        isExpandedScreen: Boolean
    ) -> Unit
    val entityEditScreen: @Composable (
        onNavigateProperty: (StructureBase, Property, IEntityOperationType) -> Unit,
        navigateUp: () -> Unit, viewModel: ODataViewModel<T>, isExpandedScreen: Boolean
    ) -> Unit
    val entityDetailScreen: @Composable (
        onNavigateProperty: (StructureBase, Property, IEntityOperationType) -> Unit, navigateUp: () -> Unit, viewModel: ODataViewModel<T>, isExpandedScreen: Boolean
    ) -> Unit
}

enum class ComplexScreenInfo(
    val complexType: ComplexType,
    override val setTitleId: Int,
    override val itemTitleId: Int,
    override val entityExpandScreen: @Composable (() -> Unit, () -> Unit, (ComplexValue, Property, IEntityOperationType) -> Unit, ODataViewModel<ComplexValue>) -> Unit,
    override val entityListScreen: @Composable (() -> Unit, () -> Unit, ODataViewModel<ComplexValue>, Boolean) -> Unit,
    override val entityEditScreen: @Composable (onNavigateProperty: (ComplexValue, Property, IEntityOperationType) -> Unit, () -> Unit, ODataViewModel<ComplexValue>, Boolean) -> Unit,
    override val entityDetailScreen: @Composable ((ComplexValue, Property, IEntityOperationType) -> Unit, () -> Unit, ODataViewModel<ComplexValue>, Boolean) -> Unit
) : IScreenInfo<ComplexValue> {
}

enum class EntityScreenInfo(
    val entityType: EntityType,
    val entitySet: EntitySet?,
    override val setTitleId: Int,
    override val itemTitleId: Int,
    val iconId: Int,
        override val entityExpandScreen: @Composable (() -> Unit, () -> Unit, (EntityValue, Property, IEntityOperationType) -> Unit, ODataViewModel<EntityValue>) -> Unit,
        override val entityListScreen: @Composable (() -> Unit, () -> Unit, ODataViewModel<EntityValue>, Boolean) -> Unit,
        override val entityEditScreen: @Composable ((EntityValue, Property, IEntityOperationType) -> Unit, () -> Unit, ODataViewModel<EntityValue>, Boolean) -> Unit,
        override val entityDetailScreen: @Composable ((EntityValue, Property, IEntityOperationType) -> Unit, () -> Unit, ODataViewModel<EntityValue>, Boolean) -> Unit,
) : IScreenInfo<EntityValue> {
    Deliveries(
        EntityTypes.deliveries,
        EntityContainerMetadata.EntitySets.deliveries,
        R.string.eset_deliveries,
        R.string.eset_deliveries_single,
        R.drawable.ic_sap_icon_product_filled_round,
        DeliveriesEntitiesExpandScreen,
        DeliveriesEntitiesScreen,
        DeliveriesEntityEditScreen,
        DeliveriesEntityDetailScreen
    ),
}

fun getEntitySetScreenInfoList(): List<EntityScreenInfo> {
    val metadataMap = EntityMetaData.entries.associateBy { it.entityType }
    return EntityScreenInfo.entries.filter { metadataMap[it.entityType]?.entitySet != null }
}

// return screen info according to specified entity type and entity set
fun getEntityScreenInfo(entityType: EntityType, entitySet: EntitySet?): EntityScreenInfo =
    EntityScreenInfo.entries.first { getKey(entityType, entitySet) == getKey(it.entityType, it.entitySet) }

fun getEntityScreenInfo(complexType: ComplexType): ComplexScreenInfo {
    return ComplexScreenInfo.entries.first { it.complexType == complexType }
}

enum class ScreenType {
    List, Details, Update, Create, NavigatedList
}

@Composable
fun <T : StructureBase> screenTitle(screenInfo: IScreenInfo<T>, screenType: ScreenType): String {
    return when (screenType) {
        //TODO: navigated list title?
        ScreenType.List, ScreenType.NavigatedList -> stringResource(id = screenInfo.setTitleId)
        ScreenType.Details -> stringResource(id = screenInfo.itemTitleId)
        ScreenType.Update -> stringResource(id = R.string.title_update_fragment) + " ${
            stringResource(
                id = screenInfo.itemTitleId
            )
        }"
        ScreenType.Create -> stringResource(
            id = R.string.title_create_fragment,
            stringResource(id = screenInfo.itemTitleId)
        )
    }
}
