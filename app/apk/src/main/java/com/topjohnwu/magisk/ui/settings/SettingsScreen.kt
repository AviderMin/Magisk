package com.topjohnwu.magisk.ui.settings

import android.webkit.URLUtil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.topjohnwu.magisk.core.utils.LocaleSetting
import com.topjohnwu.magisk.core.utils.MediaStoreUtils
import com.topjohnwu.magisk.ui.component.AdaptiveColumns
import com.topjohnwu.magisk.ui.component.MagiskSwitchPreference
import com.topjohnwu.magisk.ui.component.rememberBiometricAuth
import com.topjohnwu.magisk.ui.component.rememberNotificationPermissionLauncher
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.preference.GlasmioListPreference
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requireAuth = rememberBiometricAuth()
    val enableCheckUpdate = rememberNotificationPermissionLauncher { viewModel.setCheckUpdate(true) }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshEnvironment()
        onPauseOrDispose {}
    }

    SettingsContent(
        uiState = uiState,
        onOpenSystemLocale = {
            context.startActivity(LocaleSetting.localeSettingsIntent)
        },
        onLanguageSelected = viewModel::setLanguage,
        onColorModeSelected = viewModel::setColorMode,
        onUpdateChannelSelected = viewModel::setUpdateChannel,
        onOpenCustomUrlDialog = viewModel::openCustomChannelUrlDialog,
        onSaveCustomUrl = viewModel::saveCustomChannelUrl,
        onToggleDoh = viewModel::setDoh,
        // Update notifications need the notification permission
        onToggleCheckUpdate = { enabled ->
            if (enabled) enableCheckUpdate() else viewModel.setCheckUpdate(false)
        },
        onOpenDownloadPathDialog = viewModel::openDownloadPathDialog,
        onSaveDownloadPath = viewModel::saveDownloadPath,
        onToggleRandomPackageName = viewModel::setRandomPackageName,
        onCreateHosts = viewModel::createHosts,
        onToggleZygisk = viewModel::toggleZygisk,
        onToggleDenyList = viewModel::toggleDenyList,
        // Drop taps while a screen transition runs, so a quick second tap can't push again
        onNavigateToDenyList = dropUnlessResumed { viewModel.navigateToDenyList() },
        onToggleSuAuth = { newValue ->
            requireAuth(force = true) {
                viewModel.setSuAuth(newValue)
            }
        },
        onMultiuserModeSelected = viewModel::setMultiuserMode,
        onNamespaceModeSelected = viewModel::setNamespaceMode,
        onNotificationModeSelected = viewModel::setNotificationMode,
        onToggleSuRestrict = viewModel::setSuRestrict,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onOpenSystemLocale: () -> Unit,
    onLanguageSelected: (Int) -> Unit,
    onColorModeSelected: (Int) -> Unit,
    onUpdateChannelSelected: (Int) -> Unit,
    onOpenCustomUrlDialog: () -> Unit,
    onSaveCustomUrl: (String) -> Unit,
    onToggleDoh: (Boolean) -> Unit,
    onToggleCheckUpdate: (Boolean) -> Unit,
    onOpenDownloadPathDialog: () -> Unit,
    onSaveDownloadPath: (String) -> Unit,
    onToggleRandomPackageName: (Boolean) -> Unit,
    onCreateHosts: () -> Unit,
    onToggleZygisk: (Boolean) -> Unit,
    onToggleDenyList: (Boolean) -> Unit,
    onNavigateToDenyList: () -> Unit,
    onToggleSuAuth: (Boolean) -> Unit,
    onMultiuserModeSelected: (Int) -> Unit,
    onNamespaceModeSelected: (Int) -> Unit,
    onNotificationModeSelected: (Int) -> Unit,
    onToggleSuRestrict: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val collapseDistancePx = with(LocalDensity.current) { 56.dp.toPx() }.coerceAtLeast(1f)
    val topBarBlurProgress = (scrollState.value / collapseDistancePx).coerceIn(0f, 1f)

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.settings),
        modifier = modifier,
        topBarBlurProgress = topBarBlurProgress,
    ) { padding ->
        // Without root, the Magisk and Superuser sections are hidden, so move the
        // App section to the second column to avoid leaving it empty.
        // Each section emits a title and a grouped list, so keep them together.
        AdaptiveColumns(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(
                    scrollState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    thumbColor = GlasmioTheme.colors.secondaryContent,
                )
                .verticalScroll(scrollState),
            spacing = 16.dp,
            first = {
                CustomizationSection(
                    uiState = uiState,
                    onOpenSystemLocale = onOpenSystemLocale,
                    onLanguageSelected = onLanguageSelected,
                    onColorModeSelected = onColorModeSelected,
                )
                if (uiState.hasRootSections) {
                    AppSettingsSection(
                        uiState = uiState,
                        onUpdateChannelSelected = onUpdateChannelSelected,
                        onOpenCustomUrlDialog = onOpenCustomUrlDialog,
                        onToggleDoh = onToggleDoh,
                        onToggleCheckUpdate = onToggleCheckUpdate,
                        onOpenDownloadPathDialog = onOpenDownloadPathDialog,
                        onToggleRandomPackageName = onToggleRandomPackageName,
                    )
                }
            },
            second = {
                if (!uiState.hasRootSections) {
                    AppSettingsSection(
                        uiState = uiState,
                        onUpdateChannelSelected = onUpdateChannelSelected,
                        onOpenCustomUrlDialog = onOpenCustomUrlDialog,
                        onToggleDoh = onToggleDoh,
                        onToggleCheckUpdate = onToggleCheckUpdate,
                        onOpenDownloadPathDialog = onOpenDownloadPathDialog,
                        onToggleRandomPackageName = onToggleRandomPackageName,
                    )
                }
                if (uiState.isMagiskInstalled) {
                    MagiskSection(
                        uiState = uiState,
                        onCreateHosts = onCreateHosts,
                        onToggleZygisk = onToggleZygisk,
                        onToggleDenyList = onToggleDenyList,
                        onNavigateToDenyList = onNavigateToDenyList,
                    )
                }
                if (uiState.isSuperUserAvailable) {
                    SuperuserSection(
                        uiState = uiState,
                        onToggleSuAuth = onToggleSuAuth,
                        onMultiuserModeSelected = onMultiuserModeSelected,
                        onNamespaceModeSelected = onNamespaceModeSelected,
                        onNotificationModeSelected = onNotificationModeSelected,
                        onToggleSuRestrict = onToggleSuRestrict,
                    )
                }
            },
        )
    }

    val activeDialog = uiState.activeDialog
    UpdateChannelUrlDialog(
        initialUrl = (activeDialog as? SettingsDialog.CustomChannelUrl)?.initialUrl.orEmpty(),
        visible = activeDialog is SettingsDialog.CustomChannelUrl,
        onConfirm = onSaveCustomUrl,
        onDismiss = onDismissDialog,
    )
    DownloadPathDialog(
        initialPath = (activeDialog as? SettingsDialog.DownloadPath)?.initialPath.orEmpty(),
        visible = activeDialog is SettingsDialog.DownloadPath,
        onConfirm = onSaveDownloadPath,
        onDismiss = onDismissDialog,
    )
}

// --- Customization ---

@Composable
private fun CustomizationSection(
    uiState: SettingsUiState,
    onOpenSystemLocale: () -> Unit,
    onLanguageSelected: (Int) -> Unit,
    onColorModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlasmioPreferenceGroupTitle(stringResource(CoreR.string.settings_customization))
        GlasmioGroupedList {
            if (uiState.useLocaleManager) {
                val appLocale = LocaleSetting.instance.appLocale
                GlasmioListRow(
                    title = stringResource(CoreR.string.language),
                    subtitle = appLocale?.getDisplayName(appLocale)
                        ?: stringResource(CoreR.string.system_default),
                    onClick = onOpenSystemLocale,
                    showTopSeparator = false,
                )
            } else {
                val languages = uiState.availableLanguages
                GlasmioListPreference(
                    title = { Text(stringResource(CoreR.string.language)) },
                    selectedValue = uiState.selectedLanguageIndex,
                    values = languages.indices.toList(),
                    valueText = { index -> languages.getOrElse(index) { "" } },
                    onValueSelected = onLanguageSelected,
                    showTopSeparator = false,
                )
            }

            // Color Mode
            val colorModes = stringArrayResource(CoreR.array.color_mode).asList()
            val displayedColorModes = if (uiState.isDynamicColorSupported) colorModes else colorModes.drop(3)
            GlasmioListPreference(
                title = { Text(stringResource(CoreR.string.settings_color_mode)) },
                selectedValue = uiState.selectedColorModeIndex,
                values = displayedColorModes.indices.toList(),
                valueText = { index -> displayedColorModes.getOrElse(index) { "" } },
                onValueSelected = onColorModeSelected,
            )
        }
    }
}

// --- App Settings ---

@Composable
private fun AppSettingsSection(
    uiState: SettingsUiState,
    onUpdateChannelSelected: (Int) -> Unit,
    onOpenCustomUrlDialog: () -> Unit,
    onToggleDoh: (Boolean) -> Unit,
    onToggleCheckUpdate: (Boolean) -> Unit,
    onOpenDownloadPathDialog: () -> Unit,
    onToggleRandomPackageName: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlasmioPreferenceGroupTitle(stringResource(CoreR.string.home_app_title))
        GlasmioGroupedList {
            // Update Channel
            val updateChannels = stringArrayResource(CoreR.array.update_channel).asList()
            GlasmioListPreference(
                title = { Text(stringResource(CoreR.string.settings_update_channel_title)) },
                selectedValue = uiState.selectedUpdateChannelIndex,
                values = updateChannels.indices.toList(),
                valueText = { index -> updateChannels.getOrElse(index) { "" } },
                onValueSelected = onUpdateChannelSelected,
                showTopSeparator = false,
            )

            // Update Channel URL (for custom channel)
            if (uiState.isCustomChannel) {
                GlasmioListRow(
                    title = stringResource(CoreR.string.settings_update_custom),
                    subtitle = uiState.customChannelUrl.ifBlank { null },
                    onClick = onOpenCustomUrlDialog,
                )
            }

            // DoH Toggle
            MagiskSwitchPreference(
                title = { Text(stringResource(CoreR.string.settings_doh_title)) },
                description = stringResource(CoreR.string.settings_doh_description),
                checked = uiState.dohEnabled,
                onCheckedChange = onToggleDoh,
            )

            // Update Checker
            MagiskSwitchPreference(
                title = { Text(stringResource(CoreR.string.settings_check_update_title)) },
                description = stringResource(CoreR.string.settings_check_update_summary),
                checked = uiState.checkUpdateEnabled,
                onCheckedChange = onToggleCheckUpdate,
            )

            // Download Path
            GlasmioListRow(
                title = stringResource(CoreR.string.settings_download_path_title),
                subtitle = uiState.downloadPathDisplay,
                onClick = onOpenDownloadPathDialog,
            )

            // Random Package Name
            MagiskSwitchPreference(
                title = { Text(stringResource(CoreR.string.settings_random_name_title)) },
                description = stringResource(CoreR.string.settings_random_name_description),
                checked = uiState.randomPackageNameEnabled,
                onCheckedChange = onToggleRandomPackageName,
            )
        }
    }
}

// --- Magisk ---

@Composable
private fun MagiskSection(
    uiState: SettingsUiState,
    onCreateHosts: () -> Unit,
    onToggleZygisk: (Boolean) -> Unit,
    onToggleDenyList: (Boolean) -> Unit,
    onNavigateToDenyList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlasmioPreferenceGroupTitle(stringResource(CoreR.string.magisk))
        GlasmioGroupedList {
            // Systemless Hosts
            GlasmioListRow(
                title = stringResource(CoreR.string.settings_hosts_title),
                subtitle = stringResource(CoreR.string.settings_hosts_summary),
                onClick = onCreateHosts,
                showTopSeparator = false,
            )

            if (uiState.isZygiskSupported) {
                // Zygisk
                MagiskSwitchPreference(
                    title = { Text(stringResource(CoreR.string.zygisk)) },
                    description = stringResource(
                        if (uiState.isZygiskPendingReboot) CoreR.string.reboot_apply_change
                        else CoreR.string.settings_zygisk_summary
                    ),
                    checked = uiState.zygiskEnabled,
                    onCheckedChange = onToggleZygisk,
                )

                // DenyList
                MagiskSwitchPreference(
                    title = { Text(stringResource(CoreR.string.settings_denylist_title)) },
                    description = stringResource(CoreR.string.settings_denylist_summary),
                    checked = uiState.denyListEnabled,
                    onCheckedChange = onToggleDenyList,
                )

                // DenyList Config
                GlasmioListRow(
                    title = stringResource(CoreR.string.settings_denylist_config_title),
                    subtitle = stringResource(CoreR.string.settings_denylist_config_summary),
                    onClick = onNavigateToDenyList,
                )
            }
        }
    }
}

// --- Superuser ---

@Composable
private fun SuperuserSection(
    uiState: SettingsUiState,
    onToggleSuAuth: (Boolean) -> Unit,
    onMultiuserModeSelected: (Int) -> Unit,
    onNamespaceModeSelected: (Int) -> Unit,
    onNotificationModeSelected: (Int) -> Unit,
    onToggleSuRestrict: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GlasmioPreferenceGroupTitle(stringResource(CoreR.string.superuser))
        GlasmioGroupedList {
            // Authentication
            MagiskSwitchPreference(
                title = { Text(stringResource(CoreR.string.settings_su_auth_title)) },
                description = stringResource(
                    if (uiState.isDeviceSecure) CoreR.string.settings_su_auth_summary
                    else CoreR.string.settings_su_auth_insecure
                ),
                checked = uiState.suAuthEnabled,
                isEnabled = uiState.isDeviceSecure,
                onCheckedChange = onToggleSuAuth,
                showTopSeparator = false,
            )

            if (uiState.isMultiuserSupported) {
                // Multiuser Mode
                val multiuserSummaries = stringArrayResource(CoreR.array.multiuser_summary)
                val multiuserModes = stringArrayResource(CoreR.array.multiuser_mode).asList()
                GlasmioListPreference(
                    title = { Text(stringResource(CoreR.string.multiuser_mode)) },
                    description = multiuserSummaries.getOrElse(uiState.selectedMultiuserModeIndex) { "" },
                    selectedValue = uiState.selectedMultiuserModeIndex,
                    values = multiuserModes.indices.toList(),
                    valueText = { index -> multiuserModes.getOrElse(index) { "" } },
                    onValueSelected = onMultiuserModeSelected,
                    isEnabled = uiState.isMultiuserEditable,
                )
            }

            // Mount Namespace Mode
            val namespaceSummaries = stringArrayResource(CoreR.array.namespace_summary)
            val namespaceModes = stringArrayResource(CoreR.array.namespace).asList()
            GlasmioListPreference(
                title = { Text(stringResource(CoreR.string.mount_namespace_mode)) },
                description = namespaceSummaries.getOrElse(uiState.selectedNamespaceModeIndex) { "" },
                selectedValue = uiState.selectedNamespaceModeIndex,
                values = namespaceModes.indices.toList(),
                valueText = { index -> namespaceModes.getOrElse(index) { "" } },
                onValueSelected = onNamespaceModeSelected,
            )

            // SU Notification
            val notificationModes = stringArrayResource(CoreR.array.su_notification).asList()
            GlasmioListPreference(
                title = { Text(stringResource(CoreR.string.superuser_notification)) },
                selectedValue = uiState.selectedNotificationModeIndex,
                values = notificationModes.indices.toList(),
                valueText = { index -> notificationModes.getOrElse(index) { "" } },
                onValueSelected = onNotificationModeSelected,
            )

            // Restrict (version >= 30.1)
            if (uiState.isSuRestrictSupported) {
                MagiskSwitchPreference(
                    title = { Text(stringResource(CoreR.string.settings_su_restrict_title)) },
                    description = stringResource(CoreR.string.settings_su_restrict_summary),
                    checked = uiState.suRestrictEnabled,
                    onCheckedChange = onToggleSuRestrict,
                )
            }
        }
    }
}

// --- Dialogs ---

@Composable
private fun UpdateChannelUrlDialog(
    initialUrl: String,
    visible: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var value by rememberSaveable(visible, initialUrl, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialUrl, TextRange(initialUrl.length)))
    }
    val url = value.text
    val isValidUrl = remember(url) {
        url.isBlank() || (
            (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) &&
            URLUtil.isValidUrl(url)
        )
    }
    val focusRequester = remember { FocusRequester() }

    GlasmioAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.settings_update_custom_msg),
        modifier = modifier,
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(android.R.string.ok),
                onClick = {
                    if (isValidUrl) {
                        onConfirm(url)
                    }
                },
                enabled = isValidUrl,
            ),
        ),
    ) {
        GlasmioAlertFieldGroup {
            GlasmioAlertTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (visible && isValidUrl) onConfirm(url) },
                ),
            )
        }
        LaunchedEffect(visible) {
            if (visible) focusRequester.requestFocus()
        }
    }
}

@Composable
private fun DownloadPathDialog(
    initialPath: String,
    visible: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var value by rememberSaveable(visible, initialPath, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialPath, TextRange(initialPath.length)))
    }
    val focusRequester = remember { FocusRequester() }

    GlasmioAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.settings_download_path_title),
        message = stringResource(
            CoreR.string.settings_download_path_message,
            MediaStoreUtils.fullPath(value.text),
        ),
        modifier = modifier,
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(android.R.string.ok),
                onClick = { onConfirm(value.text.trim()) },
            ),
        ),
    ) {
        GlasmioAlertFieldGroup {
            GlasmioAlertTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (visible) onConfirm(value.text.trim()) },
                ),
            )
        }
        LaunchedEffect(visible) {
            if (visible) focusRequester.requestFocus()
        }
    }
}
