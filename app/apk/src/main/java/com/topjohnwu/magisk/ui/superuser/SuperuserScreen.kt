package com.topjohnwu.magisk.ui.superuser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.topjohnwu.magisk.ui.component.rememberBiometricAuth
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import com.topjohnwu.magisk.ui.navigation.LocalNavigator
import com.topjohnwu.magisk.ui.navigation.Route
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserScreen(
    viewModel: SuperuserViewModel,
    isFabAtBottom: Boolean,
    modifier: Modifier = Modifier,
    onRegisterFab: (((() -> Unit)?) -> Unit)? = null,
    contentFocusRequester: FocusRequester? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val requireAuth = rememberBiometricAuth()
    // Drop taps while a screen transition runs, so a quick second tap can't open another screen
    val openGrant = dropUnlessResumed { navigator.push(Route.SuperuserGrant) }

    DisposableEffect(onRegisterFab) {
        onRegisterFab?.invoke(openGrant)
        onDispose {
            onRegisterFab?.invoke(null)
        }
    }

    val gridState = rememberLazyGridState()

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.superuser),
        modifier = modifier,
        topBarBlurProgress = rememberGlasmioHeaderScrollEdge(gridState),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = GlasmioTheme.colors.accent)
                    }
                }

                uiState.policies.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                    )
                }

                else -> {
                    val listBottomPadding = padding.calculateBottomPadding() +
                        if (isFabAtBottom) 92.dp else 12.dp
                    val scrollbarBottomPadding = padding.calculateBottomPadding() +
                        if (isFabAtBottom) 80.dp else 12.dp
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(320.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScrollbar(
                                gridState,
                                contentPadding = PaddingValues(
                                    top = padding.calculateTopPadding(),
                                    bottom = scrollbarBottomPadding,
                                ),
                            ),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = padding.calculateTopPadding(),
                            end = 12.dp,
                            bottom = listBottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = uiState.policies,
                            key = { "${it.uid}_${it.packageName}" },
                            contentType = { "PolicyCard" },
                        ) { item ->
                            val isLast = item == uiState.policies.lastOrNull()
                            PolicyCard(
                                item = item,
                                onToggle = {
                                    requireAuth {
                                        viewModel.togglePolicy(item)
                                    }
                                },
                                onDetail = dropUnlessResumed {
                                    navigator.push(Route.SuperuserDetail(item.uid))
                                },
                                modifier = if (isLast && contentFocusRequester != null) {
                                    Modifier.focusRequester(contentFocusRequester)
                                } else Modifier,
                            )
                        }
                    }
                }
            }

            if (onRegisterFab == null) {
                GlasmioIconButton(
                    onClick = openGrant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 24.dp),
                    size = 56.dp,
                ) {
                    SfIcon(
                        systemName = "plus",
                        contentDescription = stringResource(CoreR.string.superuser_grant),
                        size = 26.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val colors = GlasmioTheme.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_superuser),
                contentDescription = null,
                tint = colors.secondaryContent.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = stringResource(CoreR.string.superuser_policy_none),
                style = IosTypography.body,
                color = colors.secondaryContent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PolicyCard(
    item: PolicyItem,
    onToggle: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuperuserAppCard(
        packageName = item.packageName,
        title = item.title,
        isSharedUid = item.isSharedUid,
        onClick = onDetail,
        contentDescription = item.appName,
        modifier = modifier.alpha(if (item.isEnabled) 1f else 0.5f),
        trailingContent = {
            GlasmioSwitch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle() },
            )
        },
    )
}
