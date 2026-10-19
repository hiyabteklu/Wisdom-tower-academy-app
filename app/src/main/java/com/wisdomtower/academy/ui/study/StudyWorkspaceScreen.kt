package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.ui.theme.WisdomInk
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.data.content.CourseLocks
import com.wisdomtower.academy.data.content.HubGate
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningResource
import com.wisdomtower.academy.ui.packages.NATIVE_LEARNING_HUBS
import com.wisdomtower.academy.ui.packages.OpenCard
import com.wisdomtower.academy.ui.packages.openGrid
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.launch

private const val SITE_ORIGIN = "https://www.wisdom-tower-academy.live"

/**
 * Hubs that open in a NATIVE screen (list from Supabase + native viewer) instead of the website page.
 * "books" and "life-savers" (native PDF reader, plan steps N2 + N4; Life Savers rows are PDFs exactly like
 * books: admin hub `life-savers`, content type `pdf`, file in Appwrite), "short-notes" (local KaTeX page, step N6),
 * "flashcards" (local deck page, step N3) and "question-banks" + "exams" (local quiz page, step N5).
 * To switch a hub back to the website page (for example if the native screen misbehaves on a phone),
 * remove its id from this set: nothing else changes.
 */
private val NATIVE_HUB_IDS = setOf("books", "life-savers", "short-notes", "flashcards", "question-banks", "exams")

/** Which native screen is open on top of the six hub cards (null = the cards themselves). */
private sealed interface WorkspaceView {
    data class HubList(val hubId: String) : WorkspaceView
    /** [hubId] is the hub the PDF was opened from (`books` or `life-savers`), so Back returns to that list. */
    data class Pdf(val item: LearningResource, val hubId: String) : WorkspaceView
    data class Note(val item: LearningResource) : WorkspaceView
    data class Cards(val item: LearningResource) : WorkspaceView
    /** [hubId] is the hub the item was opened from (`question-banks` or `exams`), used for the tracker scope id. */
    data class Quiz(val item: LearningResource, val isExam: Boolean, val hubId: String) : WorkspaceView
}

/** A "closed hub" message waiting on screen (plan step N9): which gate stopped the student and which hub card was tapped. */
private data class GateMessage(val gate: HubGate, val hubName: String)

/**
 * Website route shapes (website-reference/src/app/academy):
 *  - hubs live at /academy/{coc|uat|gat|exit-exam}/{hub}
 *  - hubs live at /academy/freshman/{subject}/{hub}, /academy/remedial/{subject}/{hub},
 *    /academy/grades/{grade}/{subject}/{hub}, /academy/special-packages/{slug}/{semester}/{course}/{hub}
 *  - there is NO hub directly under /academy/freshman, /academy/remedial, /academy/grades/{grade}
 *    or /academy/special-packages/{slug}/{semester}: "{base}/books" there would be read as a subject
 *    or course called "books" and give "404 Page Not Found".
 *  - Entrance Questions: the YEAR page /academy/entrance-questions/{stream}/{subject}/{year} IS the exams
 *    hub (website `HubContentView` with hub `exams`); there are no six hubs below it.
 */
private val NEEDS_SUBJECT_LEVEL = Regex(
    "^/academy/(freshman|remedial|grades/[^/]+|special-packages/[^/]+/[^/]+)$"
)

private val ENTRANCE_YEAR_PATH = Regex(
    "^/academy/entrance-questions/[^/]+/[^/]+/[0-9]{4}$"
)

// Course page -> `learning_resources.scope_path`, as the website hub pages build it (`scopePath` in
// website-reference/src/app/academy/.../[resource]/page.tsx and `eceScope` in lib/content.ts).
private val SCOPE_FRESHMAN = Regex("^/academy/freshman/([^/]+)$")
private val SCOPE_REMEDIAL = Regex("^/academy/remedial/([^/]+)$")
private val SCOPE_GRADE = Regex("^/academy/grades/([^/]+)/([^/]+)$")
private val SCOPE_ECE = Regex("^/academy/special-packages/[^/]+/(sem-[12])/([^/]+)$")

// 2026-10-12 (owner: "if I add content in the website/DB the app still catches it, go native while empty"):
// COC / UAT / GAT hub pages use the fixed scope path `coc` / `uat` / `gat` (their `[resource]/page.tsx`), and an
// Entrance Questions year page uses `entranceScope()` = `entrance/<stream>/<subject>/<year>` (data/entrance-questions.ts).
private val SCOPE_SPECIAL = Regex("^/academy/(coc|uat|gat)$")
private val SCOPE_ENTRANCE = Regex("^/academy/entrance-questions/([^/]+)/([^/]+)/([0-9]{4})$")

// Same ECE route, but keeping the package slug too (needed for the website's `trackerScopeId`).
private val TRACK_ECE = Regex("^/academy/special-packages/([^/]+)/(sem-[12])/([^/]+)$")

private fun cleanBase(basePath: String): String {
    var base = basePath.trim()
    if (base.startsWith("http")) {
        base = base.removePrefix(SITE_ORIGIN)
    }
    base = base.substringBefore('?').substringBefore('#').trimEnd('/')
    if (base.isEmpty()) base = "/academy"
    return base
}

/** Address of an Entrance Questions year page (the exams page itself), or null for any other path. */
internal fun entranceYearUrl(basePath: String): String? {
    val base = cleanBase(basePath)
    return if (ENTRANCE_YEAR_PATH.matches(base)) SITE_ORIGIN + base else null
}

/**
 * The `learning_resources.scope_path` of the course at [basePath] (for example `freshman/physics`,
 * `grade/9/biology`, `remedial/maths`, `ece/sem-2/eceg3082`, `coc`, `entrance/natural/physics/2015`), or null when
 * the path is not a course page the native screens can read (Exit Exam, package levels): those keep using the
 * website page (Exit Exam has no database content on the website either: it shows "Content coming soon").
 */
internal fun contentScopePath(basePath: String): String? {
    var base = cleanBase(basePath)
    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) {
        base = base.substringBeforeLast('/')
    }
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

/**
 * The website's `trackerScopeId` for the hub [hubId] of the course at [basePath] (the id its hub page passes to
 * `HubContentView`, and that `saveExamAttempt` writes to `academic_results.scope_id`), copied from
 * `website-reference/src/app/academy/.../[resource]/page.tsx`:
 *  - Freshman: `freshman-<subject>` (no hub in it);
 *  - Remedial: `remedial-<subject>-<hub>`; Grades: `grade-<grade>-<subject>-<hub>`;
 *  - ECE: `special-<package slug>-<sem-1|sem-2>-<course>-<hub>`;
 *  - COC / UAT / GAT: `coc-<hub>` / `uat-<hub>` / `gat-<hub>`;
 *  - Entrance Questions year: `entrance-<stream>-<subject>-<year>-exams`.
 * Null for any path the native screens do not read (the website page then does the saving).
 */
internal fun trackerScopeIdFor(basePath: String, hubId: String): String? {
    var base = cleanBase(basePath)
    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) {
        base = base.substringBeforeLast('/')
    }
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

/**
 * Builds the address of a learning hub so it can never point at a page that does not exist:
 *  1. a base that already ends with a hub id (for example "/academy/freshman/books") loses that hub id
 *     first, so the hub is never doubled ("/books/books");
 *  2. a base that is only a package level without subjects opens the real package page instead of
 *     a hub that does not exist there.
 */
internal fun safeHubUrl(basePath: String, hubId: String): String {
    var base = cleanBase(basePath)

    val last = base.substringAfterLast('/')
    if (NATIVE_LEARNING_HUBS.any { it.id == last }) {
        base = base.substringBeforeLast('/')
    }

    val path = if (NEEDS_SUBJECT_LEVEL.matches(base)) base else "$base/$hubId"
    return SITE_ORIGIN + path
}

/**
 * Native Study Workspace Screen: header + the 6 Learning Hub cards.
 *
 * S12b (2026-10-11): removed everything the website does not have (Performance Tracker, Log Score,
 * Recent Activity & Drills, invented subtitle and grade labels). A real tracker must be rebuilt only
 * from the website's real data (see STATUS.md, S12b).
 *
 * Owner card rule (2026-10-11): every card is the shared [OpenCard] (picture, title, "Open") in a
 * two-column grid. The old hub card had a dark gradient over the picture, a category tag and a
 * description; all three are gone. Hub names are the website's (`data/academy.ts` `resourceHubs`).
 * Links: see [safeHubUrl].
 *
 * Entrance Questions year (2026-10-11): the year page is the exams page itself, so this screen opens it
 * straight away (LaunchedEffect) and, as a fallback if that is ever missed, shows a single "Exams" card.
 * Since 2026-10-12 the year page is NATIVE too (scope `entrance/<stream>/<subject>/<year>`, hub `exams`).
 *
 * Native hubs (2026-10-11, plan N2 + N3 + N4 + N5 + N6): a hub listed in [NATIVE_HUB_IDS] opens [HubItemListScreen]
 * (rows read from Supabase) and then [PdfReaderScreen] (books and life savers), [NotesReaderScreen] (short notes),
 * [FlashcardsScreen] (flashcard decks) or [QuizExamScreen] (question banks and exams), all inside this screen,
 * so `MainActivity` is not involved. System Back goes viewer -> list -> the six cards (the quiz screen
 * handles Back itself first, to close its own dialogs).
 * The other hubs still open the website page through `onOpenHub`.
 *
 * COC / UAT / GAT / Entrance (2026-10-12, owner: "go native while empty"): these courses now read their rows
 * from Supabase like every other course. While no rows exist the native list shows its normal empty state, and the
 * first row added on the website (admin) shows up here with no app update, because the scope paths are the
 * website's own (see [contentScopePath]). Exit Exam is different on the website itself: its hub pages are static
 * ("Content coming soon", no database read), so the native screen shows that same message in a dialog.
 *
 * Life Savers (2026-10-12, owner request "make it native, identical to Books, 100% PDF"): the hub `life-savers`
 * is now in [NATIVE_HUB_IDS]. It uses the same list, download buttons, PDF reader, last-page memory and offline
 * saving as Books, because the repository reads any hub by id and the viewer is chosen by the row's content type.
 *
 * Access check (2026-10-11, plan N9): tapping a hub card first asks [ContentAccess] (admin locks in
 * `content_locks` + ownership; free mode = every signed-in student owns everything). A closed course shows the
 * website's "Coming soon" message; a signed-in student without the package sees "Package required" (no price,
 * no purchase button: purchases happen on the website). The state is read when the screen opens and again after
 * every card tap (cached 30 s), so a change made on the website is picked up on the next visit.
 *
 * GRANULAR LOCKS (2026-10-20, owner order): the check is now per HUB ([CourseLocks.hubGate]): a `hub:` lock row
 * closes (or opens) only that hub card, no longer all six together; chapter and item locks are applied by
 * [HubItemListScreen], which receives the same [CourseLocks]. Key formats: see [CourseLocks]. No row = the course /
 * package / static rule decides, exactly as before.
 *
 * F1 (2026-10-12): while a native list or viewer is shown, [PauseShellWhileShown] pauses the hidden website
 * WebView underneath (hypothesis for the owner's "vertical scroll freezes" report).
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
    LaunchedEffect(contentScope) {
        if (contentScope != null) {
            hubCounts = LearningContentRepository.getHubCounts(context, contentScope)
        }
    }

    val loggedIn by AuthStateManager.isLoggedIn.collectAsState()
    val uiScope = rememberCoroutineScope()
    var locks by remember(basePath, loggedIn) {
        mutableStateOf<CourseLocks>(ContentAccess.staticLocks(basePath, loggedIn))
    }
    var gateMessage by remember(basePath) { mutableStateOf<GateMessage?>(null) }

    LaunchedEffect(basePath, loggedIn) {
        locks = ContentAccess.resolveLocks(basePath, loggedIn)
    }

    if (entranceUrl != null) {
        LaunchedEffect(entranceUrl) {
            if (contentScope != null) {
                // The year page is the exams hub: open the native exams list straight away.
                view = WorkspaceView.HubList("exams")
            } else {
                onOpenHub("exams", entranceUrl)
            }
        }
    }

    // P13 initial hub: a link that ends with a hub id (for example .../physics/life-savers) opens that native
    // hub list straight away, once the access check says that hub is open. Back returns to the six cards.
    val initialHub = remember(basePath) {
        cleanBase(basePath).substringAfterLast('/')
            .takeIf { it in NATIVE_HUB_IDS && contentScope != null && entranceUrl == null }
    }
    var initialHubHandled by remember(basePath) { mutableStateOf(false) }
    LaunchedEffect(basePath, locks) {
        if (initialHub != null && !initialHubHandled && locks.hubGate(initialHub) == HubGate.Open) {
            initialHubHandled = true
            view = WorkspaceView.HubList(initialHub)
        }
    }

    // The quiz screen has its own BackHandler (it closes its dialogs first and then calls onBack below).
    BackHandler(enabled = view != null && view !is WorkspaceView.Quiz) {
        val open = view
        // Entrance year: the exams list is the whole page, so Back leaves the workspace.
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
    if (shown != null && contentScope != null) {
        // F1: keep the hidden website WebView quiet while a native list or viewer is on screen.
        PauseShellWhileShown()
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
        return
    }

    val hubs = if (entranceUrl != null) {
        NATIVE_LEARNING_HUBS.filter { it.id == "exams" }
    } else {
        NATIVE_LEARNING_HUBS
    }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Navigation & Subject Title Header
            item(key = "workspace_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScreenBackButton(onBack)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = scopeSubtitle.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WisdomCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WisdomInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // "Learning Hubs" label: same wording as the website course pages (small, uppercase, muted)
            item(key = "learning_hubs_title") {
                Text(
                    text = (if (entranceUrl != null) "Exams" else "Learning Hubs").uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WisdomMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // The 6 hub cards (Books, Short Notes, Flashcards, Question Banks, Exams, Life Savers);
            // for an Entrance Questions year only the Exams card (the year page itself)
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
                        // Exit Exam: the website hub pages are static "Content coming soon" (no database rows).
                        if (isExitExam) {
                            exitExamHub = hub.name
                        } else {
                            // N9 + granular locks: THIS hub's own gate (hub row, else course / package / static rule).
                            // Entrance Questions are never gated here.
                            val gate = if (entranceUrl == null) locks.hubGate(hub.id) else HubGate.Open
                            when (gate) {
                                HubGate.ComingSoon, HubGate.PackageRequired -> {
                                    gateMessage = GateMessage(gate, hub.name)
                                }

                                HubGate.Open -> {
                                    if (contentScope != null && hub.id in NATIVE_HUB_IDS) {
                                        view = WorkspaceView.HubList(hub.id)
                                    } else {
                                        onOpenHub(hub.id, entranceUrl ?: safeHubUrl(basePath, hub.id))
                                    }
                                }
                            }
                            if (entranceUrl == null) {
                                // Re-check in the background so the next tap uses a fresh answer (cached 30 s).
                                uiScope.launch { locks = ContentAccess.resolveLocks(basePath, loggedIn) }
                            }
                        }
                    }
                )
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(32.dp))
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
            // Website wording of `exit-exam/[resource]/page.tsx` ("Content coming soon" + first sentence).
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
