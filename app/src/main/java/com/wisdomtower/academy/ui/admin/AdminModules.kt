package com.wisdomtower.academy.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector

/** Which live count (if any) a module tile shows as a badge. Same rule as the website: shown only when above zero. */
internal enum class AdminCountKind { NONE, SCHOLARS, PAYMENTS, INQUIRIES }

/**
 * One admin module. ids, labels and aliases are copied from website-reference/src/app/admin/page.tsx
 * (academyTabs, TAB_ALIASES) so deep links such as /admin?tab=competitions keep working.
 * Icons are Material equivalents of the website's lucide icons (BUILD_PITFALLS rule d).
 */
internal data class AdminModule(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val countKind: AdminCountKind = AdminCountKind.NONE,
    val aliases: List<String> = emptyList()
)

internal val ADMIN_MODULES: List<AdminModule> = listOf(
    AdminModule("overview", "Overview", Icons.Filled.Dashboard),
    AdminModule("notifications", "Push Broadcasts", Icons.Filled.Notifications, aliases = listOf("notification")),
    AdminModule("competitions", "Competitions", Icons.Filled.EmojiEvents, aliases = listOf("competition", "contests", "contest")),
    AdminModule("users", "Scholars & Users", Icons.Filled.People, AdminCountKind.SCHOLARS, listOf("user")),
    AdminModule("payments", "Payments", Icons.Filled.CreditCard, AdminCountKind.PAYMENTS, listOf("payment")),
    AdminModule("grants", "Access Grants", Icons.Filled.VpnKey, aliases = listOf("grant")),
    AdminModule("content", "Course Content", Icons.AutoMirrored.Filled.MenuBook, aliases = listOf("contents")),
    AdminModule("locks", "Content Locks", Icons.Filled.Lock, aliases = listOf("lock")),
    AdminModule("catalog", "Pricing & Catalog", Icons.Filled.Inventory2, aliases = listOf("pricing")),
    AdminModule("free-resources", "Free Resources", Icons.Filled.LocalLibrary, aliases = listOf("free-resource", "free")),
    AdminModule("inquiries", "Inquiries", Icons.Filled.Inbox, AdminCountKind.INQUIRIES, listOf("inquiry")),
    AdminModule("database", "Database & SQL Hub", Icons.Filled.Storage, aliases = listOf("db"))
)

/** Same lookup as parseTab() on the website: id or alias, case-insensitive. Null when unknown. */
internal fun adminModuleForTab(raw: String?): AdminModule? {
    val key = raw?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return null
    return ADMIN_MODULES.firstOrNull { it.id == key || it.aliases.contains(key) }
}

/**
 * Modules that already have a NATIVE screen. Every other module still opens its website panel in the WebView
 * (AdminWebSection). Add an id here in the same commit that adds its native screen.
 * N2: inquiries. N3: payments.
 */
internal val NATIVE_ADMIN_MODULE_IDS: Set<String> = setOf("inquiries", "payments")
