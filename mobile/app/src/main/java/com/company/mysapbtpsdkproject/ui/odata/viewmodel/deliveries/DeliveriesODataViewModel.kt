package com.company.mysapbtpsdkproject.ui.odata.viewmodel.deliveries

import android.app.Application
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityViewModel
import com.company.mysapbtpsdkproject.ui.odata.screens.FieldUIState
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StructureBase

import com.sap.cloud.android.odata.entitycontainer.Deliveries

class DeliveriesODataViewModel(
    application: Application,
    orderByProperty: Property?,
    parent: EntityValue? = null,
    navigationPropertyName: String? = null,
) : EntityViewModel(
    application,
    EntityContainerMetadata.EntityTypes.deliveries,
    EntityContainerMetadata.EntitySets.deliveries,
    orderByProperty,
    parent,
    navigationPropertyName
) {
    override fun populateFiledStates(masterEntity: StructureBase, isEdit: Boolean): List<FieldUIState> {
        val list = mutableListOf<FieldUIState>()
        // add the non-computed properties to the list
        // key properties are only shown in creation mode
        if (!isEdit) {
        list.add(FieldUIState(
            property = Deliveries.id,
            value = masterEntity.getOptionalValue(Deliveries.id)?.toString() ?: "",
        ))
        }
        list.add(FieldUIState(
            property = Deliveries.orderNo,
            value = masterEntity.getOptionalValue(Deliveries.orderNo)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.customerName,
            value = masterEntity.getOptionalValue(Deliveries.customerName)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.customerPhone,
            value = masterEntity.getOptionalValue(Deliveries.customerPhone)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.deliveryAddress,
            value = masterEntity.getOptionalValue(Deliveries.deliveryAddress)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.status,
            value = masterEntity.getOptionalValue(Deliveries.status)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.driverEmail,
            value = masterEntity.getOptionalValue(Deliveries.driverEmail)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.note,
            value = masterEntity.getOptionalValue(Deliveries.note)?.toString() ?: "",
        ))
        // latitude/longitude are not rendered as text fields; they are driven by the
        // LocationSection on the edit screen but kept here so they are persisted on save.
        list.add(FieldUIState(
            property = Deliveries.latitude,
            value = masterEntity.getOptionalValue(Deliveries.latitude)?.toString() ?: "",
        ))
        list.add(FieldUIState(
            property = Deliveries.longitude,
            value = masterEntity.getOptionalValue(Deliveries.longitude)?.toString() ?: "",
        ))

        return list.map { validateFieldState(it, it.value) }
    }

//    override fun getAvatarText(entity: EntityValue?): String {
//        val customer = entity as Customer?
//        return customer?.let { "${it.lastName?.first() ?: '?'}${it.firstName?.first() ?: '?'}" }
//            ?: "??"
//    }
}
