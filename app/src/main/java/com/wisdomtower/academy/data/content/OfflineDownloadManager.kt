package com.wisdomtower.academy.data.content

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Process-lifetime download coordinator. Screen cancellation does not cancel accepted work. */
object OfflineDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Semaphore(1)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val bookJobs = ConcurrentHashMap<String, Deferred<File>>()
    private val managedUrls = ConcurrentHashMap.newKeySet<String>()
    private val mutableProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress = mutableProgress.asStateFlow()
    private val mutableFailed = MutableStateFlow<Set<String>>(emptySet())
    val failed = mutableFailed.asStateFlow()
    private val mutableCompleted = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val completed = mutableCompleted.asSharedFlow()
    private val mutableFinished = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val finished = mutableFinished.asSharedFlow()

    fun isManagingUrl(url: String): Boolean = url.trim() in managedUrls
    fun clearFailures(ids: Set<String>) { mutableFailed.value = mutableFailed.value - ids }

    fun start(context: Context, item: LearningResource): Boolean {
        val appContext = context.applicationContext
        synchronized(jobs) {
            if (jobs[item.id]?.isActive == true || bookJobs[item.id]?.isActive == true) return false
            mutableFailed.value = mutableFailed.value - item.id
            mutableProgress.value = mutableProgress.value + (item.id to -1f)
            val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                try {
                    if (item.contentType == "pdf") {
                        val url = LearningContentRepository.appwriteViewUrl(item.storagePath)
                            ?: throw IllegalStateException("No file address")
                        ensureBookFile(appContext, item.id, url)
                    } else {
                        queue.withPermit {
                            val result = LearningContentRepository.getResource(appContext, item.id, refresh = true)
                            val found = result as? ContentItemResult.Found
                                ?: throw IllegalStateException("Material is not available")
                            if (found.fromCache) throw java.io.IOException("Offline")
                            markDownloaded(appContext, item.id)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutableFailed.value = mutableFailed.value + item.id
                } finally {
                    mutableProgress.value = mutableProgress.value - item.id
                    mutableFinished.tryEmit(item.id)
                }
            }
            jobs[item.id] = job
            job.invokeOnCompletion { synchronized(jobs) { jobs.remove(item.id, job) } }
            job.start()
            return true
        }
    }

    /** Starts or joins a PDF transfer in this process scope; a screen may stop awaiting without cancelling it. */
    suspend fun ensureBookFile(context: Context, key: String, url: String, onProgress: (Float) -> Unit = {}): File {
        val appContext = context.applicationContext
        val deferred = synchronized(bookJobs) {
            val active = bookJobs[key]
            if (active != null && active.isActive) return@synchronized active
            val task = scope.async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                managedUrls.add(url.trim())
                mutableFailed.value = mutableFailed.value - key
                mutableProgress.value = mutableProgress.value + (key to -1f)
                try {
                    queue.withPermit {
                        val file = BookFileStore.ensureFile(appContext, key, url) { fraction ->
                            mutableProgress.value = mutableProgress.value + (key to fraction)
                            onProgress(fraction)
                        }
                        markDownloaded(appContext, key)
                        file
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutableFailed.value = mutableFailed.value + key
                    throw e
                } finally {
                    managedUrls.remove(url.trim())
                    mutableProgress.value = mutableProgress.value - key
                }
            }
            bookJobs[key] = task
            task.invokeOnCompletion { synchronized(bookJobs) { bookJobs.remove(key, task) } }
            task.start()
            task
        }
        return deferred.await()
    }

    private fun markDownloaded(context: Context, id: String) {
        val prefs = context.getSharedPreferences("wt_downloaded_resources", Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("ids", emptySet()).orEmpty().toMutableSet()
        ids.add(id)
        prefs.edit().putStringSet("ids", ids).apply()
        mutableCompleted.tryEmit(id)
    }
}
