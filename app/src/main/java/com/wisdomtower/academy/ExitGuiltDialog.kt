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

// Unified tokens matching the live website card language
private val DialogBg = Color(0xFF0C1424) // Deep card surface (wisdom.card)
private val DialogBorder = Color(0x3322E0FF) // 1px subtle cyan border
private val Accent = Color(0xFF22E0FF) // Source of truth cyan
private val DarkOnCyan = Color(0xFF070D17) // Dark text for solid cyan button
private val Muted = Color(0xFF94A3B8)
private val OutlineBorder = Color(0x3394A3B8)

/**
 * Clean exit confirmation styled to match website pill buttons and card surfaces.
 * Primary = Solid cyan pill (matches "Learning Hub" / "New Inquiry")
 * Secondary = Outline pill on dark surface (matches "Edit Profile")
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exit Wisdom Tower Academy?",
                    color = Color.White,
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
                            containerColor = Color(0x140E1726),
                            contentColor = Color(0xFFE2E8F0)
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
