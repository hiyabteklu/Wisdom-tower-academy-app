package com.wisdomtower.academy.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.MySubjectsStore
import com.wisdomtower.academy.data.content.ProgressFetchResult
import com.wisdomtower.academy.data.content.ProgressRow
import com.wisdomtower.academy.data.content.ResourceScopeIndex
import com.wisdomtower.academy.data.content.StudentProgressReader
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// D6h day / night colours (docs/DAY_MODE_PLAN.md, 2026-10-19). Night value = the exact old literal; day value = Ivory & Navy
// token. They are getters (not vals) so they are read while composing and follow the switch.
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val StSky: Color get() = dayNight(Color(0xFF38BDF8), WisdomAccentSky)
private val StSkyText: Color get() = dayNight(Color(0xFF7DD3FC), WisdomAccentSky)
private val StCyan: Color get() = dayNight(Color(0xFF22D3EE), WisdomCyan)
private val StCyanText: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)
private val StEmerald: Color get() = dayNight(Color(0xFF34D399), WisdomAccentEmerald)
private val StAmber: Color get() = dayNight(Color(0xFFFBBF24), WisdomAccentAmber)
private val StRose: Color get() = dayNight(Color(0xFFFB7185), WisdomAccentRose)
private val StMuted: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val StSlate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomInk)
private val StSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomMuted)
private val StBody: Color get() = dayNight(Color(0xFFF1F5F9), WisdomInk)
private val StPanelFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val StPanelBorder: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val StQuietFill: Color get() = dayNight(Color.White.copy(alpha = 0.05f), WisdomGlassFillQuiet)
private val StPillFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFillQuiet)
private val StInnerFill: Color get() = dayNight(Color.White.copy(alpha = 0.03f), WisdomGlassFillQuiet)
private val StInnerBorder: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomGlassBorder)
private val StTrack: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomInk.copy(alpha = 0.10f))
private val StHudFill: Color get() = dayNight(Color(0x660B1329).copy(alpha = 0.40f), WisdomGlassFillStrong)

/** OFFLINE STATUS (O2): shown when the numbers could not be read and nothing is saved on this phone yet (first open). */
private const val CONNECT_NOTICE = "Could not reach the server. Connect to the internet once to load your real status."

/** OFFLINE STATUS (O2): "23 Oct 2026, 14:05" in the phone's own time. */
private fun formatSavedAt(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(Date(ms))

/**
 * Native port of the "Your status" tool (`activeFeature === "analytics"` in `LearningContent.tsx`, component
 * `website-reference/src/components/StudentAnalyticsDashboard.tsx`), F7 part 5. Reads the signed-in student's own
 * `learning_progress` rows ([StudentProgressReader]) and shows: header chips "Your status" and "Standing", the track
 * selector, the study progress summary, the HUD cards, "Retention & Exam Accuracy", "Study Recommendations" and
 * "Study Alerts" with the website's texts. REAL DATA ONLY (owner rule): see [computeStatus] for what is left out and why.
 *
 * Differences from the website (on purpose): (a) no invented fallback numbers, a figure that cannot be measured shows a
 * dash; (b) no Reading Speed card and no "Cognitive Reading Diagnostic" panel (a reading speed is not recorded); the
 * second card is "Focus" with the website's line "Focus Dwell Ratio"; (c) no "Exam Solve Pacing" card and no weekday
 * chart (no timing or per-day data exists); (d) no weekly percent / "On Target" badges (the table only has lifetime seconds);
 * (e) a Close button was added and the cards are one per row on the phone; (f) signed out shows the Learning screen's
 * sign-in sentence.
 *
 * T8 "My Subjects" (2026-10-14, docs/T8_MY_SUBJECTS_PLAN.md): the rows are filtered BEFORE [computeStatus] so only the
 * subjects the student picked in Settings count (picks live on this phone, [MySubjectsStore]; the resource-to-subject map
 * is [ResourceScopeIndex]). [computeStatus] and what is saved to `learning_progress` are unchanged. With no picks at all
 * (Academic Level without a package, or not set) everything is counted and one line says so. If the subject map cannot be
 * loaded (offline, no cache) the screen says so and shows NO totals.
 *
 * D6h (2026-10-19): day mode. The `St...` colours above are now `dayNight` getters (night keeps the exact old literals).
 * By day: panels, track selector, pills and inner cards are the ivory glass; titles and body text are ink, small texts
 * muted slate; the accents (sky, cyan, emerald, amber, rose) are the deep day accents (they also colour the chips, HUD
 * numbers and borders); the progress track is a faint navy line; the HUD cards are the strong glass. The drop-down list of
 * tracks is the Material menu and follows the Material scheme from D1. Layout, texts, data and rules are unchanged.
 *
 * OFFLINE STATUS (2026-10-23, docs/OFFLINE_STATUS_PLAN.md slice O2): the screen never replaces its layout with an error
 * card. (1) Read failed, a saved copy of the same student exists ([StudentProgressReader] returns `fromCache = true`):
 * everything is drawn from the copy and one amber line says when it was saved. (2) Read failed, nothing saved (first open
 * offline): a notice with Retry, then the EMPTY skeleton: the four HUD cards with dashes, the track selector and the fixed
 * recommendation texts. [computeStatus] is NOT run on an empty list for the skeleton (it would print 0 hours, a 0-day
 * streak and a "Zero Study Sessions" alert, which is a claim, not a fact), so no Standing, no Retention panel and no
 * alerts. Slice O3 (2026-10-23): the empty skeleton also shows the "By subject" panel (`progressKnown = false`: empty rows
 * with dashes) and the "Today & this week" panel, each with its own offline states. Slice O5a (2026-10-23): when the
 * internet comes back ([rememberNetworkReturnCount] goes up) the screen reads the numbers again by itself. Online behaviour
 * is unchanged.
 *
 * STATUS CARD S2 (2026-10-23, docs/STATUS_CARD_PLAN.md): one new line in the loaded branch, [StatusWeekBlock] ("Your week"
 * plain sentences), placed right after the HUD cards. Nothing else changed in this file.
 *
 * COMPETITION POINTS (2026-10-24, owner order): [StatusCompetitionPanel] is now a panel of this screen (it used to be a card
 * floating at the top of Profile that loaded late and pushed the page down). It sits after "Today & this week" and before
 * "Retention & Exam Accuracy", in the loaded screen and in the offline skeleton. It loads its own data and keeps a saved
 * copy, so it does not move the layout when the answer arrives. Nothing else changed in this file.
 *
 * SAVED COPY FIRST (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md slice P1c, part c2b): the screen first draws the
 * saved progress copy ([StudentProgressReader.fetchAllSaved], no network) through the same T8 filter as a live read, with
 * NO "No connection" line (the server has not been asked yet), then reads the server in the background and replaces the
 * numbers (nothing flickers when they are equal). The spinner only shows when nothing is saved; a failed refresh keeps the
 * saved numbers; the amber saved-copy line only appears when the normal read really falls back to the copy. The filter
 * step is the local function `applyRows` inside the effect (same code as before, now shared by both draws).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun NativeStatusScreen(
    onBack: () -> Unit,
    isLoggedIn: Boolean,
    studentName: String?,
    educationLevel: String?,
    stream: String?,
    dailyGoalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var reloadKey by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(isLoggedIn) }
    var failed by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var countedLine by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<ProgressRow>?>(null) }
    // OFFLINE STATUS (O2): true when the rows are the saved copy of the latest sync, and when it was saved
    var fromCache by remember { mutableStateOf(false) }
    var savedAtMs by remember { mutableStateOf<Long?>(null) }
    // OFFLINE STATUS (O5a): goes up when the internet comes back, which reads the numbers again
    val netBack = rememberNetworkReturnCount()

    LaunchedEffect(isLoggedIn, reloadKey, netBack) {
        if (!isLoggedIn) {
            loading = false
            return@LaunchedEffect
        }

        /**
         * P1c c2b: the T8 "My Subjects" filter and the state the screen draws from, shared by the saved draw and the live
         * draw (it is the body the old `Success` branch had). Returns false, changing nothing, when the subject list is
         * needed but cannot be loaded; the caller decides what that means.
         */
        suspend fun applyRows(result: ProgressFetchResult.Success): Boolean {
            val userId = AuthStateManager.currentUser.value?.id
            val picks = if (userId != null) MySubjectsStore.load(context, userId, educationLevel) else null
            if (picks == null || picks.keys.isEmpty()) {
                // Nothing picked and no package for the Academic Level: count everything, say so.
                fromCache = result.fromCache
                savedAtMs = result.savedAtMs
                rows = result.rows
                countedLine = "Counting all your study activity. Choose your subjects in Settings > My Subjects."
                return true
            }
            val first = ResourceScopeIndex.get(context)
            // A record of a resource the cached map does not know (new content): refresh the map once.
            val index = if (first != null && result.rows.any { it.resourceId !in first.map }) {
                ResourceScopeIndex.get(context, forceRefresh = true) ?: first
            } else {
                first
            }
            if (index == null) return false
            val counted = MySubjectsStore.filterRows(result.rows, index.map, picks.keys)
            fromCache = result.fromCache
            savedAtMs = result.savedAtMs
            rows = counted
            countedLine = buildCountedLine(picks.keys, result.rows.size - counted.size)
            return true
        }

        // P1c c2b: the saved copy first (no network), drawn at once and WITHOUT the "No connection" line
        var drewSaved = false
        val saved = StudentProgressReader.fetchAllSaved()
        if (saved != null && applyRows(saved)) {
            fromCache = false
            savedAtMs = null
            failed = false
            errorText = null
            loading = false
            drewSaved = true
        }
        if (!drewSaved) {
            loading = true
            failed = false
            errorText = null
            fromCache = false
            savedAtMs = null
        }

        when (val result = StudentProgressReader.fetchAll()) {
            is ProgressFetchResult.Success -> {
                if (!applyRows(result) && !drewSaved) {
                    rows = null
                    failed = true
                    errorText = "Could not load your subject list. Check your internet connection and try again."
                }
            }
            else -> {
                // Nothing could be read and nothing is saved: the screen draws the empty skeleton (see below).
                // A failed refresh keeps the saved numbers that are already drawn.
                if (!drewSaved) {
                    rows = null
                    failed = true
                }
            }
        }
        loading = false
    }

    val defaultTrack = remember(educationLevel, stream) { resolveStatusTrack(educationLevel, stream) }
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var trackMenuOpen by remember { mutableStateOf(false) }
    val track = STATUS_TRACKS.firstOrNull { it.id == selectedTrackId } ?: defaultTrack
    val summary = remember(rows, track) { rows?.let { computeStatus(it, track) } }
    val name = studentName?.takeIf { it.isNotBlank() } ?: "Scholar"

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Shared top toolbar: back arrow + title (48dp touch target, top-left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScreenBackButton(onBack)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(StSky.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, StSky.copy(alpha = 0.30f)), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = StSky, modifier = Modifier.size(16.dp))
                    Text("YOUR STATUS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = StSkyText, letterSpacing = 0.8.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header: Standing, track selector, summary
                StPanel(borderColor = StSky.copy(alpha = 0.20f)) {

                    if (summary != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(StPillFill)
                                .border(BorderStroke(1.dp, StPanelBorder), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Standing: ", fontSize = 12.sp, color = StSlate300)
                            Text(summary.tier ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StCyanText)
                        }
                    }

                    // Academic track benchmark selector
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(StQuietFill)
                                .border(BorderStroke(1.dp, StPanelBorder), RoundedCornerShape(14.dp))
                                .clickable { trackMenuOpen = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (track.id == defaultTrack.id) "${track.name} (Enrolled)" else track.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = WisdomInk,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = StMuted, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = trackMenuOpen, onDismissRequest = { trackMenuOpen = false }) {
                            STATUS_TRACKS.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(if (t.id == defaultTrack.id) "${t.name} (Enrolled)" else t.name, fontSize = 13.sp)
                                            Text("${t.category} · ${formatStatusNumber(t.weeklyTargetHours)}h / week target", fontSize = 11.sp, color = StMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedTrackId = t.id
                                        trackMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    if (summary != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(StSky.copy(alpha = 0.06f))
                                .border(BorderStroke(1.dp, StSky.copy(alpha = 0.20f)), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("STUDY PROGRESS SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StSkyText, letterSpacing = 0.8.sp)
                            Text(
                                "Your cumulative study time is ${formatStatusNumber(summary.totalStudyHours)} hours (${summary.totalStudyMinutes} minutes). Your active streak is ${summary.streakDays} days.",
                                fontSize = 13.sp,
                                color = StBody,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    // OFFLINE STATUS (O2): the numbers below are the saved copy of the latest sync, say when it was saved
                    val savedAt = savedAtMs
                    if (fromCache && savedAt != null && isLoggedIn && !loading && !failed) {
                        Text(
                            "No connection to the server. Showing your status as last saved on ${formatSavedAt(savedAt)}. Connect to the internet to update.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StAmber,
                            lineHeight = 16.sp
                        )
                    }

                    // T8: what is counted (picked subjects), so the totals are never a surprise
                    val counted = countedLine
                    if (counted != null && isLoggedIn && !loading && !failed) {
                        Text(counted, fontSize = 11.sp, color = StMuted, lineHeight = 16.sp)
                    }
                }

                // States: signed out, loading, failed (empty skeleton), loaded
                if (!isLoggedIn) {
                    WisdomMessageCard( // S6-msgcard-status
                        message = "Sign in or create an account to sync your study notes, targets, and course progress.",
                        showSignIn = true
                    )
                } else if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StCyan, modifier = Modifier.size(28.dp))
                    }
                } else if (failed || summary == null) {
                    // OFFLINE STATUS (O2): a notice with Retry, then the EMPTY skeleton (dashes, no invented zeros)
                    WisdomMessageCard( // S6-msgcard-status
                        message = errorText ?: CONNECT_NOTICE,
                        isError = errorText != null,
                        onRetry = { reloadKey += 1 }
                    )
                    StHudCards(summary = null, track = track, dailyGoalMinutes = dailyGoalMinutes)
                    // OFFLINE STATUS (O3): subject rows (empty, names from the packed list) and the day panel (its own saved copy)
                    StatusSubjectsPanel(rows = emptyList(), educationLevel = educationLevel, progressKnown = false)
                    StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes)
                    // Competition points (own saved copy, own offline states)
                    StatusCompetitionPanel()
                    val fixedRecommendations = remember(track) { computeStatus(emptyList(), track).recommendations }
                    StRecommendationsPanel(recommendations = fixedRecommendations, name = name)
                } else {
                    // 2. HUD cards
                    StHudCards(summary = summary, track = track, dailyGoalMinutes = dailyGoalMinutes)
                    val accuracy = summary.accuracyPct
                    val accuracyAccent = when {
                        accuracy == null -> StMuted
                        accuracy >= 75.0 -> StEmerald
                        accuracy >= 50.0 -> StAmber
                        else -> StRose
                    }

                    // STATUS CARD (S2): plain "Your week" sentences (own file, reads StatusCardData)
                    StatusWeekBlock(rows = rows.orEmpty(), summary = summary, track = track, studentName = name, educationLevel = educationLevel)

                    // My Status V2 (S3): per-subject rings + detail card
                    StatusSubjectsPanel(rows = rows.orEmpty(), educationLevel = educationLevel)

                    // My Status V2 (S6d): today ring + 7-day bars from study_day_log
                    StatusDayPanel(dailyGoalMinutes = dailyGoalMinutes) // MYSTATUS-S6

                    // Competition points: real points and exam entries from the database (own saved copy, no pop-in)
                    StatusCompetitionPanel()

                    // 3. Retention & exam accuracy
                    StPanel(borderColor = StPanelBorder) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StChip("Retention & Exam Accuracy", StEmerald)
                            Text("Derived from drills, chapter checks, and question sets", fontSize = 11.sp, color = StMuted)
                            Text("Active Recall & Question Precision", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                        }
                        val rating = summary.retentionRating
                        if (rating != null) {
                            StChip(
                                rating,
                                when (rating) {
                                    "High Long-Term Retention" -> StEmerald
                                    "Memory Decay Risk" -> StRose
                                    else -> StAmber
                                }
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(StInnerFill)
                                .border(BorderStroke(1.dp, StInnerBorder), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ACCURACY & SOLVED VOLUME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomInk, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
                                Text(
                                    if (accuracy != null) "${formatStatusNumber(accuracy)}%" else "—",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StCyanText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(StTrack)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(((accuracy ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(accuracyAccent)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Correct: ${summary.questionsCorrect}", fontSize = 12.sp, color = StSlate300)
                                Text("Total: ${summary.questionsAttempted}", fontSize = 12.sp, color = StSlate300)
                            }
                            val explanation = summary.retentionExplanation
                            if (explanation != null) {
                                Text(explanation, fontSize = 12.sp, color = StSlate300, lineHeight = 17.sp)
                            }
                        }
                    }

                    // 4. Recommendations
                    StRecommendationsPanel(recommendations = summary.recommendations, name = name)

                    // 5. Study alerts
                    StPanel(borderColor = StRose.copy(alpha = 0.20f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = StRose, modifier = Modifier.size(14.dp))
                                StChip("Study Alerts", StRose)
                            }
                            Text("Based on your recent study patterns", fontSize = 11.sp, color = StMuted)
                            Text("Habits & Pacing to Adjust", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                            StChip("${summary.alerts.size} Alerts", StRose)
                        }
                        summary.alerts.forEach { sig ->
                            StInfoCard(
                                accent = StRose,
                                tag = "${sig.severity} Alert",
                                title = sig.signal,
                                bodyLabel = "Observed Data: ",
                                body = sig.observedData,
                                boxLabel = "Do this today:",
                                boxText = sig.immediateAction
                            )
                        }
                    }
                }
            }
        }
    }
}

/** T8: the one line that says what "Your status" is counting (real numbers only). */
private fun buildCountedLine(pickedKeys: Set<String>, excludedRecords: Int): String {
    val n = pickedKeys.size
    val labels = pickedKeys
        .map { MySubjectsStore.packageOf(it) }
        .distinct()
        .mapNotNull { id -> MySubjectsStore.packages.firstOrNull { it.id == id }?.label }
        .joinToString(", ")
    val subjects = if (n == 1) "1 subject" else "$n subjects"
    val base = if (labels.isNotEmpty()) "Counting $subjects: $labels." else "Counting $subjects."
    val other = if (excludedRecords > 0) " Other activity ($excludedRecords records) is not counted." else ""
    return "$base$other Change it in Settings > My Subjects."
}

/**
 * The four HUD cards (Study Time, Focus, Accuracy, Active Streak). With a [summary] they show the real numbers exactly as
 * before; with `summary = null` (OFFLINE STATUS O2, first open offline) every figure is a dash, the lines that need no
 * data (weekly target of the track, daily goal of the student) stay.
 */
@Composable
private fun StHudCards(summary: StatusSummary?, track: StatusTrack, dailyGoalMinutes: Int) {
    val streakAccent = when {
        summary == null -> StMuted
        summary.streakDays >= 7 -> StEmerald
        summary.streakDays >= 3 -> StAmber
        else -> StRose
    }
    val accuracy = summary?.accuracyPct
    val accuracyAccent = when {
        accuracy == null -> StMuted
        accuracy >= 75.0 -> StEmerald
        accuracy >= 50.0 -> StAmber
        else -> StRose
    }
    val accuracyBadge = when {
        accuracy == null -> null
        accuracy >= 75.0 -> "Mastery Level"
        accuracy >= 50.0 -> "Developing"
        else -> "Needs Review"
    }
    val focus = summary?.focusRatioPct
    val focusAccent = when {
        focus == null -> StMuted
        focus >= 75 -> StEmerald
        focus >= 60 -> StAmber
        else -> StRose
    }

    StHudCard(
        icon = Icons.Filled.Schedule,
        iconTint = StSky,
        title = "Study Time",
        badge = null,
        accent = StSky,
        big = if (summary != null) formatStatusNumber(summary.totalStudyHours) else "—",
        unit = if (summary != null) "hrs" else null,
        line = "Target: ${formatStatusNumber(track.weeklyTargetHours)} hrs/wk",
        footer = null
    )
    StHudCard(
        icon = Icons.Filled.Speed,
        iconTint = StCyan,
        title = "Focus",
        badge = null,
        accent = focusAccent,
        big = if (focus != null) "$focus%" else "—",
        unit = null,
        line = "Time you stayed focused",
        footer = null
    )
    StHudCard(
        icon = Icons.Filled.TrackChanges,
        iconTint = StEmerald,
        title = "Accuracy",
        badge = accuracyBadge,
        accent = accuracyAccent,
        big = if (accuracy != null) "${formatStatusNumber(accuracy)}%" else "—",
        unit = null,
        line = if (summary != null) "Solved: ${summary.questionsCorrect} of ${summary.questionsAttempted} drills" else "Solved: — of — drills",
        footer = if (summary?.retentionIndexPct != null) "Retention Index: ${summary.retentionIndexPct}%" else null
    )
    StHudCard(
        icon = Icons.Filled.LocalFireDepartment,
        iconTint = StAmber,
        title = "Active Streak",
        badge = if (summary != null && summary.streakDays > 0) (if (summary.streakDays >= 7) "Unbroken" else "Active") else null,
        accent = streakAccent,
        big = if (summary != null) "${summary.streakDays}" else "—",
        unit = if (summary != null) "days" else null,
        line = "Daily active learning recorded",
        footer = "Daily goal: $dailyGoalMinutes mins / day."
    )
}

/** The "Study Recommendations" panel (fixed texts of the website). Shared by the loaded screen and the empty skeleton. */
@Composable
private fun StRecommendationsPanel(recommendations: List<StatusRecommendation>, name: String) {
    StPanel(borderColor = StCyan.copy(alpha = 0.20f)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StChip("Study Recommendations", StCyan)
            Text("Personalized study guidance for $name", fontSize = 11.sp, color = StSlate300)
            Text("Recommended Study Routine Adjustments", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
            StChip("${recommendations.size} Recommendations", StCyan)
        }
        recommendations.forEach { rec ->
            StInfoCard(
                accent = StCyan,
                tag = rec.category,
                title = rec.title,
                bodyLabel = null,
                body = rec.description,
                boxLabel = "Next step:",
                boxText = rec.actionableStep
            )
        }
    }
}

@Composable
private fun StPanel(borderColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StPanelFill)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun StChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 2)
    }
}

@Composable
private fun StHudCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String?,
    accent: Color,
    big: String,
    unit: String?,
    line: String,
    footer: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StHudFill)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.30f)), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, iconTint.copy(alpha = 0.30f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                }
                Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StSlate300, letterSpacing = 0.8.sp)
            }
            if (badge != null) StChip(badge, accent)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(big, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            if (unit != null) {
                Text(" $unit", fontSize = 13.sp, color = StMuted, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Text(line, fontSize = 11.sp, color = StSlate300)
        if (footer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StInnerBorder)
            )
            Text(footer, fontSize = 11.sp, color = StMuted, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun StInfoCard(
    accent: Color,
    tag: String,
    title: String,
    bodyLabel: String?,
    body: String,
    boxLabel: String,
    boxText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StInnerFill)
            .border(BorderStroke(1.dp, StInnerBorder), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StChip(tag.uppercase(), accent)
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk, lineHeight = 19.sp)
        val bodyText = buildAnnotatedString {
            if (bodyLabel != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WisdomInk)) { append(bodyLabel) }
            }
            append(body)
        }
        Text(bodyText, fontSize = 12.sp, color = StSlate300, lineHeight = 17.sp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(boxLabel.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 0.6.sp)
            Text(boxText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = StSlate200, lineHeight = 17.sp)
        }
    }
}
