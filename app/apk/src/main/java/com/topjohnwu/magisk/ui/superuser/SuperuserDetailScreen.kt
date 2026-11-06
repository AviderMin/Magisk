package com.topjohnwu.magisk.ui.superuser

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.ui.component.ConfirmResult
import com.topjohnwu.magisk.ui.component.MagiskSwitchPreference
import com.topjohnwu.magisk.ui.component.rememberBiometricAuth
import com.topjohnwu.magisk.ui.component.rememberConfirmDialog
import com.topjohnwu.magisk.ui.component.verticalScrollbar
import com.topjohnwu.magisk.ui.navigation.LocalNavigator
import com.topjohnwu.magisk.ui.navigation.Route
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun SuperuserDetailScreen(
    uid: Int,
    viewModel: SuperuserViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // After a revoke, the policy is removed from the state while this screen is still
    // animating out, so keep showing the last known content instead of a blank page.
    val lastItems = remember(uid) { LastItems() }
    val currentItems = uiState.policies.filter { it.uid == uid }
    val items = currentItems
        .also { if (it.isNotEmpty()) lastItems.value = it }
        .ifEmpty { lastItems.value }
    val item = items.firstOrNull()

    // If the policy disappears while this screen is still shown (e.g. the app was uninstalled),
    // leave instead of showing stale content. After a revoke, we have already navigated away.
    val navigator = LocalNavigator.current
    val isGone = !uiState.loading && currentItems.isEmpty()
    LaunchedEffect(isGone) {
        if (isGone && navigator.current() == Route.SuperuserDetail(uid)) onBack()
    }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val requireAuth = rememberBiometricAuth()
    val revokeDialog = rememberConfirmDialog()
    val revokeTitle = stringResource(CoreR.string.su_revoke_title)
    val revokeMsg = item?.let { stringResource(CoreR.string.su_revoke_msg, it.appName) } ?: ""

    LaunchedEffect(Unit) {
        viewModel.refreshSuRestrict()
        if (uiState.policies.isEmpty()) {
            viewModel.startLoading()
        }
    }

    GlasmioPinnedPage(
        title = stringResource(CoreR.string.settings),
        modifier = modifier,
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(CoreR.string.back),
    ) { padding ->
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

            item == null -> Unit

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScrollbar(
                            scrollState,
                            contentPadding = PaddingValues(vertical = 12.dp),
                        )
                        .verticalScroll(scrollState)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppInfoCard(item)
                    SettingsCard(uiState = uiState, item = item, viewModel = viewModel, requireAuth = requireAuth)
                    RevokeButton(
                        onClick = {
                            if (Config.suAuth) {
                                requireAuth { viewModel.performDelete(item, onBack) }
                            } else {
                                scope.launch {
                                    val result = revokeDialog.awaitConfirm(
                                        title = revokeTitle,
                                        content = revokeMsg,
                                    )
                                    if (result == ConfirmResult.Confirmed) {
                                        viewModel.performDelete(item, onBack)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInfoCard(item: PolicyItem) {
    val colors = GlasmioTheme.colors
    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                packageName = item.packageName,
                contentDescription = item.appName,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = IosTypography.title3,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isSharedUid) {
                        Spacer(Modifier.width(8.dp))
                        SharedUidBadge()
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.packageName,
                    style = IosTypography.subheadline,
                    color = colors.secondaryContent,
                )
                Text(
                    text = "UID: ${item.uid}",
                    style = IosTypography.subheadline,
                    color = colors.secondaryContent,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    uiState: SuperuserViewModel.UiState,
    item: PolicyItem,
    viewModel: SuperuserViewModel,
    requireAuth: (() -> Unit) -> Unit,
) {
    val showRestrict = uiState.suRestrict || item.isRestricted
    GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
        if (showRestrict) {
            MagiskSwitchPreference(
                title = { Text(stringResource(CoreR.string.settings_su_restrict_title)) },
                checked = item.isRestricted,
                onCheckedChange = {
                    requireAuth {
                        viewModel.toggleRestrict(item)
                    }
                },
                showTopSeparator = false,
            )
        }
        MagiskSwitchPreference(
            title = { Text(stringResource(CoreR.string.superuser_toggle_notification)) },
            checked = item.notification,
            onCheckedChange = { viewModel.updateNotify(item) },
            showTopSeparator = showRestrict,
        )
        MagiskSwitchPreference(
            title = { Text(stringResource(CoreR.string.logs)) },
            checked = item.logging,
            onCheckedChange = { viewModel.updateLogging(item) },
        )
    }
}

@Composable
private fun RevokeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlasmioButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        destructive = true,
        size = GlasmioButtonSize.Large,
    ) {
        SfIcon(
            systemName = "xmark.circle.fill",
            contentDescription = null,
            size = 18.dp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(CoreR.string.superuser_toggle_revoke),
            style = IosTypography.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// Plain holder: updating it must not trigger recomposition
private class LastItems {
    var value: List<PolicyItem> = emptyList()
}
