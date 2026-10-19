package com.wisdomtower.academy.ui.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wisdomtower.academy.data.admin.AdminCounts
import com.wisdomtower.academy.data.admin.AdminCountsRepository
import com.wisdomtower.academy.ui.theme.WisdomBackground

// ADMIN-native N1 (docs/ADMIN_NATIVE_PLAN.md). THE entry point the app already calls from MainActivity.kt
// (patch ADMIN-host): AdminConsoleScreen(url, onClose, onLeaveToApp, modifier). The signature did not change, so
// MainActivity.kt needed no edit. The old WebView-only body of this function now lives in AdminConsoleScreen.kt
// under the name AdminWebSection and is used for every module that has no native screen yet.
//
// Flow: native home grid -> tap a module -> (native screen if its id is in NATIVE_ADMIN_MODULE_IDS, otherwise the
// website panel in AdminWebSection under a native section bar) -> Back returns to the grid -> Back on the grid closes.
// Deep links keep working: a url with ?tab=<id or alias> opens that module directly.
// N2 (2026-10-18): Inquiries is native. N3: Payments is native; the url path /admin/payments also opens it.
// N4: Scholars and Users is native. N5: Overview is native. N6: Access Grants, Content Locks and Pricing and
// Catalog are native. N7: Course Content is native.
// N8a (2026-10-19): Push Broadcasts is native for the history and the scheduled list. Its composer is still the
// website panel: the screen asks this host to show it (composerOpen), and Back returns to the native list.
// N9 (2026-10-19): Competitions is native (the deep link /admin?tab=competitions opens it).
// N10 (2026-10-19): Free Resources is native (photo upload with the phone's own file chooser; the Open live page
// button hands a student app address to onLeaveToApp, like the Live Academy pill on the grid).
// N10b (2026-10-19): the Database module is a native READ-ONLY status screen. The website Database and SQL Hub
// is never opened from the app any more (owner order: only the owner works on the database, other admins only
// see its health).
// To add the next native module: add its id to NATIVE_ADMIN_MODULE_IDS in AdminModules.kt and one branch in the when below.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

private const val DEFAULT_SITE_ORIGIN = "https://www.wisdom-tower-academy.live"

private fun siteOrigin(url: String): String {
    val parsed = Uri.parse(url)
    val scheme = parsed.scheme
    val authority = parsed.authority
    return if (scheme != null && authority != null) "$scheme://$authority" else DEFAULT_SITE_ORIGIN
}

@Composable
fun AdminConsoleScreen(
    url: String,
    onClose: () -> Unit,
    onLeaveToApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val startId: String? = remember(url) {
        val parsed = Uri.parse(url)
        val fromTab = adminModuleForTab(parsed.getQueryParameter("tab"))?.id
        if (fromTab != null) fromTab
        else if (parsed.path?.trimEnd('/') == "/admin/payments") "payments"
        else null
    }
    var sectionId by rememberSaveable(url) { mutableStateOf<String?>(startId) }
    var composerOpen by remember(sectionId) { mutableStateOf(false) }
    var counts by remember { mutableStateOf<AdminCounts?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val origin = remember(url) { siteOrigin(url) }

    val currentId = sectionId
    val module = if (currentId == null) null else adminModuleForTab(currentId)
    val isNative = module != null && NATIVE_ADMIN_MODULE_IDS.contains(module.id)

    // On the grid, Back closes the admin. In a native module, Back returns to the grid.
    // A website module (AdminWebSection) has its own Back handler that goes back inside the page first.
    // While the website composer is open inside Push Broadcasts, only its own handler runs.
    BackHandler(enabled = currentId == null) { onClose() }
    BackHandler(enabled = isNative && !composerOpen) { sectionId = null }

    // Fresh counts every time the grid is shown (first open, after returning from a module, after Refresh).
    LaunchedEffect(sectionId, reloadKey) {
        if (sectionId == null) {
            loading = true
            counts = AdminCountsRepository.load()
            loading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(WisdomBackground)) {
        if (module == null) {
            AdminHomeScreen(
                counts = counts,
                loading = loading,
                onRefresh = { reloadKey += 1 },
                onOpenModule = { picked -> sectionId = picked.id },
                onLiveAcademy = { onLeaveToApp("$origin/academy") },
                modifier = Modifier.weight(1f)
            )
        } else {
            AdminSectionBar(module = module, onBack = { sectionId = null })
            when (module.id) {
                "overview" -> OverviewScreen(modifier = Modifier.weight(1f))
                "inquiries" -> InquiriesScreen(modifier = Modifier.weight(1f))
                "payments" -> PaymentsScreen(origin = origin, modifier = Modifier.weight(1f))
                "users" -> UsersScreen(modifier = Modifier.weight(1f))
                "grants" -> AccessGrantsScreen(modifier = Modifier.weight(1f))
                "locks" -> LocksScreen(modifier = Modifier.weight(1f))
                "catalog" -> CatalogScreen(modifier = Modifier.weight(1f))
                "content" -> CourseContentScreen(modifier = Modifier.weight(1f))
                "competitions" -> CompetitionsScreen(modifier = Modifier.weight(1f))
                "free-resources" -> FreeResourcesScreen(
                    origin = origin,
                    onLeaveToApp = onLeaveToApp,
                    modifier = Modifier.weight(1f)
                )
                "database" -> DatabaseStatusScreen(modifier = Modifier.weight(1f))
                "notifications" -> if (composerOpen) {
                    AdminWebSection(
                        url = "$origin/admin?tab=notifications",
                        onClose = { composerOpen = false },
                        onLeaveToApp = onLeaveToApp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    BroadcastsScreen(
                        origin = origin,
                        onWriteOnWebsite = { composerOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> AdminWebSection(
                    url = "$origin/admin?tab=${module.id}",
                    onClose = { sectionId = null },
                    onLeaveToApp = onLeaveToApp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
