package com.wisdomtower.academy.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.model.AuthUser
import com.wisdomtower.academy.data.model.StudentIdData
import com.wisdomtower.academy.data.model.StudentIdGenerator
import com.wisdomtower.academy.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object AuthStateManager {
    private const val PREFS_NAME = "wt_academy_auth_prefs"
    private const val KEY_USER_ID = "auth_user_id"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_FULL_NAME = "auth_full_name"

    // Legacy plain-text token keys: only READ once to migrate old installs, then removed.
    // Tokens now live encrypted in SecureSessionStore.
    private const val KEY_ACCESS_TOKEN = "auth_access_token"
    private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
    private const val KEY_CREATED_AT = "auth_created_at"
    private const val KEY_STUDENT_ID = "auth_student_id"
    private const val KEY_EDUCATION_LEVEL = "auth_education_level"
    private const val KEY_STREAM = "auth_stream"
    private const val KEY_SCHOOL_NAME = "auth_school_name"
    private const val KEY_TOWN_REGION = "auth_town_region"
    private const val KEY_AVATAR_PRESET = "auth_avatar_preset"
    private const val KEY_AVATAR_URL = "auth_avatar_url"

    /**
     * Refresh when fewer than this many seconds are left on the access token.
     * 5 minutes (was 2): the keep-fresh timer below ticks every 45 s, so the token is always renewed
     * long before it can expire, even for a student who reads for hours without leaving a page.
     */
    private const val REFRESH_MARGIN_SECONDS = 300L

    /** How often the foreground keep-fresh timer looks at the token (cheap: no network unless it is due). */
    private const val KEEP_FRESH_TICK_MS = 45_000L

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null

    /** Full Supabase session JSON (has expires_at). Contains tokens: NEVER log it. */
    @Volatile
    private var sessionJson: String? = null

    /** Only one refresh at a time: Supabase rotates refresh tokens, a second use of the old one fails. */
    private val refreshMutex = Mutex()

    private var lastNotifiedAuthEventKey: String? = null

    /** Bumped on every local profile edit; a profile fetch that started before an edit is discarded. */
    private val profileEditVersion = AtomicInteger(0)

    /** Number of profile saves in flight; a fetch must not overwrite the screen while one is running. */
    private val pendingSaves = AtomicInteger(0)

    /** User id whose real `profiles` row was read from the server in this run (null = never read). */
    @Volatile
    private var serverProfileUserId: String? = null

    /** The activity that is on screen right now (null while the app is in the background). */
    @Volatile
    private var foregroundActivity: WeakReference<Activity>? = null

    /** The foreground keep-fresh timer (see [onAppForeground]). */
    private var keepFreshJob: Job? = null

    /**
     * Deduplicates the "Signed in successfully / Welcome back" toast so it displays
     * strictly once per authenticated session event, never on tab switches or recompositions.
     */
    fun shouldNotifyAuthSuccess(userId: String?, tokenSnippet: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val key = "$userId:${tokenSnippet?.take(16).orEmpty()}"
        if (lastNotifiedAuthEventKey == key) {
            return false
        }
        lastNotifiedAuthEventKey = key
        return true
    }

    fun resetAuthNotification() {
        lastNotifiedAuthEventKey = null
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_USER_ID, null)
        if (!uid.isNullOrBlank()) {
            val secureJson = SecureSessionStore.load(ctx)
            val secureObj = secureJson?.let {
                try {
                    JSONObject(it)
                } catch (_: Exception) {
                    null
                }
            }
            val legacyAccess = prefs.getString(KEY_ACCESS_TOKEN, null)
            val legacyRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
            val hasLegacyTokens = !legacyAccess.isNullOrBlank() || !legacyRefresh.isNullOrBlank()

            val user = AuthUser(
                id = uid,
                email = prefs.getString(KEY_EMAIL, null),
                fullName = prefs.getString(KEY_FULL_NAME, null),
                accessToken = secureObj?.optNullableString("access_token") ?: legacyAccess,
                refreshToken = secureObj?.optNullableString("refresh_token") ?: legacyRefresh,
                createdAt = prefs.getString(KEY_CREATED_AT, null)
            )
            val profile = UserProfile(
                id = uid,
                email = prefs.getString(KEY_EMAIL, null),
                fullName = prefs.getString(KEY_FULL_NAME, null),
                educationLevel = prefs.getString(KEY_EDUCATION_LEVEL, null),
                stream = prefs.getString(KEY_STREAM, null),
                schoolName = prefs.getString(KEY_SCHOOL_NAME, null),
                townRegion = prefs.getString(KEY_TOWN_REGION, null),
                avatarPreset = prefs.getString(KEY_AVATAR_PRESET, null),
                avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
                studentIdNumber = prefs.getString(KEY_STUDENT_ID, null),
                createdAt = prefs.getString(KEY_CREATED_AT, null)
            )
            _currentUser.value = user
            _currentProfile.value = profile
            _isLoggedIn.value = true
            sessionJson = secureJson

            if (hasLegacyTokens) {
                // One-time migration of an old install: move tokens into the encrypted store,
                // then delete the plain-text copies.
                if (secureJson == null && !user.accessToken.isNullOrBlank()) {
                    val rebuilt = getAuthSessionJson()
                    if (rebuilt.isNotBlank() && SecureSessionStore.save(ctx, rebuilt)) {
                        sessionJson = rebuilt
                    }
                }
                prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
            }
        }
    }

    /**
     * Called by the WebView JS bridge when localStorage['wt-academy-auth-v1'] changes,
     * and by the native sign-in screen with SupabaseAuthResult.Success.sessionJson.
     */
    fun syncAuthSession(rawJson: String?, isExplicitSignOut: Boolean = false) {
        if (rawJson.isNullOrBlank() || rawJson == "null" || rawJson == "undefined" || rawJson == "{}") {
            if (isExplicitSignOut) {
                clearSession()
            }
            return
        }

        try {
            val obj = JSONObject(rawJson)
            val userObj = obj.optJSONObject("user")
            if (userObj == null) {
                if (isExplicitSignOut) clearSession()
                return
            }

            val uid = userObj.optString("id")
            if (uid.isBlank()) {
                if (isExplicitSignOut) clearSession()
                return
            }

            val email = userObj.optNullableString("email")
            val meta = userObj.optJSONObject("user_metadata")
            val fullName = meta?.optNullableString("full_name")
                ?: meta?.optNullableString("name")
                ?: email?.substringBefore("@")
                ?: "Student Scholar"

            val accessToken = obj.optNullableString("access_token")
            val refreshToken = obj.optNullableString("refresh_token")
            val createdAt = userObj.optNullableString("created_at")

            val authUser = AuthUser(
                id = uid,
                email = email,
                fullName = fullName,
                accessToken = accessToken,
                refreshToken = refreshToken,
                createdAt = createdAt
            )

            // Keep EVERYTHING already known about this same student (phone, bio, names, goals ...);
            // only fill the auth-metadata fields that are still empty. A different user starts clean.
            val existing = _currentProfile.value?.takeIf { it.id == uid }
            val profile = (existing ?: UserProfile(id = uid)).copy(
                id = uid,
                email = email ?: existing?.email,
                fullName = existing?.fullName ?: fullName,
                educationLevel = existing?.educationLevel ?: meta?.optNullableString("education_level"),
                stream = existing?.stream ?: meta?.optNullableString("stream"),
                schoolName = existing?.schoolName ?: meta?.optNullableString("school_name"),
                townRegion = existing?.townRegion ?: meta?.optNullableString("town_region"),
                avatarPreset = existing?.avatarPreset ?: meta?.optNullableString("avatar_preset"),
                avatarUrl = existing?.avatarUrl ?: meta?.optNullableString("avatar_url"),
                studentIdNumber = existing?.studentIdNumber ?: meta?.optNullableString("student_id_number"),
                createdAt = createdAt ?: existing?.createdAt
            )

            // Keep the FULL session (with expires_at) encrypted. A local-only session without a real
            // access token (old website fallback) is never stored.
            if (!accessToken.isNullOrBlank()) {
                val normalized = withExpiry(obj)
                if (normalized != sessionJson) {
                    sessionJson = normalized
                    appContext?.let { SecureSessionStore.save(it, normalized) }
                }
            } else {
                sessionJson = null
                appContext?.let { SecureSessionStore.clear(it) }
            }

            _currentUser.value = authUser
            _currentProfile.value = profile
            _isLoggedIn.value = true

            saveToPrefs(authUser, profile)

            // Asynchronously read the student's real profile row from Supabase (same table as the website)
            scope.launch {
                fetchFullProfile(uid, accessToken)
            }
        } catch (_: Exception) {
            // Non-fatal parse error; keep current state
        }
    }

    /**
     * Result of the native sign-in screen: store it, then hand it to the WebView.
     * Call from the UI after NativeSignInScreen reports success.
     */
    fun applyNativeSession(result: SupabaseAuthResult.Success, webView: WebView? = null) {
        syncAuthSession(result.sessionJson)
        injectSessionIntoWebView(webView)
        injectIntoAllWebViews(webView)
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000L

    /** Adds expires_at (seconds) from expires_in when Supabase did not send it. */
    private fun withExpiry(obj: JSONObject): String {
        val missing = !obj.has("expires_at") || obj.isNull("expires_at")
        if (missing && obj.has("expires_in") && !obj.isNull("expires_in")) {
            obj.put("expires_at", nowSeconds() + obj.optLong("expires_in"))
        }
        return obj.toString()
    }

    private fun expiresAtSeconds(): Long? {
        val raw = sessionJson ?: return null
        return try {
            val o = JSONObject(raw)
            if (o.has("expires_at") && !o.isNull("expires_at")) o.optLong("expires_at").takeIf { it > 0L } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The app came to the foreground (called from the Application's lifecycle callbacks).
     * Remembers the visible activity (so refreshed sessions can be pushed into its WebViews) and
     * starts the keep-fresh timer once. The timer is what keeps a student signed in while they read
     * for hours inside one WebView page: inside the app the website page is NOT allowed to refresh
     * the login itself (Supabase refresh tokens are single-use, two refreshers revoke the session),
     * so this native side renews it ahead of time and pushes it into every open page.
     */
    fun onAppForeground(activity: Activity) {
        foregroundActivity = WeakReference(activity)
        startKeepFreshTimer()
    }

    /** The app left the foreground. The timer keeps running but does nothing while no activity is visible. */
    fun onAppBackground(activity: Activity) {
        if (foregroundActivity?.get() === activity) {
            foregroundActivity = null
        }
    }

    @Synchronized
    private fun startKeepFreshTimer() {
        if (keepFreshJob?.isActive == true) return
        keepFreshJob = scope.launch {
            while (true) {
                delay(KEEP_FRESH_TICK_MS)
                if (foregroundActivity?.get() == null) continue
                if (_currentUser.value == null) continue
                try {
                    refreshIfNeeded()
                } catch (_: Exception) {
                    // never let one failed tick stop the timer; the next tick tries again
                }
            }
        }
    }

    /**
     * Refreshes the Supabase session when it is about to expire (or when [force] is true).
     * Safe to call often (app resume, before opening a hub, after a 401, keep-fresh timer). Returns
     * true when the student still has a usable session afterwards. Always saves the NEW refresh token
     * (Supabase rotates them) and pushes the new session into EVERY open website WebView, so no page
     * is ever left holding a token that was already rotated. On a refresh that Supabase rejects,
     * signs the student out cleanly. Network problems keep the current session and return false
     * (try again later).
     */
    suspend fun refreshIfNeeded(webView: WebView? = null, force: Boolean = false): Boolean {
        return refreshMutex.withLock {
            val user = _currentUser.value ?: return@withLock false
            val refreshToken = user.refreshToken
            if (refreshToken.isNullOrBlank()) return@withLock false
            val expiresAt = expiresAtSeconds()
            val expiringSoon = expiresAt == null || expiresAt - nowSeconds() < REFRESH_MARGIN_SECONDS
            if (!force && !expiringSoon) return@withLock true

            when (val result = SupabaseAuthClient.refresh(refreshToken)) {
                is SupabaseAuthResult.Success -> {
                    syncAuthSession(result.sessionJson)
                    injectIntoAllWebViews(webView)
                    true
                }
                is SupabaseAuthResult.Failure -> {
                    if (result.kind == AuthFailureKind.WRONG_CREDENTIALS) {
                        // The web page may already have rotated the token and told us (bridge).
                        // Only sign out if our token is still the one Supabase just rejected.
                        if (_currentUser.value?.refreshToken == refreshToken) {
                            signOut(webView)
                            false
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * Generates a valid JSON session string matching Supabase wt-academy-auth-v1 format
     * for injection into the WebView so web handoffs inherit the authenticated session.
     * Prefers the stored full Supabase session (has token_type, expires_in, expires_at).
     */
    fun getAuthSessionJson(): String {
        sessionJson?.let { if (it.isNotBlank()) return it }
        val user = _currentUser.value ?: return ""
        val profile = _currentProfile.value
        return try {
            val userMeta = JSONObject().apply {
                put("full_name", user.fullName ?: profile?.fullName ?: "Student Scholar")
                put("name", user.fullName ?: profile?.fullName ?: "Student Scholar")
                profile?.educationLevel?.let { put("education_level", it) }
                profile?.stream?.let { put("stream", it) }
                profile?.schoolName?.let { put("school_name", it) }
                profile?.townRegion?.let { put("town_region", it) }
                profile?.avatarPreset?.let { put("avatar_preset", it) }
                profile?.avatarUrl?.let { put("avatar_url", it) }
                profile?.studentIdNumber?.let { put("student_id_number", it) }
            }
            val userObj = JSONObject().apply {
                put("id", user.id)
                put("email", user.email ?: "")
                put("created_at", user.createdAt ?: "")
                put("user_metadata", userMeta)
            }
            val root = JSONObject().apply {
                put("access_token", user.accessToken ?: "")
                put("refresh_token", user.refreshToken ?: "")
                put("user", userObj)
            }
            root.toString()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Injects the active native session into the WebView localStorage so handoffs are already signed in.
     * Overwrites a stale or missing web session (different user, no token, or older expires_at) and
     * then fires 'wta-auth-change'. A web session that is newer than ours is left alone.
     * Also marks the page as running inside the app (window.__wtaNativeApp) so the website knows that
     * the native side owns token refresh.
     */
    fun injectSessionIntoWebView(webView: WebView?) {
        val json = getAuthSessionJson()
        if (json.isBlank()) return
        val escaped = JSONObject.quote(json)
        val profile = _currentProfile.value
        val user = _currentUser.value
        val eduLevel = JSONObject.quote(profile?.educationLevel ?: "")
        val stream = JSONObject.quote(profile?.stream ?: "")
        val fullName = JSONObject.quote(profile?.fullName ?: user?.fullName ?: "")
        val email = JSONObject.quote(profile?.email ?: user?.email ?: "")
        val studentId = JSONObject.quote(profile?.studentIdNumber ?: "")
        val js = """
            (function(){
              try {
                window.__wtaNativeApp = true;
                var ours = JSON.parse($escaped);
                if (!ours || !ours.access_token || !ours.user) return;
                var cur = null;
                try { cur = JSON.parse(localStorage.getItem('wt-academy-auth-v1') || 'null'); } catch(e) { cur = null; }
                var curOk = !!(cur && cur.access_token && cur.user && cur.user.id);
                var sameUser = curOk && cur.user.id === ours.user.id;
                var oursNewer = !sameUser || ((Number(cur.expires_at) || 0) < (Number(ours.expires_at) || 0));
                if (!curOk || oursNewer) {
                  localStorage.setItem('wt-academy-auth-v1', $escaped);
                  if ($eduLevel && $eduLevel !== '""') localStorage.setItem('wt_student_academic_level', $eduLevel);
                  if ($stream && $stream !== '""') localStorage.setItem('wt_student_academic_stream', $stream);
                  if ($fullName && $fullName !== '""') localStorage.setItem('wt_user_name', $fullName);
                  if ($email && $email !== '""') localStorage.setItem('wt_user_email', $email);
                  if ($studentId && $studentId !== '""') localStorage.setItem('wt_student_id', $studentId);
                  window.dispatchEvent(new CustomEvent('wta-auth-change'));
                }
              } catch(e) {}
            })();
        """.trimIndent()
        webView?.post {
            webView.evaluateJavascript(js, null)
        }
    }

    /**
     * Pushes the current session into every website WebView that is open in the visible activity
     * (main page, tool overlay, admin console, readers ...), plus [extra] when given. Only pages of
     * our own website receive it: never a third-party page, a file:// page or a blank page.
     * Safe to call from any thread and as often as needed (a page that already holds an equal or
     * newer session is left untouched by the injected script).
     */
    fun injectIntoAllWebViews(extra: WebView? = null) {
        if (extra != null) injectSessionIntoWebView(extra)
        val activity = foregroundActivity?.get() ?: return
        activity.runOnUiThread {
            val views = LinkedHashSet<WebView>()
            collectWebViews(activity.window?.decorView, views)
            for (wv in views) {
                if (wv === extra) continue
                if (isOurSite(wv.url)) injectSessionIntoWebView(wv)
            }
        }
    }

    private fun collectWebViews(view: View?, out: MutableSet<WebView>) {
        if (view == null) return
        if (view is WebView) {
            out.add(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectWebViews(view.getChildAt(i), out)
            }
        }
    }

    /** True only for https pages of the Wisdom Tower website (www or not). */
    private fun isOurSite(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("https://")) return false
        val host = try {
            Uri.parse(url).host
        } catch (_: Exception) {
            null
        } ?: return false
        val siteHost = try {
            Uri.parse(BuildConfig.NEXT_PUBLIC_SITE_URL.trim()).host
        } catch (_: Exception) {
            null
        } ?: "www.wisdom-tower-academy.live"
        fun bare(h: String) = h.lowercase().removePrefix("www.")
        return bare(host) == bare(siteHost)
    }

    private fun saveToPrefs(user: AuthUser, profile: UserProfile) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_FULL_NAME, user.fullName)
            // Tokens are NOT stored here any more (see SecureSessionStore); remove old plain copies.
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            putString(KEY_CREATED_AT, user.createdAt)
            putString(KEY_STUDENT_ID, profile.studentIdNumber)
            putString(KEY_EDUCATION_LEVEL, profile.educationLevel)
            putString(KEY_STREAM, profile.stream)
            putString(KEY_SCHOOL_NAME, profile.schoolName)
            putString(KEY_TOWN_REGION, profile.townRegion)
            putString(KEY_AVATAR_PRESET, profile.avatarPreset)
            putString(KEY_AVATAR_URL, profile.avatarUrl)
            apply()
        }
    }

    /** Keeps the edited profile on this phone (state + SharedPreferences). Does not touch the server. */
    private fun updateLocalProfile(updated: UserProfile) {
        profileEditVersion.incrementAndGet()
        _currentProfile.value = updated
        _currentUser.value?.let { user ->
            val mergedUser = user.copy(fullName = updated.fullName ?: user.fullName)
            _currentUser.value = mergedUser
            saveToPrefs(mergedUser, updated)
        }
    }

    /**
     * Saves the edited profile on this phone AND pushes it to Supabase in the background (same
     * `profiles` table as the website). Fire and forget: the caller is not told whether the server
     * accepted it. Prefer [saveProfile] when the screen must show an honest result.
     */
    fun updateProfile(updated: UserProfile) {
        updateLocalProfile(updated)
        scope.launch { pushProfile(updated) }
    }

    /**
     * Saves the edited profile on this phone, then writes it to Supabase and returns the real result
     * ([AccountCallResult.Success] only when the server returned the updated row).
     */
    suspend fun saveProfile(updated: UserProfile): AccountCallResult {
        updateLocalProfile(updated)
        return pushProfile(updated)
    }

    private suspend fun pushProfile(profile: UserProfile): AccountCallResult {
        pendingSaves.incrementAndGet()
        try {
            refreshIfNeeded()
            var token = _currentUser.value?.accessToken
            if (token.isNullOrBlank()) {
                return AccountCallResult.Failure("Your session expired. Please sign in again.", true)
            }
            var result = trySave(profile, token)
            if (result is AccountCallResult.Failure && result.sessionExpired) {
                // The token may have expired between the check and the call: refresh once and retry once.
                if (refreshIfNeeded(force = true)) {
                    token = _currentUser.value?.accessToken
                    if (!token.isNullOrBlank()) result = trySave(profile, token)
                }
            }
            return result
        } finally {
            pendingSaves.decrementAndGet()
        }
    }

    private suspend fun trySave(profile: UserProfile, token: String): AccountCallResult {
        // Never overwrite the server's daily goal with the model default (45) unless the real
        // server value was read for this student or the student chose a different value.
        val includeGoal = serverProfileUserId == profile.id || profile.dailyStudyGoalMinutes != 45
        return SupabaseAccountClient.saveProfile(profile.id, token, profile, includeGoal)
    }

    fun clearSession() {
        _currentUser.value = null
        _currentProfile.value = null
        _isLoggedIn.value = false
        sessionJson = null
        serverProfileUserId = null
        resetAuthNotification()
        val ctx = appContext ?: return
        SecureSessionStore.clear(ctx)
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun signOut(webView: WebView?) {
        clearSession()
        webView?.post {
            webView.evaluateJavascript(
                "(function(){try{" +
                    "localStorage.removeItem('wt-academy-auth-v1');" +
                    "for(var i=localStorage.length-1;i>=0;i--){" +
                    "  var k=localStorage.key(i);" +
                    "  if(k&&(k.indexOf('sb-')===0||k.indexOf('auth')!==-1))localStorage.removeItem(k);" +
                    "}" +
                    "window.dispatchEvent(new CustomEvent('wta-auth-change'));" +
                "}catch(e){}})();",
                null
            )
        }
    }

    /**
     * Reads the student's real row from Supabase `profiles` (same table and columns as the website's
     * getFullProfile). The old call to the website route /api/account/profile is gone: that route does
     * not exist on the website (STATUS.md S8).
     */
    private suspend fun fetchFullProfile(userId: String, token: String?) {
        if (token.isNullOrBlank()) return
        // Do not read while a save is running (the server may still hold the old values).
        if (pendingSaves.get() > 0) return
        val versionAtStart = profileEditVersion.get()
        val result = SupabaseAccountClient.fetchProfile(userId, token, _currentProfile.value)
        // Only a real row is applied. "No row", "session expired" and failures change nothing here.
        val fetched = (result as? ProfileFetchResult.Found)?.profile ?: return
        // Ignore the answer if the student signed out, switched account, or edited/saved meanwhile.
        if (_currentUser.value?.id != userId) return
        if (profileEditVersion.get() != versionAtStart || pendingSaves.get() > 0) return
        serverProfileUserId = userId
        _currentProfile.value = fetched
        _currentUser.value?.let { saveToPrefs(it, fetched) }
    }

    fun getStudentIdData(): StudentIdData {
        val user = _currentUser.value
        val profile = _currentProfile.value
        return StudentIdGenerator.computeStudentId(
            userId = user?.id,
            profile = profile,
            createdAtStr = user?.createdAt
        )
    }

    private fun JSONObject.optNullableString(key: String, fallback: String? = null): String? {
        if (!has(key) || isNull(key)) return fallback
        val v = optString(key)
        return if (v.isBlank() || v == "null") fallback else v
    }
}
