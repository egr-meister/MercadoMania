package com.mercadomania.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The bazaar is always lit the same way, day or night: one fixed dark scheme
 * so the artwork and the gold plates keep their intended contrast. The
 * [isSystemInDarkTheme] value is read only to keep the composable honest about
 * its dependency; both branches resolve to the same scheme by design.
 */
private val BazaarColorScheme = darkColorScheme(
    primary = BazaarPalette.Gold,
    onPrimary = BazaarPalette.Midnight,
    primaryContainer = BazaarPalette.GoldDeep,
    onPrimaryContainer = BazaarPalette.White,

    secondary = BazaarPalette.Azure,
    onSecondary = BazaarPalette.White,
    secondaryContainer = BazaarPalette.Royal,
    onSecondaryContainer = BazaarPalette.Cream,

    tertiary = BazaarPalette.GemCyan,
    onTertiary = BazaarPalette.Midnight,

    background = BazaarPalette.Midnight,
    onBackground = BazaarPalette.Cream,
    surface = BazaarPalette.Royal,
    onSurface = BazaarPalette.Cream,
    surfaceVariant = BazaarPalette.PanelTop,
    onSurfaceVariant = BazaarPalette.Cream,

    outline = BazaarPalette.Gold,
    outlineVariant = BazaarPalette.MutedSlate,

    error = BazaarPalette.Ruby,
    onError = BazaarPalette.White
)

@Composable
fun MercadoManiaTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = BazaarColorScheme,
        typography = BazaarTypography,
        content = content
    )
}
