package com.wisdomtower.academy.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.AdminCounts
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

// ADMIN-native N1 (docs/ADMIN_NATIVE_PLAN.md). The Admin Console home, written in Compose.
// Design language = the website admin home grid (docs/ADMIN_UI_V2_PLAN.md section 3): dark cards, gold icon chips,
// 3 columns on phones 340 dp and wider (2 below), real counts only, pill links at the bottom.
// One vertical scroller (LazyColumn), no horizontal scrolling anywhere, no blur, no animation loops.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

@Composable
internal fun AdminHomeScreen(
    counts: AdminCounts?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpenModule: (AdminModule) -> Unit,
    onLiveAcademy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) ADMIN_MODULES
        else ADMIN_MODULES.filter { it.label.lowercase().contains(q) || it.id.contains(q) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth < 340.dp) 2 else 3
        val rows = filtered.chunked(columns)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Console",
                            color = WisdomTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Wisdom Tower Academy",
                            color = WisdomMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search sections",
                            tint = if (searchOpen) WisdomCyan else WisdomMuted
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh counts",
                            tint = WisdomMuted,
                            modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                        )
                    }
                }
            }

            if (searchOpen) {
                item(key = "search") {
                    AdminSearchField(query = query, onQueryChange = { query = it })
                }
            }

            item(key = "stats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatTile("Scholars", counts?.scholars, Modifier.weight(1f))
                    AdminStatTile("Pending payments", counts?.pendingPayments, Modifier.weight(1f))
                    AdminStatTile("New inquiries", counts?.newInquiries, Modifier.weight(1f))
                }
            }

            val examReviewCount = counts?.pendingExamReviews ?: 0
            val deviceAttemptCount = counts?.pendingDeviceAttempts ?: 0
            if (examReviewCount > 0 || deviceAttemptCount > 0) {
                item(key = "security-attention") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WisdomAccentAmber.copy(alpha = 0.07f))
                            .border(1.dp, WisdomAccentAmber.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SECURITY REVIEW QUEUE",
                            color = WisdomAccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (examReviewCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        adminModuleForTab("exam-integrity")?.let(onOpenModule)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exam attempts awaiting review", color = WisdomTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                AdminAttentionBadge(examReviewCount, WisdomAccentAmber)
                            }
                        }
                        if (deviceAttemptCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        adminModuleForTab("device-security")?.let(onOpenModule)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Blocked sign-ins awaiting review", color = WisdomTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                AdminAttentionBadge(deviceAttemptCount, WisdomAccentAmber)
                            }
                        }
                    }
                }
            }

            items(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (module in row) {
                        val raw: Int? = when (module.countKind) {
                            AdminCountKind.SCHOLARS -> counts?.scholars
                            AdminCountKind.PAYMENTS -> counts?.pendingPayments
                            AdminCountKind.INQUIRIES -> counts?.newInquiries
                            AdminCountKind.EXAM_FLAGS -> counts?.pendingExamReviews
                            AdminCountKind.DEVICE_ATTEMPTS -> counts?.pendingDeviceAttempts
                            AdminCountKind.NONE -> null
                        }
                        val badge = raw?.takeIf { it > 0 }
                        AdminModuleTile(
                            module = module,
                            badge = badge,
                            onClick = { onOpenModule(module) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    for (i in row.size until columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "No sections match",
                        color = WisdomMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            item(key = "links") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminPill(
                        text = "Live Academy",
                        icon = Icons.Filled.Public,
                        onClick = onLiveAcademy,
                        modifier = Modifier.weight(1f)
                    )
                    AdminPill(
                        text = "Supabase",
                        icon = Icons.Filled.Storage,
                        onClick = {
                            try {
                                val open = Intent(Intent.ACTION_VIEW, Uri.parse("https://supabase.com/dashboard"))
                                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(open)
                            } catch (e: Exception) {
                                // no app can open the link: ignore it
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Slim bar above a module that is still hosted from the website: back to the grid, gold chip, title. */
@Composable
internal fun AdminSectionBar(
    module: AdminModule,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Admin Console",
                tint = WisdomTextPrimary
            )
        }
        AdminIconChip(icon = module.icon, size = 32)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = module.label,
            color = WisdomTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminIconChip(icon: ImageVector, size: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(WisdomAccentAmber.copy(alpha = 0.12f))
            .border(1.dp, WisdomAccentAmber.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WisdomAccentAmber,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

@Composable
private fun AdminAttentionBadge(count: Int, tone: Color) {
    val shape = RoundedCornerShape(50)
    Text(
        text = count.toString(),
        color = tone,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.15f))
            .border(1.dp, tone.copy(alpha = 0.4f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun AdminStatTile(label: String, value: Int?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value?.toString() ?: "-",
            color = WisdomTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = WisdomMuted,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdminModuleTile(
    module: AdminModule,
    badge: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AdminIconChip(icon = module.icon, size = 40)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = module.label,
                color = WisdomTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badge != null) {
            val tone = when (module.countKind) {
                AdminCountKind.PAYMENTS -> WisdomAccentAmber
                AdminCountKind.INQUIRIES -> WisdomCyan
                AdminCountKind.EXAM_FLAGS -> AdminRose
                AdminCountKind.DEVICE_ATTEMPTS -> WisdomAccentAmber
                else -> WisdomInk
            }
            val badgeShape = RoundedCornerShape(50)
            Text(
                text = badge.toString(),
                color = tone,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(badgeShape)
                    .background(tone.copy(alpha = 0.15f))
                    .border(1.dp, tone.copy(alpha = 0.4f), badgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun AdminPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WisdomCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = WisdomMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdminSearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (e: Exception) {
            // the field is not attached yet: the user can tap it
        }
    }
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = WisdomTextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(WisdomCyan),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
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
                            text = "Filter admin sections...",
                            color = WisdomMuted,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            }
        }
    )
}
