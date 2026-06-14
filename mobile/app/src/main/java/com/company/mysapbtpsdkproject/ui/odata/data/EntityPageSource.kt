package com.company.mysapbtpsdkproject.ui.odata.data

import com.company.mysapbtpsdkproject.repository.RepositoryFactory
import com.sap.cloud.mobile.kotlin.odata.DataQuery
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import kotlinx.coroutines.flow.Flow

/**
 * A PagingSource implementation for loading pages of OData entities.
 *
 * @property entityType The type of the entities to be loaded.
 * @property entitySet The set from which entities are to be loaded, can be null.
 * @property orderBy The property to order the entities by, can be null.
 * @property pageSize The number of entities to be loaded per page.
 * @property parentEntity The parent entity for navigation properties, can be null.
 * @property navPropName The name of the navigation property to load related entities, can be null.
 * @property query The DataQuery instance for filtering
 */
class EntityPageSource(
    private val entityType: EntityType,
    private val entitySet: EntitySet?,
    private val orderBy: Property?,
    override val pageSize: Int,
    private val parentEntity: EntityValue? = null,
    private val navPropName: String? = null,
    private val query: DataQuery? = null,
) : AbstractODataPageSource<EntityValue>(pageSize) {

    private val repository by lazy {
        RepositoryFactory.getRepository(entityType, entitySet, orderBy)
    }

    override suspend fun loadItems(pageSize: Int, page: Int): Flow<List<EntityValue>> {
        return if (navPropName == null) {
            repository.read(
                pageSize = pageSize, page = page, query = query,
            )
        } else {
            repository.read(
                pageSize = pageSize,
                page = page,
                navPropertyName = navPropName,
                parent = parentEntity!!,
                query = query
            )
        }
    }

}

