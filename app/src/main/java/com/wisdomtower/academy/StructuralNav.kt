package com.wisdomtower.academy

/**
 * JS bridges for structural back and hard refresh.
 * Website exposes window.__wtaStructuralBack / window.__wtaHardRefresh when available.
 */
object StructuralNav {

    /**
     * Fallback back mechanism when webView.canGoBack() is false.
     * 1. Calls window.__wtaStructuralBack if present and returns true/ok -> "ok"
     * 2. Else if history.length > 1 -> history.back() -> "ok"
     * 3. Else -> "not_ok"
     */
    const val STRUCTURAL_BACK_JS = """
(function(){
  try {
    if (typeof window.__wtaStructuralBack === 'function') {
      var r = window.__wtaStructuralBack();
      if (r === true || r === 'ok') return 'ok';
    }
  } catch (e) {}
  try {
    if (window.history && window.history.length > 1) {
      window.history.back();
      return 'ok';
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
}
