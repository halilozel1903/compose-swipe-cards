package io.github.halilozel1903.swipecards.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/** Draws a destination's landscape: a gradient sky, a sun (or aurora) and its scenery. */
@Composable
fun DestinationArt(destination: Destination, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(destination.skyTop, destination.skyBottom)))
        when (destination.scenery) {
            Scenery.Aurora -> aurora(destination)
            Scenery.Coast -> coast(destination)
            Scenery.City -> city(destination)
            Scenery.Balloons -> balloons(destination)
            Scenery.Mountains -> mountains(destination)
            Scenery.Desert -> desert(destination)
        }
        // Scrim so the white text at the bottom stays readable on every scene.
        drawRect(
            Brush.verticalGradient(
                0.45f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.78f),
            ),
        )
    }
}

private fun DrawScope.sun(color: Color, center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center, radius * 2.6f),
        radius = radius * 2.6f,
        center = center,
    )
    drawCircle(color, radius = radius, center = center)
}

private fun DrawScope.ridge(color: Color, baseY: Float, points: List<Pair<Float, Float>>) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, baseY * h)
        points.forEach { (x, y) -> lineTo(x * w, y * h) }
        lineTo(w, baseY * h)
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.stars(seed: Int, count: Int, maxY: Float) {
    val random = Random(seed)
    repeat(count) {
        val center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * maxY)
        drawCircle(Color.White.copy(alpha = 0.4f + random.nextFloat() * 0.6f), radius = 1f + random.nextFloat() * 2.2f, center = center)
    }
}

private fun DrawScope.aurora(d: Destination) {
    stars(seed = d.id, count = 90, maxY = 0.6f)
    val w = size.width
    val h = size.height
    for (band in 0..2) {
        val top = h * (0.12f + band * 0.07f)
        val path = Path().apply {
            moveTo(-w * 0.1f, top + h * 0.18f)
            cubicTo(w * 0.25f, top - h * 0.06f, w * 0.55f, top + h * 0.22f, w * 1.1f, top)
            lineTo(w * 1.1f, top + h * 0.2f)
            cubicTo(w * 0.6f, top + h * 0.36f, w * 0.3f, top + h * 0.1f, -w * 0.1f, top + h * 0.34f)
            close()
        }
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(Color.Transparent, d.sun.copy(alpha = 0.55f - band * 0.12f), Color.Transparent),
                startY = top - h * 0.05f,
                endY = top + h * 0.38f,
            ),
        )
    }
    ridge(d.far, 0.66f, listOf(0.12f to 0.5f, 0.24f to 0.6f, 0.4f to 0.44f, 0.58f to 0.62f, 0.76f to 0.48f, 0.9f to 0.6f))
    ridge(d.near, 0.8f, listOf(0.2f to 0.74f, 0.45f to 0.78f, 0.7f to 0.72f))
    // Cabins on the shore.
    val cabin = Color(0xFFB91C1C)
    listOf(0.18f, 0.27f, 0.62f).forEach { x ->
        drawRect(cabin, topLeft = Offset(w * x, h * 0.755f), size = Size(w * 0.06f, h * 0.035f))
        drawCircle(Color(0xFFFDE68A), radius = w * 0.006f, center = Offset(w * (x + 0.03f), h * 0.772f))
    }
}

private fun DrawScope.coast(d: Destination) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.56f
    sun(d.sun, Offset(w * 0.68f, horizon - h * 0.1f), w * 0.1f)
    drawRect(
        Brush.verticalGradient(listOf(d.far, d.far.copy(alpha = 0.85f)), startY = horizon, endY = h),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    // Sun glitter on the water.
    for (i in 0..7) {
        val y = horizon + h * 0.02f + i * h * 0.025f
        val half = w * (0.12f - i * 0.011f)
        drawLine(d.sun.copy(alpha = 0.7f - i * 0.07f), Offset(w * 0.68f - half, y), Offset(w * 0.68f + half, y), strokeWidth = h * 0.006f)
    }
    val shore = Path().apply {
        moveTo(0f, h * 0.7f)
        cubicTo(w * 0.3f, h * 0.66f, w * 0.55f, h * 0.8f, w, h * 0.76f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(shore, d.near)
    // A cliff with white houses on the left.
    ridge(Color(0xFF7C2D12).copy(alpha = 0.85f), 0.6f, listOf(0.0f to 0.44f, 0.18f to 0.46f, 0.3f to 0.58f))
    val houses = Random(d.id)
    repeat(9) {
        val x = w * (0.01f + houses.nextFloat() * 0.22f)
        val y = h * (0.43f + houses.nextFloat() * 0.07f)
        drawRect(Color.White, topLeft = Offset(x, y), size = Size(w * 0.04f, h * 0.022f))
    }
}

private fun DrawScope.city(d: Destination) {
    val w = size.width
    val h = size.height
    sun(d.sun, Offset(w * 0.3f, h * 0.36f), w * 0.11f)
    val random = Random(d.id * 31)
    var x = -w * 0.02f
    while (x < w) {
        val bw = w * (0.08f + random.nextFloat() * 0.08f)
        val top = h * (0.36f + random.nextFloat() * 0.22f)
        drawRect(d.far, topLeft = Offset(x, top), size = Size(bw, h - top))
        x += bw * 0.9f
    }
    x = -w * 0.04f
    while (x < w) {
        val bw = w * (0.1f + random.nextFloat() * 0.1f)
        val top = h * (0.5f + random.nextFloat() * 0.18f)
        drawRect(d.near, topLeft = Offset(x, top), size = Size(bw, h - top))
        var wy = top + h * 0.02f
        while (wy < h * 0.95f) {
            var wx = x + bw * 0.12f
            while (wx < x + bw * 0.85f) {
                if (random.nextFloat() > 0.45f) {
                    drawRect(Color(0xFFFDE68A).copy(alpha = 0.8f), topLeft = Offset(wx, wy), size = Size(bw * 0.12f, h * 0.012f))
                }
                wx += bw * 0.22f
            }
            wy += h * 0.03f
        }
        x += bw + w * 0.01f
    }
}

private fun DrawScope.balloons(d: Destination) {
    val w = size.width
    val h = size.height
    sun(d.sun, Offset(w * 0.22f, h * 0.5f), w * 0.09f)
    ridge(d.far, 0.62f, listOf(0.1f to 0.56f, 0.16f to 0.5f, 0.22f to 0.57f, 0.42f to 0.52f, 0.5f to 0.46f, 0.58f to 0.54f, 0.8f to 0.5f, 0.9f to 0.56f))
    // Fairy chimneys.
    listOf(0.12f, 0.3f, 0.36f, 0.7f, 0.84f).forEachIndexed { i, cx ->
        val top = h * (0.58f + (i % 2) * 0.04f)
        val half = w * 0.035f
        val chimney = Path().apply {
            moveTo(w * cx - half, h * 0.8f)
            lineTo(w * cx - half * 0.55f, top)
            cubicTo(w * cx - half * 0.5f, top - h * 0.03f, w * cx + half * 0.5f, top - h * 0.03f, w * cx + half * 0.55f, top)
            lineTo(w * cx + half, h * 0.8f)
            close()
        }
        drawPath(chimney, d.near)
    }
    ridge(d.near.copy(red = d.near.red * 0.8f, green = d.near.green * 0.8f, blue = d.near.blue * 0.8f), 0.78f, listOf(0.3f to 0.74f, 0.6f to 0.8f))
    val colors = listOf(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF8B5CF6))
    val spots = listOf(
        Triple(0.62f, 0.2f, 0.075f),
        Triple(0.82f, 0.32f, 0.055f),
        Triple(0.42f, 0.3f, 0.05f),
        Triple(0.28f, 0.14f, 0.04f),
        Triple(0.9f, 0.12f, 0.035f),
        Triple(0.52f, 0.42f, 0.03f),
    )
    spots.forEachIndexed { i, (cx, cy, r) ->
        val center = Offset(w * cx, h * cy)
        val radius = w * r
        val color = colors[i % colors.size]
        drawCircle(
            Brush.verticalGradient(listOf(color, color.copy(alpha = 0.75f)), startY = center.y - radius, endY = center.y + radius),
            radius = radius,
            center = center,
        )
        val neck = Path().apply {
            moveTo(center.x - radius * 0.8f, center.y + radius * 0.55f)
            lineTo(center.x + radius * 0.8f, center.y + radius * 0.55f)
            lineTo(center.x + radius * 0.25f, center.y + radius * 1.35f)
            lineTo(center.x - radius * 0.25f, center.y + radius * 1.35f)
            close()
        }
        drawPath(neck, color)
        drawRect(Color(0xFF78350F), topLeft = Offset(center.x - radius * 0.2f, center.y + radius * 1.45f), size = Size(radius * 0.4f, radius * 0.3f))
    }
}

private fun DrawScope.mountains(d: Destination) {
    val w = size.width
    val h = size.height
    sun(d.sun, Offset(w * 0.78f, h * 0.16f), w * 0.07f)
    ridge(d.far.copy(alpha = 0.7f), 0.6f, listOf(0.08f to 0.44f, 0.2f to 0.52f, 0.33f to 0.36f, 0.46f to 0.5f, 0.62f to 0.3f, 0.8f to 0.48f, 0.92f to 0.4f))
    // Snow caps on the two highest peaks.
    listOf(0.33f to 0.36f, 0.62f to 0.3f).forEach { (px, py) ->
        val cap = Path().apply {
            moveTo(w * px, h * py)
            lineTo(w * (px + 0.06f), h * (py + 0.075f))
            lineTo(w * (px + 0.02f), h * (py + 0.06f))
            lineTo(w * px, h * (py + 0.08f))
            lineTo(w * (px - 0.03f), h * (py + 0.06f))
            lineTo(w * (px - 0.06f), h * (py + 0.08f))
            close()
        }
        drawPath(cap, Color.White.copy(alpha = 0.92f))
    }
    ridge(d.near, 0.7f, listOf(0.15f to 0.56f, 0.3f to 0.64f, 0.5f to 0.54f, 0.7f to 0.66f, 0.85f to 0.58f))
    // A lake in the valley.
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0F766E)), startY = h * 0.72f, endY = h),
        topLeft = Offset(0f, h * 0.72f),
        size = Size(w, h * 0.28f),
    )
    // Pines on the shore.
    val random = Random(d.id)
    repeat(14) {
        val x = w * random.nextFloat()
        val tall = h * (0.05f + random.nextFloat() * 0.05f)
        val base = h * 0.73f
        val tree = Path().apply {
            moveTo(x, base - tall)
            lineTo(x + tall * 0.28f, base)
            lineTo(x - tall * 0.28f, base)
            close()
        }
        drawPath(tree, Color(0xFF052E16))
    }
}

private fun DrawScope.desert(d: Destination) {
    val w = size.width
    val h = size.height
    sun(d.sun, Offset(w * 0.5f, h * 0.42f), w * 0.12f)
    val dunes = listOf(
        Triple(0.56f, d.far, 0.08f),
        Triple(0.66f, d.far.copy(red = d.far.red * 0.92f, green = d.far.green * 0.82f), -0.06f),
        Triple(0.76f, d.near, 0.05f),
    )
    dunes.forEach { (y, color, lean) ->
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * (y + 0.06f))
            cubicTo(w * (0.25f + lean), h * (y - 0.1f), w * (0.55f + lean), h * (y + 0.12f), w, h * (y - 0.02f))
            lineTo(w, h)
            close()
        }
        drawPath(path, color)
    }
    // A camel caravan on the ridge.
    val random = Random(d.id)
    repeat(4) { i ->
        val cx = w * (0.3f + i * 0.07f)
        val cy = h * (0.575f + random.nextFloat() * 0.006f)
        drawCircle(Color(0xFF431407), radius = w * 0.018f, center = Offset(cx, cy))
        drawRect(Color(0xFF431407), topLeft = Offset(cx - w * 0.015f, cy), size = Size(w * 0.004f, h * 0.02f))
        drawRect(Color(0xFF431407), topLeft = Offset(cx + w * 0.011f, cy), size = Size(w * 0.004f, h * 0.02f))
        drawCircle(Color(0xFF431407), radius = w * 0.008f, center = Offset(cx + w * 0.026f, cy - h * 0.012f))
    }
}
