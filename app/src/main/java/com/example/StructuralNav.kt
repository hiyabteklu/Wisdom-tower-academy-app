package com.example

/**
 * JS bridges for structural back and hard refresh.
 * Website exposes window.__wtaStructuralBack / window.__wtaHardRefresh when available.
 */
object StructuralNav {

    /** One site layer up (not browser history). Returns "ok" | "root" | "fail". */
    const val STRUCTURAL_BACK_JS = """
(function(){
  try {
    if (typeof window.__wtaStructuralBack === 'function') {
      var r = window.__wtaStructuralBack();
      return r === true ? 'ok' : 'root';
    }
  } catch (e) {}
  try {
    var path = location.pathname || '/';
    if (path.length > 1 && path.endsWith('/')) path = path.slice(0, -1);
    var parts = path.split('/').filter(Boolean);
    if (parts.length === 0) return 'root';
    parts.pop();
    var parent = '/' + parts.join('/');
    if (!parent || parent === path) return 'root';
    var roots = {
      '/academy': '/',
      '/packages': '/',
      '/learning': '/',
      '/login': '/',
      '/cart': '/packages',
      '/checkout': '/cart'
    };
    if (roots[path]) parent = roots[path];
    location.assign(parent);
    return 'ok';
  } catch (e) { return 'fail'; }
})();
"""

    /** Hard refresh without wiping OfflineVault. */
    const val HARD_REFRESH_JS = """
(function(){
  try {
    if (typeof window.__wtaHardRefresh === 'function') {
      window.__wtaHardRefresh();
      return 'ok';
    }
  } catch (e) {}
  try {
    window.dispatchEvent(new CustomEvent('wta-refresh', {
      detail: { source: 'app-hard', hard: true, at: Date.now() }
    }));
  } catch (e) {}
  return 'reload';
})();
"""
}
