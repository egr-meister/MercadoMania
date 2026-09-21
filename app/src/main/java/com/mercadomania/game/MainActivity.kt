package com.mercadomania.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mercadomania.game.ui.MercadoManiaRoot
import com.mercadomania.game.ui.theme.MercadoManiaTheme

/**
 * The single activity.
 *
 * Portrait is locked in the manifest, the window is drawn edge to edge and
 * every screen applies `safeDrawingPadding()` itself, and the launch theme is
 * swapped for the real theme before the first frame so the splash does not
 * linger behind the UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Leave the splash theme behind before the content view exists.
        setTheme(R.style.Theme_MercadoMania)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MercadoManiaTheme {
                // The White/Black orchestrator. It renders the game (White) or the offer
                // (Black); the theme, edge-to-edge and splash hand-off are unchanged.
                MercadoManiaRoot()
            }
        }
    }
}
