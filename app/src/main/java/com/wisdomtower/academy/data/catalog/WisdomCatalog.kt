package com.wisdomtower.academy.data.catalog

/**
 * Utility helpers for academic formatting.
 * All synthetic packages, subjects, and curriculum data have been removed.
 * Data is fetched exclusively from Supabase tables (catalog_items and learning_resources).
 */
object WisdomCatalog {

    /**
     * Converts a scope_path like "math-natural" or "intro_to_computing" into a clean display title
     */
    fun formatScopeTitle(scopePath: String): String {
        if (scopePath.isBlank()) return "General Studies"
        return scopePath
            .split('/', '-', '_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
