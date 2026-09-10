package com.wisdomtower.academy.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object PackageDetail : Screen("package_detail/{packageId}") {
        fun createRoute(packageId: String) = "package_detail/$packageId"
    }
    object SubjectHub : Screen("subject_hub/{packageId}/{subjectId}") {
        fun createRoute(packageId: String, subjectId: String) = "subject_hub/$packageId/$subjectId"
    }
    object NotesViewer : Screen("notes_viewer/{resourceId}") {
        fun createRoute(resourceId: String) = "notes_viewer/$resourceId"
    }
    object PdfViewer : Screen("pdf_viewer/{resourceId}") {
        fun createRoute(resourceId: String) = "pdf_viewer/$resourceId"
    }
    object Flashcards : Screen("flashcards/{resourceId}") {
        fun createRoute(resourceId: String) = "flashcards/$resourceId"
    }
    object Quiz : Screen("quiz/{resourceId}/{isTimed}") {
        fun createRoute(resourceId: String, isTimed: Boolean = false) = "quiz/$resourceId/$isTimed"
    }
    object Downloads : Screen("downloads")
}
