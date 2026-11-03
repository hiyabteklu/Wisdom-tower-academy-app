package com.wisdomtower.academy.ui.study

import android.content.Context
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.competition.CompetitionRepository
import com.wisdomtower.academy.data.competition.CompetitionResult
import com.wisdomtower.academy.data.competition.MyPoints
import com.wisdomtower.academy.data.competition.PointItem
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Day / night colours, same rule as NativeStatusScreen (night = old literal, day = Ivory & Navy token).
private fun cpPick(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val CpAmber: Color get() = cpPick(Color(0xFFFBBF24), WisdomAccentAmber)
private val CpMuted: Color get() = cpPick(Color(0xFF94A3B8), WisdomMuted)
private val CpPanelFill: Color get() = cpPick(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val CpPanelBorder: Color get() = cpPick(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val CpTrack: Color get() = cpPick(Color.White.copy(alpha = 0.10f), WisdomInk.copy(alpha = 0.10f))

/** The saved copy of the last answer, per student, so the panel is full on the first frame of the next visit. */
private object StatusPointsCache {
    private const val PREFS = "wt_status_points_cache"

    fun load(context: Context, userId: String): Pair<MyPoints, Long>? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("u_$userId", null) ?: return null
        return try {
            val o = JSONObject(raw)
            val arr = o.optJSONArray("items") ?: JSONArray()
            val items = ArrayList<PointItem>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val created: Long? = if (p.has("m")) p.optLong("m") else null
                items.add(PointItem(p.optString("c", ""), p.optString("t", ""), p.optInt("p", 0), created))
            }
            Pair(MyPoints(o.optInt("total", 0), items), o.optLong("saved", 0L))
        } catch (e: Exception) {
            null
        }
    }

    fun save(context: Context, userId: String, points: MyPoints) {
        try {
            val arr = JSONArray()
            for (it in points.items) {
                val p = JSONObject().put("c", it.competitionId).put("t", it.title).put("p", it.points)
                val m = it.createdAtMs
                if (m != null) p.put("m", m)
                arr.put(p)
            }
            val o = JSONObject().put("total", points.total).put("items", arr).put("saved", System.currentTimeMillis())
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("u_$userId", o.toString()).apply()
        } catch (e: Exception) {
            // A failed copy only means the next visit starts from the skeleton again.
        }
    }
}

/** "23 Oct 2026, 14:05" in the phone's own time. */
private fun cpFormatSavedAt(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(ms))

/**
 * Competition points as a panel of "Your status" (owner order 2026-10-24). It replaces the card that used to float at the
 * top of Profile: that card loaded after the rest of the screen and pushed everything down when it arrived.
 *
 * Same look as the other status panels. The panel shell is always drawn, so nothing moves when the data arrives:
 *  - first visit: the title, a dash for the total and three grey lines that keep the room for the entries;
 *  - later visits: the saved copy of the last answer is drawn on the first frame, then replaced by the fresh answer;
 *  - server cannot be reached: the saved copy with one amber line saying when it was saved, or, when nothing is saved,
 *    one line and a Retry.
 * Every number and title comes from the server (`get_my_competition_points`, [CompetitionRepository.myPoints]); nothing is
 * invented. The five newest entries are shown in the server's order. Reads again by itself when the internet comes back.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun StatusCompetitionPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userId = AuthStateManager.currentUser.value?.id
    val cached = remember(userId) { if (userId != null) StatusPointsCache.load(context, userId) else null }
    var points by remember(userId) { mutableStateOf<MyPoints?>(cached?.first) }
    var savedAtMs by remember(userId) { mutableStateOf<Long?>(cached?.second) }
    var fromCache by remember(userId) { mutableStateOf(cached != null) }
    var loading by remember(userId) { mutableStateOf(cached == null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    // OFFLINE STATUS (O5a): goes up when the internet comes back, which reads the points again
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(userId, reloadKey, netBack) {
        failed = false
        val r = CompetitionRepository.myPoints()
        if (r is CompetitionResult.Ok) {
            points = r.value
            fromCache = false
            savedAtMs = null
            if (userId != null) StatusPointsCache.save(context, userId, r.value)
        } else {
            failed = true
        }
        loading = false
    }

    val p = points
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CpPanelFill)
            .border(BorderStroke(1.dp, CpAmber.copy(alpha = 0.25f)), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = if (p != null) "Competition points, " + p.total.toString() else "Competition points, not loaded"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPETITION POINTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CpAmber,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (p != null) p.total.toString() else "\u2014",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CpAmber
            )
        }

        val savedAt = savedAtMs
        if (failed && p != null && fromCache && savedAt != null && savedAt > 0L) {
            Text(
                "No connection to the server. Showing your points as last saved on ${cpFormatSavedAt(savedAt)}.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = CpAmber,
                lineHeight = 16.sp
            )
        }

        if (p == null && loading) {
            // Grey lines keep the room of the entries, so the panel does not grow when the data arrives.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (fraction in floatArrayOf(0.70f, 0.55f, 0.65f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpTrack)
                    )
                }
            }
        } else if (p == null) {
            Text(
                "Could not reach the server. Connect to the internet once to load your competition points.",
                fontSize = 12.sp,
                color = CpMuted,
                lineHeight = 17.sp
            )
            Text(
                "Retry",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CpAmber,
                modifier = Modifier.wisdomTouchFeel()
                    .clickable {
                        loading = true
                        reloadKey += 1
                    }
                    .padding(vertical = 8.dp)
            )
        } else if (p.items.isEmpty()) {
            Text("No competition points yet.", fontSize = 12.sp, color = CpMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in p.items.take(5)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = item.title + ", plus " + item.points.toString() + " points"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 13.sp,
                            color = CpMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "+" + item.points.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomInk
                        )
                    }
                }
                if (p.items.size > 5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CpPanelBorder)
                    )
                    Text("Latest 5 of ${p.items.size} entries", fontSize = 11.sp, color = CpMuted)
                }
            }
        }
    }
}
