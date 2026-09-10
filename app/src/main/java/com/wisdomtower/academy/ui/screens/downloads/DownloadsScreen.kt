package com.wisdomtower.academy.ui.screens.downloads

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.local.entity.DownloadedResourceEntity
import com.wisdomtower.academy.data.model.ResourceType
import com.wisdomtower.academy.data.repository.WisdomRepository
import com.wisdomtower.academy.ui.screens.subjects.getHubIcon
import com.wisdomtower.academy.ui.theme.CyanAccent
import com.wisdomtower.academy.ui.theme.CyanPrimary
import com.wisdomtower.academy.ui.theme.EmeraldSuccess
import com.wisdomtower.academy.ui.theme.NavyBackground
import com.wisdomtower.academy.ui.theme.NavyCardBorder
import com.wisdomtower.academy.ui.theme.NavySurface
import com.wisdomtower.academy.ui.theme.NavySurfaceElevated
import com.wisdomtower.academy.ui.theme.NavySurfaceVariant
import com.wisdomtower.academy.ui.theme.RoseError
import com.wisdomtower.academy.ui.theme.TextPrimary
import com.wisdomtower.academy.ui.theme.TextSecondary
import com.wisdomtower.academy.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    repository: WisdomRepository,
    onNavigateBack: () -> Unit,
    onOpenDownloadedResource: (DownloadedResourceEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadedItems by repository.getAllDownloadedResources().collectAsState(initial = emptyList())

    val totalSizeBytes = remember(downloadedItems) {
        downloadedItems.sumOf { it.fileSizeBytes }
    }
    val formattedStorageSize = remember(totalSizeBytes) {
        val mb = totalSizeBytes / 1024 / 1024.0
        if (mb < 0.1) "${totalSizeBytes / 1024} KB" else "%.2f MB".format(mb)
    }

    var showPurgeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Encrypted Offline Vault",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${downloadedItems.size} Resources • $formattedStorageSize",
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
                    if (downloadedItems.isNotEmpty()) {
                        IconButton(
                            onClick = { showPurgeDialog = true },
                            modifier = Modifier.testTag("purge_downloads_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Purge All",
                                tint = RoseError
                            )
                        }
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
                // Vault Storage Overview Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    color = NavySurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyanPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Private App Vault",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Total Space Used: $formattedStorageSize",
                                        fontSize = 12.sp,
                                        color = CyanAccent
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sandboxed", fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "All downloaded PDFs and study modules are stored in your device's isolated app directory. Inaccessible to file managers and protected against unauthorized exports.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (downloadedItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = NavySurface
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Offline Vault is Empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Download books, notes, flashcards, or question banks from any Subject Hub to study completely offline.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Saved Resources (${downloadedItems.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(downloadedItems, key = { it.id }) { item ->
                    val type = try {
                        ResourceType.valueOf(item.resourceType)
                    } catch (e: Exception) {
                        ResourceType.SHORT_NOTE
                    }
                    val formattedDate = remember(item.downloadTimestamp) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.downloadTimestamp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(14.dp))
                            .clickable { onOpenDownloadedResource(item) }
                            .testTag("downloaded_item_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getHubIcon(type),
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.subjectName,
                                    fontSize = 11.sp,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Saved $formattedDate • ${(item.fileSizeBytes / 1024.0).let { if (it > 1024) "%.1f MB".format(it / 1024) else "%.0f KB".format(it) }}",
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.deleteDownloadedResource(item.id)
                                        Toast.makeText(context, "Deleted from vault", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete offline copy",
                                    tint = RoseError.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Purge confirmation dialog
    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = {
                Text(
                    text = "Purge All Offline Downloads?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will delete all $formattedStorageSize of locally cached textbooks, notes, and exams. You can re-download them anytime.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeDialog = false
                        coroutineScope.launch {
                            repository.fileStorage.wipeAllSecureFiles()
                            downloadedItems.forEach { repository.deleteDownloadedResource(it.id) }
                            Toast.makeText(context, "All offline files deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Purge All", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavySurfaceElevated
        )
    }
}
