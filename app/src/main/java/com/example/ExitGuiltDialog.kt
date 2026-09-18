package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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

/** 20 roasting / guilt-trip exit lines. One is picked at random each time. */
private val EXIT_LINES = listOf(
    "Leaving already? Your future self just facepalmed.",
    "One more chapter and you might actually pass. Just saying.",
    "The exam does not care that you were tired. Stay.",
    "Closing the app does not close the semester. Nice try.",
    "Your competition is still studying. Cute exit attempt though.",
    "Wisdom Tower noticed you were about to ghost your goals.",
    "Plot twist: the material does not learn itself.",
    "You opened this for a reason. Do not betray that version of you.",
    "Exit if you must. The leaderboard will remember.",
    "Skipping study mode? Bold strategy. Let's see how grades feel about it.",
    "Your notes are crying softly in the background.",
    "One click from growth, one click from regret. Choose wisely.",
    "The AI tutor was mid-sentence. Rude.",
    "Quitting mid-session is how average happens.",
    "Stay. Your GPA has trust issues already.",
    "Even the loading GIF looks disappointed in you.",
    "You can leave. Or you can be the student who did not.",
    "Tomorrow-you is begging today-you to stay five more minutes.",
    "This is not Netflix. Closing does not pause the exam date.",
    "Fine, leave. We will just be here... unlocking packages without you.",
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
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SentimentDissatisfied,
                        contentDescription = null,
                        tint = ExitAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Really leaving?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = line,
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
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I will stay and learn", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Exit anyway", color = ExitDanger, fontSize = 14.sp)
                }
            }
        }
    }
}
