package com.topjohnwu.magisk.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.model.ColorMode
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.defaultGlassColors
import io.github.neoruaa.glasmio.core.glasmioColorScheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver
import io.github.neoruaa.glasmio.icons.MaterialGlasmioIconResolver

@SuppressLint("RestrictedApi")
fun dynamicColorScheme(
    seed: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0
): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), isDark, contrastLevel)
    val dyn = MaterialDynamicColors()
    fun DynamicColor.toColor() = Color(getArgb(scheme))

    return ColorScheme(
        primary = dyn.primary().toColor(),
        onPrimary = dyn.onPrimary().toColor(),
        primaryContainer = dyn.primaryContainer().toColor(),
        onPrimaryContainer = dyn.onPrimaryContainer().toColor(),
        inversePrimary = dyn.inversePrimary().toColor(),
        secondary = dyn.secondary().toColor(),
        onSecondary = dyn.onSecondary().toColor(),
        secondaryContainer = dyn.secondaryContainer().toColor(),
        onSecondaryContainer = dyn.onSecondaryContainer().toColor(),
        tertiary = dyn.tertiary().toColor(),
        onTertiary = dyn.onTertiary().toColor(),
        tertiaryContainer = dyn.tertiaryContainer().toColor(),
        onTertiaryContainer = dyn.onTertiaryContainer().toColor(),
        background = dyn.background().toColor(),
        onBackground = dyn.onBackground().toColor(),
        surface = dyn.surface().toColor(),
        onSurface = dyn.onSurface().toColor(),
        surfaceVariant = dyn.surfaceVariant().toColor(),
        onSurfaceVariant = dyn.onSurfaceVariant().toColor(),
        surfaceTint = dyn.surfaceTint().toColor(),
        inverseSurface = dyn.inverseSurface().toColor(),
        inverseOnSurface = dyn.inverseOnSurface().toColor(),
        error = dyn.error().toColor(),
        onError = dyn.onError().toColor(),
        errorContainer = dyn.errorContainer().toColor(),
        onErrorContainer = dyn.onErrorContainer().toColor(),
        outline = dyn.outline().toColor(),
        outlineVariant = dyn.outlineVariant().toColor(),
        scrim = dyn.scrim().toColor(),
        surfaceBright = dyn.surfaceBright().toColor(),
        surfaceDim = dyn.surfaceDim().toColor(),
        surfaceContainer = dyn.surfaceContainer().toColor(),
        surfaceContainerHigh = dyn.surfaceContainerHigh().toColor(),
        surfaceContainerHighest = dyn.surfaceContainerHighest().toColor(),
        surfaceContainerLow = dyn.surfaceContainerLow().toColor(),
        surfaceContainerLowest = dyn.surfaceContainerLowest().toColor(),
        primaryFixed = dyn.primaryFixed().toColor(),
        primaryFixedDim = dyn.primaryFixedDim().toColor(),
        onPrimaryFixed = dyn.onPrimaryFixed().toColor(),
        onPrimaryFixedVariant = dyn.onPrimaryFixedVariant().toColor(),
        secondaryFixed = dyn.secondaryFixed().toColor(),
        secondaryFixedDim = dyn.secondaryFixedDim().toColor(),
        onSecondaryFixed = dyn.onSecondaryFixed().toColor(),
        onSecondaryFixedVariant = dyn.onSecondaryFixedVariant().toColor(),
        tertiaryFixed = dyn.tertiaryFixed().toColor(),
        tertiaryFixedDim = dyn.tertiaryFixedDim().toColor(),
        onTertiaryFixed = dyn.onTertiaryFixed().toColor(),
        onTertiaryFixedVariant = dyn.onTertiaryFixedVariant().toColor(),
    )
}

val MagiskAccentColor = Color(0xFF00AF9C)

private object MagiskIconResolver : GlasmioIconResolver {
    override fun resolve(systemName: String) = when (systemName) {
        "power" -> Icons.Default.PowerSettingsNew
        "wrench.and.screwdriver" -> Icons.Default.Build
        "memorychip" -> Icons.Default.Memory
        "shield.lefthalf.filled" -> Icons.Default.Security
        "square.3.layers.3d" -> Icons.Default.Layers
        "arrow.up.circle" -> Icons.Default.CloudUpload
        "arrow.down.to.line" -> Icons.Default.Download
        else -> MaterialGlasmioIconResolver.resolve(systemName)
    }
}

object ThemeState {
    var colorMode by mutableIntStateOf(Config.colorMode)
}

// Same values as androidx.activity's internal DefaultLightScrim / DefaultDarkScrim
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Enable edge-to-edge with system bar appearance derived from the app's color mode
 * instead of the system's night mode.
 */
fun ComponentActivity.applySystemBarStyle() {
    val isDark = { res: Resources ->
        val nightMode = res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        ColorMode.fromValue(ThemeState.colorMode).isDark(nightMode == Configuration.UI_MODE_NIGHT_YES)
    }
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
            android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, isDark
        ),
        navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim, isDark),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

/**
 * Hosts the whole Magisk UI inside Glasmio's Liquid Glass theme and backdrop recorder.
 * The sampled layer only holds the themed background; every screen and control is drawn
 * above it as the glass overlay layer.
 */
@Composable
fun MagiskTheme(
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val mode = ColorMode.fromValue(ThemeState.colorMode)
    val context = LocalContext.current

    val isDarkTheme = mode.isDark(isDark)
    val useDynamicColor = mode.isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor && isDarkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && !isDarkTheme -> dynamicLightColorScheme(context)
        isDarkTheme -> dynamicColorScheme(MagiskAccentColor, isDark = true)
        else -> dynamicColorScheme(MagiskAccentColor, isDark = false)
    }

    val accent = colorScheme.primary
    val glasmioScheme = remember(accent, isDarkTheme) { glasmioColorScheme(isDarkTheme, accent) }
    val glassColors = remember(accent, isDarkTheme) { defaultGlassColors(isDarkTheme, accent) }

    val activity = LocalActivity.current as? ComponentActivity
    if (activity != null) {
        LaunchedEffect(activity, isDarkTheme) {
            activity.applySystemBarStyle()
        }
    }

    GlasmioIconProvider(resolver = MagiskIconResolver) {
        GlasmioTheme(
            isDark = isDarkTheme,
            colorScheme = glasmioScheme,
            colors = glassColors,
        ) {
            GlasmioBackdropHost(
                modifier = Modifier.fillMaxSize(),
                sampledContent = {
                    Box(Modifier.fillMaxSize().background(glassColors.groupedBackground))
                },
                overlayContent = { content() },
            )
        }
    }
}
