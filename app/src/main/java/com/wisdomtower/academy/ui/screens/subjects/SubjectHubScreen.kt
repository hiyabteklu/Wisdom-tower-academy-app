package com.wisdomtower.academy.ui.screens.subjects

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.model.ResourceItem
import com.wisdomtower.academy.data.model.ResourceType
import com.wisdomtower.academy.data.repository.WisdomRepository
import com.wisdomtower.academy.ui.screens.packages.getSubjectIcon
import com.wisdomtower.academy.ui.theme.CyanAccent
import com.wisdomtower.academy.ui.theme.CyanPrimary
import com.wisdomtower.academy.ui.theme.EmeraldSuccess
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
fun SubjectHubScreen(
    packageId: String,
    subjectId: String,
    repository: WisdomRepository,
    onNavigateBack: () -> Unit,
    onOpenResource: (ResourceItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var subject by remember(packageId, subjectId) {
        mutableStateOf(
            com.wisdomtower.academy.data.model.SubjectItem(
                id = subjectId,
                packageId = packageId,
                name = com.wisdomtower.academy.data.catalog.WisdomCatalog.formatScopeTitle(subjectId),
                code = subjectId.uppercase()
            )
        )
    }
    var allResources by remember(packageId, subjectId) { mutableStateOf<List<ResourceItem>>(emptyList()) }
    var isLoading by remember(packageId, subjectId) { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(packageId, subjectId) {
        isLoading = true
        val loadedSubject = repository.getSubjectById(packageId, subjectId)
        if (loadedSubject != null) {
            subject = loadedSubject
        }
        allResources = repository.getResourcesForSubject(packageId, subjectId)
        isLoading = false
    }

    val downloadedResources by repository.getAllDownloadedResources().collectAsState(initial = emptyList())
    val downloadedIds = downloadedResources.map { it.id }.toSet()

    var selectedHub by remember { mutableStateOf<ResourceType?>(null) } // null means all hubs

    val filteredResources = if (selectedHub == null) {
        allResources
    } else {
        allResources.filter { it.resourceType == selectedHub }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = subject.code,
                            fontSize = 12.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subject.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
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
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Subject Overview Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    color = NavySurface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f))
                                .border(1.dp, CyanPrimary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getSubjectIcon(subject.iconName),
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = subject.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subject.description,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 5 Hubs Selector Row
            item {
                Column {
                    Text(
                        text = "Curriculum Hubs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedHub == null,
                                onClick = { selectedHub = null },
                                label = { Text("All Hubs (${allResources.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = NavyBackground,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }

                        items(ResourceType.values()) { type ->
                            val count = allResources.count { it.resourceType == type }
                            val icon = getHubIcon(type)
                            FilterChip(
                                selected = selectedHub == type,
                                onClick = { selectedHub = if (selectedHub == type) null else type },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text("${type.label} ($count)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = NavyBackground,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            if (filteredResources.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = NavySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(NavySurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = "Content loads from server",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Content loads from server",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Study notes, PDFs, and practice questions for this course will appear here as soon as they are published to the official academy storage server.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Modules & Learning Resources (${filteredResources.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(filteredResources) { resource ->
                    val isDownloaded = downloadedIds.contains(resource.id)
                    ResourceCard(
                        resource = resource,
                        isDownloaded = isDownloaded,
                        onOpen = { onOpenResource(resource) },
                        onDownload = {
                            coroutineScope.launch {
                                val res = repository.downloadResource(resource)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Saved to offline vault: ${resource.title}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ResourceCard(
    resource: ResourceItem,
    isDownloaded: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    var isDownloading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .testTag("resource_card_${resource.id}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hub Type Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NavySurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getHubIcon(resource.resourceType),
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = resource.resourceType.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }

                // Download Status Badge / Button
                if (isDownloaded) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            isDownloading = true
                            onDownload()
                            isDownloading = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavySurfaceElevated,
                            contentColor = CyanPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("download_resource_${resource.id}")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = CyanPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download for offline",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Offline Save", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = resource.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = resource.summary,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (resource.resourceType) {
                        ResourceType.BOOK_PDF -> "${resource.readingTimeMinutes} min reading • ${(resource.fileSizeBytes / 1024 / 1024.0).let { "%.1f MB".format(it) }}"
                        ResourceType.SHORT_NOTE -> "${resource.readingTimeMinutes} min read • High-Yield Markdown"
                        ResourceType.FLASHCARDS -> "${resource.flashcards.size} Interactive Cards"
                        ResourceType.QUESTION_BANK -> "${resource.quizQuestions.size} Practice Questions"
                        ResourceType.MOCK_EXAM -> "${resource.quizQuestions.size} Exam Questions • 10m Timed"
                        ResourceType.VIDEO -> "Video Lecture • Online Session"
                    },
                    fontSize = 11.sp,
                    color = TextTertiary
                )

                Text(
                    text = "Open →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
        }
    }
}

fun getHubIcon(type: ResourceType): ImageVector {
    return when (type) {
        ResourceType.BOOK_PDF -> Icons.Default.PictureAsPdf
        ResourceType.SHORT_NOTE -> Icons.Default.MenuBook
        ResourceType.FLASHCARDS -> Icons.Default.Style
        ResourceType.QUESTION_BANK -> Icons.Default.HelpCenter
        ResourceType.MOCK_EXAM -> Icons.Default.Timer
        ResourceType.VIDEO -> Icons.Default.PlayCircle
    }
}
