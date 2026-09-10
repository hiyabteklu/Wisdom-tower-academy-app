package com.wisdomtower.academy.ui.screens.home

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.model.PackageItem
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: WisdomRepository,
    onNavigateToPackage: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userId = repository.sessionManager.getUserId()
    val userEmail = repository.sessionManager.getUserEmail()
    val userName = repository.sessionManager.getUserDisplayName()

    val packages by repository.getPackagesFlow(userId).collectAsState(initial = emptyList())
    val downloadedResources by repository.getAllDownloadedResources().collectAsState(initial = emptyList())

    var isOfflineModeActive by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var wipeOfflineOnLogout by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            repository.syncRemoteEnrollments(userId)
        }
    }

    val displayedPackages = if (isOfflineModeActive) {
        val downloadedPkgIds = downloadedResources.map { it.packageId }.toSet()
        packages.filter { downloadedPkgIds.contains(it.id) }
    } else {
        packages
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f))
                                .border(1.dp, CyanPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Wisdom Tower Academy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Addis Ababa • Higher Education",
                                fontSize = 10.sp,
                                color = CyanAccent
                            )
                        }
                    }
                },
                actions = {
                    // Vault / Downloads icon with badge
                    IconButton(
                        onClick = onNavigateToDownloads,
                        modifier = Modifier.testTag("open_downloads_button")
                    ) {
                        Box {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = "Offline Downloads",
                                tint = if (downloadedResources.isNotEmpty()) CyanPrimary else TextSecondary
                            )
                            if (downloadedResources.isNotEmpty()) {
                                Badge(
                                    containerColor = CyanPrimary,
                                    contentColor = NavyBackground,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = "${downloadedResources.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = TextSecondary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Scholar Welcome Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
                    color = NavySurface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Welcome, $userName",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (userEmail.isNotBlank()) userEmail else "Scholar Session",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            // Security badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = NavySurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Dev Mode (Screenshots Allowed)",
                                        fontSize = 10.sp,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Offline toggle chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = isOfflineModeActive,
                                onClick = { isOfflineModeActive = !isOfflineModeActive },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isOfflineModeActive) Icons.Default.WifiOff else Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isOfflineModeActive) "Offline Mode (Active)" else "Offline Mode Filter",
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPrimary,
                                    containerColor = NavySurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isOfflineModeActive,
                                    selectedBorderColor = CyanPrimary,
                                    borderColor = NavyCardBorder
                                )
                            )

                            Text(
                                text = "${downloadedResources.size} Saved Offline",
                                fontSize = 12.sp,
                                color = CyanAccent,
                                modifier = Modifier.clickable { onNavigateToDownloads() }
                            )
                        }
                    }
                }
            }

            // Offline banner when active
            if (isOfflineModeActive) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        color = GoldAccent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = "Offline Banner",
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Offline Mode Enabled",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Displaying only downloaded resources saved in your private encrypted vault.",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Academic Pathways & Packages",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${displayedPackages.size} Available",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (displayedPackages.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        color = NavySurface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (isOfflineModeActive) Icons.Default.CloudDownload else Icons.Default.School,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isOfflineModeActive) "No Offline Packages Found" else "Loading Supabase Catalog...",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOfflineModeActive) "Turn off Offline Mode to browse all online curriculum packages." else "Connecting to live academic catalog items...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            items(displayedPackages) { pkg ->
                PackageCard(
                    packageItem = pkg,
                    onClick = { onNavigateToPackage(pkg.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out of Wisdom Tower",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to sign out? Your session tokens will be securely purged from EncryptedSharedPreferences.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { wipeOfflineOnLogout = !wipeOfflineOnLogout }
                    ) {
                        Checkbox(
                            checked = wipeOfflineOnLogout,
                            onCheckedChange = { wipeOfflineOnLogout = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Also purge downloaded offline files from this device",
                            fontSize = 12.sp,
                            color = if (wipeOfflineOnLogout) RoseError else TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            repository.sessionManager.clearSession(wipeOfflineCache = wipeOfflineOnLogout)
                            onLogout()
                        }
                    }
                ) {
                    Text("Sign Out", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavySurfaceElevated
        )
    }
}

@Composable
fun PackageCard(
    packageItem: PackageItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("package_card_${packageItem.id}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (packageItem.isFree) EmeraldSuccess.copy(alpha = 0.15f) else CyanPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (packageItem.isFree) EmeraldSuccess.copy(alpha = 0.6f) else CyanPrimary.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = packageItem.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (packageItem.isFree) EmeraldSuccess else CyanPrimary
                    )
                }

                // Ownership status badge
                if (packageItem.isOwned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Owned",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (packageItem.isFree) "Free Unlocked" else "Owned",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${packageItem.priceEtb} ETB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = packageItem.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = packageItem.description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (packageItem.subjectsCount == 0) {
                    Text(
                        text = "Content coming soon from server",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                } else {
                    Row {
                        Text(
                            text = "${packageItem.subjectsCount} Subjects",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Text(
                            text = "${packageItem.totalResourcesCount}+ Modules",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = if (packageItem.subjectsCount == 0) "View Track →" else if (packageItem.isOwned) "Open Hub →" else "Unlock Access →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
        }
    }
}
