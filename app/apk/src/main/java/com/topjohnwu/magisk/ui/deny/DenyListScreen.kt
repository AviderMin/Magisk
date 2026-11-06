package com.topjohnwu.magisk.ui.deny

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.AppSortFilterActions
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioCard
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun DenyListScreen(
    viewModel: DenyListViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val showSystem by viewModel.showSystem.collectAsStateWithLifecycle()
    val showOS by viewModel.showOS.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val sortReverse by viewModel.sortReverse.collectAsStateWithLifecycle()

    val searchBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = searchBackState,
        isBackEnabled = query.isNotEmpty(),
        onBackCompleted = {
            viewModel.setQuery("")
        }
    )

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.denylist),
        modifier = modifier,
        onNavigateBack = {
            if (query.isNotEmpty()) {
                viewModel.setQuery("")
            } else {
                onBack()
            }
        },
        navigateBackContentDescription = stringResource(CoreR.string.back),
        actions = {
            AppSortFilterActions(
                sortBy = sortBy,
                sortReverse = sortReverse,
                onSortByChanged = { viewModel.setSortBy(it) },
                onToggleSortReverse = { viewModel.toggleSortReverse() },
                showSystem = showSystem,
                showOS = showOS,
                onShowSystemChanged = { viewModel.setShowSystem(it) },
                onShowOSChanged = { viewModel.setShowOS(it) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            SearchInput(
                query = query,
                onQueryChange = viewModel::setQuery,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(CoreR.string.loading),
                            style = IosTypography.title3,
                            color = GlasmioTheme.colors.content,
                        )
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = GlasmioTheme.colors.accent)
                    }
                }
            } else {
                val gridState = rememberLazyStaggeredGridState()
                // Staggered, as cards expand to show processes
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(320.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(
                            gridState,
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = apps,
                        key = { it.info.packageName },
                        contentType = { "DenyAppCard" },
                    ) { app ->
                        DenyAppCard(
                            app = app,
                            onToggleExpand = { viewModel.toggleExpanded(app) },
                            onToggleAll = { viewModel.toggleAll(app) },
                            onToggleProcess = { proc -> viewModel.toggleProcess(app, proc) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val clearDescription = stringResource(CoreR.string.close)
    Row(
        modifier = modifier
            .height(GlasmioTheme.dimensions.controlHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.segmentedControlBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SfIcon(
            systemName = "magnifyingglass",
            contentDescription = null,
            size = 18.dp,
            tint = colors.secondaryContent,
            weight = FontWeight.Medium,
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = IosTypography.body.copy(color = colors.content),
            cursorBrush = SolidColor(colors.accent),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(CoreR.string.hide_filter_hint),
                            style = IosTypography.body,
                            color = colors.tertiaryContent,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = { onQueryChange("") })
                    .semantics { contentDescription = clearDescription },
                contentAlignment = Alignment.Center,
            ) {
                SfIcon(
                    systemName = "xmark.circle.fill",
                    contentDescription = null,
                    size = 18.dp,
                    tint = colors.secondaryContent,
                )
            }
        }
    }
}

@Composable
private fun DenyAppCard(
    app: DenyAppState,
    onToggleExpand: () -> Unit,
    onToggleAll: () -> Unit,
    onToggleProcess: (DenyProcessState) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            if (app.checkedPercent > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.separator)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(app.checkedPercent)
                            .height(3.dp)
                            .background(colors.accent)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    packageName = app.info.packageName,
                    contentDescription = app.info.label,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.info.label,
                        style = IosTypography.headline,
                        color = colors.content,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = app.info.packageName,
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent,
                    )
                }
                Spacer(Modifier.width(8.dp))
                DenyTriStateMark(
                    state = when {
                        app.itemsChecked == 0 -> ToggleableState.Off
                        app.checkedPercent < 1f -> ToggleableState.Indeterminate
                        else -> ToggleableState.On
                    },
                    onClick = onToggleAll,
                )
            }

            AnimatedVisibility(visible = app.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, bottom = 8.dp)
                ) {
                    app.processes.forEach { proc ->
                        ProcessRow(
                            proc = proc,
                            onToggle = { onToggleProcess(proc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessRow(
    proc: DenyProcessState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(
                value = proc.isEnabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = proc.displayName,
            style = IosTypography.subheadline,
            color = if (proc.isEnabled) colors.content else colors.secondaryContent,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        DenyProcessMark(checked = proc.isEnabled)
    }
}

/** Tri-state iOS capsule mark; Material has no Glasmio equivalent for this control. */
@Composable
private fun DenyTriStateMark(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .triStateToggleable(
                state = state,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        DenyMarkBody(state = state)
    }
}

@Composable
private fun DenyProcessMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        DenyMarkBody(state = if (checked) ToggleableState.On else ToggleableState.Off)
    }
}

@Composable
private fun DenyMarkBody(state: ToggleableState) {
    val colors = GlasmioTheme.colors
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                when (state) {
                    ToggleableState.Off -> Modifier.border(1.5.dp, colors.secondaryContent, CircleShape)
                    else -> Modifier.background(colors.accent)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ToggleableState.On -> SfIcon(
                systemName = "checkmark",
                contentDescription = null,
                size = 14.dp,
                tint = Color.White,
                weight = FontWeight.Bold,
            )
            ToggleableState.Indeterminate -> Box(
                Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
            ToggleableState.Off -> Unit
        }
    }
}
