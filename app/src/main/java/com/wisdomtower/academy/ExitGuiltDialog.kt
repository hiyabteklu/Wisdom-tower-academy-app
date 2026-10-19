package com.wisdomtower.academy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter

// D7c (docs/DAY_MODE_PLAN.md, 2026-10-19): these were fixed night literals read once at class load, so the dialog stayed a
// dark panel with white text by day. They are getters now. NIGHT KEEPS EXACTLY THE OLD VALUES; day uses the Ivory & Navy
// look (overlay = frosted ivory 92 percent, navy ink text, deep teal pill with white text).
private fun exitDayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

// Unified tokens matching the live website card language
private val DialogBg: Color get() = exitDayNight(Color(0xB30C1424), Color(0xEBF8F5EE)) // night: deep card surface (wisdom.card)
private val DialogBorder: Color get() = exitDayNight(Color(0x3322E0FF), Color(0x240B1B33)) // night: 1px subtle cyan border; day: navy hairline
private val Accent: Color get() = WisdomCyan // night 22E0FF (same as before), day deep teal
private val DarkOnCyan: Color get() = WisdomDarkOnCyan // night 070D17 (same as before), day white on the teal pill
private val Muted: Color get() = exitDayNight(Color(0xFF94A3B8), WisdomMuted)
private val OutlineBorder: Color get() = exitDayNight(Color(0x3394A3B8), Color(0x4D4A5A70))

/**
 * Clean exit confirmation styled to match website pill buttons and card surfaces.
 * Primary = Solid cyan pill (matches "Learning Hub" / "New Inquiry")
 * Secondary = Outline pill on dark surface (matches "Edit Profile")
 * M7: the card scales in from 0.94 and fades (wisdomDialogEnter); nothing else changed.
 */
@Composable
fun ExitGuiltDialog(
    onStay: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onStay,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true,
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DialogBg,
            border = BorderStroke(1.dp, DialogBorder),
            shadowElevation = 20.dp,
            modifier = Modifier
                .wisdomDialogEnter()
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exit Wisdom Tower Academy?",
                    color = WisdomInk, // night white (same as before), day navy ink
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Any downloaded materials in your Offline Vault will remain available when you return.",
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary outline pill (matches "Edit Profile" pill language)
                    OutlinedButton(
                        onClick = onStay,
                        border = BorderStroke(1.dp, OutlineBorder),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = exitDayNight(Color(0x140E1726), Color(0x0F0B1B33)),
                            contentColor = exitDayNight(Color(0xFFE2E8F0), WisdomTextPrimary)
                        )
                    ) {
                        Text(
                            text = "Stay in App",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Primary solid cyan pill (matches "Learning Hub" / "New Inquiry" button language)
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = DarkOnCyan
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Exit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
