package com.wisdomtower.academy.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.AdminAppRelease
import com.wisdomtower.academy.data.admin.AppReleasesRepository
import com.wisdomtower.academy.data.admin.AppReleasesResult
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomCard
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.launch

@Composable
fun AppUpdatesScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var releases by remember { mutableStateOf<List<AdminAppRelease>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var versionCode by remember { mutableStateOf("4") }
    var versionName by remember { mutableStateOf("") }
    var changelog by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }
    var telegramUrl by remember { mutableStateOf("") }
    var minVersionCode by remember { mutableStateOf("3") }
    var published by remember { mutableStateOf(false) }

    fun clearEditor() {
        versionCode = ((releases.maxOfOrNull { it.versionCode } ?: 3) + 1).toString()
        versionName = ""
        changelog = ""
        apkUrl = ""
        telegramUrl = ""
        minVersionCode = "3"
        published = false
    }

    fun loadReleases() {
        scope.launch {
            loading = true
            when (val result = AppReleasesRepository.list()) {
                is AppReleasesResult.Success -> releases = result.releases
                is AppReleasesResult.Failure -> message = result.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadReleases() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App release management", color = WisdomTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Save a draft first. Publishing a newer version immediately makes it mandatory for students. Do not publish until the production APK and links are verified.",
            color = WisdomMuted,
            fontSize = 13.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminActionButton(
                label = "New release",
                tone = WisdomCyan,
                filled = false,
                enabled = !saving,
                onClick = { clearEditor(); message = null },
                modifier = Modifier.weight(1f)
            )
            AdminActionButton(
                label = "Refresh",
                tone = WisdomCyan,
                filled = false,
                enabled = !loading && !saving,
                onClick = { loadReleases() },
                modifier = Modifier.weight(1f)
            )
        }

        Text("Release details", color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        AdminTextField(versionCode, { versionCode = it.filter(Char::isDigit).take(9) }, "Version code", keyboardType = KeyboardType.Number)
        AdminTextField(versionName, { versionName = it }, "Version name (e.g. 1.1.0)")
        AdminTextArea(changelog, { changelog = it }, "Release notes — one item per line", minHeight = 104.dp)
        AdminTextField(apkUrl, { apkUrl = it }, "Direct APK HTTPS URL")
        AdminTextField(telegramUrl, { telegramUrl = it }, "Telegram post URL (https://t.me/...)")
        AdminTextField(minVersionCode, { minVersionCode = it.filter(Char::isDigit).take(9) }, "Minimum supported version code", keyboardType = KeyboardType.Number)

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WisdomCard)
                .border(1.dp, com.wisdomtower.academy.ui.theme.WisdomBorderWhite, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Publish this release", color = WisdomTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("A published newer version blocks the older app.", color = WisdomMuted, fontSize = 12.sp)
            }
            Switch(checked = published, onCheckedChange = { published = it })
        }

        AdminActionButton(
            label = if (saving) "Saving…" else "Save release",
            tone = WisdomAccentEmerald,
            filled = true,
            enabled = !saving && !loading,
            onClick = {
                val code = versionCode.toIntOrNull()
                val minimum = minVersionCode.toIntOrNull()
                if (code == null || minimum == null) {
                    message = "Enter valid version codes."
                } else {
                    saving = true
                    message = null
                    scope.launch {
                        when (val result = AppReleasesRepository.save(
                            AdminAppRelease(
                                versionCode = code,
                                versionName = versionName,
                                changelog = changelog,
                                apkUrl = apkUrl,
                                telegramUrl = telegramUrl,
                                minVersionCode = minimum,
                                published = published
                            )
                        )) {
                            is AppReleasesResult.Success -> {
                                releases = result.releases
                                message = "Release saved."
                            }
                            is AppReleasesResult.Failure -> message = result.message
                        }
                        saving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        message?.let { Text(it, color = WisdomMuted, fontSize = 13.sp) }

        Text("Saved releases", color = WisdomTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (loading) {
            Text("Loading releases…", color = WisdomMuted, fontSize = 13.sp)
        } else if (releases.isEmpty()) {
            AdminMessageCard(
                title = "No release records",
                body = "Create an unpublished draft before preparing the next APK.",
                actionLabel = null,
                onAction = {},
                modifier = Modifier
            )
        } else {
            releases.forEach { release ->
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(WisdomCard)
                        .border(1.dp, com.wisdomtower.academy.ui.theme.WisdomBorderWhite, RoundedCornerShape(14.dp))
                        .clickable {
                            versionCode = release.versionCode.toString()
                            versionName = release.versionName
                            changelog = release.changelog
                            apkUrl = release.apkUrl
                            telegramUrl = release.telegramUrl.orEmpty()
                            minVersionCode = release.minVersionCode.toString()
                            published = release.published
                            message = "Editing version ${release.versionCode}."
                        }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "v${release.versionName} · code ${release.versionCode}",
                            color = WisdomTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (release.published) "PUBLISHED" else "DRAFT",
                            color = if (release.published) WisdomAccentEmerald else WisdomMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Minimum supported: ${release.minVersionCode}", color = WisdomMuted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
