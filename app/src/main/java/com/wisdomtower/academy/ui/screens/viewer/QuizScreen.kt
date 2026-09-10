package com.wisdomtower.academy.ui.screens.viewer

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    resourceId: String,
    isTimed: Boolean,
    repository: WisdomRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userId = repository.sessionManager.getUserId()

    val resource = remember(resourceId) {
        repository.getResourceById(resourceId)
    }
    val questions = remember(resource) { resource?.quizQuestions ?: emptyList() }

    if (resource == null || questions.isEmpty()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (isTimed) "Timed Mock Exam" else "Practice Question Bank", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                        "Exam questions for this topic have not been published to the server yet.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> selectedOptionIndex
    var isSubmitted by remember { mutableStateOf(false) }

    // Timed Exam countdown: 600 seconds (10 minutes)
    var remainingSeconds by remember { mutableIntStateOf(600) }

    LaunchedEffect(isTimed, isSubmitted) {
        if (isTimed && !isSubmitted) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            // Auto submit when time expires
            isSubmitted = true
        }
    }

    val currentQuestion = if (questions.isNotEmpty()) questions[currentQuestionIndex] else null
    val score = remember(isSubmitted) {
        if (!isSubmitted) 0
        else {
            questions.indices.count { idx ->
                userAnswers[idx] == questions[idx].correctIndex
            }
        }
    }

    // Save exam attempt in database when submitted
    LaunchedEffect(isSubmitted) {
        if (isSubmitted && questions.isNotEmpty()) {
            val percent = (score * 100) / questions.size
            repository.saveProgress(
                resourceId = resource.id,
                userId = userId,
                progressPercent = percent,
                score = score,
                totalQuestions = questions.size,
                completed = true
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
                            if (isTimed && !isSubmitted) {
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                Text(
                                    text = "Timer: %02d:%02d".format(mins, secs),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingSeconds < 60) RoseError else GoldAccent
                                )
                            } else {
                                Text(
                                    text = if (questions.isNotEmpty()) "Question ${currentQuestionIndex + 1} of ${questions.size}" else "Quiz",
                                    fontSize = 11.sp,
                                    color = CyanAccent
                                )
                            }
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

                if (questions.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size.toFloat() },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (questions.isEmpty() || currentQuestion == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No quiz questions available for this module.", color = TextSecondary)
                }
            } else if (isSubmitted) {
                // Exam Results Screen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (score >= questions.size / 2) EmeraldSuccess.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (score >= questions.size / 2) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = if (score >= questions.size / 2) EmeraldSuccess else GoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (score >= questions.size / 2) "Exam Passed!" else "Review Recommended",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Score: $score / ${questions.size} (${(score * 100) / questions.size}%)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (score >= questions.size / 2) EmeraldSuccess else GoldAccent,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = "Recorded in your offline academic progress report.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                userAnswers.clear()
                                isSubmitted = false
                                currentQuestionIndex = 0
                                remainingSeconds = 600
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = NavyBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake Assessment", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Question Review list
                Text(
                    text = "Question-by-Question Review",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                questions.forEachIndexed { index, q ->
                    val chosen = userAnswers[index]
                    val isCorrect = chosen == q.correctIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isCorrect) EmeraldSuccess.copy(alpha = 0.5f) else RoseError.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Q${index + 1}", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 12.sp)
                                Text(
                                    text = if (isCorrect) "Correct (+1)" else "Incorrect",
                                    color = if (isCorrect) EmeraldSuccess else RoseError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(q.question, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Correct Answer: ${q.options[q.correctIndex]}",
                                color = EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!isCorrect && chosen != null) {
                                Text(
                                    text = "Your Answer: ${q.options[chosen]}",
                                    color = RoseError,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NavySurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Explanation: ${q.explanation}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Active Question View
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "QUESTION ${currentQuestionIndex + 1} OF ${questions.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }

                            if (!isTimed) {
                                Text(
                                    text = "Practice Bank",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentQuestion.question,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }

                // Options List
                val selectedOption = userAnswers[currentQuestionIndex]
                val showExplanation = !isTimed && selectedOption != null

                currentQuestion.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedOption == optIndex
                    val isCorrectOption = optIndex == currentQuestion.correctIndex

                    val borderColor = when {
                        showExplanation && isCorrectOption -> EmeraldSuccess
                        showExplanation && isSelected && !isCorrectOption -> RoseError
                        isSelected -> CyanPrimary
                        else -> NavyCardBorder
                    }

                    val containerColor = when {
                        showExplanation && isCorrectOption -> EmeraldSuccess.copy(alpha = 0.15f)
                        showExplanation && isSelected && !isCorrectOption -> RoseError.copy(alpha = 0.15f)
                        isSelected -> CyanPrimary.copy(alpha = 0.15f)
                        else -> NavySurface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !showExplanation) {
                                userAnswers[currentQuestionIndex] = optIndex
                            }
                            .testTag("option_${currentQuestionIndex}_$optIndex"),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CyanPrimary else NavySurfaceVariant)
                                    .border(1.dp, if (isSelected) CyanPrimary else NavyCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ('A' + optIndex).toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NavyBackground else TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Practice Explanation Callout
                if (showExplanation) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NavySurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Answer Rationale:",
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion.explanation,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Navigation controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentQuestionIndex > 0) currentQuestionIndex--
                        },
                        enabled = currentQuestionIndex > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Previous")
                    }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex++ },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = NavyBackground
                            ),
                            modifier = Modifier.testTag("next_question_button")
                        ) {
                            Text("Next Question", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { isSubmitted = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = NavyBackground
                            ),
                            modifier = Modifier.testTag("submit_exam_button")
                        ) {
                            Text("Submit & Grade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
