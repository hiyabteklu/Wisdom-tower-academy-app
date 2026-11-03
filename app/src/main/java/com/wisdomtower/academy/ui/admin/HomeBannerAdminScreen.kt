package com.wisdomtower.academy.ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wisdomtower.academy.data.admin.AdminSupabase
import com.wisdomtower.academy.data.content.HomeBannerRepository
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomMuted
import com.wisdomtower.academy.ui.theme.WisdomTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant

private const val HOME_BANNER_BUCKET = "academy-home-banners"
private const val MAX_BANNER_BYTES = 200_000

private data class PreparedBanner(val bitmap: Bitmap, val bytes: ByteArray)

@Composable
internal fun HomeBannerAdminScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var currentVersion by remember { mutableStateOf(1L) }
    var currentIsDefault by remember { mutableStateOf(true) }
    var updatedAt by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PreparedBanner?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }
    var confirmPublish by remember { mutableStateOf(false) }
    var confirmDefault by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        loading = true
        val reply = AdminSupabase.get("academy_home_banner?select=image_url,version,is_default,updated_at&id=eq.1&limit=1")
        if (reply.ok) {
            runCatching {
                val rows = JSONArray(reply.body)
                if (rows.length() > 0) {
                    val row = rows.getJSONObject(0)
                    currentUrl = row.optString("image_url").takeIf { it.startsWith("https://") }
                    currentVersion = row.optLong("version", 1L).coerceAtLeast(1L)
                    currentIsDefault = row.optBoolean("is_default", true)
                    updatedAt = row.optString("updated_at")
                }
            }.onFailure { toast = "Could not read the banner settings" }
        } else {
            toast = reply.errorText()
        }
        loading = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                val prepared = withContext(Dispatchers.IO) { prepareBanner(context, uri) }
                selected = prepared
                toast = if (prepared == null) "Could not prepare that picture. Try another image." else
                    "Preview ready · ${prepared.bytes.size / 1024} KB · cropped to 16:9"
                busy = false
            }
        }
    }

    fun publishSelected() {
        val prepared = selected ?: return
        scope.launch {
            busy = true
            val nextVersion = currentVersion + 1L
            val objectPath = "banner_$nextVersion.jpg"
            val upload = AdminSupabase.uploadStorageObject(
                bucket = HOME_BANNER_BUCKET,
                objectPath = objectPath,
                bytes = prepared.bytes,
                contentType = "image/jpeg"
            )
            if (!upload.ok) {
                toast = "Upload failed: ${upload.errorText()}"
                busy = false
                return@launch
            }
            val publicUrl = AdminSupabase.publicStorageUrl(HOME_BANNER_BUCKET, objectPath)
            val changedAt = Instant.now().toString()
            val body = JSONObject()
                .put("image_url", publicUrl)
                .put("version", nextVersion)
                .put("is_default", false)
                .put("updated_at", changedAt)
            val update = AdminSupabase.patch("academy_home_banner?id=eq.1", body.toString())
            val changed = if (update.ok) runCatching { JSONArray(update.body).length() }.getOrDefault(0) else 0
            if (changed > 0) {
                currentUrl = publicUrl
                currentVersion = nextVersion
                currentIsDefault = false
                updatedAt = changedAt
                selected = null
                toast = "Home banner published"
            } else {
                toast = if (update.ok) "The banner settings were not updated" else update.errorText()
            }
            busy = false
            reloadKey += 1
        }
    }

    fun restoreDefault() {
        scope.launch {
            busy = true
            val nextVersion = currentVersion + 1L
            val changedAt = Instant.now().toString()
            val body = JSONObject()
                .put("image_url", JSONObject.NULL)
                .put("version", nextVersion)
                .put("is_default", true)
                .put("updated_at", changedAt)
            val update = AdminSupabase.patch("academy_home_banner?id=eq.1", body.toString())
            val changed = if (update.ok) runCatching { JSONArray(update.body).length() }.getOrDefault(0) else 0
            if (changed > 0) {
                currentUrl = null
                currentVersion = nextVersion
                currentIsDefault = true
                updatedAt = changedAt
                selected = null
                toast = "Bundled default banner restored"
            } else {
                toast = if (update.ok) "The banner settings were not updated" else update.errorText()
            }
            busy = false
            reloadKey += 1
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Home Banner", color = WisdomTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (currentIsDefault) "Using the bundled Academy picture" else
                "Last changed: ${updatedAt.take(10).ifBlank { "Date unavailable" }} · Version $currentVersion",
            color = WisdomMuted,
            fontSize = 12.sp
        )

        Text("LIVE BANNER", color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        AsyncImage(
            model = currentUrl ?: HomeBannerRepository.BUNDLED_FALLBACK,
            contentDescription = "Current Home welcome banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))
        )

        val preview = selected
        if (preview != null) {
            Text("NEW PICTURE PREVIEW", color = WisdomMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Image(
                bitmap = preview.bitmap.asImageBitmap(),
                contentDescription = "Cropped banner preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))
            )
            Text("JPEG · ${preview.bytes.size / 1024} KB · 1280 × 720 crop", color = WisdomMuted, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminActionButton(
                label = "Change picture",
                tone = WisdomAccentEmerald,
                filled = true,
                enabled = !busy && !loading,
                onClick = { picker.launch("image/*") },
                modifier = Modifier.weight(1f)
            )
            AdminActionButton(
                label = "Back to default",
                tone = WisdomAccentAmber,
                filled = false,
                enabled = !busy && !loading && !currentIsDefault,
                onClick = { confirmDefault = true },
                modifier = Modifier.weight(1f)
            )
        }

        if (preview != null) {
            Button(
                onClick = { confirmPublish = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator()
                else Text("Publish banner")
            }
        }
        if (loading) CircularProgressIndicator()
        if (toast.isNotBlank()) Text(toast, color = WisdomMuted, fontSize = 12.sp)
    }

    if (confirmPublish) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmPublish = false },
            title = { Text("Publish this picture?") },
            text = { Text("It will replace the welcome picture on Home for students after their banner cache refreshes.") },
            confirmButton = {
                TextButton(onClick = { confirmPublish = false; publishSelected() }, enabled = !busy) { Text("Publish") }
            },
            dismissButton = {
                TextButton(onClick = { confirmPublish = false }, enabled = !busy) { Text("Cancel") }
            }
        )
    }
    if (confirmDefault) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDefault = false },
            title = { Text("Restore the default picture?") },
            text = { Text("Students will see the bundled Academy welcome picture again.") },
            confirmButton = {
                TextButton(onClick = { confirmDefault = false; restoreDefault() }, enabled = !busy) { Text("Restore default") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDefault = false }, enabled = !busy) { Text("Cancel") }
            }
        )
    }
}

private fun prepareBanner(context: Context, uri: Uri): PreparedBanner? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 2560 && bounds.outHeight / (sample * 2) >= 1440) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val targetRatio = 16f / 9f
        val sourceRatio = source.width.toFloat() / source.height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        if (sourceRatio > targetRatio) {
            cropHeight = source.height
            cropWidth = (source.height * targetRatio).toInt().coerceAtLeast(1)
        } else {
            cropWidth = source.width
            cropHeight = (source.width / targetRatio).toInt().coerceAtLeast(1)
        }
        val cropped = Bitmap.createBitmap(
            source,
            ((source.width - cropWidth) / 2).coerceAtLeast(0),
            ((source.height - cropHeight) / 2).coerceAtLeast(0),
            cropWidth,
            cropHeight
        )
        var scaled = Bitmap.createScaledBitmap(cropped, 1280, 720, true)
        fun encode(bitmap: Bitmap, quality: Int): ByteArray {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            return output.toByteArray()
        }
        var quality = 90
        var bytes = encode(scaled, quality)
        while (bytes.size > MAX_BANNER_BYTES && quality > 35) {
            quality -= 5
            bytes = encode(scaled, quality)
        }
        if (bytes.size > MAX_BANNER_BYTES) {
            scaled = Bitmap.createScaledBitmap(scaled, 960, 540, true)
            quality = 82
            bytes = encode(scaled, quality)
            while (bytes.size > MAX_BANNER_BYTES && quality > 35) {
                quality -= 5
                bytes = encode(scaled, quality)
            }
        }
        if (bytes.size > MAX_BANNER_BYTES) null else PreparedBanner(scaled, bytes)
    } catch (e: Exception) {
        null
    }
}
