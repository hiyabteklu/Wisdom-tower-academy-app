package com.wisdomtower.academy

import android.net.Uri

/**
 * JS bridges for structural back and hard refresh,
 * plus URL-level structural hierarchy navigation.
 */
object StructuralNav {

    const val SITE_HOME = "https://www.wisdom-tower-academy.live/"
    const val SITE_LEARNING = "https://www.wisdom-tower-academy.live/learning"
    const val SITE_PACKAGES = "https://www.wisdom-tower-academy.live/packages"
    const val SITE_ACCOUNT = "https://www.wisdom-tower-academy.live/account"
    const val SITE_SETTINGS = "https://www.wisdom-tower-academy.live/settings"

    /**
     * Structural back JS bridge.
     * Structural parent tree is defined on the website; app invokes window.__wtaInPageBack() or window.__wtaStructuralBack().
     * If the website handles structural or in-page back -> "ok"
     * Else -> "root" / "not_ok"
     */
    const val STRUCTURAL_BACK_JS = """
(function(){
  try {
    if (typeof window.__wtaInPageBack === 'function') {
      var r = window.__wtaInPageBack();
      if (r === true || r === 'ok') return 'ok';
    }
  } catch (e) {}
  try {
    if (typeof window.__wtaStructuralBack === 'function') {
      var r = window.__wtaStructuralBack();
      if (r === true || r === 'ok') return 'ok';
      return 'root';
    }
  } catch (e) {}
  return 'not_ok';
})();
"""

    /** Hard refresh without wiping OfflineVault. */
    const val HARD_REFRESH_JS = """
(function(){
  try {
    window.dispatchEvent(new CustomEvent('wta-refresh', {
      detail: { source: 'app-bar', hard: true, at: Date.now() }
    }));
  } catch (e) {}
  try {
    if (typeof window.__wtaHardRefresh === 'function') {
      window.__wtaHardRefresh();
      return 'ok';
    }
  } catch (e) {}
  return 'reload';
})();
"""

    /**
     * Returns the direct structural parent URL for nested routes (e.g. learning chapters, admin items, package checkouts).
     * If the URL is already at a section root (e.g. /learning, /packages, /account, /settings, /) or external, returns null.
     */
    fun getParentStructuralUrl(url: String?): String? {
        if (url.isNullOrBlank() || url.startsWith("file://")) return null
        try {
            val uri = Uri.parse(url)
            val path = uri.path?.trimEnd('/') ?: ""
            if (path.isEmpty() || path == "/") return null

            // If the URL has tool query, the parent is the base path without the tool query
            if (!uri.query.isNullOrBlank() && uri.query!!.contains("tool=")) {
                return uri.buildUpon().clearQuery().query(null).fragment(null).build().toString()
            }

            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.size > 1) {
                // Drop the deepest segment to step up one level in the hierarchy
                val parentPath = "/" + segments.dropLast(1).joinToString("/")
                return uri.buildUpon().path(parentPath).query(null).fragment(null).build().toString()
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Maps a tab index (0..4) to its corresponding canonical root URL.
     */
    fun getSectionRootForTab(tabIndex: Int): String {
        return when (tabIndex) {
            1 -> SITE_LEARNING
            2 -> SITE_PACKAGES
            3 -> SITE_ACCOUNT
            4 -> SITE_SETTINGS
            else -> SITE_HOME
        }
    }

    /**
     * Returns true if the URL is the root of the Home section.
     */
    fun isHomeRoot(url: String?): Boolean {
        if (url.isNullOrBlank() || url.startsWith("file://")) return true
        val clean = url.trim().removeSuffix("/")
        return clean == "https://www.wisdom-tower-academy.live" ||
               clean == "https://wisdom-tower-academy.live" ||
               clean == "https://www.wisdomtower.tech" ||
               clean == "https://wisdomtower.tech" ||
               clean.endsWith("/home")
    }
}
