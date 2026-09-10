package com.wisdomtower.academy.data.repository

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.data.api.SupabaseClient
import com.wisdomtower.academy.data.catalog.WisdomCatalog
import com.wisdomtower.academy.data.local.WisdomDatabase
import com.wisdomtower.academy.data.local.entity.DownloadedResourceEntity
import com.wisdomtower.academy.data.local.entity.ResourceProgressEntity
import com.wisdomtower.academy.data.local.entity.UserEnrollmentEntity
import com.wisdomtower.academy.data.model.LearningResource
import com.wisdomtower.academy.data.model.PackageItem
import com.wisdomtower.academy.data.model.ResourceItem
import com.wisdomtower.academy.data.model.ResourceType
import com.wisdomtower.academy.data.model.SubjectItem
import com.wisdomtower.academy.data.security.SecureFileStorage
import com.wisdomtower.academy.data.security.SecureSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Pure native repository backed 100% by Supabase tables:
 * - catalog_items
 * - learning_resources
 * - enrollments
 *
 * All fake curriculum, synthetic packages, and PDF generation have been eliminated.
 */
class WisdomRepository(
    private val context: Context,
    val sessionManager: SecureSessionManager = SecureSessionManager(context),
    val fileStorage: SecureFileStorage = SecureFileStorage(context),
    private val database: WisdomDatabase = WisdomDatabase.getInstance(context),
    val supabaseClient: SupabaseClient = SupabaseClient()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resourceDao = database.resourceDao()
    private val progressDao = database.progressDao()
    private val enrollmentDao = database.enrollmentDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Real catalog items loaded from Supabase catalog_items table
    private val _liveCatalogFlow = MutableStateFlow<List<PackageItem>>(emptyList())

    // In-memory cache for learning_resources by package_id
    private val _packageResourcesCache = ConcurrentHashMap<String, List<LearningResource>>()

    init {
        // Initialize Supabase token from saved session if available
        val savedToken = sessionManager.getAccessToken()
        if (!savedToken.isNullOrBlank()) {
            supabaseClient.setSessionToken(savedToken)
        }

        // Automatically load real catalog on repository initialization
        scope.launch {
            refreshCatalog()
        }
    }

    suspend fun refreshCatalog() = withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken() ?: ""
            val remoteItems = supabaseClient.fetchCatalogItems(token)
            if (remoteItems.isNotEmpty()) {
                _liveCatalogFlow.value = remoteItems
            }
        } catch (e: Exception) {
            Log.e("WisdomRepo", "Error refreshing catalog: ${e.message}")
        }
    }

    // ==========================================
    // PACKAGES & OWNERSHIP
    // ==========================================

    fun getPackagesFlow(userId: String): Flow<List<PackageItem>> {
        return _liveCatalogFlow.combine(enrollmentDao.getUserEnrollments(userId)) { catalog, enrollments ->
            val ownedIds = enrollments.map { it.packageId }.toSet()
            catalog.map { pkg ->
                // Special rule: freshman is free for registered users
                val isOwned = if (pkg.id == "freshman" || pkg.isFree) {
                    sessionManager.isLoggedIn()
                } else {
                    ownedIds.contains(pkg.id)
                }
                pkg.copy(isOwned = isOwned)
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun syncRemoteEnrollments(userId: String) = withContext(Dispatchers.IO) {
        refreshCatalog()

        if (userId.isBlank()) return@withContext

        // Special rule: freshman package is always unlocked for signed-in users
        if (sessionManager.isLoggedIn()) {
            enrollmentDao.insertEnrollment(
                UserEnrollmentEntity(
                    userId = userId,
                    packageId = "freshman",
                    packageName = "Freshman Package",
                    isFreeTier = true,
                    isVerified = true
                )
            )
        }

        val token = sessionManager.getAccessToken() ?: ""
        val remoteEnrolledIds = supabaseClient.fetchUserEnrollments(userId, token)
        val currentCatalog = _liveCatalogFlow.value
        for (pkgId in remoteEnrolledIds) {
            val pkg = currentCatalog.find { it.id == pkgId }
            enrollmentDao.insertEnrollment(
                UserEnrollmentEntity(
                    userId = userId,
                    packageId = pkgId,
                    packageName = pkg?.title ?: pkgId,
                    isFreeTier = pkgId == "freshman",
                    isVerified = true
                )
            )
        }
    }

    fun isPackageOwned(userId: String, packageId: String): Flow<Boolean> {
        return enrollmentDao.isPackageUnlocked(userId, packageId)
    }

    suspend fun unlockPackage(userId: String, packageId: String, packageName: String) = withContext(Dispatchers.IO) {
        enrollmentDao.insertEnrollment(
            UserEnrollmentEntity(
                userId = userId,
                packageId = packageId,
                packageName = packageName,
                isFreeTier = packageId == "freshman",
                isVerified = true
            )
        )
    }

    // ==========================================
    // LEARNING RESOURCES & DERIVED SUBJECTS
    // ==========================================

    suspend fun loadPackageResources(packageId: String, forceReload: Boolean = false): List<LearningResource> = withContext(Dispatchers.IO) {
        if (!forceReload && _packageResourcesCache.containsKey(packageId)) {
            return@withContext _packageResourcesCache[packageId] ?: emptyList()
        }
        val token = sessionManager.getAccessToken() ?: ""
        val resources = supabaseClient.fetchLearningResources(packageId, token)
        _packageResourcesCache[packageId] = resources
        resources
    }

    /**
     * Derives subjects dynamically by grouping learning_resources by scope_path.
     * Returns emptyList() if package has no rows in learning_resources.
     */
    suspend fun getSubjects(packageId: String): List<SubjectItem> = withContext(Dispatchers.IO) {
        val resources = loadPackageResources(packageId)
        if (resources.isEmpty()) return@withContext emptyList()

        resources.groupBy { it.scopePath }.map { (scopePath, list) ->
            val cleanTitle = WisdomCatalog.formatScopeTitle(scopePath)
            SubjectItem(
                id = scopePath,
                packageId = packageId,
                name = cleanTitle,
                code = scopePath.uppercase(),
                description = "${list.size} learning resource(s)",
                iconName = "menu_book",
                booksCount = list.count { it.hub == "books" || it.contentType == "pdf" },
                notesCount = list.count { it.hub == "short-notes" || it.contentType == "markdown" },
                flashcardsCount = list.count { it.hub == "flashcards" || it.contentType == "flashcard_deck" },
                qbankCount = list.count { it.hub == "question-banks" || it.contentType == "quiz" },
                examCount = list.count { it.hub == "exams" || it.contentType == "exam" }
            )
        }.sortedBy { it.name }
    }

    suspend fun getSubjectById(packageId: String, subjectId: String): SubjectItem? = withContext(Dispatchers.IO) {
        getSubjects(packageId).find { it.id == subjectId }
    }

    private val _resourcesById = java.util.concurrent.ConcurrentHashMap<String, ResourceItem>()

    fun getResourceById(resourceId: String): ResourceItem? {
        val cached = _resourcesById[resourceId]
        if (cached != null) return cached

        for (list in _packageResourcesCache.values) {
            val found = list.find { it.id == resourceId }
            if (found != null) {
                val item = mapRawToResourceItem(found)
                _resourcesById[resourceId] = item
                return item
            }
        }
        return null
    }

    private fun mapRawToResourceItem(found: com.wisdomtower.academy.data.model.LearningResource): ResourceItem {
        val subjectName = WisdomCatalog.formatScopeTitle(found.scopePath)
        val resType = ResourceType.fromHubOrType(found.hub, found.contentType)
        return ResourceItem(
            id = found.id,
            packageId = found.packageId,
            subjectId = found.scopePath,
            subjectName = subjectName,
            title = found.title,
            resourceType = resType,
            summary = found.chapter ?: "",
            readingTimeMinutes = 15,
            fileSizeBytes = 0L,
            remoteUrl = found.signedUrl,
            storagePath = found.storagePath,
            markdownContent = found.bodyMd,
            flashcards = if (resType == ResourceType.FLASHCARDS) supabaseClient.parseFlashcardsFromMeta(found.metaJson) else emptyList(),
            quizQuestions = if (resType == ResourceType.QUESTION_BANK || resType == ResourceType.MOCK_EXAM) supabaseClient.parseQuizQuestionsFromMeta(found.metaJson) else emptyList(),
            chapter = found.chapter,
            hub = found.hub
        )
    }

    suspend fun getResourcesForSubject(packageId: String, subjectId: String): List<ResourceItem> = withContext(Dispatchers.IO) {
        val resources = loadPackageResources(packageId).filter { it.scopePath == subjectId }
        val subjectName = WisdomCatalog.formatScopeTitle(subjectId)

        val items = resources.map { lr ->
            val resType = ResourceType.fromHubOrType(lr.hub, lr.contentType)
            val flashcards = if (resType == ResourceType.FLASHCARDS) {
                supabaseClient.parseFlashcardsFromMeta(lr.metaJson)
            } else emptyList()

            val quizQuestions = if (resType == ResourceType.QUESTION_BANK || resType == ResourceType.MOCK_EXAM) {
                supabaseClient.parseQuizQuestionsFromMeta(lr.metaJson)
            } else emptyList()

            ResourceItem(
                id = lr.id,
                packageId = lr.packageId,
                subjectId = lr.scopePath,
                subjectName = subjectName,
                title = lr.title,
                resourceType = resType,
                summary = lr.chapter ?: "",
                readingTimeMinutes = 15,
                fileSizeBytes = 0L,
                remoteUrl = lr.signedUrl,
                storagePath = lr.storagePath,
                markdownContent = lr.bodyMd,
                flashcards = flashcards,
                quizQuestions = quizQuestions,
                chapter = lr.chapter,
                hub = lr.hub
            )
        }
        items.forEach { _resourcesById[it.id] = it }
        items
    }

    suspend fun getResourceById(packageId: String, resourceId: String): ResourceItem? = withContext(Dispatchers.IO) {
        val all = loadPackageResources(packageId)
        val found = all.find { it.id == resourceId } ?: return@withContext null
        val item = mapRawToResourceItem(found)
        _resourcesById[item.id] = item
        item
    }

    suspend fun getSignedUrl(storagePath: String): String? = withContext(Dispatchers.IO) {
        val token = sessionManager.getAccessToken() ?: ""
        supabaseClient.createSignedUrl(storagePath, authToken = token)
    }

    // ==========================================
    // DOWNLOADS & LOCAL STORAGE (REAL FILES ONLY)
    // ==========================================

    suspend fun downloadResource(resource: ResourceItem): Result<Unit> = withContext(Dispatchers.IO) {
        var downloadUrl = resource.remoteUrl
        if (downloadUrl.isNullOrBlank() && !resource.storagePath.isNullOrBlank()) {
            downloadUrl = getSignedUrl(resource.storagePath)
        }

        if (downloadUrl.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No remote document URL is available for this resource.")
            )
        }

        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Server returned HTTP ${response.code}"))
            }

            val bytes = response.body?.bytes() ?: return@withContext Result.failure(IllegalStateException("Empty server response"))
            val savedFile = fileStorage.saveResourceFile(resource.id, bytes, if (resource.resourceType == ResourceType.BOOK_PDF) "pdf" else "dat")

            resourceDao.insertResource(
                DownloadedResourceEntity(
                    id = resource.id,
                    title = resource.title,
                    resourceType = resource.resourceType.name,
                    packageId = resource.packageId,
                    packageName = _liveCatalogFlow.value.find { it.id == resource.packageId }?.title ?: "Academy Package",
                    subjectId = resource.subjectId,
                    subjectName = resource.subjectName,
                    localFilePath = savedFile.absolutePath,
                    textContent = resource.markdownContent,
                    fileSizeBytes = savedFile.length(),
                    downloadTimestamp = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDownloadedResource(resourceId: String) = withContext(Dispatchers.IO) {
        fileStorage.deleteResourceFile(resourceId, "pdf")
        resourceDao.deleteResourceById(resourceId)
    }

    fun getAllDownloadedResources(): Flow<List<DownloadedResourceEntity>> =
        resourceDao.getAllDownloaded()

    fun isResourceDownloaded(resourceId: String): Flow<Boolean> =
        resourceDao.isDownloaded(resourceId)

    fun getDownloadedResource(resourceId: String): Flow<DownloadedResourceEntity?> =
        resourceDao.getDownloadedById(resourceId)

    // ==========================================
    // PROGRESS TRACKING
    // ==========================================

    fun getProgress(resourceId: String, userId: String): Flow<ResourceProgressEntity?> =
        progressDao.getProgress(resourceId, userId)

    suspend fun saveProgress(
        resourceId: String,
        userId: String,
        progressPercent: Int,
        lastPage: Int = 1,
        totalPages: Int = 1,
        score: Int = 0,
        totalQuestions: Int = 0,
        completed: Boolean = false
    ) = withContext(Dispatchers.IO) {
        progressDao.upsertProgress(
            ResourceProgressEntity(
                resourceId = resourceId,
                userId = userId,
                progressPercent = progressPercent.coerceIn(0, 100),
                lastPageRead = lastPage,
                totalPages = totalPages,
                lastScore = score,
                totalQuestions = totalQuestions,
                isCompleted = completed,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
