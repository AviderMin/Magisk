package com.topjohnwu.magisk.ui.home

import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.ktx.reboot
import com.topjohnwu.magisk.core.model.UpdateInfo
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.ui.component.AdaptiveColumns
import com.topjohnwu.magisk.ui.component.LoadingDialog
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MarkdownTextAsync
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import com.topjohnwu.magisk.ui.install.InstallDialog
import com.topjohnwu.magisk.ui.install.InstallViewModel
import com.topjohnwu.magisk.ui.navigation.LocalNavigator
import com.topjohnwu.magisk.ui.navigation.Route
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import java.io.File
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    installVm: InstallViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }
    // Keep the update itself, so a restored dialog doesn't depend on Info.update, which is reset
    // on connectivity changes and after process death
    var managerUpdate by rememberSaveable { mutableStateOf<UpdateInfo?>(null) }
    var showEnvFixDialog by rememberSaveable { mutableStateOf(false) }
    var showInstallDialog by rememberSaveable { mutableStateOf(false) }
    var envFixCode by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.showUninstall) {
        if (uiState.showUninstall) {
            showUninstallDialog = true
            viewModel.onUninstallConsumed()
        }
    }
    LaunchedEffect(uiState.showManagerInstall) {
        uiState.showManagerInstall?.let {
            managerUpdate = it
            viewModel.onManagerInstallConsumed()
        }
    }
    LaunchedEffect(uiState.envFixCode) {
        if (uiState.envFixCode != 0) {
            envFixCode = uiState.envFixCode
            showEnvFixDialog = true
            viewModel.onEnvFixConsumed()
        }
    }

    LoadingDialog(visible = uiState.showLoading)

    if (showUninstallDialog) {
        UninstallComposableDialog(
            onDismiss = { showUninstallDialog = false },
            // Drop taps while a screen transition runs, so a quick second tap can't push again
            onCompleteUninstall = dropUnlessResumed {
                showUninstallDialog = false
                navigator.push(Route.Flash(Const.Value.UNINSTALL))
            },
            onRestoreImage = {
                showUninstallDialog = false
                viewModel.restoreImages()
            }
        )
    }

    managerUpdate?.let { update ->
        ManagerInstallComposableDialog(
            update = update,
            cacheDir = context.cacheDir,
            onDismiss = { managerUpdate = null },
            onInstall = {
                managerUpdate = null
                (context as? MainActivity)?.let {
                    DownloadEngine.startWithActivity(it, Subject.App(update))
                }
            }
        )
    }

    if (showEnvFixDialog) {
        EnvFixComposableDialog(
            code = envFixCode,
            onDismiss = { showEnvFixDialog = false },
            onNavigateInstall = {
                showEnvFixDialog = false
                showInstallDialog = true
            },
            onFixEnv = {
                showEnvFixDialog = false
                viewModel.fixEnv()
            }
        )
    }

    val scrollState = rememberScrollState()
    val collapseDistancePx = with(LocalDensity.current) { 56.dp.toPx() }.coerceAtLeast(1f)
    val topBarBlurProgress = (scrollState.value / collapseDistancePx).coerceIn(0f, 1f)

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.section_home),
        modifier = modifier,
        topBarBlurProgress = topBarBlurProgress,
        actions = {
            if (Info.env.isActive) {
                GlasmioIconButton(onClick = { viewModel.onDeletePressed() }) {
                    SfIcon(
                        "trash",
                        contentDescription = stringResource(CoreR.string.uninstall_magisk_title),
                    )
                }
            }
            if (Info.isRooted) {
                RebootButton()
            }
        },
    ) { padding ->
        AdaptiveColumns(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(
                    scrollState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                )
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp),
            spacing = 12.dp,
            first = {
                if (uiState.isNoticeVisible) {
                    NoticeCard(onHide = viewModel::hideNotice)
                }

                CoreCard(
                    modifier = Modifier.fillMaxWidth(),
                    state = uiState.magiskState,
                    version = uiState.magiskInstalledVersion,
                    onInstallClicked = { showInstallDialog = true }
                )

                StatusCard()

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    state = uiState.appState,
                    version = uiState.managerInstalledVersion,
                    remoteVersion = uiState.managerRemoteVersion,
                    progress = uiState.managerProgress,
                    onManagerPressed = viewModel::onManagerPressed,
                )
            },
            second = {
                SectionHeader(stringResource(CoreR.string.home_support_title))
                SupportCard(onLinkClicked = viewModel::onLinkPressed)

                SectionHeader(stringResource(CoreR.string.home_follow_title))
                DevelopersCard(onLinkClicked = viewModel::onLinkPressed)
            },
        )
    }

    InstallDialog(
        show = showInstallDialog,
        onDismiss = { showInstallDialog = false },
        installVm = installVm,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = IosTypography.footnote,
        color = GlasmioTheme.colors.secondaryContent,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun RebootButton(
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var safeModeEnabled by remember { mutableIntStateOf(Config.bootloop) }
    val colors = GlasmioTheme.colors

    val showUserspace = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        context.getSystemService<PowerManager>()?.isRebootingUserspaceSupported == true
    val showSafeMode = Const.Version.atLeast_28_0()

    val items = buildList {
        add(RebootOption(CoreR.string.reboot, "arrow.clockwise") { reboot() })
        if (showUserspace) {
            add(RebootOption(CoreR.string.reboot_userspace, "arrow.triangle.2.circlepath") { reboot("userspace") })
        }
        add(RebootOption(CoreR.string.reboot_recovery, "arrow.counterclockwise") { reboot("recovery") })
        add(RebootOption(CoreR.string.reboot_bootloader, "wrench.and.screwdriver") { reboot("bootloader") })
        add(RebootOption(CoreR.string.reboot_download, "arrow.down.circle") { reboot("download") })
        add(RebootOption(CoreR.string.reboot_edl, "memorychip") { reboot("edl") })
        if (showSafeMode) {
            add(RebootOption(CoreR.string.reboot_safe_mode, "shield.lefthalf.filled") {
                val newVal = if (safeModeEnabled >= 2) 0 else 2
                Config.bootloop = newVal
                safeModeEnabled = newVal
            })
        }
    }

    GlasmioMenu(
        expanded = showMenu,
        onExpandedChange = { showMenu = it },
        itemCount = items.size,
        modifier = modifier,
        anchor = { open ->
            GlasmioIconButton(onClick = open) {
                SfIcon(
                    "power",
                    contentDescription = stringResource(CoreR.string.reboot),
                )
            }
        },
    ) { backdrop, close ->
        items.forEach { item ->
            val isSafeMode = item.labelRes == CoreR.string.reboot_safe_mode
            GlasmioMenuItem(
                title = stringResource(item.labelRes),
                onClick = {
                    item.action()
                    if (!isSafeMode) close()
                },
                // The check mark is the only sign of the safe mode state, so expose it
                modifier = if (isSafeMode) {
                    Modifier.semantics { toggleableState = ToggleableState(safeModeEnabled >= 2) }
                } else Modifier,
                systemName = item.systemName,
                backdrop = backdrop,
                trailing = if (isSafeMode && safeModeEnabled >= 2) {
                    {
                        SfIcon(
                            "checkmark",
                            contentDescription = null,
                            size = 20.dp,
                            tint = colors.accent,
                        )
                    }
                } else null,
            )
        }
    }
}

private class RebootOption(val labelRes: Int, val systemName: String, val action: () -> Unit)

@Composable
private fun NoticeCard(
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.accent.copy(alpha = 0.12f))
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(CoreR.string.home_notice_content),
                style = IosTypography.subheadline,
                color = colors.content,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            )
            GlasmioIconButton(
                onClick = onHide,
                size = 32.dp,
                sampleBackdrop = false,
            ) {
                SfIcon(
                    "xmark",
                    contentDescription = stringResource(CoreR.string.hide),
                    size = 16.dp,
                    tint = colors.secondaryContent,
                )
            }
        }
    }
}

@Composable
private fun InstallButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    GlasmioButton(
        onClick = onClick,
        modifier = modifier,
        emphasis = if (isPrimary) GlassEmphasis.Prominent else GlassEmphasis.Regular,
        size = GlasmioButtonSize.Medium,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(text = label)
    }
}

@Composable
private fun CoreCard(
    state: HomeViewModel.State,
    version: String,
    onInstallClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val isInstalled = state != HomeViewModel.State.INVALID
    val actionLabel = when (state) {
        HomeViewModel.State.OUTDATED -> stringResource(CoreR.string.update)
        HomeViewModel.State.INVALID -> stringResource(CoreR.string.install)
        HomeViewModel.State.UP_TO_DATE -> stringResource(CoreR.string.reinstall)
        HomeViewModel.State.LOADING -> null
    }

    GlasmioCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_magisk_outline),
                    contentDescription = null,
                    tint = if (isInstalled) colors.accent else colors.tertiaryContent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(CoreR.string.magisk),
                        style = IosTypography.title3,
                        color = colors.content,
                    )
                    Text(
                        text = if (isInstalled) {
                            version.ifEmpty { stringResource(CoreR.string.not_available) }
                        } else {
                            stringResource(CoreR.string.not_installed)
                        },
                        style = IosTypography.subheadline,
                        color = if (isInstalled) colors.secondaryContent else colors.destructive,
                    )
                }
            }

            if (actionLabel != null) {
                InstallButton(
                    label = actionLabel,
                    onClick = onInstallClicked,
                    isPrimary = !isInstalled,
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    state: HomeViewModel.State,
    version: String,
    remoteVersion: String,
    progress: Int,
    onManagerPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val actionLabel = when (state) {
        HomeViewModel.State.OUTDATED -> stringResource(CoreR.string.update)
        HomeViewModel.State.UP_TO_DATE -> stringResource(CoreR.string.reinstall)
        else -> null
    }

    GlasmioCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_manager),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(CoreR.string.home_app_title),
                        style = IosTypography.title3,
                        color = colors.content,
                    )
                }

                if (actionLabel != null) {
                    InstallButton(
                        label = actionLabel,
                        onClick = onManagerPressed,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state != HomeViewModel.State.LOADING) {
                AppDetailRow(label = stringResource(CoreR.string.home_latest_version), value = remoteVersion)
            }
            AppDetailRow(label = stringResource(CoreR.string.home_installed_version), value = version)
            AppDetailRow(label = stringResource(CoreR.string.home_package), value = LocalContext.current.packageName)

            if (progress in 1..99) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.separator)
                        .progressSemantics(progress / 100f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = IosTypography.subheadline,
            color = colors.secondaryContent
        )
        Text(
            text = value,
            style = IosTypography.subheadline,
            color = colors.content
        )
    }
}

private data class StatusInfo(val label: String, val status: String)

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    val statuses = listOf(
        StatusInfo(
            label = stringResource(CoreR.string.zygisk),
            status = stringResource(if (Info.isZygiskEnabled) CoreR.string.enabled else CoreR.string.disabled)
        ),
        StatusInfo(
            label = stringResource(CoreR.string.ramdisk),
            status = stringResource(if (Info.ramdisk) CoreR.string.yes else CoreR.string.no)
        )
    )

    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            statuses.forEachIndexed { index, info ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = info.label,
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = info.status,
                        style = IosTypography.headline,
                        color = colors.content
                    )
                }
                if (index < statuses.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .fillMaxHeight()
                            .padding(vertical = 12.dp)
                            .background(colors.separator.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportCard(
    onLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(CoreR.string.home_support_content),
                style = IosTypography.subheadline,
                color = colors.secondaryContent
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlasmioButton(
                    onClick = { onLinkClicked(Const.Url.PATREON_URL) },
                    size = GlasmioButtonSize.Medium,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_patreon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = stringResource(CoreR.string.patreon))
                }
                GlasmioButton(
                    onClick = { onLinkClicked("https://paypal.me/magiskdonate") },
                    size = GlasmioButtonSize.Medium,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_paypal),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = stringResource(CoreR.string.paypal))
                }
            }
        }
    }
}

private data class LinkInfo(val label: Int, val icon: Int, val url: String)
private data class DeveloperInfo(val name: String, val links: List<LinkInfo>)

private val developers = listOf(
    DeveloperInfo("topjohnwu", listOf(
        LinkInfo(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/topjohnwu"),
        LinkInfo(CoreR.string.github, CoreR.drawable.ic_github, Const.Url.SOURCE_CODE_URL),
    )),
    DeveloperInfo("vvb2060", listOf(
        LinkInfo(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/vvb2060"),
        LinkInfo(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/vvb2060"),
    )),
    DeveloperInfo("yujincheng08", listOf(
        LinkInfo(CoreR.string.sponsor, CoreR.drawable.ic_favorite, "https://github.com/sponsors/yujincheng08"),
        LinkInfo(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/shanasaimoe"),
        LinkInfo(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/yujincheng08"),
    )),
    DeveloperInfo("aviraxp", listOf(
        LinkInfo(CoreR.string.sponsor, CoreR.drawable.ic_favorite, "https://ko-fi.com/A46115EM"),
        LinkInfo(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/aviraxp"),
    )),
    DeveloperInfo("canyie", listOf(
        LinkInfo(CoreR.string.twitter, CoreR.drawable.ic_twitter, "https://x.com/canyie2977"),
        LinkInfo(CoreR.string.github, CoreR.drawable.ic_github, "https://github.com/canyie"),
    )),
)

@Composable
private fun DevelopersCard(
    onLinkClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column {
            developers.forEachIndexed { index, dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "@${dev.name}",
                        style = IosTypography.body,
                        color = colors.content,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dev.links.forEach { link ->
                            GlasmioIconButton(
                                onClick = { onLinkClicked(link.url) },
                                size = 36.dp,
                                sampleBackdrop = false,
                            ) {
                                Icon(
                                    painter = painterResource(link.icon),
                                    contentDescription = stringResource(link.label),
                                    modifier = Modifier.size(22.dp),
                                    tint = colors.accent,
                                )
                            }
                        }
                    }
                }
                if (index < developers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(0.5.dp)
                            .background(colors.separator.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun UninstallComposableDialog(
    onDismiss: () -> Unit,
    onCompleteUninstall: () -> Unit,
    onRestoreImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.uninstall_magisk_title),
        confirmText = stringResource(CoreR.string.complete_uninstall),
        onConfirm = onCompleteUninstall,
        dismissText = stringResource(CoreR.string.restore_img),
        onDismiss = onRestoreImage,
    ) {
        Text(text = stringResource(CoreR.string.uninstall_magisk_msg))
    }
}

@Composable
private fun ManagerInstallComposableDialog(
    update: UpdateInfo,
    cacheDir: File,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.install),
        confirmText = stringResource(CoreR.string.install),
        onConfirm = onInstall,
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        scrollable = true,
    ) {
        MarkdownTextAsync {
            val text = update.note
            File(cacheDir, "${update.versionCode}.md").writeText(text)
            text
        }
    }
}

@Composable
private fun EnvFixComposableDialog(
    code: Int,
    onDismiss: () -> Unit,
    onNavigateInstall: () -> Unit,
    onFixEnv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsFullFix = code == 2 ||
        Info.env.versionCode != BuildConfig.APP_VERSION_CODE ||
        Info.env.versionString != BuildConfig.APP_VERSION_NAME

    MagiskDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = stringResource(CoreR.string.env_fix_title),
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            if (needsFullFix) onNavigateInstall() else onFixEnv()
        },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
    ) {
        Text(
            text = stringResource(
                if (needsFullFix) CoreR.string.env_full_fix_msg else CoreR.string.env_fix_msg
            )
        )
    }
}