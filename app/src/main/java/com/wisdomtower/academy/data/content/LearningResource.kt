package com.wisdomtower.academy.data.content

/**
 * One row of `public.learning_resources` (same fields as the website's `LearningResource` in
 * `website-reference/src/lib/content.ts`). Real shapes: `docs/CONTENT_SHAPES.md`.
 *
 * [contentType]: `pdf`, `markdown`, `video_url`, `quiz`, `exam`, `flashcard_deck`.
 * [hub]: `books`, `short-notes`, `videos`, `flashcards`, `question-banks`, `exams`, `life-savers`
 * (the old hub name `references` is already mapped to `short-notes`).
 * [bodyMd]: the note text for `markdown` rows; null in light lists.
 * [metaJson]: the `meta` JSON text. For flashcards `{"cards":[{"front","back"}]}`, for quiz/exam
 * `{"questions":[{"prompt","choices","correct","solution"}], "durationMin"?}`. "{}" in light lists.
 * [storagePath]: for books `appwrite:<FILE_ID>`; use [LearningContentRepository.appwriteViewUrl].
 * [itemCount]: number of flashcards / questions, read from the database column `item_count` (security slice S2,
 * `docs/SECURITY_PLAN_CONTENT_ACCESS.md`): lists show counts without downloading the heavy `meta` of every row.
 * Null when the row has none or an old saved list has no such column.
 */
data class LearningResource(
    val id: String,
    val packageId: String,
    val scopePath: String,
    val hub: String,
    val title: String,
    val chapter: Int?,
    val sortOrder: Int,
    val contentType: String,
    val storagePath: String?,
    val bodyMd: String?,
    val metaJson: String,
    val itemCount: Int? = null,
    val sizeBytes: Long? = null
)

/** Result of loading a list of resources. [fromCache] is true when the phone could not reach the server. */
sealed class ContentListResult {
    data class Success(val items: List<LearningResource>, val fromCache: Boolean) : ContentListResult()

    /** [message] is plain language, safe to show to the student. */
    data class Failure(val message: String) : ContentListResult()
}

/** Result of loading one resource with its full body / meta. */
sealed class ContentItemResult {
    data class Found(val item: LearningResource, val fromCache: Boolean) : ContentItemResult()

    object NotFound : ContentItemResult()

    data class Failure(val message: String) : ContentItemResult()
}
