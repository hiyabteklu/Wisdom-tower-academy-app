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

/** Bold roasting + emotion faces. */
private val EXIT_LINES = listOf(
    ExitLine("\uD83D\uDE11", "Leaving already? Your future self just facepalmed."),
    ExitLine("\uD83D\uDE0F", "One more chapter and you might actually pass. Just saying."),
    ExitLine("\uD83D\uDE10", "The exam does not care that you were tired. Stay."),
    ExitLine("\uD83E\uDD28", "Closing the app does not close the semester. Nice try."),
    ExitLine("\uD83D\uDE0E", "Your competition is still studying. Cute exit attempt though."),
    ExitLine("\uD83D\uDE44", "Wisdom Tower noticed you were about to ghost your goals."),
    ExitLine("\uD83E\uDD14", "Plot twist: the material does not learn itself."),
    ExitLine("\uD83D\uDE24", "You opened this for a reason. Do not betray that version of you."),
    ExitLine("\uD83D\uDE0B", "Exit if you must. The leaderboard will remember."),
    ExitLine("\uD83D\uDE12", "Skipping study mode? Bold strategy. Let's see how grades feel about it."),
    ExitLine("\uD83D\uDE22", "Your notes are crying softly in the background."),
    ExitLine("\uD83D\uDE36", "One click from growth, one click from regret. Choose wisely."),
    ExitLine("\uD83D\uDE29", "The AI tutor was mid-sentence. Rude."),
    ExitLine("\uD83D\uDE15", "Quitting mid-session is how average happens."),
    ExitLine("\uD83D\uDE05", "Stay. Your GPA has trust issues already."),
    ExitLine("\uD83E\uDD2D", "You can leave. Or you can be the student who did not."),
    ExitLine("\uD83D\uDE4F", "Tomorrow-you is begging today-you to stay five more minutes."),
    ExitLine("\uD83D\uDE21", "This is not Netflix. Closing does not pause the exam date."),
    ExitLine("\uD83D\uDE08", "Fine, leave. We will just unlock packages without you."),
    ExitLine("\uD83D\uDE31", "Really? After all that loading? At least finish the page."),
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
                    text = "Really leaving?",
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
                        text = "I will stay and learn",
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
                        text = "Exit anyway",
                        color = ExitMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
