package com.wisdomtower.academy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug

/**
 * Slice S3 (docs/SECURITY_PLAN_CONTENT_ACCESS.md): a deliberately SIMPLE check, release builds only.
 *
 * The real Wisdom Tower release is never "debuggable" and nobody attaches a debugger to it. If either is true,
 * the app was opened up or repackaged to be inspected, so [isTampered] says so and the Application closes the app.
 *
 * Honest limit (also written in the plan): this only stops casual tampering. Someone with enough skill can
 * remove this check from their own copy of the app. That is why the real protection is on the server: locked
 * content is never sent to a student who is not allowed (slices S1, S2, S4).
 *
 * Why there is NO check of the app's signing certificate: apps installed from Google Play are re-signed with
 * Google's own key, so a fixed certificate in the code would lock out every Play Store student.
 */
object IntegrityGuard {
    fun isTampered(context: Context): Boolean {
        if (BuildConfig.DEBUG) return false
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return debuggable || Debug.isDebuggerConnected()
    }
}
