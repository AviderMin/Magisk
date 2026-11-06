package com.topjohnwu.magisk.ui.component

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import io.github.neoruaa.glasmio.preference.GlasmioListPreference
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceEntry
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import io.github.neoruaa.glasmio.ui.GlasmioSwitch

@Composable
fun SettingsArrow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    GlasmioListRow(
        title = title,
        subtitle = summary,
        leading = leadingContent,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    MagiskSwitchPreference(
        title = { Text(title) },
        description = summary?.takeIf { it.isNotEmpty() },
        checked = checked,
        onCheckedChange = onCheckedChange,
        isEnabled = enabled,
        modifier = modifier,
    )
}

/** Exposes the entire preference as one switch instead of separate row and thumb actions. */
@Composable
fun MagiskSwitchPreference(
    title: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    showTopSeparator: Boolean = true,
) {
    GlasmioPreferenceEntry(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .toggleable(
                value = checked,
                enabled = isEnabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            GlasmioSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
        },
        onClick = null,
        isEnabled = isEnabled,
        showTopSeparator = showTopSeparator,
    )
}

@Composable
fun SettingsDropdown(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit
) {
    GlasmioListPreference(
        modifier = modifier,
        title = { Text(title) },
        description = summary?.takeIf { it.isNotEmpty() },
        selectedValue = selectedIndex,
        values = items.indices.toList(),
        valueText = { items.getOrNull(it).orEmpty() },
        onValueSelected = onSelectedIndexChange,
        isEnabled = enabled,
    )
}

@Composable
fun SmallTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    GlasmioSectionTitle(title = text, modifier = modifier)
}