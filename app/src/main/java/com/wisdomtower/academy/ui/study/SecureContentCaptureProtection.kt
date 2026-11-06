package com.wisdomtower.academy.ui.study

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Applies FLAG_SECURE only while a protected student reader/exam is composed.
 * Admin screens remain capturable; leaderboard exports are rendered from data, not screenshots.
 *
 * OWNER ORDER 2026-11-06 (marketing screenshots and screen recordings): FLAG_SECURE is switched OFF everywhere.
 * The two `window.addFlags(...FLAG_SECURE)` calls below are commented out. The `clearFlags` calls stay on purpose,
 * so a window can never keep the flag. To turn protection back on, remove the `//` in front of both addFlags lines
 * (marked "DISABLED 2026-11-06") and ship a new build.
 */
@Composable
fun SecureContentCaptureProtection() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, lifecycleOwner) {
        if (activity == null) {
            onDispose { }
        } else {
            val window = activity.window
            SecureWindowFlag.acquire(window)
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) SecureWindowFlag.refresh(window)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                SecureWindowFlag.release(window)
            }
        }
    }
}

private object SecureWindowFlag {
    private val lock = Any()
    private var protectedWindow: Window? = null
    private var activeReaders = 0

    fun acquire(window: Window) = synchronized(lock) {
        if (protectedWindow !== window) {
            protectedWindow?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            protectedWindow = window
            activeReaders = 0
        }
        activeReaders += 1
        // DISABLED 2026-11-06 (owner order, marketing captures):
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun refresh(window: Window) = synchronized(lock) {
        if (protectedWindow === window && activeReaders > 0) {
            // DISABLED 2026-11-06 (owner order, marketing captures):
            // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun release(window: Window) = synchronized(lock) {
        if (protectedWindow === window && activeReaders > 0) {
            activeReaders -= 1
            if (activeReaders == 0) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                protectedWindow = null
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
