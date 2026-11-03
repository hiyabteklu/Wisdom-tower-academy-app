package com.wisdomtower.academy.ui.study

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassCard
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Cover Page Generator (owner order 2026-10-16: replaces the "Courses" tile of the Study Tools dock).
 *
 * LAYOUT (owner order 2026-10-16, second request): NO screen header and NO pinned bottom bar. The app's own top bar
 * and bottom navigation stay visible, so this screen keeps every pixel between them for the form. The first thing in
 * the scroll is a compact style card (templates, colours, page borders) followed by the live preview, sized from the
 * space the screen really has so both fit without scrolling. Clear and Download PDF are the last items of the form.
 * Back: the system back gesture (BackHandler) returns to the Learning screen.
 *
 * The drawing code is in CoverPageRenderer.kt: the preview and the PDF come from the same function, so they match.
 * No new dependency, no permission: the logo comes from the system picker (GetContent) and the PDF is saved where
 * the student chooses (CreateDocument, the system "Save as" screen).
 *
 * What is typed stays in [CoverForm] until the app is closed (so Back and reopen keep it). The Clear button empties it.
 *
 * D6i (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. Most colours here were already tokens (glass cards, muted text,
 * cyan accent). What was left: `Color.White` text and field text become `WisdomInk`; the selected-colour ring is white at
 * night and ink by day; the logo box is the quiet glass by day; the filled pill text (`04121F` on the cyan) is white by
 * day (the day cyan is the deep teal); the gold "premium" template label is dark gold `5C3F0A` by day. Night keeps the
 * exact old literals as `dayNight` pairs. NOT changed on purpose: the white paper of the preview and of the template
 * thumbnails (a cover page is a white sheet in both modes) and `CoverPageRenderer.kt` (the cover art and the PDF).
 *
 * Status: written, NOT compiled by the author, not seen on a device.
 */
object CoverForm {
    var logo by mutableStateOf<Bitmap?>(null)
    var university by mutableStateOf("")
    var department by mutableStateOf("")
    var title by mutableStateOf("")
    var isGroup by mutableStateOf(true)
    var groupName by mutableStateOf("")
    val members = mutableStateListOf(CoverMember())
    var studentName by mutableStateOf("")
    var studentId by mutableStateOf("")
    var submittedTo by mutableStateOf("")
    var dateText by mutableStateOf(todayText())
    var templateIndex by mutableIntStateOf(0)
    var accentIndex by mutableIntStateOf(0)
    var borderIndex by mutableIntStateOf(0) // 0 = the template's own default border

    fun clear() {
        logo = null
        university = ""
        department = ""
        title = ""
        isGroup = true
        groupName = ""
        members.clear()
        members.add(CoverMember())
        studentName = ""
        studentId = ""
        submittedTo = ""
        dateText = todayText()
    }
}

private const val MAX_MEMBERS = 20

// ---- D6i day / night colours. Night value = the exact old literal; day value = Ivory & Navy token. Getters: read while composing. ----
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val CvRing: Color get() = dayNight(Color.White, WisdomInk)
private val CvLogoBoxFill: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomGlassFillQuiet)
private val CvPremiumInk: Color get() = dayNight(Color(0xFFE8C766), Color(0xFF5C3F0A))
private val CvPillOnInk: Color get() = dayNight(Color(0xFF04121F), Color.White)

private fun todayText(): String = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date())

private fun showDatePicker(context: Context, onPicked: (String) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val picked = Calendar.getInstance()
            picked.set(year, month, day)
            onPicked(SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(picked.time))
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).show()
}

/** Reads the chosen logo and shrinks it (longest side about 900 px at most) so the PDF stays small. */
private suspend fun loadLogoBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > 900) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null
    }
}

private fun pdfFileName(title: String, department: String): String {
    val base = (if (title.isNotBlank()) title else department)
        .trim()
        .replace(Regex("[^A-Za-z0-9]+"), "_")
        .trim('_')
        .take(30)
    return if (base.isEmpty()) "Cover_Page.pdf" else "Cover_Page_$base.pdf"
}

@Composable
fun NativeCoverPageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val accent = COVER_ACCENTS[CoverForm.accentIndex].color
    val template = CoverForm.templateIndex
    val logoBmp = CoverForm.logo
    val data = CoverPageData(
        universityName = CoverForm.university,
        department = CoverForm.department,
        title = CoverForm.title,
        isGroup = CoverForm.isGroup,
        groupName = CoverForm.groupName,
        members = if (CoverForm.isGroup) {
            CoverForm.members.toList()
        } else {
            listOf(CoverMember(CoverForm.studentName, CoverForm.studentId))
        },
        submittedTo = CoverForm.submittedTo,
        submissionDate = CoverForm.dateText
    )

    // Live preview and the template thumbnails are redrawn a moment after the last change (off the main thread).
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(data, logoBmp, template, CoverForm.accentIndex, CoverForm.borderIndex) {
        delay(140)
        val border = CoverForm.borderIndex
        val big = withContext(Dispatchers.Default) { renderCoverBitmap(data, logoBmp, template, accent, 1.6f, border) }
        val small = withContext(Dispatchers.Default) {
            COVER_TEMPLATES.indices.map { renderCoverBitmap(data, logoBmp, it, accent, 0.34f, border) }
        }
        preview = big
        thumbs = small
    }

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = loadLogoBitmap(context, uri)
                if (bmp != null) {
                    CoverForm.logo = bmp
                } else {
                    Toast.makeText(context, "Could not open that picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            writeCoverPdf(out, data, logoBmp, template, accent, CoverForm.borderIndex)
                            true
                        } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                Toast.makeText(
                    context,
                    if (ok) "Cover page saved as PDF" else "Could not save the PDF",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        // The style card above the preview is about 244 dp tall including gaps; the preview takes the rest of the
        // screen (never wider than the screen), so templates, colours, borders and the whole page fit together.
        val ratio = COVER_PAGE_W.toFloat() / COVER_PAGE_H.toFloat()
        val previewH = (maxHeight.value - 244f).coerceIn(280f, 760f)
        val previewW = minOf(previewH * ratio, maxWidth.value - 24f)

        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Compact style card: template, colour and page border
            item(key = "cover_style") {
                WisdomGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            COVER_TEMPLATES.forEachIndexed { index, t ->
                                TemplateThumb(
                                    name = t.name,
                                    premium = t.premium,
                                    bmp = thumbs.getOrNull(index),
                                    selected = index == template,
                                    onClick = { CoverForm.templateIndex = index }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(text = "Colour", fontSize = 11.sp, color = WisdomMuted)
                            COVER_ACCENTS.forEachIndexed { index, a ->
                                val selected = index == CoverForm.accentIndex
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(a.color))
                                        .border(
                                            BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) CvRing else WisdomGlassBorder),
                                            CircleShape
                                        )
                                        .clickable { CoverForm.accentIndex = index }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Border", fontSize = 11.sp, color = WisdomMuted)
                            COVER_BORDERS.forEachIndexed { index, name ->
                                CoverPill(
                                    text = name,
                                    filled = index == CoverForm.borderIndex,
                                    modifier = Modifier.width(84.dp),
                                    height = 34.dp,
                                    onClick = { CoverForm.borderIndex = index }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live preview (sized to the free screen height). The paper stays white in both modes.
            item(key = "cover_preview") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(previewW.dp)
                            .aspectRatio(ratio)
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    ) {
                        val pb = preview
                        if (pb != null) {
                            Image(
                                bitmap = pb.asImageBitmap(),
                                contentDescription = "Cover page preview",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // 3. Logo
            item(key = "cover_logo") {
                CoverSection("LOGO") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CvLogoBoxFill)
                                .border(BorderStroke(1.dp, WisdomGlassBorder), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val lb = CoverForm.logo
                            if (lb != null) {
                                Image(
                                    bitmap = lb.asImageBitmap(),
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            } else {
                                Text(text = "LOGO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WisdomMuted)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CoverPill(
                                text = if (CoverForm.logo == null) "Choose logo" else "Change logo",
                                filled = true,
                                modifier = Modifier.width(150.dp),
                                onClick = { logoLauncher.launch("image/*") }
                            )
                            if (CoverForm.logo != null) {
                                CoverPill(
                                    text = "Remove",
                                    filled = false,
                                    modifier = Modifier.width(150.dp),
                                    onClick = { CoverForm.logo = null }
                                )
                            }
                        }
                    }
                    Text(
                        text = "A PNG with a transparent background looks best.",
                        fontSize = 11.sp,
                        color = WisdomMuted
                    )
                }
            }

            // 4. Institution and title
            item(key = "cover_institution") {
                CoverSection("INSTITUTION") {
                    CoverField(CoverForm.university, { CoverForm.university = it }, "University name")
                    CoverField(CoverForm.department, { CoverForm.department = it }, "Department")
                    CoverField(CoverForm.title, { CoverForm.title = it }, "Assignment or course title (optional)")
                }
            }

            // 5. Group or individual
            item(key = "cover_people") {
                CoverSection("SUBMITTED BY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoverPill(
                            text = "Group",
                            filled = CoverForm.isGroup,
                            modifier = Modifier.weight(1f),
                            onClick = { CoverForm.isGroup = true }
                        )
                        CoverPill(
                            text = "Individual",
                            filled = !CoverForm.isGroup,
                            modifier = Modifier.weight(1f),
                            onClick = { CoverForm.isGroup = false }
                        )
                    }
                    if (CoverForm.isGroup) {
                        CoverField(CoverForm.groupName, { CoverForm.groupName = it }, "Group name or number")
                        Text(text = "Group members", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WisdomInk)
                        CoverForm.members.forEachIndexed { index, m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 12.sp,
                                    color = WisdomMuted,
                                    modifier = Modifier.width(16.dp)
                                )
                                CoverField(
                                    value = m.name,
                                    onValueChange = { v -> CoverForm.members[index] = CoverForm.members[index].copy(name = v) },
                                    label = "Full name",
                                    modifier = Modifier.weight(1.4f)
                                )
                                CoverField(
                                    value = m.id,
                                    onValueChange = { v -> CoverForm.members[index] = CoverForm.members[index].copy(id = v) },
                                    label = "ID number",
                                    modifier = Modifier.weight(1f),
                                    capitalize = false
                                )
                                if (CoverForm.members.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { CoverForm.members.removeAt(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove member",
                                            tint = WisdomMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (CoverForm.members.size < MAX_MEMBERS) {
                            CoverPill(
                                text = "+ Add member",
                                filled = false,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { CoverForm.members.add(CoverMember()) }
                            )
                        }
                    } else {
                        CoverField(CoverForm.studentName, { CoverForm.studentName = it }, "Student full name")
                        CoverField(CoverForm.studentId, { CoverForm.studentId = it }, "ID number", capitalize = false)
                    }
                }
            }

            // 6. Submitted to and date
            item(key = "cover_submission") {
                CoverSection("SUBMISSION") {
                    CoverField(CoverForm.submittedTo, { CoverForm.submittedTo = it }, "Submitted to (instructor)")
                    CoverField(
                        value = CoverForm.dateText,
                        onValueChange = { CoverForm.dateText = it },
                        label = "Submission date",
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick a date",
                                tint = WisdomCyan,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showDatePicker(context) { picked -> CoverForm.dateText = picked } }
                            )
                        }
                    )
                }
            }

            // 7. Clear and Download live at the end of the form (not pinned above the bottom navigation)
            item(key = "cover_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoverPill(
                        text = "Clear",
                        filled = false,
                        modifier = Modifier.weight(0.45f),
                        height = 48.dp,
                        onClick = { CoverForm.clear() }
                    )
                    CoverPill(
                        text = "Download PDF",
                        filled = true,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        onClick = { saveLauncher.launch(pdfFileName(CoverForm.title, CoverForm.department)) }
                    )
                }
            }

            item(key = "cover_bottom_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CoverSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    WisdomGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomMuted,
                letterSpacing = 0.8.sp
            )
            content()
        }
    }
}

@Composable
private fun TemplateThumb(name: String, premium: Boolean, bmp: Bitmap?, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(66.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_PAGE_W.toFloat() / COVER_PAGE_H.toFloat())
                .clip(shape)
                .background(Color.White)
                .border(
                    BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) WisdomCyan else WisdomGlassBorder),
                    shape
                )
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = name,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (premium) "\u2605 $name" else name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = if (selected) WisdomCyan else if (premium) CvPremiumInk else WisdomInk
        )
    }
}

@Composable
private fun CoverPill(
    text: String,
    filled: Boolean,
    modifier: Modifier,
    height: Dp = 42.dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (filled) WisdomCyan else WisdomGlassFill)
            .border(BorderStroke(1.dp, if (filled) WisdomCyan else WisdomGlassBorder), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) CvPillOnInk else WisdomInk,
            maxLines = 1
        )
    }
}

@Composable
private fun CoverField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    capitalize: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, fontSize = 12.sp, maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(fontSize = 14.sp, color = WisdomInk),
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None
        ),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WisdomInk,
            unfocusedTextColor = WisdomInk,
            focusedBorderColor = WisdomCyan,
            unfocusedBorderColor = WisdomGlassBorder,
            focusedLabelColor = WisdomCyan,
            unfocusedLabelColor = WisdomMuted,
            cursorColor = WisdomCyan,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}
