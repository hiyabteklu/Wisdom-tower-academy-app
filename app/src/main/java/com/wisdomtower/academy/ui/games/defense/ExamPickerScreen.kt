package com.wisdomtower.academy.ui.games.defense

import androidx.activity.compose.BackHandler
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.data.content.ContentListResult
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningResource
import com.wisdomtower.academy.data.games.DefenseMode
import com.wisdomtower.academy.data.games.GameStore
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

/**
 * P1f (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md): when the screen opens it first draws the SAVED exam list of the
 * course ([LearningContentRepository.listSaved], no network), then asks the server in the background and replaces the list
 * only when it differs. A failed refresh keeps the saved list. The spinner shows only when nothing is saved (an empty saved
 * list counts as nothing saved). Retry reads the server directly, as before. NOT compiled, NOT tested on a device.
 */
@Composable
fun ExamPickerScreen(
    scopePath: String,
    courseTitle: String,
    locks: CourseLocks?,
    onBack: () -> Unit,
    onStart: (mode: DefenseMode, exams: List<LearningResource>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var exams by remember { mutableStateOf<List<LearningResource>>(emptyList()) }
    var sealMsg by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()

    fun reload(savedFirst: Boolean = false) {
        error = null
        scope.launch {
            var drewSaved = false
            if (savedFirst) {
                val saved = LearningContentRepository.listSaved(context, scopePath, "exams")
                if (saved != null) {
                    exams = saved
                    loading = false
                    drewSaved = true
                }
            }
            if (!drewSaved) loading = true
            when (val r = LearningContentRepository.list(context, scopePath, "exams")) {
                is ContentListResult.Success -> {
                    if (r.items != exams) exams = r.items
                    loading = false
                }
                is ContentListResult.Failure -> {
                    // A failed refresh keeps the saved list that is already drawn.
                    if (!drewSaved) error = r.message
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(scopePath) { reload(savedFirst = true) }
    BackHandler(onBack = onBack)

    fun gateOf(item: LearningResource): HubGate? {
        if (locks == null) return null
        val g = locks.itemGate("exams", item.chapter, item.id)
        return if (g == null || g == HubGate.Open) null else g
    }

    Column(modifier.fillMaxSize().background(Color(0xFF070C16))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                tint = WisdomTextPrimary,
                modifier = Modifier.wisdomTouchFeel().clickable(onClick = onBack).padding(8.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Tower Defense", color = WisdomCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(courseTitle, color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WisdomCyan)
            }
            error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: "", color = WisdomTextPrimary)
                Spacer(Modifier.height(12.dp))
                GamePrimaryButton("Retry", onClick = { reload() })
            }
            exams.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No exams in this course yet.", color = WisdomMuted)
            }
            else -> {
                val openExams = exams.filter { gateOf(it) == null }
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Pick an exam run, or endless from all open exams.", color = WisdomMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        if (openExams.isNotEmpty()) {
                            GamePrimaryButton(
                                label = "Endless (${openExams.size} exams)",
                                onClick = { onStart(DefenseMode.Endless, openExams) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Text("Exam Run", color = WisdomTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    items(exams, key = { it.id }) { exam ->
                        val gate = gateOf(exam)
                        val sealed = gate != null
                        val best = if (!sealed) GameStore.getDefense(context, scopePath, exam.id) else null
                        Row(
                            Modifier.wisdomTouchFeel().fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF121A2A))
                                .clickable {
                                    if (sealed) {
                                        sealMsg = when (gate) {
                                            HubGate.PackageRequired ->
                                                ContentAccess.PURCHASE_TITLE to ContentAccess.PURCHASE_BODY
                                            else -> ContentAccess.COMING_SOON_TITLE to ContentAccess.COMING_SOON_BODY
                                        }
                                    } else onStart(DefenseMode.ExamRun, listOf(exam))
                                }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold)
                                val count = exam.itemCount
                                Text(if (count != null) "$count questions" else "Exam", color = WisdomMuted, fontSize = 12.sp)
                                if (best != null && best.bestScore > 0) {
                                    Text("Best ${best.bestScore} · wave ${best.bestWave}", color = GameTheme.gold, fontSize = 12.sp)
                                }
                            }
                            if (sealed) Icon(Icons.Filled.Lock, contentDescription = null, tint = WisdomMuted)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    sealMsg?.let { (title, body) ->
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { sealMsg = null }, contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(32.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121A2A)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = WisdomTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(body, color = WisdomMuted, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                GamePrimaryButton("Got it", onClick = { sealMsg = null })
            }
        }
    }
}
