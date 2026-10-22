#!/usr/bin/env python3
"""My Status V2, S6b (docs/MY_STATUS_V2_S6_DAY_LOG_PLAN.md): after a progress save succeeds and added seconds, LearningProgressRepository
also calls the Supabase function add_study_seconds (table study_day_log, local day of the phone). A failure here only logs the
HTTP code and NEVER changes the result of saveProgress. Two anchors, marker MYSTATUS-S6B, safe to run twice."""
import sys
from pathlib import Path

FILE = Path("app/src/main/java/com/wisdomtower/academy/data/content/LearningProgressRepository.kt")
MARK = "MYSTATUS-S6B"

ANCHOR1 = '''                status == STATUS_OK
            } catch (e: CancellationException) {'''
NEW1 = '''                if (status == STATUS_OK && addSeconds > 0) { // MYSTATUS-S6B
                    val dayToken = AuthStateManager.currentUser.value?.accessToken
                    if (!dayToken.isNullOrBlank()) logDay(dayToken, addSeconds, addFocusSeconds)
                }
                status == STATUS_OK
            } catch (e: CancellationException) {'''

ANCHOR2 = '''    private fun nowIso(): String ='''
NEW2 = '''    /** MYSTATUS-S6B: adds the seconds to today's row of study_day_log (day = the phone's local date). Never throws, never changes saveProgress. */
    private fun logDay(token: String, addSeconds: Int, addFocusSeconds: Int) {
        try {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val body = JSONObject()
                .put("p_day", day)
                .put("p_seconds", addSeconds)
                .put("p_focus", maxOf(0, addFocusSeconds))
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/add_study_seconds")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) Log.w(TAG, "day log failed: HTTP ${resp.code}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "day log failed: ${e.javaClass.simpleName}")
        }
    }

    private fun nowIso(): String ='''

src = FILE.read_text(encoding="utf-8")
if MARK in src:
    print("already patched, nothing to do"); sys.exit(0)
for name, a in (("save anchor", ANCHOR1), ("nowIso anchor", ANCHOR2)):
    n = src.count(a)
    if n != 1:
        print(f"ERROR: {name} found {n} times (need exactly 1); file not changed"); sys.exit(1)
FILE.write_text(src.replace(ANCHOR1, NEW1).replace(ANCHOR2, NEW2), encoding="utf-8")
print("patched")
