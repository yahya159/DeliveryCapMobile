package com.company.mysapbtpsdkproject.repository


import com.sap.cloud.android.odata.entitycontainer.EntityContainer

import com.sap.cloud.mobile.kotlin.odata.*
import com.sap.cloud.mobile.kotlin.odata.http.HttpHeaders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import org.slf4j.LoggerFactory


/**
 * Repository with specific EntitySet as parameter.
 * In other words, each entity set has its own repository and an in-memory store of all the entities
 * of that type.
 * Repository exposed the list of entities as paging data flow
 * @param entityContainer OData service
 * @param entityType entity type associated with this repository
 * @param entitySet entity set associated with this repository
 * @param orderByProperty used to order the collection retrieved from OData service
 */
class Repository(
        private val entityContainer: EntityContainer,
        private val entityType: EntityType,
        private val entitySet: EntitySet?,
        private val orderByProperty: Property?) {

    

    suspend fun read(pageSize: Int = 40, page: Int = 0, query: DataQuery? = null): Flow<List<EntityValue>> {
        return entitySet?.let{ eSet ->
            flow {
                val dataQuery = (query?.copy() ?: DataQuery()).apply { from(eSet).page(pageSize) }
                if (!entitySet.isSingleton) {
                    dataQuery.skip(page * pageSize)
                    orderByProperty?.also {
                        dataQuery.orderBy(it, SortOrder.ASCENDING)
                    }
                }
                try {
                    val result =  entityContainer.executeQuery(dataQuery, HttpHeaders.empty).getEntityList().toList()
                    emit(result)
                } catch (error: Exception) {
                    LOGGER.error("Error encountered during fetch of Category collection", error)
                    emit(listOf())
                }
            }.flowOn(Dispatchers.IO)
        } ?: throw IllegalArgumentException("Read data against containment property entity directly!")
    }

    suspend fun read(
        parent: EntityValue,
        navPropertyName: String,
        pageSize: Int = 40,
        page: Int = 0,
        query: DataQuery? = null
    ): Flow<List<EntityValue>> = flow {
        val navigationProperty = parent.entityType.getProperty(navPropertyName)
        val dataQuery = query?.copy() ?: DataQuery()
        if (navigationProperty.isCollection) {
            dataQuery.page(pageSize)
            if (!parent.entitySet.isSingleton) {
                dataQuery.skip(page * pageSize)
                orderByProperty?.also {
                    dataQuery.orderBy(
                        it,
                        SortOrder.ASCENDING
                    )
                }
            }
        }

        val entities = mutableListOf<EntityValue>()
        try {
            entityContainer.loadProperty(navigationProperty, parent, dataQuery, HttpHeaders.empty)
            val relatedData = parent.getOptionalValue(navigationProperty)

            when (navigationProperty.dataType.code) {
                DataType.ENTITY_VALUE_LIST -> entities.addAll((relatedData as EntityValueList?)!!.toList())
                DataType.ENTITY_VALUE -> if (relatedData != null) {
                    entities.add(relatedData as EntityValue)
                }
            }
        } catch (error: Exception) {
            LOGGER.error("Error encountered during fetch of Category collection", error)
        }

        emit(entities)
    }.flowOn(Dispatchers.IO)
    
    sealed class SuspendOperationResult {
        data class SuspendOperationSuccess(val newEntity: EntityValue? = null) :
            SuspendOperationResult()

        data class SuspendOperationFail(val error: Exception) : SuspendOperationResult()
    }

    private suspend fun createEntity(newEntity: EntityValue, media: StreamBase? = null, parent: EntityValue? = null, navPropName: String? = null) {
        val isSingleton = entitySet?.isSingleton ?: false
        val navProp = navPropName?.let { parent?.entityType?.getProperty(it) }
        if (isSingleton) {
            navProp?.also {
                entityContainer.metadata.resolveEntity(parent!!)
                newEntity.parentEntity = parent
                newEntity.parentProperty = it
            }
            val requestOptions = RequestOptions()
            requestOptions.updateMode = UpdateMode.REPLACE
            entityContainer.updateEntity(newEntity, HttpHeaders.empty, requestOptions)
            media?.also { entityContainer.uploadMedia(newEntity, it) }
        } else {
            navProp?.also {
                media?.also {
                    entityContainer.createRelatedMedia(newEntity, media, parent!!, navProp)
                } ?: entityContainer.createRelatedEntity(newEntity, parent!!, navProp)
            } ?: run {
                media?.also {
                    entityContainer.createMedia(newEntity, it)
                } ?: entityContainer.createEntity(newEntity)
            }
        }
    }

    suspend fun suspendCreate(newEntity: EntityValue, media: StreamBase): SuspendOperationResult {
        if (newEntity.entityType.isMedia) {
            return try {
                createEntity(newEntity, media)
                SuspendOperationResult.SuspendOperationSuccess(
                    newEntity
                )
            } catch (error: Exception) {
                LOGGER.error("Media Linked Entity creation failed.", error)
                SuspendOperationResult.SuspendOperationFail(error)
            }
        } else throw IllegalArgumentException("${newEntity.entityType} is not a media type!")
    }
    
    suspend fun suspendCreate(newEntity: EntityValue): SuspendOperationResult {
        if (newEntity.entityType.isMedia) {
            return SuspendOperationResult.SuspendOperationFail(IllegalStateException("Specify media resource for Media Linked Entity"))
        }

        return try {
            createEntity(newEntity)
            SuspendOperationResult.SuspendOperationSuccess(
                newEntity
            )
        } catch (error: Exception) {
            LOGGER.error("Entity creation failed:", error)
            SuspendOperationResult.SuspendOperationFail(error)
        }
    }

    suspend fun suspendCreateRelatedEntity(parent: EntityValue, newEntity: EntityValue, navPropName: String): SuspendOperationResult {
        return try {
            createEntity(newEntity = newEntity, parent = parent, navPropName = navPropName)
            SuspendOperationResult.SuspendOperationSuccess(
                newEntity
            )
        } catch (error: Exception) {
            LOGGER.error("Navigation child entity creation failed:", error)
            SuspendOperationResult.SuspendOperationFail(error)
        }
    }

    suspend fun suspendCreateRelatedEntity(parent: EntityValue, media: StreamBase, newEntity: EntityValue, navPropName: String): SuspendOperationResult {
        return try {
            createEntity(newEntity, media, parent, navPropName)
            SuspendOperationResult.SuspendOperationSuccess(
                newEntity
            )
        } catch (error: Exception) {
            LOGGER.error("Navigation child media entity creation failed:", error)
            SuspendOperationResult.SuspendOperationFail(error)
        }
    }

    suspend fun suspendUpdate(updateEntity: EntityValue): SuspendOperationResult {
        return try {
            entityContainer.updateEntity(updateEntity)
            SuspendOperationResult.SuspendOperationSuccess(
                updateEntity
            )
        } catch (error: Exception) {
            LOGGER.error("Entity update failed:", error)
            SuspendOperationResult.SuspendOperationFail(error)
        }
    }

    suspend fun suspendDelete(deleteEntities: List<EntityValue>): SuspendOperationResult {
        val deleteChangeSet = ChangeSet()
        for (entityToDelete in deleteEntities) {
            deleteChangeSet.deleteEntity(entityToDelete)
        }

        return try {
            entityContainer.applyChanges(deleteChangeSet)
            SuspendOperationResult.SuspendOperationSuccess()
        } catch (error: Exception) {
            LOGGER.error("Entities delete failed:", error)
            SuspendOperationResult.SuspendOperationFail(error)
        }
    }

    // download media value if entity is Media Resource, otherwise download first Stream Link property data
    suspend fun suspendDownloadMedia(entity: EntityValue): ByteArray {
        return if (entity.entityType.isMedia) {
            try {
                entityContainer.downloadMedia(entity)
            } catch (error: Exception) {
                LOGGER.error("Error encountered during fetch media", error)
                ByteArray(0)
            }
        } else {
            val streamProp = entity.entityType.streamProperties.first()
            try {
                entityContainer.downloadStream(entity, entity.getStreamLink(streamProp))
            } catch (error: Exception) {
                LOGGER.error("Error encountered during fetch stream link property {}",
                    streamProp.name, error)
                ByteArray(0)
            }
        }
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(Repository::class.java)
    }
}
