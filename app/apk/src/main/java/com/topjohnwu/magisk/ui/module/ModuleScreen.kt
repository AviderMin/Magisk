package com.topjohnwu.magisk.ui.module

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.ui.component.ConfirmResult
import com.topjohnwu.magisk.ui.component.MagiskDialog
import com.topjohnwu.magisk.ui.component.MarkdownTextAsync
import com.topjohnwu.magisk.ui.component.rememberConfirmDialog
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import com.topjohnwu.magisk.utils.textHolder
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun ModuleScreen(
    viewModel: ModuleViewModel,
    isFabAtBottom: Boolean,
    modifier: Modifier = Modifier,
    onRegisterFab: (((() -> Unit)?) -> Unit)? = null,
    contentFocusRequester: FocusRequester? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = GlasmioTheme.colors
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    val localInstallDialog = rememberConfirmDialog()
    val confirmInstallTitle = stringResource(CoreR.string.confirm_install_title)

    var pendingOnlineModule by remember { mutableStateOf<OnlineModule?>(null) }
    var showOnlineDialog by rememberSaveable { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "module.zip"
            scope.launch {
                val result = localInstallDialog.awaitConfirm(
                    title = confirmInstallTitle,
                    content = resources.getString(CoreR.string.confirm_install, displayName),
                )
                if (result == ConfirmResult.Confirmed) {
                    viewModel.confirmLocalInstall(uri)
                }
            }
        }
    }

    DisposableEffect(onRegisterFab) {
        onRegisterFab?.invoke { filePicker.launch("application/zip") }
        onDispose {
            onRegisterFab?.invoke(null)
        }
    }

    if (showOnlineDialog && pendingOnlineModule != null) {
        OnlineModuleDialog(
            item = pendingOnlineModule!!,
            onDownload = { install ->
                showOnlineDialog = false
                (context as? MainActivity)?.let { activity ->
                    DownloadEngine.startWithActivity(
                        activity,
                        OnlineModuleSubject(pendingOnlineModule!!, install)
                    )
                }
                pendingOnlineModule = null
            },
            onDismiss = {
                showOnlineDialog = false
                pendingOnlineModule = null
            }
        )
    }

    val gridState = rememberLazyStaggeredGridState()
    val topBarBlurProgress = rememberModuleHeaderScrollEdge(gridState)
    GlasmioPinnedPage(
        title = stringResource(CoreR.string.modules),
        modifier = modifier,
        topBarBlurProgress = topBarBlurProgress,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (uiState.modules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_module),
                            contentDescription = null,
                            tint = colors.secondaryContent.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(CoreR.string.module_empty),
                            style = IosTypography.body,
                            color = colors.secondaryContent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val listBottomPadding = padding.calculateBottomPadding() + if (isFabAtBottom) 92.dp else 12.dp
                val scrollbarBottomPadding = padding.calculateBottomPadding() + if (isFabAtBottom) 80.dp else 12.dp

                // Staggered, as cards expand; 360dp leaves room for the action buttons row
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(360.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(
                            gridState,
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding(),
                                bottom = scrollbarBottomPadding,
                            ),
                            thumbColor = colors.secondaryContent,
                        ),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = padding.calculateTopPadding() + 4.dp,
                        end = 12.dp,
                        bottom = listBottomPadding,
                    ),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.modules,
                        // Module ids come from module.prop and can be duplicated, directory names can't
                        key = { it.module.base.name },
                        contentType = { "ModuleCard" }
                    ) { item ->
                        val isLast = item == uiState.modules.lastOrNull()
                        ModuleCard(
                            item = item,
                            viewModel = viewModel,
                            modifier = if (isLast && contentFocusRequester != null) {
                                Modifier.focusRequester(contentFocusRequester)
                            } else Modifier,
                            onUpdateClick = { onlineModule ->
                                if (onlineModule != null && Info.isConnected.value == true) {
                                    pendingOnlineModule = onlineModule
                                    showOnlineDialog = true
                                }
                            }
                        )
                    }
                }
            }

            if (onRegisterFab == null) {
                GlasmioIconButton(
                    onClick = { filePicker.launch("application/zip") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    size = 56.dp,
                    emphasis = GlassEmphasis.Prominent,
                ) {
                    SfIcon(
                        "plus",
                        stringResource(CoreR.string.module_action_install_external),
                        size = 28.dp,
                    )
                }
            }
        }
    }
}

/** Scroll-edge progress for the staggered grid, using the same 56dp distance as Glasmio's pages. */
@Composable
private fun rememberModuleHeaderScrollEdge(state: LazyStaggeredGridState): Float {
    val density = LocalDensity.current
    val distancePx = with(density) { 56.dp.toPx().coerceAtLeast(1f) }
    val progress by remember(state, distancePx) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)
        }
    }
    return progress
}

@Composable
private fun ModuleCard(
    item: ModuleItem,
    viewModel: ModuleViewModel,
    onUpdateClick: (OnlineModule?) -> Unit,
    modifier: Modifier = Modifier
) {
    val infoAlpha = if (!item.isRemoved && item.isEnabled && !item.showNotice) 1f else 0.5f
    val strikeThrough = if (item.isRemoved) TextDecoration.LineThrough else TextDecoration.None
    val colors = GlasmioTheme.colors
    var expanded by rememberSaveable(item.module.base.name) { mutableStateOf(false) }
    val hasDescription = item.module.description.isNotBlank()

    GlasmioCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (hasDescription) {
            { expanded = !expanded }
        } else null,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.alpha(infoAlpha)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = item.module.name,
                            style = IosTypography.headline,
                            textDecoration = strikeThrough,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                CoreR.string.module_version_author,
                                item.module.version,
                                item.module.author
                            ),
                            style = IosTypography.subheadline,
                            color = colors.secondaryContent,
                            textDecoration = strikeThrough,
                        )
                    }
                    GlasmioSwitch(
                        checked = item.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled(item) },
                        // The switch is its own node, so name the module it enables
                        modifier = Modifier.semantics { contentDescription = item.module.name },
                    )
                }

                if (hasDescription) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                    ) {
                        Text(
                            text = item.module.description,
                            style = IosTypography.subheadline,
                            color = colors.secondaryContent,
                            textDecoration = strikeThrough,
                            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                        )
                    }
                }

                if (item.showNotice) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = textHolder(item.noticeText),
                        style = IosTypography.subheadline,
                        fontWeight = FontWeight.Medium,
                        color = colors.accent,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(1.dp)
                    .background(colors.separator)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = item.isEnabled && !item.isRemoved,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.showAction) {
                            GlasmioButton(
                                // Drop taps while a screen transition runs, so a quick
                                // second tap can't open another screen
                                onClick = dropUnlessResumed {
                                    viewModel.runAction(item.module.base.name, item.module.name)
                                },
                                size = GlasmioButtonSize.Medium,
                            ) {
                                SfIcon("play.fill", null, size = 18.dp)
                                Text(stringResource(CoreR.string.module_action))
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                AnimatedVisibility(
                    visible = item.showUpdate && item.updateReady,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlasmioButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { onUpdateClick(item.module.updateInfo) },
                        size = GlasmioButtonSize.Medium,
                        emphasis = GlassEmphasis.Prominent,
                    ) {
                        SfIcon("arrow.up.circle", null, size = 18.dp)
                        Text(stringResource(CoreR.string.update))
                    }
                }

                GlasmioButton(
                    onClick = { viewModel.toggleRemove(item) },
                    enabled = !item.isUpdated,
                    size = GlasmioButtonSize.Medium,
                    destructive = !item.isRemoved,
                ) {
                    SfIcon(
                        if (item.isRemoved) "arrow.uturn.backward" else "trash",
                        null,
                        size = 18.dp,
                    )
                    Text(
                        stringResource(
                            if (item.isRemoved) CoreR.string.module_state_restore
                            else CoreR.string.module_state_remove
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineModuleDialog(
    item: OnlineModule,
    onDownload: (install: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val svc = ServiceLocator.networkService
    val title = stringResource(
        CoreR.string.repo_install_title,
        item.name, item.version, item.versionCode
    )

    MagiskDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(CoreR.string.install),
        onConfirm = { onDownload(true) },
        dismissText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        neutralText = stringResource(CoreR.string.download),
        onNeutral = { onDownload(false) },
        scrollable = true,
    ) {
        MarkdownTextAsync {
            val str = svc.fetchString(item.changelog)
            if (str.length > 1000) str.substring(0, 1000) else str
        }
    }
}
