package com.mercadomania.game.ui

import androidx.annotation.DrawableRes
import com.mercadomania.game.R

/**
 * The one and only place the app names an image resource.
 *
 * Every drawable lives in `res/drawable-nodpi/` under a fixed file name, so
 * final artwork can be dropped straight over the current files without a
 * single code change:
 *
 *   bg_main.jpg       full-bleed background (opaque, so JPG)
 *   logo.png          wordmark badge
 *   mascot_a.png      Zafir the lamp genie   (host)
 *   mascot_b.png      Pico the golden gecko  (runner)
 *   mascot_c.png      Tino the automaton     (trader)
 *   btn_menu.png      ornate framed plate  (main-menu buttons)
 *   btn_plate.png     simple plate         (all other buttons)
 *   plate_round.png   round plate          (level bubbles)
 *   icon_back.png     round back button
 *   icon_pause.png    round pause button
 *   item_01..item_12  the twelve match icons
 *
 * The aspect ratios below describe the shipped placeholder art. If replacement
 * art has different proportions, change these three numbers and every button
 * and bubble in the app re-proportions itself.
 */
object Assets {

    @DrawableRes val bgMain: Int = R.drawable.bg_main
    @DrawableRes val logo: Int = R.drawable.logo

    @DrawableRes val mascotA: Int = R.drawable.mascot_a
    @DrawableRes val mascotB: Int = R.drawable.mascot_b
    @DrawableRes val mascotC: Int = R.drawable.mascot_c

    @DrawableRes val btnMenu: Int = R.drawable.btn_menu
    @DrawableRes val btnPlate: Int = R.drawable.btn_plate
    @DrawableRes val plateRound: Int = R.drawable.plate_round

    @DrawableRes val iconBack: Int = R.drawable.icon_back
    @DrawableRes val iconPause: Int = R.drawable.icon_pause

    @DrawableRes val splashLogo: Int = R.drawable.splash_logo

    /** width / height of `btn_menu.png` (ornate framed plate). */
    const val BTN_MENU_ASPECT: Float = 720f / 359f

    /** width / height of `btn_plate.png` (simple plate). */
    const val BTN_PLATE_ASPECT: Float = 660f / 443f

    /** width / height of `logo.png`. */
    const val LOGO_ASPECT: Float = 900f / 865f

    /** The three mascots, in `mascot_a..mascot_c` order. */
    val mascots: List<Int> = listOf(mascotA, mascotB, mascotC)

    /** Safe lookup - an out-of-range index falls back to the first mascot. */
    @DrawableRes
    fun mascot(index: Int): Int = mascots.getOrElse(index) { mascots.first() }

    /**
     * Drawables worth decoding on the loading screen: the ones the very first
     * frames need. Warming them means the main screen does not hitch while it
     * decodes a megabyte of artwork on the UI thread.
     */
    val preloadOnStartup: List<Int>
        get() = listOf(bgMain, logo, mascotA, mascotB, mascotC, btnMenu, btnPlate, plateRound)

    /** The twelve match icons, in `item_01..item_12` order. */
    val items: List<Int> = listOf(
        R.drawable.item_01,
        R.drawable.item_02,
        R.drawable.item_03,
        R.drawable.item_04,
        R.drawable.item_05,
        R.drawable.item_06,
        R.drawable.item_07,
        R.drawable.item_08,
        R.drawable.item_09,
        R.drawable.item_10,
        R.drawable.item_11,
        R.drawable.item_12
    )

    /**
     * Short spoken names for the match icons, used as content descriptions so
     * a face-up card is describable without relying on the picture.
     */
    val itemNames: List<String> = listOf(
        "sunburst medallion",
        "royal crown",
        "blue diamond",
        "treasure chest",
        "gilded lamp",
        "gem shield",
        "golden key",
        "hourglass",
        "compass rose",
        "jewelled scepter",
        "treasure map",
        "blue bloom"
    )

    /** Safe lookup - an out-of-range index falls back to the first icon. */
    @DrawableRes
    fun item(index: Int): Int = items.getOrElse(index) { items.first() }

    /** Safe lookup for the spoken name of a match icon. */
    fun itemName(index: Int): String = itemNames.getOrElse(index) { "bazaar relic" }
}
