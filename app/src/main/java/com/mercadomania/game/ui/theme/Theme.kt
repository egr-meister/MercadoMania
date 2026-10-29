package com.mercadomania.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Theme choice offered on the Settings screen, persisted as [storageKey] in
 * [com.mercadomania.game.data.Settings.themeMode].
 */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /** An unknown or missing value falls back to SYSTEM (the default). */
        fun fromStorage(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/**
 * The "Golden Bazaar" dark scheme: warm gold on deep lapis. This is the app's
 * identity and the default look.
 */
private val BazaarDarkScheme = darkColorScheme(
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

/**
 * A real light scheme so the Settings theme toggle actually changes something.
 * The full-bleed bazaar artwork stays dark by design (see [BazaarPalette]); light
 * mode re-tints Material surfaces - dialogs, switches, body text - against warm
 * parchment.
 */
private val BazaarLightScheme = lightColorScheme(
    primary = BazaarPalette.GoldDeep,
    onPrimary = BazaarPalette.White,
    primaryContainer = BazaarPalette.GoldLight,
    onPrimaryContainer = BazaarPalette.Midnight,

    secondary = BazaarPalette.Azure,
    onSecondary = BazaarPalette.White,
    secondaryContainer = BazaarPalette.GemCyan,
    onSecondaryContainer = BazaarPalette.Midnight,

    tertiary = BazaarPalette.Azure,
    onTertiary = BazaarPalette.White,

    background = BazaarPalette.LightBackground,
    onBackground = BazaarPalette.LightOnSurface,
    surface = BazaarPalette.LightSurface,
    onSurface = BazaarPalette.LightOnSurface,
    surfaceVariant = BazaarPalette.LightSurfaceVariant,
    onSurfaceVariant = BazaarPalette.LightOnSurfaceVariant,

    outline = BazaarPalette.GoldDeep,
    outlineVariant = BazaarPalette.MutedSlate,

    error = BazaarPalette.Ruby,
    onError = BazaarPalette.White
)

/**
 * @param themeMode SYSTEM follows [isSystemInDarkTheme]; LIGHT and DARK force the scheme.
 * Implemented with our own tokens rather than the Material defaults so White looks like a
 * product, not a template.
 */
@Composable
fun MercadoManiaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) BazaarDarkScheme else BazaarLightScheme,
        typography = BazaarTypography,
        content = content
    )
}
