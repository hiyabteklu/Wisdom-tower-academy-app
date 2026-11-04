package com.wisdomtower.academy.data.content

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.WisdomTowerApplication
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** The three lock modes of the website (`HubLockMode` in `data/content-availability.ts`). */
enum class HubLockMode { Open, RequirePurchase, ComingSoon }

/** What the app does when a student taps a learning hub of a course. */
enum class HubGate {
    /** Go on (open the hub). */
    Open,

    /** Show the website's "Coming soon" message. */
    ComingSoon,

    /** Signed in but the package is not owned: show a message, NO purchase button, NO price. */
    PackageRequired
}

/**
 * Access state of one course (its six learning hubs), the native version of the website's
 * `ResourceHubGrid` rules: `open` = free for everybody, `coming_soon` = closed for everybody,
 * `require_purchase` = open only for a signed-in student who owns the package.
 *
 * A signed-OUT student on a `require_purchase` course is NOT blocked here: the native screens show their own
 * "Sign in to open" message (owner rule: real content needs a signed-in student).
 */
data class CourseAccess(
    val mode: HubLockMode,
    val signedIn: Boolean,
    val owned: Boolean
) {
    val gate: HubGate
        get() = when (mode) {
            HubLockMode.Open -> HubGate.Open
            HubLockMode.ComingSoon -> HubGate.ComingSoon
            HubLockMode.RequirePurchase -> when {
                !signedIn -> HubGate.Open
                owned -> HubGate.Open
                else -> HubGate.PackageRequired
            }
        }

    /** The `isUnlocked` check of plan step N9. */
    val isUnlocked: Boolean get() = gate == HubGate.Open
}

/**
 * GRANULAR LOCKS (owner order 2026-10-20: "lock and unlock materials at chapter level and material list level,
 * without a code change or a release"). Every lock is ONE row of `public.content_locks`; a new lock is a new row,
 * never new code. Keys (admin dashboard / SQL writes them, the app only reads them):
 *
 *  - `package:<package id>`                         whole package          (already existed)
 *  - `scope:<scope path>`                           one course / subject   (already existed)
 *  - `hub:<scope path>:<hub id>`                    ONE hub of a course, e.g. `hub:freshman/physics:books`
 *  - `chapter:<scope path>:<hub id>:<chapter no>`   one chapter in one hub, e.g. `chapter:freshman/physics:books:3`
 *  - `item:<learning_resources.id>`                 one material (book, note, deck, bank, exam)
 *
 * Modes: `open`, `require_purchase`, `coming_soon`, `locked` (shown as coming soon). The MOST SPECIFIC row wins:
 * item, then chapter, then hub, then scope, then package, then the static rule. So "course coming soon, except
 * Books open" is one `scope:` row plus one `hub:` row set to `open`, and "everything open except chapter 4 of
 * Books" is one `chapter:` row. No row = the level above decides. Clearing a lock = deleting its row.
 *
 * [hubGate] is what a hub card tap checks; [itemGate] is what a row tap in a hub list checks.
 *
 * TELEGRAM PAID BUILD (S1, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md` section 2; [paid] true only when
 * `BuildConfig.PAID_UI`): the model is simpler. Only `item:`, `chapter:` and `package:` rows count (the most specific
 * wins; `scope:` and `hub:` rows are ignored; no package row = the static rule, [packageMode]). Locks never hide
 * anything: [hubGate] is always Open, only rows get a gate. A student who owns the package has every row open.
 * The Play build ([paid] false) runs the old code above, unchanged.
 */
class CourseLocks internal constructor(
    private val scope: String?,
    private val signedIn: Boolean,
    private val owned: Boolean,
    private val courseMode: HubLockMode,
    private val rows: Map<String, HubLockMode>,
    private val paid: Boolean = false,
    private val packageMode: HubLockMode = courseMode
) {
    private fun gateOf(mode: HubLockMode): HubGate = CourseAccess(mode, signedIn, owned).gate

    /** Paid build: owned = everything open; else Open / Coming soon / Package required by the mode. */
    private fun paidGateOf(mode: HubLockMode): HubGate = when {
        owned -> HubGate.Open
        mode == HubLockMode.Open -> HubGate.Open
        mode == HubLockMode.ComingSoon -> HubGate.ComingSoon
        !signedIn -> HubGate.Open // the native screens show their own "Sign in to open"
        else -> HubGate.PackageRequired
    }

    /** Mode of one hub: its own `hub:` row, else the course / package / static mode. */
    fun hubMode(hubId: String): HubLockMode =
        scope?.let { rows["hub:$it:$hubId"] } ?: courseMode

    /** What a tap on the hub card does. Paid build: hub cards are never closed. */
    fun hubGate(hubId: String): HubGate = if (paid) HubGate.Open else gateOf(hubMode(hubId))

    /**
     * The gate of ONE material when an `item:` or `chapter:` row exists for it, else null (null = no extra
     * lock: the hub gate, already checked on the hub card, is the only rule). Item row beats chapter row.
     *
     * Paid build: never null. item row, else chapter row, else the package mode, then the owned / signed-in rule.
     */
    fun itemGate(hubId: String, chapter: Int?, itemId: String): HubGate? {
        if (paid) {
            val mode = rows["item:$itemId"]
                ?: (if (scope != null && chapter != null) rows["chapter:$scope:$hubId:$chapter"] else null)
                ?: packageMode
            return paidGateOf(mode)
        }
        rows["item:$itemId"]?.let { return gateOf(it) }
        if (scope != null && chapter != null) {
            rows["chapter:$scope:$hubId:$chapter"]?.let { return gateOf(it) }
        }
        return null
    }

    /**
     * Paid build only: numbers of the chapters of [hubId] that have a Coming soon `chapter:` row. The hub list adds a
     * placeholder row "Chapter N: Coming soon" for each one that has no items (an empty chapter made by the admin).
     * Empty list in the Play build.
     */
    fun comingSoonChapters(hubId: String): List<Int> {
        if (!paid || scope == null) return emptyList()
        val prefix = "chapter:$scope:$hubId:"
        return rows.entries
            .filter { it.value == HubLockMode.ComingSoon && it.key.startsWith(prefix) }
            .mapNotNull { it.key.removePrefix(prefix).toIntOrNull() }
            .sorted()
    }
}

/**
 * Plan step N9 (`docs/NATIVE_CONTENT_PLAN.md`): decides whether the hubs of a course can be opened.
 * Ports, from the website:
 *  - `data/content-availability.ts` (static rules per path, package ids per path, the two texts);
 *  - `lib/content-locks.ts` `resolveEffectiveLock` (admin locks in `public.content_locks`, public read with the
 *    anon key; priority scope lock, then package lock, then the static rule; the website grid passes no hub,
 *    so neither does this).
 *
 * OWNERSHIP (S-A2, 2026-10-20): the DATABASE is the only source of truth. The app asks the function
 * `public.my_package_access()` (signed-in student's own token) which packages the student owns or was granted
 * (enrollments and access grants) and whether enforcement is on (`system:enforce` lock). Same rule as the server
 * function `content_guard.resource_visible`, which also guards the downloads:
 *  - not signed in: owns nothing (a signed-out student on a paid course sees the native "Sign in" message);
 *  - enforcement off: everything is owned (exactly like the server);
 *  - enforcement on: owned only if the answer lists the package, or `__all__`; the two ECE semesters count as one.
 * The old "free mode" switch and the "free for registered" list are gone from the decision. Offline or server
 * trouble: the last answer for the same student is reused, else NOTHING is owned (a paid package stays closed rather
 * than opening by mistake). Lock rows are read separately and a read error never closes an open course.
 *
 * TELEGRAM PAID BUILD (S1, 2026-10-23, `docs/PLAN_TELEGRAM_PAID_NATIVE_V2.md`): ownership there ignores the global
 * `enforced` flag (both apps share one database; the flag is for the Play app) and uses ONLY the list of owned packages
 * (`ownsPaid`). Read from the live function (2026-10-23): `packages` is always the student's real list (enrollments and
 * access grants), whatever `enforced` says, so this is safe. The Telegram build also reads every `package:` row, so
 * the row of an ECE semester is found by its own package id (see `buildLocks`).
 *
 * OFFLINE-LOCK FIX (2026-10-23, owner: "content that is free for me becomes Package required after I have been offline
 * for some time or killed the app; opening data for 5 seconds fixes it"): CAUSE (read from this file). The last
 * ownership answer and the lock rows were kept only in memory (`ownership`, `cache`, `courseRowsCache`). After the app
 * process was killed (by the student, or by the phone after a while) and reopened with no internet, both were empty:
 * nothing was owned, no lock rows were known (so a course the admin made open fell back to the static rule, which
 * is RequirePurchase for freshman, grades and so on) and every card showed "Package required". One successful read
 * refilled the memory, which is why a few seconds of data fixed it. FIX: the last good answers are also saved on the
 * phone (SharedPreferences `wt_content_access_cache`) and used when the server cannot be reached and the memory is
 * empty: ownership (same student only, at most [DISK_MAX_AGE_MS] old, 30 days), lock rows and course modes (no age
 * limit). The `static...` functions used for the first frame read the same saved copies. Nothing is invented: only
 * what the server last said for this student. The server still decides on every open (`open_resource`), so a saved
 * "owned" never gives anything the server refuses; the cost is that a package taken away stays shown as open until
 * the next online read. NOT compiled, NOT seen on a phone.
 *
 * 2026-10-20: hub, chapter and item locks added ([CourseLocks], [resolveLocks], [staticLocks]). [resolve],
 * [staticAccess] and [isUnlocked] are unchanged in shape (course level).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
object ContentAccess {
    private const val TAG = "ContentAccess"
    private const val CACHE_MS = 30_000L

    /** Saved copies on the phone (see OFFLINE-LOCK FIX in the class comment). */
    private const val PREFS_NAME = "wt_content_access_cache"
    private const val DISK_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L

    /**
     * No longer used for any decision (S-A2). Kept only so other files that still mention it keep compiling;
     * the server answer of `my_package_access()` decides ownership now.
     */
    val IS_FREE_MODE: Boolean = false

    /** Texts copied from `data/content-availability.ts`. */
    const val COMING_SOON_TITLE = "Under development"
    const val COMING_SOON_BODY = "We are preparing this package. We will announce as soon as it is ready."
    const val COMING_SOON_BUTTON = "Got it"
    const val PURCHASE_TITLE = "Package required"
    const val PURCHASE_BODY = "Unlock this pathway with the matching package to access study materials."
    const val PURCHASE_BUTTON = "Keep exploring"

    /** Package id that means "everything" in `enrollments` / `access_grants` (same as the server). */
    private const val ALL_PACKAGES = "__all__"

    private val HUB_IDS = setOf(
        "books", "short-notes", "references", "flashcards",
        "question-banks", "exams", "life-savers", "videos"
    )

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val cache = HashMap<String, Pair<Long, HubLockMode?>>()

    /** Granular lock rows of one course, cached 30 s (key = package + scope). */
    private val courseRowsCache = HashMap<String, Pair<Long, Map<String, HubLockMode>>>()

    /** What `my_package_access()` said: is enforcement on, and which package ids the student owns or was granted. */
    private class PackageAccess(val enforced: Boolean, val packages: Set<String>)

    /** Last ownership answer: (student id, time read, answer). Used for one student only. */
    @Volatile
    private var ownership: Triple<String, Long, PackageAccess>? = null

    /**
     * The access state of the course at [basePath] (a website route such as `/academy/freshman/physics`;
     * a trailing hub id is ignored). Reads the admin locks from Supabase (cached 30 s).
     */
    suspend fun resolve(basePath: String, signedIn: Boolean): CourseAccess {
        val base = normalize(basePath)
        val unlockIds = unlockPackageIds(base)
        val inferred = inferPackageAndScope(base, unlockIds)
        val keys = listOfNotNull(
            inferred.second?.let { "scope:$it" },
            inferred.first?.let { "package:$it" }
        )
        val dynamic = if (keys.isEmpty()) null else dynamicMode(keys)
        val mode = dynamic ?: staticLockMode(base)
        return CourseAccess(mode, signedIn, ownsFrom(liveAccess(signedIn), signedIn, unlockIds))
    }

    /**
     * Same as [resolve] without the network: the saved answers (memory, then the phone) and the static rule (used
     * until the first answer arrives).
     */
    fun staticAccess(basePath: String, signedIn: Boolean): CourseAccess {
        val base = normalize(basePath)
        val unlockIds = unlockPackageIds(base)
        val (pkg, scope) = inferPackageAndScope(base, unlockIds)
        val keys = listOfNotNull(scope?.let { "scope:$it" }, pkg?.let { "package:$it" })
        return CourseAccess(
            cachedDynamicMode(keys) ?: staticLockMode(base),
            signedIn,
            ownsFrom(cachedAccess(), signedIn, unlockIds)
        )
    }

    /** `isUnlocked(package)` of the plan: true when the hubs of the course at [basePath] may be opened now. */
    suspend fun isUnlocked(basePath: String, signedIn: Boolean): Boolean =
        resolve(basePath, signedIn).isUnlocked

    /**
     * All locks that matter inside the course at [basePath]: course / package / hub / chapter / item (see
     * [CourseLocks]). One small request (cached 30 s). Offline or server trouble: the last answer is reused (memory,
     * then the copy saved on the phone), else only the static rule applies, so a network error never locks a student
     * out of an open course.
     */
    suspend fun resolveLocks(basePath: String, signedIn: Boolean): CourseLocks {
        val base = normalize(basePath)
        val (pkg, scope) = inferPackageAndScope(base, unlockPackageIds(base))
        val rows = courseRows(pkg, scope)
        return buildLocks(base, signedIn, rows, liveAccess(signedIn))
    }

    /**
     * Same as [resolveLocks] without the network: the saved answers (memory, then the phone) and the static rule
     * (used until the first answer arrives).
     */
    fun staticLocks(basePath: String, signedIn: Boolean): CourseLocks {
        val base = normalize(basePath)
        val (pkg, scope) = inferPackageAndScope(base, unlockPackageIds(base))
        val rows = cachedCourseRows(pkg, scope)
        return buildLocks(base, signedIn, rows, cachedAccess())
    }

    /**
     * Puts the lock rows and the ownership answer of one course together. Play build: exactly the old rules (course
     * mode from the scope / package row, ownership with the `enforced` flag). Telegram build (`PAID_UI`, S1): package mode
     * from the row of the package that is SOLD for this course ([PaidCheckout.packageIdForScope], so an ECE semester
     * uses its own row), else the static rule; ownership from the owned-packages list only ([ownsPaid]).
     */
    private fun buildLocks(
        base: String,
        signedIn: Boolean,
        rows: Map<String, HubLockMode>?,
        access: PackageAccess?
    ): CourseLocks {
        val unlockIds = unlockPackageIds(base)
        val (pkg, scope) = inferPackageAndScope(base, unlockIds)
        val courseMode = scope?.let { rows?.get("scope:$it") }
            ?: pkg?.let { rows?.get("package:$it") }
            ?: staticLockMode(base)
        if (!BuildConfig.PAID_UI) {
            return CourseLocks(
                scope,
                signedIn,
                ownsFrom(access, signedIn, unlockIds),
                courseMode,
                rows ?: emptyMap()
            )
        }
        val paidPackage = scope?.let { PaidCheckout.packageIdForScope(it) } ?: pkg
        val packageMode = paidPackage?.let { rows?.get("package:$it") } ?: staticLockMode(base)
        return CourseLocks(
            scope,
            signedIn,
            ownsPaid(access, signedIn, unlockIds),
            courseMode,
            rows ?: emptyMap(),
            true,
            packageMode
        )
    }

    /**
     * Reads the ownership answer now (for example right after sign-in or when the app comes to the front), so the
     * first course screen already knows it. Safe to call often (cached 30 s). Does nothing when signed out.
     */
    suspend fun warmUp() {
        liveAccess(AuthStateManager.currentUser.value != null)
    }

    // ---------- saved copies on the phone ----------

    private fun diskPrefs() = runCatching {
        WisdomTowerApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.getOrNull()

    private fun saveOwnership(userId: String, answer: PackageAccess, time: Long) {
        val p = diskPrefs() ?: return
        p.edit()
            .putString("own_user", userId)
            .putLong("own_time", time)
            .putBoolean("own_enforced", answer.enforced)
            .putStringSet("own_packages", HashSet(answer.packages))
            .apply()
    }

    /** The ownership answer saved for THIS student, at most [DISK_MAX_AGE_MS] old; null otherwise. */
    private fun loadOwnership(userId: String): PackageAccess? {
        val p = diskPrefs() ?: return null
        if (p.getString("own_user", null) != userId) return null
        val age = System.currentTimeMillis() - p.getLong("own_time", 0L)
        if (age > DISK_MAX_AGE_MS) return null
        val set = p.getStringSet("own_packages", null) ?: return null
        return PackageAccess(p.getBoolean("own_enforced", true), HashSet(set))
    }

    private fun saveRows(cacheKey: String, rows: Map<String, HubLockMode>) {
        val p = diskPrefs() ?: return
        val o = JSONObject()
        for ((k, v) in rows) o.put(k, v.name)
        p.edit().putString("rows_$cacheKey", o.toString()).apply()
    }

    private fun loadRows(cacheKey: String): Map<String, HubLockMode>? {
        val text = diskPrefs()?.getString("rows_$cacheKey", null) ?: return null
        return try {
            val o = JSONObject(text)
            val out = HashMap<String, HubLockMode>()
            val names = o.keys()
            while (names.hasNext()) {
                val k = names.next()
                val mode = runCatching { HubLockMode.valueOf(o.getString(k)) }.getOrNull()
                if (mode != null) out[k] = mode
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    /** [mode] null is saved as "none" (a read that found no row), so "no row" is remembered too. */
    private fun saveDynamic(cacheKey: String, mode: HubLockMode?) {
        diskPrefs()?.edit()?.putString("dyn_$cacheKey", mode?.name ?: "none")?.apply()
    }

    private fun loadDynamic(cacheKey: String): HubLockMode? {
        val name = diskPrefs()?.getString("dyn_$cacheKey", null) ?: return null
        return runCatching { HubLockMode.valueOf(name) }.getOrNull()
    }

    /** Course / package mode from memory (any age), else from the phone, else null (the static rule decides). */
    private fun cachedDynamicMode(keys: List<String>): HubLockMode? {
        if (keys.isEmpty()) return null
        val cacheKey = keys.joinToString("|")
        val mem = synchronized(cache) { cache[cacheKey] }
        return if (mem != null) mem.second else loadDynamic(cacheKey)
    }

    /** Lock rows of one course from memory (any age), else from the phone, else null. */
    private fun cachedCourseRows(pkg: String?, scope: String?): Map<String, HubLockMode>? {
        val cacheKey = "${pkg.orEmpty()}|${scope.orEmpty()}"
        val mem = synchronized(courseRowsCache) { courseRowsCache[cacheKey] }
        return mem?.second ?: loadRows(cacheKey)
    }

    // ---------- ownership ----------

    /**
     * Does the student own one of [unlockIds]? Same rule as `content_guard.resource_visible` in the database
     * (see the class comment). [access] null = no answer for this student yet: nothing is owned.
     */
    private fun ownsFrom(access: PackageAccess?, signedIn: Boolean, unlockIds: List<String>): Boolean {
        if (!signedIn || access == null) return false
        if (!access.enforced) return true
        if (ALL_PACKAGES in access.packages) return true
        return unlockIds.any { id ->
            id in access.packages ||
                (id.startsWith("ece-y3-sem-") && access.packages.any { it.startsWith("ece-y3-sem-") })
        }
    }

    /**
     * TELEGRAM BUILD (S1): same as [ownsFrom] but WITHOUT the global `enforced` flag (that flag belongs to the Play app,
     * both apps share one database). Owned only if the answer lists the package (or `__all__`); the two ECE semesters
     * count as one, like the server. No answer yet (offline, nothing saved) = nothing owned.
     */
    private fun ownsPaid(access: PackageAccess?, signedIn: Boolean, unlockIds: List<String>): Boolean {
        if (!signedIn || access == null) return false
        if (ALL_PACKAGES in access.packages) return true
        return unlockIds.any { id ->
            id in access.packages ||
                (id.startsWith("ece-y3-sem-") && access.packages.any { it.startsWith("ece-y3-sem-") })
        }
    }

    /** The last answer for the student who is signed in now (memory, then the phone), without any network call. */
    private fun cachedAccess(): PackageAccess? {
        val userId = AuthStateManager.currentUser.value?.id ?: return null
        val hit = ownership
        if (hit != null && hit.first == userId) return hit.third
        return loadOwnership(userId)
    }

    /** The answer for the signed-in student: cached 30 s, else asked from the server, else the last answer. */
    private suspend fun liveAccess(signedIn: Boolean): PackageAccess? {
        if (!signedIn) return null
        val userId = AuthStateManager.currentUser.value?.id ?: return null
        val now = System.currentTimeMillis()
        val hit = ownership
        if (hit != null && hit.first == userId && now - hit.second < CACHE_MS) return hit.third
        val fetched = fetchPackageAccess()
        if (fetched != null) {
            ownership = Triple(userId, now, fetched)
            saveOwnership(userId, fetched, now)
            return fetched
        }
        return if (hit != null && hit.first == userId) hit.third else loadOwnership(userId)
    }

    private suspend fun fetchPackageAccess(): PackageAccess? = withContext(Dispatchers.IO) {
        try {
            AuthStateManager.refreshIfNeeded()
            val token = AuthStateManager.currentUser.value?.accessToken
            if (token.isNullOrBlank()) return@withContext null
            var reply = postPackageAccess(token)
            if (reply.first == 401 && AuthStateManager.refreshIfNeeded(force = true)) {
                val fresh = AuthStateManager.currentUser.value?.accessToken
                if (!fresh.isNullOrBlank()) reply = postPackageAccess(fresh)
            }
            if (reply.first !in 200..299) return@withContext null
            val json = JSONObject(reply.second)
            if (json.optString("status") != "ok") return@withContext null
            val arr = json.optJSONArray("packages") ?: JSONArray()
            val ids = HashSet<String>()
            for (i in 0 until arr.length()) {
                val id = arr.optString(i, "")
                if (id.isNotEmpty()) ids.add(id)
            }
            PackageAccess(json.optBoolean("enforced", true), ids)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "ownership read failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** POST `rpc/my_package_access` with the student's OWN token (never the anon key as the login). */
    private fun postPackageAccess(token: String): Pair<Int, String> {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/my_package_access")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .post("{}".toRequestBody(JSON_MEDIA))
            .build()
        http.newCall(request).execute().use { resp ->
            return Pair(resp.code, resp.body?.string().orEmpty())
        }
    }

    // ---------- static rules (website `data/content-availability.ts`) ----------

    /**
     * `getHubLockMode` of the website. One deliberate difference: a path the website rules do not know is
     * treated as Open here (the website says coming soon), so an unusual native path can never be blocked
     * by mistake; only GAT and Exit Exam are closed by the static rule.
     */
    internal fun staticLockMode(base: String): HubLockMode {
        if (base.contains("/academy/gat") || base.contains("/academy/exit-exam")) return HubLockMode.ComingSoon
        val known = base.contains("/academy/grades/") ||
            base.contains("/academy/entrance-questions") ||
            base.contains("/academy/freshman") ||
            base.contains("/academy/uat") ||
            base.contains("/academy/coc") ||
            base.contains("/academy/remedial") ||
            base.contains("/special-packages")
        return if (known) HubLockMode.RequirePurchase else HubLockMode.Open
    }

    /** `unlockPackageIdsForPath` of the website. */
    internal fun unlockPackageIds(base: String): List<String> = when {
        base.contains("/academy/freshman") -> listOf("freshman")
        base.contains("/academy/grades/9") -> listOf("grade-9")
        base.contains("/academy/grades/10") -> listOf("grade-10")
        base.contains("/academy/grades/11") -> listOf("grade-11")
        base.contains("/academy/grades/12") -> listOf("grade-12")
        base.contains("/academy/entrance-questions") -> listOf("entrance-questions")
        base.contains("/academy/uat") -> listOf("uat")
        base.contains("/academy/coc") -> listOf("coc")
        base.contains("/academy/remedial") -> listOf("remedial")
        base.contains("/special-packages/electrical-computer-engineering") ||
            base.contains("/special-packages/ece") -> listOf("ece-y3-sem-1", "ece-y3-sem-2")
        else -> emptyList()
    }

    private val RE_FRESHMAN = Regex("/academy/freshman/([^/]+)")
    private val RE_GRADE = Regex("/academy/grades/(9|10|11|12)/([^/]+)")
    private val RE_REMEDIAL = Regex("/academy/remedial/([^/]+)")
    private val RE_ECE = Regex("/special-packages/(?:electrical-computer-engineering|ece)/(sem-[12])/([^/]+)")

    /** `inferPackageAndScope` of `components/ResourceHubGrid.tsx`: (package id, lock scope path). */
    private fun inferPackageAndScope(base: String, unlockIds: List<String>): Pair<String?, String?> {
        var pkg: String? = unlockIds.firstOrNull()
        var scope: String? = null

        val fresh = RE_FRESHMAN.find(base)
        if (fresh != null) {
            if (pkg == null) pkg = "freshman"
            scope = "freshman/${fresh.groupValues[1]}"
        }
        val grade = RE_GRADE.find(base)
        if (grade != null) {
            if (pkg == null) pkg = "grade-${grade.groupValues[1]}"
            scope = "grade/${grade.groupValues[1]}/${grade.groupValues[2]}"
        }
        val rem = RE_REMEDIAL.find(base)
        if (rem != null) {
            if (pkg == null) pkg = "remedial"
            scope = "remedial/${rem.groupValues[1]}"
        }
        val ece = RE_ECE.find(base)
        if (ece != null) {
            if (pkg == null) pkg = if (ece.groupValues[1] == "sem-1") "ece-y3-sem-1" else "ece-y3-sem-2"
            scope = "ece/${ece.groupValues[1]}/${ece.groupValues[2]}"
        }
        for (root in listOf("coc", "uat", "gat", "exit-exam")) {
            if (base.endsWith("/academy/$root")) {
                if (pkg == null) pkg = root
                if (scope == null) scope = root
            }
        }
        return Pair(pkg, scope)
    }

    /** Route without scheme/host/query, without a trailing slash and without a trailing hub id. */
    private fun normalize(basePath: String): String {
        var base = basePath.trim()
        val schemeEnd = base.indexOf("://")
        if (schemeEnd >= 0) {
            val slash = base.indexOf('/', schemeEnd + 3)
            base = if (slash >= 0) base.substring(slash) else "/"
        }
        base = base.substringBefore('?').substringBefore('#').trimEnd('/')
        if (base.substringAfterLast('/') in HUB_IDS) base = base.substringBeforeLast('/')
        return if (base.isEmpty()) "/academy" else base
    }

    // ---------- admin locks (`public.content_locks`) ----------

    /** First known lock found for [keys] in priority order, or null when no row exists / nothing could be read. */
    private suspend fun dynamicMode(keys: List<String>): HubLockMode? = withContext(Dispatchers.IO) {
        val cacheKey = keys.joinToString("|")
        val now = System.currentTimeMillis()
        val hit = synchronized(cache) { cache[cacheKey] }
        if (hit != null && now - hit.first < CACHE_MS) return@withContext hit.second
        try {
            val modes = fetchModes(keys)
            var found: HubLockMode? = null
            for (k in keys) {
                val m = modes[k]
                if (m != null) {
                    found = m
                    break
                }
            }
            synchronized(cache) { cache[cacheKey] = Pair(now, found) }
            saveDynamic(cacheKey, found)
            found
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "locks read failed: ${e.javaClass.simpleName}")
            if (hit != null) hit.second else loadDynamic(cacheKey)
        }
    }

    private fun fetchModes(keys: List<String>): Map<String, HubLockMode> {
        val inList = keys.joinToString(",", prefix = "in.(", postfix = ")") { "\"$it\"" }
        val url = "$supabaseUrl/rest/v1/content_locks".toHttpUrl().newBuilder()
            .addQueryParameter("select", "lock_key,mode")
            .addQueryParameter("lock_key", inList)
            .build().toString()
        return readModes(url)
    }

    /**
     * Every lock row that can matter inside one course: its package row, its scope row, all its `hub:` and
     * `chapter:` rows (key prefix) and every `item:` row (item keys carry only the material id; there are only as
     * many as the admin locked one by one). Telegram build (S1): also every `package:` row (a dozen at most), because an
     * ECE semester is sold under its own package id. Null when nothing could be read and nothing is cached or saved.
     */
    private suspend fun courseRows(pkg: String?, scope: String?): Map<String, HubLockMode>? =
        withContext(Dispatchers.IO) {
            val cacheKey = "${pkg.orEmpty()}|${scope.orEmpty()}"
            val now = System.currentTimeMillis()
            val hit = synchronized(courseRowsCache) { courseRowsCache[cacheKey] }
            if (hit != null && now - hit.first < CACHE_MS) return@withContext hit.second
            try {
                val parts = ArrayList<String>()
                pkg?.let { parts.add("lock_key.eq.\"package:$it\"") }
                if (BuildConfig.PAID_UI) parts.add("lock_key.like.\"package:*\"")
                scope?.let {
                    parts.add("lock_key.eq.\"scope:$it\"")
                    parts.add("lock_key.like.\"hub:$it:*\"")
                    parts.add("lock_key.like.\"chapter:$it:*\"")
                }
                parts.add("lock_key.like.\"item:*\"")
                val url = "$supabaseUrl/rest/v1/content_locks".toHttpUrl().newBuilder()
                    .addQueryParameter("select", "lock_key,mode")
                    .addQueryParameter("or", parts.joinToString(",", prefix = "(", postfix = ")"))
                    .build().toString()
                val rows = readModes(url)
                synchronized(courseRowsCache) { courseRowsCache[cacheKey] = Pair(now, rows) }
                saveRows(cacheKey, rows)
                rows
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "course locks read failed: ${e.javaClass.simpleName}")
                hit?.second ?: loadRows(cacheKey)
            }
        }

    /** GET a `content_locks` query and turn the rows into key to mode (rows with an unknown mode are ignored). */
    private fun readModes(url: String): Map<String, HubLockMode> {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()
        val text = http.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            body
        }
        val rows = JSONArray(text)
        val out = HashMap<String, HubLockMode>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val mode = parseMode(row.optString("mode")) ?: continue
            out[row.optString("lock_key")] = mode
        }
        return out
    }

    /** `toHubLockMode` of the website: the internal mode `locked` shows as coming soon. */
    private fun parseMode(raw: String): HubLockMode? = when (raw) {
        "open" -> HubLockMode.Open
        "require_purchase" -> HubLockMode.RequirePurchase
        "coming_soon", "locked" -> HubLockMode.ComingSoon
        else -> null
    }
}
