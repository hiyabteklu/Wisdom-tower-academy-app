package com.wisdomtower.academy.ui.settings

import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import com.wisdomtower.academy.ui.theme.WisdomInk
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AccountCallResult
import com.wisdomtower.academy.data.auth.AccountDeletionClient
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter
import kotlinx.coroutines.launch

/**
 * "Delete my account" block for Settings > Security (Google Play requires in-app deletion).
 * Shown only to a signed-in student. Texts match the website page `/delete-account`.
 * The student must type DELETE; the call goes to the website route through [AccountDeletionClient];
 * on success this phone is signed out and the Settings values stored on the phone are cleared.
 * M11 slice 7: the confirmation dialog scales in and fades (wisdomDialogEnter, 200 ms); closing is the platform's instant dismiss.
 * TalkBack (S3 part 3b, 2026-10-17): the "Delete my account" row is a button and the dialog's error line is announced
 * when it appears. Texts, look and logic unchanged.
 * NOT compiled, NOT tested on a phone.
 */
@Composable
fun AccountDeletionSection(settingsPrefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLoggedIn by AuthStateManager.isLoggedIn.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (!isLoggedIn) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WisdomAccentRose.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, WisdomAccentRose.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Delete Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
        Text(
            text = "Permanently delete your account and personal data. This cannot be undone.",
            fontSize = 11.sp,
            color = WisdomMuted,
            lineHeight = 15.sp
        )
        Box(
            modifier = Modifier.wisdomTouchFeel()
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, WisdomAccentRose.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) {
                    typed = ""
                    error = null
                    showDialog = true
                }
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = WisdomAccentRose, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Delete my account", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomAccentRose)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            modifier = Modifier.wisdomDialogEnter(),
            onDismissRequest = { if (!busy) showDialog = false },
            containerColor = if (AppThemeMode.isDark) Color(0xF20F1829) else Color(0xF2FBF9F4),
            title = {
                Text(text = "Delete your account?", color = WisdomInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your sign-in account, profile, learning progress, exam attempts, results, " +
                            "enrollments and unverified payment orders will be deleted. Records of verified " +
                            "payments are kept for five years and are no longer connected to your account.",
                        fontSize = 12.sp,
                        color = WisdomMuted,
                        lineHeight = 16.sp
                    )
                    Text(text = "Type DELETE to confirm.", fontSize = 12.sp, color = WisdomInk)
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        placeholder = { Text("DELETE", fontSize = 12.sp, color = WisdomMuted) },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WisdomAccentRose,
                            unfocusedBorderColor = WisdomBorderWhite,
                            focusedTextColor = WisdomInk,
                            unfocusedTextColor = WisdomInk,
                            disabledTextColor = WisdomMuted,
                            disabledBorderColor = WisdomBorderWhite
                        )
                    )
                    if (error != null) {
                        Text(
                            text = error.orEmpty(),
                            fontSize = 11.5.sp,
                            color = WisdomAccentRose,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && typed.trim() == "DELETE",
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            AuthStateManager.refreshIfNeeded()
                            val firstToken = AuthStateManager.currentUser.value?.accessToken
                            var result: AccountCallResult = if (firstToken.isNullOrBlank()) {
                                AccountCallResult.Failure("Your session expired. Please sign in again.", true)
                            } else {
                                AccountDeletionClient.deleteAccount(firstToken)
                            }
                            val first = result
                            if (first is AccountCallResult.Failure && first.sessionExpired &&
                                AuthStateManager.refreshIfNeeded(force = true)
                            ) {
                                val retryToken = AuthStateManager.currentUser.value?.accessToken
                                if (!retryToken.isNullOrBlank()) {
                                    result = AccountDeletionClient.deleteAccount(retryToken)
                                }
                            }
                            when (val done = result) {
                                is AccountCallResult.Success -> {
                                    settingsPrefs.edit().clear().apply()
                                    AuthStateManager.signOut(null)
                                    showDialog = false
                                    Toast.makeText(context, "Your account has been deleted.", Toast.LENGTH_LONG).show()
                                }
                                is AccountCallResult.Failure -> error = done.message
                            }
                            busy = false
                        }
                    }
                ) {
                    Text(
                        text = if (busy) "Deleting..." else "Delete forever",
                        color = WisdomAccentRose,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showDialog = false }) {
                    Text(text = "Cancel", color = WisdomInk)
                }
            }
        )
    }
}
