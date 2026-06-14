package com.company.mysapbtpsdkproject.ui.odata.data

import com.sap.cloud.mobile.kotlin.odata.ComplexValue
import com.sap.cloud.mobile.kotlin.odata.ComplexValueList
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ComplexTypePageSource(
    override val pageSize: Int,
    private val parentEntity: EntityValue,
    private val navPropName: String,
) : AbstractODataPageSource<ComplexValue>(pageSize) {

    override suspend fun loadItems(pageSize: Int, page: Int): Flow<List<ComplexValue>> = flow {
        val instances = mutableListOf<ComplexValue>()
        val property = parentEntity.entityType.getProperty(navPropName)
        if (property.isComplexList) {
            val complexList = parentEntity.getOptionalValue(property) as ComplexValueList?
            complexList?.let {
                instances.addAll(it.slice(page * pageSize, (page + 1) * pageSize).toList())
            }
        } else if (property.isComplex) {
            val complex = parentEntity.getOptionalValue(property) as ComplexValue?
            complex?.let {
                instances.add(it)
            }
        }
        emit(instances)
    }.flowOn(Dispatchers.IO)

}
