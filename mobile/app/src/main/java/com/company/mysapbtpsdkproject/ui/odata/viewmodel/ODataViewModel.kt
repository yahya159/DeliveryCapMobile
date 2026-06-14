package com.company.mysapbtpsdkproject.ui.odata.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.odata.screens.FieldUIState
import com.company.mysapbtpsdkproject.util.Converter
import com.sap.cloud.mobile.kotlin.odata.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

const val PAGE_SIZE: Int = 20

interface IEntityOperationType

// unspecified type for empty screen while list items selected
enum class EntityOperationType : IEntityOperationType {
    DETAIL, CREATE, UNSPECIFIED
}

enum class EntityUpdateOperationType : IEntityOperationType {
    UPDATE_FROM_LIST, UPDATE_FROM_DETAIL
}

data class ODataUIState<T : StructureBase>(
    val masterEntity: T? = null,
    val entityOperationType: IEntityOperationType = EntityOperationType.DETAIL,
    val selectedItems: List<T> = listOf(),
    val isEntityFocused: Boolean = false,
    val editorFiledStates: List<FieldUIState> = listOf() //for editor screen only
)

/**
 * ViewModel for specific odata type,
 * including:
 * 1. master entity
 * 2. entity operation type (detail, update, creation)
 * 3. selected entities (long pressed)
 * 4. is entity Focused? => decide show list or detail in non-expand screen, long press on list will clear it, select entity will set it
 **/
abstract class ODataViewModel<T : StructureBase>(
    application: Application,
    private val orderByProperty: Property?,
    open val parent: EntityValue? = null,
    val navigationPropertyName: String? = null,
) : BaseOperationViewModel(application) {
    val pagingDataState =
        mutableStateOf<Flow<PagingData<T>>>(flowOf(PagingData.empty()))

    protected val _odataUIState = MutableStateFlow(ODataUIState<T>())
    val odataUIState = _odataUIState.asStateFlow()

    // abstract properties & functions
    abstract val internalPagingSourceFactory: (query: DataQuery?) -> PagingSource<Int, T>

    // remote entity query filter support
    private val _filter = MutableStateFlow<DataQuery?>(null)

    /**
     *create instance of entity type or complex type
     */
    abstract fun createInstance(): T

    /**
     * delete instance of entity type or complex type
     */
    abstract fun deleteInstances(instances: List<T>)

    /**
     * clear current selected items
     */
    abstract fun clearSelection()

    /**
     * save action
     */
    abstract fun onSaveAction(
        data: T,
        propValuePairs: List<Pair<Property, String>>
    )

    /**
     * selected item change in list view (via long press)
     */
    abstract fun selectedItemChange(entity: T)

    /**
     * exit update or create screen
     */
    protected abstract fun exitUpdate()
    protected abstract fun exitCreation()

    fun exitEditor() {
        when (_odataUIState.value.entityOperationType) {
            EntityOperationType.CREATE -> exitCreation()

            EntityUpdateOperationType.UPDATE_FROM_DETAIL,
            EntityUpdateOperationType.UPDATE_FROM_LIST  -> exitUpdate()
        }
    }

    /**
     * click item in list view
     */
    abstract fun onClickAction(entity: T)

    private val invalidatingPagingSourceFactory =
       InvalidatingPagingSourceFactory { internalPagingSourceFactory(_filter.value) }

    init {
        pagingDataState.value = retrieveEntities()
    }

    private fun retrieveEntities(): Flow<PagingData<T>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = true),
        pagingSourceFactory = invalidatingPagingSourceFactory
    ).flow.map {
        it.insertSeparators { before: T?, after: T? ->
            if (before == null && after != null) {
                if (_odataUIState.value.masterEntity == null) {
                    setMasterEntity(after)
                }
            }
            return@insertSeparators null
        }
    }.cachedIn(viewModelScope)

    // Update filter and trigger invalidation
    open fun updateFilter(newFilter: DataQuery?) {
        if (_filter.value != newFilter) {
            _filter.value = newFilter
            invalidatingPagingSourceFactory.invalidate() // Refresh data with new filter
        }
    }

    /* Odata UI State related operations */
    protected fun onUpdate() {
        _odataUIState.update {
            val result = if (it.selectedItems.isNotEmpty()) {
                it.copy(
                    entityOperationType = EntityUpdateOperationType.UPDATE_FROM_LIST,
                    masterEntity = it.selectedItems[0],
                    isEntityFocused = true,
                    selectedItems = listOf(),
                    editorFiledStates = populateFiledStates(it.selectedItems[0], true)
                )
            } else {
                it.copy(
                    entityOperationType = EntityUpdateOperationType.UPDATE_FROM_DETAIL,
                    isEntityFocused = true,
                    editorFiledStates = populateFiledStates(it.masterEntity!!, true)
                )
            }
            result
        }
    }

    // populate field states for the editor screen in a generic way
    open fun populateFiledStates(masterEntity: StructureBase, isEdit: Boolean): List<FieldUIState> {
        return masterEntity.let { entity ->
            entity.structureType.propertyList.toList()
                .filter {
                    val isComputed = it.annotations.has(COMPUTED_ANNOTATION_TERM)
                    // filter navigation, complex type, computed property, and primary key field in edit mode
                    it !is NavigationProperty && it.dataType.isBasic && !isComputed
                        && if (isEdit) !it.isKey else true
                }.map {
                    FieldUIState(
                        entity.getOptionalValue(it)?.toString() ?: "",
                        it,
                        false
                    )
                }.map { validateFieldState(it, it.value) } //perform init validation
        }
    }

    private fun onCreate() {
        _odataUIState.update {
            val newEntity = createInstance()
            it.copy(
                entityOperationType = EntityOperationType.CREATE,
                masterEntity = newEntity,
                isEntityFocused = true,
                selectedItems = listOf(),
                editorFiledStates = populateFiledStates(newEntity, false)
            )
        }
    }

    protected fun onEntityDetail(masterEntity: T?) {
        _odataUIState.update {
            when (it.entityOperationType) {
                EntityUpdateOperationType.UPDATE_FROM_DETAIL, EntityUpdateOperationType.UPDATE_FROM_LIST ->
                    it.copy(
                        entityOperationType = EntityOperationType.DETAIL,
                        isEntityFocused = true,
                        masterEntity = masterEntity
                    )

                EntityOperationType.CREATE ->
                    it.copy(
                        entityOperationType = EntityOperationType.DETAIL,
                        isEntityFocused = false,
                        masterEntity = masterEntity
                    )

                else -> it.copy(
                    entityOperationType = EntityOperationType.DETAIL,
                    masterEntity = masterEntity,
                    selectedItems = listOf(),
                    isEntityFocused = masterEntity != null,
                )
            }
        }
    }

    /* view model APIs*/
    // select entity in list view via long press
    fun onSelectAction(entity: T) {
        selectedItemChange(entity)
    }

    fun lostEntityFocus() {
        _odataUIState.update { it.copy(isEntityFocused = false) }
    }

    // delete master entity in details screen
    fun onDeleteAction() {
        if (_odataUIState.value.selectedItems.isNotEmpty()) {
            deleteInstances(_odataUIState.value.selectedItems)
        } else if (_odataUIState.value.masterEntity != null) {
            deleteInstances(listOf(_odataUIState.value.masterEntity!!))
        } else {
            throw IllegalArgumentException("delete with empty selection")
        }
    }

    fun refreshEntities() {
        invalidatingPagingSourceFactory.invalidate()
    }

    fun onEditAction() {
        onUpdate()
    }

    fun onCreateAction() {
        onCreate()
    }

    fun setMasterEntity(entity: T) {
        _odataUIState.update {
            it.copy(
                masterEntity = entity,
            )
        }

        if (_odataUIState.value.entityOperationType != EntityOperationType.DETAIL) {
            val isEdit = _odataUIState.value.entityOperationType != EntityOperationType.CREATE
            _odataUIState.update {
                it.copy(
                    editorFiledStates = populateFiledStates(entity, isEdit)
                )
            }
        }
    }

    //return create action when nav property value is list type or null, or entitySet is singleton and entity screen is not empty
    open fun onFloatingAdd(): (() -> Unit)? {
        val action = {
            onCreateAction()
            refreshEntities()
        }

        return parent?.let { parent ->
            return navigationPropertyName?.let {
                val navProp = parent.entityType.getProperty(navigationPropertyName)
                val navValue = parent.getOptionalValue(navProp)
                if (navProp.isEntityList || navProp.isComplexList || navValue == null) action else null
            }
        } ?: action
    }

    fun updateFieldState(
        fieldStateIndex: Int, newValue: String
    ) {
        val newState =
            validateFieldState(_odataUIState.value.editorFiledStates[fieldStateIndex], newValue)
        _odataUIState.update {
            val newStates = it.editorFiledStates.toMutableStateList()
            newStates[fieldStateIndex] = newState
            it.copy(
                editorFiledStates = newStates.toList()
            )
        }
    }

    fun validateFieldState(
        fieldUIState: FieldUIState,
        newValue: String
    ): FieldUIState {
        val property = fieldUIState.property
        if (!property.isNullable && newValue.isEmpty()) { // check if mandatory
            return fieldUIState.copy(
                isError = true,
                errorMessage = getApplication<Application>().getString(R.string.mandatory_warning),
                value = newValue
            )
        } else if (newValue.isNotEmpty()) { // check if property type valid input
            val convertResult = Converter.convert(property, newValue)
            if (convertResult is Converter.ConvertResult.ConvertError) {
                return fieldUIState.copy(
                    isError = true,
                    errorMessage = getApplication<Application>().getString(R.string.format_error),
                    value = newValue
                )
            }
        }

        //con max length
        val maxLength = property.maxLength
        return if (maxLength > 0 && newValue.length > maxLength) {
            fieldUIState.copy(value = newValue.substring(0, maxLength), isError = false)
        } else {
            fieldUIState.copy(value = newValue, isError = false)
        }

    }

    open fun getAvatarText(entity: T?): String {
        val entityPrincipleData =
            orderByProperty?.let { entity?.getOptionalValue(orderByProperty).toString() }
        return if (entityPrincipleData?.isNotEmpty() == true) {
            entityPrincipleData.take(1)
        } else {
            "?"
        }
    }

    open fun getEntityTitle(entity: T): String {
        val title =
            orderByProperty?.let { entity.getOptionalValue(orderByProperty).toString() }
        return if (title?.isNotEmpty() == true) {
            title
        } else {
            "???"
        }
    }

    companion object {
        const val COMPUTED_ANNOTATION_TERM = "Org.OData.Core.V1.Computed"
    }

}
