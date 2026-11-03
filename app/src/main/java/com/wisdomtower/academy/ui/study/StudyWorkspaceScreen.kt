package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.ui.theme.WisdomInk
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningResource
import com.wisdomtower.academy.data.content.PackagePrices
import com.wisdomtower.academy.data.content.PaidCheckout
import com.wisdomtower.academy.ui.packages.NATIVE_LEARNING_HUBS
import com.wisdomtower.academy.ui.packages.OpenCard
import com.wisdomtower.academy.ui.packages.openGrid
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomGrowLayer
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

private const val SITE_ORIGIN = "https://www.wisdom-tower-academy.live"

private val NATIVE_HUB_IDS = setOf("books", "life-savers", "short-notes", "flashcards", "question-banks", "exams")

private sealed interface WorkspaceView {
    data class HubList(val hubId: String) : WorkspaceView
    data class Pdf(val item: LearningResource, val hubId: String) : WorkspaceView
    data class Note(val item: LearningResource) : WorkspaceView
    data class Cards(val item: LearningResource) : WorkspaceView
    data class Quiz(val item: LearningResource, val isExam: Boolean, val hubId: String) : WorkspaceView
}

private data class GateMessage(val gate: HubGate, val hubName: String)

private val NEEDS_SUBJECT_LEVEL = Regex("^/academy/(freshman|remedial|grades/[^/]+|special-packages/[^/]+/[^/]+)$")
private val ENTRANCE_YEAR_PATH = Regex("^/academy/entrance-questions/[^/]+/[^/]+/[0-9]{4}$")
private val SCOPE_FRESHMAN = Regex("^/academy/freshman/([^/]+)$")
private val SCOPE_REMEDIAL = Regex("^/academy/remedial/([^/]+)$")
private val SCOPE_GRADE = Regex("^/academy/grades/([^/]+)/([^/]+)$")
private val SCOPE_ECE = Regex("^/academy/special-packages/[^/]+/(sem-[12])/([^/]+)$")
private val SCOPE_SPECIAL = Regex("^/academy/(coc|uat|gat)$")
private val SCOPE_ENTRANCE = Regex("^/academy/entrance-questions/([^/]+)/([^/]+)/([0-9]{4})$")
private val TRACK_ECE = Regex("^/academy/special-packages/([^/]+)/(sem-[12])/([^/]+)$")

private fun cleanBase(basePath: String): String {
    var base = basePath.trim()
    if (base.startsWith("http")) base = base.removePrefix(SITE_ORIGIN)
    base = base.substringBefore('?').substringBefore('#').trimEnd('/')
    if (base.isEmpty()) base = "/academy"
    return base
}

internal fun entranceYearUrl(basePath: String): String? {
    val base = cleanBase(basePath)
    return if (ENTRANCE_YEAR_PATH.matches(base)) SITE_ORIGIN + base else null
}

internal fun contentScopePath(basePath: String): String? {
    var base = cleanBase(basePath)
    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) base = base.substringBeforeLast('/')
    SCOPE_FRESHMAN.matchEntire(base)?.let { return "freshman/${it.groupValues[1]}" }
    SCOPE_REMEDIAL.matchEntire(base)?.let { return "remedial/${it.groupValues[1]}" }
    SCOPE_GRADE.matchEntire(base)?.let { return "grade/${it.groupValues[1]}/${it.groupValues[2]}" }
    SCOPE_ECE.matchEntire(base)?.let { return "ece/${it.groupValues[1]}/${it.groupValues[2]}" }
    SCOPE_SPECIAL.matchEntire(base)?.let { return it.groupValues[1] }
    SCOPE_ENTRANCE.matchEntire(base)?.let {
        return "entrance/${it.groupValues[1]}/${it.groupValues[2]}/${it.groupValues[3]}"
    }
    return null
}

internal fun trackerScopeIdFor(basePath: String, hubId: String): String? {
    var base = cleanBase(basePath)
    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) base = base.substringBeforeLast('/')
    SCOPE_FRESHMAN.matchEntire(base)?.let { return "freshman-${it.groupValues[1]}" }
    SCOPE_REMEDIAL.matchEntire(base)?.let { return "remedial-${it.groupValues[1]}-$hubId" }
    SCOPE_GRADE.matchEntire(base)?.let { return "grade-${it.groupValues[1]}-${it.groupValues[2]}-$hubId" }
    TRACK_ECE.matchEntire(base)?.let {
        return "special-${it.groupValues[1]}-${it.groupValues[2]}-${it.groupValues[3]}-$hubId"
    }
    SCOPE_SPECIAL.matchEntire(base)?.let { return "${it.groupValues[1]}-$hubId" }
    SCOPE_ENTRANCE.matchEntire(base)?.let {
        return "entrance-${it.groupValues[1]}-${it.groupValues[2]}-${it.groupValues[3]}-exams"
    }
    return null
}

internal fun safeHubUrl(basePath: String, hubId: String): String {
    var base = cleanBase(basePath)
    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) base = base.substringBeforeLast('/')
    val path = if (NEEDS_SUBJECT_LEVEL.matches(base)) base else "$base/$hubId"
    return SITE_ORIGIN + path
}

/**
 * Course page (the hub cards: Books, Short Notes, Flashcards ...).
 * COMPACT HEADER (owner request 2026-10-22): no back arrow and no title + subtitle block. ONE small muted line names the
 * course; the semester / package is already known from the previous screen. Back = phone back key / gesture
 * ([BackHandler] below handles the native study views, the app handles leaving this page).
 *
 * TELEGRAM BUILD (slice P3, `docs/TELEGRAM_PAID_BUILD_PLAN.md`): while this page is on screen it tells [PaidCheckout] which
 * course it shows and how to open a website page (the same `onOpenHub` the hub cards use), so the "Package required" card
 * (here and in the hub list) can offer Buy now / My orders. It also reads the dashboard prices once. In the Play build
 * the card never asks for any of it, and `PackagePrices.refresh()` returns at once.
 *
 * P4: every time the app comes back to the front while this page is open (for example after the student paid in the
 * website checkout and the admin or auto-verify unlocked the package), the locks and ownership are read again. The
 * ownership answer is cached 30 s inside `ContentAccess`, so a return after a real checkout always reads fresh.
 *
 * P1b (2026-11-03, `docs/READER_EXAM_FLASHCARD_FIX_PLAN.md`): the counts on the hub cards come from the saved counts of
 * this course at once ([LearningContentRepository.getHubCountsSaved], no network); the server is asked in the background
 * ([LearningContentRepository.getHubCounts]) and the cards change only when a number really changed. A failed or empty
 * refresh never wipes counts that are already shown.
 */
@Composable
fun StudyWorkspaceScreen(
    title: String,
    scopeSubtitle: String,
    basePath: String,
    onBack: () -> Unit,
    onOpenHub: (hubId: String, hubUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entranceUrl = remember(basePath) { entranceYearUrl(basePath) }
    val contentScope = remember(basePath) { contentScopePath(basePath) }
    val isExitExam = remember(basePath) {
        val clean = cleanBase(basePath)
        clean == "/academy/exit-exam" || clean.startsWith("/academy/exit-exam/")
    }
    var view by remember(basePath) { mutableStateOf<WorkspaceView?>(null) }
    var exitExamHub by remember(basePath) { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var hubCounts by remember(contentScope) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // P1b: saved counts first (drawn at once), then the server in the background; change only when different.
    LaunchedEffect(contentScope) {
        if (contentScope != null) {
            val saved = LearningContentRepository.getHubCountsSaved(context, contentScope)
            if (saved != null && hubCounts.isEmpty()) hubCounts = saved
            val fresh = LearningContentRepository.getHubCounts(context, contentScope)
            // An empty answer (failed read, nothing saved) never wipes counts that are already on the cards.
            if (fresh.isNotEmpty() && fresh != hubCounts) hubCounts = fresh
            else if (fresh.isEmpty() && hubCounts.isEmpty()) hubCounts = fresh
        }
    }

    val loggedIn by AuthStateManager.isLoggedIn.collectAsState()
    val uiScope = rememberCoroutineScope()
    var locks by remember(basePath, loggedIn) {
        mutableStateOf<CourseLocks>(ContentAccess.staticLocks(basePath, loggedIn))
    }
    var gateMessage by remember(basePath) { mutableStateOf<GateMessage?>(null) }

    // P3 (Telegram build): what the "Package required" card needs. Harmless in the Play build (the card ignores it).
    SideEffect {
        PaidCheckout.scope = contentScope
        PaidCheckout.opener = { url -> onOpenHub("checkout", url) }
    }
    DisposableEffect(Unit) {
        onDispose {
            PaidCheckout.scope = null
            PaidCheckout.opener = null
        }
    }
    LaunchedEffect(Unit) { PackagePrices.refresh() }

    LaunchedEffect(basePath, loggedIn) {
        locks = ContentAccess.resolveLocks(basePath, loggedIn)
    }

    // P4: read the locks and ownership again when the app comes back to the front (skips the first resume at start).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, basePath, loggedIn) {
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) {
                    firstResume = false
                } else if (entranceUrl == null) {
                    uiScope.launch { locks = ContentAccess.resolveLocks(basePath, loggedIn) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (entranceUrl != null) {
        LaunchedEffect(entranceUrl) {
            if (contentScope != null) view = WorkspaceView.HubList("exams")
            else onOpenHub("exams", entranceUrl)
        }
    }

    val initialHub = remember(basePath) {
        cleanBase(basePath).substringAfterLast('/').takeIf {
            it in NATIVE_HUB_IDS && contentScope != null && entranceUrl == null
        }
    }
    var initialHubHandled by remember(basePath) { mutableStateOf(false) }
    LaunchedEffect(basePath, locks) {
        if (initialHub != null && !initialHubHandled && locks.hubGate(initialHub) == HubGate.Open) {
            initialHubHandled = true
            view = WorkspaceView.HubList(initialHub)
        }
    }

    BackHandler(enabled = view != null && view !is WorkspaceView.Quiz) {
        val open = view
        if (open is WorkspaceView.HubList && entranceUrl != null) {
            onBack()
            return@BackHandler
        }
        view = when (open) {
            is WorkspaceView.Pdf -> WorkspaceView.HubList(open.hubId)
            is WorkspaceView.Note -> WorkspaceView.HubList("short-notes")
            is WorkspaceView.Cards -> WorkspaceView.HubList("flashcards")
            else -> null
        }
    }

    val shown = view
    if (shown != null && contentScope != null) PauseShellWhileShown() // V5d hub grow

    // V5d hub grow: the sub-screens (hub list, book, note, cards, quiz) are drawn by this lambda, either inside the grow layer
    // or directly (website entrance link). The two parameters and `modifier` deliberately keep the old names, so the code
    // of every branch below is the same as before; the layer or the Box around it already applies the caller's modifier.
    val subView: @Composable (WorkspaceView, String) -> Unit = { shown, contentScope ->
        val modifier = Modifier
        when (shown) {
            is WorkspaceView.HubList -> HubItemListScreen(
                scopePath = contentScope,
                hubId = shown.hubId,
                hubName = NATIVE_LEARNING_HUBS.firstOrNull { it.id == shown.hubId }?.name ?: shown.hubId,
                courseTitle = title,
                onBack = { if (entranceUrl != null) onBack() else view = null },
                onOpenItem = { item ->
                    when (item.contentType) {
                        "pdf" -> view = WorkspaceView.Pdf(item, shown.hubId)
                        "markdown" -> view = WorkspaceView.Note(item)
                        "flashcard_deck" -> view = WorkspaceView.Cards(item)
                        "quiz" -> view = WorkspaceView.Quiz(item, false, shown.hubId)
                        "exam" -> view = WorkspaceView.Quiz(item, true, shown.hubId)
                    }
                },
                modifier = modifier,
                locks = locks
            )

            is WorkspaceView.Pdf -> PdfReaderScreen(
                title = shown.item.title,
                url = LearningContentRepository.appwriteViewUrl(shown.item.storagePath),
                cacheKey = shown.item.id,
                onBack = { view = WorkspaceView.HubList(shown.hubId) },
                modifier = modifier,
                resourceId = shown.item.id
            )

            is WorkspaceView.Note -> NotesReaderScreen(
                title = shown.item.title,
                resourceId = shown.item.id,
                onBack = { view = WorkspaceView.HubList("short-notes") },
                modifier = modifier
            )

            is WorkspaceView.Cards -> FlashcardsScreen(
                title = shown.item.title,
                resourceId = shown.item.id,
                onBack = { view = WorkspaceView.HubList("flashcards") },
                modifier = modifier
            )

            is WorkspaceView.Quiz -> QuizExamScreen(
                title = shown.item.title,
                resourceId = shown.item.id,
                isExam = shown.isExam,
                trackerScopeId = trackerScopeIdFor(basePath, shown.hubId),
                onBack = { view = WorkspaceView.HubList(shown.hubId) },
                modifier = modifier
            )
        }
    }

    val layerItem: WorkspaceView? = if (contentScope != null) shown else null
    if (entranceUrl != null && layerItem != null && contentScope != null) {
        // V5d hub grow: opened straight into a hub from a website link. No grid behind it, no grow (as before).
        Box(modifier = modifier.fillMaxSize()) { subView(layerItem, contentScope) }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().then(if (layerItem != null) Modifier.clearAndSetSemantics { } else Modifier)) { // V5d hub grow
            Text(
                text = title,
                color = WisdomMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (loggedIn && contentScope != null && !isExitExam) {
                    item(key = "status_board") {
                        StatusSubjectBoard(scopePath = contentScope, title = title) // MYSTATUS-B3
                    }
                }
                val hubs = NATIVE_LEARNING_HUBS
                openGrid("hubs", hubs) { hub ->
                    val count = hubCounts[hub.id]
                    val subtitle = when {
                        count == null || count <= 0 -> null
                        hub.id == "books" -> "$count ${if (count == 1) "book" else "books"}"
                        hub.id == "short-notes" -> "$count ${if (count == 1) "note" else "notes"}"
                        hub.id == "flashcards" -> "$count ${if (count == 1) "deck" else "decks"}"
                        hub.id == "question-banks" -> "$count ${if (count == 1) "bank" else "banks"}"
                        hub.id == "exams" -> "$count ${if (count == 1) "exam" else "exams"}"
                        hub.id == "life-savers" -> "$count ${if (count == 1) "item" else "items"}"
                        else -> "$count items"
                    }
                    OpenCard(
                        title = hub.name,
                        image = hub.assetImage,
                        subtitle = subtitle,
                        onClick = {
                            if (isExitExam) {
                                exitExamHub = hub.name
                            } else {
                                val gate = if (entranceUrl == null) locks.hubGate(hub.id) else HubGate.Open
                                when (gate) {
                                    HubGate.ComingSoon, HubGate.PackageRequired ->
                                        gateMessage = GateMessage(gate, hub.name)
                                    HubGate.Open -> {
                                        if (contentScope != null && hub.id in NATIVE_HUB_IDS) {
                                            view = WorkspaceView.HubList(hub.id)
                                        } else {
                                            onOpenHub(hub.id, entranceUrl ?: safeHubUrl(basePath, hub.id))
                                        }
                                    }
                                }
                                if (entranceUrl == null) {
                                    uiScope.launch { locks = ContentAccess.resolveLocks(basePath, loggedIn) }
                                }
                            }
                        }
                    )
                }
                item(key = "bottom_space") { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // V5d hub grow: grows out of the tapped hub tile, shrinks back into it. Touches stop here (nothing under it is tapped).
        if (contentScope != null) {
            WisdomGrowLayer(item = layerItem, zIndexValue = 1f) { layerView ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                ) { subView(layerView, contentScope) }
            }
        }

        val message = gateMessage
        if (message != null) {
            if (message.gate == HubGate.ComingSoon) {
                HubGateDialog(
                    badge = ContentAccess.COMING_SOON_TITLE,
                    title = ContentAccess.COMING_SOON_TITLE,
                    hubName = message.hubName,
                    message = ContentAccess.COMING_SOON_BODY,
                    buttonText = ContentAccess.COMING_SOON_BUTTON,
                    onClose = { gateMessage = null }
                )
            } else {
                HubGateDialog(
                    badge = null,
                    title = ContentAccess.PURCHASE_TITLE,
                    hubName = message.hubName,
                    message = ContentAccess.PURCHASE_BODY,
                    buttonText = ContentAccess.PURCHASE_BUTTON,
                    onClose = { gateMessage = null }
                )
            }
        }

        val exitHub = exitExamHub
        if (exitHub != null) {
            HubGateDialog(
                badge = "Content coming soon",
                title = "Content coming soon",
                hubName = exitHub,
                message = "Exit Exam ${exitHub.lowercase()} materials will appear here.",
                buttonText = ContentAccess.COMING_SOON_BUTTON,
                onClose = { exitExamHub = null }
            )
        }
    }
}
