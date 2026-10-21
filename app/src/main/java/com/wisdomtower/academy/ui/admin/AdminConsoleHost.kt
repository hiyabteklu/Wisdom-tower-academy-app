package com.wisdomtower.academy.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wisdomtower.academy.data.admin.AdminCounts
import com.wisdomtower.academy.ui.theme.appBackdrop

/**
 * ADMIN-native entry (docs/ADMIN_NATIVE_PLAN.md).
 * Home grid -> module. Native modules listed in NATIVE_ADMIN_MODULE_IDS; others use AdminWebSection.
 * Games step 7: game-sets opens AdminGameSetsScreen.
 */
@Composable
fun AdminConsoleHost(
    initialTab: String? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sectionId by remember { mutableStateOf(initialTab) }
    val context = LocalContext.current
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val currentId = sectionId
    val module = if (currentId == null) null else adminModuleForTab(currentId)
    val isNative = module != null && NATIVE_ADMIN_MODULE_IDS.contains(module.id)

    BackHandler {
        if (sectionId != null) sectionId = null else onClose()
    }

    LaunchedEffect(sectionId) {
        if (sectionId == null) {
            counts = AdminCounts.load(context)
        }
    }

    Column(modifier = modifier.fillMaxSize().appBackdrop()) {
        if (module == null) {
            AdminHomeScreen(
                counts = counts,
                onOpenModule = { sectionId = it.id },
                onClose = onClose,
                modifier = Modifier.weight(1f)
            )
        } else {
            AdminSectionBar(module = module, onBack = { sectionId = null })
            when (module.id) {
                "overview" -> OverviewScreen(modifier = Modifier.weight(1f))
                "notifications" -> BroadcastsScreen(modifier = Modifier.weight(1f))
                "competitions" -> CompetitionsScreen(modifier = Modifier.weight(1f))
                "users" -> UsersScreen(modifier = Modifier.weight(1f))
                "payments" -> PaymentsScreen(modifier = Modifier.weight(1f))
                "grants" -> AccessGrantsScreen(modifier = Modifier.weight(1f))
                "content" -> CourseContentScreen(modifier = Modifier.weight(1f))
                "game-sets" -> AdminGameSetsScreen(modifier = Modifier.weight(1f))
                "locks" -> LocksScreen(modifier = Modifier.weight(1f))
                "catalog" -> CatalogScreen(modifier = Modifier.weight(1f))
                "free-resources" -> FreeResourcesScreen(modifier = Modifier.weight(1f))
                "inquiries" -> InquiriesScreen(modifier = Modifier.weight(1f))
                "database" -> DatabaseStatusScreen(modifier = Modifier.weight(1f))
                else -> {
                    val origin = "https://www.wisdom-tower-academy.live"
                    AdminWebSection(
                        url = "$origin/admin?tab=${module.id}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
