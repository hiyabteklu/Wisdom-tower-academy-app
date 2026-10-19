package com.wisdomtower.academy.ui.guides

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentOrange
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomAccentViolet
import com.wisdomtower.academy.ui.theme.WisdomCyan
import org.json.JSONObject

data class GuideTab(
    val slug: String,
    val title: String,
    val accentColor: Color
)

val GUIDE_TABS: List<GuideTab> get() = listOf(
    GuideTab("study-techniques", "Study Techniques", WisdomCyan),
    GuideTab("success-stories", "Success Stories", WisdomAccentAmber),
    GuideTab("campus-life", "Campus Life", WisdomAccentSky),
    GuideTab("universities", "Universities", WisdomAccentViolet),
    GuideTab("departments", "Departments", WisdomAccentOrange),
    GuideTab("scholarships", "Scholarships", WisdomAccentRose)
)

/** [imageUrl] is the full public link of the cover photo (website column `image_path`), "" when the story has none. */
data class SuccessStory(
    val id: String,
    val name: String,
    val achievement: String,
    val program: String,
    val year: String,
    val quote: String,
    val body: String,
    val imageUrl: String = ""
)

data class ScholarshipOpportunity(
    val id: String,
    val title: String,
    val provider: String,
    val amount: String,
    val level: String,
    val country: String,
    val eligibility: String,
    val deadline: String,
    val externalUrl: String,
    val body: String
)

data class UniversityDetail(
    val id: String,
    val name: String,
    val abbr: String,
    val region: String,
    val location: String,
    val website: String,
    val founded: String,
    val campuses: String,
    val climate: String,
    val elevationM: Int?,
    val distanceFromAddisKm: Int?,
    val distanceNote: String,
    val strengths: List<String>,
    val whatToExpect: List<String>,
    val tips: List<String>,
    val studentFit: String,
    val featured: Boolean = false,
    val knownFor: List<String> = emptyList(),
    val detailed: Boolean = false
)

data class DepartmentCategoryDetail(
    val id: String,
    val label: String,
    val blurb: String
)

data class DepartmentDetail(
    val id: String,
    val name: String,
    val shortName: String,
    val categoryId: String,
    val durationYears: String,
    val about: String,
    val courses: List<String>,
    val careers: List<String>,
    val market: String,
    val pros: List<String>,
    val cons: List<String>
)

/** Website `universitiesIntro.subtitle` (src/data/universities.ts). */
const val UNIVERSITIES_SUBTITLE = "What it actually feels like to study at each one"

object GuidesRepository {

    /** Website `universitiesIntro.closing`, read from the packed JSON (empty if missing). */
    fun loadUniversitiesClosing(context: Context): String {
        return try {
            val jsonStr = context.assets.open("data/universities.json").bufferedReader().use { it.readText() }
            JSONObject(jsonStr).optJSONObject("intro")?.optString("closing").orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun loadUniversities(context: Context): Pair<List<String>, List<UniversityDetail>> {
        val introParagraphs = mutableListOf<String>()
        val list = mutableListOf<UniversityDetail>()
        try {
            val jsonStr = context.assets.open("data/universities.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val introObj = root.optJSONObject("intro")
            if (introObj != null) {
                val pArr = introObj.optJSONArray("paragraphs")
                if (pArr != null) {
                    for (i in 0 until pArr.length()) {
                        introParagraphs.add(pArr.getString(i))
                    }
                }
            }

            val uArr = root.optJSONArray("universities")
            if (uArr != null) {
                for (i in 0 until uArr.length()) {
                    val u = uArr.getJSONObject(i)
                    val strengths = mutableListOf<String>()
                    val sArr = u.optJSONArray("strengths")
                    if (sArr != null) {
                        for (s in 0 until sArr.length()) strengths.add(sArr.getString(s))
                    }
                    val what = mutableListOf<String>()
                    val wArr = u.optJSONArray("whatToExpect")
                    if (wArr != null) {
                        for (w in 0 until wArr.length()) what.add(wArr.getString(w))
                    }
                    val tips = mutableListOf<String>()
                    val tArr = u.optJSONArray("tips")
                    if (tArr != null) {
                        for (t in 0 until tArr.length()) tips.add(tArr.getString(t))
                    }
                    val known = mutableListOf<String>()
                    val kArr = u.optJSONArray("knownFor")
                    if (kArr != null) {
                        for (k in 0 until kArr.length()) known.add(kArr.getString(k))
                    }

                    list.add(
                        UniversityDetail(
                            id = u.optString("id"),
                            name = u.optString("name"),
                            abbr = u.optString("abbr"),
                            region = u.optString("region"),
                            location = u.optString("location"),
                            website = u.optString("website"),
                            founded = u.optString("founded"),
                            campuses = u.optString("campuses"),
                            climate = u.optString("climate"),
                            elevationM = if (u.has("elevationM") && !u.isNull("elevationM")) u.optInt("elevationM") else null,
                            distanceFromAddisKm = if (u.has("distanceFromAddisKm") && !u.isNull("distanceFromAddisKm")) u.optInt("distanceFromAddisKm") else null,
                            distanceNote = u.optString("distanceNote"),
                            strengths = strengths,
                            whatToExpect = what,
                            tips = tips,
                            studentFit = u.optString("studentFit"),
                            featured = u.optBoolean("featured", false),
                            knownFor = known,
                            detailed = u.optBoolean("detailed", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return Pair(introParagraphs, list)
    }

    fun loadDepartments(context: Context): Pair<List<DepartmentCategoryDetail>, List<DepartmentDetail>> {
        val categories = mutableListOf<DepartmentCategoryDetail>()
        val depts = mutableListOf<DepartmentDetail>()
        try {
            val jsonStr = context.assets.open("data/departments.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)

            val cArr = root.optJSONArray("categories")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val c = cArr.getJSONObject(i)
                    categories.add(
                        DepartmentCategoryDetail(
                            id = c.optString("id"),
                            label = c.optString("label"),
                            blurb = c.optString("blurb")
                        )
                    )
                }
            }

            val dArr = root.optJSONArray("departments")
            if (dArr != null) {
                for (i in 0 until dArr.length()) {
                    val d = dArr.getJSONObject(i)
                    val courses = mutableListOf<String>()
                    val crsArr = d.optJSONArray("courses")
                    if (crsArr != null) {
                        for (c in 0 until crsArr.length()) courses.add(crsArr.getString(c))
                    }

                    val careers = mutableListOf<String>()
                    val carArr = d.optJSONArray("careers")
                    if (carArr != null) {
                        for (c in 0 until carArr.length()) careers.add(carArr.getString(c))
                    }

                    val pros = mutableListOf<String>()
                    val pArr = d.optJSONArray("pros")
                    if (pArr != null) {
                        for (p in 0 until pArr.length()) pros.add(pArr.getString(p))
                    }

                    val cons = mutableListOf<String>()
                    val conArr = d.optJSONArray("cons")
                    if (conArr != null) {
                        for (c in 0 until conArr.length()) cons.add(conArr.getString(c))
                    }

                    depts.add(
                        DepartmentDetail(
                            id = d.optString("id"),
                            name = d.optString("name"),
                            shortName = d.optString("shortName"),
                            categoryId = d.optString("category"),
                            durationYears = d.optString("durationYears"),
                            about = d.optString("about"),
                            courses = courses,
                            careers = careers,
                            market = d.optString("market"),
                            pros = pros,
                            cons = cons
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return Pair(categories, depts)
    }

    /**
     * Success stories and scholarships: REAL items from the website's Supabase table, loaded in the
     * background by [FreeResourcesLive]. The returned lists are Compose state lists (empty until the
     * first successful load, then filled; the last real answer is also kept offline). The invented
     * bundled sample content was removed.
     */
    fun loadFreeResources(context: Context): Pair<List<SuccessStory>, List<ScholarshipOpportunity>> {
        FreeResourcesLive.refresh(context)
        return Pair(FreeResourcesLive.stories, FreeResourcesLive.scholarships)
    }
}
