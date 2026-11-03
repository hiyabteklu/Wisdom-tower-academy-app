package com.wisdomtower.academy.ui.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.AcademyNotificationManager
import com.wisdomtower.academy.data.auth.AccountCallResult
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.auth.SupabaseAccountClient
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomExpandable
import com.wisdomtower.academy.ui.theme.wisdomPress
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomOnInk
import kotlinx.coroutines.launch
import java.io.File

/**
 * Native Settings screen. Since 2026-10-19 (owner order) it holds app settings ONLY:
 *  1. Dark | Transparent pill (DisplayThemeSection in DisplaySection.kt)
 *  2. Notifications & Study Alerts
 *  3. Game Sound
 *  4. Offline Data & Cloud Sync   (size is the real app storage; Sync pushes the profile for real)
 *  5. Account & Security          (Update Password calls Supabase; "Delete Account" block, see AccountDeletionSection.kt)
 *
 * Everything about the student (header card, Study Goals & Target Milestones, My Subjects) moved to the
 * Profile screen (ui/account/AccountScreen.kt), where it is saved with one "Save Profile Changes" button.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsStorage = remember { context.getSharedPreferences("wt_settings_prefs", Context.MODE_PRIVATE) }

    val currentProfile by AuthStateManager.currentProfile.collectAsState()
    val isLoggedIn by AuthStateManager.isLoggedIn.collectAsState()

    // The daily goal is edited on the Profile screen; here it is only shown in the reminder text.
    val dailyGoalMinutes = currentProfile?.dailyStudyGoalMinutes ?: 45

    // Section expanded states
    var openNotifs by remember { mutableStateOf(false) }
    var openAudio by remember { mutableStateOf(false) }
    var openStorage by remember { mutableStateOf(false) }
    var openSecurity by remember { mutableStateOf(false) }

    // Notifications: the switches the app really obeys (AcademyNotificationManager)
    var notifTimer by remember { mutableStateOf(AcademyNotificationManager.isTimerEnabled(context)) }
    var notifPlanner by remember { mutableStateOf(AcademyNotificationManager.isPlannerEnabled(context)) }
    var notifDailyStudy by remember { mutableStateOf(AcademyNotificationManager.isGoalsEnabled(context)) }
    var notifExams by remember { mutableStateOf(AcademyNotificationManager.isUpdatesEnabled(context)) }
    var notifWeeklyDigest by remember { mutableStateOf(prefsStorage.getBoolean("notif_weekly_digest", true)) }

    // Game Sound
    var soundEffects by remember { mutableStateOf(prefsStorage.getBoolean("sound_effects", true)) }
    var soundVolume by remember { mutableFloatStateOf(prefsStorage.getFloat("sound_volume", 0.5f)) }

    // Offline Data
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncIsError by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    val storageLabel = remember(syncMessage, openStorage) { formatBytes(appStorageBytes(context)) }

    // Account & Security
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordSuccess by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordBusy by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // FIRST ITEM (owner order 2026-10-19): only the Dark | Transparent pill, always visible
            item(key = "section_display_theme") {
                DisplayThemeSection()
            }

            // Notifications & Study Alerts
            item(key = "section_notifs") {
                SettingsAccordionCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications & Study Alerts",
                    subtitle = "Daily reminders, exam alerts, and weekly digest",
                    isExpanded = openNotifs,
                    onToggle = { openNotifs = !openNotifs }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow(
                            title = "Daily Study Goal Reminder",
                            subtitle = "Receive a prompt to complete your $dailyGoalMinutes-minute daily session.",
                            checked = notifDailyStudy,
                            onCheckedChange = {
                                notifDailyStudy = it
                                AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_GOALS_ENABLED, it)
                            }
                        )

                        ToggleRow(
                            title = "Exam & Syllabus Updates",
                            subtitle = "Alert when new model exams, past papers, or questions are published.",
                            checked = notifExams,
                            onCheckedChange = {
                                notifExams = it
                                AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_UPDATES_ENABLED, it)
                            }
                        )

                        ToggleRow(
                            title = "Weekly Performance Digest",
                            subtitle = "Receive a concise weekly summary of your study hours and accuracy.",
                            checked = notifWeeklyDigest,
                            onCheckedChange = {
                                notifWeeklyDigest = it
                                prefsStorage.edit().putBoolean("notif_weekly_digest", it).apply()
                            }
                        )

                        ToggleRow(
                            title = "Study Timer Rewards",
                            subtitle = "Warm completion alerts with rewarding messages when sessions end",
                            checked = notifTimer,
                            onCheckedChange = {
                                notifTimer = it
                                AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_TIMER_ENABLED, it)
                            }
                        )

                        ToggleRow(
                            title = "Planner & Deadlines",
                            subtitle = "Personalized due-time reminders for scheduled study tasks",
                            checked = notifPlanner,
                            onCheckedChange = {
                                notifPlanner = it
                                AcademyNotificationManager.setSetting(context, AcademyNotificationManager.KEY_PLANNER_ENABLED, it)
                            }
                        )
                    }
                }
            }

            // Game Sound
            item(key = "section_audio") {
                val volumePercent = (soundVolume * 100).toInt()
                val badgeText = if (soundEffects) "$volumePercent% Volume" else "Muted"

                SettingsAccordionCard(
                    icon = Icons.Default.SportsEsports,
                    title = "Game Sound",
                    subtitle = "Audio effects and volume levels for educational games and study challenges",
                    badgeText = badgeText,
                    isExpanded = openAudio,
                    onToggle = { openAudio = !openAudio }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToggleRow(
                            title = "Game Sound Effects",
                            subtitle = "Chimes for interactive minigames and challenges",
                            checked = soundEffects,
                            onCheckedChange = {
                                soundEffects = it
                                prefsStorage.edit().putBoolean("sound_effects", it).apply()
                            }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomInk.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Game Audio Volume ($volumePercent%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WisdomInk
                                    )
                                    Text(
                                        text = "Master sound level calibrated for study games",
                                        fontSize = 11.sp,
                                        color = WisdomMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                                Text(
                                    text = "Reset 50%",
                                    fontSize = 11.sp,
                                    color = WisdomCyan,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable(role = Role.Button) {
                                            soundVolume = 0.5f
                                            prefsStorage.edit().putFloat("sound_volume", 0.5f).apply()
                                        }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.VolumeMute, contentDescription = null, tint = WisdomMuted, modifier = Modifier.size(16.dp))
                                Slider(
                                    value = soundVolume,
                                    onValueChange = {
                                        soundVolume = it
                                        prefsStorage.edit().putFloat("sound_volume", it).apply()
                                    },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.weight(1f).semantics { contentDescription = "Game audio volume" },
                                    colors = SliderDefaults.colors(
                                        thumbColor = WisdomInk,
                                        activeTrackColor = WisdomInk,
                                        inactiveTrackColor = WisdomInk.copy(alpha = 0.15f)
                                    )
                                )
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = WisdomInk, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Offline Data & Cloud Sync
            item(key = "section_storage") {
                SettingsAccordionCard(
                    icon = Icons.Default.Storage,
                    title = "Offline Data & Cloud Sync",
                    subtitle = "Cached offline data ($storageLabel) and cloud synchronization",
                    isExpanded = openStorage,
                    onToggle = { openStorage = !openStorage }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (syncMessage != null) {
                            val tone = if (syncIsError) WisdomAccentRose else WisdomAccentEmerald
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tone.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, tone.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (syncIsError) Icons.Default.Warning else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = tone,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = syncMessage.orEmpty(), fontSize = 11.5.sp, color = tone, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            com.wisdomtower.academy.ui.auth.SignedOutSignInPill() // UX-CONSISTENCY-2026-10-17
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomInk.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Total Cached Offline Data", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                                Text(
                                    text = "PDF textbooks, summaries, question banks, study logs, and offline assets.",
                                    fontSize = 11.sp,
                                    color = WisdomMuted,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(
                                text = storageLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomInk,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomCyan)
                                .clickable(role = Role.Button) {
                                    val base = currentProfile
                                    if (isSyncing) return@clickable
                                    if (!isLoggedIn || base == null) {
                                        syncIsError = true
                                        syncMessage = "Sign in to sync your profile with the cloud."
                                    } else {
                                        isSyncing = true
                                        syncMessage = null
                                        scope.launch {
                                            when (val result = AuthStateManager.saveProfile(base)) {
                                                is AccountCallResult.Success -> {
                                                    syncIsError = false
                                                    syncMessage = "Synced: your profile is saved in the cloud."
                                                }
                                                is AccountCallResult.Failure -> {
                                                    syncIsError = true
                                                    syncMessage = result.message
                                                }
                                            }
                                            isSyncing = false
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = WisdomDarkOnCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSyncing) "Syncing..." else "Sync to Cloud",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WisdomDarkOnCyan
                                )
                            }
                        }
                    }
                }
            }

            // Account & Security (password + Google Play account deletion)
            item(key = "section_security") {
                SettingsAccordionCard(
                    icon = Icons.Default.Lock,
                    title = "Account & Security",
                    subtitle = "Change password or delete your account",
                    isExpanded = openSecurity,
                    onToggle = { openSecurity = !openSecurity }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(WisdomAccentAmber.copy(alpha = 0.12f))
                                .border(BorderStroke(1.dp, WisdomAccentAmber.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WisdomAccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Account Security Notice", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WisdomAccentAmber)
                                    Text(
                                        text = "Your password will be updated and your next login will require this new password. Please make sure you record it securely.",
                                        fontSize = 12.sp,
                                        color = WisdomInk,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        if (passwordSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WisdomAccentEmerald.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, WisdomAccentEmerald.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Your password has been changed successfully. Your next login will be with this new password.",
                                    fontSize = 11.5.sp,
                                    color = WisdomAccentEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (passwordError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WisdomAccentRose.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, WisdomAccentRose.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .padding(10.dp)
                            ) {
                                Text(text = passwordError.orEmpty(), fontSize = 11.5.sp, color = WisdomAccentRose, fontWeight = FontWeight.SemiBold)
                            }
                            com.wisdomtower.academy.ui.auth.SignedOutSignInPill() // UX-CONSISTENCY-2026-10-17
                        }

                        Text(text = "Change Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password", fontSize = 11.sp) },
                            placeholder = { Text("At least 6 characters", fontSize = 11.sp, color = WisdomMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = settingsFieldColors()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password", fontSize = 11.sp) },
                            placeholder = { Text("Repeat new password", fontSize = 11.sp, color = WisdomMuted) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = settingsFieldColors()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WisdomCyan)
                                .clickable(role = Role.Button) {
                                    if (passwordBusy) return@clickable
                                    passwordSuccess = false
                                    passwordError = null
                                    val wanted = newPassword
                                    if (!isLoggedIn) {
                                        passwordError = "Sign in to change your password."
                                    } else if (wanted.length < 6) {
                                        passwordError = "Password must be at least 6 characters."
                                    } else if (wanted != confirmPassword) {
                                        passwordError = "Passwords do not match. Please re-enter."
                                    } else {
                                        passwordBusy = true
                                        scope.launch {
                                            // Real server call, same as the website's supabase.auth.updateUser({ password }).
                                            AuthStateManager.refreshIfNeeded()
                                            val firstToken = AuthStateManager.currentUser.value?.accessToken
                                            var result: AccountCallResult = if (firstToken.isNullOrBlank()) {
                                                AccountCallResult.Failure("Your session expired. Please sign in again.", true)
                                            } else {
                                                SupabaseAccountClient.updatePassword(firstToken, wanted)
                                            }
                                            val first = result
                                            if (first is AccountCallResult.Failure && first.sessionExpired &&
                                                AuthStateManager.refreshIfNeeded(force = true)
                                            ) {
                                                val retryToken = AuthStateManager.currentUser.value?.accessToken
                                                if (!retryToken.isNullOrBlank()) {
                                                    result = SupabaseAccountClient.updatePassword(retryToken, wanted)
                                                }
                                            }
                                            when (val done = result) {
                                                is AccountCallResult.Success -> {
                                                    passwordSuccess = true
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                }
                                                is AccountCallResult.Failure -> passwordError = done.message
                                            }
                                            passwordBusy = false
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (passwordBusy) "Updating Password..." else "Update Password",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomDarkOnCyan
                            )
                        }

                        // Google Play: in-app account deletion (shown only when signed in).
                        AccountDeletionSection(prefsStorage)
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/** Real size of this app's stored data on the phone (cache, files and the WebView store). */
private fun appStorageBytes(context: Context): Long {
    fun size(f: File?): Long {
        if (f == null || !f.exists()) return 0L
        if (f.isFile) return f.length()
        return f.listFiles()?.sumOf { size(it) } ?: 0L
    }
    return try {
        size(context.cacheDir) + size(context.filesDir) + size(File(context.applicationInfo.dataDir, "app_webview"))
    } catch (_: Exception) {
        0L
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes > 0L -> "${maxOf(1L, bytes / 1024L)} KB"
    else -> "0 KB"
}

@Composable
private fun SettingsAccordionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val edge by animateColorAsState(
        targetValue = if (isExpanded) WisdomCyan.copy(alpha = 0.4f) else WisdomBorderWhite,
        animationSpec = tween(240),
        label = "accordionEdge"
    )
    val accordionChevronTint by animateColorAsState(
        targetValue = if (isExpanded) WisdomCyan else WisdomMuted,
        animationSpec = tween(240),
        label = "accordionChevronTintAnim"
    )
    val chevronAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "accordionChevron"
    )
    val rowInteraction = remember { MutableInteractionSource() }
    WisdomGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = edge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" }
                    .wisdomPress(rowInteraction)
                    .clickable(interactionSource = rowInteraction, indication = null, role = Role.Button, onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WisdomInk.copy(alpha = 0.05f))
                            .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = WisdomCyan, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                            if (badgeText != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(WisdomInk.copy(alpha = 0.08f))
                                        .border(BorderStroke(1.dp, WisdomBorderWhite), CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(text = badgeText, fontSize = 12.sp, color = WisdomMuted)
                                }
                            }
                        }
                        Text(text = subtitle, fontSize = 11.sp, color = WisdomMuted)
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accordionChevronTint,
                    modifier = Modifier.size(20.dp).rotate(chevronAngle)
                )
            }

            WisdomExpandable(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WisdomInk.copy(alpha = 0.02f))
                        .border(BorderStroke(1.dp, WisdomBorderWhite.copy(alpha = 0.4f)))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WisdomInk.copy(alpha = 0.03f))
            .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
            .wisdomPress(toggleInteraction)
            .toggleable(
                value = checked,
                interactionSource = toggleInteraction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
            Text(text = subtitle, fontSize = 12.sp, color = WisdomMuted, lineHeight = 15.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WisdomOnInk,
                checkedTrackColor = WisdomInk,
                uncheckedThumbColor = WisdomInk,
                uncheckedTrackColor = WisdomInk.copy(alpha = 0.15f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WisdomCyan,
    unfocusedBorderColor = WisdomBorderWhite,
    focusedContainerColor = WisdomInk.copy(alpha = 0.06f),
    unfocusedContainerColor = WisdomInk.copy(alpha = 0.06f),
    focusedTextColor = WisdomInk,
    unfocusedTextColor = WisdomInk,
    focusedLabelColor = WisdomCyan,
    unfocusedLabelColor = WisdomMuted
)
