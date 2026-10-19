package com.wisdomtower.academy.ui.notifications

import com.wisdomtower.academy.ui.theme.WisdomInk
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.model.NativeNotificationItem
import com.wisdomtower.academy.data.model.NotificationCategory
import com.wisdomtower.academy.data.repository.NotificationRepository
import com.wisdomtower.academy.ui.auth.SignInPill
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomDarkOnCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

/**
 * N12: native copy of the website notifications page (website-reference/src/app/notifications/page.tsx).
 * Texts, filters and category labels are the website's. The list is only what /api/notifications returns.
 * Not ported on purpose: the "Orders & Access" filter and order notices (no purchase UI in the app).
 *
 * GLASS (2026-10-13, docs/GLASS_PLAN.md area 4): the notice rows, the guest notice, the loading skeleton and the
 * empty state use the see-through glass look of the Targets screen. An unread notice is a little brighter
 * (white 6%) than a read one (white 2%).
 *
 * NOTIFICATIONS V2 (docs/NOTIFICATIONS_V2_PLAN.md, slice R5): three categories only (announcement,
 * material release, contest). Tap rules: an announcement opens the full-screen reading view in place
 * (NotificationReaderScreen, no navigation); a material release goes to its url; a contest opens the
 * trophy screen (/competitions). Old general/payment rows count as announcements.
 * [openId] lets a push open the reader for one notification (wired in slice R6).
 */
private enum class NoticeFilter(val label: String) {
    All("All"),
    Material("Material Releases"),
    Admin("Announcements"),
    Contest("Contests")
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    openId: String? = null
) {
    val notices by NotificationRepository.notifications.collectAsState()
    val unreadCount by NotificationRepository.unreadCount.collectAsState()
    val isRefreshing by NotificationRepository.isRefreshing.collectAsState()
    val lastError by NotificationRepository.lastError.collectAsState()
    val currentUser by AuthStateManager.currentUser.collectAsState()
    val isLoggedIn by AuthStateManager.isLoggedIn.collectAsState()
    var filter by remember { mutableStateOf(NoticeFilter.All) }
    var openedId by remember(openId) { mutableStateOf(openId) }

    // Refresh every time the screen opens (the website refetches on open as well).
    LaunchedEffect(currentUser?.id) {
        NotificationRepository.refreshNotifications(currentUser?.id, currentUser?.email)
    }

    // Opening the reader marks that notification as read.
    LaunchedEffect(openedId) {
        openedId?.let { NotificationRepository.markRead(it) }
    }

    val sorted = remember(notices) { notices.sortedByDescending { it.createdAt } }
    val shown = remember(sorted, filter) {
        when (filter) {
            NoticeFilter.All -> sorted
            NoticeFilter.Material -> sorted.filter { it.category == NotificationCategory.MATERIAL }
            NoticeFilter.Admin -> sorted.filter { it.category == NotificationCategory.ADMIN }
            NoticeFilter.Contest -> sorted.filter { it.category == NotificationCategory.CONTEST }
        }
    }

    // Reading view: full screen inside this section; back returns to the list.
    val opened = openedId?.let { id -> notices.firstOrNull { it.id == id } }
    if (opened != null) {
        NotificationReaderScreen(
            item = opened,
            onBack = { openedId = null },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top row: back + mark all read
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.clickable { onBack() }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WisdomMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Account", color = WisdomMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (unreadCount > 0) {
                Row(
                    modifier = Modifier
                        .clickable { NotificationRepository.markAllRead() }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = WisdomAccentSky,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Mark all read", color = WisdomAccentSky, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 24.dp
            )
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WisdomAccentSky.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, WisdomAccentSky.copy(alpha = 0.30f)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = WisdomAccentSky,
                            modifier = Modifier.size(24.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .background(Color(0xFFF43F5E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Notifications", color = WisdomTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Material updates, academic announcements, and study alerts.",
                            color = WisdomMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Filter tabs
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoticeFilter.values().forEach { f ->
                        val selected = f == filter
                        val label = if (f == NoticeFilter.All) "All (${sorted.size})" else f.label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) WisdomAccentSky else WisdomGlassFillStrong)
                                .border(
                                    BorderStroke(1.dp, if (selected) WisdomAccentSky else WisdomGlassBorder),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { filter = f }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) WisdomDarkOnCyan else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Guest notice (website texts)
            if (!isLoggedIn) {
                item {
                    WisdomMessageCard( // S6-msgcard-notif
                        title = "Scholar Notifications",
                        message = "Sign in to receive targeted study reminders and see your enrollment updates.",
                        showSignIn = true,
                        signInLabel = "Sign In to Account",
                        onSignIn = onSignIn
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // Real error from the last refresh (not on the website; keeps failures visible)
            if (lastError != null && !isRefreshing) {
                item {
                    WisdomMessageCard( // S6-msgcard
                        message = lastError.orEmpty(),
                        isError = true,
                        retryLabel = "Try again",
                        onRetry = {
                            NotificationRepository.refreshNotifications(currentUser?.id, currentUser?.email)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Loading skeleton (only while nothing is loaded yet)
            if (isRefreshing && notices.isEmpty()) {
                items(listOf(1, 2, 3, 4)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WisdomGlassFillQuiet)
                            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.33f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(WisdomInk.copy(alpha = 0.10f))
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.66f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(WisdomInk.copy(alpha = 0.05f))
                        )
                    }
                }
            }

            // Empty state (website texts)
            if (!isRefreshing && shown.isEmpty()) {
                item {
                    WisdomMessageCard( // S6-msgcard-notif
                        title = "No notifications",
                        message = if (filter == NoticeFilter.All) {
                            "You're all caught up! New content alerts will appear here."
                        } else {
                            val name = when (filter) {
                                NoticeFilter.Material -> "material"
                                NoticeFilter.Contest -> "contest"
                                else -> "admin"
                            }
                            "No alerts found for category \"$name\"."
                        }
                    )
                }
            }

            // Notice cards
            items(shown, key = { it.id }) { n ->
                NoticeRow(n, onClick = {
                    NotificationRepository.markRead(n.id)
                    when (n.category) {
                        // Announcement: read it right here, no navigation.
                        NotificationCategory.ADMIN -> openedId = n.id
                        // Contest: always the trophy screen.
                        NotificationCategory.CONTEST -> onOpenUrl("/competitions")
                        // Material release: the location the admin picked.
                        NotificationCategory.MATERIAL -> onOpenUrl(n.url)
                    }
                })
            }
        }
    }
}

@Composable
private fun NoticeRow(n: NativeNotificationItem, onClick: () -> Unit) {
    val category = n.category
    val icon: ImageVector = when (category) {
        NotificationCategory.MATERIAL -> Icons.AutoMirrored.Filled.MenuBook
        NotificationCategory.ADMIN -> Icons.Filled.Info
        NotificationCategory.CONTEST -> Icons.Filled.EmojiEvents
    }
    val accent: Color = categoryAccent(category)
    val label: String = categoryLabel(category)
    val dateText = n.createdAt.take(10)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (n.read) WisdomGlassFillQuiet else WisdomGlassFillStrong)
            .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.30f)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (dateText.isNotBlank()) {
                    Text("  •  $dateText", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                if (!n.read) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(6.dp).background(WisdomCyan, CircleShape))
                }
            }
            Text(
                text = n.title,
                color = if (n.read) Color(0xFFE2E8F0) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (n.body.isNotBlank()) {
                Text(
                    text = n.body,
                    color = WisdomMuted,
                    fontSize = 12.sp,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp).align(Alignment.CenterVertically)
        )
    }
}
