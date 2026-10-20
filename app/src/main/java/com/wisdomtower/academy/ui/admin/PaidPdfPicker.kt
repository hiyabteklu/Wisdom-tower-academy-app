package com.wisdomtower.academy.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.admin.PaidPdfUploader
import com.wisdomtower.academy.ui.theme.WisdomAccentEmerald
import com.wisdomtower.academy.ui.theme.WisdomBorderWhite
import com.wisdomtower.academy.ui.theme.WisdomCyan
import kotlinx.coroutines.launch

// SECURITY S5d (2026-10-20): "Choose PDF from phone" for the Life Savers editor. The upload itself is PaidPdfUploader
// (data/admin), which talks to the Edge Function paid-pdf-upload (admin only, private bucket, no key in the app).
// On success [onUploaded] gets the Appwrite File ID, which the editor puts into its File ID field. Nothing is saved to
// the database here: the admin still taps Save.
// STATUS: `[~]` written, NOT compiled, NOT tested on a device.

@Composable
internal fun PaidPdfPicker(onUploaded: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var percent by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                percent = 0
                message = ""
                isError = false
                val outcome = PaidPdfUploader.upload(context.contentResolver, uri) { p -> percent = p }
                busy = false
                if (outcome.ok) {
                    message = "Uploaded. The File ID is filled in above. Tap Save to finish."
                    onUploaded(outcome.fileId)
                } else {
                    isError = true
                    message = outcome.message
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdminActionButton(
            label = if (busy) "Uploading $percent%" else "Choose PDF from phone",
            tone = WisdomCyan,
            filled = true,
            enabled = !busy,
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth()
        )
        if (busy) {
            val bar = RoundedCornerShape(3.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(bar)
                    .background(WisdomBorderWhite)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .fillMaxHeight()
                        .background(WisdomCyan)
                )
            }
        }
        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = if (isError) AdminRose else WisdomAccentEmerald,
                fontSize = 12.sp
            )
        }
    }
}
