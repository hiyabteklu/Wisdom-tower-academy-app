package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.FeedItem
import com.wisdomtower.academy.data.admin.NamedCount
import com.wisdomtower.academy.data.admin.OrderStats
import com.wisdomtower.academy.data.admin.OverviewRepository
import com.wisdomtower.academy.data.admin.OverviewResult
import com.wisdomtower.academy.data.admin.OverviewStats
import com.wisdomtower.academy.data.admin.ProfileStats
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

// ADMIN-native N5 (docs/ADMIN_NATIVE_PLAN.md). Native version of the website AnalyticsPanel (Executive Intelligence and
// Operational Overview): Today's Pulse, 4 KPI tiles, 4 small tiles, 7-day order bars, payment method share, top
// packages, scholar stream share and the activity feed with filters. Texts copied from the website where it has them.
// Charts are plain boxes (no chart library, no Canvas). No animations, one LazyColumn, no horizontal scrolling.
// Differences on purpose: see OverviewRepository.kt; also the website text 100% Ethiopian Rail is NOT copied (a
// claim the database does not back) and the card hover tooltips have no phone equivalent.
// D7k (docs/DAY_MODE_PLAN.md, 2026-10-19): PurpleTone is now a getter (night A78BFA as before, day the deep purple token;
// the pale lavender was too light as text on ivory), and the empty bar tracks use WisdomInk at 6 percent (night: white 6
// percent as before; day: a faint navy tint, white was invisible on ivory). Night is identical.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private val FEED_FILTERS: List<Pair<String, String>> = listOf(
    "all" to "All",
    "order" to "Orders",
    "enroll" to "Grants",
    "inquiry" to "Inquiries",
    "user" to "Scholars"
)

private val PurpleTone: Color get() = if (AppThemeMode.isDark) Color(0xFFA78BFA) else WisdomAccentPurple

private fun dash(v: Int?): String = v?.toString() ?: "-"

private fun plural(n: Int, word: String): String = if (n == 1) "$n $word" else "$n ${word}s"

@Composable
internal fun OverviewScreen(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<OverviewResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var feedFilter by rememberSaveable { mutableStateOf("all") }

    LaunchedEffect(reloadKey) {
        loading = true
        result = OverviewRepository.load()
        loading = false
    }

    val current = result

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Executive Intelligence & Operational Overview",
                        color = WisdomTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Financial telemetry, student enrollment, and platform usage metrics",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { reloadKey += 1 }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = WisdomMuted,
                        modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                    )
                }
            }
        }

        if (current == null) {
            item(key = "loading") {
                Text(
                    text = "Loading executive metrics...",
                    color = WisdomMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                )
            }
        } else if (current is OverviewResult.Failure) {
            item(key = "error") {
                AdminMessageCard(
                    title = "Could not load the overview",
                    body = current.message,
                    actionLabel = "Try again",
                    onAction = { reloadKey += 1 }
                )
            }
        } else if (current is OverviewResult.Success) {
            val s = current.stats
            val feedShown: List<FeedItem> =
                (if (feedFilter == "all") s.feed else s.feed.filter { it.kind == feedFilter }).take(16)

            item(key = "pulse") { PulseCard(orders = s.orders) }
            item(key = "kpis") { KpiGrid(stats = s) }
            if (s.orders != null && s.orders.truncated) {
                item(key = "capNote") {
                    Text(
                        text = "Order figures cover the newest ${s.orders.total} orders only.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }
            item(key = "secondary") { SecondaryGrid(stats = s) }
            item(key = "velocity") { VelocityCard(orders = s.orders) }
            item(key = "methods") { MethodCard(orders = s.orders) }
            item(key = "packages") { PackagesCard(orders = s.orders) }
            item(key = "streams") { StreamsCard(profiles = s.profiles, scholars = s.scholars) }

            item(key = "feedHead") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Real-Time Operational Audit Feed",
                        color = WisdomTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student signups, order payments, access grants, and inquiries",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (entry in FEED_FILTERS) {
                            AdminChoiceChip(
                                label = entry.second,
                                selected = feedFilter == entry.first,
                                onClick = { feedFilter = entry.first },
                                modifier = Modifier.weight(1f),
                                tone = WisdomCyan
                            )
                        }
                    }
                }
            }
            if (feedShown.isEmpty()) {
                item(key = "feedEmpty") {
                    AdminMessageCard(
                        title = "No events found",
                        body = "No events found in this filter category",
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }
            items(feedShown, key = { it.id }) { entry -> FeedLine(item = entry) }
        }
    }
}

@Composable
private fun PulseCard(orders: OrderStats?) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCyan.copy(alpha = 0.08f))
            .border(1.dp, WisdomCyan.copy(alpha = 0.25f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Today's Pulse",
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (orders == null) {
            Text(text = "Orders could not be loaded", color = WisdomMuted, fontSize = 14.sp)
        } else {
            Text(
                text = "${plural(orders.todayCount, "order")} placed today \u00B7 " +
                    "${OverviewRepository.etb(orders.todayRevenue)} verified today",
                color = WisdomCyan,
                fontSize = 14.sp
            )
            if (orders.urgentPending > 0) {
                Text(
                    text = "${plural(orders.urgentPending, "order")} awaiting approval > 2 hrs",
                    color = WisdomAccentAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, hint: String, tone: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.25f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = tone,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = WisdomTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = hint,
            color = WisdomMuted,
            fontSize = 12.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KpiGrid(stats: OverviewStats) {
    val o = stats.orders
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "Verified Revenue",
                value = if (o != null) OverviewRepository.etb(o.revenueVerified) else "-",
                hint = if (o != null) "${o.verified} approved orders \u00B7 avg ${OverviewRepository.etb(o.avgOrder)}" else "Orders could not be loaded",
                tone = WisdomAccentEmerald,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Pending Pipeline",
                value = if (o != null) OverviewRepository.etb(o.pipeline) else "-",
                hint = if (o != null) "${o.pending} pending payment confirmation" else "Orders could not be loaded",
                tone = WisdomAccentAmber,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                label = "Registered Scholars",
                value = dash(stats.scholars),
                hint = if (stats.enrollments != null) "${stats.enrollments} total unlocked package grants" else "Grants could not be counted",
                tone = PurpleTone,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Order Conversion",
                value = if (o != null) "${OverviewRepository.plainNumber(o.conversionPct)}%" else "-",
                hint = if (o != null) "${o.total} checkouts initiated" else "Orders could not be loaded",
                tone = WisdomCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmallTile(label: String, value: String, caption: String, valueTone: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = WisdomMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueTone,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = caption,
            color = WisdomMuted,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecondaryGrid(stats: OverviewStats) {
    val newCount = stats.inquiriesNew
    val inquiryValue = if (newCount != null && newCount > 0) "${dash(stats.inquiriesTotal)} ($newCount new)" else dash(stats.inquiriesTotal)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallTile(
                label = "Course Enrollments",
                value = dash(stats.enrollments),
                caption = "Active scholar access seats",
                valueTone = WisdomTextPrimary,
                modifier = Modifier.weight(1f)
            )
            SmallTile(
                label = "AI Explanations",
                value = dash(stats.explanations),
                caption = "Cached tutor explanations",
                valueTone = WisdomCyan,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallTile(
                label = "Quiz Submissions",
                value = dash(stats.quizzes),
                caption = "Completed exam sessions",
                valueTone = WisdomAccentEmerald,
                modifier = Modifier.weight(1f)
            )
            SmallTile(
                label = "Inquiries & Support",
                value = inquiryValue,
                caption = "Student support messages",
                valueTone = WisdomTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, trailing: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = WisdomTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (trailing.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trailing, color = WisdomMuted, fontSize = 12.sp)
            }
        }
        content()
    }
}

@Composable
private fun CardNote(text: String) {
    Text(
        text = text,
        color = WisdomMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}

@Composable
private fun BarRow(label: String, right: String, fraction: Float, tone: Color) {
    val trackShape = RoundedCornerShape(50)
    val safe = if (fraction < 0f) 0f else if (fraction > 1f) 1f else fraction
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = WisdomTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = right, color = WisdomMuted, fontSize = 12.sp, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(trackShape)
                .background(WisdomInk.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safe)
                    .clip(trackShape)
                    .background(tone)
            )
        }
    }
}

@Composable
private fun VelocityCard(orders: OrderStats?) {
    ChartCard(title = "Daily Order & Revenue Velocity", trailing = "Last 7 Days") {
        if (orders == null) {
            CardNote(text = "Orders could not be loaded")
        } else {
            val peak = maxOf(1, orders.last7.maxOfOrNull { it.orders } ?: 1)
            val weekRevenue = orders.last7.sumOf { it.revenue }
            val trackShape = RoundedCornerShape(8.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (day in orders.last7) {
                        val fraction = if (day.orders == 0) 0.03f else maxOf(0.1f, day.orders.toFloat() / peak.toFloat())
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = day.orders.toString(),
                                color = WisdomTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(trackShape)
                                    .background(WisdomInk.copy(alpha = 0.06f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction)
                                        .clip(trackShape)
                                        .background(WisdomCyan)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = day.label, color = WisdomMuted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Text(
                    text = "Orders placed per day \u00B7 verified revenue in these 7 days: ${OverviewRepository.etb(weekRevenue)}",
                    color = WisdomMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MethodCard(orders: OrderStats?) {
    ChartCard(
        title = "Payment Rail Share",
        trailing = if (orders != null) "${orders.total} transactions" else ""
    ) {
        if (orders == null) {
            CardNote(text = "Orders could not be loaded")
        } else if (orders.byMethod.isEmpty()) {
            CardNote(text = "No orders recorded yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (m in orders.byMethod) {
                    val pct = if (orders.total > 0) Math.round(m.count * 100.0 / orders.total).toInt() else 0
                    BarRow(
                        label = m.name.uppercase(),
                        right = "${OverviewRepository.etb(m.amount)} \u00B7 ${m.count} ($pct%)",
                        fraction = pct / 100f,
                        tone = WisdomAccentEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun PackagesCard(orders: OrderStats?) {
    ChartCard(title = "Top Packages by Enrollment", trailing = "By order volume") {
        if (orders == null) {
            CardNote(text = "Orders could not be loaded")
        } else if (orders.byPackage.isEmpty()) {
            CardNote(text = "No package enrollments yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((index, pkg) in orders.byPackage.withIndex()) {
                    PackageRankRow(rank = index + 1, pkg = pkg)
                }
            }
        }
    }
}

@Composable
private fun PackageRankRow(rank: Int, pkg: NamedCount) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            color = WisdomMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = pkg.name,
            color = WisdomTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = OverviewRepository.etb(pkg.amount),
                color = WisdomAccentAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(text = "${pkg.count} orders", color = WisdomMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StreamsCard(profiles: ProfileStats?, scholars: Int?) {
    ChartCard(
        title = "Scholar Stream Demographics",
        trailing = if (scholars != null) "$scholars registered" else ""
    ) {
        if (profiles == null) {
            CardNote(text = "Scholar profiles could not be loaded")
        } else if (profiles.byStream.isEmpty()) {
            CardNote(text = "No student profiles registered yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (stream in profiles.byStream) {
                    val pct = if (profiles.loaded > 0) Math.round(stream.count * 100.0 / profiles.loaded).toInt() else 0
                    BarRow(
                        label = stream.name,
                        right = "${stream.count} scholars ($pct%)",
                        fraction = pct / 100f,
                        tone = PurpleTone
                    )
                }
                if (profiles.truncated) {
                    Text(
                        text = "Shares are based on the newest ${profiles.loaded} scholars.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun feedTone(kind: String): Color = when (kind) {
    "order" -> WisdomAccentAmber
    "enroll" -> WisdomAccentEmerald
    "inquiry" -> WisdomCyan
    else -> PurpleTone
}

@Composable
private fun FeedLine(item: FeedItem) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(feedTone(item.kind))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text,
                color = WisdomTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.detail != null) {
                Text(
                    text = item.detail,
                    color = WisdomMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = OverviewRepository.timeAgo(item.whenIso), color = WisdomMuted, fontSize = 12.sp, maxLines = 1)
    }
}
