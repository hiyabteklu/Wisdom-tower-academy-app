const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const APK_PATH = path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');

const HTML_CONTENT = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Wisdom Tower Academy — Android App Preview</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    body {
      background: #090D16;
      color: #F8FAFC;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      overflow-x: hidden;
      padding: 12px;
    }
    
    .toolbar-top {
      width: 100%;
      max-width: 440px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
      padding: 0 4px;
    }
    .badge-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(0, 229, 255, 0.1);
      border: 1px solid rgba(0, 229, 255, 0.3);
      color: #00E5FF;
      font-size: 0.76rem;
      font-weight: 700;
      padding: 4px 10px;
      border-radius: 9999px;
      letter-spacing: 0.02em;
    }
    .btn-apk-download {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: linear-gradient(135deg, #00E5FF, #0284C7);
      color: #0F172A;
      font-size: 0.8rem;
      font-weight: 700;
      padding: 6px 14px;
      border-radius: 10px;
      text-decoration: none;
      box-shadow: 0 4px 12px rgba(0, 229, 255, 0.3);
      transition: transform 0.15s ease, opacity 0.15s ease;
    }
    .btn-apk-download:active {
      transform: scale(0.96);
    }

    /* Device frame */
    .device-wrapper {
      position: relative;
      width: 100%;
      max-width: 412px;
      height: 840px;
      max-height: calc(100vh - 70px);
      background: #0F172A;
      border-radius: 38px;
      border: 3.5px solid #1E293B;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.7), 0 0 0 1px rgba(0, 229, 255, 0.15);
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    /* Android Status Bar */
    .status-bar {
      height: 32px;
      background: #0F172A;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 18px;
      font-size: 0.75rem;
      font-weight: 600;
      color: #CBD5E1;
      user-select: none;
      z-index: 50;
    }
    .status-icons {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Native App Top Bar */
    .native-top-bar {
      height: 54px;
      background: #0F172A;
      border-bottom: 0.8px solid rgba(255, 255, 255, 0.08);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 12px;
      z-index: 50;
    }
    .bar-btn {
      width: 40px;
      height: 40px;
      border: none;
      background: transparent;
      color: #00E5FF;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      border-radius: 10px;
    }
    .bar-btn:active {
      background: rgba(0, 229, 255, 0.1);
    }
    .brand-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #FFFFFF;
      letter-spacing: -0.01em;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .brand-logo-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #00E5FF;
      box-shadow: 0 0 10px #00E5FF;
    }

    /* Main WebView Area */
    .webview-container {
      flex: 1;
      position: relative;
      background: #0F172A;
      overflow: hidden;
    }
    iframe {
      width: 100%;
      height: 100%;
      border: none;
      background: #0F172A;
    }

    /* Floating Study Timer Widget */
    .timer-widget {
      position: absolute;
      top: 12px;
      right: 12px;
      background: rgba(15, 23, 42, 0.92);
      border: 1px solid rgba(0, 229, 255, 0.4);
      border-radius: 9999px;
      padding: 6px 14px;
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.8rem;
      font-weight: 700;
      color: #00E5FF;
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
      backdrop-filter: blur(8px);
      z-index: 40;
      user-select: none;
    }
    .timer-pulse {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #10B981;
      animation: pulse 1.5s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.85); }
    }

    /* Native Bottom Navigation */
    .bottom-nav {
      height: 60px;
      background: #0F172A;
      border-top: 1px solid rgba(0, 229, 255, 0.15);
      display: flex;
      align-items: center;
      justify-content: space-around;
      padding: 0 4px;
      user-select: none;
      z-index: 50;
    }
    .nav-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 3px;
      height: 100%;
      background: transparent;
      border: none;
      color: #64748B;
      cursor: pointer;
      font-size: 0.68rem;
      font-weight: 600;
      transition: color 0.15s ease, transform 0.1s ease;
    }
    .nav-item.active {
      color: #00E5FF;
    }
    .nav-item:active {
      transform: scale(0.95);
    }
    .nav-item svg {
      width: 20px;
      height: 20px;
      stroke-width: 2.2;
    }

    /* Native Menu Drawer */
    .drawer-backdrop {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.65);
      z-index: 59;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
    }
    .drawer-backdrop.open {
      opacity: 1;
      pointer-events: auto;
    }
    .menu-drawer {
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      width: 250px;
      height: 100%;
      background: #0F172A;
      border-right: 1px solid rgba(0, 229, 255, 0.25);
      box-shadow: 10px 0 30px rgba(0, 0, 0, 0.7);
      display: flex;
      flex-direction: column;
      z-index: 60;
      transform: translateX(-100%);
      transition: transform 0.24s cubic-bezier(0.4, 0, 0.2, 1);
      padding: 16px 14px;
    }
    .menu-drawer.open {
      transform: translateX(0);
    }
    .drawer-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-bottom: 14px;
      border-bottom: 1px solid rgba(0, 229, 255, 0.15);
      margin-bottom: 12px;
    }
    .drawer-title-group {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .drawer-icon-box {
      width: 34px;
      height: 34px;
      border-radius: 9px;
      background: rgba(0, 229, 255, 0.12);
      border: 1px solid rgba(0, 229, 255, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #00E5FF;
    }
    .drawer-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: #FFFFFF;
    }
    .loader-overlay {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.75);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 45;
    }
    .drawer-close {
      width: 32px;
      height: 32px;
      background: transparent;
      border: none;
      color: #94A3B8;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
    }
    .drawer-close:active {
      background: rgba(255, 255, 255, 0.1);
      color: #FFFFFF;
    }
    .drawer-links {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;
      overflow-y: auto;
    }
    .menu-item {
      padding: 10px 12px;
      font-size: 0.88rem;
      font-weight: 600;
      color: #E2E8F0;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 12px;
      border-radius: 12px;
      transition: background 0.15s ease, color 0.15s ease;
    }
    .menu-item svg {
      width: 18px;
      height: 18px;
      stroke-width: 2.2;
      color: #00E5FF;
    }
    .menu-item:hover, .menu-item:active {
      background: rgba(0, 229, 255, 0.1);
      color: #00E5FF;
    }
    .menu-divider {
      height: 1px;
      background: rgba(255, 255, 255, 0.08);
      margin: 8px 4px;
    }
    .menu-item.logout {
      color: #F87171;
    }
    .menu-item.logout svg {
      color: #F87171;
    }
    .menu-item.logout:hover, .menu-item.logout:active {
      background: rgba(239, 68, 68, 0.12);
      color: #FCA5A5;
    }
    .drawer-footer {
      padding-top: 12px;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      font-size: 0.7rem;
      color: #64748B;
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="toolbar-top">
    <div class="badge-chip">
      <span style="width: 6px; height: 6px; border-radius: 50%; background: #00E5FF;"></span>
      Wisdom Tower Academy v1.0
    </div>
    <a href="/download-apk" class="btn-apk-download" title="Download compiled Android Debug APK">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      Download APK (23MB)
    </a>
  </div>

  <div class="device-wrapper">
    <!-- Android Status Bar -->
    <div class="status-bar">
      <span id="clockTime">09:41</span>
      <div class="status-icons">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L12 22l7.03-4.39C20.26 16.07 21 14.12 21 12c0-4.97-4.03-9-9-9zm0 15c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6z"/></svg>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4C7.31 4 3.07 5.9 0 8.98L12 21 24 8.98C20.93 5.9 16.69 4 12 4z"/></svg>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z"/></svg>
      </div>
    </div>

    <!-- Native App Top Bar -->
    <div class="native-top-bar">
      <button class="bar-btn" id="menuToggle" aria-label="Menu">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>

      <div class="brand-title">
        <span class="brand-logo-dot"></span>
        Wisdom Tower Academy
      </div>

      <div style="display: flex; align-items: center; gap: 2px;">
        <button class="bar-btn" id="btnRefresh" title="Refresh">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
        </button>
        <button class="bar-btn" id="btnNotif" title="Notifications">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        </button>
      </div>
    </div>

    <!-- Web Content Area -->
    <div class="webview-container">
      <iframe id="mainWebview" src="https://www.wisdom-tower-academy.live/" allow="autoplay; clipboard-write"></iframe>
      
      <!-- Custom spinning GIF loader overlay for page loading/transitions -->
      <div class="loader-overlay" id="loaderOverlay" style="display: none;">
        <img src="/animation.gif" alt="Loading" style="width: 100px; height: 100px; object-fit: contain;">
      </div>

      <div class="timer-widget" id="studyTimerWidget">
        <div class="timer-pulse"></div>
        <span id="timerText">Study Timer • 25:00</span>
      </div>

      <!-- Backdrop for Drawer -->
      <div class="drawer-backdrop" id="drawerBackdrop"></div>

      <!-- Slide-in Navigation Drawer -->
      <div class="menu-drawer" id="menuDrawer">
        <div class="drawer-header">
          <div class="drawer-title-group">
            <div class="drawer-icon-box">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
            </div>
            <div>
              <div class="drawer-title">Wisdom Tower</div>
            </div>
          </div>
          <button class="drawer-close" id="drawerClose" aria-label="Close menu">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="drawer-links">
          <a href="https://www.wisdom-tower-academy.live/about" class="menu-item" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            About
          </a>
          <a href="https://www.wisdom-tower-academy.live/contact" class="menu-item" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
            Contact us
          </a>
          <a href="https://www.wisdom-tower-academy.live/academy/faq" class="menu-item" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            FAQ
          </a>
          <a href="https://www.wisdom-tower-academy.live/privacy" class="menu-item" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            Privacy
          </a>
          <a href="https://www.wisdom-tower-academy.live/terms" class="menu-item" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
            Terms
          </a>

          <div class="menu-divider"></div>

          <a href="https://www.wisdom-tower-academy.live/logout" class="menu-item logout" target="mainWebview">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            Sign out
          </a>
        </div>

        <div class="drawer-footer">
          Wisdom Tower Academy • v1.0
        </div>
      </div>
    </div>

    <!-- Native Bottom Navigation Bar -->
    <div class="bottom-nav">
      <button class="nav-item active" data-url="https://www.wisdom-tower-academy.live/">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        <span>Home</span>
      </button>

      <button class="nav-item" data-url="https://www.wisdom-tower-academy.live/learning">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        <span>Learning</span>
      </button>

      <button class="nav-item" data-url="https://www.wisdom-tower-academy.live/packages">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
        <span>Packages</span>
      </button>

      <button class="nav-item" data-url="https://www.wisdom-tower-academy.live/account">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        <span>Account</span>
      </button>

      <button class="nav-item" data-url="https://www.wisdom-tower-academy.live/settings">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        <span>Settings</span>
      </button>
    </div>
  </div>

  <script>
    function updateClock() {
      const now = new Date();
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      document.getElementById('clockTime').textContent = h + ':' + m;
    }
    updateClock();
    setInterval(updateClock, 1000);

    const wv = document.getElementById('mainWebview');
    const navItems = document.querySelectorAll('.nav-item');
    const menuToggle = document.getElementById('menuToggle');
    const menuDrawer = document.getElementById('menuDrawer');
    const drawerBackdrop = document.getElementById('drawerBackdrop');
    const drawerClose = document.getElementById('drawerClose');
    const btnRefresh = document.getElementById('btnRefresh');
    const btnNotif = document.getElementById('btnNotif');

    function openDrawer() {
      menuDrawer.classList.add('open');
      drawerBackdrop.classList.add('open');
    }
    function closeDrawer() {
      menuDrawer.classList.remove('open');
      drawerBackdrop.classList.remove('open');
    }

    menuToggle.addEventListener('click', function(e) {
      e.stopPropagation();
      openDrawer();
    });
    drawerClose.addEventListener('click', closeDrawer);
    drawerBackdrop.addEventListener('click', closeDrawer);

    document.querySelectorAll('.menu-drawer .menu-item').forEach(function(item) {
      item.addEventListener('click', function() {
        closeDrawer();
      });
    });

    const loaderOverlay = document.getElementById('loaderOverlay');
    wv.addEventListener('load', function() {
      if (loaderOverlay) loaderOverlay.style.display = 'none';
    });

    btnRefresh.addEventListener('click', function() {
      if (loaderOverlay) loaderOverlay.style.display = 'flex';
      try { wv.contentWindow.location.reload(); } catch(_) { wv.src = wv.src; }
      setTimeout(function() { if (loaderOverlay) loaderOverlay.style.display = 'none'; }, 2000);
    });

    btnNotif.addEventListener('click', function() {
      if (loaderOverlay) loaderOverlay.style.display = 'flex';
      wv.src = 'https://www.wisdom-tower-academy.live/notifications';
    });

    navItems.forEach(function(item) {
      item.addEventListener('click', function() {
        navItems.forEach(function(i) { i.classList.remove('active'); });
        item.classList.add('active');
        const url = item.getAttribute('data-url');
        if (url) {
          if (loaderOverlay) loaderOverlay.style.display = 'flex';
          wv.src = url;
          setTimeout(function() { if (loaderOverlay) loaderOverlay.style.display = 'none'; }, 2000);
        }
      });
    });
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  try {
    const host = req.headers.host || 'localhost:3000';
    let pathname = '/';
    try {
      const urlObj = new URL(req.url, 'http://' + host);
      pathname = urlObj.pathname;
    } catch {
      pathname = req.url.split('?')[0] || '/';
    }

    if (pathname === '/animation.gif') {
      const gifPath = path.join(__dirname, 'animation.gif');
      if (fs.existsSync(gifPath)) {
        res.writeHead(200, { 'Content-Type': 'image/gif' });
        fs.createReadStream(gifPath).pipe(res);
        return;
      }
    }

    if (pathname === '/download-apk' || pathname === '/app-debug.apk') {
      if (fs.existsSync(APK_PATH)) {
        const stat = fs.statSync(APK_PATH);
        res.writeHead(200, {
          'Content-Type': 'application/vnd.android.package-archive',
          'Content-Disposition': 'attachment; filename="Wisdom-Tower-Academy-debug.apk"',
          'Content-Length': stat.size
        });
        fs.createReadStream(APK_PATH).pipe(res);
        return;
      } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('APK not found. Please compile the app first.');
        return;
      }
    }

    if (pathname === '/api/health' || pathname === '/health') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'healthy', timestamp: new Date().toISOString() }));
      return;
    }

    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache'
    });
    res.end(HTML_CONTENT);
  } catch (err) {
    console.error('Request handling error:', err);
    if (!res.headersSent) {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('Internal Server Error');
    }
  }
});

server.on('error', (err) => {
  console.error('Server error:', err);
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Dev server listening on port ' + PORT);
});
