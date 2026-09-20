package com.mercadomania.game.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercadomania.game.BuildConfig
import com.mercadomania.game.R
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.ModalOverlay
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.PlateButton
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette

/**
 * Sound toggle, the offline privacy notice, a store link, and a destructive
 * reset behind a confirmation panel.
 *
 * The privacy policy is shown from the app's own resources first, so it is
 * readable with no connection at all; the web copy is one extra tap away and
 * degrades to a message when nothing on the device can open a link.
 */
private const val PRIVACY_URL = "https://mercadomania.example/privacy"

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacy by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = showPrivacy || showResetConfirm) {
        showPrivacy = false
        showResetConfirm = false
    }

    BazaarBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                backDescription = stringResource(R.string.cd_back),
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingRow {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_sound),
                            style = MaterialTheme.typography.titleMedium,
                            color = BazaarPalette.Cream
                        )
                        // State is spelled out in words as well as shown by
                        // the switch position.
                        Text(
                            text = stringResource(
                                if (soundEnabled) R.string.settings_sound_on
                                else R.string.settings_sound_off
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (soundEnabled) BazaarPalette.GemCyan else BazaarPalette.MutedSlate
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BazaarPalette.Midnight,
                            checkedTrackColor = BazaarPalette.Gold,
                            uncheckedThumbColor = BazaarPalette.Cream,
                            uncheckedTrackColor = BazaarPalette.Midnight,
                            uncheckedBorderColor = BazaarPalette.MutedSlate
                        )
                    )
                }

                SettingRow(onClick = { showPrivacy = true }) {
                    Text(
                        text = stringResource(R.string.settings_privacy),
                        style = MaterialTheme.typography.titleMedium,
                        color = BazaarPalette.Cream,
                        modifier = Modifier.weight(1f)
                    )
                    Chevron()
                }

                SettingRow(onClick = { openStoreListing(context) }) {
                    Text(
                        text = stringResource(R.string.settings_rate),
                        style = MaterialTheme.typography.titleMedium,
                        color = BazaarPalette.Cream,
                        modifier = Modifier.weight(1f)
                    )
                    Chevron()
                }

                SettingRow(onClick = { showResetConfirm = true }) {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        style = MaterialTheme.typography.titleMedium,
                        color = BazaarPalette.Ruby,
                        modifier = Modifier.weight(1f)
                    )
                    Chevron(color = BazaarPalette.Ruby)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.settings_offline_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BazaarPalette.MutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = BazaarPalette.MutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (showPrivacy) {
            ModalOverlay(onScrimClick = { showPrivacy = false }) {
                ModalPanel(title = stringResource(R.string.privacy_title)) {
                    PanelBody(
                        text = stringResource(R.string.privacy_body),
                        align = TextAlign.Start
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        PlateButton(
                            text = stringResource(R.string.settings_privacy),
                            onClick = { openUrl(context, PRIVACY_URL) },
                            textStyle = MaterialTheme.typography.labelMedium,
                            labelWidthFraction = 0.84f,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        PlateButton(
                            text = stringResource(R.string.action_close),
                            onClick = { showPrivacy = false },
                            textStyle = MaterialTheme.typography.labelMedium,
                            labelWidthFraction = 0.84f,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (showResetConfirm) {
            ModalOverlay {
                ModalPanel(title = stringResource(R.string.settings_reset_confirm_title)) {
                    PanelBody(text = stringResource(R.string.settings_reset_confirm_body))
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        PlateButton(
                            text = stringResource(R.string.action_cancel),
                            onClick = { showResetConfirm = false },
                            textStyle = MaterialTheme.typography.labelMedium,
                            labelWidthFraction = 0.84f,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        PlateButton(
                            text = stringResource(R.string.action_reset),
                            onClick = {
                                showResetConfirm = false
                                onReset()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_reset_done),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            textStyle = MaterialTheme.typography.labelMedium,
                            labelWidthFraction = 0.84f,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(BazaarPalette.PanelTop, BazaarPalette.PanelBottom)
                )
            )
            .border(2.dp, BazaarPalette.Gold.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun Chevron(color: Color = BazaarPalette.Gold) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * 0.36f, w * 0.22f)
            lineTo(w * 0.68f, w * 0.50f)
            lineTo(w * 0.36f, w * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.settings_no_browser, Toast.LENGTH_SHORT).show()
    }
}

private fun openStoreListing(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${context.packageName}")
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(marketIntent)
    } catch (e: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            context.startActivity(webIntent)
        } catch (inner: ActivityNotFoundException) {
            Toast.makeText(context, R.string.settings_no_store, Toast.LENGTH_SHORT).show()
        }
    }
}
