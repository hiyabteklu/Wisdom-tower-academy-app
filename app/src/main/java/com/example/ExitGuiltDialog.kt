package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.random.Random

private val ExitBg = Color(0xFF0F172A)
private val ExitAccent = Color(0xFF00E5FF)
private val ExitMuted = Color(0xFF94A3B8)
private val ExitSoft = Color(0xFFCBD5E1)

private data class ExitLine(val emoji: String, val message: String)

/** Emotion expression emojis only. Mix of teasing, encouragement, dry humor. */
private val EXIT_LINES = listOf(
    ExitLine("\uD83D\uDE0A", "You were doing great. Five more minutes will not hurt."),
    ExitLine("\uD83D\uDE42", "Your books are still open. So is your potential."),
    ExitLine("\uD83E\uDD14", "Breaks are fine. Quitting the whole session is optional."),
    ExitLine("\uD83D\uDE0F", "One short review now beats a long panic later."),
    ExitLine("\uD83D\uDE09", "Future you will thank present you for staying a bit."),
    ExitLine("\uD83D\uDE0C", "Grab water, stretch, then come back. We will wait."),
    ExitLine("\uD83D\uDE07", "Leaving is allowed. Coming back stronger is better."),
    ExitLine("\uD83D\uDE4F", "You already started. That is the hardest part."),
    ExitLine("\uD83E\uDD29", "Small progress today still counts toward the goal."),
    ExitLine("\uD83D\uDE10", "Okay, go rest. Just do not forget where you left off."),
    ExitLine("\uD83D\uDE4C", "Consistency beats intensity. A little more goes a long way."),
    ExitLine("\uD83E\uDD28", "If you leave now, who will unlock the next milestone?"),
    ExitLine("\uD83D\uDE0E", "The exam date did not move. Your focus still can."),
    ExitLine("\uD83D\uDE0D", "Students who show up again are the ones who pass."),
    ExitLine("\uD83E\uDD17", "Be kind to your goals. Give them a few more minutes."),
    ExitLine("\uD83D\uDE24", "You came here with intent. Keep a little of that energy."),
    ExitLine("\uD83E\uDD13", "Your brain likes finishing loops. Close this one nicely."),
    ExitLine("\uD83D\uDE05", "No pressure. Just a friendly nudge to stay on track."),
    ExitLine("\uD83D\uDE03", "Champions take breaks. They also return to the work."),
    ExitLine("\uD83D\uDE01", "Almost done for today. Finish strong, then rest well."),
)

@Composable
fun ExitGuiltDialog(
    onStay: () -> Unit,
    onExit: () -> Unit,
) {
    val line = remember { EXIT_LINES[Random.nextInt(EXIT_LINES.size)] }

    Dialog(
        onDismissRequest = onStay,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF162032),
                            Color(0xFF0F172A),
                            Color(0xFF0B1220),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                BrandLoader(size = 96.dp, showCard = false)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = line.emoji,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Heading out?",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = 0.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = line.message,
                    color = ExitSoft,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    letterSpacing = 0.15.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = onStay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExitAccent,
                        contentColor = ExitBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Keep learning",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Exit for now",
                        color = ExitMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
