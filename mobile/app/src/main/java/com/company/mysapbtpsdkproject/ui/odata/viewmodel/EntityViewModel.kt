package com.company.mysapbtpsdkproject.ui.odata.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.repository.Repository
import com.company.mysapbtpsdkproject.repository.RepositoryFactory
import com.company.mysapbtpsdkproject.ui.odata.data.EntityMediaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.company.mysapbtpsdkproject.ui.odata.data.EntityPageSource
import com.company.mysapbtpsdkproject.ui.odata.screens.OperationResult
import com.company.mysapbtpsdkproject.util.Converter
import com.sap.cloud.mobile.kotlin.odata.ByteStream
import com.sap.cloud.mobile.kotlin.odata.DataQuery
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StreamBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for specific odata entity type,
 **/
open class EntityViewModel(
    application: Application,
    val entityType: EntityType,
    val entitySet: EntitySet?,
    orderByProperty: Property?,
    parent: EntityValue? = null,
    navigationPropertyName: String? = null,
) : ODataViewModel<EntityValue>(application, orderByProperty, parent, navigationPropertyName) {

    private val repository: Repository =
        RepositoryFactory.getRepository(entityType, entitySet, orderByProperty)

    override val internalPagingSourceFactory: (query: DataQuery?) -> PagingSource<Int, EntityValue> = {
        EntityPageSource(
            entityType,
            entitySet,
            orderByProperty,
            PAGE_SIZE,
            parent,
            navigationPropertyName,
            it
        )
    }

    override fun createInstance(): EntityValue {
        val emptyEntity = entityType.objectFactory!!.create() as EntityValue
        // Let masterEntity be the copy of emptyEntity.
        val newEntity = emptyEntity.copyEntity()
        // Set the oldEntity of masterEntity to make sure creating Singleton will be successful.
        // (For Singleton, create action using API `updateEntity` which requires oldEntity non-null. No effect on EntitySet.)
        newEntity.oldEntity = emptyEntity
        entitySet?.also { entitySet -> newEntity.entitySet = entitySet }
        return newEntity
    }

    override fun deleteInstances(instances: List<EntityValue>) {
        viewModelScope.launch(Dispatchers.IO) {
            operationStart()
            when (val operationResult =
                repository.suspendDelete(instances)) {
                is Repository.SuspendOperationResult.SuspendOperationSuccess -> {
                    refreshEntities()
                    operationFinished(result = OperationResult.OperationSuccess("Delete Success"))
                    clearSelection()
                }

                is Repository.SuspendOperationResult.SuspendOperationFail -> {
                    operationFinished(
                        result = OperationResult.OperationFail(
                            operationResult.error.message ?: "Delete fail"
                        )
                    )
                }
            }
        }
    }

    override fun clearSelection() {
        _odataUIState.update {
            it.copy(
                selectedItems = listOf(),
                isEntityFocused = false,
                entityOperationType = EntityOperationType.DETAIL,
                masterEntity = null
            )
        }
    }

    override fun onSaveAction(
        data: EntityValue,
        propValuePairs: List<Pair<Property, String>>
    ) {
        val result = Converter.populateEntityWithPropertyValue(data, propValuePairs)
        if (result.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                operationStart()
                val isCreation =
                    _odataUIState.value.entityOperationType == EntityOperationType.CREATE
                when (val operationResult =
                    if (isCreation) {
                        if (parent != null && navigationPropertyName != null) {
                            val parentEntity = parent as EntityValue
                            if (data.entityType.isMedia)
                                repository.suspendCreateRelatedEntity(
                                    parentEntity,
                                    defaultMediaResource,
                                    data,
                                    navigationPropertyName
                                )
                            else
                                repository.suspendCreateRelatedEntity(
                                    parentEntity,
                                    data,
                                    navigationPropertyName
                                )
                        } else {
                            if (data.entityType.isMedia)
                                repository.suspendCreate(data, defaultMediaResource)
                            else
                                repository.suspendCreate(data)
                        }
                    } else {
                        repository.suspendUpdate(data)
                    }) {
                    is Repository.SuspendOperationResult.SuspendOperationSuccess -> {
                        refreshEntities()
                        operationFinished(result = OperationResult.OperationSuccess("${if (isCreation) "Create" else "Update"} Success"))
                        onEntityDetail(if (isCreation) null else data)
                    }

                    is Repository.SuspendOperationResult.SuspendOperationFail -> {
                        operationFinished(
                            result = OperationResult.OperationFail(
                                operationResult.error.message
                                    ?: "${if (isCreation) "Create" else "Update"} Fail"
                            )
                        )
                    }
                }
            }
        }
    }

    //return create action when nav property value is list type or null, or entitySet is singleton and entity screen is not empty
    override fun onFloatingAdd(): (() -> Unit)? {
        // If a singleton entity already exists on the screen, remove the "+" floating button.
        entitySet?.let {
            if (it.isSingleton && _odataUIState.value.masterEntity != null) {
                return null
            }
        }

        return super.onFloatingAdd()
    }

    override fun selectedItemChange(entity: EntityValue) {
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
                    entityOperationType = EntityOperationType.UNSPECIFIED,
                    masterEntity = null
                )
            } else {
                it.copy(
                    selectedItems = items,
                    isEntityFocused = true,
                    entityOperationType = EntityOperationType.DETAIL,
                    masterEntity = entity
                )
            }

        }
    }

    override fun exitUpdate() {
        _odataUIState.update {
            it.copy(
                entityOperationType = EntityOperationType.DETAIL,
                masterEntity = restoreComplexPropertyValue(it.masterEntity),
                isEntityFocused = it.entityOperationType == EntityUpdateOperationType.UPDATE_FROM_DETAIL
            )
        }
    }

    //restore complex property value to discard the changes of complex property
    private fun restoreComplexPropertyValue(
        data: EntityValue?,
    ): EntityValue? {
        data?.apply {
            val properties = structureType.propertyList
            properties.forEach { property ->
                if (property.isComplex || property.isComplexList) {
                    val oldComplexValue = oldEntity?.getDataValue(property)
                    setDataValue(property, oldComplexValue)
                }
            }
        }
        return data
    }

    override fun exitCreation() {
        _odataUIState.update {
            it.copy(entityOperationType = EntityOperationType.DETAIL)
        }
        lostEntityFocus()
    }

    //for entity list, navigate to detail screen
    override fun onClickAction(entity: EntityValue) {
        onEntityDetail(entity)
    }

    private val defaultMediaResource: StreamBase
        get() {
            val inputStream = getApplication<Application>().resources.openRawResource(R.raw.blank)
            val byteStream = ByteStream.fromInput(inputStream)
            byteStream.mediaType = "image/png"
            return byteStream
        }


    fun loadMasterEntityMedia(): Flow<ByteArray?> {
        return _odataUIState.map {
            it.masterEntity?.let { entity ->
                loadMedia(
                    entity,
                )
            }
        }
    }

    suspend fun loadMedia(entity: EntityValue) : ByteArray? {
        return if (EntityMediaResource.hasMediaResources(entity.entityType)) {
            //TODO: support steam link properties
            repository.suspendDownloadMedia(entity)
        } else {
            null
        }
    }

}
