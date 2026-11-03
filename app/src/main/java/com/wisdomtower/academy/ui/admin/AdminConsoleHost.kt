package com.wisdomtower.academy.ui.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import com.wisdomtower.academy.ui.theme.appBackdrop

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

    BackHandler(enabled = currentId == null) { onClose() }
    BackHandler(enabled = isNative && !composerOpen) { sectionId = null }

    LaunchedEffect(sectionId, reloadKey) {
        if (sectionId == null) {
            loading = true
            counts = AdminCountsRepository.load()
            loading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().appBackdrop()) {
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
                "home-banner" -> HomeBannerAdminScreen(modifier = Modifier.weight(1f))
                "inquiries" -> InquiriesScreen(origin = origin, modifier = Modifier.weight(1f))
                "payments" -> PaymentsScreen(origin = origin, modifier = Modifier.weight(1f))
                "users" -> UsersScreen(origin = origin, modifier = Modifier.weight(1f))
                "grants" -> AccessGrantsScreen(modifier = Modifier.weight(1f))
                "locks" -> LocksScreen(modifier = Modifier.weight(1f))
                "catalog" -> CatalogScreen(modifier = Modifier.weight(1f))
                "content" -> CourseContentScreen(modifier = Modifier.weight(1f))
                "game-sets" -> AdminGameSetsScreen(modifier = Modifier.weight(1f))
                "competitions" -> CompetitionsScreen(modifier = Modifier.weight(1f))
                "free-resources" -> FreeResourcesScreen(
                    origin = origin,
                    onLeaveToApp = onLeaveToApp,
                    modifier = Modifier.weight(1f)
                )
                "database" -> DatabaseStatusScreen(modifier = Modifier.weight(1f))
                "exam-integrity" -> ExamIntegrityAdminScreen(modifier = Modifier.weight(1f))
                "device-security" -> DeviceSecurityAdminScreen(modifier = Modifier.weight(1f))
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
