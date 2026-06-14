package com.company.mysapbtpsdkproject.ui.odata

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.ODataViewModel
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.deliveries.DeliveriesODataViewModel
import com.sap.cloud.mobile.kotlin.odata.EntitySet
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntityTypes

class ODataEntityViewModelFactory(
    private val application: Application,
    private val entityType: EntityType,
    private val entitySet: EntitySet?,
    private val orderByProperty: Property?,
    private val parent: EntityValue? = null,
    private val navigationPropertyName: String? = null,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (getKey(entityType, entitySet)) {
            getKey(EntityTypes.deliveries, EntityContainerMetadata.EntitySets.deliveries) -> DeliveriesODataViewModel(
                application,
                orderByProperty,
                parent,
                navigationPropertyName
            ) as T
            else -> { throw UnsupportedOperationException() }
        }
    }
}
