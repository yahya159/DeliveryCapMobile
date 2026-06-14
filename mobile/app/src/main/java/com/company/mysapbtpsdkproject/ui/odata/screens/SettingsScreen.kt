package com.company.mysapbtpsdkproject.ui.odata.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.qos.logback.classic.Level
import com.company.mysapbtpsdkproject.R
import com.company.mysapbtpsdkproject.ui.launchWelcomeActivity
import com.company.mysapbtpsdkproject.ui.odata.flows.CrashReportCollectionConsentFlow
import com.company.mysapbtpsdkproject.ui.odata.flows.UsageCollectionConsentFlow
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.SettingsViewModel
import com.sap.cloud.mobile.fiori.compose.theme.fioriHorizonAttributes
import com.sap.cloud.mobile.fiori.compose.dialog.FioriAlertDialog
import com.sap.cloud.mobile.fiori.compose.switch.model.FioriSwitchContent
import com.sap.cloud.mobile.fiori.compose.switch.model.ToggleLabel
import com.sap.cloud.mobile.fiori.compose.switch.ui.FioriSwitch
import com.sap.cloud.mobile.flows.compose.core.ConsentType
import com.sap.cloud.mobile.flows.compose.core.FlowContextRegistry
import com.sap.cloud.mobile.flows.compose.flows.FlowType
import com.sap.cloud.mobile.flows.compose.flows.FlowUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(), navigateUp: () -> Unit) {
    val uiState = viewModel.settingUIState.collectAsState()

    var openLogLevelDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val launchScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val dismissDialog = remember { { openLogLevelDialog = false } }
    val openDialog = remember { { openLogLevelDialog = true } }
    val updateLogLevel: (Level) -> Unit = remember {
        { level ->
            viewModel.updateLogLevel(level)
            dismissDialog.invoke()
        }
    }

    if (openLogLevelDialog) {
        FioriAlertDialog(
            title = "Log Level",
            text = { LogLevelSelection(uiState.value.level, updateLogLevel) },
            dismissButtonText = stringResource(R.string.cancel),
            onDismissButtonClick = dismissDialog
        )
    }

    OperationScreen(
        screenSettings = OperationScreenSettings(
            title = context.getString(R.string.settings_activity_name),
            navigateUp = navigateUp,
        ),
        snackbarHostState = snackbarHostState,
        viewModel = viewModel
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())){
            if (viewModel.supportLogging) {
                SettingsGroup(title = { Text(text = context.getString(R.string.logging)) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center
                    ) {
                        SettingsMenuLink(
                            title = { Text(text = context.getString(R.string.log_level)) },
                            subtitle = { Text(text = "${logStrings().associate { it }[uiState.value.level]}") },
                            onClick = openDialog
                        )

                        SettingsMenuLink(
                            title = { Text(text = context.getString(R.string.upload_log)) },
                        ) {
                            viewModel.uploadLog(lifecycleOwner)
                        }
                    }
                }

                HorizontalDivider()
            }

            SettingsGroup(title = { Text(text = context.getString(R.string.passcode)) }) {
                SettingsMenuLink(
                    title = { Text(text = context.getString(R.string.manage_passcode)) },
                ) {
                    startChangePasscodeFlow(context, launchScope, snackbarHostState)
                }
            }

            if (viewModel.supportUsage) {
                HorizontalDivider()

                SettingsGroup(title = { Text(text = context.getString(R.string.usage)) }) {
                    FioriSwitch(
                        content = FioriSwitchContent(
                            label = ToggleLabel(
                                checkedLabel = context.getString(R.string.set_usage_consent),
                                uncheckedLabel = context.getString(R.string.set_usage_consent)
                            ),
                        ),
                        checked = uiState.value.consentUsageCollection,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                viewModel.updateConsents(ConsentType.USAGE, false)
                            } else {
                                startUsageConsentFlow(context, viewModel)
                            }
                        }
                    )
                    SettingsMenuLink(
                        title = { Text(text = context.getString(R.string.upload_usage)) },
                        enabled = uiState.value.consentUsageCollection
                    ) {
                        viewModel.uploadUsageData(lifecycleOwner)
                    }
                }
            }

            if (viewModel.supportCrashReport) {
                HorizontalDivider()

                SettingsGroup(title = { Text(text = context.getString(R.string.crash_report)) }) {
                    FioriSwitch(
                        content = FioriSwitchContent(
                            label = ToggleLabel(
                                checkedLabel = context.getString(R.string.set_crash_report_consent),
                                uncheckedLabel = context.getString(R.string.set_crash_report_consent)
                            ),
                        ),
                        checked = uiState.value.consentCrashReportCollection,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                viewModel.updateConsents(ConsentType.CRASH_REPORT, false)
                            } else {
                                startCrashReportConsentFlow(context, viewModel)
                            }
                        }
                    )
                }
            }

            HorizontalDivider()

            SettingsGroup(title = { Text(text = context.getString(R.string.reset)) }) {
                SettingsMenuLink(
                    title = { Text(text = context.getString(R.string.reset_app)) },
                ) {
                    startResetAppFlow(context)
                }
            }
        }
    }
}

private fun startUsageConsentFlow(
    context: Context,
    viewModel: SettingsViewModel
) {
    val flowContext = FlowContextRegistry.flowContext.copy(
        flow = UsageCollectionConsentFlow(context)
    )
    FlowUtil.startFlow(context, flowContext) { resultCode, data ->
        if (resultCode == Activity.RESULT_OK) {
            viewModel.updateConsents(ConsentType.USAGE, true)
        }
    }
}

private fun startCrashReportConsentFlow(
    context: Context,
    viewModel: SettingsViewModel
) {
    val flowContext = FlowContextRegistry.flowContext.copy(
        flow = CrashReportCollectionConsentFlow(context)
    )
    FlowUtil.startFlow(context, flowContext) { resultCode, _ ->
        if (resultCode == Activity.RESULT_OK) {
            viewModel.updateConsents(ConsentType.CRASH_REPORT, true)
        }
    }
}

private fun startResetAppFlow(context: Context) {
    FlowUtil.startFlow(
        context = context,
        flowContext = FlowContextRegistry.flowContext.copy(
            flowType = FlowType.Reset, flow = null
        )
    ) { resultCode, _ ->
        if (resultCode == Activity.RESULT_OK) {
            launchWelcomeActivity(context)
        }
    }
}

private fun startChangePasscodeFlow(
    context: Context,
    launchScope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val flowContext =
        FlowContextRegistry.flowContext.copy(
            flowType = FlowType.ChangePasscode,
            flow = null
        )
    FlowUtil.startFlow(context = context, flowContext = flowContext) { resultCode, _ ->
        if (resultCode == Activity.RESULT_OK) {
            launchScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Passcode changed",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            if (title != null) {
                SettingsGroupTitle(title)
            }
            content()
        }
    }
}

@Composable
internal fun SettingsGroupTitle(title: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp), contentAlignment = Alignment.CenterStart
    ) {
        val primary = MaterialTheme.fioriHorizonAttributes.SapFioriColorHeaderCaption
        val titleStyle =
            MaterialTheme.fioriHorizonAttributes.textAppearanceSubtitle1.copy(color = primary)
        ProvideTextStyle(value = titleStyle) { title() }
    }
}

@Composable
fun SettingsMenuLink(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick, enabled = enabled),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsTileTexts(title = title, subtitle = subtitle, enabled = enabled)
            }
        }
    }
}

@Composable
internal fun RowScope.SettingsTileTexts(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier.Companion.weight(1f),
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title, enabled)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle)
        }
    }
}

@Composable
internal fun SettingsTileTitle(title: @Composable () -> Unit, enabled: Boolean = true) {
    val textStyle = MaterialTheme.fioriHorizonAttributes.textAppearanceSubtitle1.let {
        if(!enabled) it.copy(color = Color.LightGray) else it
    }
    ProvideTextStyle(value = textStyle) {
        title()
    }
}

@Composable
internal fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.fioriHorizonAttributes.textAppearanceCaption) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.fioriHorizonAttributes.SapFioriColorHeaderCaption,
            content = subtitle
        )
    }
}

@Composable
fun LogLevelSelection(level: Level, onSelect: (Level) -> Unit) {
    val radioOptions = logStrings()
    Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
        radioOptions.forEach { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (text.first == level),
                        onClick = { onSelect(text.first) })
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (text.first == level), onClick = { onSelect(text.first) },
                    colors = RadioButtonDefaults.colors(
                        MaterialTheme.fioriHorizonAttributes.SapFioriColorBorderSelected,
                        MaterialTheme.fioriHorizonAttributes.SapFioriColorBorderDefault
                    ),
                )
                Text(
                    text = text.second,
                    style = MaterialTheme.fioriHorizonAttributes.textAppearanceBody1.merge(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun logStrings() = listOf<Pair<Level, String>>(
    Level.ALL to stringResource(R.string.log_level_path),
    Level.DEBUG to stringResource(R.string.log_level_debug),
    Level.INFO to stringResource(R.string.log_level_info),
    Level.WARN to stringResource(R.string.log_level_warning),
    Level.ERROR to stringResource(R.string.log_level_error),
    Level.OFF to stringResource(R.string.log_level_none)
)

