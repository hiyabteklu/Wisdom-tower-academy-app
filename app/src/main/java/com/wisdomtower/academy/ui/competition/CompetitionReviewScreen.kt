package com.wisdomtower.academy.ui.competition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.competition.CompetitionRepository
import com.wisdomtower.academy.data.competition.CompetitionResult
import com.wisdomtower.academy.data.competition.ExamLockdown
import com.wisdomtower.academy.data.competition.ReviewQuestion
import com.wisdomtower.academy.data.competition.ReviewResult
import com.wisdomtower.academy.data.content.AiExplainRepository
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// D7e (docs/DAY_MODE_PLAN.md, 2026-10-19): the three private colours were top-level vals (read once at class load, so they
// would not follow the day / night switch). They are getters now. Night keeps the old values exactly (cyan 22E0FF, muted
// 94A3B8, red F87171); day uses WisdomCyan (deep teal 0B7285), WisdomMuted (4A5A70) and a deeper red B91C1C. The two white
// text lines became WisdomInk.
private val RvCyan: Color
    get() = WisdomCyan
private val RvMuted: Color
    get() = if (AppThemeMode.isDark) Color(0xFF94A3B8) else WisdomMuted
private val RvRed: Color
    get() = if (AppThemeMode.isDark) Color(0xFFF87171) else Color(0xFFB91C1C)

// Solid-ish base under each floating button, so the text of the question never shows through the pill.
private val FloatBase: Color
    get() = if (AppThemeMode.isDark) Color(0xF2101829) else Color(0xF2FBF9F4)

// C7 slice 3: review of the student's own ranked attempt after the window closed. Loads
// CompetitionRepository.review (the server refuses it earlier, the plain message is shown), then shows one
// question at a time through CompetitionReviewView (math rendered): the correct answer, the student's pick and
// the solution, all exactly as the server sent them. Previous and Next move between questions.
// C7 slice 4: Explain with AI button (owner decision Q10). It calls AiExplainRepository.explain with the same body
// the quiz page sends (mode question, text, question, choices, studentAnswer, correctAnswer, solution). The answer
// is kept per question for this screen only (nothing is stored) and is shown in the review page with math rendered.
// The button is not shown while ExamLockdown.active is true (a ranked exam is running).
// Session AJ (owner, 2026-10-15): the review follows the hub's review look. The two hub buttons (Official
// solution, Explain with AI) now live inside the page in ONE row, the solution is closed when a question opens,
// the separate native Explain button is gone, and a slide left or right moves to the next or previous question
// (the page calls onSwipe).
// Owner request 2026-10-24: FULL HEIGHT. Once the review is loaded there is no Back row, no title, no score line and no
// Question N of M line any more: the page fills the whole screen (leave with the phone Back button or gesture, the
// BackHandler below). Previous and Next are two separate floating pills over the page (no strip, no second footer), with
// a small N / M pill between them. The page gets extra bottom padding (bottomPaddingDp) so the last lines can scroll
// above the pills. The Back row stays only while loading, on an error and when there are no questions, so there is
// never a dead end.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.
@Composable
fun CompetitionReviewScreen(
    competitionId: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loading by remember(competitionId) { mutableStateOf(true) }
    var review by remember(competitionId) { mutableStateOf<ReviewResult?>(null) }
    var errorText by remember(competitionId) { mutableStateOf<String?>(null) }
    var reloadKey by remember(competitionId) { mutableIntStateOf(0) }
    var index by remember(competitionId) { mutableIntStateOf(0) }
    val explanations = remember(competitionId) { mutableStateMapOf<String, String>() }
    var explainingId by remember(competitionId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(competitionId, reloadKey) {
        loading = true
        errorText = null
        val r = CompetitionRepository.review(competitionId)
        if (r is CompetitionResult.Ok) {
            review = r.value
        } else if (r is CompetitionResult.Failed) {
            errorText = r.message
        }
        loading = false
    }

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        val res = review
        val err = errorText
        if (res == null || res.questions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompetitionPillButton(label = "Back", onClick = onBack, modifier = Modifier.size(width = 76.dp, height = 40.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = title,
                        color = WisdomInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (loading && res == null) {
                    Text(text = "Loading...", color = RvMuted, fontSize = 14.sp)
                } else if (res == null) {
                    Text(text = err ?: "The review could not be loaded.", color = RvRed, fontSize = 13.sp)
                    CompetitionPillButton(
                        label = "Try again",
                        onClick = { reloadKey += 1 },
                        emphasized = true,
                        modifier = Modifier.size(width = 120.dp, height = 40.dp)
                    )
                } else {
                    Text(text = "There are no questions to review.", color = RvMuted, fontSize = 14.sp)
                }
            }
        } else {
            val questions = res.questions
            val position = if (index >= questions.size) questions.size - 1 else index
            val q = questions[position]

            CompetitionReviewView(
                questionId = q.id,
                prompt = q.prompt,
                choices = q.choices,
                correct = q.correct,
                chosen = q.chosen,
                solution = q.solution,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                bottomPaddingDp = 72,
                explanation = explanations[q.id] ?: "",
                // the Explain button is not drawn while a ranked exam is running
                canExplain = !ExamLockdown.active,
                explaining = explainingId == q.id,
                onSwipe = { next ->
                    if (next) {
                        if (position < questions.size - 1) index = position + 1
                    } else {
                        if (position > 0) index = position - 1
                    }
                },
                onExplain = {
                    if (explainingId == null && !ExamLockdown.active) {
                        val asked = q
                        scope.launch {
                            explainingId = asked.id
                            val answer = AiExplainRepository.explain(buildExplainPayload(asked))
                            explanations[asked.id] = answer
                            explainingId = null
                        }
                    }
                }
            )

            // Floating pills: no strip behind them, the gaps between them pass touches through to the page.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingNavPill(
                    label = "Previous",
                    enabled = position > 0,
                    emphasized = false,
                    onClick = { index = position - 1 }
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FloatBase)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (position + 1).toString() + " / " + questions.size.toString(),
                        color = RvCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                FloatingNavPill(
                    label = "Next",
                    enabled = position < questions.size - 1,
                    emphasized = true,
                    onClick = { index = position + 1 }
                )
            }
        }
    }
}

// One floating pill: the glass pill on top of a solid-ish base so it stays readable over the page text.
@Composable
private fun FloatingNavPill(
    label: String,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier
        .clip(CircleShape)
        .background(FloatBase)) {
        CompetitionPillButton(
            label = label,
            onClick = onClick,
            emphasized = emphasized,
            enabled = enabled,
            modifier = Modifier.size(width = 108.dp, height = 40.dp)
        )
    }
}

// The body the website's quiz page sends to /api/ai/explain, built only from what the server sent for this question.
private fun buildExplainPayload(q: ReviewQuestion): String {
    val list = JSONArray()
    for (c in q.choices) list.put(c)
    val picked = q.chosen
    val student = if (picked != null && picked >= 0 && picked < q.choices.size) q.choices[picked] else "(not answered)"
    val right = if (q.correct >= 0 && q.correct < q.choices.size) q.choices[q.correct] else ""
    return JSONObject()
        .put("mode", "question")
        .put("text", q.prompt)
        .put("question", q.prompt)
        .put("choices", list)
        .put("studentAnswer", student)
        .put("correctAnswer", right)
        .put("solution", q.solution)
        .toString()
}
