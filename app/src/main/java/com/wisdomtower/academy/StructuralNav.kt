package com.wisdomtower.academy

/**
 * JS bridges for structural back and hard refresh.
 * Website exposes window.__wtaStructuralBack / window.__wtaHardRefresh when available.
 */
object StructuralNav {

    /**
     * Structural back JS bridge.
     * Structural parent tree is defined on the website; app only invokes window.__wtaStructuralBack().
     * If the website handles structural back -> "ok"
     * Else -> "root" / "not_ok"
     */
    const val STRUCTURAL_BACK_JS = """
(function(){
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
}
