package com.wisdomtower.academy.data.content

import android.content.Context
import android.util.Log
import com.wisdomtower.academy.BuildConfig
import com.wisdomtower.academy.WisdomTowerApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * TELEGRAM BUILD ONLY (owner order 2026-10-23, `docs/TELEGRAM_PAID_BUILD_PLAN.md`): package prices.
 *
 * ONE source: the admin dashboard catalog, table `public.catalog_items` (`id`, `price_etb`, `active`), read with the
 * public key (policy `ci_public_select_active`). NO built-in fallback price (owner: "price is decided from admin
 * dashboard and saved as copy in the app for offline use"). The last good answer is saved on the phone
 * (SharedPreferences `wt_package_prices`) and shown while offline. A package with no price in the dashboard (no row,
 * inactive, price 0) has NO price here, and the screens show nothing for it. Never invented.
 *
 * In the Play build ([BuildConfig.PAID_UI] false) nothing is read, nothing is saved, nothing is shown.
 *
 * STATUS: written, NOT compiled, NOT seen on a phone.
 */
object PackagePrices {
    private const val TAG = "PackagePrices"
    private const val PREFS_NAME = "wt_package_prices"
    private const val KEY_JSON = "prices_json"
    private const val REFRESH_MS = 60_000L

    private val supabaseUrl: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trim().trimEnd('/')
    private val anonKey: String get() = BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY.trim()

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val lock = Mutex()

    @Volatile
    private var lastRefreshMs = 0L

    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())

    /** Package id to price in ETB (only prices above 0). Starts with the saved copy from the phone. */
    val prices: StateFlow<Map<String, Double>> = _prices.asStateFlow()

    init {
        if (BuildConfig.PAID_UI) {
            _prices.value = loadSaved()
        }
    }

    private fun diskPrefs() = runCatching {
        WisdomTowerApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.getOrNull()

    private fun loadSaved(): Map<String, Double> {
        val text = diskPrefs()?.getString(KEY_JSON, null) ?: return emptyMap()
        return try {
            val o = JSONObject(text)
            val out = HashMap<String, Double>()
            val names = o.keys()
            while (names.hasNext()) {
                val k = names.next()
                val v = o.optDouble(k, 0.0)
                if (v > 0.0) out[k] = v
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun save(map: Map<String, Double>) {
        val o = JSONObject()
        for ((k, v) in map) o.put(k, v)
        diskPrefs()?.edit()?.putString(KEY_JSON, o.toString())?.apply()
    }

    /**
     * Reads the dashboard prices now (at most once a minute unless [force]). On any failure (offline, server error)
     * the saved copy stays on screen and nothing is changed.
     */
    suspend fun refresh(force: Boolean = false) {
        if (!BuildConfig.PAID_UI) return
        lock.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshMs < REFRESH_MS) return
            val fetched = withContext(Dispatchers.IO) { fetch() } ?: return
            lastRefreshMs = now
            _prices.value = fetched
            save(fetched)
        }
    }

    /** Null when the read failed (so the saved copy is kept); an empty map when the dashboard has no price at all. */
    private fun fetch(): Map<String, Double>? {
        return try {
            val url = "$supabaseUrl/rest/v1/catalog_items".toHttpUrl().newBuilder()
                .addQueryParameter("select", "id,price_etb")
                .addQueryParameter("active", "eq.true")
                .build()
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
            val out = HashMap<String, Double>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("id", "")
                val price = row.optDouble("price_etb", 0.0)
                if (id.isNotEmpty() && price > 0.0) out[id] = price
            }
            out
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "price read failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Same look as the website `formatEtb`: "500 ETB", "1,200 ETB" (cents only when the price has them). */
    fun format(price: Double): String {
        val whole = price % 1.0 == 0.0
        val number = if (whole) {
            String.format(Locale.US, "%,d", price.toLong())
        } else {
            String.format(Locale.US, "%,.2f", price)
        }
        return "$number ETB"
    }
}
