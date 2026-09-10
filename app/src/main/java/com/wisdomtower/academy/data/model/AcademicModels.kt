package com.wisdomtower.academy.data.model

enum class ResourceType(val label: String, val iconName: String) {
    BOOK_PDF("Reference Book (PDF)", "picture_as_pdf"),
    SHORT_NOTE("High-Yield Note", "menu_book"),
    FLASHCARDS("Smart Flashcards", "style"),
    QUESTION_BANK("Practice Q-Bank", "help_center"),
    MOCK_EXAM("Timed Mock Exam", "timer"),
    VIDEO("Video Lecture", "play_circle");

    companion object {
        fun fromHubOrType(hub: String, contentType: String): ResourceType {
            return when {
                hub == "books" || contentType == "pdf" -> BOOK_PDF
                hub == "short-notes" || contentType == "markdown" -> SHORT_NOTE
                hub == "flashcards" || contentType == "flashcard_deck" -> FLASHCARDS
                hub == "question-banks" || contentType == "quiz" -> QUESTION_BANK
                hub == "exams" || contentType == "exam" -> MOCK_EXAM
                hub == "videos" || contentType == "video_url" -> VIDEO
                else -> SHORT_NOTE
            }
        }
    }
}

data class PackageItem(
    val id: String,
    val title: String,
    val tag: String,
    val description: String,
    val isFree: Boolean,
    val priceEtb: Int,
    val isOwned: Boolean = false,
    val subjectsCount: Int = 0,
    val totalResourcesCount: Int = 0,
    val colorHex: Long = 0xFF00BCD4,
    val shortName: String = "",
    val groupKey: String = "",
    val includes: List<String> = emptyList(),
    val imageUrl: String? = null
)

/**
 * Derived dynamically from learning_resources grouped by scope_path
 */
data class SubjectItem(
    val id: String, // scope_path (e.g. "physics", "math-natural", "mechanics")
    val packageId: String,
    val name: String, // Formatted title
    val code: String = "",
    val description: String = "",
    val iconName: String = "menu_book",
    val booksCount: Int = 0,
    val notesCount: Int = 0,
    val flashcardsCount: Int = 0,
    val qbankCount: Int = 0,
    val examCount: Int = 0
)

data class FlashcardItem(
    val id: String,
    val front: String,
    val back: String,
    val hint: String = "",
    val category: String = ""
)

data class QuizQuestionItem(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * Direct representation of table learning_resources
 */
data class LearningResource(
    val id: String,
    val packageId: String,
    val scopePath: String,
    val hub: String,
    val title: String,
    val chapter: String? = null,
    val sortOrder: Int = 0,
    val contentType: String,
    val storagePath: String? = null,
    val bodyMd: String? = null,
    val metaJson: String? = null,
    val published: Boolean = true,
    val signedUrl: String? = null
)

data class ResourceItem(
    val id: String,
    val packageId: String,
    val subjectId: String, // scopePath
    val subjectName: String,
    val title: String,
    val resourceType: ResourceType,
    val summary: String = "",
    val readingTimeMinutes: Int = 15,
    val fileSizeBytes: Long = 0,
    val remoteUrl: String? = null,
    val storagePath: String? = null,
    val markdownContent: String? = null,
    val flashcards: List<FlashcardItem> = emptyList(),
    val quizQuestions: List<QuizQuestionItem> = emptyList(),
    val isDownloaded: Boolean = false,
    val chapter: String? = null,
    val hub: String = ""
)

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val isGuest: Boolean = false,
    val enrolledPackageIds: Set<String> = emptySet()
)

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val profile: UserProfile? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)
