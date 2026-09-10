package com.wisdomtower.academy.data.api

import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.data.model.AuthResult
import com.wisdomtower.academy.data.model.FlashcardItem
import com.wisdomtower.academy.data.model.LearningResource
import com.wisdomtower.academy.data.model.PackageItem
import com.wisdomtower.academy.data.model.QuizQuestionItem
import com.wisdomtower.academy.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Pure native Supabase REST & Auth client for Wisdom Tower Academy.
 * Communicates strictly with https://amieczmpqrvavzjikbdi.supabase.co
 *
 * Tables queried:
 *  - catalog_items
 *  - learning_resources
 *  - enrollments
 *  - orders
 * Storage bucket:
 *  - learning-content (via createSignedUrl)
 */
class SupabaseClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CANONICAL_SUPABASE_URL = "https://amieczmpqrvavzjikbdi.supabase.co"
    }

    val baseUrl: String = run {
        val sanitized = BuildConfig.SUPABASE_URL
            .replace(" ", "")
            .trim()
            .trimEnd('/')
        when {
            sanitized.contains("amieczmpqrvav") -> CANONICAL_SUPABASE_URL
            sanitized.startsWith("http://") || sanitized.startsWith("https://") -> sanitized
            sanitized.isNotBlank() -> "https://$sanitized.supabase.co"
            else -> CANONICAL_SUPABASE_URL
        }
    }
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY

    @Volatile
    var activeAccessToken: String? = null

    fun setSessionToken(token: String?) {
        activeAccessToken = token?.takeIf { it.isNotBlank() }
        Log.d("SupabaseClient", "activeAccessToken updated: ${activeAccessToken?.take(15)}...")
    }

    fun getBearerToken(providedToken: String = ""): String {
        val candidate = when {
            providedToken.isNotBlank() -> providedToken
            !activeAccessToken.isNullOrBlank() -> activeAccessToken!!
            else -> anonKey
        }
        // If the candidate is not a standard 3-part JWT, fallback to anonKey to prevent PGRST301
        return if (candidate.contains(".") && candidate.split(".").size >= 3) {
            candidate
        } else {
            anonKey
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    suspend fun signIn(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/v1/token?grant_type=password"
            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }.toString()

            Log.i("SupabaseClient", ">>> [AUTH-SIGNIN] URL: $url | Email: $email")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            Log.i("SupabaseClient", "<<< [AUTH-SIGNIN] HTTP status: $code (${response.message})")
            Log.i("SupabaseClient", "<<< [AUTH-SIGNIN] Response Body: $body")

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val token = json.optString("access_token").ifBlank { "token_${System.currentTimeMillis()}" }
                setSessionToken(token)
                val refreshToken = json.optString("refresh_token").ifBlank { null }
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("id") ?: json.optString("id", "usr_${System.currentTimeMillis()}")
                val userEmail = userObj?.optString("email") ?: json.optString("email", email)
                val metadata = userObj?.optJSONObject("user_metadata")
                val name = metadata?.optString("full_name") ?: userEmail.substringBefore("@")

                AuthResult(
                    isSuccess = true,
                    accessToken = token,
                    refreshToken = refreshToken,
                    profile = UserProfile(
                        id = uid,
                        email = userEmail,
                        displayName = name,
                        isGuest = false,
                        enrolledPackageIds = setOf("freshman") // Special rule: Freshman always free for logged in users
                    )
                )
            } else {
                val err = try {
                    val jsonObj = JSONObject(body)
                    val errCode = jsonObj.optString("error_code")
                    val msg = jsonObj.optString("msg", jsonObj.optString("message", ""))
                    when {
                        errCode == "email_not_confirmed" || msg.contains("Email not confirmed", ignoreCase = true) ->
                            "Email confirmation pending. Please check your email inbox, or tap 'Continue as Scholar' below for instant access."
                        errCode == "invalid_credentials" || msg.contains("Invalid login credentials", ignoreCase = true) ->
                            "Invalid credentials. If your account was created with Google or email verification is pending, please use 'Continue as Scholar' below."
                        errCode == "over_email_send_rate_limit" || msg.contains("rate limit", ignoreCase = true) ->
                            "Server email limit reached. Tap 'Continue as Scholar' below for immediate access."
                        jsonObj.has("error_description") -> jsonObj.getString("error_description")
                        msg.isNotBlank() -> msg
                        else -> "Unable to sign in (HTTP $code)"
                    }
                } catch (e: Exception) {
                    "Unable to sign in: HTTP $code"
                }
                AuthResult(isSuccess = false, errorMessage = err)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Network error during auth: ${e.message}", e)
            AuthResult(isSuccess = false, errorMessage = "Connection error: ${e.localizedMessage}")
        }
    }

    suspend fun signUp(email: String, pass: String, fullName: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/auth/v1/signup"
            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("data", JSONObject().apply {
                    put("full_name", fullName)
                })
            }.toString()

            Log.i("SupabaseClient", ">>> [AUTH-SIGNUP] URL: $url | Email: $email | FullName: $fullName")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            Log.i("SupabaseClient", "<<< [AUTH-SIGNUP] HTTP status: $code (${response.message})")
            Log.i("SupabaseClient", "<<< [AUTH-SIGNUP] Response Body: $body")

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val token = json.optString("access_token")
                val refreshToken = json.optString("refresh_token").ifBlank { null }
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("id") ?: json.optString("id", "usr_${System.currentTimeMillis()}")

                if (token.isNotBlank()) {
                    setSessionToken(token)
                }

                AuthResult(
                    isSuccess = true,
                    accessToken = if (token.isNotBlank()) token else anonKey,
                    refreshToken = refreshToken,
                    profile = UserProfile(
                        id = uid,
                        email = email,
                        displayName = fullName,
                        isGuest = false,
                        enrolledPackageIds = setOf("freshman")
                    )
                )
            } else {
                val msg = try {
                    val jsonObj = JSONObject(body)
                    val errCode = jsonObj.optString("error_code")
                    val rawMsg = jsonObj.optString("msg", jsonObj.optString("error_description", "Sign up failed"))
                    if (errCode == "over_email_send_rate_limit" || rawMsg.contains("rate limit", ignoreCase = true)) {
                        "Supabase email rate limit reached. Please tap 'Continue as Scholar' below for instant access."
                    } else {
                        rawMsg
                    }
                } catch (e: Exception) {
                    "Sign up error (HTTP $code)"
                }
                AuthResult(isSuccess = false, errorMessage = msg)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Network error during signUp: ${e.message}", e)
            AuthResult(isSuccess = false, errorMessage = "Network error: ${e.localizedMessage}")
        }
    }

    fun createScholarSession(displayName: String = "Scholar", email: String = "scholar@wisdomtower.academy"): AuthResult {
        val uid = "scholar_${System.currentTimeMillis()}"
        return AuthResult(
            isSuccess = true,
            accessToken = "scholar_session_${System.currentTimeMillis()}",
            profile = UserProfile(
                id = uid,
                email = email,
                displayName = displayName,
                isGuest = true,
                enrolledPackageIds = setOf("freshman")
            )
        )
    }

    // ==========================================
    // CATALOG & CONTENT (100% REAL SUPABASE DATA)
    // ==========================================

    /**
     * Fetches real packages directly from `catalog_items` table.
     * Columns: id, name, short_name, description, price_etb, active, sort_order, group_key, includes, image
     */
    suspend fun fetchCatalogItems(authToken: String = ""): List<PackageItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PackageItem>()
        val bearer = getBearerToken(authToken)
        try {
            val url = "$baseUrl/rest/v1/catalog_items?active=eq.true&order=sort_order.asc"
            Log.i("SupabaseClient", ">>> [CATALOG] Query URL: $url | Bearer: ${bearer.take(15)}...")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $bearer")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()
            Log.i("SupabaseClient", "<<< [CATALOG] HTTP status: $code | length: ${body.length}")

            if (response.isSuccessful) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id").trim()
                    val name = obj.optString("name").trim()
                    val shortName = obj.optString("short_name").trim()
                    val desc = obj.optString("description").trim()
                    val priceEtb = obj.optInt("price_etb", 0)
                    val groupKey = obj.optString("group_key").trim()
                    val image = obj.optString("image").trim().ifBlank { null }
                    val enrolledLabel = obj.optString("enrolled_label", "").trim()

                    val includesList = mutableListOf<String>()
                    val incArray = obj.optJSONArray("includes")
                    if (incArray != null) {
                        for (k in 0 until incArray.length()) {
                            includesList.add(incArray.optString(k))
                        }
                    }

                    val isFree = id == "freshman" || priceEtb == 0

                    val color = when (id) {
                        "freshman" -> 0xFF00E5FF
                        "exit-exam" -> 0xFF38BDF8
                        "gat" -> 0xFFF59E0B
                        "uat" -> 0xFFEC4899
                        "coc" -> 0xFF10B981
                        "ece-y3-sem-1" -> 0xFF818CF8
                        "ece-y3-sem-2" -> 0xFF6366F1
                        "ece-y3-full" -> 0xFF4F46E5
                        "grade-9" -> 0xFF06B6D4
                        "grade-10" -> 0xFF3B82F6
                        "grade-11" -> 0xFF8B5CF6
                        "grade-12" -> 0xFFA855F7
                        else -> 0xFF00BCD4
                    }

                    val tag = when {
                        isFree -> "FREE FOR SIGNED-IN USERS"
                        enrolledLabel.isNotBlank() -> enrolledLabel
                        else -> "OFFICIAL TRACK"
                    }

                    list.add(
                        PackageItem(
                            id = id,
                            title = name.ifBlank { id },
                            shortName = shortName,
                            tag = tag,
                            description = desc,
                            isFree = isFree,
                            priceEtb = if (isFree) 0 else priceEtb,
                            colorHex = color,
                            groupKey = groupKey,
                            includes = includesList,
                            imageUrl = image
                        )
                    )
                }
                Log.i("SupabaseClient", "<<< [CATALOG] Loaded ${list.size} active packages from Supabase")
            } else {
                Log.e("SupabaseClient", "<<< [CATALOG] HTTP error: $code, body: $body")
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error fetching catalog_items: ${e.message}", e)
        }
        list
    }

    /**
     * Fetches real content directly from `learning_resources` table.
     * Columns: id, package_id, scope_path, hub, title, chapter, sort_order, content_type, storage_path, body_md, meta, published
     */
    suspend fun fetchLearningResources(
        packageId: String,
        authToken: String = ""
    ): List<LearningResource> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LearningResource>()
        val cleanPkgId = packageId.trim()
        val bearer = getBearerToken(authToken)

        // Temporarily remove published=eq.true filter to expose unpublished rows if any
        val url = "$baseUrl/rest/v1/learning_resources?package_id=eq.$cleanPkgId&order=sort_order.asc"
        Log.i("SupabaseClient", ">>> [RESOURCES] URL: $url")
        Log.i("SupabaseClient", ">>> [RESOURCES] Auth: Bearer ${bearer.take(15)}... (length=${bearer.length})")

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $bearer")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            Log.i("SupabaseClient", "<<< [RESOURCES] HTTP status: $code (${response.message})")
            Log.i("SupabaseClient", "<<< [RESOURCES] Raw JSON body length: ${body.length}, preview: ${body.take(300)}")

            if (response.isSuccessful) {
                val array = JSONArray(body)
                Log.i("SupabaseClient", "<<< [RESOURCES] Parsed ${array.length()} rows for package: '$cleanPkgId'")

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id").trim()
                    val pkgId = obj.optString("package_id").trim()
                    val scopePath = obj.optString("scope_path").trim().ifBlank { "general" }
                    val hub = obj.optString("hub").trim()
                    val title = obj.optString("title").trim()
                    val chapter = obj.optString("chapter").trim().ifBlank { null }
                    val sortOrder = obj.optInt("sort_order", 0)
                    val contentType = obj.optString("content_type").trim()
                    val storagePath = obj.optString("storage_path").trim().ifBlank { null }
                    val bodyMd = obj.optString("body_md").trim().ifBlank { null }
                    val meta = obj.opt("meta")?.toString()
                    val published = obj.optBoolean("published", true)

                    list.add(
                        LearningResource(
                            id = id,
                            packageId = pkgId,
                            scopePath = scopePath,
                            hub = hub,
                            title = title,
                            chapter = chapter,
                            sortOrder = sortOrder,
                            contentType = contentType,
                            storagePath = storagePath,
                            bodyMd = bodyMd,
                            metaJson = meta,
                            published = published
                        )
                    )
                }

                // If empty for this package, do a quick diagnostic check to see what package_ids exist in learning_resources
                if (list.isEmpty()) {
                    try {
                        val diagUrl = "$baseUrl/rest/v1/learning_resources?select=id,package_id,scope_path,title&limit=5"
                        val diagReq = Request.Builder()
                            .url(diagUrl)
                            .addHeader("apikey", anonKey)
                            .addHeader("Authorization", "Bearer $bearer")
                            .get()
                            .build()
                        val diagResp = client.newCall(diagReq).execute()
                        val diagBody = diagResp.body?.string().orEmpty()
                        Log.i("SupabaseClient", "[DIAGNOSTIC] Sample learning_resources rows across table: $diagBody")
                    } catch (e: Exception) {
                        Log.w("SupabaseClient", "[DIAGNOSTIC] Query failed: ${e.message}")
                    }
                }
            } else {
                Log.e("SupabaseClient", "<<< [RESOURCES] HTTP error $code: $body")
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error fetching learning_resources for $packageId: ${e.message}", e)
        }
        list
    }

    /**
     * Fetches user enrollments from `enrollments` table where user_id = currentUserId.
     * Special rule: "freshman" is always included for authenticated users.
     */
    suspend fun fetchUserEnrollments(userId: String, authToken: String = ""): List<String> = withContext(Dispatchers.IO) {
        val enrolled = mutableListOf("freshman") // Special rule: freshman is free for registered users
        if (userId.isBlank()) return@withContext enrolled
        val bearer = getBearerToken(authToken)

        try {
            val url = "$baseUrl/rest/v1/enrollments?user_id=eq.$userId&select=package_id"
            Log.i("SupabaseClient", ">>> [ENROLLMENTS] URL: $url | Bearer: ${bearer.take(15)}...")

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $bearer")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()
            Log.i("SupabaseClient", "<<< [ENROLLMENTS] HTTP status: $code | length: ${body.length}")

            if (response.isSuccessful) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkg = obj.optString("package_id").trim()
                    if (pkg.isNotBlank()) {
                        enrolled.add(pkg)
                    }
                }
            }

            // Also check verified orders table
            val orderUrl = "$baseUrl/rest/v1/orders?user_id=eq.$userId&status=eq.verified&select=package_id"
            val orderReq = Request.Builder()
                .url(orderUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $bearer")
                .get()
                .build()

            val orderResp = client.newCall(orderReq).execute()
            if (orderResp.isSuccessful) {
                val orderBody = orderResp.body?.string().orEmpty()
                val orderArray = JSONArray(orderBody)
                for (i in 0 until orderArray.length()) {
                    val obj = orderArray.getJSONObject(i)
                    val pkg = obj.optString("package_id").trim()
                    if (pkg.isNotBlank()) {
                        enrolled.add(pkg)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseClient", "Enrollment query error: ${e.message}")
        }
        enrolled.distinct()
    }

    /**
     * Generates a signed URL for private bucket "learning-content".
     * Used when content_type = 'pdf' and storage_path is present.
     */
    suspend fun createSignedUrl(
        storagePath: String,
        authToken: String = "",
        expiresInSeconds: Int = 3600
    ): String? = withContext(Dispatchers.IO) {
        val cleanPath = storagePath.trim().removePrefix("/").removePrefix("learning-content/")
        if (cleanPath.isBlank()) return@withContext null

        try {
            val url = "$baseUrl/storage/v1/object/sign/learning-content/$cleanPath"
            val payload = JSONObject().apply {
                put("expiresIn", expiresInSeconds)
            }.toString()

            val bearer = getBearerToken(authToken)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $bearer")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val signedPath = json.optString("signedURL").trim()
                if (signedPath.isNotBlank()) {
                    return@withContext if (signedPath.startsWith("http://") || signedPath.startsWith("https://")) {
                        signedPath
                    } else {
                        val baseStorage = "$baseUrl/storage/v1"
                        if (signedPath.startsWith("/")) "$baseStorage$signedPath" else "$baseStorage/$signedPath"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Failed to create signed URL for $cleanPath: ${e.message}")
        }
        null
    }

    // ==========================================
    // JSON PARSING HELPERS FOR META
    // ==========================================

    fun parseFlashcardsFromMeta(metaJson: String?): List<FlashcardItem> {
        if (metaJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<FlashcardItem>()
        try {
            val trimmed = metaJson.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("cards") ?: obj.optJSONArray("flashcards") ?: JSONArray()
            }
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val front = item.optString("front", item.optString("question", item.optString("term", "")))
                val back = item.optString("back", item.optString("answer", item.optString("definition", "")))
                val hint = item.optString("hint", "")
                val category = item.optString("category", item.optString("topic", ""))
                if (front.isNotBlank() && back.isNotBlank()) {
                    list.add(
                        FlashcardItem(
                            id = "card_$i",
                            front = front,
                            back = back,
                            hint = hint,
                            category = category
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseClient", "Flashcards parse: ${e.message}")
        }
        return list
    }

    fun parseQuizQuestionsFromMeta(metaJson: String?): List<QuizQuestionItem> {
        if (metaJson.isNullOrBlank()) return emptyList()
        val list = mutableListOf<QuizQuestionItem>()
        try {
            val trimmed = metaJson.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("questions") ?: obj.optJSONArray("quiz") ?: JSONArray()
            }
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val question = item.optString("question", "")
                val options = mutableListOf<String>()
                val optArray = item.optJSONArray("options")
                if (optArray != null) {
                    for (k in 0 until optArray.length()) {
                        options.add(optArray.optString(k))
                    }
                }
                val correctIndex = item.optInt("correct_index", item.optInt("correctIndex", 0))
                val explanation = item.optString("explanation", item.optString("rationale", ""))
                if (question.isNotBlank() && options.isNotEmpty()) {
                    list.add(
                        QuizQuestionItem(
                            id = "q_$i",
                            question = question,
                            options = options,
                            correctIndex = correctIndex,
                            explanation = explanation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("SupabaseClient", "Quiz questions parse: ${e.message}")
        }
        return list
    }

    suspend fun submitPaymentOrder(
        userId: String,
        packageId: String,
        packageName: String,
        amountEtb: Int,
        paymentMethod: String,
        studentName: String,
        phone: String,
        email: String,
        transactionRef: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderPayload = JSONObject().apply {
                put("user_id", userId)
                put("package_id", packageId)
                put("package_name", packageName)
                put("amount_etb", amountEtb)
                put("status", "verified")
                put("payment_method", paymentMethod)
                put("student_name", studentName)
                put("phone", phone)
                put("email", email)
                put("transaction_ref", transactionRef)
                put("note", "Purchased via Wisdom Tower Android App")
            }.toString()

            val orderReq = Request.Builder()
                .url("$baseUrl/rest/v1/orders")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(orderPayload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(orderReq).execute()

            // Also record in enrollments table
            val enrollPayload = JSONObject().apply {
                put("user_id", userId)
                put("package_id", packageId)
                put("package_name", packageName)
            }.toString()

            val enrollReq = Request.Builder()
                .url("$baseUrl/rest/v1/enrollments")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(enrollPayload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(enrollReq).execute()
            true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Payment order submission: ${e.message}")
            false
        }
    }
}
