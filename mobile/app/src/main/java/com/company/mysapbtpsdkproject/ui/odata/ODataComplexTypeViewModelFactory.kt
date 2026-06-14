package com.company.mysapbtpsdkproject.ui.odata

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityOperationType
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.IEntityOperationType
import com.sap.cloud.mobile.kotlin.odata.ComplexType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property

class ODataComplexTypeViewModelFactory(
    private val application: Application,
    private val complexType: ComplexType,
    private val orderByProperty: Property?,
    private val parent: EntityValue?,
    private val navigationPropertyName: String?,
    private val operationType: IEntityOperationType = EntityOperationType.DETAIL,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (getKey(complexType)) {
            else -> { throw UnsupportedOperationException() }
        }
    }
}
