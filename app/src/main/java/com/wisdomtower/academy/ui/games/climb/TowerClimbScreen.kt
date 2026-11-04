package com.wisdomtower.academy.ui.games.climb

import com.wisdomtower.academy.ui.theme.WisdomPopupScrim
import com.wisdomtower.academy.ui.theme.WisdomPopupFill
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.wisdomtower.academy.data.games.ClimbFloor
import com.wisdomtower.academy.data.games.ClimbTowerBuilder
import com.wisdomtower.academy.data.games.ClimbTowerModel
import com.wisdomtower.academy.data.games.GameStore
import com.wisdomtower.academy.ui.games.common.GamePrimaryButton
import com.wisdomtower.academy.ui.games.common.GameSecondaryButton
import com.wisdomtower.academy.ui.games.common.GameTheme
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

/**
 * P1f (2026-11-03, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md): when the screen opens (and when the student comes back from a
 * floor) it first builds the tower from the SAVED question-bank list ([LearningContentRepository.listSaved], no network),
 * then asks the server in the background and builds it again from the answer. A failed refresh keeps the tower that is
 * already drawn. The "Lighting the lantern" spinner shows only when nothing is saved (an empty saved list counts as
 * nothing saved). Retry reads the server directly, as before. NOT compiled, NOT tested on a device.
 */
@Composable
fun TowerClimbScreen(
    scopePath: String,
    courseTitle: String,
    locks: CourseLocks?,
    onBack: () -> Unit,
    onStartFloor: (ClimbFloor) -> Unit = {},
    onOpenAttic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var model by remember { mutableStateOf<ClimbTowerModel?>(null) }
    var records by remember { mutableStateOf<Map<String, GameStore.FloorRecord>>(emptyMap()) }
    var progress by remember { mutableStateOf(GameStore.getClimb(context, scopePath)) }
    var selected by remember { mutableStateOf<ClimbFloor?>(null) }
    var sealMessage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var playFloor by remember { mutableStateOf<ClimbFloor?>(null) }
    var showAttic by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    /** P1f: the tower build shared by the saved draw and the live draw (the body the old Success branch had). */
    fun applyItems(items: List<LearningResource>) {
        val climb = GameStore.getClimb(context, scopePath)
        progress = climb
        val cleared = climb.floors.filter { it.value.stars > 0 }.keys
        records = climb.floors
        model = ClimbTowerBuilder.build(scopePath, items, locks, cleared)
    }

    fun reload(savedFirst: Boolean = false) {
        error = null
        scope.launch {
            var drewSaved = false
            if (savedFirst) {
                val saved = LearningContentRepository.listSaved(context, scopePath, "question-banks")
                if (saved != null) {
                    applyItems(saved)
                    loading = false
                    drewSaved = true
                }
            }
            if (!drewSaved) loading = true
            when (val result = LearningContentRepository.list(context, scopePath, "question-banks")) {
                is ContentListResult.Success -> {
                    applyItems(result.items)
                    loading = false
                }
                is ContentListResult.Failure -> {
                    // A failed refresh keeps the tower that is already drawn.
                    if (!drewSaved) error = result.message
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(scopePath) { reload(savedFirst = true) }

    BackHandler(onBack = {
        when {
            playFloor != null -> playFloor = null
            showAttic -> showAttic = false
            else -> onBack()
        }
    })

    if (playFloor != null) {
        ClimbPlayScreen(
            scopePath = scopePath,
            floor = playFloor!!,
            locks = locks,
            allFloorsForBoss = model?.floors ?: emptyList(),
            onExitToTower = {
                playFloor = null
                reload(savedFirst = true)
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showAttic) {
        AtticScreen(
            attic = progress.attic,
            onBack = { showAttic = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070C16))
    ) {
        ClimbHeader(
            courseTitle = courseTitle,
            xp = progress.xp,
            bestStreak = progress.bestStreak,
            atticCount = progress.attic.size,
            onBack = onBack,
            onAttic = {
                showAttic = true
                onOpenAttic()
            }
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = WisdomCyan)
                        Spacer(Modifier.height(12.dp))
                        Text("Lighting the lantern…", color = WisdomMuted, fontSize = 14.sp)
                    }
                }
                error != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "", color = WisdomTextPrimary)
                        Spacer(Modifier.height(16.dp))
                        GamePrimaryButton(label = "Retry", onClick = { reload() })
                    }
                }
                model != null && model!!.floors.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No question banks in this course yet.", color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Check back when materials are published.", color = WisdomMuted, fontSize = 14.sp)
                    }
                }
                model != null -> {
                    TowerMapView(
                        model = model!!,
                        floorRecords = records,
                        onFloorTap = { floor ->
                            if (floor.sealed) {
                                sealMessage = sealCopy(floor.sealGate ?: HubGate.ComingSoon)
                            } else {
                                selected = floor
                            }
                        },
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                    val current = model!!.currentFloorNumber
                    if (current != null) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(WisdomCyan)
                                .clickable {
                                    scope.launch {
                                        val floors = model!!.floors
                                        val idx = floors.indexOfFirst { it.number == current }
                                        if (idx >= 0) {
                                            val revIndex = floors.size - 1 - idx
                                            listState.animateScrollToItem(revIndex + 1)
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("Jump to me", color = Color(0xFF070D17), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    selected?.let { floor ->
        FloorSheet(
            floor = floor,
            stars = records[floor.key]?.stars ?: 0,
            onDismiss = { selected = null },
            onClimb = {
                selected = null
                playFloor = floor
                onStartFloor(floor)
            }
        )
    }

    sealMessage?.let { (title, body) ->
        SealDialog(title = title, body = body, onDismiss = { sealMessage = null })
    }
}

private fun sealCopy(gate: HubGate): Pair<String, String> {
    return when (gate) {
        HubGate.ComingSoon -> ContentAccess.COMING_SOON_TITLE to ContentAccess.COMING_SOON_BODY
        HubGate.PackageRequired -> ContentAccess.PURCHASE_TITLE to ContentAccess.PURCHASE_BODY
        HubGate.Open -> ContentAccess.COMING_SOON_TITLE to ContentAccess.COMING_SOON_BODY
    }
}

@Composable
private fun ClimbHeader(
    courseTitle: String,
    xp: Int,
    bestStreak: Int,
    atticCount: Int,
    onBack: () -> Unit,
    onAttic: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xEE0A101C)).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = WisdomTextPrimary,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
        )
        Column(Modifier.weight(1f)) {
            Text("Tower Climb", color = WisdomCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(courseTitle, color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Text("XP $xp", color = GameTheme.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(6.dp))
        if (bestStreak > 0) {
            Text("🔥$bestStreak", fontSize = 13.sp)
            Spacer(Modifier.padding(4.dp))
        }
        Text(
            text = "Attic ($atticCount)",
            color = WisdomMuted,
            fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAttic).padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FloorSheet(
    floor: ClimbFloor,
    stars: Int,
    onDismiss: () -> Unit,
    onClimb: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(WisdomPopupScrim).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(WisdomPopupFill)
                .clickable { }
                .padding(20.dp)
        ) {
            Text(
                text = if (floor.isBoss) "Boss · Floor ${floor.number}" else "Floor ${floor.number}",
                color = WisdomTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(floor.title, color = WisdomMuted, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("${floor.questionHint} questions in pool", color = WisdomTextPrimary, fontSize = 14.sp)
            if (stars > 0) {
                Text("Best: ${"★".repeat(stars)}", color = GameTheme.gold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
            GamePrimaryButton(label = "Climb", onClick = onClimb, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GameSecondaryButton(label = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SealDialog(title: String, body: String, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(32.dp).clip(RoundedCornerShape(16.dp)).background(WisdomPopupFill).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = WisdomMuted)
            Spacer(Modifier.height(8.dp))
            Text(title, color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(body, color = WisdomMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            GamePrimaryButton(label = "Got it", onClick = onDismiss)
        }
    }
}
