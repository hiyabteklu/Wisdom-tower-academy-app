package com.wisdomtower.academy.data.games

import android.content.Context
import com.wisdomtower.academy.data.content.ContentItemResult
import com.wisdomtower.academy.data.content.LearningContentRepository
import com.wisdomtower.academy.data.content.LearningResource
import java.util.LinkedHashMap

/**
 * Loads question banks / exams through the security door [LearningContentRepository.getResource].
 * In-memory LRU cache of the last 12 parsed banks. Never queries meta directly.
 */
object GameQuestionLoader {

    private const val CACHE_CAP = 12

    sealed class LoadResult {
        data class Loaded(val questions: List<GameQuestion>, val skipped: Int) : LoadResult()
        data class Locked(val message: String) : LoadResult()
        object NeedsInternet : LoadResult()
        data class Failed(val message: String) : LoadResult()
    }

    private val cache = object : LinkedHashMap<String, Pair<List<GameQuestion>, Int>>(CACHE_CAP, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<List<GameQuestion>, Int>>?): Boolean {
            return size > CACHE_CAP
        }
    }

    @Synchronized
    fun cached(resourceId: String): Pair<List<GameQuestion>, Int>? = cache[resourceId]

    @Synchronized
    private fun putCache(resourceId: String, questions: List<GameQuestion>, skipped: Int) {
        cache[resourceId] = questions to skipped
    }

    /**
     * Load and parse one bank/exam. Uses cache when present.
     * A Failure whose message contains "locked" (case-insensitive) becomes [LoadResult.Locked].
     * IO-style "internet" messages become [LoadResult.NeedsInternet].
     */
    suspend fun load(context: Context, resource: LearningResource): LoadResult {
        val cached = cached(resource.id)
        if (cached != null) {
            return LoadResult.Loaded(cached.first, cached.second)
        }
        return when (val result = LearningContentRepository.getResource(context, resource.id)) {
            is ContentItemResult.Found -> {
                val item = result.item
                val (questions, skipped) = GameQuestionParser.parse(
                    metaJson = item.metaJson,
                    bankId = item.id,
                    bankTitle = item.title,
                    chapter = item.chapter
                )
                putCache(item.id, questions, skipped)
                LoadResult.Loaded(questions, skipped)
            }
            is ContentItemResult.NotFound -> LoadResult.Failed("This material was not found.")
            is ContentItemResult.Failure -> {
                val msg = result.message
                val lower = msg.lowercase()
                when {
                    lower.contains("locked") -> LoadResult.Locked(msg)
                    lower.contains("internet") || lower.contains("connection") -> LoadResult.NeedsInternet
                    else -> LoadResult.Failed(msg)
                }
            }
        }
    }

    /**
     * Preload a list of resources in the background (caller launches the coroutine).
     * Failures are ignored; the play path will surface them if needed.
     */
    suspend fun preload(context: Context, resources: List<LearningResource>) {
        for (r in resources) {
            if (cached(r.id) != null) continue
            try {
                load(context, r)
            } catch (_: Exception) {
                // best-effort preload
            }
        }
    }

    @Synchronized
    fun clearCache() {
        cache.clear()
    }
}
