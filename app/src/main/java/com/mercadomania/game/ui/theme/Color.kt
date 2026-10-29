package com.mercadomania.game.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ===========================================================================
 * VISUAL CONCEPT: "Golden Bazaar"
 * ===========================================================================
 * A night market of gilded relics: warm polished gold against deep lapis
 * blue, lit by gem-cyan highlights. Gold carries every interactive surface,
 * lapis carries every readable surface, cream carries every long line of text.
 *
 * PALETTE (hex + usage)
 *   #FFC800  Bazaar Gold      primary; button plates, level bubbles, emphasis
 *   #FFE44D  Light Gold       top of gold gradients, focus rings, sparkle
 *   #B87400  Deep Gold        bottom of gold gradients, plate shadow line
 *   #02133F  Midnight Lapis   window + scrim base, darkest panel stop
 *   #0A2A8C  Royal Blue       panel fill, card backs, secondary containers
 *   #1546D6  Azure            panel gradient top, selected states
 *   #33B5FF  Gem Cyan         progress, timers, tertiary accents, links
 *   #FFF7DC  Parchment Cream  body text on dark, panel body copy
 *   #FFFFFF  White            ALL button labels (bold, on the plate)
 *   #1FA34A  Emerald          correct answer (always paired with a check mark)
 *   #E0342C  Ruby             wrong answer (always paired with a cross)
 *   #7B8BB8  Muted Slate      disabled / locked, secondary meta text
 *
 * Colour is never the only signal: correct/wrong answers also carry a glyph
 * and a spoken state description, locked levels also carry a lock glyph and a
 * content description, and the sound toggle also carries an on/off label.
 * ===========================================================================
 */
object BazaarPalette {
    val Gold = Color(0xFFFFC800)
    val GoldLight = Color(0xFFFFE44D)
    val GoldDeep = Color(0xFFB87400)

    val Midnight = Color(0xFF02133F)
    val Royal = Color(0xFF0A2A8C)
    val Azure = Color(0xFF1546D6)
    val GemCyan = Color(0xFF33B5FF)

    val Cream = Color(0xFFFFF7DC)
    val White = Color(0xFFFFFFFF)

    val Emerald = Color(0xFF1FA34A)
    val Ruby = Color(0xFFE0342C)
    val MutedSlate = Color(0xFF7B8BB8)

    /** Scrim laid over the photographic background so text always clears AA. */
    val ScrimTop = Color(0xCC02133F)
    val ScrimMid = Color(0x9902133F)
    val ScrimBottom = Color(0xF202133F)

    /** Panel gradient stops for Compose-drawn modal panels. */
    val PanelTop = Color(0xFF16307F)
    val PanelBottom = Color(0xFF061845)

    // -----------------------------------------------------------------------
    // Light theme. The bazaar artwork is a fixed dark photograph, so light mode
    // re-tints the Material surfaces (dialogs, switches, text) against a warm
    // parchment rather than repainting the whole screen; the backdrop stays dark
    // by design. These exist so the Settings theme toggle is a real change.
    // -----------------------------------------------------------------------
    val LightBackground = Color(0xFFFBF3DD)
    val LightSurface = Color(0xFFFFFDF5)
    val LightSurfaceVariant = Color(0xFFEDE0B8)
    val LightOnSurface = Color(0xFF241A05)
    val LightOnSurfaceVariant = Color(0xFF5A4A1A)
}
