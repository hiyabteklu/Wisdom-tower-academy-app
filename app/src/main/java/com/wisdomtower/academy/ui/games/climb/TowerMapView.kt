package com.wisdomtower.academy.ui.games.climb

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.games.ClimbFloor
import com.wisdomtower.academy.data.games.ClimbFloorState
import com.wisdomtower.academy.data.games.ClimbTowerModel
import com.wisdomtower.academy.data.games.GameStore
import com.wisdomtower.academy.ui.games.common.GameTheme
import kotlin.math.cos
import kotlin.math.sin

private val FloorHeight = 200.dp

@Composable
fun TowerMapView(
    model: ClimbTowerModel,
    floorRecords: Map<String, GameStore.FloorRecord>,
    onFloorTap: (ClimbFloor) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val total = model.floors.size
    val infinite = rememberInfiniteTransition(label = "tower")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val gear by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "gear"
    )
    val ordered = remember(model.floors) { model.floors.asReversed() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val skyBrush = remember(model.currentFloorNumber, total) {
            val zone = GameTheme.zoneForFloor(model.currentFloorNumber ?: 1, total.coerceAtLeast(1))
            val p = GameTheme.zonePalette(zone)
            Brush.verticalGradient(listOf(p.skyTop, p.skyBottom))
        }
        Box(Modifier.fillMaxSize().background(skyBrush))
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "cap") { CapSegment(phase = phase) }
            items(ordered, key = { it.key }) { floor ->
                val state = floorUiState(floor, model.currentFloorNumber, floorRecords)
                FloorSegment(
                    floor = floor,
                    state = state,
                    totalFloors = total,
                    phase = phase,
                    gearAngle = gear,
                    stars = floorRecords[floor.key]?.stars ?: 0,
                    onTap = { onFloorTap(floor) }
                )
            }
            item(key = "foundation") { FoundationSegment() }
        }
    }
}

private fun floorUiState(
    floor: ClimbFloor,
    currentNumber: Int?,
    records: Map<String, GameStore.FloorRecord>
): ClimbFloorState {
    if (floor.sealed) return ClimbFloorState.Sealed
    val rec = records[floor.key]
    if (rec != null && rec.stars > 0) return ClimbFloorState.Cleared
    if (currentNumber != null && floor.number == currentNumber) return ClimbFloorState.Current
    return ClimbFloorState.Available
}

@Composable
private fun FloorSegment(
    floor: ClimbFloor,
    state: ClimbFloorState,
    totalFloors: Int,
    phase: Float,
    gearAngle: Float,
    stars: Int,
    onTap: () -> Unit
) {
    val zone = GameTheme.zoneForFloor(floor.number, totalFloors.coerceAtLeast(1))
    val palette = GameTheme.zonePalette(zone)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (floor.isBoss) 220.dp else FloorHeight)
            .clickable(onClick = onTap)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wallW = if (floor.isBoss) w * 0.88f else w * 0.72f
            val left = (w - wallW) / 2f
            val dim = if (state == ClimbFloorState.Sealed) 0.55f else 1f
            drawRoundRect(
                color = palette.wall.copy(alpha = dim),
                topLeft = Offset(left, 8f),
                size = Size(wallW, h - 16f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = palette.wallDark.copy(alpha = dim),
                topLeft = Offset(left + 6f, 14f),
                size = Size(wallW - 12f, h - 28f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            when (zone) {
                GameTheme.Zone.StoneFoundation -> {
                    for (row in 0 until 4) {
                        val yy = 24f + row * 28f
                        drawLine(
                            Color(0xFF2A2A30).copy(alpha = 0.5f * dim),
                            Offset(left + 10f, yy),
                            Offset(left + wallW - 10f, yy),
                            strokeWidth = 2f
                        )
                    }
                    drawCircle(Color(0x88FF9800), 8f + 2f * sin(phase * 3f), Offset(left + 20f, h * 0.35f))
                }
                GameTheme.Zone.LibraryHall -> {
                    for (col in 0 until 5) {
                        val cx = left + 18f + col * (wallW - 36f) / 4f
                        drawRect(Color(0xFF8B4513).copy(alpha = 0.6f * dim), Offset(cx, 30f), Size(14f, h - 50f))
                    }
                }
                GameTheme.Zone.ClockworkGallery -> {
                    val cx = left + wallW / 2f
                    val cy = h / 2f
                    drawCircle(palette.accent.copy(alpha = 0.5f * dim), 22f, Offset(cx, cy), style = Stroke(4f))
                    for (i in 0 until 8) {
                        val a = Math.toRadians((gearAngle + i * 45.0))
                        val gx = cx + (28f * cos(a)).toFloat()
                        val gy = cy + (28f * sin(a)).toFloat()
                        drawCircle(palette.accent.copy(alpha = 0.7f * dim), 5f, Offset(gx, gy))
                    }
                }
                GameTheme.Zone.Observatory -> {
                    for (s in 0 until 12) {
                        val sx = left + 10f + (s * 37f) % (wallW - 20f)
                        val sy = 20f + (s * 19f) % (h - 40f)
                        drawCircle(Color.White.copy(alpha = 0.4f * dim), 1.5f, Offset(sx, sy))
                    }
                }
                GameTheme.Zone.SkyCrown -> {
                    drawCircle(Color(0x44A78BFA), 40f + 8f * sin(phase), Offset(left + wallW / 2f, h * 0.3f))
                }
            }
            val winColor = when (state) {
                ClimbFloorState.Cleared -> Color(0xFFFFE08A)
                ClimbFloorState.Current -> Color(0xFF22E0FF).copy(alpha = 0.7f + 0.3f * sin(phase * 2f))
                ClimbFloorState.Available -> Color(0xFF334155)
                ClimbFloorState.Sealed -> Color(0xFF1E293B)
            }
            val wx = left + wallW * 0.5f - 18f
            val wy = h * 0.35f
            drawRoundRect(winColor, Offset(wx, wy), Size(36f, 44f), CornerRadius(4f, 4f))
            if (state == ClimbFloorState.Cleared && stars > 0) {
                for (s in 0 until stars) {
                    drawCircle(GameTheme.gold, 5f, Offset(wx + 8f + s * 12f, wy - 12f))
                }
            }
            if (floor.isBoss) {
                drawRoundRect(
                    GameTheme.gold.copy(alpha = 0.85f),
                    Offset(left + wallW * 0.2f, 4f),
                    Size(wallW * 0.6f, 22f),
                    CornerRadius(4f, 4f)
                )
            }
            if (state == ClimbFloorState.Sealed) {
                drawLine(Color(0xFF94A3B8), Offset(left + 20f, 30f), Offset(left + wallW - 20f, h - 30f), 3f)
                drawLine(Color(0xFF94A3B8), Offset(left + wallW - 20f, 30f), Offset(left + 20f, h - 30f), 3f)
                drawCircle(Color(0xFF64748B), 10f, Offset(left + wallW / 2f, h / 2f))
            }
            if (state == ClimbFloorState.Current) {
                OwlPainter.draw(
                    this,
                    center = Offset(left + wallW * 0.75f, h * 0.55f),
                    sizePx = 56f,
                    phase = phase,
                    flap = 0.15f * sin(phase * 1.5f),
                    blink = (sin(phase * 0.4f) > 0.92f)
                )
            }
            drawCircle(palette.accent.copy(alpha = 0.8f), 14f, Offset(left + 28f, h - 28f))
        }
        Text(
            text = "${floor.number}",
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 14.dp)
        )
        Text(
            text = if (floor.isBoss) "Boss · ${floor.title}" else floor.title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = if (floor.isBoss) 28.dp else 12.dp)
        )
    }
}

@Composable
private fun FoundationSegment() {
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        drawRect(Color(0xFF2A2A30), Offset(0f, size.height * 0.3f), Size(size.width, size.height * 0.7f))
        for (i in 0 until 8) {
            drawRect(
                Color(0xFF3A3A42),
                Offset(i * size.width / 8f, size.height * 0.3f),
                Size(size.width / 8f - 2f, size.height * 0.7f)
            )
        }
    }
}

@Composable
private fun CapSegment(phase: Float) {
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        val p = GameTheme.zonePalette(GameTheme.Zone.SkyCrown)
        drawRect(brush = Brush.verticalGradient(listOf(p.skyTop, p.skyBottom)))
        drawCircle(Color(0x33E8C87A), 50f + 10f * sin(phase), Offset(size.width / 2f, size.height * 0.7f))
        for (i in 0 until 3) {
            val x = size.width * (0.3f + i * 0.2f)
            val path = Path().apply {
                moveTo(x, size.height)
                lineTo(x - 12f, size.height * 0.4f)
                lineTo(x + 12f, size.height * 0.4f)
                close()
            }
            drawPath(path, GameTheme.gold.copy(alpha = 0.7f))
        }
    }
}
