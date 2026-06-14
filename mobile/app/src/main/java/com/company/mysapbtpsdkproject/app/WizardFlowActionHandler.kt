package com.company.mysapbtpsdkproject.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.SpannedString
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.FileProvider
import androidx.core.text.getSpans
import com.company.mysapbtpsdkproject.BuildConfig
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.app.WizardFlowActionHandler.Companion.logger
import com.company.mysapbtpsdkproject.app.WizardFlowActionHandler.Companion.temp_log_zip_file
import com.company.mysapbtpsdkproject.app.WizardFlowActionHandler.Companion.log_file_name
import com.company.mysapbtpsdkproject.ui.WelcomeActivity
import com.sap.cloud.mobile.fiori.compose.dialog.FioriAlertDialog
import com.sap.cloud.mobile.fiori.compose.mdtext.FioriMarkdownText
import com.sap.cloud.mobile.fiori.compose.text.model.FioriTextFieldContent
import com.sap.cloud.mobile.fiori.compose.text.ui.FioriPasswordTextField
import com.sap.cloud.mobile.fiori.compose.text.ui.FioriSimpleTextField
import com.sap.cloud.mobile.fiori.compose.text.ui.FioriTextFieldInteractionState
import com.sap.cloud.mobile.fiori.compose.theme.fioriHorizonAttributes
import com.sap.cloud.mobile.flows.compose.ext.CustomStepInsertionPoint
import com.sap.cloud.mobile.flows.compose.ext.FlowActionHandler
import com.sap.cloud.mobile.flows.compose.flows.BaseFlow
import com.sap.cloud.mobile.flows.compose.flows.FlowType
import com.sap.cloud.mobile.foundation.ext.SDKCustomTabsLauncher
import com.sap.cloud.mobile.foundation.logging.LoggingService
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer
import com.sap.cloud.mobile.onboarding.compose.screens.LaunchScreen
import com.sap.cloud.mobile.onboarding.compose.screens.rememberLaunchScreenState
import com.sap.cloud.mobile.onboarding.compose.settings.LaunchScreenContentSettings
import com.sap.cloud.mobile.onboarding.compose.settings.LaunchScreenSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.slf4j.LoggerFactory
import java.io.File

class WizardFlowActionHandler(val application: SAPWizardApplication) : FlowActionHandler() {


    @Composable
    private fun getLaunchScreenAnnotatedString(rId: Int, settings: LaunchScreenSettings): AnnotatedString {
        val context = LocalContext.current
        val spannedString = context.getText(rId) as SpannedString
        val annotations = spannedString.getSpans<android.text.Annotation>(0, spannedString.length)
        val annotatedString = buildAnnotatedString {
            append(stringResource(rId))
            annotations.forEach { annotation ->
                val start = spannedString.getSpanStart(annotation)
                val end = spannedString.getSpanEnd(annotation)
                if (annotation.key == "key") {
                    when (annotation.value) {
                        "eula" -> {
                            addStringAnnotation(
                                tag = annotation.value,
                                annotation = settings.eulaUrl,
                                start = start, end = end
                            )
                        }

                        "term" -> {
                            addStringAnnotation(
                                tag = annotation.value,
                                annotation = settings.privacyPolicyUrl,
                                start = start, end = end
                            )
                        }
                    }
                }
                addStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.fioriHorizonAttributes.SapFioriColorT4
                    ), start, end
                )
            }
        }

        return annotatedString
    }

    @Composable
    private fun PrivacyDialogContent(annotatedString: AnnotatedString) {
        val context = LocalContext.current
        ClickableText(
            text = annotatedString,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            style = MaterialTheme.fioriHorizonAttributes.textAppearanceBody1.copy(
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    start = offset, end = offset
                ).firstOrNull()?.also { annotation ->
                    if (SDKCustomTabsLauncher.customTabsSupported(context)) {
                        SDKCustomTabsLauncher.launchCustomTabs(context, annotation.item)
                    }
                }
            },
        )
    }

    override fun getFlowCustomizationSteps(
        flow: BaseFlow,
        insertionPoint: CustomStepInsertionPoint
    ) {
        if (flow.flowName == FlowType.Onboarding.name) {
            when (insertionPoint) {
                CustomStepInsertionPoint.BeforeEula -> {
                    flow.addSingleStep(step_welcome, secure = false) {
                        val context = LocalContext.current
                        val state = rememberLaunchScreenState(
                            showTermLinks = true,
                            defaultAgreeStatus = false
                        )
                        val showExportLogDialog = remember { mutableStateOf(false) }
                        val launchScreenSettings = LaunchScreenSettings(
                            titleResId = R.string.application_name,
                            contentSettings = LaunchScreenContentSettings(
                                contentImage = R.drawable.ic_sap_icon_sdk_transparent,
                                title = R.string.launch_screen_content_title,
                                content = R.string.launch_screen_content_body,
                            ),
                            bottomPrivacyUrl = "http://www.sap.com",
                            eulaUrl = "http://www.sap.com",
                            secondaryButtonCaption = R.string.export_log
                        )
                        LaunchScreen(
                            primaryViewClickListener = {
                                flow.flowDone(step_welcome)
                            },
                            secondaryViewClickListener = {
                                showExportLogDialog.value = true
                            },
                            state = state,
                            linkClickListener = { url ->
                                if (SDKCustomTabsLauncher.customTabsSupported(context)) {
                                    SDKCustomTabsLauncher.launchCustomTabs(context, url)
                                }
                            },
                            launchScreenSettings = launchScreenSettings
                        )

                        ExportLogDialog(visibleState = showExportLogDialog)

                        if (BuildConfig.FLAVOR == "tencentAppStoreforChinaMarket") {
                            var showPrivacyDialog by remember { mutableStateOf(true) }
                            var closeCount by remember { mutableIntStateOf(0) }
                            var title by remember { mutableStateOf("") }
                            var contentText by remember { mutableStateOf(AnnotatedString("")) }
                            var cBtnText by remember { mutableStateOf("") }
                            var dBtnText by remember { mutableStateOf("") }
                            var dBtnVisible by remember { mutableStateOf(false) }
                            if (closeCount == 0) {
                                val annotatedString : AnnotatedString =
                                    getLaunchScreenAnnotatedString(
                                        R.string.privacy_dialog_content,
                                        launchScreenSettings
                                    )
                                title = context.getString(R.string.launch_screen_dialog_title)
                                contentText = annotatedString
                                cBtnText = context.getString(R.string.launch_screen_dialog_button_agree)
                                dBtnVisible = true
                                dBtnText = context.getString(R.string.launch_screen_dialog_button_disagree)
                            } else {
                                val confirmationAnnotatedString: AnnotatedString =
                                    getLaunchScreenAnnotatedString(
                                        R.string.privacy_confirmation_dialog_content,
                                        launchScreenSettings
                                    )
                                title = context.getString(R.string.launch_screen_dialog_title)
                                contentText = confirmationAnnotatedString
                                cBtnText = context.getString(R.string.launch_screen_dialog_disagree_confirm)
                                dBtnVisible = false
                            }
                            if (showPrivacyDialog) {
                                val onClose: (Boolean) -> Unit = { agreed ->
                                    if (closeCount == 0) {
                                        state.agreeState.value = agreed
                                        state.showTermLinksState.value = !agreed
                                    }
                                    if (agreed) {
                                        showPrivacyDialog = false
                                    } else {
                                        if (closeCount !=0) showPrivacyDialog = false
                                        closeCount = if (closeCount == 0) 1 else 0
                                    }
                                }
                                FioriAlertDialog(
                                    modifier = Modifier.wrapContentHeight(),
                                    title = title,
                                    text = { PrivacyDialogContent(contentText) },
                                    confirmButtonText = cBtnText,
                                    onConfirmButtonClick = { onClose(true) },
                                    dismissButtonText = if (dBtnVisible) {
                                        dBtnText
                                    } else {
                                        null
                                    },
                                    onDismissButtonClick = { onClose(false) }
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    override fun shouldStartTimeoutFlow(activity: Activity): Boolean = when (activity) {
        is WelcomeActivity -> false
        else -> super.shouldStartTimeoutFlow(activity)
    }

    companion object {
        private const val step_welcome = "step_custom_welcome"
        const val temp_log_zip_file = "log.zip"
        const val log_file_name = "log.txt"
        val logger = LoggerFactory.getLogger(WizardFlowActionHandler::class.java)
    }
}

/**
 * Export log dialog
 * @param visibleState: MutableState<Boolean> to control the dialog visibility
 */
@Composable
fun ExportLogDialog(visibleState: MutableState<Boolean>) {
    var emailAddressState by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(true) }
    var errorMsg by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val emailLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            // delete log zip file
            coroutineScope.launch(Dispatchers.IO) {
                val zipFile = File(context.cacheDir, temp_log_zip_file)
                if(zipFile.exists()) {
                    zipFile.delete()
                }
            }
        }

    if(visibleState.value) {
        FioriAlertDialog(
            title = stringResource(R.string.export_log),
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FioriMarkdownText(
                        text = stringResource(R.string.export_log_dlg_description),
                        style = MaterialTheme.fioriHorizonAttributes.textAppearanceBody1
                    )

                    FioriSimpleTextField(
                        modifier = Modifier.fillMaxWidth(),
                        content = FioriTextFieldContent(
                            label = stringResource(R.string.export_log_dlg_email_label),
                            placeholder = "Sample@email.com",
                            errorMessage = errorMsg
                        ),
                        isError = isError,
                        value = emailAddressState,
                        onValueChange = {
                            emailAddressState = it
                            isError = !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()
                            errorMsg = if (isError) {
                                context.getString(R.string.export_log_dlg_invalid_email_address_msg)
                            } else {
                                ""
                            }
                        }
                    )
                    FioriPasswordTextField(
                        value = password,
                        onValueChange = { password = it },
                        content = FioriTextFieldContent(
                            label = stringResource(R.string.export_log_dlg_passcode_label),
                            placeholder = "Enter your passcode"
                        ),
                        interactionState = FioriTextFieldInteractionState.NORMAL
                    )
                }

            },
            confirmButtonText = "Send",
            onConfirmButtonClick = {
                if (!isError) {
                    visibleState.value = false
                    coroutineScope.launch {
                        pack(context, password)?.also {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddressState))
                                putExtra(Intent.EXTRA_SUBJECT, "Log file")
                                putExtra(
                                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        it
                                    )
                                )
                            }
                            emailLauncher.launch(intent)
                        }
                    }
                }
            },
            dismissButtonText = context.getString(R.string.cancel),
            onDismissButtonClick = {
                visibleState.value = false
            }
        )
    }
}

suspend fun pack(context: Context, password: String): File? = withContext(Dispatchers.IO) {
    val zipParameters = ZipParameters()
    zipParameters.compressionMethod = CompressionMethod.DEFLATE
    zipParameters.encryptionMethod = EncryptionMethod.AES
    zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
    zipParameters.isEncryptFiles = true
    zipParameters.fileNameInZip = log_file_name

    val loggingService = SDKInitializer.getService(LoggingService::class)
    loggingService?.let { service ->
        val outputZipFile = File(context.cacheDir, temp_log_zip_file)
        service.export().use {
            ZipFile(outputZipFile, password.toCharArray()).addStream(it, zipParameters)
        }
        outputZipFile
    }
}
