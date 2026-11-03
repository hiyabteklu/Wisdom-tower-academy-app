package com.wisdomtower.academy.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Requests from anywhere in the app (drawer row, website links caught in MainActivity) to open a NATIVE tool on the
 * Learning tab. `LearningScreen` watches the flags, opens the native screen and resets the flag.
 * Written for patches P7 (tutor), P8 (calculator), P9 (timer), P10 (planner), P11 (notebook, targets) and P12 (your
 * status), owner problems 1 and 2, 2026-10-12. Compose state, so a change is seen by the screen at once.
 *
 * T1 (2026-11-02, docs/READER_EXAM_FLASHCARD_FIX_PLAN.md): [requestTutorWithContext] opens the global tutor overlay with the
 * text of the chapter the student is reading (set by the "Ask AI tutor" button of the notes reader). The context is kept in
 * [tutorContext] (a plain field, NOT Compose state, because `NativeTutorScreen` reads it once while it is being composed)
 * and handed over with [takeTutorContext], which also clears it, so a tutor opened later from the drawer never inherits an
 * old chapter.
 */
object NativeToolRequests {
    var pendingTutor by mutableStateOf(false)
    var pendingCalculator by mutableStateOf(false)
    var pendingTimer by mutableStateOf(false)
    var pendingPlanner by mutableStateOf(false)
    var pendingNotebook by mutableStateOf(false)
    var pendingTargets by mutableStateOf(false)
    var pendingStatus by mutableStateOf(false)

    @Volatile
    private var tutorContext: String? = null

    fun requestTutor() {
        pendingTutor = true
    }

    /** Opens the tutor with [context] (the chapter text and a short instruction) for the NEXT tutor screen only. */
    fun requestTutorWithContext(context: String) {
        tutorContext = context
        pendingTutor = true
    }

    /** Returns the chapter context set by [requestTutorWithContext] once, then forgets it. Null when there is none. */
    fun takeTutorContext(): String? {
        val c = tutorContext
        tutorContext = null
        return c
    }

    fun requestCalculator() {
        pendingCalculator = true
    }

    fun requestTimer() {
        pendingTimer = true
    }

    fun requestPlanner() {
        pendingPlanner = true
    }

    fun requestNotebook() {
        pendingNotebook = true
    }

    fun requestTargets() {
        pendingTargets = true
    }

    fun requestStatus() {
        pendingStatus = true
    }
}
