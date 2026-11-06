package com.topjohnwu.magisk.ui.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.hardware.usb.UsbManager
import android.net.Uri
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.registerRuntimeReceiver
import com.topjohnwu.magisk.core.repository.FirmwareCrawler
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.displayName
import com.topjohnwu.magisk.core.utils.UsbBlockDevice
import com.topjohnwu.magisk.ui.component.ConfirmResult
import com.topjohnwu.magisk.ui.component.MarkdownText
import com.topjohnwu.magisk.ui.component.rememberConfirmDialog
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import io.github.neoruaa.glasmio.ui.GlasmioSheetButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetToolbarButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import io.github.neoruaa.glasmio.ui.GlasmioTextField
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun InstallDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    installVm: InstallViewModel,
    modifier: Modifier = Modifier
) {
    val installUiState by installVm.uiState.collectAsStateWithLifecycle()
    var showPatchDialog by rememberSaveable { mutableStateOf(false) }

    val secondSlotDialog = rememberConfirmDialog()
    val secondSlotTitle = stringResource(android.R.string.dialog_alert_title)
    val secondSlotMsg = stringResource(CoreR.string.install_inactive_slot_msg)

    LaunchedEffect(installUiState.showSecondSlotWarning) {
        if (installUiState.showSecondSlotWarning) {
            val result = secondSlotDialog.awaitConfirm(title = secondSlotTitle, content = secondSlotMsg)
            installVm.onSecondSlotWarningConsumed()
            if (result == ConfirmResult.Confirmed) {
                installVm.install()
            }
        }
    }

    LaunchedEffect(installUiState.showPatchDialog) {
        if (installUiState.showPatchDialog) {
            showPatchDialog = true
            installVm.onPatchDialogConsumed()
        }
    }

    if (showPatchDialog) {
        PatchComposableDialog(
            candidate = installUiState.candidate,
            showUsbDestination = installVm.isGooglebook,
            onDismiss = { showPatchDialog = false },
            onConfirm = { uri, usbDevice ->
                showPatchDialog = false
                installVm.onPatchSelected(uri, usbDevice)
            }
        )
    }

    val listState = rememberLazyListState()
    val topBarBlurProgress = rememberGlasmioHeaderScrollEdge(listState)

    GlasmioBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        visible = show,
        title = stringResource(CoreR.string.install),
        topBarBlurProgress = topBarBlurProgress,
        topBar = {
            GlasmioSheetTopToolbar(
                title = stringResource(CoreR.string.install),
                navigation = {
                    GlasmioSheetToolbarButton(onClick = onDismiss) {
                        SfIcon(
                            "xmark",
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                },
            )
        },
    ) {
        val sheetPadding = LocalGlasmioSheetContentPadding.current
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = sheetPadding.calculateTopPadding() + 8.dp,
                bottom = sheetPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (installUiState.notes.isNotEmpty()) {
                item {
                    MarkdownText(installUiState.notes)
                }
            }

            if (!installVm.skipOptions) {
                item {
                    InstallOptionsSection()
                }
            }

            item {
                GlasmioGroupedList {
                    GlasmioListRow(
                        title = stringResource(CoreR.string.patch_image),
                        showTopSeparator = false,
                        onClick = {
                            onDismiss()
                            installVm.selectMethod(InstallViewModel.Method.PATCH)
                        },
                    )

                    if (installVm.isRooted) {
                        GlasmioListRow(
                            title = stringResource(CoreR.string.direct_install),
                            onClick = {
                                onDismiss()
                                installVm.selectMethod(InstallViewModel.Method.DIRECT)
                                installVm.install()
                            },
                        )
                    }

                    if (!installVm.noSecondSlot) {
                        GlasmioListRow(
                            title = stringResource(CoreR.string.install_inactive_slot),
                            onClick = {
                                onDismiss()
                                installVm.selectMethod(InstallViewModel.Method.INACTIVE_SLOT)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallOptionsSection(
    modifier: Modifier = Modifier
) {
    GlasmioGroupedList(modifier = modifier) {
        var showTopSeparator = false

        if (!Info.isSAR) {
            var keepVerity by remember { mutableStateOf(Config.keepVerity) }
            GlasmioListRow(
                title = stringResource(CoreR.string.keep_dm_verity),
                showTopSeparator = showTopSeparator,
                onClick = {
                    keepVerity = !keepVerity
                    Config.keepVerity = keepVerity
                },
                trailing = {
                    GlasmioSwitch(
                        checked = keepVerity,
                        onCheckedChange = {
                            keepVerity = it
                            Config.keepVerity = it
                        },
                    )
                },
            )
            showTopSeparator = true
        }
        if (Info.isFDE) {
            var keepEnc by remember { mutableStateOf(Config.keepEnc) }
            GlasmioListRow(
                title = stringResource(CoreR.string.keep_force_encryption),
                showTopSeparator = showTopSeparator,
                onClick = {
                    keepEnc = !keepEnc
                    Config.keepEnc = keepEnc
                },
                trailing = {
                    GlasmioSwitch(
                        checked = keepEnc,
                        onCheckedChange = {
                            keepEnc = it
                            Config.keepEnc = it
                        },
                    )
                },
            )
            showTopSeparator = true
        }
        if (!Info.ramdisk) {
            var recovery by remember { mutableStateOf(Config.recovery) }
            GlasmioListRow(
                title = stringResource(CoreR.string.recovery_mode),
                showTopSeparator = showTopSeparator,
                onClick = {
                    recovery = !recovery
                    Config.recovery = recovery
                },
                trailing = {
                    GlasmioSwitch(
                        checked = recovery,
                        onCheckedChange = {
                            recovery = it
                            Config.recovery = it
                        },
                    )
                },
            )
        }
    }
}

private fun parseHttpsUrl(url: String): Uri? {
    if (url.isEmpty()) return null
    val uri = url.toUri()
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (uri.host.isNullOrEmpty()) return null
    if (uri.path.isNullOrEmpty()) return null
    return uri
}

private enum class PatchSource {
    CANDIDATE, FILE, URL
}

@Composable
fun PatchComposableDialog(
    onDismiss: () -> Unit,
    onConfirm: (Uri, String?) -> Unit,
    modifier: Modifier = Modifier,
    candidate: FirmwareCrawler.Candidate? = null,
    showUsbDestination: Boolean = false,
) {
    val context = LocalContext.current
    // The user's choice, defaulting to the downloadable image when there is one. Not keyed on the
    // candidate: it is only found after the firmware search, which runs again after the process
    // is recreated, and must not reset a choice made before that
    var chosenSource by rememberSaveable { mutableStateOf<PatchSource?>(null) }
    val source = chosenSource ?: if (candidate != null) PatchSource.CANDIDATE else PatchSource.FILE
    var localFileUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingSubmitAfterPick by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var isUrlError by rememberSaveable { mutableStateOf(false) }

    var usbDevices by remember {
        mutableStateOf(
            if (showUsbDestination) UsbBlockDevice.listDevices(context) else emptyList()
        )
    }
    var selectedUsb by rememberSaveable { mutableStateOf<String?>(null) }
    var preferFile by rememberSaveable { mutableStateOf(false) }

    if (showUsbDestination) {
        DisposableEffect(context) {
            usbDevices = UsbBlockDevice.listDevices(context)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    usbDevices = UsbBlockDevice.listDevices(context)
                }
            }
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            context.registerRuntimeReceiver(receiver, filter)
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }

        LaunchedEffect(usbDevices) {
            if (selectedUsb != null && usbDevices.none { it.deviceName == selectedUsb }) {
                selectedUsb = usbDevices.firstOrNull()?.deviceName
            } else if (selectedUsb == null && !preferFile && usbDevices.isNotEmpty()) {
                selectedUsb = usbDevices.first().deviceName
            }
        }
    }

    val confirmWithUri = { targetUri: Uri ->
        val usbTarget = if (showUsbDestination) {
            selectedUsb?.takeIf { name -> usbDevices.any { it.deviceName == name } }
        } else {
            null
        }
        onConfirm(targetUri, usbTarget)
    }

    val localFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val shouldSubmit = pendingSubmitAfterPick
        pendingSubmitAfterPick = false
        if (uri != null) {
            localFileUri = uri
            chosenSource = PatchSource.FILE
            if (shouldSubmit) {
                confirmWithUri(uri)
            }
        }
    }

    val submit = submit@{
        val targetUri = when (source) {
            PatchSource.CANDIDATE -> candidate?.url?.toUri() ?: return@submit
            PatchSource.FILE -> localFileUri ?: run {
                pendingSubmitAfterPick = true
                localFilePicker.launch("*/*")
                return@submit
            }
            PatchSource.URL -> parseHttpsUrl(url.trim()) ?: run {
                isUrlError = true
                return@submit
            }
        }
        confirmWithUri(targetUri)
    }

    val listState = rememberLazyListState()
    val topBarBlurProgress = rememberGlasmioHeaderScrollEdge(listState)

    GlasmioBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(CoreR.string.patch_image),
        topBarBlurProgress = topBarBlurProgress,
        topBar = {
            GlasmioSheetTopToolbar(
                title = stringResource(CoreR.string.patch_image),
                navigation = {
                    GlasmioSheetToolbarButton(onClick = onDismiss) {
                        SfIcon(
                            "xmark",
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                },
                actions = {
                    GlasmioSheetButton(onClick = submit) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
            )
        },
    ) {
        val sheetPadding = LocalGlasmioSheetContentPadding.current
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = sheetPadding.calculateTopPadding() + 8.dp,
                bottom = sheetPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PatchMessage()
            }

            item {
                val colors = GlasmioTheme.colors
                var showTopSeparator = false
                Column(modifier = Modifier.selectableGroup()) {
                    GlasmioGroupedList {
                        if (candidate != null) {
                            PatchSourceRow(
                                title = candidate.title,
                                subtitle = candidate.description.takeIf { it.isNotEmpty() },
                                selected = source == PatchSource.CANDIDATE,
                                showTopSeparator = showTopSeparator,
                                onClick = { chosenSource = PatchSource.CANDIDATE },
                            )
                            showTopSeparator = true
                        }

                        val fileName = remember(localFileUri) {
                            localFileUri?.let { runCatching { it.displayName }.getOrNull() }
                        }
                        PatchSourceRow(
                            title = stringResource(CoreR.string.select_from_storage),
                            subtitle = fileName?.takeIf { it.isNotEmpty() },
                            selected = source == PatchSource.FILE,
                            showTopSeparator = showTopSeparator,
                            onClick = {
                                chosenSource = PatchSource.FILE
                                pendingSubmitAfterPick = false
                                localFilePicker.launch("*/*")
                            },
                        )
                        showTopSeparator = true

                        PatchSourceRow(
                            title = stringResource(CoreR.string.download_dialog_title),
                            selected = source == PatchSource.URL,
                            showTopSeparator = showTopSeparator,
                            onClick = { chosenSource = PatchSource.URL },
                        )

                        if (source == PatchSource.URL) {
                            GlasmioTextField(
                                value = url,
                                onValueChange = {
                                    url = it
                                    isUrlError = false
                                },
                                placeholder = stringResource(CoreR.string.download_dialog_msg),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submit() }
                                ),
                            )
                            if (isUrlError) {
                                Text(
                                    text = stringResource(CoreR.string.download_dialog_title),
                                    style = IosTypography.footnote,
                                    color = colors.destructive,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (showUsbDestination) {
                item {
                    GlasmioSectionTitle(stringResource(CoreR.string.output_destination))
                }

                item {
                    val colors = GlasmioTheme.colors
                    var showTopSeparator = false
                    Column(modifier = Modifier.selectableGroup()) {
                        GlasmioGroupedList {
                            PatchSourceRow(
                                title = stringResource(CoreR.string.save_to_file),
                                selected = selectedUsb == null,
                                showTopSeparator = showTopSeparator,
                                onClick = {
                                    preferFile = true
                                    selectedUsb = null
                                },
                            )
                            showTopSeparator = true

                            for (device in usbDevices) {
                                PatchSourceRow(
                                    title = "USB: ${device.label}",
                                    selected = selectedUsb == device.deviceName,
                                    showTopSeparator = showTopSeparator,
                                    onClick = {
                                        preferFile = false
                                        selectedUsb = device.deviceName
                                    },
                                )
                                showTopSeparator = true
                            }
                        }

                        if (usbDevices.isEmpty()) {
                            Text(
                                text = stringResource(CoreR.string.no_usb_drive_found),
                                style = IosTypography.footnote,
                                color = colors.secondaryContent,
                                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                            )
                        } else if (selectedUsb != null) {
                            Text(
                                text = stringResource(CoreR.string.usb_overwrite_warning),
                                style = IosTypography.footnote,
                                color = colors.destructive,
                                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PatchSourceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showTopSeparator: Boolean = true,
) {
    val colors = GlasmioTheme.colors
    GlasmioListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton,
        ),
        onClick = null,
        showTopSeparator = showTopSeparator,
        trailing = {
            if (selected) {
                SfIcon(
                    "checkmark",
                    contentDescription = null,
                    size = 20.dp,
                    tint = colors.accent,
                )
            }
        },
    )
}

@Composable
private fun PatchMessage() {
    val resources = LocalResources.current
    val boldColor = GlasmioTheme.colors.content
    val secondaryColor = GlasmioTheme.colors.secondaryContent
    val lines = remember(resources, boldColor) {
        val cs = resources.getText(CoreR.string.patch_image_msg)
        val full = buildAnnotatedString {
            append(cs.toString())
            if (cs is Spanned) {
                for (span in cs.getSpans(0, cs.length, StyleSpan::class.java)) {
                    if (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC) {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor),
                            cs.getSpanStart(span),
                            cs.getSpanEnd(span)
                        )
                    }
                }
            }
        }
        buildList {
            var start = 0
            while (start <= full.length) {
                val end = full.indexOf('\n', start).let { if (it == -1) full.length else it }
                add(full.subSequence(start, end))
                start = end + 1
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            if (line.startsWith("• ")) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "•",
                        style = IosTypography.footnote,
                        color = secondaryColor,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    )
                    Text(
                        text = line.subSequence(2, line.length),
                        style = IosTypography.footnote,
                        color = secondaryColor
                    )
                }
            } else if (line.isNotEmpty()) {
                Text(
                    text = line,
                    style = IosTypography.subheadline,
                    color = secondaryColor
                )
            }
        }
    }
}
