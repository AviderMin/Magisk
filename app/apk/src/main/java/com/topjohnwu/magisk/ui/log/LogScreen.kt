package com.topjohnwu.magisk.ui.log

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.timeDateFormat
import com.topjohnwu.magisk.core.ktx.toTime
import com.topjohnwu.magisk.core.model.su.SuLog
import com.topjohnwu.magisk.ui.component.rememberExternalStoragePermissionLauncher
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun LogScreen(
    viewModel: LogViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val hasRoot = Info.env.isActive
    val tabTitles = listOf(
        stringResource(CoreR.string.superuser),
        stringResource(CoreR.string.magisk)
    )
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val isMagiskLogPage = !hasRoot || pagerState.currentPage == 1
    val saveMagiskLog = rememberExternalStoragePermissionLauncher {
        viewModel.saveMagiskLog()
    }
    val suGridState = rememberLazyGridState()
    val magiskListState = rememberLazyListState()
    val suScrollEdge = rememberGlasmioHeaderScrollEdge(suGridState)
    val magiskScrollEdge = rememberGlasmioHeaderScrollEdge(magiskListState)
    val topBarBlurProgress = if (hasRoot && pagerState.currentPage == 0) {
        suScrollEdge
    } else {
        magiskScrollEdge
    }

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.logs),
        modifier = modifier,
        topBarBlurProgress = topBarBlurProgress,
        actions = {
            if (isMagiskLogPage) {
                GlasmioIconButton(onClick = saveMagiskLog) {
                    SfIcon(
                        systemName = "arrow.down.to.line",
                        contentDescription = stringResource(CoreR.string.menuSaveLog),
                    )
                }
            }
            GlasmioIconButton(
                onClick = {
                    if (isMagiskLogPage) viewModel.clearMagiskLog()
                    else viewModel.clearLog()
                }
            ) {
                SfIcon(
                    systemName = "trash",
                    contentDescription = stringResource(CoreR.string.menuClearLog),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasRoot) {
                GlasmioSegmentedControl(
                    items = tabTitles.mapIndexed { index, title -> index to title },
                    selected = pagerState.currentPage,
                    onSelected = { page ->
                        scope.launch { pagerState.animateScrollToPage(page) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                )
            }

            when {
                uiState.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GlasmioTheme.colors.accent)
                    }
                }
                !hasRoot -> {
                    MagiskLogTab(
                        entries = uiState.magiskLogEntries,
                        listState = magiskListState,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        val isCurrentLogPage = pagerState.currentPage == page
                        val childOwner = rememberNavigationEventDispatcherOwner(enabled = isCurrentLogPage)
                        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusProperties {
                                        onEnter = {
                                            if (!isCurrentLogPage) {
                                                cancelFocusChange()
                                            }
                                        }
                                        onExit = {
                                            if (requestedFocusDirection == FocusDirection.Left ||
                                                requestedFocusDirection == FocusDirection.Right
                                            ) {
                                                cancelFocusChange()
                                            }
                                        }
                                    }
                                    .focusGroup()
                            ) {
                                when (page) {
                                    0 -> SuLogTab(
                                        logs = uiState.suLogs,
                                        gridState = suGridState,
                                    )
                                    1 -> MagiskLogTab(
                                        entries = uiState.magiskLogEntries,
                                        listState = magiskListState,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuLogTab(
    logs: List<SuLog>,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Column(modifier = modifier.fillMaxSize()) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(CoreR.string.log_data_none),
                    style = IosTypography.body,
                    color = colors.secondaryContent,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollbar(
                        gridState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        thumbColor = colors.secondaryContent,
                    ),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = logs,
                    key = { it.id },
                    contentType = { "SuLogCard" }
                ) { log ->
                    SuLogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun SuLogCard(
    log: SuLog,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    val res = LocalResources.current
    val pm = LocalContext.current.packageManager
    val icon = remember(log.packageName) {
        runCatching {
            pm.getApplicationInfo(log.packageName, 0).loadIcon(pm)
        }.getOrDefault(pm.defaultActivityIcon)
    }
    val allowed = log.action >= 2

    val uidPidText = buildString {
        append("UID: ${log.toUid}  PID: ${log.fromPid}")
        if (log.target != -1) {
            val target = if (log.target == 0) "magiskd" else log.target.toString()
            append("  → $target")
        }
    }

    val details = buildString {
        if (log.context.isNotEmpty()) {
            append(res.getString(CoreR.string.selinux_context, log.context))
        }
        if (log.gids.isNotEmpty()) {
            if (isNotEmpty()) append("\n")
            append(res.getString(CoreR.string.supp_group, log.gids))
        }
        if (log.command.isNotEmpty()) {
            if (isNotEmpty()) append("\n")
            append(log.command)
        }
    }

    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberDrawablePainter(icon),
                    contentDescription = log.appName,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.appName,
                            style = IosTypography.headline,
                            color = colors.content,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        SuActionBadge(allowed = allowed)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = uidPidText,
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = log.time.toTime(timeDateFormat),
                        style = IosTypography.caption2.copy(fontFamily = FontFamily.Monospace),
                        color = colors.secondaryContent,
                    )
                }
            }

            if (details.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(colors.separator)
                )
                Text(
                    text = details,
                    style = IosTypography.footnote.copy(fontFamily = FontFamily.Monospace),
                    color = colors.secondaryContent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SuActionBadge(
    allowed: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    val text = if (allowed) stringResource(CoreR.string.granted) else stringResource(CoreR.string.denied)
    LogBadge(
        text = text,
        containerColor = if (allowed) colors.accent.copy(alpha = 0.16f) else colors.destructive.copy(alpha = 0.16f),
        contentColor = if (allowed) colors.accent else colors.destructive,
        modifier = modifier,
    )
}

@Composable
private fun MagiskLogTab(
    entries: List<MagiskLogEntry>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(entries) {
        if (!didInitialScroll && entries.isNotEmpty()) {
            listState.scrollToItem(entries.lastIndex)
            didInitialScroll = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(CoreR.string.log_data_magisk_none),
                    style = IosTypography.body,
                    color = colors.secondaryContent,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollbar(
                        listState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        thumbColor = colors.secondaryContent,
                    ),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // A log reads top to bottom, so keep one column but cap its width
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(
                    items = entries,
                    key = { index, entry -> "${entry.timestamp}|${entry.pid}|$index" },
                    contentType = { _, _ -> "MagiskLogCard" },
                ) { _, entry ->
                    MagiskLogCard(entry = entry, modifier = Modifier.widthIn(max = 960.dp))
                }
            }
        }
    }
}

@Composable
private fun MagiskLogCard(
    entry: MagiskLogEntry,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    var expanded by rememberSaveable(entry.timestamp, entry.pid, entry.message) { mutableStateOf(false) }

    GlasmioCard(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (entry.isParsed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LogLevelBadge(level = entry.level)
                        if (entry.pid != 0) {
                            val pidTidText = if (entry.tid != 0 && entry.tid != entry.pid) {
                                "PID: ${entry.pid}  TID: ${entry.tid}"
                            } else {
                                "PID: ${entry.pid}"
                            }
                            Text(
                                text = pidTidText,
                                style = IosTypography.caption2.copy(fontFamily = FontFamily.Monospace),
                                color = colors.secondaryContent,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = entry.timestamp,
                        style = IosTypography.caption2.copy(fontFamily = FontFamily.Monospace),
                        color = colors.secondaryContent,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = entry.message,
                style = IosTypography.footnote.copy(fontFamily = FontFamily.Monospace),
                color = colors.content,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LogLevelBadge(
    level: Char,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    val warning = Color(0xFFFF9F0A)
    val (bg, fg) = when (level) {
        'V' -> colors.secondaryContent.copy(alpha = 0.12f) to colors.secondaryContent
        'D' -> colors.accent.copy(alpha = 0.14f) to colors.accent
        'I' -> colors.accent.copy(alpha = 0.20f) to colors.accent
        'W' -> warning.copy(alpha = 0.16f) to warning
        'E', 'F' -> colors.destructive.copy(alpha = 0.16f) to colors.destructive
        else -> colors.secondaryContent.copy(alpha = 0.12f) to colors.secondaryContent
    }
    LogBadge(
        text = level.toString(),
        containerColor = bg,
        contentColor = fg,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun LogBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = IosTypography.caption2,
            color = contentColor,
            fontWeight = fontWeight,
            maxLines = 1,
        )
    }
}
