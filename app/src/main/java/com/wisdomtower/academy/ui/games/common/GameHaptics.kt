package com.wisdomtower.academy.ui.games.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Haptics via performHapticFeedback only (no VIBRATE permission, no Vibrator service).
 */
object GameHaptics {

    fun tap(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } catch (_: Exception) {
        }
    }

    fun correct(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } catch (_: Exception) {
        }
    }

    fun wrong(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            val code = if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(code)
        } catch (_: Exception) {
        }
    }

    fun heartLost(view: View, enabled: Boolean) {
        if (!enabled) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {
        }
    }
}
