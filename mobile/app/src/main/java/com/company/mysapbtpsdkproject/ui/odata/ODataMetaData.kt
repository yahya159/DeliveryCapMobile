package com.company.mysapbtpsdkproject.ui.odata

import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata
import com.sap.cloud.mobile.kotlin.odata.ComplexType
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntityTypes

enum class EntityMetaData(
    val entityType: EntityType,
    val orderByProperty: Property?,
    val entitySet: EntitySet? = null,
) {
    Deliveries(
        EntityTypes.deliveries,
        com.sap.cloud.android.odata.entitycontainer.Deliveries.customerName,
        EntityContainerMetadata.EntitySets.deliveries,
        ),
}

enum class ComplexTypeMetaData(
    val complexType: ComplexType,
    val orderByProperty: Property?,
) {
}

fun getOrderByProperty(entityType: EntityType): Property? {
    return EntityMetaData.entries.first { it.entityType == entityType }.orderByProperty
}

fun getOrderByProperty(complexType: ComplexType): Property? {
    return ComplexTypeMetaData.entries.first { it.complexType == complexType }.orderByProperty
}

fun getKey(entityType: EntityType, entitySet: EntitySet? = null): String {
    return "${entitySet?.localName}_${entityType.localName}"
}

fun getKey(complexType: ComplexType): String {
    return "complex_${complexType.localName}"
}
