package com.wisdomtower.academy.ui.screens.viewer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.wisdomtower.academy.ui.theme.TextPrimary
import com.wisdomtower.academy.ui.theme.TextSecondary
import com.wisdomtower.academy.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesViewerScreen(
    resourceId: String,
    repository: WisdomRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userId = repository.sessionManager.getUserId()

    val resource = remember(resourceId) {
        repository.getResourceById(resourceId)
    }
    val downloadedItem by repository.getDownloadedResource(resourceId).collectAsState(initial = null)
    val isDownloaded = downloadedItem != null

    if (resource == null && downloadedItem == null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Study Notes", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                        "Study notes for this topic have not been published to the server yet.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val activeTitle = resource?.title ?: downloadedItem?.title ?: "Study Notes"
    val activeSubject = resource?.subjectName ?: downloadedItem?.subjectName ?: "Academy Course"
    val activeId = resource?.id ?: downloadedItem?.id ?: resourceId

    // Markdown content (either from local offline room cache or remote resource)
    val markdownRaw = downloadedItem?.textContent ?: resource?.markdownContent ?: ""
    val paragraphs = remember(markdownRaw) { if (markdownRaw.isNotBlank()) markdownRaw.split("\n\n") else emptyList() }

    val listState = rememberLazyListState()

    // Calculate reading progress percentage without floating overlay covering text
    val readingProgressPercent by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) 0
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / totalItems.toFloat() * 100).toInt().coerceIn(0, 100)
            }
        }
    }

    // Save reading progress periodically
    LaunchedEffect(readingProgressPercent) {
        if (readingProgressPercent > 5) {
            repository.saveProgress(
                resourceId = activeId,
                userId = userId,
                progressPercent = readingProgressPercent,
                completed = readingProgressPercent >= 95
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
                                text = activeTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "$activeSubject • $readingProgressPercent% read",
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
                    actions = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (isDownloaded) {
                                        repository.deleteDownloadedResource(activeId)
                                        Toast.makeText(context, "Removed from offline vault", Toast.LENGTH_SHORT).show()
                                    } else if (resource != null) {
                                        val res = repository.downloadResource(resource)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Saved for offline reading!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Download failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("toggle_download_note_button")
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = "Download for offline",
                                tint = if (isDownloaded) EmeraldSuccess else CyanPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = NavySurface
                    )
                )

                // Clean fixed linear progress bar at the very top edge, NOT floating over content
                LinearProgressIndicator(
                    progress = { readingProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = CyanPrimary,
                    trackColor = NavySurfaceVariant,
                )
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Note Metadata Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NavySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Curated High-Yield Notes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Estimated read: ${resource?.readingTimeMinutes ?: 5} min • Private In-App Reader",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (paragraphs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NavySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Content loads from server", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("The lecture notes for this section will be downloaded once available.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(paragraphs.size) { index ->
                    val block = paragraphs[index].trim()
                    MarkdownBlockRenderer(block = block)
                }
            }

            item {
                // Completed Reading Footer
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "End of Study Note",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Progress saved automatically to your offline scholar profile.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Anti-Leak Protected by Wisdom Tower",
                                fontSize = 10.sp,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun MarkdownBlockRenderer(block: String) {
    when {
        block.startsWith("# ") -> {
            Text(
                text = block.removePrefix("# ").trim(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary,
                lineHeight = 28.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        block.startsWith("## ") -> {
            Text(
                text = block.removePrefix("## ").trim(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        block.startsWith("### ") -> {
            Text(
                text = block.removePrefix("### ").trim(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        block.startsWith("> ") -> {
            // Callout / Exam Tip box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldAccent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = block.removePrefix("> ").trim(),
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    color = GoldAccent,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        block.startsWith("---") -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NavyCardBorder)
            )
        }
        block.startsWith("$$") || block.contains("=") -> {
            // Mathematical formula / code block
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NavySurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = block.replace("$$", "").trim(),
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    lineHeight = 19.sp
                )
            }
        }
        block.contains("* ") || block.contains("- ") -> {
            // Bullet list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.lines().forEach { line ->
                    val cleanLine = line.trimStart('*', '-', ' ').trim()
                    if (cleanLine.isNotBlank()) {
                        Row(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = "• ",
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = cleanLine,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // Standard body paragraph
            Text(
                text = block,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}
