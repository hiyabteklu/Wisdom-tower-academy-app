package com.wisdomtower.academy.ui.packages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wisdomtower.academy.ui.study.StatusSubjectIcon

/**
 * My Status V2 (S5): the normal [OpenCard] with the small gauge icon ([StatusSubjectIcon]) on its top right corner,
 * shown only for a signed-in student. Tapping the icon opens the subject status card in place; tapping the card itself
 * works exactly as before. [scopePath] = the subject path the card opens (same string as `scope_path` of the catalog).
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun OpenCardWithStatus(
    title: String,
    image: Any?,
    scopePath: String,
    loggedIn: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OpenCard(title, image, onClick)
        if (loggedIn) {
            StatusSubjectIcon(
                scopePath = scopePath,
                title = title,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}
