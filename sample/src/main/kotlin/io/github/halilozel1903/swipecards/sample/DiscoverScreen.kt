package io.github.halilozel1903.swipecards.sample

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.swipecards.SwipeCardDefaults
import io.github.halilozel1903.swipecards.SwipeCardStack
import io.github.halilozel1903.swipecards.SwipeCardState
import io.github.halilozel1903.swipecards.core.SwipeDirection
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(28.dp)

@Composable
fun DiscoverScreen(state: SwipeCardState) {
    val scope = rememberCoroutineScope()
    // The undo history already knows every swipe, so the liked list is derived from it.
    val liked = state.swipes
        .filter { it.direction == SwipeDirection.Right || it.direction == SwipeDirection.Up }
        .mapNotNull { destinations.getOrNull(it.index) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Header(likedCount = liked.size, remaining = (destinations.size - state.currentIndex).coerceAtLeast(0))

        SwipeCardStack(
            items = destinations,
            state = state,
            onSwiped = { destination, direction -> Log.d("Discover", "${destination.name}: $direction") },
            itemKey = { it.id },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp + SwipeCardDefaults.StackOffset * 2),
            emptyContent = { AllSeen(liked, onStartOver = { state.jumpTo(0) }) },
        ) { destination ->
            DestinationCard(destination)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val leaning = state.swipeDirection
            val progress = state.swipeProgress
            fun emphasis(direction: SwipeDirection) = if (leaning == direction) 1f + 0.18f * progress else 1f

            RoundButton("↺", "Undo", Color(0xFFF59E0B), 52.dp, 1f, enabled = state.canUndo) {
                scope.launch { state.undo() }
            }
            RoundButton("✕", "Nope", SwipeCardDefaults.overlayColor(SwipeDirection.Left), 68.dp, emphasis(SwipeDirection.Left), enabled = !state.isExhausted) {
                scope.launch { state.swipe(SwipeDirection.Left) }
            }
            RoundButton("★", "Super like", SwipeCardDefaults.overlayColor(SwipeDirection.Up), 52.dp, emphasis(SwipeDirection.Up), enabled = !state.isExhausted) {
                scope.launch { state.swipe(SwipeDirection.Up) }
            }
            RoundButton("♥", "Like", SwipeCardDefaults.overlayColor(SwipeDirection.Right), 68.dp, emphasis(SwipeDirection.Right), enabled = !state.isExhausted) {
                scope.launch { state.swipe(SwipeDirection.Right) }
            }
        }
    }
}

@Composable
private fun Header(likedCount: Int, remaining: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Discover", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "$remaining destinations to go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "♥ $likedCount liked",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DestinationCard(destination: Destination) {
    Box(
        Modifier
            .fillMaxSize()
            .shadow(10.dp, CardShape)
            .clip(CardShape),
    ) {
        DestinationArt(destination, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(22.dp),
        ) {
            Text(
                destination.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "${destination.country} · best ${destination.bestTime}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                destination.blurb,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                destination.tags.forEach { tag ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            tag,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllSeen(liked: List<Destination>, onStartOver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✈", fontSize = 56.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("You've seen every destination", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (liked.isEmpty()) "Nothing caught your eye this time." else "Liked: " + liked.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onStartOver) { Text("Start over") }
    }
}

@Composable
private fun RoundButton(
    glyph: String,
    label: String,
    color: Color,
    size: Dp,
    scale: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, color.copy(alpha = if (enabled) 0.6f else 0.2f)),
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                glyph,
                color = color.copy(alpha = if (enabled) 1f else 0.35f),
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
