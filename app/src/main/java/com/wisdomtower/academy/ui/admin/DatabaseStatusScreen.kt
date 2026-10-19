package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.DatabaseStatusRepository
import com.wisdomtower.academy.data.admin.DbTableState
import com.wisdomtower.academy.data.admin.DbTableStatus
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary

// ADMIN-native N10b (docs/ADMIN_NATIVE_PLAN.md). READ-ONLY database status. Replaces the website Database and SQL Hub
// for the app: it shows how many rows each tracked table has and whether the table answered, and nothing else.
// There is no SQL script, no copy button, no link to the Supabase SQL Editor, no setup steps and no button that
// writes. The owner is the only person who works on the database; other admins only see this status.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

@Composable
internal fun DatabaseStatusScreen(modifier: Modifier = Modifier) {
    var results by remember { mutableStateOf<List<DbTableStatus>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        results = DatabaseStatusRepository.check()
        loading = false
    }

    val current = results

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Database status",
                        color = WisdomTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Read only. This page only counts the rows of each table. It cannot run SQL or change any data.",
                        color = WisdomMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { reloadKey += 1 }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = WisdomMuted,
                        modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                    )
                }
            }
        }

        if (current == null) {
            item(key = "loading") {
                Text(
                    text = "Checking tables...",
                    color = WisdomMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        } else {
            item(key = "summary") {
                val okCount = current.count { it.state == DbTableState.OK }
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(WisdomCard)
                        .border(1.dp, WisdomBorderWhite, shape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Database health", color = WisdomMuted, fontSize = 12.sp)
                    Text(
                        text = "$okCount / ${current.size}",
                        color = if (okCount == current.size) WisdomAccentEmerald else WisdomAccentAmber,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (okCount == current.size) {
                            "All tracked tables answered"
                        } else {
                            "${current.size - okCount} table(s) did not answer normally"
                        },
                        color = WisdomMuted,
                        fontSize = 13.sp
                    )
                }
            }
            items(current, key = { it.table }) { row ->
                TableStatusCard(row = row, dim = loading)
            }
        }
    }
}

@Composable
private fun TableStatusCard(row: DbTableStatus, dim: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    val tone = when (row.state) {
        DbTableState.OK -> WisdomAccentEmerald
        DbTableState.MISSING -> WisdomAccentAmber
        else -> AdminRose
    }
    val badge = when (row.state) {
        DbTableState.OK -> "Active"
        DbTableState.MISSING -> "Table missing"
        DbTableState.DENIED -> "No permission"
        DbTableState.UNREACHABLE -> "Unreachable"
        DbTableState.ERROR -> "Error"
    }
    val count = row.rowCount
    val countText = if (row.state != DbTableState.OK || count == null) {
        "-"
    } else if (count == 1) {
        "1 row"
    } else {
        "$count rows"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.6f else 1f)
            .clip(shape)
            .background(WisdomCard)
            .border(1.dp, WisdomBorderWhite, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "public.${row.table}",
            color = WisdomCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = row.label,
            color = WisdomTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = row.description, color = WisdomMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminToneBadge(text = badge, tone = tone)
            Text(text = countText, color = WisdomMuted, fontSize = 13.sp)
        }
        if (row.state != DbTableState.OK && row.detail.isNotEmpty()) {
            Text(text = row.detail, color = tone, fontSize = 12.sp)
        }
    }
}
