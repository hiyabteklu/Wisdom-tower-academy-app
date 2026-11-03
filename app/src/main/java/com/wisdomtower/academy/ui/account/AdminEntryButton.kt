package com.wisdomtower.academy.ui.account

import androidx.compose.foundation.BorderStroke
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.AdminAccess
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple

/**
 * ADMIN-entry: the "Admin Console" button on the Account screen. Draws nothing unless the database confirms
 * the signed-in account is an admin ([AdminAccess.isAdmin]). [userKey] only restarts the check when the
 * signed-in account changes; it is never stored. Tapping calls [onOpen] (the Account screen sends "/admin").
 * Purple, like the website's Admin button.
 *
 * TalkBack (S3 part 2c, 2026-10-17): the row is announced as a button (Role.Button). Nothing else changed.
 *
 * STATUS: `[~]` written, NOT compiled, NOT tested on a device.
 */
@Composable
fun AdminEntryButton(userKey: String?, onOpen: () -> Unit) {
    var isAdmin by remember(userKey) { mutableStateOf(false) }
    LaunchedEffect(userKey) { isAdmin = AdminAccess.isAdmin() }
    if (!isAdmin) return

    val accent = if (AppThemeMode.isDark) Color(0xFFC084FC) else WisdomAccentPurple // D5: day mode
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.wisdomTouchFeel()
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Admin Console", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}
