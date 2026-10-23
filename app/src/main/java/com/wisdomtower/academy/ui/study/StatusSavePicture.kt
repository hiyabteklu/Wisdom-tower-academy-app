package com.wisdomtower.academy.ui.study

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.data.auth.AuthStateManager
import com.wisdomtower.academy.data.content.ProgressRow
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * STATUS CARD (docs/STATUS_CARD_PLAN.md, slice S4): the "Save as picture" button. Tap: the picture is built off the main thread
 * (data, logo, serif font, drawing), a small preview opens, then "Save to gallery" writes a JPG (quality 92).
 * API 29+: MediaStore into Pictures/Wisdom Tower, no permission. API 24-28: the system "Save as" screen (CreateDocument),
 * same as the Cover Page. A toast says if it worked. The bitmap is recycled when the preview closes.
 *
 * REAL DATA ONLY: the picture reads [StatusCardData] (same numbers as the "Your week" block). The student ID is the real one
 * from the profile; with none, the line is left out. No new permission, no new dependency.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
private val SspInk = Color(0xFF06121F)

@Composable
fun StatusSavePictureButton(
    rows: List<ProgressRow>,
    summary: StatusSummary,
    track: StatusTrack,
    studentName: String?,
    educationLevel: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var making by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    // API 24-28 only: the student picks where to save
    val legacySaveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri: Uri? ->
        val bmp = preview
        if (uri != null && bmp != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { sspWriteJpeg(context, bmp, uri) }
                saving = false
                Toast.makeText(context, if (ok) "Picture saved" else "Could not save the picture", Toast.LENGTH_LONG).show()
                if (ok) preview = null
            }
        } else {
            saving = false
        }
    }

    Button(
        onClick = {
            if (!making) {
                making = true
                scope.launch {
                    val made: Bitmap? = try {
                        sspMakePicture(context, rows, summary, track, studentName, educationLevel)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    } catch (e: OutOfMemoryError) {
                        null
                    }
                    making = false
                    if (made != null) {
                        preview = made
                    } else {
                        Toast.makeText(context, "Could not make the picture. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        enabled = !making && preview == null,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = WisdomCyan, contentColor = SspInk)
    ) {
        if (making) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SspInk)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Making your picture...", fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Save as picture", fontWeight = FontWeight.Bold)
        }
    }

    val bmp = preview
    if (bmp != null) {
        // recycle only after the preview has left the screen
        DisposableEffect(bmp) { onDispose { bmp.recycle() } }
        AlertDialog(
            onDismissRequest = { if (!saving) preview = null },
            title = { Text("Your week picture") },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview of your weekly status picture",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                Button(
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = WisdomCyan, contentColor = SspInk),
                    onClick = {
                        saving = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { sspSaveToGallery(context, bmp) }
                                saving = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Pictures > Wisdom Tower" else "Could not save the picture",
                                    Toast.LENGTH_LONG
                                ).show()
                                if (ok) preview = null
                            }
                        } else {
                            legacySaveLauncher.launch(sspFileName())
                        }
                    }
                ) { Text(if (saving) "Saving..." else "Save to gallery", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { preview = null }) { Text("Close") }
            }
        )
    }
}

/** Builds the data and draws the picture. The reads switch dispatchers themselves; logo, font and drawing run off the main thread. */
private suspend fun sspMakePicture(
    context: Context,
    rows: List<ProgressRow>,
    summary: StatusSummary,
    track: StatusTrack,
    studentName: String?,
    educationLevel: String?
): Bitmap {
    val data = buildStatusCardData(
        context = context,
        rows = rows,
        summary = summary,
        track = track,
        studentName = studentName,
        studentIdNumber = AuthStateManager.currentProfile.value?.studentIdNumber,
        educationLevel = educationLevel
    )
    val logo = withContext(Dispatchers.IO) { loadStatusCardLogo(context) }
    val serif = withContext(Dispatchers.IO) { loadStatusCardSerif(context) }
    return withContext(Dispatchers.Default) {
        try {
            renderStatusCardBitmap(data, logo, serif, Typeface.SANS_SERIF)
        } finally {
            logo?.recycle()
        }
    }
}

private fun sspFileName(): String =
    "WisdomTower-status-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".jpg"

/** API 29+: saves into Pictures/Wisdom Tower through MediaStore (no permission needed). */
@RequiresApi(Build.VERSION_CODES.Q)
private fun sspSaveToGallery(context: Context, bmp: Bitmap): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DISPLAY_NAME, sspFileName())
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wisdom Tower")
    values.put(MediaStore.Images.Media.IS_PENDING, 1)
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        val ok = resolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) } ?: false
        if (ok) {
            val done = ContentValues()
            done.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, done, null, null)
            true
        } else {
            resolver.delete(uri, null, null)
            false
        }
    } catch (e: Exception) {
        try {
            resolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
        false
    }
}

/** API 24-28: writes the JPG to the place the student chose in the system "Save as" screen. */
private fun sspWriteJpeg(context: Context, bmp: Bitmap, uri: Uri): Boolean = try {
    context.contentResolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) } ?: false
} catch (e: Exception) {
    false
}
