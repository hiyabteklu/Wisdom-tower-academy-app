package com.wisdomtower.academy.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.auth.SignInNavigator
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMessageCard
import com.wisdomtower.academy.ui.theme.wisdomPressClickable
import com.wisdomtower.academy.ui.theme.wisdomScreenEntrance

/**
 * U1b (2026-11-06, docs/PLAN_2026-11-06_URGENT_FIVE_FIXES.md): what the AI tutor screen shows to a signed-out student
 * instead of the chat. Owner order: the AI tutor (everywhere it can be opened: drawer, Learning tile, "Ask AI tutor" in a
 * note) asks for sign-in first. Nothing else in the app is gated by this.
 *
 * Uses the shared [WisdomMessageCard] with its sign-in pill, so it follows the Night, Day and Transparent looks. "Sign in"
 * first closes this screen, then opens the app's normal sign-in route ([SignInNavigator]), so the student does not come
 * back to a tutor screen that still sits on top of the sign-in page. Back also closes it.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun TutorSignInGate(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Box(modifier = modifier.fillMaxSize().wisdomScreenEntrance()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WisdomMessageCard(
                title = "Sign in to use the AI Tutor",
                message = "The AI Tutor is for signed-in students. Sign in or create a free account to ask questions.",
                showSignIn = true,
                signInLabel = "Sign in",
                onSignIn = {
                    onBack()
                    SignInNavigator.open?.invoke()
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .wisdomPressClickable { onBack() }
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, WisdomCyan.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
                    .background(WisdomCyan.copy(alpha = 0.10f))
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Back", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WisdomCyan)
            }
        }
    }
}
