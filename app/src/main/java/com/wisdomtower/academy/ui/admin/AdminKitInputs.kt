package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ADMIN-native N4 (docs/ADMIN_NATIVE_PLAN.md). More shared pieces for the native admin screens, kept next to
// AdminKit.kt so that file was not resent: a one-line text field, a message card (empty state or error with an
// optional retry button) and a date-only formatter. Reuse these; merge both kit files in the regression slice.
// N6: AdminTextField got an optional keyboardType (default Text, so earlier callers are unchanged).
// N6c: AdminTextArea (several lines) added.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** One-line text input in the same look as AdminSearchBox, without the magnifier. 44 dp high. */
@Composable
internal fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(shape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
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
    )
}

/** Multi-line text input in the same look as AdminTextField. Grows with the text, never smaller than minHeight. */
@Composable
internal fun AdminTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 120.dp
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = false,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minHeight)
                    .clip(shape)
                    .background(WisdomCard)
                    .border(1.dp, WisdomBorderWhite, shape)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

/** Centered card for an empty state or an error. actionLabel null = no button. */
@Composable
internal fun AdminMessageCard(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            color = WisdomMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            AdminActionButton(
                label = actionLabel,
                tone = WisdomCyan,
                filled = false,
                enabled = true,
                onClick = onAction
            )
        }
    }
}

/** Date only, in the phone's time zone. created_at from Supabase is UTC. Falls back to the first 10 characters. */
internal fun adminFormatDay(iso: String): String {
    if (iso.length < 19) return iso.take(10)
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.substring(0, 19))
        if (date == null) iso.take(10) else DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
    } catch (e: Exception) {
        iso.take(10)
    }
}
