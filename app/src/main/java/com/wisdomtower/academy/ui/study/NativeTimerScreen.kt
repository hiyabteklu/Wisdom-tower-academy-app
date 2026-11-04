package com.wisdomtower.academy.ui.study

import com.wisdomtower.academy.ui.theme.WisdomPopupFill

import androidx.activity.compose.BackHandler
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentSky
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.wisdomDialogEnter
import kotlin.math.roundToInt

/**
 * Native port of the website's focus timer (owner problem 1, 2026-10-12, F7 part 2):
 * `website-reference/src/components/learning/PomodoroTimer.tsx` inside the page frame of
 * `website-reference/src/app/learning/LearningContent.tsx` ("Focus Pomodoro Station" header with a Close button).
 * Same presets (Focus 25, Short 5, Long 15, Sprint 45, Custom 1 to 360 minutes), same ring, same clock, same
 * Ready / Focus / Done label, same start line while running, same Pause / Start / Reset buttons, same footer sentence and
 * the same "quit?" pop-up (face, title, text, "Keep studying" / "Pause anyway" / "Reset anyway") with the website's
 * 8 start lines and 25 pop-up lines. The rules live in [NativeFocusTimer] (port of `lib/focus-timer.ts`).
 * Not ported: the phone vibration at the end of a session (needs the VIBRATE permission; the app sends its
 * "session finished" notification instead, from the shell) and the website's sub-title, which the website hides on phones.
 *
 * D6e (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode. Night keeps the exact old literals as `dayNight` pairs. By day: the
 * card, chips and buttons are the ivory glass; the clock and texts are ink or muted slate; the ring arc, the Start / Set /
 * Keep studying buttons and the selected chip use the deep teal (white text on the solid ones); the quit pop-up is the
 * 95 percent ivory overlay. The two big composables were re-bracketed while editing (the old file had a mis-indented block);
 * the layout is the same.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun NativeTimerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { NativeFocusTimer.init(context) }

    val left = NativeFocusTimer.seconds
    val totalSec = NativeFocusTimer.totalSec
    val running = NativeFocusTimer.running && left > 0

    var startLine by remember { mutableStateOf<String?>(null) }
    var nudge by remember { mutableStateOf<FocusNudge?>(null) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var customOpen by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(30) }

    val isPreset = FOCUS_PRESETS.any { it.minutes * 60 == totalSec }
    val progress = if (totalSec > 0) (1f - left.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f) else 0f

    fun choosePreset(minutes: Int) {
        NativeFocusTimer.setPreset(minutes * 60)
        startLine = null
        customOpen = false
    }

    fun startSession() {
        NativeFocusTimer.start()
        startLine = FOCUS_START_LINES.random()
    }

    fun requestStop(kind: String) {
        if (!NativeFocusTimer.running) {
            if (kind == "reset") {
                NativeFocusTimer.reset()
                startLine = null
            }
            return
        }
        pendingAction = kind
        nudge = FOCUS_NUDGE_LINES.random()
    }

    fun confirmStop() {
        if (pendingAction == "pause") NativeFocusTimer.pause()
        if (pendingAction == "reset") NativeFocusTimer.reset()
        pendingAction = null
        nudge = null
        startLine = null
    }

    fun keepGoing() {
        pendingAction = null
        nudge = null
    }

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Shared top toolbar: back arrow + title (48dp touch target, top-left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScreenBackButton(onBack)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = WisdomAccentSky, modifier = Modifier.size(20.dp))
                    Text("Focus Pomodoro Station", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card with the timer itself
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(TmCardFill)
                            .border(BorderStroke(1.dp, TmCardBorder), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Presets: Focus 25, Short 5, Long 15, Sprint 45, Custom
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FOCUS_PRESETS.take(3).forEach { p ->
                                    TimerChip(
                                        label = p.label,
                                        selected = totalSec == p.minutes * 60,
                                        enabled = !running,
                                        onClick = { choosePreset(p.minutes) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FOCUS_PRESETS.drop(3).forEach { p ->
                                    TimerChip(
                                        label = p.label,
                                        selected = totalSec == p.minutes * 60,
                                        enabled = !running,
                                        onClick = { choosePreset(p.minutes) }
                                    )
                                }
                                TimerChip(
                                    label = if (!isPreset) "Custom (${(totalSec / 60.0).roundToInt()}m)" else "Custom",
                                    selected = customOpen || !isPreset,
                                    enabled = !running,
                                    icon = Icons.Filled.Tune,
                                    onClick = { customOpen = !customOpen }
                                )
                            }
                        }

                        // Inline custom minutes selector
                        if (customOpen && !running) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 20.dp)
                                    .widthIn(max = 280.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(TmInnerFill)
                                    .border(BorderStroke(1.dp, TmInnerBorder), RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Custom Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TmSlate300)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BasicTextField(
                                        value = customMinutes.toString(),
                                        onValueChange = { v ->
                                            customMinutes = (v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 1).coerceIn(1, 360)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = WisdomInk,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(WisdomInk),
                                        modifier = Modifier
                                            .width(80.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(TmFieldFill)
                                            .border(BorderStroke(1.dp, TmFieldBorder), RoundedCornerShape(50))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                    Text("min", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TmSlate300)
                                    TimerPillButton(
                                        label = "Set",
                                        icon = Icons.Filled.Check,
                                        bg = TmPrimaryBg,
                                        borderColor = TmPrimaryBg,
                                        textColor = TmPrimaryInk,
                                        hPad = 14.dp,
                                        vPad = 7.dp,
                                        fontSize = 12.sp,
                                        onClick = { choosePreset(customMinutes) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Ring with the clock and the Ready / Focus / Done label
                        FocusRing(
                            progress = progress,
                            clock = formatFocusClock(left),
                            label = if (running) "Focus" else if (left == 0) "Done" else "Ready"
                        )

                        val line = startLine
                        if (line != null && running) {
                            Text(
                                text = line,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TmSlate200,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp,
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .widthIn(max = 360.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pause (or Start) and Reset
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (running) {
                                TimerPillButton(
                                    label = "Pause",
                                    icon = Icons.Filled.Pause,
                                    bg = TmPauseFill,
                                    borderColor = TmPauseBorder,
                                    textColor = WisdomInk,
                                    hPad = 28.dp,
                                    onClick = { requestStop("pause") }
                                )
                            } else {
                                TimerPillButton(
                                    label = "Start",
                                    icon = Icons.Filled.PlayArrow,
                                    bg = TmPrimaryBg,
                                    borderColor = TmPrimaryBg,
                                    textColor = TmPrimaryInk,
                                    enabled = left > 0,
                                    hPad = 28.dp,
                                    onClick = { startSession() }
                                )
                            }
                            TimerPillButton(
                                label = "Reset",
                                icon = Icons.Filled.Refresh,
                                bg = TmQuietFill,
                                borderColor = TmQuietBorder,
                                textColor = TmSlate300,
                                hPad = 20.dp,
                                onClick = { requestStop("reset") }
                            )
                        }

                        Text(
                            text = "Timer continues running across all subjects and notes.",
                            fontSize = 12.sp,
                            color = TmSlate400,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }

    val shownNudge = nudge
    if (shownNudge != null) {
        NudgeDialog(
            nudge = shownNudge,
            confirmLabel = if (pendingAction == "reset") "Reset anyway" else "Pause anyway",
            onKeep = { keepGoing() },
            onConfirm = { confirmStop() }
        )
    }
}

// ---- D6e day / night colours. Night value = the exact old literal; day value = Ivory & Navy token. Getters: read while composing / drawing. ----
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val TmSlate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val TmSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomMuted)
private val TmSlate200: Color get() = dayNight(Color(0xFFE2E8F0), WisdomInk)
private val TmCardFill: Color get() = dayNight(Color.White.copy(alpha = 0.03f), WisdomGlassFill)
private val TmCardBorder: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomGlassBorder)
private val TmInnerFill: Color get() = dayNight(Color.White.copy(alpha = 0.05f), WisdomGlassFillQuiet)
private val TmInnerBorder: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val TmFieldFill: Color get() = dayNight(Color(0x4D020617).copy(alpha = 0.80f), WisdomGlassFillStrong)
private val TmFieldBorder: Color get() = dayNight(Color.White.copy(alpha = 0.15f), WisdomGlassBorder)
private val TmPrimaryBg: Color get() = dayNight(Color.White, WisdomCyan)
private val TmPrimaryInk: Color get() = dayNight(Color(0xFF020617), Color.White)
private val TmPauseFill: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassFillStrong)
private val TmPauseBorder: Color get() = dayNight(Color.White.copy(alpha = 0.15f), WisdomGlassBorder)
private val TmQuietFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFillQuiet)
private val TmQuietBorder: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val TmRingTrack: Color get() = dayNight(Color.White.copy(alpha = 0.06f), WisdomInk.copy(alpha = 0.12f))
private val TmRingArc: Color get() = dayNight(Color.White, WisdomCyan)
private val TmChipOnFill: Color get() = dayNight(Color.White.copy(alpha = 0.20f), WisdomCyan.copy(alpha = 0.16f))
private val TmChipOnBorder: Color get() = dayNight(Color.White.copy(alpha = 0.30f), WisdomCyan.copy(alpha = 0.50f))
private val TmChipOffFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFillQuiet)
private val TmChipOffBorder: Color get() = dayNight(Color.White.copy(alpha = 0.08f), WisdomGlassBorder)
private val TmDialogFill: Color get() = WisdomPopupFill
private val TmDialogBorder: Color get() = dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder)
private val TmDialogQuietFill: Color get() = dayNight(Color.White.copy(alpha = 0.05f), WisdomGlassFillQuiet)
private val TmDialogQuietBorder: Color get() = dayNight(Color.White.copy(alpha = 0.15f), WisdomGlassBorder)

private data class FocusPreset(val label: String, val minutes: Int)

private data class FocusNudge(val face: String, val title: String, val body: String)

private val FOCUS_PRESETS = listOf(
    FocusPreset("Focus 25", 25),
    FocusPreset("Short 5", 5),
    FocusPreset("Long 15", 15),
    FocusPreset("Sprint 45", 45)
)

private fun formatFocusClock(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return m.toString().padStart(2, '0') + ":" + s.toString().padStart(2, '0')
}

@Composable
private fun FocusRing(progress: Float, clock: String, label: String) {
    Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Same geometry as the website SVG: 120 x 120 box, radius 54, stroke 8, starts at the top, clockwise
            val strokeWidth = size.minDimension * (8f / 120f)
            val radius = size.minDimension * (54f / 120f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            drawArc(
                color = TmRingTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            if (progress > 0f) {
                drawArc(
                    color = TmRingArc,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = clock,
                style = TextStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = WisdomInk,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1
            )
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = TmSlate400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TimerChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.wisdomTouchFeel()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (selected) TmChipOnFill else TmChipOffFill)
            .border(
                BorderStroke(1.dp, if (selected) TmChipOnBorder else TmChipOffBorder),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) WisdomInk else TmSlate300,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) WisdomInk else TmSlate300,
            maxLines = 1
        )
    }
}

@Composable
private fun TimerPillButton(
    label: String,
    icon: ImageVector,
    bg: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    hPad: Dp = 24.dp,
    vPad: Dp = 13.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.wisdomTouchFeel()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = hPad, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
        Text(text = label, fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
    }
}

@Composable
private fun NudgeDialog(
    nudge: FocusNudge,
    confirmLabel: String,
    onKeep: () -> Unit,
    onConfirm: () -> Unit
) {
    // Like the website pop-up, only the two buttons close it (Back counts as "Keep studying").
    Dialog(onDismissRequest = onKeep) {
        Column(
            modifier = Modifier
                .wisdomDialogEnter()
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TmDialogFill)
                .border(BorderStroke(1.dp, TmDialogBorder), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = nudge.face, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = nudge.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WisdomInk,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = nudge.body,
                fontSize = 12.sp,
                color = TmSlate300,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.wisdomTouchFeel()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(TmPrimaryBg)
                    .clickable { onKeep() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Keep studying", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TmPrimaryInk)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier.wisdomTouchFeel()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(TmDialogQuietFill)
                    .border(BorderStroke(1.dp, TmDialogQuietBorder), RoundedCornerShape(50))
                    .clickable { onConfirm() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(confirmLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TmSlate300)
            }
        }
    }
}

/** `FOCUS_START_LINES` of `website-reference/src/lib/focus-timer.ts`, word for word. */
private val FOCUS_START_LINES = listOf(
    "One block of focus now beats a whole evening of half-work.",
    "Stay with this session. Future you will thank you.",
    "Quiet phone. Open notes. This hour is yours.",
    "You already started. That is the hard part.",
    "Small steady work beats last-minute panic.",
    "This is practice for the person you want to become.",
    "Keep going. Clarity comes after you sit with the work.",
    "Your classmates are studying too. Match their effort."
)

/** `FOCUS_NUDGE_LINES` of `website-reference/src/lib/focus-timer.ts`, word for word. */
private val FOCUS_NUDGE_LINES = listOf(
    FocusNudge("😤", "Already quitting?", "You set a timer, then tried to ghost it. Finish the block. Your future self is watching."),
    FocusNudge("😔", "Walking away early", "The notes did not insult you. Sit back down and make them useful."),
    FocusNudge("🫠", "Procrastination in a nice outfit", "Calling this a break does not make it one. The work is still on the desk."),
    FocusNudge("👀", "Still time on the clock", "TikTok will be there in twenty minutes. Your exam will not reschedule for you."),
    FocusNudge("😕", "Almost there", "You already paid the start cost. Stopping now is paying twice for half the result."),
    FocusNudge("🤨", "Bold strategy", "Set a focus timer, then leave. Historians will call this peak procrastination."),
    FocusNudge("😅", "The phone is not a study partner", "It will still be full of notifications after you finish this session. The chapter will not."),
    FocusNudge("😴", "Sudden fatigue detected", "Funny how tired only shows up when the notes open. Stay. Energy returns after page two."),
    FocusNudge("🧐", "Checking the time again?", "The timer is fine. Your attention is the one wandering. Bring it back."),
    FocusNudge("😬", "This is the hard minute", "Everyone wants to quit right here. The ones who pass are the ones who do not."),
    FocusNudge("🫡", "Discipline check", "Motivation left the chat. Good. Discipline is what finishes timers."),
    FocusNudge("🙃", "Nice try", "You cannot negotiate with a countdown. Either finish it or admit you were never serious."),
    FocusNudge("😤", "Respect the timer", "It is a short session, not a prison sentence. Hold the line."),
    FocusNudge("🫣", "Sneaking out?", "We see you. The progress bar sees you. Stay and make this session count."),
    FocusNudge("😌", "Rest is earned", "Breaks feel better after real work. Finish this block, then rest without the guilt."),
    FocusNudge("🧠", "Brain wants comfort", "Comfort is free. Competence is not. One more solid stretch before you leave."),
    FocusNudge("🔥", "Do not cool off now", "You finally got into the work. Walking away resets the engine. Keep the heat."),
    FocusNudge("😏", "Classic move", "Start strong, quit mid-way, regret at night. Skip step two this time."),
    FocusNudge("📱", "The scroll can wait", "Nothing urgent is happening on your phone. Something urgent is happening on this page."),
    FocusNudge("🏆", "Small win available", "Finishing this timer is a win you can actually feel. Quitting is free and empty."),
    FocusNudge("🫠", "Future you is unimpressed", "Tonight-you wants ease. Exam-you wants this work done. Pick a side."),
    FocusNudge("😤", "Not this again", "Same timer, same early exit, same score later. Break the pattern today."),
    FocusNudge("😄", "Laugh, then stay", "Yes, studying is annoying. So is failing. Choose the temporary annoyance."),
    FocusNudge("💪", "One more push", "You are closer to done than to the start. Do not waste the distance you already covered.")
)
