package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomNavy
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ADMIN-native N3 (docs/ADMIN_NATIVE_PLAN.md). The shared pieces for the native admin screens that come after
// Inquiries: search box, choice chip, tone badge, action button, confirm dialog, date text. Reuse these, do not add
// a fourth copy. (InquiriesScreen.kt still has its own private chip from N2; tidy up in the regression slice.)
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** Soft red used for rejected and destructive actions (the website uses rose). */
internal val AdminRose: Color = Color(0xFFFB7185)

@Composable
internal fun AdminSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(shape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = WisdomMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = WisdomMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
internal fun AdminChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Color = Color(0xFFF59E0B)
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) tone.copy(alpha = 0.15f) else WisdomCard)
            .border(1.dp, if (selected) tone.copy(alpha = 0.4f) else WisdomBorderWhite, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) tone else WisdomMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun AdminToneBadge(text: String, tone: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text.uppercase(),
        color = tone,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** 44 dp high pill button. filled = solid tone with white text, otherwise outlined. */
@Composable
internal fun AdminActionButton(
    label: String,
    tone: Color,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (filled) tone else Color.Transparent)
            .border(1.dp, tone.copy(alpha = 0.5f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (filled) Color.White else tone,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AdminConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    confirmTone: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WisdomNavy,
        title = {
            Text(text = title, color = WisdomTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = text, color = WisdomMuted, fontSize = 14.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = confirmTone, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = WisdomMuted)
            }
        }
    )
}

/** created_at from Supabase is UTC (+00:00). Shown in the phone's own time zone. Falls back to the raw text. */
internal fun adminFormatWhen(iso: String): String {
    if (iso.length < 19) return iso
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.substring(0, 19))
        if (date == null) iso else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
    } catch (e: Exception) {
        iso.take(16).replace('T', ' ')
    }
}
