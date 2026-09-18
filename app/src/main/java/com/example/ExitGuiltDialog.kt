package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.random.Random

private val ExitBg = Color(0xFF0F172A)
private val ExitCard = Color(0xFF1E293B)
private val ExitAccent = Color(0xFF00E5FF)
private val ExitMuted = Color(0xFF94A3B8)
private val ExitDanger = Color(0xFFF87171)

private data class ExitLine(val emoji: String, val message: String)

/** Mix of teasing, encouragement, and dry humor. One picked at random each exit. */
private val EXIT_LINES = listOf(
    ExitLine("\u2728", "You were doing great. Five more minutes will not hurt."),
    ExitLine("\uD83D\uDCDA", "Your books are still open. So is your potential."),
    ExitLine("\uD83C\uDFC3", "Breaks are fine. Quitting the whole session is optional."),
    ExitLine("\uD83D\uDCA1", "One short review now beats a long panic later."),
    ExitLine("\uD83C\uDF1F", "Future you will thank present you for staying a bit."),
    ExitLine("\u2615", "Grab water, stretch, then come back. We will wait."),
    ExitLine("\uD83D\uDE0A", "Leaving is allowed. Coming back stronger is better."),
    ExitLine("\uD83D\uDCD6", "You already started. That is the hardest part."),
    ExitLine("\uD83C\uDFAF", "Small progress today still counts toward the goal."),
    ExitLine("\uD83D\uDC4B", "Okay, go rest. Just do not forget where you left off."),
    ExitLine("\uD83D\uDE4C", "Consistency beats intensity. A little more goes a long way."),
    ExitLine("\uD83E\uDD14", "If you leave now, who will unlock the next milestone?"),
    ExitLine("\uD83D\uDE09", "The exam date did not move. Your focus still can."),
    ExitLine("\uD83C\uDF93", "Students who show up again are the ones who pass."),
    ExitLine("\uD83D\uDC99", "Be kind to your goals. Give them a few more minutes."),
    ExitLine("\uD83D\uDD25", "You came here with intent. Keep a little of that energy."),
    ExitLine("\uD83E\uDDE0", "Your brain likes finishing loops. Close this one nicely."),
    ExitLine("\uD83D\uDE42", "No pressure. Just a friendly nudge to stay on track."),
    ExitLine("\uD83C\uDFC6", "Champions take breaks. They also return to the work."),
    ExitLine("\uD83D\uDE80", "Almost done for today. Finish strong, then rest well."),
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
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ExitCard)
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                BrandLoader(size = 88.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = line.emoji,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Heading out?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = line.message,
                    color = ExitMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onStay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExitAccent,
                        contentColor = ExitBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Keep learning", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Exit for now", color = ExitDanger, fontSize = 14.sp)
                }
            }
        }
    }
}
