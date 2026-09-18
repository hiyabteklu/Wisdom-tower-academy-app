package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val OnboardBg = Color(0xFF0F172A)
private val OnboardAccent = Color(0xFF00E5FF)
private val OnboardCard = Color(0xFF1E293B)
private val OnboardMuted = Color(0xFF94A3B8)

private data class OnboardPage(
    val title: String,
    val body: String,
    val icon: ImageVector? = null,
    val showGif: Boolean = false,
)

private val pages = listOf(
    OnboardPage(
        title = "Welcome to\nWisdom Tower Academy",
        body = "Your complete academic companion for Ethiopia.",
        showGif = true,
    ),
    OnboardPage(
        title = "Everything for your journey",
        body = "The most complete online learning platform in Ethiopia. Pathways, packages, and materials for your entire academic path.",
        icon = Icons.Filled.School,
    ),
    OnboardPage(
        title = "Zero margin for error",
        body = "Accuracy-first learning materials built for real exams and real results.",
        icon = Icons.Filled.Verified,
    ),
    OnboardPage(
        title = "Powerful AI, free with your course",
        body = "Built-in AI support for every purchased course. Study smarter, not harder.",
        icon = Icons.Filled.AutoAwesome,
    ),
    OnboardPage(
        title = "Stay on track",
        body = "Turn on notifications for learning updates, exams, opportunities, and tips that help you improve.",
        icon = Icons.Filled.Notifications,
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }

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
        if (page < pages.lastIndex) page++ else finishOnboarding()
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (page < pages.lastIndex) {
                    TextButton(onClick = { finishOnboarding() }) {
                        Text("Skip", color = OnboardMuted, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(280)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
                },
                label = "onboardPage",
                modifier = Modifier.weight(1f)
            ) { index ->
                val p = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (p.showGif) {
                        BrandLoader(size = 160.dp)
                        Spacer(modifier = Modifier.height(28.dp))
                    } else if (p.icon != null) {
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "cardScale"
                        )
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .size(120.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(OnboardCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = p.icon,
                                    contentDescription = null,
                                    tint = OnboardAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Text(
                        text = p.title,
                        color = Color.White,
                        fontSize = if (p.showGif) 28.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = if (p.showGif) 34.sp else 30.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = p.body,
                        color = OnboardMuted,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == page) OnboardAccent else Color(0xFF334155))
                    )
                }
            }

            if (page < pages.lastIndex) {
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
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Not now", color = OnboardMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
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
