package com.wisdomtower.academy.ui.screens.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.repository.WisdomRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    resourceId: String,
    repository: WisdomRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userId = repository.sessionManager.getUserId()
    val userEmail = repository.sessionManager.getUserEmail()

    val resource = remember(resourceId) {
        repository.getResourceById(resourceId)
    }
    val downloadedItem by repository.getDownloadedResource(resourceId).collectAsState(initial = null)
    val isDownloaded = downloadedItem != null

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPagesCount by remember { mutableIntStateOf(1) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val activeTitle = resource?.title ?: downloadedItem?.title ?: "Document Viewer"
    val activeSubject = resource?.subjectName ?: downloadedItem?.subjectName ?: "Academy Track"

    // Prepare PDF in app-private storage
    LaunchedEffect(resourceId) {
        withContext(Dispatchers.IO) {
            val existing = repository.fileStorage.getResourceFile(resourceId, "pdf")
            if (existing != null && existing.exists()) {
                pdfFile = existing
                isLoading = false
            } else if (resource != null) {
                val result = repository.downloadResource(resource)
                if (result.isSuccess) {
                    pdfFile = repository.fileStorage.getResourceFile(resourceId, "pdf")
                } else {
                    loadError = result.exceptionOrNull()?.localizedMessage ?: "No document file uploaded to server yet."
                }
                isLoading = false
            } else {
                loadError = "Document not found."
                isLoading = false
            }
        }
    }

    // Render current page using Android PdfRenderer
    LaunchedEffect(pdfFile, currentPageIndex) {
        val file = pdfFile ?: return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        totalPagesCount = renderer.pageCount
                        val safeIndex = currentPageIndex.coerceIn(0, renderer.pageCount - 1)
                        val page = renderer.openPage(safeIndex)
                        val densityMultiplier = 2 // High DPI crisp rendering
                        val width = page.width * densityMultiplier
                        val height = page.height * densityMultiplier
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderedBitmap = bitmap
                    }
                }
                repository.saveProgress(
                    resourceId = resource?.id ?: downloadedItem?.id ?: resourceId,
                    userId = userId,
                    progressPercent = ((currentPageIndex + 1).toFloat() / totalPagesCount.toFloat() * 100).toInt(),
                    lastPage = currentPageIndex + 1,
                    totalPages = totalPagesCount
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
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
                            text = if (renderedBitmap != null) "Page ${currentPageIndex + 1} of $totalPagesCount • In-App Viewer" else activeSubject,
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
                                val currentId = resource?.id ?: downloadedItem?.id ?: resourceId
                                if (isDownloaded) {
                                    repository.deleteDownloadedResource(currentId)
                                    Toast.makeText(context, "Removed from offline cache", Toast.LENGTH_SHORT).show()
                                } else if (resource != null) {
                                    val res = repository.downloadResource(resource)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Saved to offline vault", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("toggle_download_pdf_button")
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = "Save Offline",
                            tint = if (isDownloaded) EmeraldSuccess else CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavySurface
                )
            )
        },
        bottomBar = {
            // PDF Page Navigation Controls
            Surface(
                color = NavySurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NavyCardBorder),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) currentPageIndex--
                        },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.testTag("pdf_prev_page_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) CyanPrimary else TextSecondary.copy(alpha = 0.4f)
                        )
                    }

                    // Zoom Controls & Page Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) }
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextSecondary)
                        }

                        Text(
                            text = "${currentPageIndex + 1} / $totalPagesCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.0f) }
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextSecondary)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentPageIndex < totalPagesCount - 1) currentPageIndex++
                        },
                        enabled = currentPageIndex < totalPagesCount - 1,
                        modifier = Modifier.testTag("pdf_next_page_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < totalPagesCount - 1) CyanPrimary else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading Academic Document...",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            } else if (renderedBitmap != null) {
                val bitmap = renderedBitmap!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Rendered PDF Page with Anti-Piracy Overlay
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth(zoomScale)
                                .height((bitmap.height / (bitmap.width / 360f)).dp * zoomScale),
                            contentScale = ContentScale.Fit
                        )

                        // Subtle Anti-Leak Security Watermark Overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = "Licensed to ${userEmail.ifBlank { "Scholar" }} • Wisdom Tower",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Content loads from server",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PDF documents for this subject have not been uploaded to the academy server yet.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
