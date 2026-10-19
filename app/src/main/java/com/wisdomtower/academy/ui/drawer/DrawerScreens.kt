package com.wisdomtower.academy.ui.drawer

import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomOnInk
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.AppThemeMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomReveal
import kotlinx.coroutines.launch

/*
 * Drawer pages. Rewritten 2026-10-11 (owner rule: no invented text).
 * Every text here is copied from the website pages in website-reference/src/app/:
 * about/page.tsx, contact/page.tsx, academy/faq/page.tsx (+ components/AcademyComingSoon.tsx),
 * privacy/page.tsx, terms/page.tsx (the last two live in LegalContent.kt).
 * The website call-to-action buttons ("Create Free Account...", "Explore Academic Pathways" ...)
 * are left out on purpose; they are website navigation.
 *
 * OWNER DECISION 2026-10-12 (About Us only): the numeric claims of the website About page are NOT
 * shown in the app. The four metric cards (30K+, 20+, 100%, 0ms) are replaced by one line,
 * "Trusted by tens of thousands of scholars", and the other numbers in the About text
 * ("100% Aligned", "20+ common freshman courses", "100% focused") were reworded without the number.
 * This is the only intended difference from the website text.
 *
 * 2026-10-16 (UI_AD, M11 slice 5): entrance motion only. The header, the first cards and every card that
 * scrolls into view rise in once per app session with wisdomReveal. No text, layout or logic changed.
 */

@Composable
fun DrawerHeaderBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .wisdomReveal(key = "drawer_header_" + title, index = 0),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WisdomGlassFillStrong)
                .border(BorderStroke(1.dp, WisdomGlassBorder), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = WisdomCyan.copy(alpha = 0.2f)),
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WisdomInk,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = WisdomInk
        )
    }
}

/* ---------------------------------------------------------------------------------------
   Small shared pieces
--------------------------------------------------------------------------------------- */

@Composable
private fun Eyebrow(text: String, color: Color = WisdomCyan) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun AboutSectionHeader(eyebrow: String, title: String, subtitle: String? = null, color: Color = WisdomCyan) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(eyebrow, color)
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WisdomInk, lineHeight = 26.sp)
        if (subtitle != null) {
            Text(subtitle, fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp)
        }
    }
}

/** Full-width text card: optional small top label, title, body. Used for reading text only (never for packages/subjects). */
@Composable
private fun AboutTextCard(
    title: String,
    body: String,
    topLeft: String? = null,
    topRight: String? = null,
    borderColor: Color = WisdomBorderWhite
) {
    WisdomGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wisdomReveal(key = "about_card_" + title, index = 0),
        cornerRadius = 16.dp,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topLeft != null || topRight != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(topLeft.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WisdomAccentAmber)
                    Text(topRight.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomMuted)
                }
            }
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WisdomInk, lineHeight = 19.sp)
            if (body.isNotEmpty()) {
                Text(body, fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp)
            }
        }
    }
}

/* =========================================================================================
   1. ABOUT US  (website-reference/src/app/about/page.tsx; numeric claims left out, see top comment)
========================================================================================= */

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillars = listOf(
        Triple("21st-Century Learning Architecture", "Advanced EdTech", "Learning today requires active cognition, not passive consumption. We fuse adaptive question banks, interactive flashcard recall, and on-demand AI conceptual tutoring into a single frictionless workspace."),
        Triple("Aligned to the New Curriculum", "Official Alignment", "Built from the ground up to reflect Ethiopia’s reformed competence-based modular framework. Every chapter note, practice test, and syllabus track matches the exact standards taught across the nation."),
        Triple("Nationwide Low-Bandwidth Resilience", "Offline-First", "Education must reach every student, regardless of connectivity. Our platform uses edge-caching and offline-first storage so materials remain instantly readable across all regions of Ethiopia."),
        Triple("Beyond Academics: The Whole Scholar", "Holistic Trajectory", "We bridge students from exam halls to real-world impact. Explore verified scholarship databases, university campus guides, career competency prep, and lifelong intellectual networks.")
    )

    val continuum = listOf(
        Triple("Grades 9 – 12", "Secondary Foundation & National Matriculation", "Comprehensive natural and social science curriculum tracks, chapter question banks, model exams, and matriculation preparation."),
        Triple("Remedial & UAT", "Higher Education Transition & Entrance", "Rigorous prerequisite remediation and University Admission Test (UAT) drills designed to secure university placement."),
        Triple("Freshman Core", "First-Year University Excellence", "All common freshman courses with official midterm and final exams worked, Ethiopian GPA tracking, and field leaderboards."),
        Triple("Senior Tracks & Exit Exam", "Department Specialization & Graduation", "Advanced engineering tracks (ECE), professional competency assessments (COC), and National University Exit Exam question banks.")
    )

    val horizon = listOf(
        "Verified Scholarship Portals:" to "Curated listings of fully funded international and domestic scholarship opportunities.",
        "Campus Life & University Field Guides:" to "Deep-dive overviews into Ethiopian public and private universities to help students choose the right environment.",
        "Career Competencies & COC Readiness:" to "Practical guidance and occupational assessments that bridge technical skills to market demand."
    )

    val features = listOf(
        "Ethiopian GPA Scale Engine" to "Calculate, project, and optimize your semester GPA",
        "Field Leaderboards" to "Measure your progress with peers in your discipline",
        "Explain with AI" to "Formative conceptual breakdowns on tricky exam problems"
    )

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                DrawerHeaderBar(title = "About Us", onBack = onBack)
            }

            item(key = "hero") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "about_hero", index = 1),
                    cornerRadius = 22.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Eyebrow("The Next Era of Ethiopian EdTech", WisdomAccentAmber)
                        Text(
                            text = "Elevating Ethiopian Education into a World-Class Sphere.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = WisdomInk,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Wisdom Tower Academy is engineered from first principles around how Ethiopia’s new curriculum works and how modern 21st-century minds learn. We bring all of high school, university entrance, freshman year, and department graduation into one unified, distraction-free ecosystem.",
                            fontSize = 12.5.sp,
                            color = WisdomMuted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Owner decision 2026-10-12: one trust line instead of the four numeric metric cards.
            item(key = "trusted") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "about_trusted", index = 2),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Trusted by tens of thousands of scholars",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomAccentAmber,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item(key = "paradigm") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "about_paradigm", index = 3),
                    cornerRadius = 22.dp,
                    borderColor = WisdomAccentAmber.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Eyebrow("The Paradigm Shift", WisdomAccentAmber)
                        Text(
                            text = "Ending the Era of Scattered PDFs, Chaotic Groups, and Guesswork.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WisdomInk,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = "For years, Ethiopian students have fought an uphill battle: hunting through dozens of random Telegram channels, deciphering low-resolution photocopies of outdated materials, and struggling to find worked solutions that actually explain the underlying science. When the Ethiopian Ministry of Education introduced the comprehensive new curriculum framework, this challenge multiplied.",
                            fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp
                        )
                        Text(
                            text = "We refused to accept that standard. We asked: What if an Ethiopian student had access to a learning environment as refined, fluid, and powerful as the world's leading institutions?",
                            fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp
                        )
                        Text(
                            text = "Wisdom Tower Academy is our answer. A unified platform where textbooks, chapter-by-chapter summaries, question banks, flashcard decks, and official university midterm and final exams live together with clean typography, instant search, and zero distractions.",
                            fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp
                        )
                    }
                }
            }

            item(key = "pillars_header") {
                AboutSectionHeader(
                    eyebrow = "Core Engineering Philosophy",
                    title = "Pillars of the 21st-Century Scholar",
                    subtitle = "Every pixel, algorithm, and curriculum module is designed to give you an unfair intellectual advantage."
                )
            }
            items(pillars.size, key = { "pillar_$it" }) { i ->
                val (t, badge, d) = pillars[i]
                AboutTextCard(title = t, body = d, topLeft = badge)
            }

            item(key = "continuum_header") {
                AboutSectionHeader(
                    eyebrow = "One Unified Journey",
                    title = "High School to Graduation & Beyond",
                    subtitle = "Never start from scratch again. Your notes, your test history, and your study momentum stay with you across every pivotal transition of your academic career.",
                    color = WisdomAccentAmber
                )
            }
            items(continuum.size, key = { "continuum_$it" }) { i ->
                val (level, t, d) = continuum[i]
                AboutTextCard(title = t, body = d, topLeft = "0${i + 1}", topRight = level)
            }

            item(key = "horizon") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "about_horizon", index = 0),
                    cornerRadius = 22.dp,
                    borderColor = WisdomCyan.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Eyebrow("The Broader Horizon")
                        Text(
                            text = "Bridging Ethiopian Students Beyond the Classroom.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WisdomInk,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = "True leadership requires more than acing a test. We equip students with strategic tools to navigate higher education and professional careers:",
                            fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp
                        )
                        horizon.forEach { (lead, text) ->
                            Text(
                                text = buildAnnotatedString {
                                    append("• ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WisdomInk)) { append(lead) }
                                    append(" ")
                                    append(text)
                                },
                                fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            items(features.size, key = { "feature_$it" }) { i ->
                val (t, d) = features[i]
                AboutTextCard(title = t, body = d)
            }

            item(key = "ecosystem") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "about_ecosystem", index = 0),
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Part of the Wisdom Tower Ecosystem",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomInk,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Wisdom Tower Academy is the dedicated educational wing of Wisdom Tower. Our digital agency services, custom software development, and enterprise technologies operate under Wisdom Digital. Here at the Academy, every line of code and every faculty hour is fully focused on student success, academic excellence, and the intellectual future of Ethiopia.",
                            fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp, textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Addis Ababa, Ethiopia • Serving Scholars Nationwide",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WisdomMuted, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item(key = "closing") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "You Belong at the Top of Your Field.",
                        fontSize = 20.sp, fontWeight = FontWeight.Black, color = WisdomInk, textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Stop studying with scattered fragments. Enter a structured, premium ecosystem built to make you thrive.",
                        fontSize = 12.sp, color = WisdomMuted, textAlign = TextAlign.Center, lineHeight = 17.sp
                    )
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/* =========================================================================================
   2. CONTACT  (website-reference/src/app/contact/page.tsx)
   The message is stored in the same Supabase table `inquiries` as the website does (InquirySubmitter).
========================================================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var serviceValue by remember { mutableStateOf("") }
    var serviceLabel by remember { mutableStateOf("Select a topic…") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("idle") } // idle | sending | success | error
    var errorMsg by remember { mutableStateOf("") }
    var topicDropdownExpanded by remember { mutableStateOf(false) }

    // (value stored in the database, label shown) exactly as the website <select>
    val topics = listOf(
        "" to "Select a topic…",
        "packages" to "Packages & pricing",
        "grades-9-12" to "Grades 9–12",
        "freshman" to "Freshman",
        "exit-exam" to "Exit Exam",
        "gat" to "GAT",
        "account-access" to "Account / login help",
        "payment" to "Payment & access",
        "other" to "Other"
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = WisdomCyan,
        unfocusedBorderColor = WisdomBorderWhite,
        focusedTextColor = WisdomInk,
        unfocusedTextColor = WisdomInk,
        focusedContainerColor = WisdomInk.copy(alpha = 0.06f),
        unfocusedContainerColor = WisdomInk.copy(alpha = 0.06f)
    )

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                DrawerHeaderBar(title = "Contact Academy", onBack = onBack)
            }

            if (status == "success") {
                item(key = "success_state") {
                    WisdomGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(WisdomAccentEmerald.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, WisdomAccentEmerald.copy(alpha = 0.4f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WisdomAccentEmerald,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text("Message received", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                            Text(
                                text = "Thanks for reaching out. We aim to reply within 24 hours. You can also explore Academy packages while you wait.",
                                fontSize = 12.sp,
                                color = WisdomMuted,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                                    .clickable { status = "idle" }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text("Send another", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                            }
                        }
                    }
                }
            } else {
                item(key = "intro") {
                    Column(
                        modifier = Modifier.wisdomReveal(key = "contact_intro", index = 1),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Contact Academy", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = WisdomInk)
                        Text(
                            "Questions about pathways, packages, or your account? Send a message; we aim to reply within 24 hours.",
                            fontSize = 13.sp,
                            color = WisdomMuted,
                            lineHeight = 18.sp
                        )
                    }
                }

                item(key = "form_card") {
                    Column(
                        modifier = Modifier.wisdomReveal(key = "contact_form", index = 2),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WisdomInk)
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                placeholder = { Text("Your name", fontSize = 12.sp, color = WisdomMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                singleLine = true
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Email Address", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WisdomInk)
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("you@example.com", fontSize = 12.sp, color = WisdomMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                singleLine = true
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Topic (optional)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WisdomInk)
                            ExposedDropdownMenuBox(
                                expanded = topicDropdownExpanded,
                                onExpandedChange = { topicDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = serviceLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = fieldColors
                                )
                                ExposedDropdownMenu(
                                    expanded = topicDropdownExpanded,
                                    onDismissRequest = { topicDropdownExpanded = false },
                                    modifier = Modifier.background(if (AppThemeMode.isDark) Color(0x4D0F172A) else Color(0xF2FBF9F4))
                                ) {
                                    topics.forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = WisdomInk, fontSize = 12.sp) },
                                            onClick = {
                                                serviceValue = value
                                                serviceLabel = label
                                                topicDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WisdomInk)
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                placeholder = { Text("How can we help? Include your grade/level if relevant.", fontSize = 12.sp, color = WisdomMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                maxLines = 5
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(WisdomCyan.copy(alpha = if (status == "sending") 0.6f else 1f))
                                .clickable {
                                    if (status != "sending") {
                                        if (fullName.isBlank() || email.isBlank() || message.isBlank()) {
                                            errorMsg = "Please fill in your name, email and message."
                                            status = "error"
                                        } else {
                                            status = "sending"
                                            errorMsg = ""
                                            scope.launch {
                                                val error = InquirySubmitter.submit(fullName, email, serviceValue, message)
                                                if (error == null) {
                                                    fullName = ""
                                                    email = ""
                                                    serviceValue = ""
                                                    serviceLabel = "Select a topic…"
                                                    message = ""
                                                    status = "success"
                                                } else {
                                                    errorMsg = error
                                                    status = "error"
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (status == "sending") "Sending..." else "Send Message",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WisdomOnInk
                                )
                                if (status != "sending") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = WisdomOnInk, modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        if (status == "error") {
                            Text(
                                text = errorMsg.ifEmpty { "Something went wrong. Please try again." },
                                color = if (AppThemeMode.isDark) Color(0xFFF87171) else WisdomAccentRose,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/* =========================================================================================
   3. FAQ  (website-reference/src/app/academy/faq/page.tsx is a "Coming soon" page)
   The earlier native FAQ had five invented questions and answers (offline, payments, GPA ...).
   They are gone: the website has no FAQ content yet. Show what the website shows.
========================================================================================= */

@Composable
fun FaqScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                DrawerHeaderBar(title = "FAQ", onBack = onBack)
            }

            item(key = "coming_soon") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "faq_card", index = 1),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Eyebrow("Academy program", WisdomMuted)
                        Text("FAQ", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WisdomAccentAmber)
                        Text(
                            "Answers to common questions about programs, enrollment, and study support.",
                            fontSize = 13.sp,
                            color = WisdomMuted,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Coming soon",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WisdomInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "We're building the full FAQ experience. Materials and enrollment options will appear here shortly.",
                            fontSize = 12.sp,
                            color = WisdomMuted,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/* =========================================================================================
   4. PRIVACY POLICY and 5. TERMS OF SERVICE: one layout, text in LegalContent.kt
========================================================================================= */

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LegalDocumentScreen(
        doc = PRIVACY_DOCUMENT,
        accent = WisdomCyan,
        headerTitle = "Privacy Policy",
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun TermsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LegalDocumentScreen(
        doc = TERMS_DOCUMENT,
        accent = WisdomAccentAmber,
        headerTitle = "Terms of Service",
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun LegalDocumentScreen(
    doc: LegalDocument,
    accent: Color,
    headerTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                DrawerHeaderBar(title = headerTitle, onBack = onBack)
            }

            item(key = "intro_card") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "legal_intro_" + headerTitle, index = 1),
                    cornerRadius = 22.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow(doc.badge, accent)
                        Text(doc.effective, fontSize = 11.sp, color = WisdomMuted)
                        Text(doc.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = WisdomInk, lineHeight = 28.sp)
                        Text(doc.intro, fontSize = 12.sp, color = WisdomMuted, lineHeight = 17.sp)
                        Text(doc.meta, fontSize = 12.sp, color = WisdomMuted)
                    }
                }
            }

            item(key = "summary_card") {
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "legal_summary_" + headerTitle, index = 2),
                    cornerRadius = 18.dp,
                    borderColor = accent.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow(doc.summaryTitle, accent)
                        doc.summaryItems.forEach { (lead, text) ->
                            Text(
                                text = buildAnnotatedString {
                                    append("• ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WisdomInk)) { append(lead) }
                                    append(" ")
                                    append(text)
                                },
                                fontSize = 12.sp,
                                color = WisdomMuted,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            items(doc.sections.size, key = { "section_${doc.sections[it].number}" }) { i ->
                val section = doc.sections[i]
                WisdomGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wisdomReveal(key = "legal_section_" + headerTitle + "_" + section.number, index = 0),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(section.number, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                            Text(
                                section.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WisdomInk,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        section.blocks.forEach { block -> LegalBlockView(block, accent) }
                    }
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LegalBlockView(block: LegalBlock, accent: Color) {
    val uriHandler = LocalUriHandler.current
    when (block) {
        is LegalBlock.Para -> {
            Text(
                text = buildAnnotatedString {
                    if (block.lead.isNotEmpty()) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WisdomInk)) { append(block.lead) }
                        if (block.text.isNotEmpty()) append(" ")
                    }
                    append(block.text)
                },
                fontSize = 12.sp,
                color = WisdomMuted,
                lineHeight = 17.sp,
                fontStyle = if (block.italic) FontStyle.Italic else FontStyle.Normal
            )
        }

        is LegalBlock.Bullets -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.items.forEach { (lead, text) ->
                    Text(
                        text = buildAnnotatedString {
                            append("• ")
                            if (lead.isNotEmpty()) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WisdomInk)) { append(lead) }
                                append(" ")
                            }
                            append(text)
                        },
                        fontSize = 12.sp,
                        color = WisdomMuted,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        is LegalBlock.Card -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WisdomInk.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(block.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                Text(block.text, fontSize = 11.5.sp, color = WisdomMuted, lineHeight = 16.sp)
            }
        }

        is LegalBlock.Contact -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WisdomInk.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, WisdomBorderWhite), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(block.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                block.lines.forEach { line ->
                    Text(line, fontSize = 11.5.sp, color = WisdomMuted)
                }
                block.links.forEach { (prefix, label, uri) ->
                    Row {
                        Text(prefix, fontSize = 11.5.sp, color = WisdomMuted)
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            color = accent,
                            modifier = Modifier.clickable { uriHandler.openUri(uri) }
                        )
                    }
                }
            }
        }
    }
}
