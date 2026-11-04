import type { ReactNode } from "react";

/*
 * Admin-only layout (slice A2 of docs/ADMIN_MOBILE_PLAN.md, extended by slice U0 of docs/ADMIN_UI_V2_PLAN.md).
 * Wraps /admin and /admin/payments only; student pages are untouched.
 * It is a SAFETY NET for sideways overflow and WebView gesture/tap quirks,
 * not the fix: the real overflow causes were removed panel by panel (A5 to A16).
 * - overflow-x: clip (not hidden): clips sideways overflow WITHOUT making a scroll
 *   container, so position: sticky headers keep working.
 * - overscroll-behavior-y: contain on the page: no rubber-band chaining into the app.
 * - no tap-highlight flash, long words/ids wrap instead of widening the page.
 * - touch-action: pan-y was added after A16 (2026-10-15) and REMOVED on 2026-10-16 (plan item F12, A/B step):
 *   the owner still saw scroll freezes after the U0 fixes, so the cheapest suspect goes first.
 *   If scrolling still freezes, the next suspects are in docs/ADMIN_UI_V2_PLAN.md (F10, F11).
 * U0 (2026-10-16, owner: "the app scroll freezes", "things touching the top nav"):
 * - .site-atmosphere (the student site's fixed background with three blur(90px) glows) is hidden here.
 *   The admin root is opaque, so it was fully covered and cost compositing for nothing (hypothesis: part of the freeze).
 * - scroll-behavior: auto (the site sets smooth on html; smooth programmatic scrolling can fight touch fling in a WebView).
 * - .wta-admin-fade: a 14 px fixed fade under the app top bar, so rows dissolve at the edge instead of being cut flush.
 */
const GUARD_CSS = `
html { overscroll-behavior-y: contain; scroll-behavior: auto !important; -webkit-tap-highlight-color: transparent; }
.site-atmosphere { display: none !important; }
.wta-admin-root { max-width: 100vw; overflow-x: clip; overflow-wrap: anywhere; -webkit-tap-highlight-color: transparent; }
.wta-admin-fade { position: fixed; top: 0; left: 0; right: 0; height: 14px; z-index: 40; pointer-events: none; background: linear-gradient(to bottom, rgb(var(--wt-dark-rgb)) 0%, rgb(var(--wt-dark-rgb) / 0) 100%); }
`;

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="wta-admin-root">
      <style>{GUARD_CSS}</style>
      <div className="wta-admin-fade" aria-hidden="true" />
      {children}
    </div>
  );
}
