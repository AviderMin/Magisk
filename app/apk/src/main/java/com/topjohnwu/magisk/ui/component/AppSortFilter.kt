package com.topjohnwu.magisk.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.topjohnwu.magisk.core.R as CoreR
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem

enum class AppSortBy(@StringRes val labelRes: Int) {
    NAME(CoreR.string.sort_by_name),
    PACKAGE_NAME(CoreR.string.sort_by_package_name),
    INSTALL_TIME(CoreR.string.sort_by_install_time),
    UPDATE_TIME(CoreR.string.sort_by_update_time);
}

@Composable
fun AppSortFilterActions(
    sortBy: AppSortBy,
    sortReverse: Boolean,
    onSortByChanged: (AppSortBy) -> Unit,
    onToggleSortReverse: () -> Unit,
    showSystem: Boolean,
    showOS: Boolean,
    onShowSystemChanged: (Boolean) -> Unit,
    onShowOSChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    GlasmioMenu(
        expanded = showSortMenu,
        onExpandedChange = { showSortMenu = it },
        itemCount = AppSortBy.entries.size + 1,
        modifier = modifier,
        anchor = { openMenu ->
            GlasmioIconButton(onClick = openMenu) {
                SfIcon(
                    "arrow.up.arrow.down",
                    contentDescription = stringResource(CoreR.string.menu_sort),
                )
            }
        },
    ) { backdrop, close ->
        AppSortBy.entries.forEach { sort ->
            GlasmioMenuItem(
                title = stringResource(sort.labelRes),
                onClick = {
                    onSortByChanged(sort)
                    close()
                },
                systemName = if (sortBy == sort) "checkmark" else null,
                backdrop = backdrop,
            )
        }
        GlasmioMenuItem(
            title = stringResource(CoreR.string.sort_reverse),
            onClick = {
                onToggleSortReverse()
                close()
            },
            systemName = if (sortReverse) "checkmark" else null,
            backdrop = backdrop,
        )
    }

    GlasmioMenu(
        expanded = showFilterMenu,
        onExpandedChange = { showFilterMenu = it },
        itemCount = 2,
        anchor = { openMenu ->
            GlasmioIconButton(onClick = openMenu) {
                SfIcon(
                    "slider.horizontal.3",
                    contentDescription = stringResource(CoreR.string.hide_filter_hint),
                )
            }
        },
    ) { backdrop, close ->
        GlasmioMenuItem(
            title = stringResource(CoreR.string.show_system_app),
            onClick = {
                onShowSystemChanged(!showSystem)
                close()
            },
            systemName = if (showSystem) "checkmark" else null,
            backdrop = backdrop,
        )
        GlasmioMenuItem(
            title = stringResource(CoreR.string.show_os_app),
            onClick = {
                if (!showOS && !showSystem) {
                    onShowSystemChanged(true)
                }
                onShowOSChanged(!showOS)
                close()
            },
            systemName = if (showOS) "checkmark" else null,
            backdrop = backdrop,
        )
    }
}
