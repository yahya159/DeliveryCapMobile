package com.company.mysapbtpsdkproject.ui.odata.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.AlertDialogComponent
import com.company.mysapbtpsdkproject.ui.odata.ActionItem
import com.company.mysapbtpsdkproject.ui.odata.OverflowMode
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.ODataViewModel
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.EntityViewModel
import com.sap.cloud.mobile.fiori.compose.avatar.model.FioriAvatarData
import com.sap.cloud.mobile.fiori.compose.common.FioriIcon
import com.sap.cloud.mobile.fiori.compose.common.FioriImage
import com.sap.cloud.mobile.fiori.compose.objectheader.model.FioriObjectHeaderData
import com.sap.cloud.mobile.fiori.compose.objectheader.model.FioriObjectHeaderLabelItemData
import com.sap.cloud.mobile.fiori.compose.objectheader.model.FioriObjectHeaderStatusData
import com.sap.cloud.mobile.fiori.compose.objectheader.model.FioriObjectHeaderStatusType
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StructureBase

@Composable
fun <T : StructureBase> DeleteEntityWithConfirmation(viewModel: ODataViewModel<T>, confirmState: MutableState<Boolean>) {
    val context = LocalContext.current

    if (confirmState.value) {
        AlertDialogComponent(title = context.getString(R.string.delete_dialog_title),
            text = context.getString(R.string.delete_one_item),
            onNegativeButtonClick = { confirmState.value = false },
            positiveButtonText = context.getString(R.string.delete),
            onPositiveButtonClick = {
                confirmState.value = false
                viewModel.onDeleteAction()
            })
    }
}

@Composable
fun LeaveEditorWithConfirmation(confirmState: MutableState<Boolean>, onLeave: () -> Unit) {
    val context = LocalContext.current
    if (confirmState.value) {
        AlertDialogComponent(
            title = context.getString(R.string.before_navigation_dialog_title),
            text = context.getString(R.string.before_navigation_dialog_message),
            onNegativeButtonClick = { confirmState.value = false },
            positiveButtonText = context.getString(R.string.before_navigation_dialog_positive_button),
            negativeButtonText = context.getString(R.string.before_navigation_dialog_negative_button),
            onPositiveButtonClick = {
                confirmState.value = false
                onLeave()
            }
        )
    }
}

@Composable
fun <T : StructureBase> getSelectedItemActionsList(
    navigateToHome: () -> Unit,
    viewModel: ODataViewModel<T>,
    deleteState: MutableState<Boolean>
): List<ActionItem> {
    val uiState = viewModel.odataUIState.collectAsState()
    return when (uiState.value.selectedItems.size) {
        0 -> listOf(
            ActionItem(
                nameRes = R.string.menu_home,
                iconRes = R.drawable.ic_sap_icon_home,
                overflowMode = OverflowMode.IF_NECESSARY,
                doAction = navigateToHome
            ),
        )

        else -> getSelectedItemActionsList(
            viewModel,
            deleteState
        )
    }
}

@Composable
fun <T : StructureBase> getSelectedItemActionsList(
    viewModel: ODataViewModel<T>,
    deleteState: MutableState<Boolean>
): List<ActionItem> {
    val uiState = viewModel.odataUIState.collectAsState()
    return when (uiState.value.selectedItems.size) {
        0 -> listOf() //should not happen
        1 -> listOf(
            ActionItem(
                nameRes = R.string.menu_update,
                iconRes = R.drawable.ic_sap_icon_edit,
                overflowMode = OverflowMode.IF_NECESSARY,
                doAction = viewModel::onEditAction
            ),
            ActionItem(
                nameRes = R.string.menu_delete,
                iconRes = R.drawable.ic_sap_icon_delete,
                overflowMode = OverflowMode.IF_NECESSARY,
                doAction = { deleteState.value = true }
            ),
        )

        else -> listOf(
            ActionItem(
                nameRes = R.string.menu_delete,
                iconRes = R.drawable.ic_sap_icon_delete,
                overflowMode = OverflowMode.IF_NECESSARY,
                doAction = { deleteState.value = true }
            ),
        )
    }
}

data class StateIcon(@param:DrawableRes val icon: Int, @param:StringRes val desc: Int)

fun getEntityStateIcon(it: EntityValue): StateIcon {
    return when {
        it.inErrorState -> StateIcon(
            R.drawable.ic_error_state, R.string.error_state
        )

        it.isUpdated -> StateIcon(
            R.drawable.ic_updated_state, R.string.updated_state
        )

        it.isLocal -> StateIcon(
            R.drawable.ic_local_state, R.string.local_state
        )

        else ->
            StateIcon(
                R.drawable.ic_download_state, R.string.download_state
            )
    }

}

sealed interface ResultOf<out Bitmap> {
    data object Loading : ResultOf<Nothing>
    data class Success(val bitmap: Bitmap) : ResultOf<Bitmap>
    data object Failure : ResultOf<Nothing>
}

@Composable
fun loadMedia(
    entity: EntityValue,
    viewModel: EntityViewModel
): State<ResultOf<Bitmap>> {
    return produceState<ResultOf<Bitmap>>(initialValue = ResultOf.Loading, entity, viewModel) {
        val media = viewModel.loadMedia(entity)
        value = if (media?.size != 0) {
            ResultOf.Success(
                BitmapFactory.decodeByteArray(media, 0, media!!.size)
            )
        } else {
            ResultOf.Failure
        }
    }
}

fun defaultObjectHeaderData(
    title: String,
    imageByteArray: ByteArray? = null,
    imageUrl: String? = null,
    imageChars: String? = null
): FioriObjectHeaderData {

    val content = FioriObjectHeaderData(
        title
    )

    content.detailImage = imageByteArray?.let {
        if (it.isNotEmpty()) {
            FioriAvatarData(
                FioriImage(
                    bitmap = BitmapFactory.decodeByteArray(it, 0, it.size),
                    contentDescription = "Detail image"
                )
            )
        } else {
            //load image fail
            imageChars?.let { char ->
                FioriAvatarData(
                    text = char,
                    textFontSize = 24.sp,
                )
            }
        }
    } ?: imageUrl?.let {
        FioriAvatarData(
            FioriImage(
                url = it,
                contentDescription = "Detail image"
            )
        )
    } ?: imageChars?.let {
        FioriAvatarData(
            text = it,
            textFontSize = 24.sp,
        )
    } ?: FioriAvatarData(
        FioriImage(
            resId = R.drawable.ic_sync,
            contentDescription = "Loading image"
        )
    )

    content.subtitle = "This is a subtitle that can take up to a maximum of three lines."

    content.accessoryKpi = "accKpi"
    content.accessoryKpiLabel = "accKpiLabel"



    content.status = FioriObjectHeaderStatusData(
        label = "Status",
        icon = FioriIcon(
            resId = R.drawable.ic_sap_icon_home,
            contentDescription = "Positive status icon",
        ),
        type = FioriObjectHeaderStatusType.Positive,
        isIconAtStart = true
    )
    content.subStatus = FioriObjectHeaderStatusData(
        label = "SubStatus",
        icon = FioriIcon(
            resId = R.drawable.ic_error_state,
            contentDescription = "Critical status icon",
        ),
        type = FioriObjectHeaderStatusType.Critical,
        isIconAtStart = false
    )


    content.labelItems = listOf(
        FioriObjectHeaderLabelItemData(
            label = "Attribute1"
        ),
        FioriObjectHeaderLabelItemData(
            label = "Attribute2",
            icon = FioriIcon(
                resId = R.drawable.ic_sap_icon_home
            ),
            isIconAtStart = false
        ),
        FioriObjectHeaderLabelItemData(
            label = "Subtitle"
        ),
        FioriObjectHeaderLabelItemData(
            label = "3/28/2022",
            icon = FioriIcon(
                resId = R.drawable.ic_sap_icon_delete
            )
        )
    )


    content.description =
        "description show here"


    return content
}

data class FieldUIState(
    val value: String,
    val property: Property,
    val isError: Boolean = false,
    val errorMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is FieldUIState) return false
        return (this.value == other.value && this.property.name == other.property.name && this.isError == other.isError && errorMessage == other.errorMessage)
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + property.hashCode()
        result = 31 * result + isError.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
