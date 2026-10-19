package com.wisdomtower.academy.ui.competition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.competition.CompetitionAction
import com.wisdomtower.academy.data.competition.CompetitionDisplay
import com.wisdomtower.academy.data.competition.CompetitionError
import com.wisdomtower.academy.data.competition.CompetitionPhase
import com.wisdomtower.academy.data.competition.CompetitionPhaseLogic
import com.wisdomtower.academy.data.competition.CompetitionRepository
import com.wisdomtower.academy.data.competition.CompetitionResult
import com.wisdomtower.academy.data.competition.CompetitionSession
import com.wisdomtower.academy.data.competition.CompetitionSummary
import com.wisdomtower.academy.data.competition.CountdownKind
import com.wisdomtower.academy.data.competition.SubmitResult
import com.wisdomtower.academy.ui.study.ScreenBackButton
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import com.wisdomtower.academy.ui.theme.wisdomReveal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// D7f (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. The private colours were top-level vals (read once at class load, so
// they would not follow the switch); they are getters now. Night keeps the old values exactly (cyan 22E0FF, green 22C55E,
// muted 94A3B8, red F87171, detail line E2E8F0). Day: cyan and muted come from the tokens, green 15803D and red B91C1C
// (the night green and red are too light on ivory), detail line = WisdomTextPrimary. The three white text lines became
// WisdomInk. Layout, texts and logic unchanged.
private val CyanAccent: Color
    get() = WisdomCyan
private val LiveGreen: Color
    get() = if (AppThemeMode.isDark) Color(0xFF22C55E) else Color(0xFF15803D)
private val MutedText: Color
    get() = if (AppThemeMode.isDark) Color(0xFF94A3B8) else WisdomMuted
private val ErrorRed: Color
    get() = if (AppThemeMode.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
private val DetailText: Color
    get() = if (AppThemeMode.isDark) Color(0xFFE2E8F0) else WisdomTextPrimary

// C5 slice 1: the competitions hub opened by the trophy button (list of contests with phase badge,
// live countdown, start and end time in Ethiopia time). Data comes only from the server through
// CompetitionRepository; every text on a card is a server value or a plain label, nothing is invented.
// The list reloads every 30 seconds so an admin pause or start-now reaches an open screen (owner Q7),
// and again when a card's phase moves on with the clock.
// C5 slice 2: a contest the server still marks as unseen gets a small cyan dot in front of its title
// (no text, so nothing is invented). The screen keeps the dot for as long as it stays open and tells the
// server through CompetitionRepository.markSeen, so the dot is gone the next time the hub opens.
// A failed markSeen call is simply tried again on the next 30 second reload.
// C6 slice 3: Join, Resume and Practice buttons. They call CompetitionRepository.start and then show
// CompetitionExamScreen (so MainActivity needs no change). Resume starts at once.
// Practice is offered only when the server says it is available (after the window ends, decision A).
// After the exam a short line says it was submitted; a practice score is shown only as the server sent it.
// Session U (owner, 2026-10-15): Join no longer asks with one line on the card. It opens the full-screen
// rules page (CompetitionRulesScreen) and only its Start button calls start (the ranked attempt cannot be
// restarted, decision C). The exam itself is shown full screen too (CompetitionFullScreen).
// Session W (owner, 2026-10-15, phone test): the Leaderboard and Review screens showed only a flicker of the
// Packages page. Cause (code read): MainActivity mounts this hub as an overlay with a background and a high
// zIndex, but this screen gave that modifier only to the contest LIST. The leaderboard, the review (and the
// exam in the first test) were drawn with a plain fillMaxSize, so they sat behind the Packages screen.
// Now the list is always the base of this screen and the exam, the rules page, the leaderboard and the
// review are each shown through CompetitionFullScreen, a separate window that does not depend on the
// layering of MainActivity.
// C7 slice 2: a Leaderboard button on every ended contest the student may enter (decision G).
// C7 slice 3: a Review button on an ended contest where the student has a ranked attempt
// (correct answers and solutions, math rendered).
// Session P: a live contest this student cannot take part in (not in the audience, package missing, or
// disqualified) shows one plain line saying why, instead of a card with no explanation. The wording is the
// same as the repository's error messages.
// Session Y (open item C of session X): the title of the contest that was started is passed to the exam
// screen, so the score card of the exam window shows it. The title is the server's own value from the card.
// Session AH (owner, 2026-10-15, phone test): the contest card is a glass card (WisdomGlassCard) instead of a
// solid navy box, and the card buttons are CompetitionPillButton (round ends, no bright solid colour, one
// line of text). The Review label no longer wraps into two lines because the buttons share the row evenly.
// Session M9 (docs/UI_MOTION_PLAN.md, motion only): each contest card rises in with a stagger (wisdomReveal,
// once per contest per app session, so the 30 second reload never replays it). Nothing else changed.
// The lockdown flag is raised by CompetitionExamScreen (C6 slice 4), not here.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun CompetitionsHubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var contests by remember { mutableStateOf<List<CompetitionSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableLongStateOf(CompetitionRepository.serverNowMs()) }
    // ids that were unseen when this screen first got them (the dot stays while the screen is open)
    var freshIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // ids the server already confirmed as seen
    val markedIds = remember { mutableSetOf<String>() }
    // C6 slice 3: the running exam (null = none), the contest being started, the rules page
    var session by remember { mutableStateOf<CompetitionSession?>(null) }
    // session Y: the title of the contest whose exam is open (empty when unknown)
    var sessionTitle by remember { mutableStateOf("") }
    var startingId by remember { mutableStateOf<String?>(null) }
    var confirmId by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // C7: the contest whose leaderboard (slice 2) or review (slice 3) is open (null = not open)
    var leaderboardOf by remember { mutableStateOf<CompetitionSummary?>(null) }
    var reviewOf by remember { mutableStateOf<CompetitionSummary?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        if (contests.isEmpty()) loading = true
        when (val r = CompetitionRepository.listCompetitions()) {
            is CompetitionResult.Ok -> {
                contests = r.value.items
                errorText = null
                val unseenIds = r.value.items.filter { it.unseen }.map { it.id }
                if (unseenIds.isNotEmpty()) {
                    freshIds = freshIds + unseenIds
                    for (id in unseenIds) {
                        if (id !in markedIds) {
                            val marked = CompetitionRepository.markSeen(id)
                            if (marked is CompetitionResult.Ok) markedIds.add(id)
                        }
                    }
                }
            }
            is CompetitionResult.Failed -> errorText = r.message
        }
        loading = false
    }

    // clock for the countdown (server-corrected), once per second
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = CompetitionRepository.serverNowMs()
            delay(1000L)
        }
    }

    // poll the server every 30 seconds (not while an exam, a leaderboard or a review is open)
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            if (session == null && leaderboardOf == null && reviewOf == null) reloadKey += 1
        }
    }

    val known = CompetitionRepository.isServerTimeKnown()
    val rows = contests.map { it to CompetitionPhaseLogic.describe(it, nowMs, known) }
    val anyNeedsRefresh = rows.any { it.second.needsRefresh }
    LaunchedEffect(anyNeedsRefresh) {
        if (anyNeedsRefresh) {
            delay(1500L)
            reloadKey += 1
        }
    }

    fun startContest(id: String, ranked: Boolean) {
        if (startingId != null) return
        startingId = id
        actionError = null
        notice = null
        scope.launch {
            val r = CompetitionRepository.start(id, ranked)
            if (r is CompetitionResult.Ok) {
                confirmId = null
                sessionTitle = contests.firstOrNull { it.id == id }?.title ?: ""
                session = r.value
            } else if (r is CompetitionResult.Failed) {
                confirmId = null
                actionError = r.message
                reloadKey += 1
            }
            startingId = null
        }
    }

    val active = session
    val board = leaderboardOf
    val reviewing = reviewOf
    val rulesFor = contests.firstOrNull { it.id == confirmId }

    // the base of this screen: the list of contests (this is the part MainActivity layers on top of the tabs)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScreenBackButton(onClick = onBack)
            Text(
                text = "Competitions",
                color = WisdomInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val shownNotice = notice
        if (shownNotice != null) {
            Text(
                text = shownNotice,
                color = LiveGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
        val shownError = actionError
        if (shownError != null) {
            Text(
                text = shownError,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading && contests.isEmpty()) {
                CircularProgressIndicator(color = CyanAccent, strokeWidth = 3.dp)
            } else if (contests.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { // UX-CONSISTENCY-2026-10-17
                    WisdomMessageCard( // S6-msgcard-comp
                        message = errorText ?: "No competitions right now.",
                        isError = errorText != null,
                        onRetry = errorText?.let { { reloadKey += 1 } }
                    )
                    com.wisdomtower.academy.ui.auth.SignedOutSignInPill(topPadding = 14.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(rows, key = { _, item -> item.first.id }) { index, row ->
                        val contestId = row.first.id
                        CompetitionCard(
                            summary = row.first,
                            display = row.second,
                            isNew = contestId in freshIds,
                            busy = startingId != null,
                            starting = startingId == contestId,
                            onStart = { ranked -> startContest(contestId, ranked) },
                            onAskJoin = {
                                actionError = null
                                notice = null
                                confirmId = contestId
                            },
                            onOpenLeaderboard = {
                                actionError = null
                                notice = null
                                leaderboardOf = row.first
                            },
                            onOpenReview = {
                                actionError = null
                                notice = null
                                reviewOf = row.first
                            },
                            modifier = Modifier.wisdomReveal(key = "competition_card_" + contestId, index = index)
                        )
                    }
                }
            }
        }
    }

    // The exam, the rules page, the leaderboard and the review each cover the whole screen in their own window
    if (active != null) {
        CompetitionFullScreen(
            onBack = {
                session = null
                reloadKey += 1
            }
        ) {
            CompetitionExamScreen(
                session = active,
                onFinished = { result ->
                    session = null
                    notice = finishedNotice(result)
                    reloadKey += 1
                },
                onExit = {
                    session = null
                    reloadKey += 1
                },
                modifier = Modifier.fillMaxSize(),
                title = sessionTitle
            )
        }
    } else if (rulesFor != null) {
        CompetitionFullScreen(
            onBack = {
                if (startingId == null) confirmId = null
            }
        ) {
            CompetitionRulesScreen(
                title = rulesFor.title,
                questionCount = rulesFor.questionCount,
                durationMin = rulesFor.durationMin,
                starting = startingId != null,
                onStart = { startContest(rulesFor.id, true) },
                onCancel = { confirmId = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (board != null) {
        CompetitionFullScreen(onBack = { leaderboardOf = null }) {
            CompetitionLeaderboardScreen(
                competitionId = board.id,
                title = board.title,
                onBack = { leaderboardOf = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (reviewing != null) {
        CompetitionFullScreen(onBack = { reviewOf = null }) {
            CompetitionReviewScreen(
                competitionId = reviewing.id,
                title = reviewing.title,
                onBack = { reviewOf = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// One plain line after an exam. A practice score is shown only when the server sent it; a ranked score is
// not shown here (the leaderboard shows it after the window ends).
private fun finishedNotice(result: SubmitResult?): String {
    if (result == null) return "Your answers were already submitted."
    val score = result.score
    val total = result.total
    if (result.mode == "practice" && score != null && total != null) {
        return "Practice finished: " + score.toString() + " of " + total.toString() + "."
    }
    return "Your answers were submitted."
}

// Session P: why a live contest is closed to this student. Same wording as the repository's error messages.
private fun blockedText(reason: CompetitionError?): String = when (reason) {
    CompetitionError.NOT_ALLOWED_PACKAGE -> "This competition is not included in your account."
    CompetitionError.NOT_ALLOWED_AUDIENCE -> "This competition is not available for your level."
    CompetitionError.DISQUALIFIED -> "You were disqualified from this competition."
    else -> "You cannot take part in this competition."
}

@Composable
private fun CompetitionCard(
    summary: CompetitionSummary,
    display: CompetitionDisplay,
    isNew: Boolean,
    busy: Boolean,
    starting: Boolean,
    onStart: (Boolean) -> Unit,
    onAskJoin: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badge = if (display.phase == CompetitionPhase.PAUSED) {
        "Postponed by admin"
    } else {
        CompetitionPhaseLogic.phaseLabel(display.phase)
    }
    val badgeColor = when (display.phase) {
        CompetitionPhase.LIVE -> LiveGreen
        CompetitionPhase.UPCOMING -> CyanAccent
        else -> MutedText
    }

    // Session AH: see-through glass card, same call shape as the Account screen uses
    WisdomGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isNew) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(CyanAccent, CircleShape)
                    )
                }
                Text(
                    text = summary.title,
                    color = WisdomInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (badge.isNotEmpty()) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            val description = summary.description
            if (!description.isNullOrBlank()) {
                Text(text = description, color = MutedText, fontSize = 13.sp)
            }

            Text(
                text = summary.questionCount.toString() + " questions  \u2022  " + summary.durationMin.toString() + " min",
                color = DetailText,
                fontSize = 12.sp
            )

            if (display.countdownKind != CountdownKind.NONE) {
                val lead = if (display.countdownKind == CountdownKind.STARTS_IN) "Starts in " else "Ends in "
                Text(
                    text = lead + CompetitionPhaseLogic.formatCountdown(display.countdownMs),
                    color = CyanAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(text = "Starts: " + formatEat(summary.startsAtMs), color = MutedText, fontSize = 12.sp)
            Text(text = "Ends: " + formatEat(summary.endsAtMs), color = MutedText, fontSize = 12.sp)

            if (display.action == CompetitionAction.SUBMITTED) {
                Text(text = "Submitted", color = LiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (display.action == CompetitionAction.BLOCKED) {
                Text(
                    text = blockedText(display.blockedReason),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // C6 slice 3: the buttons. Join opens the rules page first (the ranked attempt cannot be restarted).
            val primaryLabel: String? = when (display.action) {
                CompetitionAction.JOIN -> "Join"
                CompetitionAction.RESUME -> "Resume"
                CompetitionAction.PRACTICE -> "Practice"
                else -> null
            }
            val showSecondaryPractice = display.action != CompetitionAction.PRACTICE &&
                display.canPractice && summary.practiceAvailable
            // C7 slice 2: the leaderboard of an ended contest (decision G: everyone in the audience)
            val showLeaderboard = display.canViewLeaderboard && summary.canEnter
            // C7 slice 3: the review, only for a student who has a ranked attempt in an ended contest
            val showReview = display.action == CompetitionAction.VIEW_RESULT

            // Session AH: pill buttons that share the row evenly, so none of the labels wraps
            if (primaryLabel != null || showSecondaryPractice || showLeaderboard || showReview) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (primaryLabel != null) {
                        CompetitionPillButton(
                            label = if (starting && display.action != CompetitionAction.JOIN) "Starting..." else primaryLabel,
                            emphasized = true,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (display.action) {
                                    CompetitionAction.JOIN -> onAskJoin()
                                    CompetitionAction.RESUME -> onStart(true)
                                    else -> onStart(false)
                                }
                            }
                        )
                    }
                    if (showSecondaryPractice) {
                        CompetitionPillButton(
                            label = if (starting) "Starting..." else "Practice",
                            emphasized = false,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = { onStart(false) }
                        )
                    }
                    if (showLeaderboard) {
                        CompetitionPillButton(
                            label = "Leaderboard",
                            emphasized = display.action == CompetitionAction.VIEW_RESULT,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenLeaderboard
                        )
                    }
                    if (showReview) {
                        CompetitionPillButton(
                            label = "Review",
                            emphasized = false,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenReview
                        )
                    }
                }
            }

            // prize and points arrive from the server only after the window ended; shown exactly as sent
            val prizeTitle = summary.prizeTitle
            if (!prizeTitle.isNullOrBlank()) {
                Text(text = prizeTitle, color = WisdomInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val prizeDetails = summary.prizeDetails
                if (!prizeDetails.isNullOrBlank()) {
                    Text(text = prizeDetails, color = MutedText, fontSize = 12.sp)
                }
            }
            val points = summary.points
            if (points != null) {
                Text(text = points.toString() + " points", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// All competition times are shown in Ethiopia time (owner Q8), whatever the phone's zone is.
private fun formatEat(ms: Long): String {
    val format = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US)
    format.timeZone = TimeZone.getTimeZone("GMT+03:00")
    return format.format(Date(ms)) + " EAT"
}
