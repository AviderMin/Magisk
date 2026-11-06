package com.topjohnwu.magisk.ui.superuser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.ui.component.AppIcon
import com.topjohnwu.magisk.core.R as CoreR
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioCard

@Composable
internal fun SharedUidBadge(modifier: Modifier = Modifier) {
    val colors = GlasmioTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.segmentedControlBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(CoreR.string.shared_uid),
            style = IosTypography.caption2,
            fontWeight = FontWeight.SemiBold,
            color = colors.secondaryContent,
            maxLines = 1,
        )
    }
}

@Composable
private fun SuperuserAppRow(
    packageName: String,
    title: String,
    isSharedUid: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = packageName,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = IosTypography.headline,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isSharedUid) {
                    Spacer(Modifier.width(6.dp))
                    SharedUidBadge()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = packageName,
                style = IosTypography.subheadline,
                color = colors.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent?.let {
            Spacer(Modifier.width(8.dp))
            it()
        }
    }
}

@Composable
fun SuperuserAppListItem(
    packageName: String,
    title: String,
    isSharedUid: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SuperuserAppRow(
        packageName = packageName,
        title = title,
        isSharedUid = isSharedUid,
        modifier = modifier,
        trailingContent = trailingContent,
    )
}

/**
 * Glasmio card used for both the browseable policy list and the grant list.
 * When [checked]/[onCheckedChange] are supplied the whole card acts as a checkbox row;
 * otherwise [onClick] turns it into a button card.
 */
@Composable
fun SuperuserAppCard(
    packageName: String,
    title: String,
    isSharedUid: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val selectable = checked != null && onCheckedChange != null
    GlasmioCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        onClick = if (!selectable && enabled) onClick else null,
    ) {
        SuperuserAppRow(
            packageName = packageName,
            title = title,
            isSharedUid = isSharedUid,
            modifier = if (selectable) {
                Modifier.toggleable(
                    value = checked == true,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { value -> onCheckedChange?.invoke(value) },
                )
            } else Modifier,
            trailingContent = trailingContent,
        )
    }
}

/** A compact iOS-style checkmark used as the trailing accessory of a selectable card. */
@Composable
internal fun SuperuserSelectionMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(colors.accent)
                else Modifier.border(1.5.dp, colors.secondaryContent, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            SfIcon(
                systemName = "checkmark",
                contentDescription = null,
                size = 14.dp,
                tint = Color.White,
                weight = FontWeight.Bold,
            )
        }
    }
}
