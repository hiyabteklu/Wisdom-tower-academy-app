package com.wisdomtower.academy.ui.screens.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.repository.WisdomRepository
import com.wisdomtower.academy.ui.theme.CyanAccent
import com.wisdomtower.academy.ui.theme.CyanPrimary
import com.wisdomtower.academy.ui.theme.EmeraldSuccess
import com.wisdomtower.academy.ui.theme.GoldAccent
import com.wisdomtower.academy.ui.theme.NavyBackground
import com.wisdomtower.academy.ui.theme.NavyCardBorder
import com.wisdomtower.academy.ui.theme.NavySurface
import com.wisdomtower.academy.ui.theme.NavySurfaceElevated
import com.wisdomtower.academy.ui.theme.NavySurfaceVariant
import com.wisdomtower.academy.ui.theme.RoseError
import com.wisdomtower.academy.ui.theme.TextPrimary
import com.wisdomtower.academy.ui.theme.TextSecondary
import com.wisdomtower.academy.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    resourceId: String,
    repository: WisdomRepository,
    onNavigateBack: () -> Unit
) {
    val userId = repository.sessionManager.getUserId()
    val resource = remember(resourceId) {
        repository.getResourceById(resourceId)
    }
    val cards = remember(resource) { resource?.flashcards ?: emptyList() }

    if (resource == null || cards.isEmpty()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Active Recall Flashcards", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = NavyBackground)
                )
            },
            containerColor = NavyBackground
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Content loads from server", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Interactive flashcards for this topic have not been published to the server yet.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val masteredCards = remember { mutableSetOf<String>() }

    val currentCard = if (cards.isNotEmpty()) cards[currentIndex] else null

    // 3D Flip Animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "card_flip"
    )

    // Save progress when card count finishes
    LaunchedEffect(masteredCards.size) {
        if (cards.isNotEmpty()) {
            val percent = (masteredCards.size * 100) / cards.size
            repository.saveProgress(
                resourceId = resource.id,
                userId = userId,
                progressPercent = percent,
                score = masteredCards.size,
                totalQuestions = cards.size,
                completed = masteredCards.size == cards.size
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = resource.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (cards.isNotEmpty()) "Card ${currentIndex + 1} of ${cards.size}" else "Deck",
                                fontSize = 11.sp,
                                color = CyanAccent
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = NavySurface
                    )
                )

                if (cards.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / cards.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = CyanPrimary,
                        trackColor = NavySurfaceVariant
                    )
                }
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (cards.isEmpty() || currentCard == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No flashcards found for this module.", color = TextSecondary)
                }
            } else {
                // Top Category & Mastery info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavySurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder)
                    ) {
                        Text(
                            text = currentCard.category.ifBlank { "Active Recall" },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    Text(
                        text = "Mastered: ${masteredCards.size}/${cards.size}",
                        fontSize = 12.sp,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The 3D Interactive Flashcard
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable {
                            isFlipped = !isFlipped
                        }
                        .testTag("interactive_flashcard"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rotation <= 90f) NavySurface else NavySurfaceElevated
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (rotation <= 90f) CyanPrimary.copy(alpha = 0.4f) else EmeraldSuccess.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Front of card (Question)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyanPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "QUESTION",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = currentCard.front,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Tap card to reveal answer",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        } else {
                            // Back of card (Answer) - counter rotate 180 to avoid mirroring
                            Column(
                                modifier = Modifier.graphicsLayer { rotationY = 180f },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "ANSWER & RATIONALE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = currentCard.back,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }

                // Hint callout if available
                if (currentCard.hint.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (showHint) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldAccent.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = currentCard.hint, fontSize = 12.sp, color = GoldAccent)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showHint = true },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Show Hint", fontSize = 11.sp, color = GoldAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls: Needs Review vs Mastered & Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            masteredCards.remove(currentCard.id)
                            isFlipped = false
                            showHint = false
                            if (currentIndex < cards.size - 1) currentIndex++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("review_card_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Needs Review", color = RoseError, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            masteredCards.add(currentCard.id)
                            isFlipped = false
                            showHint = false
                            if (currentIndex < cards.size - 1) currentIndex++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mastered_card_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = NavyBackground
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mastered", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
