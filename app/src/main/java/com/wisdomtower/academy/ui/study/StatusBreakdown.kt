package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.data.content.CatalogItem
import com.wisdomtower.academy.data.content.ExamAttemptRow
import com.wisdomtower.academy.data.content.ProgressRow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * My Status V2 (S1): pure functions, NO UI, NO network. Real numbers only (owner rule 2026-10-11, G4): a value that
 * cannot be known is null (the UI shows a dash), never 0 and never an estimate. Definitions: docs/MY_STATUS_V2_PLAN.md
 * section 4.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
data class BreakdownTotals(
    val itemsTotal: Int,
    /** items with a progress row that shows any activity (progress, time, or an opened date) */
    val itemsOpened: Int,
    /** mean `progress_pct` over ALL published items (unopened = 0), 0..100; null when there are no items */
    val completionPct: Double?,
    val studySeconds: Long,
    val focusSeconds: Long,
    /** focus share of study time, 0..100; null when no study time */
    val focusPct: Int?,
    /** hubs books + short-notes */
    val readingSeconds: Long,
    /** hub videos */
    val watchSeconds: Long,
    val quizAttempted: Int,
    val quizCorrect: Int,
    val accuracyPct: Int?,
    val flashSeen: Int,
    val flashKnow: Int,
    val flashKnownPct: Int?,
    /** newest `last_opened_at` (ISO text) among the items, null when none */
    val lastActiveIso: String?
)

data class HubBreakdown(
    val hub: String,
    val itemsTotal: Int,
    val itemsOpened: Int,
    val studySeconds: Long,
    val quizAttempted: Int,
    val quizCorrect: Int,
    val accuracyPct: Int?
)

data class ChapterBreakdown(
    /** null = items with no chapter (show as "General") */
    val chapter: Int?,
    val totals: BreakdownTotals,
    val hubs: List<HubBreakdown>
)

data class ExamAttemptBreakdown(
    val resourceId: String,
    val title: String?,
    val startedAtIso: String?,
    val score: Double?,
    val total: Int?,
    /** score / total in percent; null unless both are known and score is within 0..total */
    val percent: Int?,
    val durationSeconds: Long?,
    /** percentage points against the previous attempt of the SAME exam; null when not comparable */
    val deltaVsPrevious: Int?
)

data class ExamBreakdown(
    /** attempts that have a submitted time */
    val attemptsTaken: Int,
    val bestPct: Int?,
    val latestPct: Int?,
    val averagePct: Int?,
    /** newest first */
    val attempts: List<ExamAttemptBreakdown>
)

data class SubjectBreakdown(
    val subjectKey: String,
    val totals: BreakdownTotals,
    /** numbered chapters ascending, the "no chapter" group last */
    val chapters: List<ChapterBreakdown>,
    val hubs: List<HubBreakdown>,
    val exams: ExamBreakdown,
    /** last PDF page of the most recently opened book, null when unknown */
    val lastPdfPage: Int?
)

object StatusBreakdown {
    private val READING_HUBS = setOf("books", "short-notes")
    private const val WATCH_HUB = "videos"
    private const val EXAMS_HUB = "exams"
    private val HUB_ORDER = listOf(
        "books", "short-notes", "videos", "flashcards", "question-banks", "exams", "life-savers"
    )

    fun progressMap(rows: List<ProgressRow>): Map<String, ProgressRow> = rows.associateBy { it.resourceId }

    /** Every subject key that has at least one published item, sorted. */
    fun subjectKeys(catalog: List<CatalogItem>): List<String> =
        catalog.mapNotNull { it.subjectKey }.distinct().sorted()

    private fun pct(part: Long, whole: Long): Int? =
        if (whole <= 0L) null else (part * 100.0 / whole).roundToInt().coerceIn(0, 100)

    private fun pct(part: Int, whole: Int): Int? =
        if (whole <= 0) null else (part * 100.0 / whole).roundToInt().coerceIn(0, 100)

    private fun isOpened(r: ProgressRow): Boolean =
        r.progressPct > 0.0 || r.totalSeconds > 0L || r.lastOpenedAt != null

    /** Adds up the given items against the student's progress rows. */
    fun totals(items: List<CatalogItem>, progressById: Map<String, ProgressRow>): BreakdownTotals {
        var opened = 0
        var pctSum = 0.0
        var study = 0L
        var focus = 0L
        var reading = 0L
        var watch = 0L
        var qa = 0
        var qc = 0
        var fs = 0
        var fk = 0
        var last: String? = null
        for (item in items) {
            val r = progressById[item.id] ?: continue
            if (isOpened(r)) opened++
            pctSum += r.progressPct.coerceIn(0.0, 100.0)
            study += r.totalSeconds
            focus += r.focusSeconds
            if (item.hub in READING_HUBS) reading += r.totalSeconds
            if (item.hub == WATCH_HUB) watch += r.totalSeconds
            qa += r.quizAttempted
            qc += r.quizCorrect
            fs += r.flashcardsSeen
            fk += r.flashcardsKnow
            val at = r.lastOpenedAt
            if (at != null && (last == null || at > last)) last = at
        }
        return BreakdownTotals(
            itemsTotal = items.size,
            itemsOpened = opened,
            completionPct = if (items.isEmpty()) null else (pctSum / items.size).coerceIn(0.0, 100.0),
            studySeconds = study,
            focusSeconds = focus,
            focusPct = pct(focus, study),
            readingSeconds = reading,
            watchSeconds = watch,
            quizAttempted = qa,
            quizCorrect = qc,
            accuracyPct = pct(qc, qa),
            flashSeen = fs,
            flashKnow = fk,
            flashKnownPct = pct(fk, fs),
            lastActiveIso = last
        )
    }

    private fun hubBreakdowns(items: List<CatalogItem>, progressById: Map<String, ProgressRow>): List<HubBreakdown> {
        val byHub = items.groupBy { it.hub }
        val ordered = HUB_ORDER.filter { byHub.containsKey(it) } + byHub.keys.filter { it !in HUB_ORDER }.sorted()
        return ordered.map { hub ->
            val t = totals(byHub.getValue(hub), progressById)
            HubBreakdown(hub, t.itemsTotal, t.itemsOpened, t.studySeconds, t.quizAttempted, t.quizCorrect, t.accuracyPct)
        }
    }

    /** One chapter (or the "no chapter" group when `chapter` is null) of the given subject items. */
    fun chapterStatus(
        subjectItems: List<CatalogItem>,
        chapter: Int?,
        progressById: Map<String, ProgressRow>
    ): ChapterBreakdown {
        val items = subjectItems.filter { it.chapter == chapter }
        return ChapterBreakdown(chapter, totals(items, progressById), hubBreakdowns(items, progressById))
    }

    /** Null when the subject has no published items. */
    fun subjectStatus(
        subjectKey: String,
        catalog: List<CatalogItem>,
        progressById: Map<String, ProgressRow>,
        attempts: List<ExamAttemptRow>
    ): SubjectBreakdown? {
        val items = catalog.filter { it.subjectKey == subjectKey }
        if (items.isEmpty()) return null
        val numbered = items.mapNotNull { it.chapter }.distinct().sorted()
        val chapters = ArrayList<ChapterBreakdown>()
        for (c in numbered) chapters.add(chapterStatus(items, c, progressById))
        if (items.any { it.chapter == null }) chapters.add(chapterStatus(items, null, progressById))

        // last page of the most recently opened book that has one
        val lastPage = items
            .filter { it.hub == "books" }
            .mapNotNull { progressById[it.id] }
            .filter { it.lastPage != null && it.lastOpenedAt != null }
            .maxByOrNull { it.lastOpenedAt.orEmpty() }
            ?.lastPage

        return SubjectBreakdown(
            subjectKey = subjectKey,
            totals = totals(items, progressById),
            chapters = chapters,
            hubs = hubBreakdowns(items, progressById),
            exams = examStatus(subjectKey, catalog, attempts),
            lastPdfPage = lastPage
        )
    }

    private fun attemptPercent(a: ExamAttemptRow): Int? {
        val score = a.score ?: return null
        val total = a.total ?: return null
        if (total <= 0 || score < 0.0 || score > total.toDouble()) return null
        return (score * 100.0 / total).roundToInt().coerceIn(0, 100)
    }

    private fun isoToEpochSeconds(iso: String?): Long? {
        if (iso == null || iso.length < 19) return null
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            f.timeZone = TimeZone.getTimeZone("UTC")
            f.isLenient = false
            val d = f.parse(iso.substring(0, 19).replace(' ', 'T')) ?: return null
            d.time / 1000L
        } catch (e: Exception) {
            null
        }
    }

    private fun durationSeconds(a: ExamAttemptRow): Long? {
        val s = isoToEpochSeconds(a.startedAt) ?: return null
        val e = isoToEpochSeconds(a.submittedAt) ?: return null
        return if (e >= s) e - s else null
    }

    /** The student's attempts at exams of this subject (exam titles come from the catalog). */
    fun examStatus(
        subjectKey: String,
        catalog: List<CatalogItem>,
        attempts: List<ExamAttemptRow>
    ): ExamBreakdown {
        val exams = catalog.filter { it.subjectKey == subjectKey && it.hub == EXAMS_HUB }.associateBy { it.id }
        val mine = attempts.filter { exams.containsKey(it.resourceId) }.sortedBy { it.startedAt.orEmpty() }
        val lastPctByExam = HashMap<String, Int>()
        val list = ArrayList<ExamAttemptBreakdown>()
        for (a in mine) {
            val p = attemptPercent(a)
            val prev = lastPctByExam[a.resourceId]
            val delta = if (p != null && prev != null) p - prev else null
            if (p != null) lastPctByExam[a.resourceId] = p
            list.add(
                ExamAttemptBreakdown(
                    resourceId = a.resourceId,
                    title = exams[a.resourceId]?.title,
                    startedAtIso = a.startedAt,
                    score = a.score,
                    total = a.total,
                    percent = p,
                    durationSeconds = durationSeconds(a),
                    deltaVsPrevious = delta
                )
            )
        }
        val pcts = list.mapNotNull { it.percent }
        return ExamBreakdown(
            attemptsTaken = mine.count { it.submittedAt != null },
            bestPct = pcts.maxOrNull(),
            latestPct = pcts.lastOrNull(),
            averagePct = if (pcts.isEmpty()) null else pcts.average().roundToInt(),
            attempts = list.reversed()
        )
    }
}
