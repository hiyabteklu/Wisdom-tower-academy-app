package com.wisdomtower.academy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * O2 (docs/ONBOARDING_STARTUP_PLAN.md, owner order 2026-10-23): three animated screens instead of five.
 *  1. Welcome (brand logo with soft pulse rings)
 *  2. The journey: one glowing line draws down through the stages, each stage lights up as the line reaches it
 *  3. Everything in one place: the logo in the middle, the tools turning around it on two rings
 * Only transform / alpha / Canvas progress animation (cheap). If the system animation scale is 0 the end state is shown.
 * Text comes from the owner's words and the old welcome text. Not compiled, not seen on a phone when written.
 * Public API unchanged: OnboardingScreen(onFinished), hasCompletedOnboarding, setOnboardingCompleted.
 */
private val OnboardBg = Color(0xFF060B15)
private val OnboardAccent = Color(0xFF22E0FF)
private val OnboardGold = Color(0xFFFBBF24)
private val OnboardCard = Color(0xCC0C1424)
private val OnboardMuted = Color(0xFF94A3B8)
private val OnboardTrack = Color(0xFF1E293B)
private val OnboardDim = Color(0xFF334155)

private const val PAGE_COUNT = 3

/** Journey stages, in the owner's words. ORDER IS A PROPOSAL until the owner confirms it. */
private val journeyStops = listOf(
    "Grades 9\u201312",
    "EUEE",
    "Remedial",
    "Freshman",
    "University",
    "Exit Exam",
)
private val journeyExtras = listOf("COC", "GAT")

private class OrbitTool(val label: String, val icon: ImageVector)

private val innerTools = listOf(
    OrbitTool("AI Tutor", Icons.Filled.AutoAwesome),
    OrbitTool("Planner", Icons.Filled.CalendarMonth),
    OrbitTool("Notes", Icons.Filled.EditNote),
)
private val outerTools = listOf(
    OrbitTool("Scholarship", Icons.Filled.School),
    OrbitTool("Info", Icons.Filled.Info),
    OrbitTool("Tools", Icons.Filled.Calculate),
    OrbitTool("Resources", Icons.AutoMirrored.Filled.MenuBook),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    val animate = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onFinished()
    }

    fun finishOnboarding() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onFinished()
    }

    fun next() {
        if (page < PAGE_COUNT - 1) page++ else finishOnboarding()
    }

    fun prev() {
        if (page > 0) page--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1220), OnboardBg, Color(0xFF0B1628))
                )
            )
            .pointerInput(page) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            drag < -80f -> next()
                            drag > 80f -> prev()
                        }
                        drag = 0f
                    },
                    onHorizontalDrag = { _, delta ->
                        drag += delta
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page < PAGE_COUNT - 1) {
                    TextButton(onClick = { finishOnboarding() }) {
                        Text("Skip", color = OnboardMuted, fontSize = 14.sp)
                    }
                }
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(280)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
                },
                label = "onboardPage",
                modifier = Modifier.weight(1f)
            ) { index ->
                when (index) {
                    0 -> WelcomePage(animate)
                    1 -> JourneyPage(animate)
                    else -> OrbitPage(animate)
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                for (i in 0 until PAGE_COUNT) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == page) OnboardAccent else OnboardDim)
                    )
                }
            }

            if (page < PAGE_COUNT - 1) {
                Button(
                    onClick = { next() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnboardAccent,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { finishOnboarding() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnboardAccent,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Start learning", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onFinished() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Not now", color = OnboardMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PageTitle(text: String, size: Int = 24) {
    Text(
        text = text,
        color = Color.White,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        lineHeight = (size + 6).sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

// ---------------------------------------------------------------------------------------------------------------
// Screen 1: welcome
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun WelcomePage(animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "welcomeRings")
    val ringPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "ringPhase"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                val strokeW = 2.dp.toPx()
                for (k in 0..1) {
                    val ph = if (animate) (ringPhase.value + k * 0.5f) % 1f else 0.4f + k * 0.3f
                    drawCircle(
                        color = OnboardAccent.copy(alpha = (1f - ph) * 0.35f),
                        radius = maxR * (0.45f + 0.55f * ph),
                        center = center,
                        style = Stroke(width = strokeW)
                    )
                }
            }
            BrandLoader(size = 150.dp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        PageTitle("Welcome to\nWisdom Tower Academy", size = 28)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Your complete academic companion for Ethiopia.",
            color = OnboardMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Screen 2: the journey (one continuous animation)
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun JourneyPage(animate: Boolean) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            delay(350)
            progress.animateTo(1f, tween(3400, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val n = journeyStops.size
    // g runs from 0 (first stage) to n - 1 (last stage); stage i is reached when g == i
    val g = progress.value * (n - 1)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PageTitle("Every stage of your journey")
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.width(240.dp)) {
            journeyStops.forEachIndexed { i, label ->
                val lit = ((g - i) * 4f + 1f).coerceIn(0f, 1f)
                val above = if (i == 0) 0f else (g - (i - 1)).coerceIn(0f, 1f)
                val below = if (i == n - 1) 0f else (g - i).coerceIn(0f, 1f)
                JourneyRow(
                    label = label,
                    lit = lit,
                    above = above,
                    below = below,
                    isFirst = i == 0,
                    isLast = i == n - 1,
                    finalStop = i == n - 1
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val chipAlpha = ((progress.value - 0.92f) / 0.08f).coerceIn(0f, 1f)
        Row(
            modifier = Modifier.alpha(chipAlpha),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            journeyExtras.forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(OnboardGold.copy(alpha = 0.12f))
                        .border(1.dp, OnboardGold.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = OnboardGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun JourneyRow(
    label: String,
    lit: Float,
    above: Float,
    below: Float,
    isFirst: Boolean,
    isLast: Boolean,
    finalStop: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val w = 3.dp.toPx()
            if (!isFirst) drawLine(OnboardTrack, Offset(cx, 0f), Offset(cx, cy), w)
            if (!isLast) drawLine(OnboardTrack, Offset(cx, cy), Offset(cx, size.height), w)

            // each segment between two stages is drawn half in the upper stage row and half in the lower one
            val upper = (above * 2f - 1f).coerceIn(0f, 1f)
            val lower = (below * 2f).coerceIn(0f, 1f)
            if (!isFirst && upper > 0f) {
                drawLine(OnboardAccent, Offset(cx, 0f), Offset(cx, cy * upper), w)
                if (upper < 1f) drawCircle(Color.White.copy(alpha = 0.9f), 3.5.dp.toPx(), Offset(cx, cy * upper))
            }
            if (!isLast && lower > 0f) {
                val endY = cy + (size.height - cy) * lower
                drawLine(OnboardAccent, Offset(cx, cy), Offset(cx, endY), w)
                if (lower < 1f) drawCircle(Color.White.copy(alpha = 0.9f), 3.5.dp.toPx(), Offset(cx, endY))
            }

            val nodeColor = if (finalStop) OnboardGold else OnboardAccent
            if (lit > 0f) {
                drawCircle(
                    nodeColor.copy(alpha = 0.25f * lit),
                    13.dp.toPx() * (0.6f + 0.4f * lit),
                    Offset(cx, cy)
                )
                drawCircle(nodeColor, 7.dp.toPx(), Offset(cx, cy))
            } else {
                drawCircle(OnboardDim, 6.dp.toPx(), Offset(cx, cy))
            }
        }
        Text(
            text = label,
            color = if (finalStop) OnboardGold else Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 14.dp)
                .alpha(lit)
                .graphicsLayer { translationX = (1f - lit) * 24f }
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Screen 3: everything in one place (tools orbiting the brand)
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun OrbitPage(animate: Boolean) {
    val entry = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) entry.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) else entry.snapTo(1f)
    }
    val transition = rememberInfiniteTransition(label = "orbit")
    val innerPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "innerPhase"
    )
    val outerPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(36000, easing = LinearEasing)),
        label = "outerPhase"
    )
    var selected by remember { mutableStateOf<String?>(null) }
    val innerRadius = 84.dp
    val outerRadius = 128.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PageTitle("Everything in one place")
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val e = entry.value
                val sw = 1.dp.toPx()
                drawCircle(
                    OnboardAccent.copy(alpha = 0.18f * e),
                    innerRadius.toPx() * e,
                    center,
                    style = Stroke(width = sw)
                )
                drawCircle(
                    OnboardAccent.copy(alpha = 0.12f * e),
                    outerRadius.toPx() * e,
                    center,
                    style = Stroke(width = sw)
                )
            }
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(OnboardAccent.copy(alpha = 0.30f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            BrandLogo(size = 70.dp)

            innerTools.forEachIndexed { i, tool ->
                val base = i * (360f / innerTools.size) - 90f
                OrbitIcon(
                    tool = tool,
                    radius = innerRadius,
                    angleDeg = { base + (if (animate) innerPhase.value else 0f) * 360f },
                    entry = { entry.value },
                    onClick = { selected = tool.label }
                )
            }
            outerTools.forEachIndexed { i, tool ->
                val base = i * (360f / outerTools.size) - 45f
                OrbitIcon(
                    tool = tool,
                    radius = outerRadius,
                    angleDeg = { base - (if (animate) outerPhase.value else 0f) * 360f },
                    entry = { entry.value },
                    onClick = { selected = tool.label }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val shown = selected
        if (shown != null) {
            Text(shown, color = OnboardAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(
                text = "AI Tutor \u00B7 Planner \u00B7 Notes \u00B7 Scholarship \u00B7 Info \u00B7 Tools \u00B7 Resources",
                color = OnboardMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun OrbitIcon(
    tool: OrbitTool,
    radius: Dp,
    angleDeg: () -> Float,
    entry: () -> Float,
    onClick: () -> Unit,
) {
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    Box(
        modifier = Modifier
            .graphicsLayer {
                val a = Math.toRadians(angleDeg().toDouble())
                val e = entry()
                translationX = (cos(a) * radiusPx * e).toFloat()
                translationY = (sin(a) * radiusPx * e).toFloat()
                alpha = e
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(OnboardCard)
            .border(1.dp, OnboardAccent.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.label,
            tint = OnboardAccent,
            modifier = Modifier.size(22.dp)
        )
    }
}

fun hasCompletedOnboarding(context: android.content.Context): Boolean {
    return context.getSharedPreferences("wta_prefs", android.content.Context.MODE_PRIVATE)
        .getBoolean("onboarding_done", false)
}

fun setOnboardingCompleted(context: android.content.Context) {
    context.getSharedPreferences("wta_prefs", android.content.Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_done", true)
        .apply()
}
