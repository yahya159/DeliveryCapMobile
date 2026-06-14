package com.company.mysapbtpsdkproject.ui.odata.viewmodel

import android.app.Application
import androidx.paging.PagingSource
import com.company.mysapbtpsdkproject.ui.odata.data.ComplexTypePageSource
import com.company.mysapbtpsdkproject.util.Converter
import com.sap.cloud.mobile.kotlin.odata.ComplexType
import com.sap.cloud.mobile.kotlin.odata.ComplexValue
import com.sap.cloud.mobile.kotlin.odata.ComplexValueList
import com.sap.cloud.mobile.kotlin.odata.DataQuery
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import kotlinx.coroutines.flow.update

/**
 * ViewModel for odata complex type,
 **/
open class ComplexTypeViewModel(
    application: Application,
    val complexType: ComplexType,
    orderByProperty: Property?,
    override val parent: EntityValue,
    navigationPropertyName: String,
    operationType: IEntityOperationType
) : ODataViewModel<ComplexValue>(application, orderByProperty, parent, navigationPropertyName) {

    init {
        super._odataUIState.update {
            it.copy(
                entityOperationType = operationType,
            )
        }
    }

    // ComplexType does not support DataQuery filter for now, just ignore the query
    override val internalPagingSourceFactory: (query: DataQuery?) -> PagingSource<Int, ComplexValue> = {
        ComplexTypePageSource(
            PAGE_SIZE,
            parent,
            navigationPropertyName
        )
    }

    override fun updateFilter(newFilter: DataQuery?) {
        throw UnsupportedOperationException("Complex type does not support DataQuery filter")
    }

    override fun createInstance(): ComplexValue {
        val instance = complexType.objectFactory!!.create() as ComplexValue
        return instance
    }

    override fun deleteInstances(instances: List<ComplexValue>) {
        val navProp = parent.entityType.getProperty(navigationPropertyName!!)
        if (navProp.isComplexList) {
            val newComplexList = (parent.getOptionalValue(navProp) as ComplexValueList).filter {
                !instances.contains(it)
            }
            parent.setDataValue(navProp, newComplexList)
        } else if (navProp.isComplex) {
            val complexValue = parent.getOptionalValue(navProp) as ComplexValue
            if (instances.contains(complexValue)) {
                parent.setDataValue(navProp, null)
            }
        }
        clearSelection()
        refreshEntities()
    }

    override fun clearSelection() {
        _odataUIState.update {
            it.copy(
                selectedItems = listOf(),
                isEntityFocused = false,
                masterEntity = null
            )
        }
    }

    override fun onSaveAction(
        data: ComplexValue,
        propValuePairs: List<Pair<Property, String>>
    ) {
        val result = Converter.populateEntityWithPropertyValue(data, propValuePairs)
        if (result.isEmpty()) {
            val navProp = parent.entityType.getProperty(navigationPropertyName!!)
            if (navProp.isComplexList) {
                if (_odataUIState.value.entityOperationType == EntityOperationType.CREATE) {
                    (parent.getOptionalValue(navProp) as ComplexValueList).add(data)
                }
            } else if (navProp.isComplex) {
                parent.setDataValue(navProp, data)
            }

            _odataUIState.update {
                it.copy(
                    isEntityFocused = false, // back to list screen
                    masterEntity = null
                )
            }

            refreshEntities() //reset master entity with the first item
        }
    }

    override fun exitUpdate() {
        lostEntityFocus()
    }

    override fun exitCreation() {
        lostEntityFocus()
    }

    override fun selectedItemChange(entity: ComplexValue) {
        _odataUIState.update {
            var items = it.selectedItems
            items = if (items.contains(entity)) {
                items - entity
            } else {
                items + entity
            }

            if (items.isNotEmpty()) {
                it.copy(
                    selectedItems = items,
                    isEntityFocused = false,
                    masterEntity = null
                )
            } else {
                it.copy(
                    selectedItems = items,
                    isEntityFocused = true,
                    masterEntity = entity
                )
            }
        }
    }

    // for complex type UI list, the current operation type will be kept
    override fun onClickAction(entity: ComplexValue) {
        when (_odataUIState.value.entityOperationType) {
            EntityOperationType.DETAIL -> onEntityDetail(entity)
            EntityUpdateOperationType.UPDATE_FROM_DETAIL, EntityUpdateOperationType.UPDATE_FROM_LIST, EntityOperationType.CREATE -> {
                _odataUIState.update {
                    it.copy(
                        masterEntity = entity
                    )
                }
                onUpdate()
            }
            else -> throw IllegalArgumentException("Invalid operation type")
        }
    }

}
