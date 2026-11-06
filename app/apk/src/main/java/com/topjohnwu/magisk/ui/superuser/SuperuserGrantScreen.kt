package com.topjohnwu.magisk.ui.superuser

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.topjohnwu.magisk.ui.component.AppSortFilterActions
import com.topjohnwu.magisk.ui.component.rememberBiometricAuth
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserGrantScreen(
    viewModel: SuperuserGrantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val showSystem by viewModel.showSystem.collectAsStateWithLifecycle()
    val showOS by viewModel.showOS.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val sortReverse by viewModel.sortReverse.collectAsStateWithLifecycle()

    val requireAuth = rememberBiometricAuth()

    val searchBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = searchBackState,
        isBackEnabled = query.isNotEmpty(),
        onBackCompleted = {
            viewModel.setQuery("")
        }
    )

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.superuser_grant),
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
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(320.dp),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = apps,
                        key = { it.info.packageName },
                        contentType = { "SuGrantAppCard" },
                    ) { app ->
                        SuGrantAppCard(
                            app = app,
                            onToggle = {
                                requireAuth {
                                    viewModel.toggleGrant(app)
                                }
                            },
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
private fun SuGrantAppCard(
    app: SuGrantAppState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuperuserAppCard(
        packageName = app.info.packageName,
        title = app.info.label,
        isSharedUid = app.info.isSharedUid,
        checked = app.isGranted,
        onCheckedChange = { onToggle() },
        contentDescription = app.info.label,
        modifier = modifier,
        trailingContent = {
            SuperuserSelectionMark(checked = app.isGranted)
        },
    )
}
