package com.topjohnwu.magisk.ui.module

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.ui.component.rememberConfirmDialog
import com.topjohnwu.magisk.ui.component.rememberExternalStoragePermissionLauncher
import com.topjohnwu.magisk.ui.terminal.TerminalScreen
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import com.topjohnwu.magisk.core.R as CoreR

@Composable
fun ActionScreen(
    viewModel: ActionViewModel,
    actionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val finished = actionState != ActionViewModel.State.RUNNING
    val saveLog = rememberExternalStoragePermissionLauncher {
        viewModel.saveLog()
    }

    val exitConfirmDialog = rememberConfirmDialog(onConfirm = onBack)
    val exitTitle = stringResource(CoreR.string.running_exit_title)
    val exitMessage = stringResource(CoreR.string.running_exit_msg)

    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = !finished,
        onBackCompleted = {
            if (!finished) {
                exitConfirmDialog.showConfirm(
                    title = exitTitle,
                    content = exitMessage,
                )
            } else {
                onBack()
            }
        }
    )

    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    GlasmioPinnedPage(
        title = actionName,
        modifier = modifier,
        onNavigateBack = {
            if (!finished) {
                exitConfirmDialog.showConfirm(
                    title = exitTitle,
                    content = exitMessage,
                )
            } else {
                onBack()
            }
        },
        navigateBackContentDescription = stringResource(CoreR.string.back),
        actions = {
            if (finished) {
                GlasmioIconButton(onClick = saveLog) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = stringResource(CoreR.string.menuSaveLog),
                    )
                }
            }
        },
    ) { padding ->
        TerminalScreen(
            emulator = viewModel.emulator,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = navigationBarPadding,
                ),
            onResized = viewModel::onTerminalResized,
        )
    }
}
