package com.wisdomtower.academy.data.games

import android.content.Context
import com.wisdomtower.academy.data.auth.AuthStateManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Local game progress in SharedPreferences `wt_games_v1`.
 * One JSON blob per student id (AuthStateManager.currentUser.id or "guest").
 * Never writes to exam_attempts / academic_results / learning_progress / competition tables.
 * Corrupt JSON = start fresh, never crash.
 */
object GameStore {

    private const val PREFS = "wt_games_v1"
    private val EAT: TimeZone = TimeZone.getTimeZone("Africa/Addis_Ababa")

    data class FloorRecord(
        val stars: Int,
        val bestAccuracy: Float,
        val plays: Int
    )

    data class AtticEntry(
        val questionId: String,
        val bankId: String,
        val correctInRow: Int
    )

    data class ClimbProgress(
        val floors: Map<String, FloorRecord>,
        val xp: Int,
        val bestStreak: Int,
        val lastDailyDate: String?,
        val dailyStreak: Int,
        val attic: List<AtticEntry>
    )

    data class DefenseProgress(
        val bestScore: Int,
        val bestWave: Int,
        val bestCombo: Int,
        val runs: Int
    )

    data class GlobalSettings(
        val soundOn: Boolean,
        val vibrationOn: Boolean
    )

    private fun studentId(): String {
        return AuthStateManager.currentUser.value?.id?.takeIf { it.isNotBlank() } ?: "guest"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readRoot(context: Context): JSONObject {
        val raw = prefs(context).getString(studentId(), null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        prefs(context).edit().putString(studentId(), root.toString()).apply()
    }

    fun getSettings(context: Context): GlobalSettings {
        val root = readRoot(context)
        return GlobalSettings(
            soundOn = root.optBoolean("soundOn", true),
            vibrationOn = root.optBoolean("vibrationOn", true)
        )
    }

    fun setSoundOn(context: Context, on: Boolean) {
        val root = readRoot(context)
        root.put("soundOn", on)
        writeRoot(context, root)
    }

    fun setVibrationOn(context: Context, on: Boolean) {
        val root = readRoot(context)
        root.put("vibrationOn", on)
        writeRoot(context, root)
    }

    fun getClimb(context: Context, scopePath: String): ClimbProgress {
        val root = readRoot(context)
        val climb = root.optJSONObject("climb") ?: return emptyClimb()
        val scope = climb.optJSONObject(scopePath) ?: return emptyClimb()
        val floorsObj = scope.optJSONObject("floors") ?: JSONObject()
        val floors = LinkedHashMap<String, FloorRecord>()
        val keys = floorsObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val o = floorsObj.optJSONObject(k) ?: continue
            floors[k] = FloorRecord(
                stars = o.optInt("stars", 0).coerceIn(0, 3),
                bestAccuracy = o.optDouble("bestAccuracy", 0.0).toFloat(),
                plays = o.optInt("plays", 0)
            )
        }
        val atticArr = scope.optJSONArray("attic") ?: JSONArray()
        val attic = ArrayList<AtticEntry>(atticArr.length())
        for (i in 0 until atticArr.length()) {
            val o = atticArr.optJSONObject(i) ?: continue
            val qid = o.optString("questionId", "")
            if (qid.isEmpty()) continue
            attic.add(
                AtticEntry(
                    questionId = qid,
                    bankId = o.optString("bankId", ""),
                    correctInRow = o.optInt("correctInRow", 0)
                )
            )
        }
        return ClimbProgress(
            floors = floors,
            xp = scope.optInt("xp", 0),
            bestStreak = scope.optInt("bestStreak", 0),
            lastDailyDate = scope.optString("lastDailyDate", null).takeIf { !it.isNullOrBlank() },
            dailyStreak = scope.optInt("dailyStreak", 0),
            attic = attic
        )
    }

    private fun emptyClimb() = ClimbProgress(
        floors = emptyMap(),
        xp = 0,
        bestStreak = 0,
        lastDailyDate = null,
        dailyStreak = 0,
        attic = emptyList()
    )

    fun recordFloorClear(
        context: Context,
        scopePath: String,
        floorKey: String,
        stars: Int,
        accuracy: Float,
        xpGained: Int,
        runStreak: Int
    ) {
        val root = readRoot(context)
        val climb = root.optJSONObject("climb") ?: JSONObject().also { root.put("climb", it) }
        val scope = climb.optJSONObject(scopePath) ?: JSONObject().also { climb.put(scopePath, it) }
        val floors = scope.optJSONObject("floors") ?: JSONObject().also { scope.put("floors", it) }
        val existing = floors.optJSONObject(floorKey)
        val prevStars = existing?.optInt("stars", 0) ?: 0
        val prevAcc = existing?.optDouble("bestAccuracy", 0.0) ?: 0.0
        val prevPlays = existing?.optInt("plays", 0) ?: 0
        val rec = JSONObject()
        rec.put("stars", maxOf(prevStars, stars.coerceIn(0, 3)))
        rec.put("bestAccuracy", maxOf(prevAcc, accuracy.toDouble()))
        rec.put("plays", prevPlays + 1)
        floors.put(floorKey, rec)
        scope.put("xp", scope.optInt("xp", 0) + xpGained.coerceAtLeast(0))
        scope.put("bestStreak", maxOf(scope.optInt("bestStreak", 0), runStreak))
        writeRoot(context, root)
    }

    fun recordFloorFail(context: Context, scopePath: String, floorKey: String) {
        val root = readRoot(context)
        val climb = root.optJSONObject("climb") ?: JSONObject().also { root.put("climb", it) }
        val scope = climb.optJSONObject(scopePath) ?: JSONObject().also { climb.put(scopePath, it) }
        val floors = scope.optJSONObject("floors") ?: JSONObject().also { scope.put("floors", it) }
        val existing = floors.optJSONObject(floorKey) ?: JSONObject()
        existing.put("plays", existing.optInt("plays", 0) + 1)
        if (!existing.has("stars")) existing.put("stars", 0)
        if (!existing.has("bestAccuracy")) existing.put("bestAccuracy", 0.0)
        floors.put(floorKey, existing)
        writeRoot(context, root)
    }

    fun setAttic(context: Context, scopePath: String, attic: List<AtticEntry>) {
        val root = readRoot(context)
        val climb = root.optJSONObject("climb") ?: JSONObject().also { root.put("climb", it) }
        val scope = climb.optJSONObject(scopePath) ?: JSONObject().also { climb.put(scopePath, it) }
        val arr = JSONArray()
        for (e in attic) {
            val o = JSONObject()
            o.put("questionId", e.questionId)
            o.put("bankId", e.bankId)
            o.put("correctInRow", e.correctInRow)
            arr.put(o)
        }
        scope.put("attic", arr)
        writeRoot(context, root)
    }

    fun todayEat(): String {
        val cal = Calendar.getInstance(EAT)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = EAT
        return fmt.format(cal.time)
    }

    fun recordDailyClimb(context: Context, scopePath: String) {
        val root = readRoot(context)
        val climb = root.optJSONObject("climb") ?: JSONObject().also { root.put("climb", it) }
        val scope = climb.optJSONObject(scopePath) ?: JSONObject().also { climb.put(scopePath, it) }
        val today = todayEat()
        val last = scope.optString("lastDailyDate", "")
        val streak = scope.optInt("dailyStreak", 0)
        if (last != today) {
            val cal = Calendar.getInstance(EAT)
            cal.add(Calendar.DAY_OF_MONTH, -1)
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = EAT
            val yesterday = fmt.format(cal.time)
            val next = if (last == yesterday) streak + 1 else 1
            scope.put("dailyStreak", next)
            scope.put("lastDailyDate", today)
        }
        writeRoot(context, root)
    }

    fun getDefense(context: Context, scopePath: String, examId: String): DefenseProgress {
        val root = readRoot(context)
        val def = root.optJSONObject("defense") ?: return DefenseProgress(0, 0, 0, 0)
        val key = "$scopePath|$examId"
        val o = def.optJSONObject(key) ?: return DefenseProgress(0, 0, 0, 0)
        return DefenseProgress(
            bestScore = o.optInt("bestScore", 0),
            bestWave = o.optInt("bestWave", 0),
            bestCombo = o.optInt("bestCombo", 0),
            runs = o.optInt("runs", 0)
        )
    }

    fun recordDefenseRun(
        context: Context,
        scopePath: String,
        examId: String,
        score: Int,
        wave: Int,
        combo: Int
    ) {
        val root = readRoot(context)
        val def = root.optJSONObject("defense") ?: JSONObject().also { root.put("defense", it) }
        val key = "$scopePath|$examId"
        val o = def.optJSONObject(key) ?: JSONObject()
        o.put("bestScore", maxOf(o.optInt("bestScore", 0), score))
        o.put("bestWave", maxOf(o.optInt("bestWave", 0), wave))
        o.put("bestCombo", maxOf(o.optInt("bestCombo", 0), combo))
        o.put("runs", o.optInt("runs", 0) + 1)
        def.put(key, o)
        writeRoot(context, root)
    }
}
