package com.company.mysapbtpsdkproject.repository

import com.company.mysapbtpsdkproject.service.OfflineWorkerUtil
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntityTypes
import com.company.mysapbtpsdkproject.ui.odata.getKey
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.Property

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
object RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 */
{
    private val repositories: WeakHashMap<String, Repository> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entityType: EntityType, entitySet: EntitySet?, orderByProperty: Property?): Repository {
        val entityContainer = OfflineWorkerUtil.entityContainer
        val key = getKey(entityType, entitySet)
        var repository: Repository? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                getKey(EntityTypes.deliveries, EntityContainerMetadata.EntitySets.deliveries) ->
                    Repository(entityContainer, EntityTypes.deliveries, EntityContainerMetadata.EntitySets.deliveries, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
